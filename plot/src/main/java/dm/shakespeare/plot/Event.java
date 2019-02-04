package dm.shakespeare.plot;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dm.shakespeare.BackStage;
import dm.shakespeare.actor.AbstractBehavior;
import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Behavior.Context;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.Options;
import dm.shakespeare.function.Observer;
import dm.shakespeare.message.Bounce;
import dm.shakespeare.message.Receipt;
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
  private static final Object NULL = new Object();

  @NotNull
  public static Event<Boolean> ofFalse() {
    return ofResolution(Boolean.FALSE);
  }

  @NotNull
  public static <T> Event<T> ofIncident(@NotNull final Throwable obstacle) {
    return new IncidentEvent<T>(obstacle);
  }

  @NotNull
  public static <T> Event<T> ofNull() {
    return ofResolution(null);
  }

  @NotNull
  public static <T> Event<T> ofResolution(final T result) {
    Event<T> event;
    final Setting setting = Setting.get();
    if (result == null) {
      event = setting.get(NULL);
      if (event == null) {
        event = new ResolutionEvent<T>(null);
        setting.put(NULL, event);
      }

    } else {
      event = setting.get(result);
      if (event == null) {
        event = new ResolutionEvent<T>(result);
        setting.put(result, event);
      }
    }
    return event;
  }

  @NotNull
  public static Event<Boolean> ofTrue() {
    return ofResolution(Boolean.TRUE);
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

  public void observe(@Nullable final Observer<? super T> resolutionObserver,
      @Nullable final Observer<? super Throwable> incidentObserver) {
    observe(new DefaultEventObserver<T>(resolutionObserver, incidentObserver));
  }

  public void observe(@NotNull final EventObserver<? super T> eventObserver) {
    final Actor actor = BackStage.newActor(new EventObserverScript<T>(eventObserver));
    final String threadId = actor.getId();
    getActor().tell(GET, new Options().withReceiptId(threadId).withThread(threadId), actor);
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <E1 extends Throwable> Event<T> resolve(@NotNull final Class<? extends E1> firstType,
      @NotNull final UnaryFunction<? super E1, ? extends Event<T>> incidentHandler) {
    final HashSet<Class<? extends Throwable>> types = new HashSet<Class<? extends Throwable>>();
    types.add(firstType);
    return new ResolveEvent<T>(getActor(), types,
        (UnaryFunction<? super Throwable, ? extends Event<T>>) incidentHandler);
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <E extends Throwable> Event<T> resolve(
      @NotNull final Iterable<? extends Class<? extends E>> incidentTypes,
      @NotNull final UnaryFunction<? super E, ? extends Event<T>> incidentHandler) {
    return new ResolveEvent<T>(getActor(),
        Iterables.<Class<? extends Throwable>>toSet(incidentTypes),
        (UnaryFunction<? super Throwable, ? extends Event<T>>) incidentHandler);
  }

  @NotNull
  public <R> Event<R> then(
      @NotNull UnaryFunction<? super T, ? extends Event<R>> resolutionHandler) {
    return when(this, resolutionHandler);
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
    DefaultEventObserver(@Nullable final Observer<? super T> resolutionObserver,
        @Nullable final Observer<? super Throwable> incidentObserver) {
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
    private final String mInputThread;
    private final Object[] mInputs;
    private final Options mOptions;
    private final String mOutputThread;
    private final HashMap<String, Sender> mSenders = new HashMap<String, Sender>();
    private final Setting mSetting;

    private int mInputCount;
    private Actor mOutputActor;

    private AbstractEvent(final int numInputs) {
      mInputs = new Object[numInputs];
      final Setting setting = (mSetting = Setting.get());
      final String actorId = (mActor = BackStage.newActor(new PlayScript(setting) {

        @NotNull
        @Override
        public Behavior getBehavior(@NotNull final String id) {
          return new InitBehavior();
        }
      })).getId();
      mInputThread = actorId + ":input";
      mOutputThread = actorId + ":output";
      mOptions = new Options().withReceiptId(actorId);
    }

    @NotNull
    Actor getActor() {
      return mActor;
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

    @NotNull
    Setting getSetting() {
      return mSetting;
    }

    private void fail(@NotNull final Incident incident, @NotNull final Context context) {
      final Actor self = context.getSelf();
      for (final Sender sender : mSenders.values()) {
        sender.getSender().tell(incident, sender.getOptions(), self);
      }
      context.setBehavior(new IncidentBehavior(incident.getCause()));
    }

    private void incident(@NotNull final Incident incident, @NotNull final Context context) {
      try {
        final Actor incidentActor = getIncidentActor(incident);
        if (incidentActor != null) {
          (mOutputActor = incidentActor).tell(GET, mOptions.withThread(mOutputThread),
              context.getSelf());
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
    }

    private class InitBehavior extends AbstractBehavior {

      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          final Options options = envelop.getOptions().threadOnly();
          mSenders.put(options.getThread(), new Sender(envelop.getSender(), options));
          final Actor self = context.getSelf();
          final StringBuilder builder = new StringBuilder();
          for (final Actor actor : getInputActors()) {
            final String threadId = mInputThread + builder.append('#').toString();
            actor.tell(GET, mOptions.withThread(threadId), self);
          }
          context.setBehavior(new InputBehavior());

        } else if (message == CANCEL) {
          fail(new Incident(new PlotCancelledException()), context);
        }
        envelop.preventReceipt();
      }
    }

    private class InputBehavior extends AbstractBehavior {

      @SuppressWarnings("unchecked")
      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          // TODO: 25/01/2019 loop detection?
          final Options options = envelop.getOptions().threadOnly();
          mSenders.put(options.getThread(), new Sender(envelop.getSender(), options));

        } else if (message == CANCEL) {
          fail(new Incident(new PlotCancelledException()), context);

        } else {
          final String inputThread = mInputThread;
          final String thread = envelop.getOptions().getThread();
          if ((thread != null) && thread.startsWith(inputThread)) {
            if (message instanceof Incident) {
              incident((Incident) message, context);

            } else if (message instanceof Bounce) {
              final Incident incident = new Incident(PlotStateException.getOrNew((Bounce) message));
              incident(incident, context);

            } else if (!(message instanceof Receipt)) {
              final int index = thread.length() - inputThread.length() - 1;
              final Object[] inputs = mInputs;
              if ((index >= 0) && (index < inputs.length)) {
                inputs[index] = message;
                if (++mInputCount == inputs.length) {
                  try {
                    final Actor outputActor = getOutputActor(inputs);
                    if (outputActor != null) {
                      (mOutputActor = outputActor).tell(GET, mOptions.withThread(mOutputThread),
                          context.getSelf());
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
        envelop.preventReceipt();
      }
    }

    private class OutputBehavior extends AbstractBehavior {

      @SuppressWarnings("unchecked")
      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == CANCEL) {
          mOutputActor.tell(CANCEL, new Options().withThread(context.getSelf().getId()),
              context.getSelf());
          fail(new Incident(new PlotCancelledException()), context);

        } else if (mOutputThread.equals(envelop.getOptions().getThread())) {
          if (message instanceof Incident) {
            fail((Incident) message, context);

          } else if (message instanceof Bounce) {
            final Throwable obstacle = PlotStateException.getOrNew((Bounce) message);
            fail(new Incident(obstacle), context);

          } else if (!(message instanceof Receipt)) {
            final Actor self = context.getSelf();
            for (final Sender sender : mSenders.values()) {
              sender.getSender().tell(message, sender.getOptions(), self);
            }
            context.setBehavior(new ResolutionBehavior(message));
          }
        }
        envelop.preventReceipt();
      }
    }
  }

  private static class GenericEvent<T, R> extends AbstractEvent<R> {

    private final List<Actor> mActors;
    private final UnaryFunction<? super List<T>, ? extends Event<R>> mResolutionHandler;

    private GenericEvent(@NotNull final Iterable<? extends Event<? extends T>> events,
        @NotNull final UnaryFunction<? super List<T>, ? extends Event<R>> resolutionHandler) {
      super(Iterables.size(events));
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
      Setting.set(getSetting());
      try {
        final ArrayList<T> inputList = new ArrayList<T>();
        for (final Object input : inputs) {
          inputList.add((T) input);
        }
        final Event<R> event = mResolutionHandler.call(inputList);
        return ((event != null) ? event : ofNull()).getActor();

      } finally {
        Setting.unset();
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
      envelop.preventReceipt();
    }
  }

  private static class IncidentEvent<T> extends Event<T> {

    private final Actor mActor;

    private IncidentEvent(@NotNull final Throwable obstacle) {
      ConstantConditions.notNull("obstacle", obstacle);
      mActor = BackStage.newActor(new TrampolinePlayScript(Setting.get()) {

        @NotNull
        @Override
        public Behavior getBehavior(@NotNull final String id) {
          return new IncidentBehavior(obstacle);
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
      envelop.preventReceipt();
    }
  }

  private static class ResolutionEvent<T> extends Event<T> {

    private final Actor mActor;

    private ResolutionEvent(final T result) {
      mActor = BackStage.newActor(new TrampolinePlayScript(Setting.get()) {

        @NotNull
        @Override
        public Behavior getBehavior(@NotNull final String id) {
          return new ResolutionBehavior(result);
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
          Setting.set(getSetting());
          try {
            final Event<T> event = mIncidentHandler.call(obstacle);
            return ((event != null) ? event : ofNull()).getActor();

          } finally {
            Setting.unset();
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

  private static class Sender {

    private final Options mOptions;
    private final Actor mSender;

    private Sender(@NotNull final Actor sender, @NotNull final Options options) {
      mSender = sender;
      mOptions = options;
    }

    @NotNull
    Options getOptions() {
      return mOptions;
    }

    @NotNull
    Actor getSender() {
      return mSender;
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
      Setting.set(getSetting());
      try {
        final Event<R> event = mResolutionHandler.call((T1) inputs[0]);
        return ((event != null) ? event : ofNull()).getActor();

      } finally {
        Setting.unset();
      }
    }
  }
}
