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

import dm.shakespeare.BackStage;
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
public abstract class Event<T> {

  static final Object CANCEL = new Object();
  static final Object GET = new Object();

  private static final Observer<?> NO_OP = new Observer<Object>() {

    public void accept(final Object value) {
    }
  };

  // TODO: 28/01/2019 incident/resolution???

  @NotNull
  public static <T> Event<T> ofIncident(@NotNull final Throwable obstacle) {
    return new IncidentEvent<T>(obstacle);
  }

  @NotNull
  public static <T> Event<T> ofResolution(final T result) {
    return new ResolutionEvent<T>(result);
  }

  @NotNull
  public static <T1, R> Event<R> when(@NotNull final Event<? extends T1> firstEvent,
      @NotNull final UnaryFunction<? super T1, ? extends Event<R>> resolutionHandler) {
    return new UnaryEvent<T1, R>(firstEvent, resolutionHandler);
  }

  @NotNull
  public static <T, R> Event<R> when(@NotNull final Iterable<? extends Event<? extends T>> events,
      @NotNull final UnaryFunction<? super List<T>, ? extends Event<R>> resolutionHandler) {
    return new GenericEvent<T, R>(events, resolutionHandler);
  }

  public boolean isMemorized() {
    return true;
  }

  public void observe(@Nullable final Observer<? super T> resolutionObserver,
      @Nullable final Observer<? super Throwable> incidentObserver) {
    observe(new DefaultEventObserver<T>(resolutionObserver, incidentObserver));
  }

  public void observe(@NotNull final EventObserver<? super T> eventObserver) {
    final Actor actor = BackStage.newActor(new EventObserverScript<T>(eventObserver));
    getActor().tell(GET, new Options().withReceiptId(actor.getId()), actor);
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <T extends Throwable, R> Event<R> resolve(
      @NotNull final Iterable<? extends Class<? extends T>> incidentTypes,
      @NotNull final UnaryFunction<? super T, ? extends Event<R>> incidentHandler) {
    return new ResolveEvent<R>(getActor(),
        Iterables.<Class<? extends Throwable>>toSet(incidentTypes),
        (UnaryFunction<? super Throwable, ? extends Event<R>>) incidentHandler);
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <T1 extends Throwable, R> Event<R> resolve(@NotNull final Class<? extends T1> firstType,
      @NotNull final UnaryFunction<? super T1, ? extends Event<R>> incidentHandler) {
    final HashSet<Class<? extends Throwable>> types = new HashSet<Class<? extends Throwable>>();
    types.add(firstType);
    return new ResolveEvent<R>(getActor(), types,
        (UnaryFunction<? super Throwable, ? extends Event<R>>) incidentHandler);
  }

  @NotNull
  public <R> Event<R> then(@NotNull UnaryFunction<? super T, ? extends Event<R>> messageHandler) {
    return when(this, messageHandler);
  }

  @NotNull
  abstract Actor getActor();

  public interface EventObserver<T> {

    void onIncident(@NotNull Throwable obstacle) throws Exception;

    void onResolution(T result) throws Exception;
  }

  static class DefaultEventObserver<T> implements EventObserver<T> {

    private final Observer<Object> mIncidentObserver;
    private final Observer<Object> mResolutionObserver;

    @SuppressWarnings("unchecked")
    DefaultEventObserver(@Nullable Observer<? super T> resolutionObserver,
        @Nullable Observer<? super Throwable> incidentObserver) {
      mResolutionObserver =
          (Observer<Object>) ((resolutionObserver != null) ? resolutionObserver : NO_OP);
      mIncidentObserver =
          (Observer<Object>) ((incidentObserver != null) ? incidentObserver : NO_OP);
    }

    public void onIncident(@NotNull final Throwable obstacle) throws Exception {
      mIncidentObserver.accept(obstacle);
    }

    public void onResolution(final T result) throws Exception {
      mResolutionObserver.accept(result);
    }
  }

  private abstract static class AbstractEvent<T> extends Event<T> {

    private final Actor mActor;
    private final Object[] mInputs;
    private final Options mOptions;
    private final PlayContext mPlayContext;
    private final HashMap<Actor, String> mSenders = new HashMap<Actor, String>();

    private int mInputCount;
    private Actor mOutputActor;

    private AbstractEvent(final int numInputs) {
      mInputs = new Object[numInputs];
      final PlayContext playContext = (mPlayContext = PlayContext.get());
      final Actor actor = (mActor = BackStage.newActor(new PlayScript(playContext) {

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
    Actor getIncidentActor(@NotNull final Incident incident) throws Exception {
      return null;
    }

    @NotNull
    abstract List<Actor> getInputActors();

    @Nullable
    Actor getOutputActor(@NotNull final Object[] inputs) throws Exception {
      return null;
    }

    private void fail(@NotNull final Incident incident, @NotNull final Context context) {
      for (final Entry<Actor, String> entry : mSenders.entrySet()) {
        entry.getKey()
            .tell(incident, new Options().withThread(entry.getValue()), context.getSelf());
      }
      context.setBehavior(new IncidentBehavior(incident.getCause()));
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
          fail(new Incident(new PlotCancelledException()), context);
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
          fail(new Incident(new PlotCancelledException()), context);

        } else if (message instanceof Incident) {
          try {
            final Actor incidentActor = getIncidentActor((Incident) message);
            if (incidentActor != null) {
              final Actor self = context.getSelf();
              (mOutputActor = incidentActor).tell(GET, mOptions, self);
              context.setBehavior(new OutputBehavior());

            } else {
              fail((Incident) message, context);
            }

          } catch (final Throwable t) {
            fail(new Incident(t), context);
            if (t instanceof InterruptedException) {
              Thread.currentThread().interrupt();
            }
          }

        } else if (message instanceof Bounce) {
          final Incident incident = new Incident(PlotStateException.getOrNew((Bounce) message));
          try {
            final Actor incidentActor = getIncidentActor(incident);
            if (incidentActor != null) {
              final Actor self = context.getSelf();
              (mOutputActor = incidentActor).tell(GET, mOptions, self);
              context.setBehavior(new OutputBehavior());

            } else {
              fail(incident, context);
            }

          } catch (final Throwable t) {
            fail(new Incident(t), context);
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
                  context.setBehavior(new ResolutionBehavior(message));
                }

              } catch (final Throwable t) {
                fail(new Incident(t), context);
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
          fail(new Incident(new PlotCancelledException()), context);

        } else if (message instanceof Incident) {
          fail((Incident) message, context);

        } else if (message instanceof Bounce) {
          final Throwable obstacle = PlotStateException.getOrNew((Bounce) message);
          fail(new Incident(obstacle), context);

        } else if (envelop.getSender().equals(mOutputActor)) {
          for (final Entry<Actor, String> entry : mSenders.entrySet()) {
            entry.getKey()
                .tell(message, new Options().withThread(entry.getValue()), context.getSelf());
          }
          context.setBehavior(new ResolutionBehavior(message));
        }
      }
    }
  }

  private static class GenericEvent<T, R> extends AbstractEvent<R> {

    private final List<Actor> mActors;
    private final UnaryFunction<? super List<T>, ? extends Event<R>> mResolutionHandler;

    private GenericEvent(@NotNull final Iterable<? extends Event<? extends T>> events,
        @NotNull final UnaryFunction<? super List<T>, ? extends Event<R>> resolutionHandler) {
      super(1);
      final ArrayList<Actor> actors = new ArrayList<Actor>();
      for (final Event<? extends T> event : events) {
        actors.add(event.getActor());
      }
      mActors = actors;
      mResolutionHandler = ConstantConditions.notNull("resolutionHandler", resolutionHandler);
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
        return mResolutionHandler.call(inputList).getActor();

      } finally {
        PlayContext.unset();
      }
    }
  }

  private static class IncidentBehavior extends AbstractBehavior {

    private final Incident mIncident;

    private IncidentBehavior(@NotNull final Throwable obstacle) {
      mIncident = new Incident(obstacle);
    }

    public void onMessage(final Object message, @NotNull final Envelop envelop,
        @NotNull final Context context) {
      if (message == GET) {
        envelop.getSender().tell(mIncident, envelop.getOptions().threadOnly(), context.getSelf());
      }
    }
  }

  private static class IncidentEvent<T> extends Event<T> {

    private final Actor mActor;

    private IncidentEvent(@NotNull final Throwable obstacle) {
      final Incident incident = new Incident(obstacle);
      mActor = BackStage.newActor(new PlayScript(PlayContext.get()) {

        @NotNull
        @Override
        public Behavior getBehavior(@NotNull final String id) {
          return newBehavior().onMessageEqualTo(GET, new Handler<Object>() {

            public void handle(final Object message, @NotNull final Envelop envelop,
                @NotNull final Context context) {
              envelop.getSender()
                  .tell(incident, envelop.getOptions().threadOnly(), context.getSelf());
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

  private static class ResolutionBehavior extends AbstractBehavior {

    private final Object mResult;

    private ResolutionBehavior(final Object result) {
      mResult = result;
    }

    public void onMessage(final Object message, @NotNull final Envelop envelop,
        @NotNull final Context context) {
      if (message == GET) {
        envelop.getSender().tell(mResult, envelop.getOptions().threadOnly(), context.getSelf());
      }
    }
  }

  private static class ResolutionEvent<T> extends Event<T> {

    private final Actor mActor;

    private ResolutionEvent(final T result) {
      mActor = BackStage.newActor(new PlayScript(PlayContext.get()) {

        @NotNull
        @Override
        public Behavior getBehavior(@NotNull final String id) {
          return newBehavior().onMessageEqualTo(GET, new Handler<Object>() {

            public void handle(final Object message, @NotNull final Envelop envelop,
                @NotNull final Context context) {
              envelop.getSender()
                  .tell(result, envelop.getOptions().threadOnly(), context.getSelf());
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

  private static class ResolveEvent<T> extends AbstractEvent<T> {

    private final List<Actor> mActors;
    private final UnaryFunction<? super Throwable, ? extends Event<T>> mIncidentHandler;
    private final Set<Class<? extends Throwable>> mIncidentTypes;

    private ResolveEvent(@NotNull final Actor eventActor,
        @NotNull final Set<Class<? extends Throwable>> incidentTypes,
        @NotNull final UnaryFunction<? super Throwable, ? extends Event<T>> incidentHandler) {
      super(1);
      mActors = Collections.singletonList(eventActor);
      mIncidentTypes = ConstantConditions.notNullElements("incidentTypes", incidentTypes);
      mIncidentHandler = ConstantConditions.notNull("incidentHandler", incidentHandler);
    }

    @Nullable
    @Override
    Actor getIncidentActor(@NotNull final Incident incident) throws Exception {
      final Throwable obstacle = incident.getCause();
      for (final Class<? extends Throwable> incidentType : mIncidentTypes) {
        if (incidentType.isInstance(obstacle)) {
          PlayContext.set(getContext());
          try {
            return mIncidentHandler.call(obstacle).getActor();

          } finally {
            PlayContext.unset();
          }
        }
      }
      return super.getIncidentActor(incident);
    }

    @NotNull
    List<Actor> getInputActors() {
      return mActors;
    }
  }

  private static class UnaryEvent<T1, R> extends AbstractEvent<R> {

    private final List<Actor> mActors;
    private final UnaryFunction<? super T1, ? extends Event<R>> mResolutionHandler;

    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    private UnaryEvent(@NotNull final Event<? extends T1> firstEvent,
        @NotNull final UnaryFunction<? super T1, ? extends Event<R>> resolutionHandler) {
      super(1);
      mActors = Arrays.asList(firstEvent.getActor());
      mResolutionHandler = ConstantConditions.notNull("resolutionHandler", resolutionHandler);
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
        return mResolutionHandler.call((T1) inputs[0]).getActor();

      } finally {
        PlayContext.unset();
      }
    }
  }
}
