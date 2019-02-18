package dm.shakespeare.plot;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import dm.shakespeare.plot.memory.ListMemory;
import dm.shakespeare.plot.memory.SingletonMemory;
import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare.util.Iterables;

/**
 * Created by davide-maestroni on 01/22/2019.
 */
public abstract class Story<T> extends Event<Iterable<T>> {

  // TODO: 05/02/2019 PROGRESS???
  // TODO: 15/02/2019 untriggered actors, serialization

  static final Object BREAK = new Object();
  static final Object END = new Object();
  static final Object NEXT = new Object();

  private static final NullaryFunction<? extends ByteBuffer> DEFAULT_BUFFER_CREATOR =
      new NullaryFunction<ByteBuffer>() {

        public ByteBuffer call() {
          return ByteBuffer.allocate(8 << 10);
        }
      };
  private static final NullaryFunction<Event<Boolean>> INFINITE_LOOP =
      new NullaryFunction<Event<Boolean>>() {

        public Event<Boolean> call() {
          return Event.ofTrue();
        }
      };
  private static final Action NO_OP = new Action() {

    public void run() {
    }
  };

  @NotNull
  public static <T> Story<T> ofEffects(@NotNull final Iterable<T> effects) {
    final Cache cache = Setting.get().getCache(Story.class);
    Story<T> story = cache.get(effects);
    if (story == null) {
      story = new EffectsStory<T>(effects);
      cache.put(effects, story);
    }
    return story;
  }

  @NotNull
  public static <T> Story<T> ofEmpty() {
    return ofEffects(Collections.<T>emptyList());
  }

  @NotNull
  public static <T> Story<T> ofIncidents(@NotNull final Iterable<? extends Throwable> incidents) {
    return new IncidentsStory<T>(incidents);
  }

  @NotNull
  public static Story<ByteBuffer> ofInputStream(@NotNull final InputStream inputStream) {
    return ofInputStream(new ListMemory(), inputStream, DEFAULT_BUFFER_CREATOR);
  }

  @NotNull
  public static Story<ByteBuffer> ofInputStream(@NotNull final InputStream inputStream,
      @NotNull final NullaryFunction<? extends ByteBuffer> bufferCreator) {
    return ofInputStream(new ListMemory(), inputStream, bufferCreator);
  }

  @NotNull
  public static Story<ByteBuffer> ofInputStream(@NotNull final Memory memory,
      @NotNull final InputStream inputStream) {
    return ofInputStream(memory, inputStream, DEFAULT_BUFFER_CREATOR);
  }

  @NotNull
  public static Story<ByteBuffer> ofInputStream(@NotNull final Memory memory,
      @NotNull final InputStream inputStream,
      @NotNull final NullaryFunction<? extends ByteBuffer> bufferCreator) {
    return new EffectsStory<ByteBuffer>(
        new InputStreamIterable(memory, inputStream, bufferCreator));
  }

  @NotNull
  public static <T> Story<T> ofNarrations(@NotNull final Memory memory,
      @NotNull final Narrator<T> storyNarrator) {
    return new NarratorStory<T>(memory, storyNarrator);
  }

  @NotNull
  public static <T> Story<T> ofNarrations(@NotNull final Memory memory,
      @NotNull final NullaryFunction<T> narrationCreator) {
    return ofNarrations(memory, new InfiniteNarrator<T>(narrationCreator));
  }

  @NotNull
  public static <T> Story<T> ofNarrations(@NotNull final Memory memory,
      @NotNull final UnaryFunction<? super Narrator<T>, ? extends Boolean> narrationCreator) {
    return ofNarrations(memory, new FunctionNarrator<T>(narrationCreator));
  }

  @NotNull
  public static <T> Story<T> ofNarrations(@NotNull final Narrator<T> storyNarrator) {
    return ofNarrations(new ListMemory(), storyNarrator);
  }

  @NotNull
  public static <T> Story<T> ofNarrations(@NotNull final NullaryFunction<T> narrationCreator) {
    return ofNarrations(new ListMemory(), narrationCreator);
  }

  @NotNull
  public static <T> Story<T> ofNarrations(
      @NotNull final UnaryFunction<? super Narrator<T>, ? extends Boolean> narrationCreator) {
    return ofNarrations(new ListMemory(), narrationCreator);
  }

  @NotNull
  public static <T> Story<T> ofSingleEffect(final T effect) {
    final Cache cache = Setting.get().getCache(Story.class);
    Story<T> story = cache.get(effect);
    if (story == null) {
      story = new EffectStory<T>(effect);
      cache.put(effect, story);
    }
    return story;
  }

  @NotNull
  public static <T> Story<T> ofSingleEvent(@NotNull final Event<T> event) {
    return new EventStory<T>(event);
  }

  @NotNull
  public static <T> Story<T> ofSingleIncident(@NotNull final Throwable incident) {
    return new IncidentStory<T>(incident);
  }

  @NotNull
  public static <T> Story<T> ofStory(@NotNull final Memory memory,
      @NotNull final NullaryFunction<? extends Story<T>> storyCreator) {
    return new FunctionStory<T>(memory, storyCreator);
  }

  @NotNull
  public static <T> Story<T> ofStory(
      @NotNull final NullaryFunction<? extends Story<T>> storyCreator) {
    return ofStory(new ListMemory(), storyCreator);
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public static <T> Story<T> unfold(@NotNull final Event<? extends Iterable<T>> event) {
    if (event instanceof Story) {
      return (Story<T>) event;
    }
    return new UnfoldStory<T>(event);
  }

  @NotNull
  public static <T, R> Story<R> when(@NotNull final Iterable<? extends Story<? extends T>> stories,
      @NotNull final NullaryFunction<? extends Event<? extends Boolean>> conditionHandler,
      @NotNull final UnaryFunction<? super List<T>, ? extends Story<R>> effectHandler) {
    return when(new ListMemory(), stories, conditionHandler, effectHandler);
  }

  @NotNull
  public static <T, R> Story<R> when(@NotNull final Iterable<? extends Story<? extends T>> stories,
      @NotNull final StoryLooper<? super List<T>, R> storyLooper) {
    return when(stories, new LooperConditionHandler(storyLooper),
        new LooperEffectHandler<List<T>, R>(storyLooper));
  }

  @NotNull
  public static <T, R> Story<R> when(@NotNull final Memory memory,
      @NotNull final Iterable<? extends Story<? extends T>> stories,
      @NotNull final NullaryFunction<? extends Event<? extends Boolean>> conditionHandler,
      @NotNull final UnaryFunction<? super List<T>, ? extends Story<R>> effectHandler) {
    return new GenericStory<T, R>(memory, stories, conditionHandler, effectHandler);
  }

  @NotNull
  public static <T, R> Story<R> when(@NotNull final Memory memory,
      @NotNull final Iterable<? extends Story<? extends T>> stories,
      @NotNull final StoryLooper<? super List<T>, R> storyLooper) {
    return when(memory, stories, new LooperConditionHandler(storyLooper),
        new LooperEffectHandler<List<T>, R>(storyLooper));
  }

  @NotNull
  public static <T1, R> Story<R> when(@NotNull final Memory memory,
      @NotNull final Story<? extends T1> firstStory,
      @NotNull final NullaryFunction<? extends Event<? extends Boolean>> conditionHandler,
      @NotNull final UnaryFunction<? super T1, ? extends Story<R>> effectHandler) {
    return new UnaryStory<T1, R>(memory, firstStory, conditionHandler, effectHandler);
  }

  @NotNull
  public static <T1, R> Story<R> when(@NotNull final Story<? extends T1> firstStory,
      @NotNull final NullaryFunction<? extends Event<? extends Boolean>> conditionHandler,
      @NotNull final UnaryFunction<? super T1, ? extends Story<R>> effectHandler) {
    return when(new ListMemory(), firstStory, conditionHandler, effectHandler);
  }

  private static boolean isEmpty(@Nullable final Story<?> story) {
    final Cache cache = Setting.get().getCache(Story.class);
    return (cache.get(Collections.EMPTY_LIST) == story) || (cache.get(Collections.EMPTY_SET)
        == story);
  }

  @SuppressWarnings("ConstantConditions")
  private static boolean isSameThread(@Nullable final String expectedThread,
      @Nullable final String actualThread) {
    return expectedThread.equals(actualThread);
  }

  @NotNull
  @Override
  public Story<T> eventually(@NotNull final Action eventualAction) {
    return eventually(new ListMemory(), eventualAction);
  }

  @NotNull
  public Story<T> eventually(@NotNull final Memory memory, @NotNull final Action eventualAction) {
    return new EventualStory<T>(memory, this, eventualAction);
  }

  public void playAll(@NotNull final EventObserver<? super T> eventObserver) {
    playAll(new EventStoryObserver<T>(eventObserver));
  }

  public void playAll(@Nullable final Observer<? super T> effectObserver,
      @Nullable final Observer<? super Throwable> incidentObserver,
      @Nullable final Action endAction) {
    playAll(new DefaultStoryObserver<T>(effectObserver, incidentObserver, endAction));
  }

  public void playAll(@NotNull final StoryObserver<? super T> storyObserver) {
    final Actor actor = BackStage.newActor(new StoryObserverScript<T>(storyObserver));
    final String actorId = actor.getId();
    getActor().tell(NEXT, new Options().withReceiptId(actorId).withThread(actorId), actor);
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <E1 extends Throwable> Story<T> resolve(@NotNull final Class<? extends E1> firstType,
      @NotNull final NullaryFunction<? extends Event<? extends Boolean>> conditionHandler,
      @NotNull final UnaryFunction<? super Throwable, ? extends Story<? extends T>> incidentHandler) {
    return resolve(new ListMemory(), firstType, conditionHandler, incidentHandler);
  }

  @NotNull
  public <E1 extends Throwable> Story<T> resolve(@NotNull final Class<? extends E1> firstType,
      @NotNull final StoryLooper<? super Throwable, ? extends T> storyLooper) {
    return resolve(firstType, new LooperConditionHandler(storyLooper),
        new LooperEffectHandler<Throwable, T>(storyLooper));
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <E extends Throwable> Story<T> resolve(
      @NotNull final Iterable<? extends Class<? extends E>> incidentTypes,
      @NotNull final NullaryFunction<? extends Event<? extends Boolean>> conditionHandler,
      @NotNull final UnaryFunction<? super Throwable, ? extends Story<? extends T>> incidentHandler) {
    return resolve(new ListMemory(), incidentTypes, conditionHandler, incidentHandler);
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <E1 extends Throwable> Story<T> resolve(@NotNull final Memory memory,
      @NotNull final Class<? extends E1> firstType,
      @NotNull final NullaryFunction<? extends Event<? extends Boolean>> conditionHandler,
      @NotNull final UnaryFunction<? super Throwable, ? extends Story<? extends T>> incidentHandler) {
    final HashSet<Class<? extends Throwable>> types = new HashSet<Class<? extends Throwable>>();
    types.add(firstType);
    return new ResolveStory<T>(memory, this, types, conditionHandler,
        (UnaryFunction<? super Throwable, ? extends Story<T>>) incidentHandler);
  }

  @NotNull
  public <E1 extends Throwable> Story<T> resolve(@NotNull final Memory memory,
      @NotNull final Class<? extends E1> firstType,
      @NotNull final StoryLooper<? super Throwable, ? extends T> storyLooper) {
    return resolve(memory, firstType, new LooperConditionHandler(storyLooper),
        new LooperEffectHandler<Throwable, T>(storyLooper));
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <E extends Throwable> Story<T> resolve(@NotNull final Memory memory,
      @NotNull final Iterable<? extends Class<? extends E>> incidentTypes,
      @NotNull final NullaryFunction<? extends Event<? extends Boolean>> conditionHandler,
      @NotNull final UnaryFunction<? super Throwable, ? extends Story<? extends T>> incidentHandler) {
    return new ResolveStory<T>(memory, this,
        Iterables.<Class<? extends Throwable>>toSet(incidentTypes), conditionHandler,
        (UnaryFunction<? super Throwable, ? extends Story<T>>) incidentHandler);
  }

  @NotNull
  public Story<T> scheduleAtFixedRate(final long initialDelay, final long period,
      @NotNull final TimeUnit unit) {
    return scheduleAtFixedRate(new ListMemory(), initialDelay, period, unit);
  }

  @NotNull
  public Story<T> scheduleAtFixedRate(@NotNull final Memory memory, final long initialDelay,
      final long period, @NotNull final TimeUnit unit) {
    return new ScheduleAtFixedRateStory<T>(memory, this, initialDelay, period, unit);
  }

  @NotNull
  public Story<T> scheduleWithFixedDelay(final long initialDelay, final long delay,
      @NotNull final TimeUnit unit) {
    return scheduleWithFixedDelay(new ListMemory(), initialDelay, delay, unit);
  }

  @NotNull
  public Story<T> scheduleWithFixedDelay(@NotNull final Memory memory, final long initialDelay,
      final long delay, @NotNull final TimeUnit unit) {
    return new ScheduleWithFixedDelayStory<T>(memory, this, initialDelay, delay, unit);
  }

  @NotNull
  public <R> Story<R> thenBlend(final int maxConcurrency,
      @NotNull final UnaryFunction<? super T, ? extends Story<R>> effectHandler) {
    return thenBlend(new ListMemory(), maxConcurrency, effectHandler);
  }

  @NotNull
  public <R> Story<R> thenBlend(@NotNull final Memory memory, final int maxConcurrency,
      @NotNull final UnaryFunction<? super T, ? extends Story<R>> effectHandler) {
    return new BlendStory<T, R>(memory, this, maxConcurrency, effectHandler);
  }

  @NotNull
  public <R> Story<R> thenJoin(final int maxConcurrency, final int maxEventWindow,
      @NotNull final UnaryFunction<? super T, ? extends Event<R>> effectHandler) {
    return thenJoin(new ListMemory(), maxConcurrency, maxEventWindow, effectHandler);
  }

  @NotNull
  public <R> Story<R> thenJoin(@NotNull final Memory memory, final int maxConcurrency,
      final int maxEventWindow,
      @NotNull final UnaryFunction<? super T, ? extends Event<R>> effectHandler) {
    return new JoinStory<T, R>(memory, this, maxConcurrency, maxEventWindow, effectHandler);
  }

  @NotNull
  public Story<T> thenWatchEach(@NotNull final EventObserver<? super T> eventObserver) {
    return thenWatchEach(new ListMemory(), eventObserver);
  }

  @NotNull
  public Story<T> thenWatchEach(@NotNull final Memory memory,
      @NotNull final EventObserver<? super T> eventObserver) {
    return new WatchStory<T>(memory, this, eventObserver);
  }

  @NotNull
  public Story<T> thenWatchEach(@NotNull final Memory memory,
      @Nullable final Observer<? super T> effectObserver,
      @Nullable final Observer<? super Throwable> incidentObserver) {
    return thenWatchEach(memory, new DefaultEventObserver<T>(effectObserver, incidentObserver));
  }

  @NotNull
  public Story<T> thenWatchEach(@Nullable final Observer<? super T> effectObserver,
      @Nullable final Observer<? super Throwable> incidentObserver) {
    return thenWatchEach(new DefaultEventObserver<T>(effectObserver, incidentObserver));
  }

  @NotNull
  public <R> Story<R> thenWhile(@NotNull final Memory memory,
      @NotNull final NullaryFunction<? extends Event<? extends Boolean>> conditionHandler,
      @NotNull final UnaryFunction<? super T, ? extends Story<R>> effectHandler) {
    return when(memory, this, conditionHandler, effectHandler);
  }

  @NotNull
  public <R> Story<R> thenWhile(@NotNull final Memory memory,
      @NotNull final StoryLooper<? super T, ? extends R> storyLooper) {
    return thenWhile(memory, new LooperConditionHandler(storyLooper),
        new LooperEffectHandler<T, R>(storyLooper));
  }

  @NotNull
  public <R> Story<R> thenWhile(
      @NotNull final NullaryFunction<? extends Event<? extends Boolean>> conditionHandler,
      @NotNull final UnaryFunction<? super T, ? extends Story<R>> effectHandler) {
    return when(this, conditionHandler, effectHandler);
  }

  @NotNull
  public <R> Story<R> thenWhile(@NotNull final StoryLooper<? super T, ? extends R> storyLooper) {
    return thenWhile(new LooperConditionHandler(storyLooper),
        new LooperEffectHandler<T, R>(storyLooper));
  }

  public interface Memory extends Iterable<Object> {

    void put(Object value);
  }

  public interface StoryLooper<T, R> {

    @Nullable
    Event<? extends Boolean> canContinue() throws Exception;

    @Nullable
    Story<R> elaborate(T effect) throws Exception;
  }

  public interface StoryObserver<T> extends EventObserver<T> {

    void onEnd() throws Exception;
  }

  static class DefaultStoryObserver<T> extends DefaultEventObserver<T> implements StoryObserver<T> {

    private final Action mEndAction;

    DefaultStoryObserver(@Nullable final Observer<? super T> effectObserver,
        @Nullable final Observer<? super Throwable> incidentObserver,
        @Nullable final Action endAction) {
      super(effectObserver, incidentObserver);
      mEndAction = (endAction != null) ? endAction : NO_OP;
    }

    public void onEnd() throws Exception {
      mEndAction.run();
    }
  }

  static class EventStoryObserver<T> implements StoryObserver<T> {

    private final EventObserver<? super T> mEventObserver;

    EventStoryObserver(@NotNull final EventObserver<? super T> eventObserver) {
      mEventObserver = ConstantConditions.notNull("eventObserver", eventObserver);
    }

    public void onEffect(final T effect) throws Exception {
      mEventObserver.onEffect(effect);
    }

    public void onIncident(@NotNull final Throwable incident) throws Exception {
      mEventObserver.onIncident(incident);
    }

    public void onEnd() {
    }
  }

  private abstract static class AbstractStory<T> extends Story<T> {

    private final Actor mActor;
    private final NullaryFunction<? extends Event<? extends Boolean>> mConditionHandler;
    private final String mContinueThread;
    private final HashMap<Actor, Options> mInputActors = new HashMap<Actor, Options>();
    private final String mInputThread;
    private final Object[] mInputs;
    private final HashMap<String, SenderIterator> mNextSenders =
        new HashMap<String, SenderIterator>();
    private final Options mOptions;
    private final String mOutputThread;
    private final Setting mSetting;

    private Conflict mConflict;
    private Actor mContinueActor;
    private int mContinueCount;
    private Options mContinueOptions;
    private HashMap<String, Sender> mGetSenders = new HashMap<String, Sender>();
    private int mInputCount;
    private Memory mMemory;
    private Actor mOutputActor;
    private Options mOutputOptions;

    private AbstractStory(@NotNull final Memory memory,
        @NotNull final NullaryFunction<? extends Event<? extends Boolean>> conditionHandler,
        final int numInputs) {
      mMemory = ConstantConditions.notNull("memory", memory);
      mConditionHandler = ConstantConditions.notNull("conditionHandler", conditionHandler);
      mInputs = new Object[numInputs];
      final Setting setting = (mSetting = Setting.get());
      final String actorId = (mActor = BackStage.newActor(new PlayScript(setting) {

        @NotNull
        @Override
        public Behavior getBehavior(@NotNull final String id) {
          return new InitBehavior();
        }
      })).getId();
      mContinueThread = actorId + ":continue";
      mInputThread = actorId + ":input";
      mOutputThread = actorId + ":output";
      mOptions = new Options().withReceiptId(actorId);
    }

    void endAction() throws Exception {
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

    private void conflict(@NotNull Conflict conflict, @NotNull final Context context) {
      Actor conflictActor = null;
      try {
        conflictActor = getConflictActor(conflict);

      } catch (final Throwable t) {
        conflict = new Conflict(t);
        if (t instanceof InterruptedException) {
          Thread.currentThread().interrupt();
        }
      }

      if (conflictActor != null) {
        (mOutputActor = conflictActor).tell(GET, mOutputOptions, context.getSelf());
        context.setBehavior(new OutputBehavior());

      } else {
        mMemory.put(conflict);
        final Actor self = context.getSelf();
        for (final SenderIterator sender : mNextSenders.values()) {
          sender.tellNext(self);
        }

        if (!mGetSenders.isEmpty()) {
          loop(++mContinueCount, context);

        } else {
          context.setBehavior(new NextContinueBehavior());
        }
      }
    }

    private void done(@NotNull final Context context) {
      final Memory memory = mMemory;
      try {
        endAction();

      } catch (final Throwable t) {
        memory.put(new Conflict(t));
        if (t instanceof InterruptedException) {
          Thread.currentThread().interrupt();
        }
      }
      Object effects = memory;
      for (final Object effect : memory) {
        if (effect instanceof Conflict) {
          effects = effect;
          break;
        }
      }
      final Actor self = context.getSelf();
      for (final Sender sender : mGetSenders.values()) {
        sender.getSender().tell(effects, sender.getOptions(), self);
      }
      mGetSenders = null;
      final HashMap<String, SenderIterator> nextSenders = mNextSenders;
      for (final SenderIterator sender : nextSenders.values()) {
        if (sender.isWaitNext() && !sender.tellNext(self)) {
          sender.getSender().tell(END, sender.getOptions(), self);
        }
      }
      context.setBehavior(new DoneBehavior(effects, memory, nextSenders));
    }

    private void fail(@NotNull Conflict conflict, @NotNull final Context context) {
      tellInputActors(BREAK, context);
      final Actor outputActor = mOutputActor;
      if (outputActor != null) {
        outputActor.tell(BREAK, mOutputOptions, context.getSelf());
      }

      try {
        endAction();

      } catch (final Throwable t) {
        conflict = new Conflict(t);
        if (t instanceof InterruptedException) {
          Thread.currentThread().interrupt();
        }
      }
      final Actor self = context.getSelf();
      for (final Sender sender : mGetSenders.values()) {
        sender.getSender().tell(conflict, sender.getOptions(), self);
      }
      mGetSenders = null;
      final Memory memory = mMemory;
      memory.put(conflict);
      final HashMap<String, SenderIterator> nextSenders = mNextSenders;
      for (final SenderIterator sender : nextSenders.values()) {
        sender.tellNext(self);
      }
      context.setBehavior(new DoneBehavior(conflict, memory, nextSenders));
    }

    private void loop(final int loopCount, @NotNull final Context context) {
      Setting.set(getSetting());
      try {
        final Event<? extends Boolean> event;
        Setting.set(getSetting());
        try {
          event = mConditionHandler.call();

        } finally {
          Setting.unset();
        }

        if ((event == null) || Event.isFalse(event) || Event.isNull(event)) {
          done(context);

        } else if (Event.isTrue(event)) {
          setThreadIds(loopCount);
          tellInputActors(NEXT, context);
          context.setBehavior(new InputBehavior());

        } else {
          setThreadIds(loopCount);
          (mContinueActor = event.getActor()).tell(GET, mContinueOptions, context.getSelf());
          context.setBehavior(new ContinueBehavior());
        }

      } catch (final Throwable t) {
        fail(new Conflict(t), context);
        if (t instanceof InterruptedException) {
          Thread.currentThread().interrupt();
        }
      }
    }

    private void setThreadIds(final int count) {
      final Options options = mOptions;
      mContinueOptions = options.withThread(mContinueThread + "#" + count);
      mOutputOptions = options.withThread(mOutputThread + "#" + count);
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
          mGetSenders.put(options.getThread(), new Sender(envelop.getSender(), options));

        } else if (message == NEXT) {
          final Options options = envelop.getOptions();
          final String thread = options.getThread();
          final HashMap<String, SenderIterator> nextSenders = mNextSenders;
          SenderIterator sender = nextSenders.get(thread);
          if (sender != null) {
            sender.waitNext();

          } else {
            sender = new SenderIterator(envelop.getSender(), options.threadOnly());
            sender.setIterator(mMemory.iterator());
            nextSenders.put(thread, sender);
          }
          sender.tellNext(context.getSelf());

        } else if (message == BREAK) {
          mNextSenders.remove(envelop.getOptions().getThread());

        } else {
          final Options outputOptions = mOutputOptions;
          if (isSameThread(outputOptions.getThread(), envelop.getOptions().getThread())) {
            mOutputActor.tell(BREAK, outputOptions, context.getSelf());
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

    private class ContinueBehavior extends AbstractBehavior {

      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          final Options options = envelop.getOptions().threadOnly();
          mGetSenders.put(options.getThread(), new Sender(envelop.getSender(), options));

        } else if (message == NEXT) {
          final Options options = envelop.getOptions();
          final String thread = options.getThread();
          final HashMap<String, SenderIterator> nextSenders = mNextSenders;
          SenderIterator sender = nextSenders.get(thread);
          if (sender != null) {
            sender.waitNext();

          } else {
            sender = new SenderIterator(envelop.getSender(), options.threadOnly());
            sender.setIterator(mMemory.iterator());
            nextSenders.put(thread, sender);
          }
          sender.tellNext(context.getSelf());

        } else if (message == BREAK) {
          mNextSenders.remove(envelop.getOptions().getThread());

        } else if (message == CANCEL) {
          mContinueActor.tell(CANCEL, mOutputOptions, context.getSelf());
          tellInputActors(CANCEL, context);
          context.setBehavior(new CancelBehavior());

        } else {
          final String thread = envelop.getOptions().getThread();
          if (isSameThread(mContinueOptions.getThread(), thread)) {
            if (message instanceof Conflict) {
              fail((Conflict) message, context);

            } else if (message instanceof Bounce) {
              fail(Conflict.ofBounce((Bounce) message), context);

            } else {
              if (Boolean.TRUE.equals(message)) {
                tellInputActors(NEXT, context);
                context.setBehavior(new InputBehavior());

              } else {
                done(context);
              }
            }
          }
        }
        envelop.preventReceipt();
      }
    }

    private class InitBehavior extends AbstractBehavior {

      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          final Options options = envelop.getOptions().threadOnly();
          mGetSenders.put(options.getThread(), new Sender(envelop.getSender(), options));
          loop(mContinueCount, context);

        } else if (message == NEXT) {
          final Options options = envelop.getOptions().threadOnly();
          final SenderIterator sender = new SenderIterator(envelop.getSender(), options);
          sender.setIterator(mMemory.iterator());
          mNextSenders.put(options.getThread(), sender);
          loop(mContinueCount, context);

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
          final Options options = envelop.getOptions().threadOnly();
          mGetSenders.put(options.getThread(), new Sender(envelop.getSender(), options));

        } else if (message == NEXT) {
          final Options options = envelop.getOptions();
          final String thread = options.getThread();
          final HashMap<String, SenderIterator> nextSenders = mNextSenders;
          SenderIterator sender = nextSenders.get(thread);
          if (sender != null) {
            sender.waitNext();

          } else {
            sender = new SenderIterator(envelop.getSender(), options.threadOnly());
            sender.setIterator(mMemory.iterator());
            nextSenders.put(thread, sender);
          }
          sender.tellNext(context.getSelf());

        } else if (message == BREAK) {
          mNextSenders.remove(envelop.getOptions().getThread());

        } else if (message == CANCEL) {
          tellInputActors(CANCEL, context);
          context.setBehavior(new CancelBehavior());

        } else {
          final String inputThread = mInputThread;
          final String thread = envelop.getOptions().getThread();
          if ((thread != null) && thread.startsWith(inputThread)) {
            if (message == END) {
              tellInputActors(BREAK, context);
              done(context);

            } else if (message instanceof Conflict) {
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
                  mInputCount = 0;
                  if (mConflict != null) {
                    conflict(mConflict, context);

                  } else {
                    Object effect = message;
                    Actor outputActor = null;
                    try {
                      outputActor = getOutputActor(inputs);

                    } catch (final Throwable t) {
                      effect = new Conflict(t);
                      if (t instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                      }
                    }

                    final Actor self = context.getSelf();
                    if (outputActor != null) {
                      (mOutputActor = outputActor).tell(NEXT, mOutputOptions, self);
                      context.setBehavior(new OutputBehavior());

                    } else {
                      mMemory.put(effect);
                      for (final SenderIterator sender : mNextSenders.values()) {
                        sender.tellNext(self);
                      }

                      if (!mGetSenders.isEmpty()) {
                        loop(++mContinueCount, context);

                      } else {
                        context.setBehavior(new NextContinueBehavior());
                      }
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

    private class NextContinueBehavior extends AbstractBehavior {

      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          final Options options = envelop.getOptions().threadOnly();
          mGetSenders.put(options.getThread(), new Sender(envelop.getSender(), options));
          loop(++mContinueCount, context);

        } else if (message == NEXT) {
          final Options options = envelop.getOptions();
          final String thread = options.getThread();
          final HashMap<String, SenderIterator> nextSenders = mNextSenders;
          SenderIterator sender = nextSenders.get(thread);
          if (sender != null) {
            sender.waitNext();

          } else {
            sender = new SenderIterator(envelop.getSender(), options.threadOnly());
            sender.setIterator(mMemory.iterator());
            nextSenders.put(thread, sender);
          }
          final Actor self = context.getSelf();
          if (!sender.tellNext(self)) {
            loop(++mContinueCount, context);
          }

        } else if (message == BREAK) {
          mNextSenders.remove(envelop.getOptions().getThread());

        } else if (message == CANCEL) {
          tellInputActors(CANCEL, context);
          fail(Conflict.ofCancel(), context);
        }
        envelop.preventReceipt();
      }
    }

    private class NextOutputBehavior extends AbstractBehavior {

      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          final Options options = envelop.getOptions().threadOnly();
          mGetSenders.put(options.getThread(), new Sender(envelop.getSender(), options));
          mOutputActor.tell(NEXT, mOutputOptions, context.getSelf());
          context.setBehavior(new OutputBehavior());

        } else if (message == NEXT) {
          final Options options = envelop.getOptions();
          final String thread = options.getThread();
          final HashMap<String, SenderIterator> nextSenders = mNextSenders;
          SenderIterator sender = nextSenders.get(thread);
          if (sender != null) {
            sender.waitNext();

          } else {
            sender = new SenderIterator(envelop.getSender(), options.threadOnly());
            sender.setIterator(mMemory.iterator());
            nextSenders.put(thread, sender);
          }
          final Actor self = context.getSelf();
          if (!sender.tellNext(self)) {
            mOutputActor.tell(NEXT, mOutputOptions, self);
            context.setBehavior(new OutputBehavior());
          }

        } else if (message == BREAK) {
          mNextSenders.remove(envelop.getOptions().getThread());

        } else if (message == CANCEL) {
          mOutputActor.tell(BREAK, mOutputOptions, context.getSelf());
          tellInputActors(CANCEL, context);
          fail(Conflict.ofCancel(), context);
        }
        envelop.preventReceipt();
      }
    }

    private class OutputBehavior extends AbstractBehavior {

      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          final Options options = envelop.getOptions().threadOnly();
          mGetSenders.put(options.getThread(), new Sender(envelop.getSender(), options));

        } else if (message == NEXT) {
          final Options options = envelop.getOptions();
          final String thread = options.getThread();
          final HashMap<String, SenderIterator> nextSenders = mNextSenders;
          SenderIterator sender = nextSenders.get(thread);
          if (sender != null) {
            sender.waitNext();

          } else {
            sender = new SenderIterator(envelop.getSender(), options.threadOnly());
            sender.setIterator(mMemory.iterator());
            nextSenders.put(thread, sender);
          }
          sender.tellNext(context.getSelf());

        } else if (message == BREAK) {
          mNextSenders.remove(envelop.getOptions().getThread());

        } else if (message == CANCEL) {
          mOutputActor.tell(CANCEL, mOutputOptions, context.getSelf());
          tellInputActors(CANCEL, context);
          context.setBehavior(new CancelBehavior());

        } else {
          final Options outputOptions = mOutputOptions;
          if (isSameThread(outputOptions.getThread(), envelop.getOptions().getThread())) {
            if (message == END) {
              mOutputActor.tell(BREAK, outputOptions, context.getSelf());
              loop(++mContinueCount, context);

            } else if (message instanceof Bounce) {
              fail(Conflict.ofBounce((Bounce) message), context);

            } else {
              // pass on conflicts
              mMemory.put(message);
              final Actor self = context.getSelf();
              for (final SenderIterator sender : mNextSenders.values()) {
                sender.tellNext(self);
              }

              if (!mGetSenders.isEmpty()) {
                mOutputActor.tell(NEXT, outputOptions, self);

              } else {
                context.setBehavior(new NextOutputBehavior());
              }
            }
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

  private static class BlendStory<T, R> extends Story<R> {

    private final Actor mActor;
    private final UnaryFunction<? super T, ? extends Story<R>> mEffectHandler;
    private final Actor mInputActor;
    private final Options mInputOptions;
    private final int mMaxConcurrency;
    private final Memory mMemory;
    private final HashMap<Actor, Options> mNextActors = new HashMap<Actor, Options>();
    private final HashMap<String, SenderIterator> mNextSenders =
        new HashMap<String, SenderIterator>();
    private final Options mOptions;
    private final HashMap<Actor, Options> mOutputActors = new HashMap<Actor, Options>();
    private final String mOutputThread;
    private final Setting mSetting;

    private HashMap<String, Sender> mGetSenders = new HashMap<String, Sender>();
    private Conflict mInputConflict;
    private boolean mInputsEnded;
    private boolean mInputsPending;
    private long mOutputCount;

    private BlendStory(@NotNull final Memory memory, @NotNull final Story<? extends T> story,
        final int maxConcurrency,
        @NotNull final UnaryFunction<? super T, ? extends Story<R>> effectHandler) {
      mMemory = ConstantConditions.notNull("memory", memory);
      mInputActor = story.getActor();
      mMaxConcurrency = ConstantConditions.positive("maxConcurrency", maxConcurrency);
      mEffectHandler = ConstantConditions.notNull("effectHandler", effectHandler);
      final Setting setting = (mSetting = Setting.get());
      final String actorId = (mActor = BackStage.newActor(new PlayScript(setting) {

        @NotNull
        @Override
        public Behavior getBehavior(@NotNull final String id) {
          return new InitBehavior();
        }
      })).getId();
      final Options options = (mOptions = new Options().withReceiptId(actorId));
      mOutputThread = actorId + ":output";
      mInputOptions = options.withThread(actorId + ":input");
    }

    private void done(@NotNull final Context context) {
      final Memory memory = mMemory;
      Object effects = memory;
      for (final Object effect : memory) {
        if (effect instanceof Conflict) {
          effects = effect;
          break;
        }
      }
      final Actor self = context.getSelf();
      for (final Sender sender : mGetSenders.values()) {
        sender.getSender().tell(effects, sender.getOptions(), self);
      }
      mGetSenders = null;
      final HashMap<String, SenderIterator> nextSenders = mNextSenders;
      for (final SenderIterator sender : nextSenders.values()) {
        if (sender.isWaitNext() && !sender.tellNext(self)) {
          sender.getSender().tell(END, sender.getOptions(), self);
        }
      }
      context.setBehavior(new DoneBehavior(effects, memory, nextSenders));
    }

    private void fail(@NotNull final Conflict conflict, @NotNull final Context context) {
      final Actor self = context.getSelf();
      for (final Sender sender : mGetSenders.values()) {
        sender.getSender().tell(conflict, sender.getOptions(), self);
      }
      mGetSenders = null;
      final Memory memory = mMemory;
      memory.put(conflict);
      final HashMap<String, SenderIterator> nextSenders = mNextSenders;
      for (final SenderIterator sender : nextSenders.values()) {
        sender.tellNext(self);
      }
      context.setBehavior(new DoneBehavior(conflict, memory, nextSenders));
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private Actor getOutputActor(@NotNull final Object input) throws Exception {
      Setting.set(mSetting);
      try {
        final Story<R> story = mEffectHandler.call((T) input);
        if ((story == null) || isEmpty(story)) {
          return null;
        }
        return story.getActor();

      } finally {
        Setting.unset();
      }
    }

    private class CancelBehavior extends AbstractBehavior {

      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          final Options options = envelop.getOptions().threadOnly();
          mGetSenders.put(options.getThread(), new Sender(envelop.getSender(), options));

        } else if (message == NEXT) {
          final Options options = envelop.getOptions();
          final String thread = options.getThread();
          final HashMap<String, SenderIterator> nextSenders = mNextSenders;
          SenderIterator sender = nextSenders.get(thread);
          if (sender != null) {
            sender.waitNext();

          } else {
            sender = new SenderIterator(envelop.getSender(), options.threadOnly());
            sender.setIterator(mMemory.iterator());
            nextSenders.put(thread, sender);
          }
          sender.tellNext(context.getSelf());

        } else if (message == BREAK) {
          mNextSenders.remove(envelop.getOptions().getThread());

        } else {
          final Options inputOptions = mInputOptions;
          if (isSameThread(inputOptions.getThread(), envelop.getOptions().getThread())) {
            mInputActor.tell(BREAK, inputOptions, context.getSelf());
            mInputsEnded = true;
            if (mOutputActors.isEmpty()) {
              fail(Conflict.ofCancel(), context);
            }

          } else {
            final String thread = envelop.getOptions().getThread();
            if ((thread != null) && thread.startsWith(mOutputThread)) {
              final HashMap<Actor, Options> outputActors = mOutputActors;
              final Actor sender = envelop.getSender();
              sender.tell(BREAK, outputActors.get(sender), context.getSelf());
              outputActors.remove(sender);
              if (outputActors.isEmpty() && mInputsEnded) {
                fail(Conflict.ofCancel(), context);
              }
            }
          }
        }
        envelop.preventReceipt();
      }
    }

    private class InitBehavior extends AbstractBehavior {

      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          final Options options = envelop.getOptions().threadOnly();
          mGetSenders.put(options.getThread(), new Sender(envelop.getSender(), options));
          mInputActor.tell(NEXT, mInputOptions, context.getSelf());
          context.setBehavior(new InputBehavior());

        } else if (message == NEXT) {
          final Options options = envelop.getOptions().threadOnly();
          final SenderIterator sender = new SenderIterator(envelop.getSender(), options);
          sender.setIterator(mMemory.iterator());
          mNextSenders.put(options.getThread(), sender);
          mInputActor.tell(NEXT, mInputOptions, context.getSelf());
          context.setBehavior(new InputBehavior());

        } else if (message == CANCEL) {
          mInputActor.tell(CANCEL, mInputOptions, context.getSelf());
          fail(Conflict.ofCancel(), context);
        }
        envelop.preventReceipt();
      }
    }

    private class InputBehavior extends AbstractBehavior {

      public void onMessage(Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          final Options options = envelop.getOptions().threadOnly();
          mGetSenders.put(options.getThread(), new Sender(envelop.getSender(), options));

        } else if (message == NEXT) {
          final Options options = envelop.getOptions();
          final String thread = options.getThread();
          final HashMap<String, SenderIterator> nextSenders = mNextSenders;
          SenderIterator sender = nextSenders.get(thread);
          if (sender != null) {
            sender.waitNext();

          } else {
            sender = new SenderIterator(envelop.getSender(), options.threadOnly());
            sender.setIterator(mMemory.iterator());
            nextSenders.put(thread, sender);
          }

        } else if (message == BREAK) {
          mNextSenders.remove(envelop.getOptions().getThread());

        } else if (message == CANCEL) {
          mInputActor.tell(CANCEL, mInputOptions, context.getSelf());
          context.setBehavior(new CancelBehavior());

        } else {
          final Options inputOptions = mInputOptions;
          if (isSameThread(inputOptions.getThread(), envelop.getOptions().getThread())) {
            if (message == END) {
              mInputActor.tell(BREAK, inputOptions, context.getSelf());
              done(context);

            } else if (message instanceof Conflict) {
              mInputActor.tell(BREAK, inputOptions, context.getSelf());
              fail((Conflict) message, context);

            } else if (message instanceof Bounce) {
              fail(Conflict.ofBounce((Bounce) message), context);

            } else {
              try {
                final Actor outputActor = getOutputActor(message);
                if (outputActor != null) {
                  final String outputThreadId = mOutputThread + "#" + mOutputCount++;
                  final Options options = mOptions.withThread(outputThreadId);
                  final HashMap<Actor, Options> outputActors = mOutputActors;
                  outputActors.put(outputActor, options);
                  outputActor.tell(NEXT, options, context.getSelf());
                  if ((mInputsPending = outputActors.size() < mMaxConcurrency)) {
                    mInputActor.tell(NEXT, inputOptions, context.getSelf());
                  }
                  context.setBehavior(new OutputBehavior());

                } else if ((mInputsPending = mOutputActors.size() < mMaxConcurrency)) {
                  mInputActor.tell(NEXT, inputOptions, context.getSelf());
                }

              } catch (final Throwable t) {
                mInputActor.tell(BREAK, inputOptions, context.getSelf());
                fail(new Conflict(t), context);
                if (t instanceof InterruptedException) {
                  Thread.currentThread().interrupt();
                }
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
          mGetSenders.put(options.getThread(), new Sender(envelop.getSender(), options));
          final HashMap<Actor, Options> nextActors = mNextActors;
          final Actor self = context.getSelf();
          for (final Entry<Actor, Options> entry : nextActors.entrySet()) {
            entry.getKey().tell(NEXT, entry.getValue(), self);
          }
          nextActors.clear();

        } else if (message == NEXT) {
          final Options options = envelop.getOptions();
          final String thread = options.getThread();
          final HashMap<String, SenderIterator> nextSenders = mNextSenders;
          SenderIterator sender = nextSenders.get(thread);
          if (sender != null) {
            sender.waitNext();

          } else {
            sender = new SenderIterator(envelop.getSender(), options.threadOnly());
            sender.setIterator(mMemory.iterator());
            nextSenders.put(thread, sender);
          }
          final HashMap<Actor, Options> outputActors = mOutputActors;
          if (!sender.tellNext(context.getSelf()) && (mNextActors.size() == outputActors.size())) {
            final Actor self = context.getSelf();
            for (final Entry<Actor, Options> entry : outputActors.entrySet()) {
              entry.getKey().tell(NEXT, entry.getValue(), self);
            }
          }

        } else if (message == BREAK) {
          mNextSenders.remove(envelop.getOptions().getThread());

        } else if (message == CANCEL) {
          final Actor self = context.getSelf();
          for (final Entry<Actor, Options> entry : mOutputActors.entrySet()) {
            entry.getKey().tell(CANCEL, entry.getValue(), self);
          }
          mInputActor.tell(CANCEL, mInputOptions, self);
          context.setBehavior(new CancelBehavior());

        } else {
          final Options inputOptions = mInputOptions;
          if (isSameThread(inputOptions.getThread(), envelop.getOptions().getThread())) {
            if (message == END) {
              mInputActor.tell(BREAK, inputOptions, context.getSelf());
              mInputsEnded = true;
              if (mOutputActors.isEmpty()) {
                done(context);
              }

            } else if (message instanceof Conflict) {
              mInputActor.tell(BREAK, inputOptions, context.getSelf());
              mInputsEnded = true;
              final Conflict conflict = (mInputConflict = (Conflict) message);
              if (mOutputActors.isEmpty()) {
                fail(conflict, context);
              }

            } else if (message instanceof Bounce) {
              mInputsEnded = true;
              final Conflict conflict = (mInputConflict = Conflict.ofBounce((Bounce) message));
              if (mOutputActors.isEmpty()) {
                fail(conflict, context);
              }

            } else {
              try {
                final Actor outputActor = getOutputActor(message);
                if (outputActor != null) {
                  final String outputThreadId = mOutputThread + "#" + mOutputCount++;
                  final Options options = mOptions.withThread(outputThreadId);
                  final HashMap<Actor, Options> outputActors = mOutputActors;
                  outputActors.put(outputActor, options);
                  outputActor.tell(NEXT, options, context.getSelf());
                  if ((mInputsPending = outputActors.size() < mMaxConcurrency)) {
                    mInputActor.tell(NEXT, inputOptions, context.getSelf());
                  }

                } else if ((mInputsPending = mOutputActors.size() < mMaxConcurrency)) {
                  mInputActor.tell(NEXT, inputOptions, context.getSelf());
                }

              } catch (final Throwable t) {
                mInputsEnded = true;
                final Conflict conflict = (mInputConflict = new Conflict(t));
                if (mOutputActors.isEmpty()) {
                  fail(conflict, context);
                }

                if (t instanceof InterruptedException) {
                  Thread.currentThread().interrupt();
                }
              }
            }

          } else {
            final String thread = envelop.getOptions().getThread();
            if ((thread != null) && thread.startsWith(mOutputThread)) {
              if (message == END) {
                final Actor self = context.getSelf();
                final Actor sender = envelop.getSender();
                sender.tell(BREAK, mOutputActors.get(sender), self);
                final HashMap<Actor, Options> outputActors = mOutputActors;
                outputActors.remove(sender);
                if (!mInputsEnded) {
                  if (!mInputsPending) {
                    mInputsPending = true;
                    mInputActor.tell(NEXT, inputOptions, self);
                  }

                } else if (outputActors.isEmpty()) {
                  final Conflict conflict = mInputConflict;
                  if (conflict != null) {
                    fail(conflict, context);

                  } else {
                    done(context);
                  }
                }

              } else if (message instanceof Bounce) {
                mMemory.put(Conflict.ofBounce((Bounce) message));
                final Actor self = context.getSelf();
                for (final SenderIterator sender : mNextSenders.values()) {
                  sender.tellNext(self);
                }

                final HashMap<Actor, Options> outputActors = mOutputActors;
                outputActors.remove(envelop.getSender());
                if (!mInputsEnded) {
                  if (!mInputsPending) {
                    mInputsPending = true;
                    mInputActor.tell(NEXT, inputOptions, self);
                  }

                } else if (outputActors.isEmpty()) {
                  final Conflict conflict = mInputConflict;
                  if (conflict != null) {
                    fail(conflict, context);

                  } else {
                    done(context);
                  }
                }

              } else {
                // pass on conflicts
                mMemory.put(message);
                final Actor self = context.getSelf();
                for (final SenderIterator sender : mNextSenders.values()) {
                  sender.tellNext(self);
                }

                final Actor sender = envelop.getSender();
                if (!mGetSenders.isEmpty()) {
                  sender.tell(NEXT, mOutputActors.get(sender), self);

                } else {
                  mNextActors.put(sender, mOutputActors.get(sender));
                }
              }
            }
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

  private static class DoneBehavior extends AbstractBehavior {

    private final Object mEffect;
    private final Iterable<?> mEffects;
    private final Map<String, SenderIterator> mNextSenders;

    private DoneBehavior(final Object effect, @NotNull final Iterable<?> effects,
        @NotNull final Map<String, SenderIterator> nextSenders) {
      mEffect = effect;
      mEffects = effects;
      mNextSenders = nextSenders;
    }

    public void onMessage(final Object message, @NotNull final Envelop envelop,
        @NotNull final Context context) {
      if (message == GET) {
        envelop.getSender().tell(mEffect, envelop.getOptions().threadOnly(), context.getSelf());

      } else if (message == NEXT) {
        final Actor self = context.getSelf();
        final Options options = envelop.getOptions().threadOnly();
        final String thread = options.getThread();
        final Map<String, SenderIterator> nextSenders = mNextSenders;
        SenderIterator sender = nextSenders.get(thread);
        if (sender != null) {
          sender.waitNext();

        } else {
          sender = new SenderIterator(envelop.getSender(), options);
          sender.setIterator(mEffects.iterator());
          nextSenders.put(thread, sender);
        }

        if (!sender.tellNext(self)) {
          sender.getSender().tell(END, options, self);
        }

      } else if (message == BREAK) {
        mNextSenders.remove(envelop.getOptions().getThread());
      }
      envelop.preventReceipt();
    }
  }

  private static class EffectStory<T> extends Story<T> {

    private final Actor mActor;

    private EffectStory(final T effect) {
      mActor = BackStage.newActor(new TrampolinePlayScript(Setting.get()) {

        private final HashSet<String> mThreads = new HashSet<String>();

        @NotNull
        @Override
        public Behavior getBehavior(@NotNull final String id) {
          return new AbstractBehavior() {

            public void onMessage(final Object message, @NotNull final Envelop envelop,
                @NotNull final Context context) {
              if (message == GET) {
                envelop.getSender()
                    .tell(effect, envelop.getOptions().threadOnly(), context.getSelf());

              } else if (message == NEXT) {
                final HashSet<String> threads = mThreads;
                final Options options = envelop.getOptions().threadOnly();
                final String thread = options.getThread();
                if (!threads.contains(thread)) {
                  envelop.getSender()
                      .tell(effect, envelop.getOptions().threadOnly(), context.getSelf());
                  threads.add(thread);

                } else {
                  envelop.getSender()
                      .tell(END, envelop.getOptions().threadOnly(), context.getSelf());
                }

              } else if (message == BREAK) {
                mThreads.remove(envelop.getOptions().getThread());
              }
              envelop.preventReceipt();
            }
          };
        }
      });
    }

    @NotNull
    Actor getActor() {
      return mActor;
    }
  }

  private static class EffectsStory<T> extends Story<T> {

    private final Actor mActor;

    private EffectsStory(@NotNull final Iterable<T> effects) {
      ConstantConditions.notNull("effects", effects);
      mActor = BackStage.newActor(new TrampolinePlayScript(Setting.get()) {

        private final HashMap<String, Iterator<T>> mThreads = new HashMap<String, Iterator<T>>();

        @NotNull
        @Override
        public Behavior getBehavior(@NotNull final String id) {
          return new AbstractBehavior() {

            public void onMessage(final Object message, @NotNull final Envelop envelop,
                @NotNull final Context context) {
              if (message == GET) {
                envelop.getSender()
                    .tell(Iterables.toList(effects), envelop.getOptions().threadOnly(),
                        context.getSelf());

              } else if (message == NEXT) {
                final HashMap<String, Iterator<T>> threads = mThreads;
                final Options options = envelop.getOptions().threadOnly();
                final String thread = options.getThread();
                Iterator<T> iterator = threads.get(thread);
                if (iterator == null) {
                  iterator = effects.iterator();
                  threads.put(thread, iterator);
                }
                envelop.getSender()
                    .tell(iterator.hasNext() ? iterator.next() : END, options, context.getSelf());

              } else if (message == BREAK) {
                mThreads.remove(envelop.getOptions().getThread());
              }
              envelop.preventReceipt();
            }
          };
        }
      });
    }

    @NotNull
    Actor getActor() {
      return mActor;
    }
  }

  private static class EventStory<T> extends Story<T> {

    private final Actor mActor;
    private final Actor mInputActor;
    private final HashMap<String, SenderIterator> mNextSenders =
        new HashMap<String, SenderIterator>();
    private final Options mOptions;

    private HashMap<String, Sender> mGetSenders = new HashMap<String, Sender>();

    private EventStory(@NotNull final Event<T> event) {
      mInputActor = event.getActor();
      final String actorId = (mActor = BackStage.newActor(new TrampolinePlayScript(Setting.get()) {

        @NotNull
        @Override
        public Behavior getBehavior(@NotNull final String id) {
          return new InitBehavior();
        }
      })).getId();
      mOptions = new Options().withReceiptId(actorId).withThread(actorId);
    }

    private void fail(@NotNull final Conflict conflict, @NotNull final Context context) {
      final Actor self = context.getSelf();
      for (final Sender sender : mGetSenders.values()) {
        sender.getSender().tell(conflict, sender.getOptions(), self);
      }
      mGetSenders = null;
      final Set<Conflict> conflicts = Collections.singleton(conflict);
      for (final SenderIterator sender : mNextSenders.values()) {
        sender.setIterator(conflicts.iterator());
      }
      context.setBehavior(new DoneBehavior(conflict, conflicts, mNextSenders));
    }

    private class CancelBehavior extends AbstractBehavior {

      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          final Options options = envelop.getOptions().threadOnly();
          mGetSenders.put(options.getThread(), new Sender(envelop.getSender(), options));

        } else if (message == NEXT) {
          final Options options = envelop.getOptions().threadOnly();
          final String thread = options.getThread();
          final HashMap<String, SenderIterator> nextSenders = mNextSenders;
          SenderIterator sender = nextSenders.get(thread);
          if (sender != null) {
            sender.waitNext();

          } else {
            sender = new SenderIterator(envelop.getSender(), options.threadOnly());
            nextSenders.put(thread, sender);
          }

        } else if (message == BREAK) {
          mNextSenders.remove(envelop.getOptions().getThread());

        } else {
          if (context.getSelf().getId().equals(envelop.getOptions().getThread())) {
            fail(Conflict.ofCancel(), context);
          }
        }
        envelop.preventReceipt();
      }
    }

    private class InitBehavior extends AbstractBehavior {

      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          final Options options = envelop.getOptions().threadOnly();
          mGetSenders.put(options.getThread(), new Sender(envelop.getSender(), options));
          mInputActor.tell(GET, mOptions, context.getSelf());
          context.setBehavior(new InputBehavior());

        } else if (message == NEXT) {
          final Options options = envelop.getOptions().threadOnly();
          mNextSenders.put(options.getThread(), new SenderIterator(envelop.getSender(), options));
          mInputActor.tell(GET, mOptions, context.getSelf());
          context.setBehavior(new InputBehavior());

        } else if (message == CANCEL) {
          mInputActor.tell(CANCEL, mOptions, context.getSelf());
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
          mGetSenders.put(options.getThread(), new Sender(envelop.getSender(), options));

        } else if (message == NEXT) {
          final Options options = envelop.getOptions().threadOnly();
          final String thread = options.getThread();
          final HashMap<String, SenderIterator> nextSenders = mNextSenders;
          SenderIterator sender = nextSenders.get(thread);
          if (sender != null) {
            sender.waitNext();

          } else {
            sender = new SenderIterator(envelop.getSender(), options.threadOnly());
            nextSenders.put(thread, sender);
          }

        } else if (message == BREAK) {
          mNextSenders.remove(envelop.getOptions().getThread());

        } else if (message == CANCEL) {
          mInputActor.tell(CANCEL, mOptions, context.getSelf());
          context.setBehavior(new CancelBehavior());

        } else {
          final Actor self = context.getSelf();
          if (self.getId().equals(envelop.getOptions().getThread())) {
            if (message instanceof Conflict) {
              fail((Conflict) message, context);

            } else if (message instanceof Bounce) {
              fail(Conflict.ofBounce((Bounce) message), context);

            } else {
              for (final Sender sender : mGetSenders.values()) {
                sender.getSender().tell(message, sender.getOptions(), self);
              }
              mGetSenders = null;
              final Set<Object> effects = Collections.singleton(message);
              for (final SenderIterator sender : mNextSenders.values()) {
                sender.setIterator(effects.iterator());
                sender.tellNext(self);
              }
              context.setBehavior(new DoneBehavior(effects, effects, mNextSenders));
            }
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

  private static class EventualStory<T> extends AbstractStory<T> {

    private final List<Actor> mActors;
    private final Action mEventualAction;

    private EventualStory(@NotNull final Memory memory, @NotNull final Story<T> story,
        @NotNull final Action eventualAction) {
      super(memory, INFINITE_LOOP, 1);
      mActors = Collections.singletonList(story.getActor());
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

  private static class FunctionStory<T> extends AbstractStory<T> {

    private final NullaryFunction<? extends Story<T>> mStoryCreator;

    private List<Actor> mActors;

    private FunctionStory(@NotNull final Memory memory,
        @NotNull final NullaryFunction<? extends Story<T>> storyCreator) {
      super(memory, INFINITE_LOOP, 1);
      mStoryCreator = ConstantConditions.notNull("storyCreator", storyCreator);
    }

    @NotNull
    List<Actor> getInputActors() {
      if (mActors == null) {
        Setting.set(getSetting());
        Story<T> story;
        try {
          story = mStoryCreator.call();
          if (story == null) {
            story = ofEmpty();
          }

        } catch (final Throwable t) {
          story = ofSingleIncident(t);
          if (t instanceof InterruptedException) {
            Thread.currentThread().interrupt();
          }

        } finally {
          Setting.unset();
        }
        mActors = Collections.singletonList(story.getActor());
      }
      return mActors;
    }
  }

  private static class GenericStory<T, R> extends AbstractStory<R> {

    private final UnaryFunction<? super List<T>, ? extends Story<R>> mEffectHandler;
    private final List<Actor> mInputActors;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private GenericStory(@NotNull final Memory memory,
        @NotNull final Iterable<? extends Story<? extends T>> stories,
        @NotNull final NullaryFunction<? extends Event<? extends Boolean>> conditionHandler,
        @NotNull final UnaryFunction<? super List<T>, ? extends Story<R>> effectHandler) {
      super(memory, conditionHandler, Iterables.size(stories));
      final ArrayList<Actor> inputActors = new ArrayList<Actor>();
      for (final Story<? extends T> story : stories) {
        inputActors.add(story.getActor());
      }
      ConstantConditions.positive("stories size", inputActors.size());
      mInputActors = inputActors;
      mEffectHandler = ConstantConditions.notNull("effectHandler", effectHandler);
    }

    @NotNull
    List<Actor> getInputActors() {
      return mInputActors;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    Actor getOutputActor(@NotNull final Object[] inputs) throws Exception {
      Setting.set(getSetting());
      try {
        final ArrayList<T> inputList = new ArrayList<T>();
        for (final Object input : inputs) {
          inputList.add((T) input);
        }
        final Story<R> story = mEffectHandler.call(inputList);
        if ((story == null) || isEmpty(story)) {
          return null;
        }
        return story.getActor();

      } finally {
        Setting.unset();
      }
    }
  }

  private static class IncidentStory<T> extends Story<T> {

    private final Actor mActor;

    private IncidentStory(@NotNull final Throwable incident) {
      final Conflict conflict = new Conflict(incident);
      mActor = BackStage.newActor(new TrampolinePlayScript(Setting.get()) {

        private final HashSet<String> mThreads = new HashSet<String>();

        @NotNull
        @Override
        public Behavior getBehavior(@NotNull final String id) {
          return new AbstractBehavior() {

            public void onMessage(final Object message, @NotNull final Envelop envelop,
                @NotNull final Context context) {
              if (message == GET) {
                envelop.getSender()
                    .tell(conflict, envelop.getOptions().threadOnly(), context.getSelf());

              } else if (message == NEXT) {
                final HashSet<String> threads = mThreads;
                final Options options = envelop.getOptions().threadOnly();
                final String thread = options.getThread();
                if (!threads.contains(thread)) {
                  envelop.getSender().tell(conflict, options, context.getSelf());
                  threads.add(thread);

                } else {
                  envelop.getSender()
                      .tell(END, envelop.getOptions().threadOnly(), context.getSelf());
                }

              } else if (message == BREAK) {
                mThreads.remove(envelop.getOptions().getThread());
              }
              envelop.preventReceipt();
            }
          };
        }
      });
    }

    @NotNull
    Actor getActor() {
      return mActor;
    }
  }

  private static class IncidentsStory<T> extends Story<T> {

    private final Actor mActor;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private IncidentsStory(@NotNull final Iterable<? extends Throwable> incidents) {
      ConstantConditions.notNull("incidents", incidents);
      mActor = BackStage.newActor(new TrampolinePlayScript(Setting.get()) {

        private final HashMap<String, Iterator<? extends Throwable>> mThreads =
            new HashMap<String, Iterator<? extends Throwable>>();

        @NotNull
        @Override
        public Behavior getBehavior(@NotNull final String id) {
          return new AbstractBehavior() {

            public void onMessage(final Object message, @NotNull final Envelop envelop,
                @NotNull final Context context) {
              if (message == GET) {
                envelop.getSender()
                    .tell(new Conflict(Iterables.first(incidents)),
                        envelop.getOptions().threadOnly(), context.getSelf());

              } else if (message == NEXT) {
                final HashMap<String, Iterator<? extends Throwable>> threads = mThreads;
                final Options options = envelop.getOptions().threadOnly();
                final String thread = options.getThread();
                Iterator<? extends Throwable> iterator = threads.get(thread);
                if (iterator == null) {
                  iterator = incidents.iterator();
                  threads.put(thread, iterator);
                }
                envelop.getSender()
                    .tell(iterator.hasNext() ? new Conflict(iterator.next()) : END, options,
                        context.getSelf());

              } else if (message == BREAK) {
                mThreads.remove(envelop.getOptions().getThread());
              }
              envelop.preventReceipt();
            }
          };
        }
      });
    }

    @NotNull
    Actor getActor() {
      return mActor;
    }
  }

  private static class InputStreamIterable implements Iterable<ByteBuffer> {

    private final NullaryFunction<? extends ByteBuffer> mBufferCreator;
    private final InputStream mInputStream;
    private final Memory mMemory;
    private NullaryFunction<ByteBuffer> mReader;

    private InputStreamIterable(@NotNull final Memory memory,
        @NotNull final InputStream inputStream,
        @NotNull final NullaryFunction<? extends ByteBuffer> bufferCreator) {
      mMemory = ConstantConditions.notNull("memory", memory);
      mInputStream = ConstantConditions.notNull("inputStream", inputStream);
      mBufferCreator = ConstantConditions.notNull("bufferCreator", bufferCreator);
      if (inputStream instanceof FileInputStream) {
        mReader = new NullaryFunction<ByteBuffer>() {

          public ByteBuffer call() throws Exception {
            final ByteBuffer byteBuffer = mBufferCreator.call();
            final int read = ((FileInputStream) mInputStream).getChannel().read(byteBuffer);
            if (read > 0) {
              return byteBuffer;
            }
            mReader = new NullaryFunction<ByteBuffer>() {

              public ByteBuffer call() {
                return null;
              }
            };
            return null;
          }
        };

      } else {
        mReader = new NullaryFunction<ByteBuffer>() {

          public ByteBuffer call() throws Exception {
            final InputStream inputStream = mInputStream;
            final ByteBuffer byteBuffer = mBufferCreator.call();
            int read = 0;
            if (byteBuffer.hasArray()) {
              read = inputStream.read(byteBuffer.array());
              if (read > 0) {
                byteBuffer.position(read);
              }

            } else {
              while (byteBuffer.hasRemaining()) {
                byteBuffer.put((byte) inputStream.read());
                ++read;
              }
            }
            if (read > 0) {
              return byteBuffer;
            }
            mReader = new NullaryFunction<ByteBuffer>() {

              public ByteBuffer call() {
                return null;
              }
            };
            return null;
          }
        };
      }
    }

    @NotNull
    public Iterator<ByteBuffer> iterator() {
      return new InputStreamIterator();
    }

    private class InputStreamIterator implements Iterator<ByteBuffer> {

      private final Iterator<Object> mIterator;

      private InputStreamIterator() {
        mIterator = mMemory.iterator();
      }

      public boolean hasNext() {
        if (!mIterator.hasNext()) {
          try {
            final ByteBuffer byteBuffer = mReader.call();
            if (byteBuffer != null) {
              mMemory.put(byteBuffer);
            }

          } catch (final Exception e) {
            throw new IllegalStateException(e);
          }
        }
        return mIterator.hasNext();
      }

      public ByteBuffer next() {
        return (ByteBuffer) mIterator.next();
      }

      public void remove() {
        throw new UnsupportedOperationException("remove");
      }
    }
  }

  private static class JoinStory<T, R> extends Story<R> {

    private final Actor mActor;
    private final UnaryFunction<? super T, ? extends Event<R>> mEffectHandler;
    private final Actor mInputActor;
    private final Options mInputOptions;
    private final int mMaxConcurrency;
    private final int mMaxEventWindow;
    private final Memory mMemory;
    private final HashMap<String, SenderIterator> mNextSenders =
        new HashMap<String, SenderIterator>();
    private final Options mOptions;
    private final LinkedHashMap<Actor, OutputEffect> mOutputEffects =
        new LinkedHashMap<Actor, OutputEffect>();
    private final String mOutputThread;
    private final Setting mSetting;

    private int mActorCount;
    private HashMap<String, Sender> mGetSenders = new HashMap<String, Sender>();
    private Conflict mInputConflict;
    private boolean mInputsEnded;
    private boolean mInputsPending;
    private long mOutputCount;

    private JoinStory(@NotNull final Memory memory, @NotNull final Story<? extends T> story,
        final int maxConcurrency, final int maxEventWindow,
        @NotNull final UnaryFunction<? super T, ? extends Event<R>> effectHandler) {
      mMemory = ConstantConditions.notNull("memory", memory);
      mInputActor = story.getActor();
      mMaxConcurrency = ConstantConditions.positive("maxConcurrency", maxConcurrency);
      mMaxEventWindow = ConstantConditions.positive("maxEventWindow", maxEventWindow);
      mEffectHandler = ConstantConditions.notNull("effectHandler", effectHandler);
      final Setting setting = (mSetting = Setting.get());
      final String actorId = (mActor = BackStage.newActor(new PlayScript(setting) {

        @NotNull
        @Override
        public Behavior getBehavior(@NotNull final String id) {
          return new InitBehavior();
        }
      })).getId();
      final Options options = (mOptions = new Options().withReceiptId(actorId));
      mOutputThread = actorId + ":output";
      mInputOptions = options.withThread(actorId + ":input");
    }

    private void done(@NotNull final Context context) {
      final Memory memory = mMemory;
      Object effects = memory;
      for (final Object effect : memory) {
        if (effect instanceof Conflict) {
          effects = effect;
          break;
        }
      }
      final Actor self = context.getSelf();
      for (final Sender sender : mGetSenders.values()) {
        sender.getSender().tell(effects, sender.getOptions(), self);
      }
      mGetSenders = null;
      final HashMap<String, SenderIterator> nextSenders = mNextSenders;
      for (final SenderIterator sender : nextSenders.values()) {
        if (sender.isWaitNext() && !sender.tellNext(self)) {
          sender.getSender().tell(END, sender.getOptions(), self);
        }
      }
      context.setBehavior(new DoneBehavior(effects, memory, nextSenders));
    }

    private void fail(@NotNull final Conflict conflict, @NotNull final Context context) {
      final Actor self = context.getSelf();
      for (final Sender sender : mGetSenders.values()) {
        sender.getSender().tell(conflict, sender.getOptions(), self);
      }
      mGetSenders = null;
      final Memory memory = mMemory;
      memory.put(conflict);
      final HashMap<String, SenderIterator> nextSenders = mNextSenders;
      for (final SenderIterator sender : nextSenders.values()) {
        sender.tellNext(self);
      }
      context.setBehavior(new DoneBehavior(conflict, memory, nextSenders));
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private Actor getOutputActor(@NotNull final Object input) throws Exception {
      Setting.set(mSetting);
      try {
        final Event<R> event = mEffectHandler.call((T) input);
        return ((event != null) ? event : Event.ofNull()).getActor();

      } finally {
        Setting.unset();
      }
    }

    private class CancelBehavior extends AbstractBehavior {

      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          final Options options = envelop.getOptions().threadOnly();
          mGetSenders.put(options.getThread(), new Sender(envelop.getSender(), options));

        } else if (message == NEXT) {
          final Options options = envelop.getOptions();
          final String thread = options.getThread();
          final HashMap<String, SenderIterator> nextSenders = mNextSenders;
          SenderIterator sender = nextSenders.get(thread);
          if (sender != null) {
            sender.waitNext();

          } else {
            sender = new SenderIterator(envelop.getSender(), options.threadOnly());
            sender.setIterator(mMemory.iterator());
            nextSenders.put(thread, sender);
          }
          sender.tellNext(context.getSelf());

        } else if (message == BREAK) {
          mNextSenders.remove(envelop.getOptions().getThread());

        } else {
          final Options inputOptions = mInputOptions;
          if (isSameThread(inputOptions.getThread(), envelop.getOptions().getThread())) {
            mInputActor.tell(BREAK, inputOptions, context.getSelf());
            mInputsEnded = true;
            if (mActorCount == 0) {
              fail(Conflict.ofCancel(), context);
            }

          } else {
            final String thread = envelop.getOptions().getThread();
            if ((thread != null) && thread.startsWith(mOutputThread)) {
              final Actor sender = envelop.getSender();
              sender.tell(BREAK, mOutputEffects.get(sender).getOptions(), context.getSelf());
              if ((--mActorCount == 0) && mInputsEnded) {
                fail(Conflict.ofCancel(), context);
              }
            }
          }
        }
        envelop.preventReceipt();
      }
    }

    private class InitBehavior extends AbstractBehavior {

      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          final Options options = envelop.getOptions().threadOnly();
          mGetSenders.put(options.getThread(), new Sender(envelop.getSender(), options));
          mInputActor.tell(NEXT, mInputOptions, context.getSelf());
          context.setBehavior(new InputBehavior());

        } else if (message == NEXT) {
          final Options options = envelop.getOptions().threadOnly();
          final SenderIterator sender = new SenderIterator(envelop.getSender(), options);
          sender.setIterator(mMemory.iterator());
          mNextSenders.put(options.getThread(), sender);
          mInputActor.tell(NEXT, mInputOptions, context.getSelf());
          context.setBehavior(new InputBehavior());

        } else if (message == CANCEL) {
          mInputActor.tell(CANCEL, mInputOptions, context.getSelf());
          fail(Conflict.ofCancel(), context);
        }
        envelop.preventReceipt();
      }
    }

    private class InputBehavior extends AbstractBehavior {

      public void onMessage(Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          final Options options = envelop.getOptions().threadOnly();
          mGetSenders.put(options.getThread(), new Sender(envelop.getSender(), options));

        } else if (message == NEXT) {
          final Options options = envelop.getOptions();
          final String thread = options.getThread();
          final HashMap<String, SenderIterator> nextSenders = mNextSenders;
          SenderIterator sender = nextSenders.get(thread);
          if (sender != null) {
            sender.waitNext();

          } else {
            sender = new SenderIterator(envelop.getSender(), options.threadOnly());
            sender.setIterator(mMemory.iterator());
            nextSenders.put(thread, sender);
          }

        } else if (message == BREAK) {
          mNextSenders.remove(envelop.getOptions().getThread());

        } else if (message == CANCEL) {
          mInputActor.tell(CANCEL, mInputOptions, context.getSelf());
          context.setBehavior(new CancelBehavior());

        } else {
          final Options inputOptions = mInputOptions;
          if (isSameThread(inputOptions.getThread(), envelop.getOptions().getThread())) {
            if (message == END) {
              mInputActor.tell(BREAK, inputOptions, context.getSelf());
              done(context);

            } else if (message instanceof Conflict) {
              mInputActor.tell(BREAK, inputOptions, context.getSelf());
              fail((Conflict) message, context);

            } else if (message instanceof Bounce) {
              fail(Conflict.ofBounce((Bounce) message), context);

            } else {
              try {
                final Actor outputActor = getOutputActor(message);
                final String outputThreadId = mOutputThread + "#" + mOutputCount++;
                final Options options = mOptions.withThread(outputThreadId);
                final LinkedHashMap<Actor, OutputEffect> outputEffects = mOutputEffects;
                outputEffects.put(outputActor, new OutputEffect(options));
                outputActor.tell(GET, options, context.getSelf());
                if ((mInputsPending = (++mActorCount < mMaxConcurrency) && (outputEffects.size()
                    < mMaxEventWindow))) {
                  mInputActor.tell(NEXT, inputOptions, context.getSelf());
                }
                context.setBehavior(new OutputBehavior());

              } catch (final Throwable t) {
                mInputActor.tell(BREAK, inputOptions, context.getSelf());
                fail(new Conflict(t), context);
                if (t instanceof InterruptedException) {
                  Thread.currentThread().interrupt();
                }
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
          mGetSenders.put(options.getThread(), new Sender(envelop.getSender(), options));

        } else if (message == NEXT) {
          final Options options = envelop.getOptions();
          final String thread = options.getThread();
          final HashMap<String, SenderIterator> nextSenders = mNextSenders;
          SenderIterator sender = nextSenders.get(thread);
          if (sender != null) {
            sender.waitNext();

          } else {
            sender = new SenderIterator(envelop.getSender(), options.threadOnly());
            sender.setIterator(mMemory.iterator());
            nextSenders.put(thread, sender);
          }
          sender.tellNext(context.getSelf());

        } else if (message == BREAK) {
          mNextSenders.remove(envelop.getOptions().getThread());

        } else if (message == CANCEL) {
          final Actor self = context.getSelf();
          for (final Entry<Actor, OutputEffect> entry : mOutputEffects.entrySet()) {
            final OutputEffect outputEffect = entry.getValue();
            if (!outputEffect.isSet()) {
              entry.getKey().tell(CANCEL, outputEffect.getOptions(), self);
            }
          }
          mInputActor.tell(CANCEL, mInputOptions, self);
          context.setBehavior(new CancelBehavior());

        } else {
          final Options inputOptions = mInputOptions;
          if (isSameThread(inputOptions.getThread(), envelop.getOptions().getThread())) {
            if (message == END) {
              mInputActor.tell(BREAK, inputOptions, context.getSelf());
              mInputsEnded = true;
              if (mActorCount == 0) {
                done(context);
              }

            } else if (message instanceof Conflict) {
              mInputActor.tell(BREAK, inputOptions, context.getSelf());
              mInputsEnded = true;
              final Conflict conflict = (mInputConflict = (Conflict) message);
              if (mActorCount == 0) {
                fail(conflict, context);
              }

            } else if (message instanceof Bounce) {
              mInputsEnded = true;
              final Conflict conflict = (mInputConflict = Conflict.ofBounce((Bounce) message));
              if (mActorCount == 0) {
                fail(conflict, context);
              }

            } else {
              try {
                final Actor outputActor = getOutputActor(message);
                final String outputThreadId = mOutputThread + "#" + mOutputCount++;
                final Options options = mOptions.withThread(outputThreadId);
                final LinkedHashMap<Actor, OutputEffect> outputEffects = mOutputEffects;
                outputEffects.put(outputActor, new OutputEffect(options));
                outputActor.tell(GET, options, context.getSelf());
                if ((mInputsPending = (++mActorCount < mMaxConcurrency) && (outputEffects.size()
                    < mMaxEventWindow))) {
                  mInputActor.tell(NEXT, inputOptions, context.getSelf());
                }

              } catch (final Throwable t) {
                mInputsEnded = true;
                final Conflict conflict = (mInputConflict = new Conflict(t));
                if (mActorCount == 0) {
                  fail(conflict, context);
                }

                if (t instanceof InterruptedException) {
                  Thread.currentThread().interrupt();
                }
              }
            }

          } else {
            final String thread = envelop.getOptions().getThread();
            if ((thread != null) && thread.startsWith(mOutputThread)) {
              final Object effect;
              if (message instanceof Bounce) {
                effect = Conflict.ofBounce((Bounce) message);

              } else {
                effect = message;
              }
              // pass on conflicts
              @SuppressWarnings("UnnecessaryLocalVariable") final Memory memory = mMemory;
              final LinkedHashMap<Actor, OutputEffect> outputEffects = mOutputEffects;
              outputEffects.get(envelop.getSender()).set(effect);
              final Iterator<OutputEffect> iterator = outputEffects.values().iterator();
              while (iterator.hasNext()) {
                final OutputEffect outputEffect = iterator.next();
                if (outputEffect.isSet()) {
                  memory.put(outputEffect.getEffect());
                  iterator.remove();

                } else {
                  break;
                }
              }
              final Actor self = context.getSelf();
              for (final SenderIterator sender : mNextSenders.values()) {
                sender.tellNext(self);
              }

              if (outputEffects.size() < mMaxEventWindow) {
                if (!mInputsEnded) {
                  if (!mInputsPending) {
                    mInputsPending = true;
                    mInputActor.tell(NEXT, inputOptions, self);
                  }

                } else if (--mActorCount == 0) {
                  final Conflict conflict = mInputConflict;
                  if (conflict != null) {
                    fail(conflict, context);

                  } else {
                    done(context);
                  }
                }
              }
            }
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

  private static class LooperConditionHandler implements NullaryFunction<Event<? extends Boolean>> {

    private final StoryLooper<?, ?> mStoryLooper;

    LooperConditionHandler(@NotNull final StoryLooper<?, ?> storyLooper) {
      mStoryLooper = ConstantConditions.notNull("storyLooper", storyLooper);
    }

    public Event<? extends Boolean> call() throws Exception {
      return mStoryLooper.canContinue();
    }
  }

  private static class LooperEffectHandler<T, R> implements UnaryFunction<T, Story<R>> {

    private final StoryLooper<? super T, ? extends R> mStoryLooper;

    LooperEffectHandler(@NotNull final StoryLooper<? super T, ? extends R> storyLooper) {
      mStoryLooper = ConstantConditions.notNull("storyLooper", storyLooper);
    }

    @SuppressWarnings("unchecked")
    public Story<R> call(final T first) throws Exception {
      return (Story<R>) mStoryLooper.elaborate(first);
    }
  }

  private static class NarratorStory<T> extends Story<T> {

    private NarratorStory(@NotNull final Memory memory, @NotNull final Narrator<T> storyNarrator) {
      // TODO: 17/02/2019 implement
    }

    @NotNull
    Actor getActor() {
      return null;
    }
  }

  private static class OutputEffect {

    private final Options mOptions;
    private Object mEffect;
    private boolean mIsSet;

    private OutputEffect(@NotNull final Options options) {
      mOptions = options;
    }

    Object getEffect() {
      return mEffect;
    }

    @NotNull
    Options getOptions() {
      return mOptions;
    }

    boolean isSet() {
      return mIsSet;
    }

    void set(final Object effect) {
      mIsSet = true;
      mEffect = effect;
    }
  }

  private static class ResolveStory<T> extends AbstractStory<T> {

    private final UnaryFunction<? super Throwable, ? extends Story<T>> mIncidentHandler;
    private final Set<Class<? extends Throwable>> mIncidentTypes;
    private final List<Actor> mInputActors;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private ResolveStory(@NotNull final Memory memory, @NotNull final Story<? extends T> story,
        @NotNull final Set<Class<? extends Throwable>> incidentTypes,
        @NotNull final NullaryFunction<? extends Event<? extends Boolean>> conditionHandler,
        @NotNull final UnaryFunction<? super Throwable, ? extends Story<T>> incidentHandler) {
      super(memory, conditionHandler, 1);
      mInputActors = Collections.singletonList(story.getActor());
      ConstantConditions.positive("incidentTypes size", incidentTypes.size());
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
            final Story<T> story = mIncidentHandler.call(incident);
            return ((story != null) ? story : ofEmpty()).getActor();

          } finally {
            Setting.unset();
          }
        }
      }
      return super.getConflictActor(conflict);
    }

    @NotNull
    List<Actor> getInputActors() {
      return mInputActors;
    }
  }

  private static class ScheduleAtFixedRateStory<T> extends Story<T> implements Runnable {

    private final Actor mActor;
    private final long mInitialDelay;
    private final Actor mInputActor;
    private final Options mInputOptions;
    private final Memory mMemory;
    private final HashMap<String, SenderIterator> mNextSenders =
        new HashMap<String, SenderIterator>();
    private final long mPeriod;
    private final TimeUnit mUnit;

    private HashMap<String, Sender> mGetSenders = new HashMap<String, Sender>();
    private boolean mInputsPending;
    private ScheduledFuture<?> mScheduledFuture;

    private ScheduleAtFixedRateStory(@NotNull final Memory memory,
        @NotNull final Story<? extends T> story, final long initialDelay, final long period,
        @NotNull final TimeUnit unit) {
      mMemory = ConstantConditions.notNull("memory", memory);
      mInputActor = story.getActor();
      mInitialDelay = Math.max(initialDelay, 0);
      mPeriod = ConstantConditions.positive("period", period);
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
      mInputsPending = true;
      mInputActor.tell(NEXT, mInputOptions, mActor);
    }

    private void done(@NotNull final Context context) {
      final ScheduledFuture<?> scheduledFuture = mScheduledFuture;
      if (scheduledFuture != null) {
        scheduledFuture.cancel(false);
      }
      final Memory memory = mMemory;
      Object effects = memory;
      for (final Object effect : memory) {
        if (effect instanceof Conflict) {
          effects = effect;
          break;
        }
      }
      final Actor self = context.getSelf();
      for (final Sender sender : mGetSenders.values()) {
        sender.getSender().tell(effects, sender.getOptions(), self);
      }
      mGetSenders = null;
      final HashMap<String, SenderIterator> nextSenders = mNextSenders;
      for (final SenderIterator sender : nextSenders.values()) {
        if (sender.isWaitNext() && !sender.tellNext(self)) {
          sender.getSender().tell(END, sender.getOptions(), self);
        }
      }
      context.setBehavior(new DoneBehavior(effects, memory, nextSenders));
    }

    private void fail(@NotNull final Conflict conflict, @NotNull final Context context) {
      final ScheduledFuture<?> scheduledFuture = mScheduledFuture;
      if (scheduledFuture != null) {
        scheduledFuture.cancel(false);
      }
      final Actor self = context.getSelf();
      for (final Sender sender : mGetSenders.values()) {
        sender.getSender().tell(conflict, sender.getOptions(), self);
      }
      mGetSenders = null;
      final Memory memory = mMemory;
      memory.put(conflict);
      final HashMap<String, SenderIterator> nextSenders = mNextSenders;
      for (final SenderIterator sender : nextSenders.values()) {
        sender.tellNext(self);
      }
      context.setBehavior(new DoneBehavior(conflict, memory, nextSenders));
    }

    private class CancelBehavior extends AbstractBehavior {

      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          final Options options = envelop.getOptions().threadOnly();
          mGetSenders.put(options.getThread(), new Sender(envelop.getSender(), options));

        } else if (message == NEXT) {
          final Options options = envelop.getOptions();
          final String thread = options.getThread();
          final HashMap<String, SenderIterator> nextSenders = mNextSenders;
          SenderIterator sender = nextSenders.get(thread);
          if (sender != null) {
            sender.waitNext();

          } else {
            sender = new SenderIterator(envelop.getSender(), options.threadOnly());
            sender.setIterator(mMemory.iterator());
            nextSenders.put(thread, sender);
          }
          sender.tellNext(context.getSelf());

        } else if (message == BREAK) {
          mNextSenders.remove(envelop.getOptions().getThread());

        } else {
          final Options inputOptions = mInputOptions;
          if (isSameThread(inputOptions.getThread(), envelop.getOptions().getThread())) {
            mInputActor.tell(BREAK, inputOptions, context.getSelf());
            fail(Conflict.ofCancel(), context);
          }
        }
        envelop.preventReceipt();
      }
    }

    private class InitBehavior extends AbstractBehavior {

      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          final Options options = envelop.getOptions().threadOnly();
          mGetSenders.put(options.getThread(), new Sender(envelop.getSender(), options));
          mScheduledFuture = context.getScheduledExecutor()
              .scheduleAtFixedRate(ScheduleAtFixedRateStory.this, mInitialDelay, mPeriod, mUnit);
          context.setBehavior(new InputBehavior());

        } else if (message == NEXT) {
          final Options options = envelop.getOptions().threadOnly();
          final SenderIterator sender = new SenderIterator(envelop.getSender(), options);
          sender.setIterator(mMemory.iterator());
          mNextSenders.put(options.getThread(), sender);
          mScheduledFuture = context.getScheduledExecutor()
              .scheduleAtFixedRate(ScheduleAtFixedRateStory.this, mInitialDelay, mPeriod, mUnit);
          context.setBehavior(new InputBehavior());

        } else if (message == CANCEL) {
          mInputActor.tell(CANCEL, mInputOptions, context.getSelf());
          fail(Conflict.ofCancel(), context);
        }
        envelop.preventReceipt();
      }
    }

    private class InputBehavior extends AbstractBehavior {

      public void onMessage(Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          final Options options = envelop.getOptions().threadOnly();
          mGetSenders.put(options.getThread(), new Sender(envelop.getSender(), options));

        } else if (message == NEXT) {
          final Options options = envelop.getOptions();
          final String thread = options.getThread();
          final HashMap<String, SenderIterator> nextSenders = mNextSenders;
          SenderIterator sender = nextSenders.get(thread);
          if (sender != null) {
            sender.waitNext();

          } else {
            sender = new SenderIterator(envelop.getSender(), options.threadOnly());
            sender.setIterator(mMemory.iterator());
            nextSenders.put(thread, sender);
          }
          sender.tellNext(context.getSelf());

        } else if (message == BREAK) {
          mNextSenders.remove(envelop.getOptions().getThread());

        } else if (message == CANCEL) {
          mInputActor.tell(CANCEL, mInputOptions, context.getSelf());
          if (mInputsPending) {
            context.setBehavior(new CancelBehavior());

          } else {
            fail(Conflict.ofCancel(), context);
          }

        } else {
          final Options inputOptions = mInputOptions;
          if (isSameThread(inputOptions.getThread(), envelop.getOptions().getThread())) {
            if (message == END) {
              mInputActor.tell(BREAK, inputOptions, context.getSelf());
              done(context);

            } else if (message instanceof Conflict) {
              mInputActor.tell(BREAK, inputOptions, context.getSelf());
              fail((Conflict) message, context);

            } else if (message instanceof Bounce) {
              fail(Conflict.ofBounce((Bounce) message), context);

            } else {
              mMemory.put(message);
              final Actor self = context.getSelf();
              for (final SenderIterator sender : mNextSenders.values()) {
                sender.tellNext(self);
              }
            }
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

  private static class ScheduleWithFixedDelayStory<T> extends Story<T> implements Runnable {

    private final Actor mActor;
    private final long mDelay;
    private final long mInitialDelay;
    private final Actor mInputActor;
    private final Options mInputOptions;
    private final Memory mMemory;
    private final HashMap<String, SenderIterator> mNextSenders =
        new HashMap<String, SenderIterator>();
    private final TimeUnit mUnit;

    private HashMap<String, Sender> mGetSenders = new HashMap<String, Sender>();
    private boolean mInputsPending;
    private ScheduledFuture<?> mScheduledFuture;

    private ScheduleWithFixedDelayStory(@NotNull final Memory memory,
        @NotNull final Story<? extends T> story, final long initialDelay, final long delay,
        @NotNull final TimeUnit unit) {
      mMemory = ConstantConditions.notNull("memory", memory);
      mInputActor = story.getActor();
      mInitialDelay = Math.max(initialDelay, 0);
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

    private void done(@NotNull final Context context) {
      final ScheduledFuture<?> scheduledFuture = mScheduledFuture;
      if (scheduledFuture != null) {
        scheduledFuture.cancel(false);
      }
      final Memory memory = mMemory;
      Object effects = memory;
      for (final Object effect : memory) {
        if (effect instanceof Conflict) {
          effects = effect;
          break;
        }
      }
      final Actor self = context.getSelf();
      for (final Sender sender : mGetSenders.values()) {
        sender.getSender().tell(effects, sender.getOptions(), self);
      }
      mGetSenders = null;
      final HashMap<String, SenderIterator> nextSenders = mNextSenders;
      for (final SenderIterator sender : nextSenders.values()) {
        if (sender.isWaitNext() && !sender.tellNext(self)) {
          sender.getSender().tell(END, sender.getOptions(), self);
        }
      }
      context.setBehavior(new DoneBehavior(effects, memory, nextSenders));
    }

    private void fail(@NotNull final Conflict conflict, @NotNull final Context context) {
      final ScheduledFuture<?> scheduledFuture = mScheduledFuture;
      if (scheduledFuture != null) {
        scheduledFuture.cancel(false);
      }
      final Actor self = context.getSelf();
      for (final Sender sender : mGetSenders.values()) {
        sender.getSender().tell(conflict, sender.getOptions(), self);
      }
      mGetSenders = null;
      final Memory memory = mMemory;
      memory.put(conflict);
      final HashMap<String, SenderIterator> nextSenders = mNextSenders;
      for (final SenderIterator sender : nextSenders.values()) {
        sender.tellNext(self);
      }
      context.setBehavior(new DoneBehavior(conflict, memory, nextSenders));
    }

    private class CancelBehavior extends AbstractBehavior {

      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          final Options options = envelop.getOptions().threadOnly();
          mGetSenders.put(options.getThread(), new Sender(envelop.getSender(), options));

        } else if (message == NEXT) {
          final Options options = envelop.getOptions();
          final String thread = options.getThread();
          final HashMap<String, SenderIterator> nextSenders = mNextSenders;
          SenderIterator sender = nextSenders.get(thread);
          if (sender != null) {
            sender.waitNext();

          } else {
            sender = new SenderIterator(envelop.getSender(), options.threadOnly());
            sender.setIterator(mMemory.iterator());
            nextSenders.put(thread, sender);
          }
          sender.tellNext(context.getSelf());

        } else if (message == BREAK) {
          mNextSenders.remove(envelop.getOptions().getThread());

        } else {
          final Options inputOptions = mInputOptions;
          if (isSameThread(inputOptions.getThread(), envelop.getOptions().getThread())) {
            mInputActor.tell(BREAK, inputOptions, context.getSelf());
            fail(Conflict.ofCancel(), context);
          }
        }
        envelop.preventReceipt();
      }
    }

    private class InitBehavior extends AbstractBehavior {

      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          final Options options = envelop.getOptions().threadOnly();
          mGetSenders.put(options.getThread(), new Sender(envelop.getSender(), options));
          mScheduledFuture = context.getScheduledExecutor()
              .schedule(ScheduleWithFixedDelayStory.this, mInitialDelay, mUnit);
          context.setBehavior(new InputBehavior());

        } else if (message == NEXT) {
          final Options options = envelop.getOptions().threadOnly();
          final SenderIterator sender = new SenderIterator(envelop.getSender(), options);
          sender.setIterator(mMemory.iterator());
          mNextSenders.put(options.getThread(), sender);
          mScheduledFuture = context.getScheduledExecutor()
              .schedule(ScheduleWithFixedDelayStory.this, mInitialDelay, mUnit);
          context.setBehavior(new InputBehavior());

        } else if (message == CANCEL) {
          mInputActor.tell(CANCEL, mInputOptions, context.getSelf());
          fail(Conflict.ofCancel(), context);
        }
        envelop.preventReceipt();
      }
    }

    private class InputBehavior extends AbstractBehavior {

      public void onMessage(Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          final Options options = envelop.getOptions().threadOnly();
          mGetSenders.put(options.getThread(), new Sender(envelop.getSender(), options));

        } else if (message == NEXT) {
          final Options options = envelop.getOptions();
          final String thread = options.getThread();
          final HashMap<String, SenderIterator> nextSenders = mNextSenders;
          SenderIterator sender = nextSenders.get(thread);
          if (sender != null) {
            sender.waitNext();

          } else {
            sender = new SenderIterator(envelop.getSender(), options.threadOnly());
            sender.setIterator(mMemory.iterator());
            nextSenders.put(thread, sender);
          }
          sender.tellNext(context.getSelf());

        } else if (message == BREAK) {
          mNextSenders.remove(envelop.getOptions().getThread());

        } else if (message == CANCEL) {
          mInputActor.tell(CANCEL, mInputOptions, context.getSelf());
          if (mInputsPending) {
            context.setBehavior(new CancelBehavior());

          } else {
            fail(Conflict.ofCancel(), context);
          }

        } else {
          final Options inputOptions = mInputOptions;
          if (isSameThread(inputOptions.getThread(), envelop.getOptions().getThread())) {
            if (message == END) {
              mInputActor.tell(BREAK, inputOptions, context.getSelf());
              done(context);

            } else if (message instanceof Conflict) {
              mInputActor.tell(BREAK, inputOptions, context.getSelf());
              fail((Conflict) message, context);

            } else if (message instanceof Bounce) {
              fail(Conflict.ofBounce((Bounce) message), context);

            } else {
              mMemory.put(message);
              final Actor self = context.getSelf();
              for (final SenderIterator sender : mNextSenders.values()) {
                sender.tellNext(self);
              }
              mScheduledFuture = context.getScheduledExecutor()
                  .schedule(ScheduleWithFixedDelayStory.this, mDelay, mUnit);
            }
          }
        }
        envelop.preventReceipt();
      }
    }

    public void run() {
      mInputsPending = true;
      mInputActor.tell(NEXT, mInputOptions, mActor);
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

  private static class SenderIterator extends Sender {

    private Iterator<?> mIterator;
    private boolean mWaitNext = true;

    private SenderIterator(@NotNull final Actor sender, @NotNull final Options options) {
      super(sender, options);
    }

    boolean isWaitNext() {
      return mWaitNext;
    }

    void setIterator(@NotNull final Iterator<?> iterator) {
      mIterator = iterator;
    }

    boolean tellNext(@NotNull final Actor self) {
      final Iterator<?> iterator = mIterator;
      if (mWaitNext && iterator.hasNext()) {
        mWaitNext = false;
        getSender().tell(iterator.next(), getOptions(), self);
        return true;
      }
      return false;
    }

    void waitNext() {
      mWaitNext = true;
    }
  }

  private static class UnaryStory<T1, R> extends AbstractStory<R> {

    private final UnaryFunction<? super T1, ? extends Story<R>> mEffectHandler;
    private final List<Actor> mInputActors;

    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    private UnaryStory(@NotNull final Memory memory, @NotNull final Story<? extends T1> firstStory,
        @NotNull final NullaryFunction<? extends Event<? extends Boolean>> conditionHandler,
        @NotNull final UnaryFunction<? super T1, ? extends Story<R>> effectHandler) {
      super(memory, conditionHandler, 1);
      mInputActors = Arrays.asList(firstStory.getActor());
      mEffectHandler = ConstantConditions.notNull("effectHandler", effectHandler);
    }

    @NotNull
    List<Actor> getInputActors() {
      return mInputActors;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    Actor getOutputActor(@NotNull final Object[] inputs) throws Exception {
      Setting.set(getSetting());
      try {
        final Story<R> story = mEffectHandler.call((T1) inputs[0]);
        if ((story == null) || isEmpty(story)) {
          return null;
        }
        return story.getActor();

      } finally {
        Setting.unset();
      }
    }
  }

  private static class UnfoldStory<T> extends Story<T> {

    private final Actor mActor;
    private final Actor mInputActor;
    private final Options mInputOptions;
    private final String mInputThreadId;
    private final HashMap<String, SenderIterator> mNextSenders =
        new HashMap<String, SenderIterator>();

    private HashMap<String, Sender> mGetSenders = new HashMap<String, Sender>();

    private UnfoldStory(@NotNull final Event<? extends Iterable<T>> event) {
      mInputActor = event.getActor();
      final String actorId = (mActor = BackStage.newActor(new TrampolinePlayScript(Setting.get()) {

        @NotNull
        @Override
        public Behavior getBehavior(@NotNull final String id) {
          return new InitBehavior();
        }
      })).getId();
      mInputThreadId = actorId;
      mInputOptions = new Options().withReceiptId(actorId).withThread(actorId);
    }

    private void fail(@NotNull final Conflict conflict, @NotNull final Context context) {
      final Actor self = context.getSelf();
      for (final Sender sender : mGetSenders.values()) {
        sender.getSender().tell(conflict, sender.getOptions(), self);
      }
      mGetSenders = null;
      final SingletonMemory memory = new SingletonMemory();
      memory.put(conflict);
      final HashMap<String, SenderIterator> nextSenders = mNextSenders;
      for (final SenderIterator sender : nextSenders.values()) {
        sender.setIterator(memory.iterator());
        sender.tellNext(self);
      }
      context.setBehavior(new DoneBehavior(conflict, memory, nextSenders));
    }

    private class CancelBehavior extends AbstractBehavior {

      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          final Options options = envelop.getOptions().threadOnly();
          mGetSenders.put(options.getThread(), new Sender(envelop.getSender(), options));

        } else if (message == NEXT) {
          final Options options = envelop.getOptions();
          final String thread = options.getThread();
          final HashMap<String, SenderIterator> nextSenders = mNextSenders;
          final SenderIterator sender = nextSenders.get(thread);
          if (sender != null) {
            sender.waitNext();

          } else {
            nextSenders.put(thread, new SenderIterator(envelop.getSender(), options.threadOnly()));
          }

        } else if (message == BREAK) {
          mNextSenders.remove(envelop.getOptions().getThread());

        } else if (mInputThreadId.equals(envelop.getOptions().getThread())) {
          fail(Conflict.ofCancel(), context);
        }
      }
    }

    private class InitBehavior extends AbstractBehavior {

      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          final Options options = envelop.getOptions().threadOnly();
          mGetSenders.put(options.getThread(), new Sender(envelop.getSender(), options));
          mInputActor.tell(GET, mInputOptions, context.getSelf());
          context.setBehavior(new InputBehavior());

        } else if (message == NEXT) {
          final Options options = envelop.getOptions().threadOnly();
          mNextSenders.put(options.getThread(), new SenderIterator(envelop.getSender(), options));
          mInputActor.tell(GET, mInputOptions, context.getSelf());
          context.setBehavior(new InputBehavior());

        } else if (message == CANCEL) {
          mInputActor.tell(CANCEL, mInputOptions, context.getSelf());
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
          mGetSenders.put(options.getThread(), new Sender(envelop.getSender(), options));

        } else if (message == NEXT) {
          final Options options = envelop.getOptions();
          final String thread = options.getThread();
          final HashMap<String, SenderIterator> nextSenders = mNextSenders;
          final SenderIterator sender = nextSenders.get(thread);
          if (sender != null) {
            sender.waitNext();

          } else {
            nextSenders.put(thread, new SenderIterator(envelop.getSender(), options.threadOnly()));
          }

        } else if (message == BREAK) {
          mNextSenders.remove(envelop.getOptions().getThread());

        } else if (message == CANCEL) {
          mInputActor.tell(CANCEL, mInputOptions, context.getSelf());
          context.setBehavior(new CancelBehavior());

        } else if (mInputThreadId.equals(envelop.getOptions().getThread())) {
          if (message instanceof Conflict) {
            fail((Conflict) message, context);

          } else if (message instanceof Bounce) {
            fail(Conflict.ofBounce((Bounce) message), context);

          } else {
            final Actor self = context.getSelf();
            final Iterable<?> effects = (Iterable<?>) message;
            for (final Sender sender : mGetSenders.values()) {
              sender.getSender().tell(effects, sender.getOptions(), self);
            }
            mGetSenders = null;
            for (final SenderIterator sender : mNextSenders.values()) {
              sender.setIterator(effects.iterator());
              sender.tellNext(self);
            }
            context.setBehavior(new DoneBehavior(effects, effects, mNextSenders));
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

  private static class WatchStory<T> extends AbstractStory<T> {

    private final EventObserver<? super T> mEventObserver;
    private final List<Actor> mInputActors;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private WatchStory(@NotNull final Memory memory, @NotNull final Story<? extends T> story,
        @NotNull final EventObserver<? super T> eventObserver) {
      super(memory, INFINITE_LOOP, 1);
      mInputActors = Collections.singletonList(story.getActor());
      mEventObserver = ConstantConditions.notNull("eventObserver", eventObserver);
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

    @NotNull
    List<Actor> getInputActors() {
      return mInputActors;
    }
  }
}
