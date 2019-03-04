package dm.shakespeare.plot;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import dm.shakespeare.BackStage;
import dm.shakespeare.actor.AbstractBehavior;
import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Behavior.Context;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.Options;
import dm.shakespeare.actor.Script;
import dm.shakespeare.concurrent.ExecutorServices;
import dm.shakespeare.function.Observer;
import dm.shakespeare.message.Bounce;
import dm.shakespeare.message.Receipt;
import dm.shakespeare.plot.Setting.Cache;
import dm.shakespeare.plot.function.Action;
import dm.shakespeare.plot.function.NullaryFunction;
import dm.shakespeare.plot.function.UnaryFunction;
import dm.shakespeare.plot.memory.ListMemory;
import dm.shakespeare.plot.memory.SingletonMemory;
import dm.shakespeare.plot.narrator.Narrator;
import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare.util.Iterables;

/**
 * Created by davide-maestroni on 01/22/2019.
 */
public abstract class Story<T> extends Event<Iterable<T>> {

  // TODO: 05/02/2019 PROGRESS???
  // TODO: 15/02/2019 untriggered actors, serialization
  // TODO: 28/02/2019 isBoundless()?, mustache, swagger converter

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
  public static <T> Story<T> ofEffects(@NotNull final Iterable<? extends T> effects) {
    final Cache cache = Setting.get().getCache(Story.class);
    if ((effects == Collections.EMPTY_LIST) || (effects == Collections.EMPTY_SET)) {
      Story<T> story = cache.get(effects);
      if (story == null) {
        story = new EffectsStory<T>(effects);
        cache.put(effects, story);
      }
    }
    return new EffectsStory<T>(effects);
  }

  @NotNull
  public static <T> Story<T> ofEffects(@NotNull final NullaryFunction<? extends T> effectsCreator) {
    return ofEffects(new CreatorIterable<T>(effectsCreator));
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
  public static <T> Story<T> ofIncidents(
      @NotNull final NullaryFunction<? extends Throwable> incidentsCreator) {
    return ofIncidents(new CreatorIterable<Throwable>(incidentsCreator));
  }

  @NotNull
  public static Story<ByteBuffer> ofInputStream(@NotNull final InputStream inputStream) {
    return ofInputStream(inputStream, DEFAULT_BUFFER_CREATOR, new ListMemory());
  }

  @NotNull
  public static Story<ByteBuffer> ofInputStream(@NotNull final InputStream inputStream,
      @NotNull final Memory memory) {
    return ofInputStream(inputStream, DEFAULT_BUFFER_CREATOR, memory);
  }

  @NotNull
  public static Story<ByteBuffer> ofInputStream(@NotNull final InputStream inputStream,
      @NotNull final NullaryFunction<? extends ByteBuffer> bufferCreator) {
    return ofInputStream(inputStream, bufferCreator, new ListMemory());
  }

  @NotNull
  public static Story<ByteBuffer> ofInputStream(@NotNull final InputStream inputStream,
      @NotNull final NullaryFunction<? extends ByteBuffer> bufferCreator,
      @NotNull final Memory memory) {
    return new EffectsStory<ByteBuffer>(
        new InputStreamIterable(inputStream, bufferCreator, memory));
  }

  @NotNull
  public static <T> StoryNarrator<T> ofNarrations() {
    return ofNarrations(new LinkedBlockingQueue<Object>());
  }

  @NotNull
  public static <T> StoryNarrator<T> ofNarrations(@NotNull final BlockingQueue<Object> queue) {
    return ofNarrations(queue, new ListMemory());
  }

  @NotNull
  public static <T> StoryNarrator<T> ofNarrations(@NotNull final BlockingQueue<Object> queue,
      @NotNull final Memory memory) {
    return new StoryNarrator<T>(queue, memory);
  }

  @NotNull
  public static <T> StoryNarrator<T> ofNarrations(@NotNull final Memory memory) {
    return ofNarrations(new LinkedBlockingQueue<Object>(), memory);
  }

  @NotNull
  public static <T> Story<T> ofSingleEffect(final T effect) {
    return new EffectStory<T>(effect);
  }

  @NotNull
  public static <T> Story<T> ofSingleEvent(@NotNull final Event<? extends T> event) {
    return new EventStory<T>(event);
  }

  @NotNull
  public static <T> Story<T> ofSingleIncident(@NotNull final Throwable incident) {
    return new IncidentStory<T>(incident);
  }

  @NotNull
  public static <T> Story<T> ofStory(
      @NotNull final NullaryFunction<? extends Story<? extends T>> storyCreator) {
    return ofStory(storyCreator, new ListMemory());
  }

  @NotNull
  public static <T> Story<T> ofStory(
      @NotNull final NullaryFunction<? extends Story<? extends T>> storyCreator,
      @NotNull final Memory memory) {
    return new FunctionStory<T>(storyCreator, memory);
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public static <T> Story<T> unfold(@NotNull final Event<? extends Iterable<? extends T>> event) {
    if (event instanceof Story) {
      return (Story<T>) event;
    }
    return new UnfoldStory<T>(event);
  }

  @NotNull
  public static <T, R> Story<R> when(@NotNull final Iterable<? extends Story<? extends T>> stories,
      @NotNull final NullaryFunction<? extends Event<? extends Boolean>> conditionHandler,
      @NotNull final UnaryFunction<? super List<T>, ? extends Story<? extends R>> effectHandler) {
    return when(stories, conditionHandler, effectHandler, new ListMemory());
  }

  @NotNull
  public static <T, R> Story<R> when(@NotNull final Iterable<? extends Story<? extends T>> stories,
      @NotNull final NullaryFunction<? extends Event<? extends Boolean>> conditionHandler,
      @NotNull final UnaryFunction<? super List<T>, ? extends Story<? extends R>> effectHandler,
      @NotNull final Memory memory) {
    return new GenericStory<T, R>(stories, conditionHandler, effectHandler, memory);
  }

  @NotNull
  public static <T, R> Story<R> when(@NotNull final Iterable<? extends Story<? extends T>> stories,
      @NotNull final StoryEvolver<? super List<T>, ? extends R> storyEvolver) {
    return when(stories, new LooperConditionHandler(storyEvolver),
        new LooperEffectHandler<List<T>, R>(storyEvolver));
  }

  @NotNull
  public static <T, R> Story<R> when(@NotNull final Iterable<? extends Story<? extends T>> stories,
      @NotNull final StoryEvolver<? super List<T>, ? extends R> storyEvolver,
      @NotNull final Memory memory) {
    return when(stories, new LooperConditionHandler(storyEvolver),
        new LooperEffectHandler<List<T>, R>(storyEvolver), memory);
  }

  @NotNull
  public static <T1, R> Story<R> when(@NotNull final Story<? extends T1> firstStory,
      @NotNull final NullaryFunction<? extends Event<? extends Boolean>> conditionHandler,
      @NotNull final UnaryFunction<? super T1, ? extends Story<? extends R>> effectHandler) {
    return when(firstStory, conditionHandler, effectHandler, new ListMemory());
  }

  @NotNull
  public static <T1, R> Story<R> when(@NotNull final Story<? extends T1> firstStory,
      @NotNull final NullaryFunction<? extends Event<? extends Boolean>> conditionHandler,
      @NotNull final UnaryFunction<? super T1, ? extends Story<? extends R>> effectHandler,
      @NotNull final Memory memory) {
    return new UnaryStory<T1, R>(firstStory, conditionHandler, effectHandler, memory);
  }

  static void done(@NotNull final Memory memory, @NotNull final Map<String, Sender> getSenders,
      @NotNull final Map<String, SenderIterator> nextSenders, @NotNull final Context context) {
    Object effects = memory;
    for (final Object effect : memory) {
      if (effect instanceof Conflict) {
        effects = effect;
        break;
      }
    }
    final Actor self = context.getSelf();
    for (final Sender sender : getSenders.values()) {
      sender.getSender().tell(effects, sender.getOptions(), self);
    }
    for (final SenderIterator sender : nextSenders.values()) {
      if (sender.isWaitNext() && !sender.tellNext(self)) {
        sender.getSender().tell(END, sender.getOptions(), self);
      }
    }
    context.setBehavior(new DoneBehavior(effects, memory, nextSenders));
  }

  static void fail(@NotNull final Conflict conflict, @NotNull final Memory memory,
      @NotNull final Map<String, Sender> getSenders,
      @NotNull final Map<String, SenderIterator> nextSenders, @NotNull final Context context) {
    final Actor self = context.getSelf();
    for (final Sender sender : getSenders.values()) {
      sender.getSender().tell(conflict, sender.getOptions(), self);
    }
    memory.put(conflict);
    for (final SenderIterator sender : nextSenders.values()) {
      sender.tellNext(self);
    }
    context.setBehavior(new DoneBehavior(conflict, memory, nextSenders));
  }

  private static boolean isEmpty(@Nullable final Story<?> story) {
    final Cache cache = Setting.get().getCache(Story.class);
    return (cache.get(Collections.EMPTY_LIST) == story) || (cache.get(Collections.EMPTY_SET)
        == story);
  }

  private static boolean isSame(@Nullable final Object expected, @Nullable final Object actual) {
    return (expected == actual) || (expected != null) && expected.equals(actual);
  }

  @NotNull
  public <R> Story<R> blend(final int maxConcurrency,
      @NotNull final UnaryFunction<? super T, ? extends Story<? extends R>> effectHandler) {
    return blend(maxConcurrency, effectHandler, new ListMemory());
  }

  @NotNull
  public <R> Story<R> blend(final int maxConcurrency,
      @NotNull final UnaryFunction<? super T, ? extends Story<? extends R>> effectHandler,
      @NotNull final Memory memory) {
    return new BlendStory<T, R>(this, maxConcurrency, effectHandler, memory);
  }

  @NotNull
  @Override
  public Story<T> eventually(@NotNull final Action eventualAction) {
    return eventually(eventualAction, new ListMemory());
  }

  @NotNull
  public Story<T> eventually(@NotNull final Action eventualAction, @NotNull final Memory memory) {
    return new EventualStory<T>(this, eventualAction, memory);
  }

  @NotNull
  public <R> Story<R> forAll(
      @NotNull final UnaryFunction<? super T, ? extends Story<? extends R>> effectHandler) {
    return forAll(effectHandler, new ListMemory());
  }

  @NotNull
  public <R> Story<R> forAll(
      @NotNull final UnaryFunction<? super T, ? extends Story<? extends R>> effectHandler,
      @NotNull final Memory memory) {
    return new AllStory<T, R>(this, effectHandler, memory);
  }

  @NotNull
  public <R> Story<R> forWhile(
      @NotNull final NullaryFunction<? extends Event<? extends Boolean>> conditionHandler,
      @NotNull final UnaryFunction<? super T, ? extends Story<? extends R>> effectHandler) {
    return when(this, conditionHandler, effectHandler);
  }

  @NotNull
  public <R> Story<R> forWhile(
      @NotNull final NullaryFunction<? extends Event<? extends Boolean>> conditionHandler,
      @NotNull final UnaryFunction<? super T, ? extends Story<? extends R>> effectHandler,
      @NotNull final Memory memory) {
    return when(this, conditionHandler, effectHandler, memory);
  }

  @NotNull
  public <R> Story<R> forWhile(@NotNull final StoryEvolver<? super T, ? extends R> storyEvolver) {
    return forWhile(new LooperConditionHandler(storyEvolver),
        new LooperEffectHandler<T, R>(storyEvolver));
  }

  @NotNull
  public <R> Story<R> forWhile(@NotNull final StoryEvolver<? super T, ? extends R> storyEvolver,
      @NotNull final Memory memory) {
    return forWhile(new LooperConditionHandler(storyEvolver),
        new LooperEffectHandler<T, R>(storyEvolver), memory);
  }

  @NotNull
  public <R> Story<R> join(final int maxConcurrency, final int maxEventWindow,
      @NotNull final UnaryFunction<? super T, ? extends Event<? extends R>> effectHandler) {
    return join(maxConcurrency, maxEventWindow, effectHandler, new ListMemory());
  }

  @NotNull
  public <R> Story<R> join(final int maxConcurrency, final int maxEventWindow,
      @NotNull final UnaryFunction<? super T, ? extends Event<? extends R>> effectHandler,
      @NotNull final Memory memory) {
    return new JoinStory<T, R>(this, maxConcurrency, maxEventWindow, effectHandler, memory);
  }

  public void playEach(@NotNull final EventObserver<? super T> eventObserver) {
    playEach(new EventStoryObserver<T>(eventObserver));
  }

  public void playEach(@Nullable final Observer<? super T> effectObserver,
      @Nullable final Observer<? super Throwable> incidentObserver,
      @Nullable final Action endAction) {
    playEach(new DefaultStoryObserver<T>(effectObserver, incidentObserver, endAction));
  }

  public void playEach(@NotNull final StoryObserver<? super T> storyObserver) {
    final Actor actor = BackStage.newActor(new StoryObserverScript<T>(storyObserver));
    final String actorId = actor.getId();
    getActor().tell(NEXT, new Options().withReceiptId(actorId).withThread(actorId), actor);
  }

  @NotNull
  public <E1 extends Throwable, E2 extends Throwable, E3 extends Throwable> Story<T> resolve(
      @NotNull final Class<? extends E1> firstType, @NotNull final Class<? extends E2> secondType,
      @NotNull final Class<? extends E3> thirdType,
      @NotNull final NullaryFunction<? extends Event<? extends Boolean>> conditionHandler,
      @NotNull final UnaryFunction<? super Throwable, ? extends Story<? extends T>> incidentHandler) {
    return resolve(firstType, secondType, thirdType, conditionHandler, incidentHandler,
        new ListMemory());
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <E1 extends Throwable, E2 extends Throwable, E3 extends Throwable> Story<T> resolve(
      @NotNull final Class<? extends E1> firstType, @NotNull final Class<? extends E2> secondType,
      @NotNull final Class<? extends E3> thirdType,
      @NotNull final NullaryFunction<? extends Event<? extends Boolean>> conditionHandler,
      @NotNull final UnaryFunction<? super Throwable, ? extends Story<? extends T>> incidentHandler,
      @NotNull final Memory memory) {
    final HashSet<Class<? extends Throwable>> types = new HashSet<Class<? extends Throwable>>();
    types.add(firstType);
    types.add(secondType);
    types.add(thirdType);
    return new ResolveStory<T>(this, types, conditionHandler,
        (UnaryFunction<? super Throwable, ? extends Story<T>>) incidentHandler, memory);
  }

  @NotNull
  public <E1 extends Throwable, E2 extends Throwable, E3 extends Throwable> Story<T> resolve(
      @NotNull final Class<? extends E1> firstType, @NotNull final Class<? extends E2> secondType,
      @NotNull final Class<? extends E3> thirdType,
      @NotNull final StoryEvolver<? super Throwable, ? extends T> storyEvolver) {
    return resolve(firstType, secondType, thirdType, new LooperConditionHandler(storyEvolver),
        new LooperEffectHandler<Throwable, T>(storyEvolver));
  }

  @NotNull
  public <E1 extends Throwable, E2 extends Throwable, E3 extends Throwable> Story<T> resolve(
      @NotNull final Class<? extends E1> firstType, @NotNull final Class<? extends E2> secondType,
      @NotNull final Class<? extends E3> thirdType,
      @NotNull final StoryEvolver<? super Throwable, ? extends T> storyEvolver,
      @NotNull final Memory memory) {
    return resolve(firstType, secondType, thirdType, new LooperConditionHandler(storyEvolver),
        new LooperEffectHandler<Throwable, T>(storyEvolver), memory);
  }

  @NotNull
  public <E1 extends Throwable, E2 extends Throwable> Story<T> resolve(
      @NotNull final Class<? extends E1> firstType, @NotNull final Class<? extends E2> secondType,
      @NotNull final NullaryFunction<? extends Event<? extends Boolean>> conditionHandler,
      @NotNull final UnaryFunction<? super Throwable, ? extends Story<? extends T>> incidentHandler) {
    return resolve(firstType, secondType, conditionHandler, incidentHandler, new ListMemory());
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <E1 extends Throwable, E2 extends Throwable> Story<T> resolve(
      @NotNull final Class<? extends E1> firstType, @NotNull final Class<? extends E2> secondType,
      @NotNull final NullaryFunction<? extends Event<? extends Boolean>> conditionHandler,
      @NotNull final UnaryFunction<? super Throwable, ? extends Story<? extends T>> incidentHandler,
      @NotNull final Memory memory) {
    final HashSet<Class<? extends Throwable>> types = new HashSet<Class<? extends Throwable>>();
    types.add(firstType);
    types.add(secondType);
    return new ResolveStory<T>(this, types, conditionHandler,
        (UnaryFunction<? super Throwable, ? extends Story<T>>) incidentHandler, memory);
  }

  @NotNull
  public <E1 extends Throwable, E2 extends Throwable> Story<T> resolve(
      @NotNull final Class<? extends E1> firstType, @NotNull final Class<? extends E2> secondType,
      @NotNull final StoryEvolver<? super Throwable, ? extends T> storyEvolver) {
    return resolve(firstType, secondType, new LooperConditionHandler(storyEvolver),
        new LooperEffectHandler<Throwable, T>(storyEvolver));
  }

  @NotNull
  public <E1 extends Throwable, E2 extends Throwable> Story<T> resolve(
      @NotNull final Class<? extends E1> firstType, @NotNull final Class<? extends E2> secondType,
      @NotNull final StoryEvolver<? super Throwable, ? extends T> storyEvolver,
      @NotNull final Memory memory) {
    return resolve(firstType, secondType, new LooperConditionHandler(storyEvolver),
        new LooperEffectHandler<Throwable, T>(storyEvolver), memory);
  }

  @NotNull
  public <E1 extends Throwable> Story<T> resolve(@NotNull final Class<? extends E1> firstType,
      @NotNull final NullaryFunction<? extends Event<? extends Boolean>> conditionHandler,
      @NotNull final UnaryFunction<? super Throwable, ? extends Story<? extends T>> incidentHandler) {
    return resolve(firstType, conditionHandler, incidentHandler, new ListMemory());
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <E1 extends Throwable> Story<T> resolve(@NotNull final Class<? extends E1> firstType,
      @NotNull final NullaryFunction<? extends Event<? extends Boolean>> conditionHandler,
      @NotNull final UnaryFunction<? super Throwable, ? extends Story<? extends T>> incidentHandler,
      @NotNull final Memory memory) {
    final Set<Class<? extends Throwable>> types =
        Collections.<Class<? extends Throwable>>singleton(firstType);
    return new ResolveStory<T>(this, types, conditionHandler,
        (UnaryFunction<? super Throwable, ? extends Story<T>>) incidentHandler, memory);
  }

  @NotNull
  public <E1 extends Throwable> Story<T> resolve(@NotNull final Class<? extends E1> firstType,
      @NotNull final StoryEvolver<? super Throwable, ? extends T> storyEvolver) {
    return resolve(firstType, new LooperConditionHandler(storyEvolver),
        new LooperEffectHandler<Throwable, T>(storyEvolver));
  }

  @NotNull
  public <E1 extends Throwable> Story<T> resolve(@NotNull final Class<? extends E1> firstType,
      @NotNull final StoryEvolver<? super Throwable, ? extends T> storyEvolver,
      @NotNull final Memory memory) {
    return resolve(firstType, new LooperConditionHandler(storyEvolver),
        new LooperEffectHandler<Throwable, T>(storyEvolver), memory);
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <E extends Throwable> Story<T> resolve(
      @NotNull final Iterable<? extends Class<? extends E>> incidentTypes,
      @NotNull final NullaryFunction<? extends Event<? extends Boolean>> conditionHandler,
      @NotNull final UnaryFunction<? super Throwable, ? extends Story<? extends T>> incidentHandler) {
    return resolve(incidentTypes, conditionHandler, incidentHandler, new ListMemory());
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <E extends Throwable> Story<T> resolve(
      @NotNull final Iterable<? extends Class<? extends E>> incidentTypes,
      @NotNull final NullaryFunction<? extends Event<? extends Boolean>> conditionHandler,
      @NotNull final UnaryFunction<? super Throwable, ? extends Story<? extends T>> incidentHandler,
      @NotNull final Memory memory) {
    return new ResolveStory<T>(this, Iterables.<Class<? extends Throwable>>toSet(incidentTypes),
        conditionHandler, (UnaryFunction<? super Throwable, ? extends Story<T>>) incidentHandler,
        memory);
  }

  @NotNull
  public Story<T> scheduleAtFixedRate(final long initialDelay, final long period,
      @NotNull final TimeUnit unit) {
    return scheduleAtFixedRate(initialDelay, period, unit, new ListMemory());
  }

  @NotNull
  public Story<T> scheduleAtFixedRate(final long initialDelay, final long period,
      @NotNull final TimeUnit unit, @NotNull final Memory memory) {
    return new ScheduleAtFixedRateStory<T>(this, initialDelay, period, unit, memory);
  }

  @NotNull
  public Story<T> scheduleWithFixedDelay(final long initialDelay, final long delay,
      @NotNull final TimeUnit unit) {
    return scheduleWithFixedDelay(initialDelay, delay, unit, new ListMemory());
  }

  @NotNull
  public Story<T> scheduleWithFixedDelay(final long initialDelay, final long delay,
      @NotNull final TimeUnit unit, @NotNull final Memory memory) {
    return new ScheduleWithFixedDelayStory<T>(this, initialDelay, delay, unit, memory);
  }

  @NotNull
  public Story<T> watchEach(@NotNull final EventObserver<? super T> eventObserver) {
    return watchEach(eventObserver, new ListMemory());
  }

  @NotNull
  public Story<T> watchEach(@NotNull final EventObserver<? super T> eventObserver,
      @NotNull final Memory memory) {
    return new WatchStory<T>(this, eventObserver, memory);
  }

  @NotNull
  public Story<T> watchEach(@Nullable final Observer<? super T> effectObserver,
      @Nullable final Observer<? super Throwable> incidentObserver) {
    return watchEach(new DefaultEventObserver<T>(effectObserver, incidentObserver));
  }

  @NotNull
  public Story<T> watchEach(@Nullable final Observer<? super T> effectObserver,
      @Nullable final Observer<? super Throwable> incidentObserver, @NotNull final Memory memory) {
    return watchEach(new DefaultEventObserver<T>(effectObserver, incidentObserver), memory);
  }

  public interface Memory extends Iterable<Object> {

    void put(Object value);
  }

  public interface StoryEvolver<T, R> {

    @Nullable
    Event<? extends Boolean> canContinue() throws Exception;

    @Nullable
    Story<R> evolve(T effect) throws Exception;
  }

  public interface StoryObserver<T> extends EventObserver<T> {

    void onEnd() throws Exception;
  }

  public static class StoryNarrator<T> extends Story<T> implements Narrator<T> {

    private final Actor mActor;
    private final AtomicBoolean mIsClosed = new AtomicBoolean();
    private final Memory mMemory;
    private final HashMap<String, SenderIterator> mNextSenders =
        new HashMap<String, SenderIterator>();
    private final BlockingQueue<Object> mQueue;

    private volatile Throwable mException;
    private HashMap<String, Sender> mGetSenders = new HashMap<String, Sender>();

    private StoryNarrator(@NotNull final BlockingQueue<Object> queue,
        @NotNull final Memory memory) {
      final Setting setting = Setting.get();
      mQueue = ConstantConditions.notNull("queue", queue);
      mMemory = ConstantConditions.notNull("memory", memory);
      mActor = setting.newActor(new PlayScript(setting) {

        @NotNull
        @Override
        public Behavior getBehavior(@NotNull final String id) {
          return new InitBehavior();
        }
      });
    }

    public void close() {
      if (!mIsClosed.getAndSet(true)) {
        if (mException == null) {
          mException = new NarrationStoppedException();
        }
        if (mQueue.isEmpty()) {
          mActor.tell(AVAILABLE, null, BackStage.standIn());
        }
      }
    }

    public boolean report(@NotNull final Throwable incident, final long timeout,
        @NotNull final TimeUnit unit) throws InterruptedException {
      return enqueue(new Conflict(incident), timeout, unit);
    }

    public boolean tell(final T effect, final long timeout, @NotNull final TimeUnit unit) throws
        InterruptedException {
      return enqueue((effect != null) ? effect : NULL, timeout, unit);
    }

    private void cancel(@NotNull final Throwable cause) {
      if (mException == null) {
        mException = cause;
      }
      mQueue.clear();
    }

    private void done(@NotNull final Context context) {
      done(mMemory, mGetSenders, mNextSenders, context);
      mGetSenders = null;
    }

    private boolean enqueue(@NotNull final Object resolution, final long timeout,
        @NotNull final TimeUnit unit) throws InterruptedException {
      final Throwable exception = mException;
      if (exception != null) {
        if (exception instanceof RuntimeException) {
          throw (RuntimeException) exception;

        } else {
          throw new PlotFailureException(exception);
        }
      }
      final BlockingQueue<Object> queue = mQueue;
      final boolean wasEmpty = queue.isEmpty();
      if (queue.offer(resolution, timeout, unit)) {
        if (wasEmpty) {
          mActor.tell(AVAILABLE, null, BackStage.standIn());
        }
        return true;
      }
      return false;
    }

    private boolean next(@NotNull final Context context) {
      final Actor self = context.getSelf();
      Object effect = mQueue.poll();
      if (effect != null) {
        if (effect == NULL) {
          effect = null;
        }
        mMemory.put(effect);
        for (final SenderIterator sender : mNextSenders.values()) {
          sender.tellNext(self);
        }

      } else if (mIsClosed.get()) {
        done(context);
        return false;
      }

      if (!mGetSenders.isEmpty()) {
        context.getExecutor().execute(new Runnable() {

          public void run() {
            next(context);
          }
        });
      }
      return true;
    }

    private class InitBehavior extends AbstractBehavior {

      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          final Options options = envelop.getOptions().threadOnly();
          mGetSenders.put(options.getThread(), new Sender(envelop.getSender(), options));
          if (next(context)) {
            context.setBehavior(new InputBehavior());
          }

        } else if (message == NEXT) {
          final Options options = envelop.getOptions().threadOnly();
          final SenderIterator sender = new SenderIterator(envelop.getSender(), options);
          sender.setIterator(mMemory.iterator());
          mNextSenders.put(options.getThread(), sender);
          if (next(context)) {
            context.setBehavior(new InputBehavior());
          }

        } else if (message == CANCEL) {
          final Conflict conflict = Conflict.ofCancel();
          cancel(conflict.getCause());
          context.setBehavior(
              new DoneBehavior(conflict, Collections.singleton(conflict), mNextSenders));
        }
        envelop.preventReceipt();
      }
    }

    private class InputBehavior extends AbstractBehavior {

      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          final HashMap<String, Sender> getSenders = mGetSenders;
          final boolean wasEmpty = getSenders.isEmpty();
          final Options options = envelop.getOptions().threadOnly();
          getSenders.put(options.getThread(), new Sender(envelop.getSender(), options));
          if (wasEmpty) {
            next(context);
          }

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
          if (mGetSenders.isEmpty()) {
            next(context);
          }

        } else if (message == BREAK) {
          mNextSenders.remove(envelop.getOptions().getThread());

        } else if (message == AVAILABLE) {
          if (mGetSenders.isEmpty()) {
            next(context);
          }

        } else if (message == CANCEL) {
          final Conflict conflict = Conflict.ofCancel();
          cancel(conflict.getCause());
          context.setBehavior(
              new DoneBehavior(conflict, Collections.singleton(conflict), mNextSenders));
        }
        envelop.preventReceipt();
      }
    }

    @NotNull
    Actor getActor() {
      return mActor;
    }
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

    private AbstractStory(
        @NotNull final NullaryFunction<? extends Event<? extends Boolean>> conditionHandler,
        final int numInputs, @NotNull final Memory memory) {
      mConditionHandler = ConstantConditions.notNull("conditionHandler", conditionHandler);
      mInputs = new Object[numInputs];
      mMemory = ConstantConditions.notNull("memory", memory);
      final Setting setting = (mSetting = Setting.get());
      final String actorId = (mActor = setting.newActor(new PlayScript(setting) {

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

    private void cancelInputActors(@NotNull final Context context) {
      final Actor self = context.getSelf();
      for (final Actor actor : getInputActors()) {
        actor.tell(CANCEL, null, self);
      }
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
          next(++mContinueCount, context);

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
      done(memory, mGetSenders, mNextSenders, context);
      mGetSenders = null;
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
      fail(conflict, mMemory, mGetSenders, mNextSenders, context);
    }

    private void next(final int loopCount, @NotNull final Context context) {
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
        @SuppressWarnings("UnnecessaryLocalVariable") final String inputThread = mInputThread;
        final StringBuilder builder = new StringBuilder();
        for (final Actor actor : getInputActors()) {
          final String threadId = inputThread + builder.append('#').toString();
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
          if (isSame(outputOptions.getThread(), envelop.getOptions().getThread())) {
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
                    conflictActor.tell(CANCEL, null, context.getSelf());
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
          mContinueActor.tell(CANCEL, null, context.getSelf());
          cancelInputActors(context);
          context.setBehavior(new CancelBehavior());

        } else {
          final String thread = envelop.getOptions().getThread();
          if (isSame(mContinueOptions.getThread(), thread)) {
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
          next(mContinueCount, context);

        } else if (message == NEXT) {
          final Options options = envelop.getOptions().threadOnly();
          final SenderIterator sender = new SenderIterator(envelop.getSender(), options);
          sender.setIterator(mMemory.iterator());
          mNextSenders.put(options.getThread(), sender);
          next(mContinueCount, context);

        } else if (message == CANCEL) {
          cancelInputActors(context);
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
          cancelInputActors(context);
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
                        next(++mContinueCount, context);

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
          next(++mContinueCount, context);

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
            next(++mContinueCount, context);
          }

        } else if (message == BREAK) {
          mNextSenders.remove(envelop.getOptions().getThread());

        } else if (message == CANCEL) {
          cancelInputActors(context);
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
          final Actor self = context.getSelf();
          mOutputActor.tell(BREAK, mOutputOptions, self).tell(CANCEL, null, self);
          cancelInputActors(context);
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
          mOutputActor.tell(CANCEL, null, context.getSelf());
          cancelInputActors(context);
          context.setBehavior(new CancelBehavior());

        } else {
          final Options outputOptions = mOutputOptions;
          if (isSame(outputOptions.getThread(), envelop.getOptions().getThread())) {
            if (message == END) {
              mOutputActor.tell(BREAK, outputOptions, context.getSelf());
              next(++mContinueCount, context);

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

  private static class AllStory<T, R> extends Story<R> {

    private final Actor mActor;
    private final UnaryFunction<? super T, ? extends Story<? extends R>> mEffectHandler;
    private final Actor mInputActor;
    private final Options mInputOptions;
    private final Memory mMemory;
    private final HashMap<String, SenderIterator> mNextSenders =
        new HashMap<String, SenderIterator>();
    private final Options mOptions;
    private final String mOutputThread;
    private final Setting mSetting;

    private HashMap<String, Sender> mGetSenders = new HashMap<String, Sender>();
    private Iterator<?> mInputIterator;
    private Actor mOutputActor;
    private int mOutputCount;
    private Options mOutputOptions;

    private AllStory(@NotNull final Story<? extends T> story,
        @NotNull final UnaryFunction<? super T, ? extends Story<? extends R>> effectHandler,
        @NotNull final Memory memory) {
      mInputActor = story.getActor();
      mEffectHandler = ConstantConditions.notNull("effectHandler", effectHandler);
      mMemory = ConstantConditions.notNull("memory", memory);
      final Setting setting = (mSetting = Setting.get());
      final String actorId = (mActor = setting.newActor(new PlayScript(setting) {

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
      done(mMemory, mGetSenders, mNextSenders, context);
      mGetSenders = null;
    }

    private void fail(@NotNull final Conflict conflict, @NotNull final Context context) {
      final Actor outputActor = mOutputActor;
      if (outputActor != null) {
        outputActor.tell(BREAK, mOutputOptions, context.getSelf());
      }
      fail(conflict, mMemory, mGetSenders, mNextSenders, context);
      mGetSenders = null;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private Actor getNextOutputActor() throws Exception {
      final Iterator<?> inputIterator = mInputIterator;
      if (!inputIterator.hasNext()) {
        return null;
      }
      Actor actor;
      Setting.set(mSetting);
      try {
        do {
          final Object input = inputIterator.next();
          final Story<? extends R> story = mEffectHandler.call((T) input);
          if ((story == null) || isEmpty(story)) {
            actor = null;

          } else {
            actor = story.getActor();
          }

        } while ((actor == null) && inputIterator.hasNext());
        return actor;

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

        } else if (isSame(mInputOptions.getThread(), envelop.getOptions().getThread())) {
          fail(Conflict.ofCancel(), context);

        } else {
          final Options outputOptions = mOutputOptions;
          if (isSame(outputOptions.getThread(), envelop.getOptions().getThread())) {
            mOutputActor.tell(BREAK, mOutputOptions, context.getSelf());
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
          mInputActor.tell(GET, mInputOptions, context.getSelf());
          context.setBehavior(new InputBehavior());

        } else if (message == NEXT) {
          final Options options = envelop.getOptions().threadOnly();
          final SenderIterator sender = new SenderIterator(envelop.getSender(), options);
          sender.setIterator(mMemory.iterator());
          mNextSenders.put(options.getThread(), sender);
          mInputActor.tell(GET, mInputOptions, context.getSelf());
          context.setBehavior(new InputBehavior());

        } else if (message == CANCEL) {
          mInputActor.tell(CANCEL, null, context.getSelf());
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
          mInputActor.tell(CANCEL, null, context.getSelf());
          context.setBehavior(new CancelBehavior());

        } else if (isSame(mInputOptions.getThread(), envelop.getOptions().getThread())) {
          if (message instanceof Conflict) {
            fail((Conflict) message, context);

          } else if (message instanceof Bounce) {
            fail(Conflict.ofBounce((Bounce) message), context);

          } else {
            mInputIterator = ((Iterable<?>) message).iterator();
            try {
              final Actor outputActor = (mOutputActor = getNextOutputActor());
              if (outputActor != null) {
                final String outputThreadId = mOutputThread + "#" + mOutputCount++;
                final Options options = (mOutputOptions = mOptions.withThread(outputThreadId));
                outputActor.tell(NEXT, options, context.getSelf());
                context.setBehavior(new OutputBehavior());

              } else {
                done(context);
              }

            } catch (final Throwable t) {
              fail(new Conflict(t), context);
              if (t instanceof InterruptedException) {
                Thread.currentThread().interrupt();
              }
            }
          }
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
          final Actor self = context.getSelf();
          mOutputActor.tell(BREAK, mOutputOptions, self).tell(CANCEL, null, self);
          mInputActor.tell(CANCEL, null, self);
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
          final Actor self = context.getSelf();
          mOutputActor.tell(CANCEL, null, self);
          mInputActor.tell(CANCEL, null, self);
          context.setBehavior(new CancelBehavior());

        } else {
          final Options outputOptions = mOutputOptions;
          if (isSame(outputOptions.getThread(), envelop.getOptions().getThread())) {
            if (message == END) {
              final Actor self = context.getSelf();
              mOutputActor.tell(BREAK, outputOptions, self);
              try {
                final Actor outputActor = (mOutputActor = getNextOutputActor());
                if (outputActor != null) {
                  final String outputThreadId = mOutputThread + "#" + mOutputCount++;
                  final Options options = (mOutputOptions = mOptions.withThread(outputThreadId));
                  outputActor.tell(NEXT, options, context.getSelf());

                } else {
                  done(context);
                }

              } catch (final Throwable t) {
                fail(new Conflict(t), context);
                if (t instanceof InterruptedException) {
                  Thread.currentThread().interrupt();
                }
              }

            } else if (message instanceof Bounce) {
              mMemory.put(Conflict.ofBounce((Bounce) message));
              final Actor self = context.getSelf();
              for (final SenderIterator sender : mNextSenders.values()) {
                sender.tellNext(self);
              }

              try {
                final Actor outputActor = (mOutputActor = getNextOutputActor());
                if (outputActor != null) {
                  final String outputThreadId = mOutputThread + "#" + mOutputCount++;
                  final Options options = (mOutputOptions = mOptions.withThread(outputThreadId));
                  outputActor.tell(NEXT, options, context.getSelf());

                } else {
                  done(context);
                }

              } catch (final Throwable t) {
                fail(new Conflict(t), context);
                if (t instanceof InterruptedException) {
                  Thread.currentThread().interrupt();
                }
              }

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
    private final UnaryFunction<? super T, ? extends Story<? extends R>> mEffectHandler;
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

    private BlendStory(@NotNull final Story<? extends T> story, final int maxConcurrency,
        @NotNull final UnaryFunction<? super T, ? extends Story<? extends R>> effectHandler,
        @NotNull final Memory memory) {
      mInputActor = story.getActor();
      mMaxConcurrency = ConstantConditions.positive("maxConcurrency", maxConcurrency);
      mEffectHandler = ConstantConditions.notNull("effectHandler", effectHandler);
      mMemory = ConstantConditions.notNull("memory", memory);
      final Setting setting = (mSetting = Setting.get());
      final String actorId = (mActor = setting.newActor(new PlayScript(setting) {

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
      done(mMemory, mGetSenders, mNextSenders, context);
      mGetSenders = null;
    }

    private void fail(@NotNull final Conflict conflict, @NotNull final Context context) {
      fail(conflict, mMemory, mGetSenders, mNextSenders, context);
      mGetSenders = null;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private Actor getOutputActor(@NotNull final Object input) throws Exception {
      Setting.set(mSetting);
      try {
        final Story<? extends R> story = mEffectHandler.call((T) input);
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
          if (isSame(inputOptions.getThread(), envelop.getOptions().getThread())) {
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
          mInputActor.tell(CANCEL, null, context.getSelf());
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
          mInputActor.tell(CANCEL, null, context.getSelf());
          context.setBehavior(new CancelBehavior());

        } else {
          final Options inputOptions = mInputOptions;
          if (isSame(inputOptions.getThread(), envelop.getOptions().getThread())) {
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
          for (final Actor outputActor : mOutputActors.keySet()) {
            outputActor.tell(CANCEL, null, self);
          }
          mInputActor.tell(CANCEL, null, self);
          context.setBehavior(new CancelBehavior());

        } else {
          final Options inputOptions = mInputOptions;
          if (isSame(inputOptions.getThread(), envelop.getOptions().getThread())) {
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
      final Setting setting = Setting.get();
      mActor = setting.newActor(new TrampolinePlayScript(setting) {

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

    private EffectsStory(@NotNull final Iterable<? extends T> effects) {
      final Setting setting = Setting.get();
      ConstantConditions.notNull("effects", effects);
      mActor = setting.newActor(new TrampolinePlayScript(setting) {

        private final HashMap<String, Iterator<? extends T>> mThreads =
            new HashMap<String, Iterator<? extends T>>();

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
                final HashMap<String, Iterator<? extends T>> threads = mThreads;
                final Options options = envelop.getOptions().threadOnly();
                final String thread = options.getThread();
                Iterator<? extends T> iterator = threads.get(thread);
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

    private EventStory(@NotNull final Event<? extends T> event) {
      final Setting setting = Setting.get();
      mInputActor = event.getActor();
      final String actorId = (mActor = setting.newActor(new TrampolinePlayScript(setting) {

        @NotNull
        @Override
        public Behavior getBehavior(@NotNull final String id) {
          return new InitBehavior();
        }
      })).getId();
      mOptions = new Options().withReceiptId(actorId).withThread(actorId);
    }

    private void fail(@NotNull final Conflict conflict, @NotNull final Context context) {
      final SingletonMemory memory = new SingletonMemory();
      for (final SenderIterator sender : mNextSenders.values()) {
        sender.setIterator(memory.iterator());
      }
      fail(conflict, memory, mGetSenders, mNextSenders, context);
      mGetSenders = null;
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
          mInputActor.tell(CANCEL, null, context.getSelf());
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
          mInputActor.tell(CANCEL, null, context.getSelf());
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

    private EventualStory(@NotNull final Story<T> story, @NotNull final Action eventualAction,
        @NotNull final Memory memory) {
      super(INFINITE_LOOP, 1, memory);
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

    private final NullaryFunction<? extends Story<? extends T>> mStoryCreator;

    private List<Actor> mActors;

    private FunctionStory(@NotNull final NullaryFunction<? extends Story<? extends T>> storyCreator,
        @NotNull final Memory memory) {
      super(INFINITE_LOOP, 1, memory);
      mStoryCreator = ConstantConditions.notNull("storyCreator", storyCreator);
    }

    @NotNull
    List<Actor> getInputActors() {
      if (mActors == null) {
        Setting.set(getSetting());
        Story<? extends T> story;
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

    private final UnaryFunction<? super List<T>, ? extends Story<? extends R>> mEffectHandler;
    private final List<Actor> mInputActors;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private GenericStory(@NotNull final Iterable<? extends Story<? extends T>> stories,
        @NotNull final NullaryFunction<? extends Event<? extends Boolean>> conditionHandler,
        @NotNull final UnaryFunction<? super List<T>, ? extends Story<? extends R>> effectHandler,
        @NotNull final Memory memory) {
      super(conditionHandler, Iterables.size(stories), memory);
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
        final Story<? extends R> story = mEffectHandler.call(inputList);
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
      final Setting setting = Setting.get();
      final Conflict conflict = new Conflict(incident);
      mActor = setting.newActor(new TrampolinePlayScript(setting) {

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
      final Setting setting = Setting.get();
      ConstantConditions.notNull("incidents", incidents);
      mActor = setting.newActor(new TrampolinePlayScript(setting) {

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

  private static class JoinStory<T, R> extends Story<R> {

    private final Actor mActor;
    private final UnaryFunction<? super T, ? extends Event<? extends R>> mEffectHandler;
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

    private JoinStory(@NotNull final Story<? extends T> story, final int maxConcurrency,
        final int maxEventWindow,
        @NotNull final UnaryFunction<? super T, ? extends Event<? extends R>> effectHandler,
        @NotNull final Memory memory) {
      mInputActor = story.getActor();
      mMaxConcurrency = ConstantConditions.positive("maxConcurrency", maxConcurrency);
      mMaxEventWindow = ConstantConditions.positive("maxEventWindow", maxEventWindow);
      mEffectHandler = ConstantConditions.notNull("effectHandler", effectHandler);
      mMemory = ConstantConditions.notNull("memory", memory);
      final Setting setting = (mSetting = Setting.get());
      final String actorId = (mActor = setting.newActor(new PlayScript(setting) {

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
      done(mMemory, mGetSenders, mNextSenders, context);
      mGetSenders = null;
    }

    private void fail(@NotNull final Conflict conflict, @NotNull final Context context) {
      fail(conflict, mMemory, mGetSenders, mNextSenders, context);
      mGetSenders = null;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private Actor getOutputActor(@NotNull final Object input) throws Exception {
      Setting.set(mSetting);
      try {
        final Event<? extends R> event = mEffectHandler.call((T) input);
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
          if (isSame(inputOptions.getThread(), envelop.getOptions().getThread())) {
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
          mInputActor.tell(CANCEL, null, context.getSelf());
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
          mInputActor.tell(CANCEL, null, context.getSelf());
          context.setBehavior(new CancelBehavior());

        } else {
          final Options inputOptions = mInputOptions;
          if (isSame(inputOptions.getThread(), envelop.getOptions().getThread())) {
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
              entry.getKey().tell(CANCEL, null, self);
            }
          }
          mInputActor.tell(CANCEL, null, self);
          context.setBehavior(new CancelBehavior());

        } else {
          final Options inputOptions = mInputOptions;
          if (isSame(inputOptions.getThread(), envelop.getOptions().getThread())) {
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

    private final StoryEvolver<?, ?> mStoryEvolver;

    LooperConditionHandler(@NotNull final StoryEvolver<?, ?> storyEvolver) {
      mStoryEvolver = ConstantConditions.notNull("storyEvolver", storyEvolver);
    }

    public Event<? extends Boolean> call() throws Exception {
      return mStoryEvolver.canContinue();
    }
  }

  private static class LooperEffectHandler<T, R> implements UnaryFunction<T, Story<R>> {

    private final StoryEvolver<? super T, ? extends R> mStoryEvolver;

    LooperEffectHandler(@NotNull final StoryEvolver<? super T, ? extends R> storyEvolver) {
      mStoryEvolver = ConstantConditions.notNull("storyEvolver", storyEvolver);
    }

    @SuppressWarnings("unchecked")
    public Story<R> call(final T first) throws Exception {
      return (Story<R>) mStoryEvolver.evolve(first);
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
    private ResolveStory(@NotNull final Story<? extends T> story,
        @NotNull final Set<Class<? extends Throwable>> incidentTypes,
        @NotNull final NullaryFunction<? extends Event<? extends Boolean>> conditionHandler,
        @NotNull final UnaryFunction<? super Throwable, ? extends Story<T>> incidentHandler,
        @NotNull final Memory memory) {
      super(conditionHandler, 1, memory);
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

    private ScheduleAtFixedRateStory(@NotNull final Story<? extends T> story,
        final long initialDelay, final long period, @NotNull final TimeUnit unit,
        @NotNull final Memory memory) {
      final Setting setting = Setting.get();
      mInputActor = story.getActor();
      mInitialDelay = Math.max(initialDelay, 0);
      mPeriod = ConstantConditions.positive("period", period);
      mUnit = ConstantConditions.notNull("unit", unit);
      mMemory = ConstantConditions.notNull("memory", memory);
      final String actorId = (mActor = setting.newActor(new PlayScript(setting) {

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
      done(mMemory, mGetSenders, mNextSenders, context);
      mGetSenders = null;
    }

    private void fail(@NotNull final Conflict conflict, @NotNull final Context context) {
      final ScheduledFuture<?> scheduledFuture = mScheduledFuture;
      if (scheduledFuture != null) {
        scheduledFuture.cancel(false);
      }
      fail(conflict, mMemory, mGetSenders, mNextSenders, context);
      mGetSenders = null;
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
          if (isSame(inputOptions.getThread(), envelop.getOptions().getThread())) {
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
          mInputActor.tell(CANCEL, null, context.getSelf());
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
          mInputActor.tell(CANCEL, null, context.getSelf());
          if (mInputsPending) {
            context.setBehavior(new CancelBehavior());

          } else {
            fail(Conflict.ofCancel(), context);
          }

        } else {
          final Options inputOptions = mInputOptions;
          if (isSame(inputOptions.getThread(), envelop.getOptions().getThread())) {
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

    public void run() {
      mInputsPending = true;
      mInputActor.tell(NEXT, mInputOptions, mActor);
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

    private ScheduleWithFixedDelayStory(@NotNull final Story<? extends T> story,
        final long initialDelay, final long delay, @NotNull final TimeUnit unit,
        @NotNull final Memory memory) {
      final Setting setting = Setting.get();
      mInputActor = story.getActor();
      mInitialDelay = Math.max(initialDelay, 0);
      mDelay = ConstantConditions.positive("delay", delay);
      mUnit = ConstantConditions.notNull("unit", unit);
      mMemory = ConstantConditions.notNull("memory", memory);
      final String actorId = (mActor = setting.newActor(new PlayScript(setting) {

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
      done(mMemory, mGetSenders, mNextSenders, context);
      mGetSenders = null;
    }

    private void fail(@NotNull final Conflict conflict, @NotNull final Context context) {
      final ScheduledFuture<?> scheduledFuture = mScheduledFuture;
      if (scheduledFuture != null) {
        scheduledFuture.cancel(false);
      }
      fail(conflict, mMemory, mGetSenders, mNextSenders, context);
      mGetSenders = null;
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
          if (isSame(inputOptions.getThread(), envelop.getOptions().getThread())) {
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
          mInputActor.tell(CANCEL, null, context.getSelf());
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
          mInputActor.tell(CANCEL, null, context.getSelf());
          if (mInputsPending) {
            context.setBehavior(new CancelBehavior());

          } else {
            fail(Conflict.ofCancel(), context);
          }

        } else {
          final Options inputOptions = mInputOptions;
          if (isSame(inputOptions.getThread(), envelop.getOptions().getThread())) {
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

  private static class StoryObserverScript<T> extends Script {

    private final StoryObserver<? super T> mStoryObserver;

    private StoryObserverScript(@NotNull final StoryObserver<? super T> storyObserver) {
      mStoryObserver = ConstantConditions.notNull("storyObserver", storyObserver);
    }

    @NotNull
    public Behavior getBehavior(@NotNull final String id) {
      final Options options = new Options().withReceiptId(id).withThread(id);
      return new AbstractBehavior() {

        private Actor mSender;

        @Override
        public void onStop(@NotNull final Context context) {
          final Actor sender = mSender;
          if (sender != null) {
            sender.tell(Story.BREAK, options, context.getSelf());
          }
        }

        @SuppressWarnings("unchecked")
        public void onMessage(final Object message, @NotNull final Envelop envelop,
            @NotNull final Context context) throws Exception {
          mSender = envelop.getSender();
          final Actor self = context.getSelf();
          if (message instanceof Conflict) {
            mStoryObserver.onIncident(((Conflict) message).getCause());
            envelop.getSender().tell(Story.NEXT, options, self);

          } else if (message instanceof Bounce) {
            mStoryObserver.onIncident(PlotFailureException.getOrNew((Bounce) message));
            context.dismissSelf();

          } else if (message == Story.END) {
            mStoryObserver.onEnd();
            context.dismissSelf();

          } else if (!(message instanceof Receipt)) {
            mStoryObserver.onEffect((T) message);
            envelop.getSender().tell(Story.NEXT, options, self);
          }
        }

      };
    }

    @NotNull
    @Override
    public ExecutorService getExecutor(@NotNull final String id) {
      return ExecutorServices.trampolineExecutor();
    }
  }

  private static class UnaryStory<T1, R> extends AbstractStory<R> {

    private final UnaryFunction<? super T1, ? extends Story<? extends R>> mEffectHandler;
    private final List<Actor> mInputActors;

    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    private UnaryStory(@NotNull final Story<? extends T1> firstStory,
        @NotNull final NullaryFunction<? extends Event<? extends Boolean>> conditionHandler,
        @NotNull final UnaryFunction<? super T1, ? extends Story<? extends R>> effectHandler,
        @NotNull final Memory memory) {
      super(conditionHandler, 1, memory);
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
        final Story<? extends R> story = mEffectHandler.call((T1) inputs[0]);
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

    private UnfoldStory(@NotNull final Event<? extends Iterable<? extends T>> event) {
      final Setting setting = Setting.get();
      mInputActor = event.getActor();
      final String actorId = (mActor = setting.newActor(new TrampolinePlayScript(setting) {

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
      final SingletonMemory memory = new SingletonMemory();
      for (final SenderIterator sender : mNextSenders.values()) {
        sender.setIterator(memory.iterator());
      }
      fail(conflict, memory, mGetSenders, mNextSenders, context);
      mGetSenders = null;
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
          mInputActor.tell(CANCEL, null, context.getSelf());
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
          mInputActor.tell(CANCEL, null, context.getSelf());
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
    private WatchStory(@NotNull final Story<? extends T> story,
        @NotNull final EventObserver<? super T> eventObserver, @NotNull final Memory memory) {
      super(INFINITE_LOOP, 1, memory);
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
