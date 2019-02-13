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
  // TODO: 13/02/2019 save options

  static final Object BREAK = new Object();
  static final Object END = new Object();
  static final Object NEXT = new Object();

  private static final NullaryFunction<? extends ByteBuffer> DEFAULT_BUFFER_CREATOR =
      new NullaryFunction<ByteBuffer>() {

        public ByteBuffer call() {
          return ByteBuffer.allocate(8 << 10);
        }
      };
  private static final NullaryFunction<Event<Boolean>> INDEFINITE_LOOP =
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
  public static <T> Story<T> ofEmpty() {
    return ofResults(Collections.<T>emptyList());
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
    return new ResultsStory<ByteBuffer>(
        new InputStreamIterable(memory, inputStream, bufferCreator));
  }

  @NotNull
  public static <T> Story<T> ofResults(@NotNull final Iterable<T> results) {
    final Cache cache = Setting.get().getCache(Story.class);
    Story<T> story = cache.get(results);
    if (story == null) {
      story = new ResultsStory<T>(results);
      cache.put(results, story);
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
  public static <T> Story<T> ofSingleResult(final T result) {
    final Cache cache = Setting.get().getCache(Story.class);
    Story<T> story = cache.get(result);
    if (story == null) {
      story = new ResultStory<T>(result);
      cache.put(result, story);
    }
    return story;
  }

  @NotNull
  public static <T> Story<T> ofStory(
      @NotNull final NullaryFunction<? extends Story<T>> storyCreator) {
    return new FunctionStory<T>(new ListMemory(), storyCreator);
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
      @NotNull final NullaryFunction<? extends Event<? extends Boolean>> loopHandler,
      @NotNull final UnaryFunction<? super List<T>, ? extends Story<R>> resolutionHandler) {
    return new GenericStory<T, R>(new ListMemory(), stories, loopHandler, resolutionHandler);
  }

  @NotNull
  public static <T, R> Story<R> when(@NotNull final Iterable<? extends Story<? extends T>> stories,
      @NotNull final EventLooper<? super List<T>, R> eventLooper) {
    return when(stories, new LooperLoopHandler(eventLooper),
        new LooperEventHandler<List<T>, R>(eventLooper));
  }

  @NotNull
  public static <T1, R> Story<R> when(@NotNull final Story<? extends T1> firstStory,
      @NotNull final NullaryFunction<? extends Event<? extends Boolean>> loopHandler,
      @NotNull final UnaryFunction<? super T1, ? extends Story<R>> resolutionHandler) {
    return new UnaryStory<T1, R>(new ListMemory(), firstStory, loopHandler, resolutionHandler);
  }

  private static boolean isEmpty(@Nullable final Story<?> story) {
    final Cache cache = Setting.get().getCache(Story.class);
    return (cache.get(Collections.EMPTY_LIST) == story) || (cache.get(Collections.EMPTY_SET)
        == story);
  }

  @NotNull
  @Override
  public Story<T> eventually(@NotNull final Action eventualAction) {
    return new EventualStory<T>(new ListMemory(), this, eventualAction);
  }

  @NotNull
  public <E1 extends Throwable> Story<T> resolve(@NotNull final Class<? extends E1> firstType,
      @NotNull final EventLooper<? super Throwable, ? extends T> eventLooper) {
    return resolve(firstType, new LooperLoopHandler(eventLooper),
        new LooperEventHandler<Throwable, T>(eventLooper));
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <E1 extends Throwable> Story<T> resolve(@NotNull final Class<? extends E1> firstType,
      @NotNull final NullaryFunction<? extends Event<? extends Boolean>> loopHandler,
      @NotNull final UnaryFunction<? super Throwable, ? extends Story<? extends T>> conflictHandler) {
    final HashSet<Class<? extends Throwable>> types = new HashSet<Class<? extends Throwable>>();
    types.add(firstType);
    return new ResolveStory<T>(new ListMemory(), getActor(), types, loopHandler,
        (UnaryFunction<? super Throwable, ? extends Story<T>>) conflictHandler);
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <E extends Throwable> Story<T> resolve(
      @NotNull final Iterable<? extends Class<? extends E>> conflictTypes,
      @NotNull final NullaryFunction<? extends Event<? extends Boolean>> loopHandler,
      @NotNull final UnaryFunction<? super Throwable, ? extends Story<? extends T>> conflictHandler) {
    return new ResolveStory<T>(new ListMemory(), getActor(),
        Iterables.<Class<? extends Throwable>>toSet(conflictTypes), loopHandler,
        (UnaryFunction<? super Throwable, ? extends Story<T>>) conflictHandler);
  }

  @NotNull
  public <R> Story<R> thenParallely(final int maxConcurrency,
      @NotNull final UnaryFunction<? super T, ? extends Story<R>> resolutionHandler) {
    return new ParallelStory<T, R>(new ListMemory(), this, maxConcurrency, resolutionHandler);
  }

  @NotNull
  public <R> Story<R> thenParallelyOrdered(final int maxConcurrency, final int maxEventWindow,
      @NotNull final UnaryFunction<? super T, ? extends Event<R>> resolutionHandler) {
    return new ParallelOrderedStory<T, R>(new ListMemory(), this, maxConcurrency, maxEventWindow,
        resolutionHandler);
  }

  @NotNull
  public <R> Story<R> thenWhile(@NotNull final EventLooper<? super T, ? extends R> eventLooper) {
    return thenWhile(new LooperLoopHandler(eventLooper), new LooperEventHandler<T, R>(eventLooper));
  }

  @NotNull
  public <R> Story<R> thenWhile(
      @NotNull final NullaryFunction<? extends Event<? extends Boolean>> loopHandler,
      @NotNull final UnaryFunction<? super T, ? extends Story<R>> resolutionHandler) {
    return when(this, loopHandler, resolutionHandler);
  }

  public void watchAll(@NotNull final StoryObserver<? super T> storyObserver) {
    final Actor actor = BackStage.newActor(new StoryObserverScript<T>(storyObserver));
    final String actorId = actor.getId();
    getActor().tell(NEXT, new Options().withReceiptId(actorId).withThread(actorId), actor);
  }

  public void watchAll(@Nullable final Observer<? super T> resolutionObserver,
      @Nullable final Observer<? super Throwable> conflictObserver,
      @Nullable final Action endAction) {
    watchAll(new DefaultStoryObserver<T>(resolutionObserver, conflictObserver, endAction));
  }

  public void watchAll(@NotNull final EventObserver<? super T> eventObserver) {
    final Actor actor =
        BackStage.newActor(new StoryObserverScript<T>(new EventStoryObserver<T>(eventObserver)));
    final String actorId = actor.getId();
    getActor().tell(NEXT, new Options().withReceiptId(actorId).withThread(actorId), actor);
  }

  public interface EventLooper<T, R> {

    @Nullable
    Event<? extends Boolean> loop() throws Exception;

    @Nullable
    Story<R> resolve(T event) throws Exception;
  }

  public interface Memory extends Iterable<Object> {

    void put(Object value);
  }

  public interface StoryObserver<T> extends EventObserver<T> {

    void onEnd() throws Exception;
  }

  static class DefaultStoryObserver<T> extends DefaultEventObserver<T> implements StoryObserver<T> {

    private final Action mEndAction;

    DefaultStoryObserver(@Nullable final Observer<? super T> resolutionObserver,
        @Nullable final Observer<? super Throwable> conflictObserver,
        @Nullable final Action endAction) {
      super(resolutionObserver, conflictObserver);
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

    public void onConflict(@NotNull final Throwable incident) throws Exception {
      mEventObserver.onConflict(incident);
    }

    public void onResolution(final T result) throws Exception {
      mEventObserver.onResolution(result);
    }

    public void onEnd() {
    }
  }

  private abstract static class AbstractStory<T> extends Story<T> {

    private final Actor mActor;
    private final String mInputThread;
    private final Object[] mInputs;
    private final NullaryFunction<? extends Event<? extends Boolean>> mLoopHandler;
    private final String mLoopThread;
    private final HashMap<String, SenderIterator> mNextSenders =
        new HashMap<String, SenderIterator>();
    private final Options mOptions;
    private final String mOutputThread;
    private final Setting mSetting;

    private Conflict mConflict;
    private HashMap<String, Sender> mGetSenders = new HashMap<String, Sender>();
    private int mInputCount;
    private Actor mLoopActor;
    private int mLoopCount;
    private String mLoopThreadId;
    private Memory mMemory;
    private Actor mOutputActor;
    private String mOutputThreadId;

    private AbstractStory(@NotNull final Memory memory,
        @NotNull final NullaryFunction<? extends Event<? extends Boolean>> loopHandler,
        final int numInputs) {
      mMemory = ConstantConditions.notNull("memory", memory);
      mLoopHandler = ConstantConditions.notNull("loopHandler", loopHandler);
      mInputs = new Object[numInputs];
      final Setting setting = (mSetting = Setting.get());
      final String actorId = (mActor = BackStage.newActor(new PlayScript(setting) {

        @NotNull
        @Override
        public Behavior getBehavior(@NotNull final String id) {
          return new InitBehavior();
        }
      })).getId();
      mLoopThread = actorId + ":loop";
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

    boolean putResult(@NotNull final Object[] inputs, @NotNull final Memory memory) {
      return false;
    }

    private void conflict(@NotNull final Conflict conflict, @NotNull final Context context) {
      try {
        final Actor conflictActor = getConflictActor(conflict);
        if (conflictActor != null) {
          (mOutputActor = conflictActor).tell(GET, mOptions.withThread(mOutputThreadId),
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
      Object results = memory;
      for (final Object result : memory) {
        if (result instanceof Conflict) {
          results = result;
          break;
        }
      }
      final Actor self = context.getSelf();
      for (final Sender sender : mGetSenders.values()) {
        sender.getSender().tell(results, sender.getOptions(), self);
      }
      mGetSenders = null;
      final HashMap<String, SenderIterator> nextSenders = mNextSenders;
      for (final SenderIterator sender : nextSenders.values()) {
        if (sender.isWaitNext() && !sender.tellNext(self)) {
          sender.getSender().tell(END, sender.getOptions(), self);
        }
      }
      context.setBehavior(new DoneBehavior(results, memory, nextSenders));
    }

    private void fail(@NotNull Conflict conflict, @NotNull final Context context) {
      tellInputActors(BREAK, context);
      final Actor outputActor = mOutputActor;
      if (outputActor != null) {
        outputActor.tell(BREAK, mOptions.withThread(mOutputThreadId), context.getSelf());
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
          event = mLoopHandler.call();

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
          (mLoopActor = event.getActor()).tell(GET, mOptions.withThread(mLoopThreadId),
              context.getSelf());
          context.setBehavior(new LoopBehavior());
        }

      } catch (final Throwable t) {
        fail(new Conflict(t), context);
        if (t instanceof InterruptedException) {
          Thread.currentThread().interrupt();
        }
      }
    }

    private void setThreadIds(final int count) {
      mLoopThreadId = mLoopThread + "#" + count;
      mOutputThreadId = mOutputThread + "#" + count;
    }

    private void tellInputActors(final Object message, @NotNull final Context context) {
      final Actor self = context.getSelf();
      final StringBuilder builder = new StringBuilder();
      for (final Actor actor : getInputActors()) {
        final String threadId = mInputThread + builder.append('#').toString();
        actor.tell(message, mOptions.withThread(threadId), self);
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

        } else if (mOutputThreadId.equals(envelop.getOptions().getThread())) {
          mOutputActor.tell(BREAK, mOptions.withThread(mOutputThreadId), context.getSelf());
          fail(Conflict.ofCancel(), context);

        } else {
          final String thread = envelop.getOptions().getThread();
          if ((thread != null) && thread.startsWith(mInputThread)) {
            if (++mInputCount == mInputs.length) {
              final Conflict conflict = Conflict.ofCancel();
              try {
                final Actor conflictActor = getConflictActor(conflict);
                if (conflictActor != null) {
                  conflictActor.tell(CANCEL, mOptions.withThread(mOutputThreadId),
                      context.getSelf());
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

    private class InitBehavior extends AbstractBehavior {

      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          final Options options = envelop.getOptions().threadOnly();
          mGetSenders.put(options.getThread(), new Sender(envelop.getSender(), options));
          loop(mLoopCount, context);

        } else if (message == NEXT) {
          final Options options = envelop.getOptions().threadOnly();
          final SenderIterator sender = new SenderIterator(envelop.getSender(), options);
          sender.setIterator(mMemory.iterator());
          mNextSenders.put(options.getThread(), sender);
          loop(mLoopCount, context);

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
                    try {
                      final Actor self = context.getSelf();
                      final Actor outputActor = getOutputActor(inputs);
                      if (outputActor != null) {
                        (mOutputActor = outputActor).tell(NEXT,
                            mOptions.withThread(mOutputThreadId), self);
                        context.setBehavior(new OutputBehavior());

                      } else {
                        if (putResult(inputs, mMemory)) {
                          for (final SenderIterator sender : mNextSenders.values()) {
                            sender.tellNext(self);
                          }
                        }

                        if (!mGetSenders.isEmpty()) {
                          loop(++mLoopCount, context);

                        } else {
                          context.setBehavior(new NextLoopBehavior());
                        }
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
        }
        envelop.preventReceipt();
      }
    }

    private class LoopBehavior extends AbstractBehavior {

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
          mLoopActor.tell(CANCEL, mOptions.withThread(mLoopThreadId), context.getSelf());
          tellInputActors(CANCEL, context);
          context.setBehavior(new CancelBehavior());

        } else {
          final String thread = envelop.getOptions().getThread();
          final String loopThread = mLoopThreadId;
          if (loopThread.equals(thread)) {
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

    private class NextBehavior extends AbstractBehavior {

      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          final Options options = envelop.getOptions().threadOnly();
          mGetSenders.put(options.getThread(), new Sender(envelop.getSender(), options));
          mOutputActor.tell(NEXT, mOptions.withThread(mOutputThreadId), context.getSelf());
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
            mOutputActor.tell(NEXT, mOptions.withThread(mOutputThreadId), self);
            context.setBehavior(new OutputBehavior());
          }

        } else if (message == BREAK) {
          mNextSenders.remove(envelop.getOptions().getThread());

        } else if (message == CANCEL) {
          mOutputActor.tell(BREAK, mOptions.withThread(mOutputThreadId), context.getSelf());
          tellInputActors(CANCEL, context);
          fail(Conflict.ofCancel(), context);
        }
        envelop.preventReceipt();
      }
    }

    private class NextLoopBehavior extends AbstractBehavior {

      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          final Options options = envelop.getOptions().threadOnly();
          mGetSenders.put(options.getThread(), new Sender(envelop.getSender(), options));
          loop(++mLoopCount, context);

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
            loop(++mLoopCount, context);
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
          mOutputActor.tell(CANCEL, mOptions.withThread(mOutputThreadId), context.getSelf());
          tellInputActors(CANCEL, context);
          context.setBehavior(new CancelBehavior());

        } else if (mOutputThreadId.equals(envelop.getOptions().getThread())) {
          if (message == END) {
            mOutputActor.tell(BREAK, mOptions.withThread(mOutputThreadId), context.getSelf());
            loop(++mLoopCount, context);

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
              mOutputActor.tell(NEXT, mOptions.withThread(mOutputThreadId), self);

            } else {
              context.setBehavior(new NextBehavior());
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

    private final Map<String, SenderIterator> mNextSenders;
    private final Object mResult;
    private final Iterable<?> mResults;

    private DoneBehavior(final Object result, @NotNull final Iterable<?> results,
        @NotNull final Map<String, SenderIterator> nextSenders) {
      mResult = result;
      mResults = results;
      mNextSenders = nextSenders;
    }

    public void onMessage(final Object message, @NotNull final Envelop envelop,
        @NotNull final Context context) {
      if (message == GET) {
        envelop.getSender().tell(mResult, envelop.getOptions().threadOnly(), context.getSelf());

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
          sender.setIterator(mResults.iterator());
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
              final Set<Object> results = Collections.singleton(message);
              for (final SenderIterator sender : mNextSenders.values()) {
                sender.setIterator(results.iterator());
                sender.tellNext(self);
              }
              context.setBehavior(new DoneBehavior(results, results, mNextSenders));
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
      super(memory, INDEFINITE_LOOP, 1);
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
      super(memory, INDEFINITE_LOOP, 1);
      mStoryCreator = ConstantConditions.notNull("storyCreator", storyCreator);
    }

    @Override
    boolean putResult(@NotNull final Object[] inputs, @NotNull final Memory memory) {
      memory.put(inputs[0]);
      return true;
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

    private final List<Actor> mInputActors;
    private final UnaryFunction<? super List<T>, ? extends Story<R>> mResolutionHandler;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private GenericStory(@NotNull final Memory memory,
        @NotNull final Iterable<? extends Story<? extends T>> stories,
        @NotNull final NullaryFunction<? extends Event<? extends Boolean>> loopHandler,
        @NotNull final UnaryFunction<? super List<T>, ? extends Story<R>> resolutionHandler) {
      super(memory, loopHandler, Iterables.size(stories));
      final ArrayList<Actor> inputActors = new ArrayList<Actor>();
      for (final Story<? extends T> story : stories) {
        inputActors.add(story.getActor());
      }
      ConstantConditions.positive("stories size", inputActors.size());
      mInputActors = inputActors;
      mResolutionHandler = ConstantConditions.notNull("resolutionHandler", resolutionHandler);
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
        final Story<R> story = mResolutionHandler.call(inputList);
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

  private static class LooperEventHandler<T, R> implements UnaryFunction<T, Story<R>> {

    private final EventLooper<? super T, ? extends R> mEventLooper;

    LooperEventHandler(@NotNull final EventLooper<? super T, ? extends R> eventLooper) {
      mEventLooper = ConstantConditions.notNull("eventLooper", eventLooper);
    }

    @SuppressWarnings("unchecked")
    public Story<R> call(final T first) throws Exception {
      return (Story<R>) mEventLooper.resolve(first);
    }
  }

  private static class LooperLoopHandler implements NullaryFunction<Event<? extends Boolean>> {

    private final EventLooper<?, ?> mEventLooper;

    LooperLoopHandler(@NotNull final EventLooper<?, ?> eventLooper) {
      mEventLooper = ConstantConditions.notNull("eventLooper", eventLooper);
    }

    public Event<? extends Boolean> call() throws Exception {
      return mEventLooper.loop();
    }
  }

  private static class OutputResult {

    private final Options mOptions;

    private boolean mIsSet;
    private Object mResult;

    private OutputResult(@NotNull final Options options) {
      mOptions = options;
    }

    @NotNull
    Options getOptions() {
      return mOptions;
    }

    Object getResult() {
      return mResult;
    }

    boolean isSet() {
      return mIsSet;
    }

    void set(final Object result) {
      mIsSet = true;
      mResult = result;
    }
  }

  private static class ParallelOrderedStory<T, R> extends Story<R> {

    private final Actor mActor;
    private final Actor mInputActor;
    private final String mInputThreadId;
    private final int mMaxConcurrency;
    private final int mMaxEventWindow;
    private final Memory mMemory;
    private final HashMap<String, SenderIterator> mNextSenders =
        new HashMap<String, SenderIterator>();
    private final Options mOptions;
    private final LinkedHashMap<Actor, OutputResult> mOutputResults =
        new LinkedHashMap<Actor, OutputResult>();
    private final String mOutputThread;
    private final UnaryFunction<? super T, ? extends Event<R>> mResolutionHandler;
    private final Setting mSetting;

    private int mActorCount;
    private HashMap<String, Sender> mGetSenders = new HashMap<String, Sender>();
    private Conflict mInputConflict;
    private boolean mInputsEnded;
    private long mOutputCount;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private ParallelOrderedStory(@NotNull final Memory memory,
        @NotNull final Story<? extends T> story, final int maxConcurrency, final int maxEventWindow,
        @NotNull final UnaryFunction<? super T, ? extends Event<R>> resolutionHandler) {
      mMemory = ConstantConditions.notNull("memory", memory);
      mInputActor = story.getActor();
      mMaxConcurrency = ConstantConditions.positive("maxConcurrency", maxConcurrency);
      mMaxEventWindow = ConstantConditions.positive("maxEventWindow", maxEventWindow);
      mResolutionHandler = ConstantConditions.notNull("resolutionHandler", resolutionHandler);
      final Setting setting = (mSetting = Setting.get());
      final String actorId = (mActor = BackStage.newActor(new PlayScript(setting) {

        @NotNull
        @Override
        public Behavior getBehavior(@NotNull final String id) {
          return new InitBehavior();
        }
      })).getId();
      mInputThreadId = actorId + ":input";
      mOutputThread = actorId + ":output";
      mOptions = new Options().withReceiptId(actorId);
    }

    private void done(@NotNull final Context context) {
      final Memory memory = mMemory;
      Object results = memory;
      for (final Object result : memory) {
        if (result instanceof Conflict) {
          results = result;
          break;
        }
      }
      final Actor self = context.getSelf();
      for (final Sender sender : mGetSenders.values()) {
        sender.getSender().tell(results, sender.getOptions(), self);
      }
      mGetSenders = null;
      final HashMap<String, SenderIterator> nextSenders = mNextSenders;
      for (final SenderIterator sender : nextSenders.values()) {
        if (sender.isWaitNext() && !sender.tellNext(self)) {
          sender.getSender().tell(END, sender.getOptions(), self);
        }
      }
      context.setBehavior(new DoneBehavior(results, memory, nextSenders));
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
        final Event<R> event = mResolutionHandler.call((T) input);
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

        } else if (mInputThreadId.equals(envelop.getOptions().getThread())) {
          envelop.getSender().tell(BREAK, envelop.getOptions().threadOnly(), context.getSelf());
          mInputsEnded = true;
          if (mActorCount == 0) {
            fail(Conflict.ofCancel(), context);
          }

        } else {
          final String thread = envelop.getOptions().getThread();
          if ((thread != null) && thread.startsWith(mOutputThread)) {
            final Actor sender = envelop.getSender();
            sender.tell(BREAK, mOutputResults.get(sender).getOptions(), context.getSelf());
            if ((--mActorCount == 0) && mInputsEnded) {
              fail(Conflict.ofCancel(), context);
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
          mInputActor.tell(NEXT, mOptions.withThread(mInputThreadId), context.getSelf());
          context.setBehavior(new InputBehavior());

        } else if (message == NEXT) {
          final Options options = envelop.getOptions().threadOnly();
          final SenderIterator sender = new SenderIterator(envelop.getSender(), options);
          sender.setIterator(mMemory.iterator());
          mNextSenders.put(options.getThread(), sender);
          mInputActor.tell(NEXT, mOptions.withThread(mInputThreadId), context.getSelf());
          context.setBehavior(new InputBehavior());

        } else if (message == CANCEL) {
          mInputActor.tell(CANCEL, mOptions.withThread(mInputThreadId), context.getSelf());
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
          mInputActor.tell(CANCEL, mOptions.withThread(mInputThreadId), context.getSelf());
          context.setBehavior(new CancelBehavior());

        } else if (mInputThreadId.equals(envelop.getOptions().getThread())) {
          if (message == END) {
            envelop.getSender().tell(BREAK, envelop.getOptions().threadOnly(), context.getSelf());
            done(context);

          } else if (message instanceof Conflict) {
            fail((Conflict) message, context);

          } else if (message instanceof Bounce) {
            fail(Conflict.ofBounce((Bounce) message), context);

          } else {
            try {
              final Actor outputActor = getOutputActor(message);
              final String outputThreadId = mOutputThread + "#" + mOutputCount++;
              final Options options = mOptions.withThread(outputThreadId);
              final LinkedHashMap<Actor, OutputResult> outputResults = mOutputResults;
              outputResults.put(outputActor, new OutputResult(options));
              outputActor.tell(GET, options, context.getSelf());
              if ((++mActorCount < mMaxConcurrency) && (outputResults.size() < mMaxEventWindow)) {
                mInputActor.tell(NEXT, mOptions.withThread(mInputThreadId), context.getSelf());
              }
              context.setBehavior(new OutputBehavior());

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
          for (final Entry<Actor, OutputResult> entry : mOutputResults.entrySet()) {
            final OutputResult outputResult = entry.getValue();
            if (!outputResult.isSet()) {
              entry.getKey().tell(CANCEL, outputResult.getOptions(), self);
            }
          }
          mInputActor.tell(CANCEL, mOptions.withThread(mInputThreadId), self);
          context.setBehavior(new CancelBehavior());

        } else if (mInputThreadId.equals(envelop.getOptions().getThread())) {
          if (message == END) {
            envelop.getSender().tell(BREAK, envelop.getOptions().threadOnly(), context.getSelf());
            mInputsEnded = true;
            if (mActorCount == 0) {
              done(context);
            }

          } else if (message instanceof Conflict) {
            envelop.getSender().tell(BREAK, envelop.getOptions().threadOnly(), context.getSelf());
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
              final LinkedHashMap<Actor, OutputResult> outputResults = mOutputResults;
              outputResults.put(outputActor, new OutputResult(options));
              outputActor.tell(GET, options, context.getSelf());
              if ((++mActorCount < mMaxConcurrency) && (outputResults.size() < mMaxEventWindow)) {
                mInputActor.tell(NEXT, mOptions.withThread(mInputThreadId), context.getSelf());
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
            final Object result;
            if (message instanceof Bounce) {
              result = Conflict.ofBounce((Bounce) message);

            } else {
              result = message;
            }
            // pass on conflicts
            @SuppressWarnings("UnnecessaryLocalVariable") final Memory memory = mMemory;
            final LinkedHashMap<Actor, OutputResult> outputResults = mOutputResults;
            outputResults.get(envelop.getSender()).set(result);
            final Iterator<OutputResult> iterator = outputResults.values().iterator();
            while (iterator.hasNext()) {
              final OutputResult outputResult = iterator.next();
              if (outputResult.isSet()) {
                memory.put(outputResult.getResult());
                iterator.remove();

              } else {
                break;
              }
            }
            final Actor self = context.getSelf();
            for (final SenderIterator sender : mNextSenders.values()) {
              sender.tellNext(self);
            }

            if (outputResults.size() < mMaxEventWindow) {
              if (!mInputsEnded) {
                // TODO: 13/02/2019 repeated
                mInputActor.tell(NEXT, mOptions.withThread(mInputThreadId), self);

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
        envelop.preventReceipt();
      }
    }

    @NotNull
    Actor getActor() {
      return mActor;
    }
  }

  private static class ParallelStory<T, R> extends Story<R> {

    private final Actor mActor;
    private final Actor mInputActor;
    private final String mInputThreadId;
    private final int mMaxConcurrency;
    private final Memory mMemory;
    private final HashMap<Actor, Options> mNextActors = new HashMap<Actor, Options>();
    private final HashMap<String, SenderIterator> mNextSenders =
        new HashMap<String, SenderIterator>();
    private final Options mOptions;
    private final HashMap<Actor, Options> mOutputActors = new HashMap<Actor, Options>();
    private final String mOutputThread;
    private final UnaryFunction<? super T, ? extends Story<R>> mResolutionHandler;
    private final Setting mSetting;

    private HashMap<String, Sender> mGetSenders = new HashMap<String, Sender>();
    private Conflict mInputConflict;
    private boolean mInputsEnded;
    private long mOutputCount;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private ParallelStory(@NotNull final Memory memory, @NotNull final Story<? extends T> story,
        final int maxConcurrency,
        @NotNull final UnaryFunction<? super T, ? extends Story<R>> resolutionHandler) {
      mMemory = ConstantConditions.notNull("memory", memory);
      mInputActor = story.getActor();
      mMaxConcurrency = ConstantConditions.positive("maxConcurrency", maxConcurrency);
      mResolutionHandler = ConstantConditions.notNull("resolutionHandler", resolutionHandler);
      final Setting setting = (mSetting = Setting.get());
      final String actorId = (mActor = BackStage.newActor(new PlayScript(setting) {

        @NotNull
        @Override
        public Behavior getBehavior(@NotNull final String id) {
          return new InitBehavior();
        }
      })).getId();
      mInputThreadId = actorId + ":input";
      mOutputThread = actorId + ":output";
      mOptions = new Options().withReceiptId(actorId);
    }

    private void done(@NotNull final Context context) {
      final Memory memory = mMemory;
      Object results = memory;
      for (final Object result : memory) {
        if (result instanceof Conflict) {
          results = result;
          break;
        }
      }
      final Actor self = context.getSelf();
      for (final Sender sender : mGetSenders.values()) {
        sender.getSender().tell(results, sender.getOptions(), self);
      }
      mGetSenders = null;
      final HashMap<String, SenderIterator> nextSenders = mNextSenders;
      for (final SenderIterator sender : nextSenders.values()) {
        if (sender.isWaitNext() && !sender.tellNext(self)) {
          sender.getSender().tell(END, sender.getOptions(), self);
        }
      }
      context.setBehavior(new DoneBehavior(results, memory, nextSenders));
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
        final Story<R> story = mResolutionHandler.call((T) input);
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

        } else if (mInputThreadId.equals(envelop.getOptions().getThread())) {
          envelop.getSender().tell(BREAK, envelop.getOptions().threadOnly(), context.getSelf());
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
        envelop.preventReceipt();
      }
    }

    private class InitBehavior extends AbstractBehavior {

      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          final Options options = envelop.getOptions().threadOnly();
          mGetSenders.put(options.getThread(), new Sender(envelop.getSender(), options));
          mInputActor.tell(NEXT, mOptions.withThread(mInputThreadId), context.getSelf());
          context.setBehavior(new InputBehavior());

        } else if (message == NEXT) {
          final Options options = envelop.getOptions().threadOnly();
          final SenderIterator sender = new SenderIterator(envelop.getSender(), options);
          sender.setIterator(mMemory.iterator());
          mNextSenders.put(options.getThread(), sender);
          mInputActor.tell(NEXT, mOptions.withThread(mInputThreadId), context.getSelf());
          context.setBehavior(new InputBehavior());

        } else if (message == CANCEL) {
          mInputActor.tell(CANCEL, mOptions.withThread(mInputThreadId), context.getSelf());
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
          mInputActor.tell(CANCEL, mOptions.withThread(mInputThreadId), context.getSelf());
          context.setBehavior(new CancelBehavior());

        } else if (mInputThreadId.equals(envelop.getOptions().getThread())) {
          if (message == END) {
            envelop.getSender().tell(BREAK, envelop.getOptions().threadOnly(), context.getSelf());
            done(context);

          } else if (message instanceof Conflict) {
            envelop.getSender().tell(BREAK, envelop.getOptions().threadOnly(), context.getSelf());
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
                if (outputActors.size() < mMaxConcurrency) {
                  mInputActor.tell(NEXT, mOptions.withThread(mInputThreadId), context.getSelf());
                }
                context.setBehavior(new OutputBehavior());

              } else if (mOutputActors.size() < mMaxConcurrency) {
                mInputActor.tell(NEXT, mOptions.withThread(mInputThreadId), context.getSelf());
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
          mInputActor.tell(CANCEL, mOptions.withThread(mInputThreadId), self);
          context.setBehavior(new CancelBehavior());

        } else if (mInputThreadId.equals(envelop.getOptions().getThread())) {
          if (message == END) {
            envelop.getSender().tell(BREAK, envelop.getOptions().threadOnly(), context.getSelf());
            mInputsEnded = true;
            if (mOutputActors.isEmpty()) {
              done(context);
            }

          } else if (message instanceof Conflict) {
            envelop.getSender().tell(BREAK, envelop.getOptions().threadOnly(), context.getSelf());
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
                if (outputActors.size() < mMaxConcurrency) {
                  mInputActor.tell(NEXT, mOptions.withThread(mInputThreadId), context.getSelf());
                }

              } else if (mOutputActors.size() < mMaxConcurrency) {
                mInputActor.tell(NEXT, mOptions.withThread(mInputThreadId), context.getSelf());
              }

            } catch (final Throwable t) {
              mInputsEnded = true;
              mInputConflict = new Conflict(t);
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
                // TODO: 13/02/2019 repeated
                mInputActor.tell(NEXT, mOptions.withThread(mInputThreadId), self);

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
                // TODO: 13/02/2019 repeated
                mInputActor.tell(NEXT, mOptions.withThread(mInputThreadId), self);

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
                mNextActors.put(sender, mOptions.withThread(thread));
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

  private static class ResolveStory<T> extends AbstractStory<T> {

    private final UnaryFunction<? super Throwable, ? extends Story<T>> mConflictHandler;
    private final Set<Class<? extends Throwable>> mConflictTypes;
    private final List<Actor> mInputActors;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private ResolveStory(@NotNull final Memory memory, @NotNull final Actor eventActor,
        @NotNull final Set<Class<? extends Throwable>> conflictTypes,
        @NotNull final NullaryFunction<? extends Event<? extends Boolean>> loopHandler,
        @NotNull final UnaryFunction<? super Throwable, ? extends Story<T>> conflictHandler) {
      super(memory, loopHandler, 1);
      mInputActors = Collections.singletonList(eventActor);
      ConstantConditions.positive("conflictTypes size", conflictTypes.size());
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
            final Story<T> story = mConflictHandler.call(incident);
            return ((story != null) ? story : ofEmpty()).getActor();

          } finally {
            Setting.unset();
          }
        }
      }
      return super.getConflictActor(conflict);
    }

    @Override
    boolean putResult(@NotNull final Object[] inputs, @NotNull final Memory memory) {
      memory.put(inputs[0]);
      return true;
    }

    @NotNull
    List<Actor> getInputActors() {
      return mInputActors;
    }
  }

  private static class ResultStory<T> extends Story<T> {

    private final Actor mActor;

    private ResultStory(final T result) {
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
                    .tell(result, envelop.getOptions().threadOnly(), context.getSelf());

              } else if (message == NEXT) {
                final HashSet<String> threads = mThreads;
                final Options options = envelop.getOptions().threadOnly();
                final String thread = options.getThread();
                if (!threads.contains(thread)) {
                  envelop.getSender()
                      .tell(result, envelop.getOptions().threadOnly(), context.getSelf());
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

  private static class ResultsStory<T> extends Story<T> {

    private final Actor mActor;

    private ResultsStory(@NotNull final Iterable<T> results) {
      ConstantConditions.notNull("results", results);
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
                    .tell(Iterables.toList(results), envelop.getOptions().threadOnly(),
                        context.getSelf());

              } else if (message == NEXT) {
                final HashMap<String, Iterator<T>> threads = mThreads;
                final Options options = envelop.getOptions().threadOnly();
                final String thread = options.getThread();
                Iterator<T> iterator = threads.get(thread);
                if (iterator == null) {
                  iterator = results.iterator();
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

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
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

    private final List<Actor> mInputActors;
    private final UnaryFunction<? super T1, ? extends Story<R>> mResolutionHandler;

    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    private UnaryStory(@NotNull final Memory memory, @NotNull final Story<? extends T1> firstStory,
        @NotNull final NullaryFunction<? extends Event<? extends Boolean>> loopHandler,
        @NotNull final UnaryFunction<? super T1, ? extends Story<R>> resolutionHandler) {
      super(memory, loopHandler, 1);
      mInputActors = Arrays.asList(firstStory.getActor());
      mResolutionHandler = ConstantConditions.notNull("resolutionHandler", resolutionHandler);
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
        final Story<R> story = mResolutionHandler.call((T1) inputs[0]);
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
    private final String mInputThreadId;
    private final HashMap<String, SenderIterator> mNextSenders =
        new HashMap<String, SenderIterator>();
    private final Options mOptions;

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
      mOptions = new Options().withReceiptId(actorId).withThread(actorId);
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
          mInputActor.tell(CANCEL, mOptions, context.getSelf());
          context.setBehavior(new CancelBehavior());

        } else if (mInputThreadId.equals(envelop.getOptions().getThread())) {
          if (message instanceof Conflict) {
            fail((Conflict) message, context);

          } else if (message instanceof Bounce) {
            fail(Conflict.ofBounce((Bounce) message), context);

          } else {
            final Actor self = context.getSelf();
            final Iterable<?> results = (Iterable<?>) message;
            for (final Sender sender : mGetSenders.values()) {
              sender.getSender().tell(results, sender.getOptions(), self);
            }
            mGetSenders = null;
            for (final SenderIterator sender : mNextSenders.values()) {
              sender.setIterator(results.iterator());
              sender.tellNext(self);
            }
            context.setBehavior(new DoneBehavior(results, results, mNextSenders));
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
}
