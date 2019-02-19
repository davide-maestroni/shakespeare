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
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import dm.shakespeare.BackStage;
import dm.shakespeare.actor.AbstractBehavior;
import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Behavior.Context;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.Options;
import dm.shakespeare.function.Observer;
import dm.shakespeare.message.Bounce;
import dm.shakespeare.plot.Setting.Cache;
import dm.shakespeare.plot.function.Action;
import dm.shakespeare.plot.function.NullaryFunction;
import dm.shakespeare.plot.function.UnaryFunction;
import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare.util.Iterables;

import static dm.shakespeare.plot.Narrator.NULL;
import static dm.shakespeare.plot.Narrator.STOP;

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

  @NotNull
  public static <T> Event<T> ofEffect(final T effect) {
    Event<T> event;
    final Cache cache = Setting.get().getCache(Event.class);
    if (effect == null) {
      event = cache.get(NULL);
      if (event == null) {
        event = new EffectEvent<T>(null);
        cache.put(NULL, event);
      }

    } else {
      event = cache.get(effect);
      if (event == null) {
        event = new EffectEvent<T>(effect);
        cache.put(effect, event);
      }
    }
    return event;
  }

  @NotNull
  public static <T> Event<T> ofEvent(
      @NotNull final NullaryFunction<? extends Event<T>> eventCreator) {
    return new FunctionEvent<T>(eventCreator);
  }

  @NotNull
  public static Event<Boolean> ofFalse() {
    return ofEffect(Boolean.FALSE);
  }

  @NotNull
  public static <T> Event<T> ofIncident(@NotNull final Throwable incident) {
    return new IncidentEvent<T>(incident);
  }

  @NotNull
  public static <T> Event<T> ofNarration(@NotNull final Narrator<T> eventNarrator) {
    return new NarratorEvent<T>(eventNarrator);
  }

  @NotNull
  public static <T> Event<T> ofNarration(@NotNull final NullaryFunction<T> effectCreator) {
    ConstantConditions.notNull("effectCreator", effectCreator);
    return ofEvent(new NullaryFunction<Event<T>>() {

      public Event<T> call() throws Exception {
        return Event.ofEffect(effectCreator.call());
      }
    });
  }

  @NotNull
  public static <T> Event<T> ofNull() {
    return ofEffect(null);
  }

  @NotNull
  public static Event<Boolean> ofTrue() {
    return ofEffect(Boolean.TRUE);
  }

  @NotNull
  public static <T1, R> Event<R> when(@NotNull final Event<? extends T1> firstEvent,
      @NotNull final UnaryFunction<? super T1, ? extends Event<R>> effectHandler) {
    return new UnaryEvent<T1, R>(firstEvent, effectHandler);
  }

  @NotNull
  public static <T, R> Event<R> when(@NotNull final Iterable<? extends Event<? extends T>> events,
      @NotNull final UnaryFunction<? super List<T>, ? extends Event<R>> effectHandler) {
    return new GenericEvent<T, R>(events, effectHandler);
  }

  static boolean isFalse(@Nullable final Event<?> event) {
    final Cache cache = Setting.get().getCache(Event.class);
    return cache.get(Boolean.FALSE) == event;
  }

  static boolean isNull(@Nullable final Event<?> event) {
    final Cache cache = Setting.get().getCache(Event.class);
    return cache.get(NULL) == event;
  }

  static boolean isTrue(@Nullable final Event<?> event) {
    final Cache cache = Setting.get().getCache(Event.class);
    return cache.get(Boolean.TRUE) == event;
  }

  @SuppressWarnings("ConstantConditions")
  private static boolean isSameThread(@Nullable final String expectedThread,
      @Nullable final String actualThread) {
    return expectedThread.equals(actualThread);
  }

  public void cancel() {
    getActor().tell(CANCEL, null, BackStage.standIn());
  }

  @NotNull
  public Event<T> eventually(@NotNull final Action eventualAction) {
    return new EventualEvent<T>(this, eventualAction);
  }

  public void play(@NotNull final EventObserver<? super T> eventObserver) {
    final Actor actor = BackStage.newActor(new EventObserverScript<T>(eventObserver));
    final String threadId = actor.getId();
    getActor().tell(GET, new Options().withReceiptId(threadId).withThread(threadId), actor);
  }

  public void play(@Nullable final Observer<? super T> effectObserver,
      @Nullable final Observer<? super Throwable> incidentObserver) {
    play(new DefaultEventObserver<T>(effectObserver, incidentObserver));
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <E1 extends Throwable> Event<T> resolve(@NotNull final Class<? extends E1> firstType,
      @NotNull final UnaryFunction<? super E1, ? extends Event<T>> incidentObserver) {
    final HashSet<Class<? extends Throwable>> types = new HashSet<Class<? extends Throwable>>();
    types.add(firstType);
    return new ResolveEvent<T>(this, types,
        (UnaryFunction<? super Throwable, ? extends Event<T>>) incidentObserver);
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <E extends Throwable> Event<T> resolve(
      @NotNull final Iterable<? extends Class<? extends E>> incidentTypes,
      @NotNull final UnaryFunction<? super E, ? extends Event<T>> incidentObserver) {
    return new ResolveEvent<T>(this, Iterables.<Class<? extends Throwable>>toSet(incidentTypes),
        (UnaryFunction<? super Throwable, ? extends Event<T>>) incidentObserver);
  }

  @NotNull
  public Event<T> scheduleWithDelay(final long delay, @NotNull final TimeUnit unit) {
    return new ScheduleWithDelayEvent<T>(this, delay, unit);
  }

  @NotNull
  public <R> Event<R> then(
      @NotNull final UnaryFunction<? super T, ? extends Event<R>> effectHandler) {
    return when(this, effectHandler);
  }

  @NotNull
  public Event<T> thenWatch(@NotNull final EventObserver<? super T> effectHandler) {
    return new WatchEvent<T>(this, effectHandler);
  }

  @NotNull
  public Event<T> thenWatch(@Nullable final Observer<? super T> effectObserver,
      @Nullable final Observer<? super Throwable> incidentObserver) {
    return thenWatch(new DefaultEventObserver<T>(effectObserver, incidentObserver));
  }

  @NotNull
  abstract Actor getActor();

  public interface EventObserver<T> {

    void onEffect(T effect) throws Exception;

    void onIncident(@NotNull Throwable incident) throws Exception;
  }

  static class DefaultEventObserver<T> implements EventObserver<T> {

    private final Observer<Object> mEffectObserver;
    private final Observer<Object> mIncidentObserver;

    @SuppressWarnings("unchecked")
    DefaultEventObserver(@Nullable final Observer<? super T> effectObserver,
        @Nullable final Observer<? super Throwable> incidentObserver) {
      mEffectObserver = (Observer<Object>) ((effectObserver != null) ? effectObserver : NO_OP);
      mIncidentObserver =
          (Observer<Object>) ((incidentObserver != null) ? incidentObserver : NO_OP);
    }

    public void onEffect(final T effect) throws Exception {
      mEffectObserver.accept(effect);
    }

    public void onIncident(@NotNull final Throwable incident) throws Exception {
      mIncidentObserver.accept(incident);
    }
  }

  private abstract static class AbstractEvent<T> extends Event<T> {

    private final Actor mActor;
    private final HashMap<Actor, Options> mInputActors = new HashMap<Actor, Options>();
    private final String mInputThread;
    private final Object[] mInputs;
    private final Options mOptions;
    private final Options mOutputOptions;
    private final Setting mSetting;

    private Conflict mConflict;
    private int mInputCount;
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
      final Options options = (mOptions = new Options().withReceiptId(actorId));
      mInputThread = actorId + ":input";
      mOutputOptions = options.withThread(actorId + ":output");
    }

    void endAction() throws Exception {
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
          conflictActor.tell(GET, mOutputOptions, context.getSelf());
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

    private void done(Object message, @NotNull final Context context) {
      try {
        endAction();

      } catch (final Throwable t) {
        message = new Conflict(t);
        if (t instanceof InterruptedException) {
          Thread.currentThread().interrupt();
        }
      }
      final Actor self = context.getSelf();
      for (final Sender sender : mSenders.values()) {
        sender.getSender().tell(message, sender.getOptions(), self);
      }
      mSenders = null;
      context.setBehavior(new DoneBehavior(message));
    }

    private void fail(@NotNull Conflict conflict, @NotNull final Context context) {
      try {
        endAction();

      } catch (final Throwable t) {
        conflict = new Conflict(t);
        if (t instanceof InterruptedException) {
          Thread.currentThread().interrupt();
        }
      }
      final Actor self = context.getSelf();
      for (final Sender sender : mSenders.values()) {
        sender.getSender().tell(conflict, sender.getOptions(), self);
      }
      mSenders = null;
      context.setBehavior(new DoneBehavior(conflict));
    }

    private void tellInputActors(final Object message, @NotNull final Context context) {
      final Actor self = context.getSelf();
      final HashMap<Actor, Options> inputActors = mInputActors;
      if (inputActors.isEmpty()) {
        @SuppressWarnings("UnnecessaryLocalVariable") final Options options = mOptions;
        final StringBuilder builder = new StringBuilder();
        for (final Actor actor : getInputActors()) {
          final String threadId = mInputThread + builder.append('#').toString();
          final Options inputOptions = options.withThread(threadId);
          inputActors.put(actor, inputOptions);
          actor.tell(message, inputOptions, self);
        }

      } else {
        for (final Entry<Actor, Options> entry : inputActors.entrySet()) {
          entry.getKey().tell(message, entry.getValue(), self);
        }
      }
    }

    private class CancelBehavior extends AbstractBehavior {

      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          final Options options = envelop.getOptions().threadOnly();
          mSenders.put(options.getThread(), new Sender(envelop.getSender(), options));

        } else {
          final Options outputOptions = mOutputOptions;
          if (isSameThread(outputOptions.getThread(), envelop.getOptions().getThread())) {
            fail(Conflict.ofCancel(), context);

          } else {
            final String thread = envelop.getOptions().getThread();
            if ((thread != null) && thread.startsWith(mInputThread)) {
              if (++mInputCount == mInputs.length) {
                final Conflict conflict = Conflict.ofCancel();
                try {
                  final Actor conflictActor = getConflictActor(conflict);
                  if (conflictActor != null) {
                    conflictActor.tell(CANCEL, outputOptions, context.getSelf());
                  }
                  fail(conflict, context);

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
    }

    private class InitBehavior extends AbstractBehavior {

      public void onMessage(Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          final Options options = envelop.getOptions().threadOnly();
          mSenders.put(options.getThread(), new Sender(envelop.getSender(), options));
          tellInputActors(GET, context);
          context.setBehavior(new InputBehavior());

        } else if (message == CANCEL) {
          tellInputActors(CANCEL, context);
          fail(Conflict.ofCancel(), context);
        }
        envelop.preventReceipt();
      }
    }

    private class InputBehavior extends AbstractBehavior {

      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          // TODO: 25/01/2019 loop detection?
          final Options options = envelop.getOptions().threadOnly();
          mSenders.put(options.getThread(), new Sender(envelop.getSender(), options));

        } else if (message == CANCEL) {
          tellInputActors(CANCEL, context);
          context.setBehavior(new CancelBehavior());

        } else {
          final String inputThread = mInputThread;
          final String thread = envelop.getOptions().getThread();
          if ((thread != null) && thread.startsWith(inputThread)) {
            if (message instanceof Conflict) {
              if (mConflict == null) {
                mConflict = (Conflict) message;
              }

              if (++mInputCount == mInputs.length) {
                conflict((Conflict) message, context);
              }

            } else if (message instanceof Bounce) {
              final Conflict conflict = Conflict.ofBounce((Bounce) message);
              if (mConflict == null) {
                mConflict = conflict;
              }

              if (++mInputCount == mInputs.length) {
                conflict(conflict, context);
              }

            } else {
              final int index = thread.length() - inputThread.length() - 1;
              final Object[] inputs = mInputs;
              if ((index >= 0) && (index < inputs.length)) {
                inputs[index] = message;
                if (++mInputCount == inputs.length) {
                  if (mConflict != null) {
                    conflict(mConflict, context);

                  } else {
                    try {
                      final Actor outputActor = getOutputActor(inputs);
                      if (outputActor != null) {
                        outputActor.tell(GET, mOutputOptions, context.getSelf());
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

              } else {
                conflict(Conflict.ofCancel(), context);
              }
            }
          }
        }
        envelop.preventReceipt();
      }
    }

    private class OutputBehavior extends AbstractBehavior {

      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          final Options options = envelop.getOptions().threadOnly();
          mSenders.put(options.getThread(), new Sender(envelop.getSender(), options));

        } else if (message == CANCEL) {
          tellInputActors(CANCEL, context);
          context.setBehavior(new CancelBehavior());

        } else {
          if (isSameThread(mOutputOptions.getThread(), envelop.getOptions().getThread())) {
            if (message instanceof Conflict) {
              fail((Conflict) message, context);

            } else if (message instanceof Bounce) {
              fail(Conflict.ofBounce((Bounce) message), context);

            } else {
              done(message, context);
            }
          }
        }
        envelop.preventReceipt();
      }
    }
  }

  private static class DoneBehavior extends AbstractBehavior {

    private final Object mEffect;

    private DoneBehavior(final Object effect) {
      mEffect = effect;
    }

    public void onMessage(final Object message, @NotNull final Envelop envelop,
        @NotNull final Context context) {
      if (message == GET) {
        envelop.getSender().tell(mEffect, envelop.getOptions().threadOnly(), context.getSelf());
      }
      envelop.preventReceipt();
    }
  }

  private static class EffectEvent<T> extends Event<T> {

    private final Actor mActor;

    private EffectEvent(final T effect) {
      mActor = BackStage.newActor(new TrampolinePlayScript(Setting.get()) {

        @NotNull
        @Override
        public Behavior getBehavior(@NotNull final String id) {
          return new DoneBehavior(effect);
        }
      });
    }

    @NotNull
    Actor getActor() {
      return mActor;
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

    @Override
    void endAction() throws Exception {
      Setting.set(getSetting());
      try {
        mEventualAction.run();

      } finally {
        Setting.unset();
      }
    }

    @NotNull
    List<Actor> getInputActors() {
      return mActors;
    }
  }

  private static class FunctionEvent<T> extends AbstractEvent<T> {

    private final NullaryFunction<? extends Event<T>> mEventCreator;

    private List<Actor> mActors;

    private FunctionEvent(@NotNull final NullaryFunction<? extends Event<T>> eventCreator) {
      super(1);
      mEventCreator = ConstantConditions.notNull("eventCreator", eventCreator);
    }

    @NotNull
    List<Actor> getInputActors() {
      if (mActors == null) {
        Setting.set(getSetting());
        Event<T> event;
        try {
          event = mEventCreator.call();
          if (event == null) {
            event = ofNull();
          }

        } catch (final Throwable t) {
          event = ofIncident(t);
          if (t instanceof InterruptedException) {
            Thread.currentThread().interrupt();
          }

        } finally {
          Setting.unset();
        }
        mActors = Collections.singletonList(event.getActor());
      }
      return mActors;
    }
  }

  private static class GenericEvent<T, R> extends AbstractEvent<R> {

    private final List<Actor> mActors;
    private final UnaryFunction<? super List<T>, ? extends Event<R>> mEffectHandler;

    private GenericEvent(@NotNull final Iterable<? extends Event<? extends T>> events,
        @NotNull final UnaryFunction<? super List<T>, ? extends Event<R>> effectHandler) {
      super(Iterables.size(events));
      final ArrayList<Actor> actors = new ArrayList<Actor>();
      for (final Event<? extends T> event : events) {
        actors.add(event.getActor());
      }
      mActors = actors;
      mEffectHandler = ConstantConditions.notNull("effectHandler", effectHandler);
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
        final Event<R> event = mEffectHandler.call(inputList);
        return ((event != null) ? event : ofNull()).getActor();

      } finally {
        Setting.unset();
      }
    }
  }

  private static class IncidentEvent<T> extends Event<T> {

    private final Actor mActor;

    private IncidentEvent(@NotNull final Throwable incident) {
      final Conflict conflict = new Conflict(incident);
      mActor = BackStage.newActor(new TrampolinePlayScript(Setting.get()) {

        @NotNull
        @Override
        public Behavior getBehavior(@NotNull final String id) {
          return new DoneBehavior(conflict);
        }
      });
    }

    @NotNull
    Actor getActor() {
      return mActor;
    }
  }

  private static class NarratorEvent<T> extends Event<T> {

    private final Actor mActor;
    private final Narrator<T> mEventNarrator;

    private HashMap<String, Sender> mSenders = new HashMap<String, Sender>();

    private NarratorEvent(@NotNull final Narrator<T> eventNarrator) {
      mEventNarrator = ConstantConditions.notNull("eventNarrator", eventNarrator);
      mActor = BackStage.newActor(new PlayScript(Setting.get()) {

        @NotNull
        @Override
        public Behavior getBehavior(@NotNull final String id) {
          return new InitBehavior();
        }
      });
    }

    private class InitBehavior extends AbstractBehavior {

      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        final Narrator<T> eventNarrator = mEventNarrator;
        if (message == GET) {
          eventNarrator.setActor(context.getSelf());
          Object effect = eventNarrator.takeEffect();
          if (effect != null) {
            eventNarrator.close();
            if ((effect == NULL) || (effect == STOP)) {
              effect = null;
            }
            context.setBehavior(new DoneBehavior(effect));

          } else {
            final Options options = envelop.getOptions().threadOnly();
            mSenders.put(options.getThread(), new Sender(envelop.getSender(), options));
            context.setBehavior(new InputBehavior());
          }

        } else if (message == CANCEL) {
          final Conflict conflict = Conflict.ofCancel();
          eventNarrator.cancel(conflict.getCause());
          context.setBehavior(new DoneBehavior(conflict));
        }
        envelop.preventReceipt();
      }
    }

    private class InputBehavior extends AbstractBehavior {

      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          final Options options = envelop.getOptions().threadOnly();
          mSenders.put(options.getThread(), new Sender(envelop.getSender(), options));

        } else if (message == CANCEL) {
          final Conflict conflict = Conflict.ofCancel();
          mEventNarrator.cancel(conflict.getCause());
          context.setBehavior(new DoneBehavior(conflict));

        } else if (message == Narrator.AVAILABLE) {
          final Narrator<T> eventNarrator = mEventNarrator;
          Object effect = eventNarrator.takeEffect();
          if (effect != null) {
            eventNarrator.close();
            if ((effect == NULL) || (effect == STOP)) {
              effect = null;
            }
            final Actor self = context.getSelf();
            for (final Sender sender : mSenders.values()) {
              sender.getSender().tell(effect, sender.getOptions(), self);
            }
            mSenders = null;
            context.setBehavior(new DoneBehavior(effect));
          }
        }
        envelop.preventReceipt();
      }
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

    private ResolveEvent(@NotNull final Event<? extends T> event,
        @NotNull final Set<Class<? extends Throwable>> incidentTypes,
        @NotNull final UnaryFunction<? super Throwable, ? extends Event<T>> incidentHandler) {
      super(1);
      mActors = Collections.singletonList(event.getActor());
      mIncidentTypes = ConstantConditions.notNullElements("incidentTypes", incidentTypes);
      mIncidentHandler = ConstantConditions.notNull("incidentHandler", incidentHandler);
    }

    @Nullable
    @Override
    Actor getConflictActor(@NotNull final Conflict conflict) throws Exception {
      final Throwable incident = conflict.getCause();
      for (final Class<? extends Throwable> incidentType : mIncidentTypes) {
        if (incidentType.isInstance(incident)) {
          Setting.set(getSetting());
          try {
            final Event<T> event = mIncidentHandler.call(incident);
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

  private static class ScheduleWithDelayEvent<T> extends Event<T> implements Runnable {

    private final Actor mActor;
    private final long mDelay;
    private final Actor mInputActor;
    private final Options mInputOptions;
    private final TimeUnit mUnit;

    private boolean mInputPending;
    private ScheduledFuture<?> mScheduledFuture;
    private HashMap<String, Sender> mSenders = new HashMap<String, Sender>();

    private ScheduleWithDelayEvent(@NotNull final Event<? extends T> event, final long delay,
        @NotNull final TimeUnit unit) {
      mInputActor = event.getActor();
      mDelay = ConstantConditions.positive("delay", delay);
      mUnit = ConstantConditions.notNull("unit", unit);
      final String actorId = (mActor = BackStage.newActor(new PlayScript(Setting.get()) {

        @NotNull
        @Override
        public Behavior getBehavior(@NotNull final String id) {
          return new InitBehavior();
        }
      })).getId();
      mInputOptions = new Options().withReceiptId(actorId).withThread(actorId + ":input");
    }

    public void run() {
      mInputPending = true;
      mInputActor.tell(GET, mInputOptions, mActor);
    }

    private void done(Object message, @NotNull final Context context) {
      final ScheduledFuture<?> scheduledFuture = mScheduledFuture;
      if (scheduledFuture != null) {
        scheduledFuture.cancel(false);
      }
      final Actor self = context.getSelf();
      for (final Sender sender : mSenders.values()) {
        sender.getSender().tell(message, sender.getOptions(), self);
      }
      mSenders = null;
      context.setBehavior(new DoneBehavior(message));
    }

    private void fail(@NotNull final Conflict conflict, @NotNull final Context context) {
      final ScheduledFuture<?> scheduledFuture = mScheduledFuture;
      if (scheduledFuture != null) {
        scheduledFuture.cancel(false);
      }
      final Actor self = context.getSelf();
      for (final Sender sender : mSenders.values()) {
        sender.getSender().tell(conflict, sender.getOptions(), self);
      }
      mSenders = null;
      context.setBehavior(new DoneBehavior(conflict));
    }

    private class CancelBehavior extends AbstractBehavior {

      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          final Options options = envelop.getOptions().threadOnly();
          mSenders.put(options.getThread(), new Sender(envelop.getSender(), options));

        } else if (isSameThread(mInputOptions.getThread(), envelop.getOptions().getThread())) {
          fail(Conflict.ofCancel(), context);
        }
      }
    }

    private class InitBehavior extends AbstractBehavior {

      public void onMessage(Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          final Options options = envelop.getOptions().threadOnly();
          mSenders.put(options.getThread(), new Sender(envelop.getSender(), options));
          mScheduledFuture =
              context.getScheduledExecutor().schedule(ScheduleWithDelayEvent.this, mDelay, mUnit);
          context.setBehavior(new InputBehavior());

        } else if (message == CANCEL) {
          mInputActor.tell(CANCEL, mInputOptions, mActor);
          fail(Conflict.ofCancel(), context);
        }
        envelop.preventReceipt();
      }
    }

    private class InputBehavior extends AbstractBehavior {

      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          final Options options = envelop.getOptions().threadOnly();
          mSenders.put(options.getThread(), new Sender(envelop.getSender(), options));

        } else if (message == CANCEL) {
          mInputActor.tell(CANCEL, mInputOptions, mActor);
          if (mInputPending) {
            context.setBehavior(new CancelBehavior());

          } else {
            fail(Conflict.ofCancel(), context);
          }

        } else if (isSameThread(mInputOptions.getThread(), envelop.getOptions().getThread())) {
          if (message instanceof Conflict) {
            fail((Conflict) message, context);

          } else if (message instanceof Bounce) {
            fail(Conflict.ofBounce((Bounce) message), context);

          } else {
            done(message, context);
          }
        }
        envelop.preventReceipt();
      }
    }

    @NotNull
    Actor getActor() {
      return mActor;
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
    private final UnaryFunction<? super T1, ? extends Event<R>> mEffectHandler;

    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    private UnaryEvent(@NotNull final Event<? extends T1> firstEvent,
        @NotNull final UnaryFunction<? super T1, ? extends Event<R>> effectHandler) {
      super(1);
      mActors = Arrays.asList(firstEvent.getActor());
      mEffectHandler = ConstantConditions.notNull("effectHandler", effectHandler);
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
        final Event<R> event = mEffectHandler.call((T1) inputs[0]);
        return ((event != null) ? event : ofNull()).getActor();

      } finally {
        Setting.unset();
      }
    }
  }

  private static class WatchEvent<T> extends AbstractEvent<T> {

    private final List<Actor> mActors;
    private final EventObserver<? super T> mEventObserver;

    private WatchEvent(@NotNull final Event<? extends T> event,
        @NotNull final EventObserver<? super T> eventObserver) {
      super(1);
      mActors = Collections.singletonList(event.getActor());
      mEventObserver = ConstantConditions.notNull("eventObserver", eventObserver);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    Actor getOutputActor(@NotNull final Object[] inputs) throws Exception {
      Setting.set(getSetting());
      try {
        mEventObserver.onEffect((T) inputs[0]);

      } finally {
        Setting.unset();
      }
      return super.getOutputActor(inputs);
    }

    @Nullable
    @Override
    Actor getConflictActor(@NotNull final Conflict conflict) throws Exception {
      Setting.set(getSetting());
      try {
        mEventObserver.onIncident(conflict.getCause());

      } finally {
        Setting.unset();
      }
      return super.getConflictActor(conflict);
    }

    @NotNull
    List<Actor> getInputActors() {
      return mActors;
    }
  }
}
