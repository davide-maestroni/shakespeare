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
import dm.shakespeare.plot.Setting.Cache;
import dm.shakespeare.plot.function.Action;
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
  public static <T> Event<T> ofConflict(@NotNull final Throwable incident) {
    return new ConflictEvent<T>(incident);
  }

  @NotNull
  public static <T> Event<T> ofEvent(@NotNull final Event<T> event) {
    return new EventEvent<T>(event);
  }

  @NotNull
  public static Event<Boolean> ofFalse() {
    return ofResolution(Boolean.FALSE);
  }

  @NotNull
  public static <T> Event<T> ofNull() {
    return ofResolution(null);
  }

  @NotNull
  public static <T> Event<T> ofResolution(final T result) {
    Event<T> event;
    final Cache cache = Setting.get().getCache(Event.class);
    if (result == null) {
      event = cache.get(NULL);
      if (event == null) {
        event = new ResolutionEvent<T>(null);
        cache.put(NULL, event);
      }

    } else {
      event = cache.get(result);
      if (event == null) {
        event = new ResolutionEvent<T>(result);
        cache.put(result, event);
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

  @NotNull
  public Event<T> eventually(@NotNull final Action eventualAction) {
    return new EventualEvent<T>(this, eventualAction);
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <E1 extends Throwable> Event<T> resolve(@NotNull final Class<? extends E1> firstType,
      @NotNull final UnaryFunction<? super E1, ? extends Event<T>> conflictHandler) {
    final HashSet<Class<? extends Throwable>> types = new HashSet<Class<? extends Throwable>>();
    types.add(firstType);
    return new ResolveEvent<T>(getActor(), types,
        (UnaryFunction<? super Throwable, ? extends Event<T>>) conflictHandler);
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <E extends Throwable> Event<T> resolve(
      @NotNull final Iterable<? extends Class<? extends E>> conflictTypes,
      @NotNull final UnaryFunction<? super E, ? extends Event<T>> conflictHandler) {
    return new ResolveEvent<T>(getActor(),
        Iterables.<Class<? extends Throwable>>toSet(conflictTypes),
        (UnaryFunction<? super Throwable, ? extends Event<T>>) conflictHandler);
  }

  @NotNull
  public <R> Event<R> then(
      @NotNull final UnaryFunction<? super T, ? extends Event<R>> resolutionHandler) {
    return when(this, resolutionHandler);
  }

  public void watch(@NotNull final EventObserver<? super T> eventObserver) {
    final Actor actor = BackStage.newActor(new EventObserverScript<T>(eventObserver));
    final String threadId = actor.getId();
    getActor().tell(GET, new Options().withReceiptId(threadId).withThread(threadId), actor);
  }

  public void watch(@Nullable final Observer<? super T> resolutionObserver,
      @Nullable final Observer<? super Throwable> conflictObserver) {
    watch(new DefaultEventObserver<T>(resolutionObserver, conflictObserver));
  }

  @NotNull
  abstract Actor getActor();

  public interface EventObserver<T> {

    void onConflict(@NotNull Throwable incident) throws Exception;

    void onResolution(T result) throws Exception;
  }

  static class DefaultEventObserver<T> implements EventObserver<T> {

    private final Observer<Object> mConflictObserver;
    private final Observer<Object> mResolutionObserver;

    @SuppressWarnings("unchecked")
    DefaultEventObserver(@Nullable final Observer<? super T> resolutionObserver,
        @Nullable final Observer<? super Throwable> conflictObserver) {
      mResolutionObserver =
          (Observer<Object>) ((resolutionObserver != null) ? resolutionObserver : NO_OP);
      mConflictObserver =
          (Observer<Object>) ((conflictObserver != null) ? conflictObserver : NO_OP);
    }

    public void onConflict(@NotNull final Throwable incident) throws Exception {
      mConflictObserver.accept(incident);
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
    private final Setting mSetting;

    private int mInputCount;
    private Actor mOutputActor;
    private HashMap<String, Sender> mSenders = new HashMap<String, Sender>();

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
    Actor getConflictActor(@NotNull final Conflict conflict) throws Exception {
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

    private void conflict(@NotNull final Conflict conflict, @NotNull final Context context) {
      try {
        final Actor conflictActor = getConflictActor(conflict);
        if (conflictActor != null) {
          (mOutputActor = conflictActor).tell(GET, mOptions.withThread(mOutputThread),
              context.getSelf());
          context.setBehavior(new OutputBehavior());

        } else {
          fail(conflict, context);
        }

      } catch (final Throwable t) {
        fail(new Conflict(t), context);
        if (t instanceof InterruptedException) {
          Thread.currentThread().interrupt();
        }
      }
    }

    private void done(final Object message, @NotNull final Context context) {
      final Actor self = context.getSelf();
      for (final Sender sender : mSenders.values()) {
        sender.getSender().tell(message, sender.getOptions(), self);
      }
      mSenders = null;
      context.setBehavior(new ResolutionBehavior(message));
    }

    private void fail(@NotNull final Conflict conflict, @NotNull final Context context) {
      final Actor self = context.getSelf();
      for (final Sender sender : mSenders.values()) {
        sender.getSender().tell(conflict, sender.getOptions(), self);
      }
      mSenders = null;
      context.setBehavior(new ConflictBehavior(conflict.getCause()));
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
          fail(new Conflict(new PlotCancelledException()), context);
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
          fail(new Conflict(new PlotCancelledException()), context);

        } else {
          final String inputThread = mInputThread;
          final String thread = envelop.getOptions().getThread();
          if ((thread != null) && thread.startsWith(inputThread)) {
            if (message instanceof Conflict) {
              conflict((Conflict) message, context);

            } else if (message instanceof Bounce) {
              final Conflict conflict = new Conflict(PlotStateException.getOrNew((Bounce) message));
              conflict(conflict, context);

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
                      done(message, context);
                    }

                  } catch (final Throwable t) {
                    fail(new Conflict(t), context);
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
          fail(new Conflict(new PlotCancelledException()), context);

        } else if (mOutputThread.equals(envelop.getOptions().getThread())) {
          if (message instanceof Conflict) {
            fail((Conflict) message, context);

          } else if (message instanceof Bounce) {
            final Throwable incident = PlotStateException.getOrNew((Bounce) message);
            fail(new Conflict(incident), context);

          } else if (!(message instanceof Receipt)) {
            done(message, context);
          }
        }
        envelop.preventReceipt();
      }
    }
  }

  private static class ConflictBehavior extends AbstractBehavior {

    private final Conflict mConflict;

    private ConflictBehavior(@NotNull final Throwable incident) {
      mConflict = new Conflict(incident);
    }

    public void onMessage(final Object message, @NotNull final Envelop envelop,
        @NotNull final Context context) {
      if (message == GET) {
        envelop.getSender().tell(mConflict, envelop.getOptions().threadOnly(), context.getSelf());
      }
      envelop.preventReceipt();
    }
  }

  private static class ConflictEvent<T> extends Event<T> {

    private final Actor mActor;

    private ConflictEvent(@NotNull final Throwable incident) {
      ConstantConditions.notNull("incident", incident);
      mActor = BackStage.newActor(new TrampolinePlayScript(Setting.get()) {

        @NotNull
        @Override
        public Behavior getBehavior(@NotNull final String id) {
          return new ConflictBehavior(incident);
        }
      });
    }

    @NotNull
    Actor getActor() {
      return mActor;
    }
  }

  private static class EventEvent<T> extends AbstractEvent<T> {

    private final List<Actor> mActors;

    private EventEvent(@NotNull final Event<T> event) {
      super(1);
      mActors = Collections.singletonList(event.getActor());
    }

    @NotNull
    List<Actor> getInputActors() {
      return mActors;
    }
  }

  private static class EventualEvent<T> extends AbstractEvent<T> {

    private final List<Actor> mActors;
    private final Action mEventualAction;

    private EventualEvent(@NotNull final Event<T> event, @NotNull final Action eventualAction) {
      super(1);
      mActors = Collections.singletonList(event.getActor());
      mEventualAction = ConstantConditions.notNull("eventualAction", eventualAction);
    }

    @Nullable
    @Override
    Actor getConflictActor(@NotNull final Conflict conflict) throws Exception {
      Setting.set(getSetting());
      try {
        mEventualAction.run();
        return ofConflict(conflict.getCause()).getActor();

      } finally {
        Setting.unset();
      }
    }

    @NotNull
    List<Actor> getInputActors() {
      return mActors;
    }

    @Nullable
    @Override
    Actor getOutputActor(@NotNull final Object[] inputs) throws Exception {
      Setting.set(getSetting());
      try {
        mEventualAction.run();
        return ofResolution(inputs[0]).getActor();

      } finally {
        Setting.unset();
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
    private final UnaryFunction<? super Throwable, ? extends Event<T>> mConflictHandler;
    private final Set<Class<? extends Throwable>> mConflictTypes;

    private ResolveEvent(@NotNull final Actor eventActor,
        @NotNull final Set<Class<? extends Throwable>> conflictTypes,
        @NotNull final UnaryFunction<? super Throwable, ? extends Event<T>> conflictHandler) {
      super(1);
      mActors = Collections.singletonList(eventActor);
      mConflictTypes = ConstantConditions.notNullElements("conflictTypes", conflictTypes);
      mConflictHandler = ConstantConditions.notNull("conflictHandler", conflictHandler);
    }

    @Nullable
    @Override
    Actor getConflictActor(@NotNull final Conflict conflict) throws Exception {
      final Throwable incident = conflict.getCause();
      for (final Class<? extends Throwable> conflictType : mConflictTypes) {
        if (conflictType.isInstance(incident)) {
          Setting.set(getSetting());
          try {
            final Event<T> event = mConflictHandler.call(incident);
            return ((event != null) ? event : ofNull()).getActor();

          } finally {
            Setting.unset();
          }
        }
      }
      return super.getConflictActor(conflict);
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
