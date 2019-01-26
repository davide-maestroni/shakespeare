package dm.shakespeare.plot;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import dm.shakespeare.LocalStage;
import dm.shakespeare.actor.AbstractBehavior;
import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Behavior.Context;
import dm.shakespeare.actor.BehaviorBuilder.Handler;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.Options;
import dm.shakespeare.function.Observer;
import dm.shakespeare.message.Bounce;
import dm.shakespeare.plot.function.UnaryFunction;
import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare.util.Iterables;

/**
 * Created by davide-maestroni on 01/22/2019.
 */
public abstract class Line<T> {

  static final Object CANCEL = new Object();
  static final Object GET = new Object();

  private static final Observer<?> NO_OP = new Observer<Object>() {

    public void accept(final Object value) {
    }
  };

  private static final LocalStage sStage = new LocalStage();

  @NotNull
  public static <T> Line<T> ofError(@NotNull final Throwable error) {
    return new ErrorLine<T>(error);
  }

  @NotNull
  public static <T> Line<T> ofValue(final T value) {
    return new ValueLine<T>(value);
  }

  @NotNull
  public static <T1, R> Line<R> translate(@NotNull final Line<? extends T1> firstLine,
      @NotNull final UnaryFunction<? super T1, ? extends Line<R>> messageHandler) {
    return new UnaryLine<T1, R>(firstLine, messageHandler);
  }

  @NotNull
  public static <T, R> Line<R> translate(@NotNull final Iterable<? extends Line<? extends T>> lines,
      @NotNull final UnaryFunction<? super List<T>, ? extends Line<R>> messageHandler) {
    return new GenericLine<T, R>(lines, messageHandler);
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <T1 extends Throwable, R> Line<R> correct(@NotNull final Class<? extends T1> firstError,
      @NotNull final UnaryFunction<? super T1, ? extends Line<R>> errorHandler) {
    final HashSet<Class<? extends Throwable>> errors = new HashSet<Class<? extends Throwable>>();
    errors.add(firstError);
    return new CorrectLine<R>(getActor(), errors,
        (UnaryFunction<? super Throwable, ? extends Line<R>>) errorHandler);
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <T extends Throwable, R> Line<R> correct(
      @NotNull final Iterable<? extends Class<? extends T>> errors,
      @NotNull final UnaryFunction<? super T, ? extends Line<R>> errorHandler) {
    return new CorrectLine<R>(getActor(), Iterables.<Class<? extends Throwable>>toSet(errors),
        (UnaryFunction<? super Throwable, ? extends Line<R>>) errorHandler);
  }

  public boolean isMemorized() {
    return true;
  }

  public void read(@Nullable final Observer<? super T> valueObserver,
      @Nullable final Observer<? super Throwable> errorObserver) {
    read(new DefaultLineObserver<T>(valueObserver, errorObserver));
  }

  public void read(@NotNull final LineObserver<? super T> lineObserver) {
    final Actor actor = sStage.newActor(new ReadLineScript<T>(lineObserver));
    getActor().tell(GET, new Options().withReceiptId(actor.getId()), actor);
  }

  @NotNull
  public <R> Line<R> translate(
      @NotNull UnaryFunction<? super T, ? extends Line<R>> messageHandler) {
    return translate(this, messageHandler);
  }

  @NotNull
  abstract Actor getActor();

  public interface LineObserver<T> {

    void onError(@NotNull Throwable error) throws Exception;

    void onMessage(T message) throws Exception;
  }

  static class DefaultLineObserver<T> implements LineObserver<T> {

    private final Observer<Object> mErrorObserver;
    private final Observer<Object> mValueObserver;

    @SuppressWarnings("unchecked")
    DefaultLineObserver(@Nullable Observer<? super T> valueObserver,
        @Nullable Observer<? super Throwable> errorObserver) {
      mValueObserver = (Observer<Object>) ((valueObserver != null) ? valueObserver : NO_OP);
      mErrorObserver = (Observer<Object>) ((errorObserver != null) ? errorObserver : NO_OP);
    }

    public void onError(@NotNull final Throwable error) throws Exception {
      mErrorObserver.accept(error);
    }

    public void onMessage(final T message) throws Exception {
      mValueObserver.accept(message);
    }
  }

  private abstract static class AbstractLine<T> extends Line<T> {

    private final Actor mActor;
    private final Object[] mInputs;
    private final Options mOptions;
    private final PlayContext mPlayContext;
    private final HashMap<Actor, String> mSenders = new HashMap<Actor, String>();

    private int mInputCount;
    private Actor mOutputActor;

    private AbstractLine(final int numInputs) {
      mInputs = new Object[numInputs];
      final PlayContext playContext = (mPlayContext = PlayContext.get());
      final Actor actor = (mActor = playContext.getStage().newActor(new PlayScript(playContext) {

        @NotNull
        @Override
        public Behavior getBehavior(@NotNull final String id) {
          return new InitBehavior();
        }
      }));
      mOptions = new Options().withReceiptId(actor.getId());
    }

    @NotNull
    Actor getActor() {
      return mActor;
    }

    @NotNull
    PlayContext getContext() {
      return mPlayContext;
    }

    @Nullable
    Actor getFailureActor(@NotNull final LineFailure failure) throws Exception {
      return null;
    }

    @NotNull
    abstract List<Actor> getInputActors();

    @Nullable
    Actor getOutputActor(@NotNull final Object[] inputs) throws Exception {
      return null;
    }

    private void fail(@NotNull final LineFailure failure, @NotNull final Context context) {
      for (final Entry<Actor, String> entry : mSenders.entrySet()) {
        entry.getKey().tell(failure, new Options().withThread(entry.getValue()), context.getSelf());
      }
      context.setBehavior(new FailureBehavior(failure.getCause()));
    }

    private class InitBehavior extends AbstractBehavior {

      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          mSenders.put(envelop.getSender(), envelop.getOptions().getThread());
          final Actor self = context.getSelf();
          for (final Actor actor : getInputActors()) {
            actor.tell(GET, mOptions, self);
          }
          context.setBehavior(new InputBehavior());

        } else if (message == CANCEL) {
          fail(new LineFailure(new PlotCancelledException()), context);
        }
      }
    }

    private class InputBehavior extends AbstractBehavior {

      @SuppressWarnings("unchecked")
      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          // TODO: 25/01/2019 loop detection?
          mSenders.put(envelop.getSender(), envelop.getOptions().getThread());

        } else if (message == CANCEL) {
          fail(new LineFailure(new PlotCancelledException()), context);

        } else if (message instanceof LineFailure) {
          try {
            final Actor failureActor = getFailureActor((LineFailure) message);
            if (failureActor != null) {
              final Actor self = context.getSelf();
              (mOutputActor = failureActor).tell(GET, mOptions, self);
              context.setBehavior(new OutputBehavior());

            } else {
              fail((LineFailure) message, context);
            }

          } catch (final Throwable t) {
            fail(new LineFailure(t), context);
            if (t instanceof InterruptedException) {
              Thread.currentThread().interrupt();
            }
          }

        } else if (message instanceof Bounce) {
          final LineFailure lineFailure =
              new LineFailure(PlotStateException.getError((Bounce) message));
          try {
            final Actor failureActor = getFailureActor(lineFailure);
            if (failureActor != null) {
              final Actor self = context.getSelf();
              (mOutputActor = failureActor).tell(GET, mOptions, self);
              context.setBehavior(new OutputBehavior());

            } else {
              fail(lineFailure, context);
            }

          } catch (final Throwable t) {
            fail(new LineFailure(t), context);
            if (t instanceof InterruptedException) {
              Thread.currentThread().interrupt();
            }
          }

        } else {
          final int index = getInputActors().indexOf(envelop.getSender());
          if (index >= 0) {
            final Object[] inputs = mInputs;
            inputs[index] = message;
            if (++mInputCount == inputs.length) {
              try {
                final Actor outputActor = getOutputActor(inputs);
                if (outputActor != null) {
                  final Actor self = context.getSelf();
                  (mOutputActor = outputActor).tell(GET, mOptions, self);
                  context.setBehavior(new OutputBehavior());

                } else {
                  context.setBehavior(new ValueBehavior(message));
                }

              } catch (final Throwable t) {
                fail(new LineFailure(t), context);
                if (t instanceof InterruptedException) {
                  Thread.currentThread().interrupt();
                }
              }
            }
          }
        }
      }
    }

    private class OutputBehavior extends AbstractBehavior {

      @SuppressWarnings("unchecked")
      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == CANCEL) {
          fail(new LineFailure(new PlotCancelledException()), context);

        } else if (message instanceof LineFailure) {
          fail((LineFailure) message, context);

        } else if (message instanceof Bounce) {
          final Throwable error = PlotStateException.getError((Bounce) message);
          fail(new LineFailure(error), context);

        } else if (envelop.getSender().equals(mOutputActor)) {
          for (final Entry<Actor, String> entry : mSenders.entrySet()) {
            entry.getKey()
                .tell(message, new Options().withThread(entry.getValue()), context.getSelf());
          }
          context.setBehavior(new ValueBehavior(message));
        }
      }
    }
  }

  private static class CorrectLine<T> extends AbstractLine<T> {

    private final List<Actor> mActors;
    private final UnaryFunction<? super Throwable, ? extends Line<T>> mErrorHandler;
    private final Set<Class<? extends Throwable>> mErrorTypes;

    private CorrectLine(@NotNull final Actor lineActor,
        @NotNull final Set<Class<? extends Throwable>> errorTypes,
        @NotNull final UnaryFunction<? super Throwable, ? extends Line<T>> errorHandler) {
      super(1);
      mActors = Collections.singletonList(lineActor);
      mErrorTypes = ConstantConditions.notNullElements("errorTypes", errorTypes);
      mErrorHandler = ConstantConditions.notNull("errorHandler", errorHandler);
    }

    @Nullable
    @Override
    Actor getFailureActor(@NotNull final LineFailure failure) throws Exception {
      final Throwable error = failure.getCause();
      for (final Class<? extends Throwable> errorType : mErrorTypes) {
        if (errorType.isInstance(error)) {
          PlayContext.set(getContext());
          try {
            return mErrorHandler.call(error).getActor();

          } finally {
            PlayContext.unset();
          }
        }
      }
      return super.getFailureActor(failure);
    }

    @NotNull
    List<Actor> getInputActors() {
      return mActors;
    }
  }

  private static class ErrorLine<T> extends Line<T> {

    private final Actor mActor;

    private ErrorLine(@NotNull final Throwable error) {
      final LineFailure failure = new LineFailure(error);
      final PlayContext playContext = PlayContext.get();
      mActor = playContext.getStage().newActor(new PlayScript(playContext) {

        @NotNull
        @Override
        public Behavior getBehavior(@NotNull final String id) {
          return newBehavior().onMessageEqualTo(GET, new Handler<Object>() {

            public void handle(final Object message, @NotNull final Envelop envelop,
                @NotNull final Context context) {
              envelop.getSender()
                  .tell(failure, envelop.getOptions().threadOnly(), context.getSelf());
            }
          }).build();
        }
      });
    }

    @NotNull
    Actor getActor() {
      return mActor;
    }
  }

  private static class FailureBehavior extends AbstractBehavior {

    private final LineFailure mFailure;

    private FailureBehavior(@NotNull final Throwable error) {
      mFailure = new LineFailure(error);
    }

    public void onMessage(final Object message, @NotNull final Envelop envelop,
        @NotNull final Context context) {
      if (message == GET) {
        envelop.getSender().tell(mFailure, envelop.getOptions().threadOnly(), context.getSelf());
      }
    }
  }

  private static class GenericLine<T, R> extends AbstractLine<R> {

    private final List<Actor> mActors;
    private final UnaryFunction<? super List<T>, ? extends Line<R>> mMessageHandler;

    private GenericLine(@NotNull final Iterable<? extends Line<? extends T>> lines,
        @NotNull final UnaryFunction<? super List<T>, ? extends Line<R>> messageHandler) {
      super(1);
      final ArrayList<Actor> actors = new ArrayList<Actor>();
      for (final Line<? extends T> line : lines) {
        actors.add(line.getActor());
      }
      mActors = actors;
      mMessageHandler = ConstantConditions.notNull("messageHandler", messageHandler);
    }

    @NotNull
    List<Actor> getInputActors() {
      return mActors;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    Actor getOutputActor(@NotNull final Object[] inputs) throws Exception {
      PlayContext.set(getContext());
      try {
        final ArrayList<T> inputList = new ArrayList<T>();
        for (final Object input : inputs) {
          inputList.add((T) input);
        }
        return mMessageHandler.call(inputList).getActor();

      } finally {
        PlayContext.unset();
      }
    }
  }

  private static class UnaryLine<T1, R> extends AbstractLine<R> {

    private final List<Actor> mActors;
    private final UnaryFunction<? super T1, ? extends Line<R>> mMessageHandler;

    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    private UnaryLine(@NotNull final Line<? extends T1> firstLine,
        @NotNull final UnaryFunction<? super T1, ? extends Line<R>> messageHandler) {
      super(1);
      mActors = Arrays.asList(firstLine.getActor());
      mMessageHandler = ConstantConditions.notNull("messageHandler", messageHandler);
    }

    @NotNull
    List<Actor> getInputActors() {
      return mActors;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    Actor getOutputActor(@NotNull final Object[] inputs) throws Exception {
      PlayContext.set(getContext());
      try {
        return mMessageHandler.call((T1) inputs[0]).getActor();

      } finally {
        PlayContext.unset();
      }
    }
  }

  private static class ValueBehavior extends AbstractBehavior {

    private final Object mValue;

    private ValueBehavior(final Object value) {
      mValue = value;
    }

    public void onMessage(final Object message, @NotNull final Envelop envelop,
        @NotNull final Context context) {
      if (message == GET) {
        envelop.getSender().tell(mValue, envelop.getOptions().threadOnly(), context.getSelf());
      }
    }
  }

  private static class ValueLine<T> extends Line<T> {

    private final Actor mActor;

    private ValueLine(final T value) {
      final PlayContext playContext = PlayContext.get();
      mActor = playContext.getStage().newActor(new PlayScript(playContext) {

        @NotNull
        @Override
        public Behavior getBehavior(@NotNull final String id) {
          return newBehavior().onMessageEqualTo(GET, new Handler<Object>() {

            public void handle(final Object message, @NotNull final Envelop envelop,
                @NotNull final Context context) {
              envelop.getSender().tell(value, envelop.getOptions().threadOnly(), context.getSelf());
            }
          }).build();
        }
      });
    }

    @NotNull
    Actor getActor() {
      return mActor;
    }
  }
}
