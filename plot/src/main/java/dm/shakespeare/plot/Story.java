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
import java.util.List;
import java.util.Map;
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

  static final Object BREAK = new Object();
  static final Object END = new Object();
  static final Object NEXT = new Object();

  private static final NullaryFunction<? extends ByteBuffer> DEFAULT_BUFFER_CREATOR =
      new NullaryFunction<ByteBuffer>() {

        public ByteBuffer call() {
          return ByteBuffer.allocate(8 << 10);
        }
      };
  private static final Action NO_OP = new Action() {

    public void run() {
    }
  };

  @NotNull
  public static <T> Story<T> crossOver(
      @NotNull final Iterable<? extends Story<? extends T>> stories) {
    return new CrossOverStory<T>(new ListMemory(), stories);
  }

  @NotNull
  public static <T> Story<T> crossOverEventually(
      @NotNull final Iterable<? extends Story<? extends T>> stories) {
    return new CrossOverEventuallyStory<T>(new ListMemory(), stories);
  }

  @NotNull
  public static <T> Story<T> lineUp(@NotNull final Iterable<? extends Event<? extends T>> events) {
    return new LineUpStory<T>(new ListMemory(), events);
  }

  @NotNull
  public static <T> Story<T> lineUpEventually(
      @NotNull final Iterable<? extends Event<? extends T>> events) {
    return new LineUpEventuallyStory<T>(new ListMemory(), events);
  }

  @NotNull
  public static <T> Story<T> ofConflicts(@NotNull final Iterable<? extends Throwable> incidents) {
    return new ConflictsStory<T>(incidents);
  }

  @NotNull
  public static <T> Story<T> ofEmpty() {
    return ofResolutions(Collections.<T>emptyList());
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
    return new ResolutionsStory<ByteBuffer>(
        new InputStreamIterable(memory, inputStream, bufferCreator));
  }

  @NotNull
  public static <T> Story<T> ofResolutions(@NotNull final Iterable<T> results) {
    final Cache cache = Setting.get().getCache(Story.class);
    Story<T> story = cache.get(results);
    if (story == null) {
      story = new ResolutionsStory<T>(results);
      cache.put(results, story);
    }
    return story;
  }

  @NotNull
  public static <T> Story<T> ofSingleConflict(@NotNull final Throwable incident) {
    return new ConflictStory<T>(incident);
  }

  @NotNull
  public static <T> Story<T> ofSingleResolution(final T result) {
    final Cache cache = Setting.get().getCache(Story.class);
    Story<T> story = cache.get(result);
    if (story == null) {
      story = new ResolutionStory<T>(result);
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
  public <R> Story<R> then(@NotNull final EventLooper<? super T, ? extends R> eventLooper) {
    return then(new LooperLoopHandler(eventLooper), new LooperEventHandler<T, R>(eventLooper));
  }

  @NotNull
  public <R> Story<R> then(
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
      // TODO: 10/02/2019 eventually
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
        if (!sender.tellNext(self)) {
          sender.getSender().tell(END, sender.getOptions(), self);
        }
      }
      context.setBehavior(new DoneBehavior(results, memory, nextSenders));
    }

    private void fail(@NotNull final Conflict conflict, @NotNull final Context context) {
      tellInputActors(BREAK, context);
      final Actor outputActor = mOutputActor;
      if (outputActor != null) {
        outputActor.tell(BREAK, mOptions.withThread(mOutputThreadId), context.getSelf());
      }
      // TODO: 10/02/2019 eventually
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
          SenderIterator sender = mNextSenders.get(thread);
          if (sender != null) {
            sender.waitNext();

          } else {
            sender = new SenderIterator(envelop.getSender(), options.threadOnly());
            sender.setIterator(mMemory.iterator());
            mNextSenders.put(thread, sender);
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
              fail(Conflict.ofCancel(), context);
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
          SenderIterator sender = mNextSenders.get(thread);
          if (sender != null) {
            sender.waitNext();

          } else {
            sender = new SenderIterator(envelop.getSender(), options.threadOnly());
            sender.setIterator(mMemory.iterator());
            mNextSenders.put(thread, sender);
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
              final Conflict conflict = new Conflict(PlotStateException.getOrNew((Bounce) message));
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
                        loop(++mLoopCount, context);
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
          SenderIterator sender = mNextSenders.get(thread);
          if (sender != null) {
            sender.waitNext();

          } else {
            sender = new SenderIterator(envelop.getSender(), options.threadOnly());
            sender.setIterator(mMemory.iterator());
            mNextSenders.put(thread, sender);
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
              fail(new Conflict(PlotStateException.getOrNew((Bounce) message)), context);

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
          SenderIterator sender = mNextSenders.get(thread);
          if (sender != null) {
            sender.waitNext();

          } else {
            sender = new SenderIterator(envelop.getSender(), options.threadOnly());
            sender.setIterator(mMemory.iterator());
            mNextSenders.put(thread, sender);
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

    private class OutputBehavior extends AbstractBehavior {

      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          final Options options = envelop.getOptions().threadOnly();
          mGetSenders.put(options.getThread(), new Sender(envelop.getSender(), options));

        } else if (message == NEXT) {
          final Options options = envelop.getOptions();
          final String thread = options.getThread();
          SenderIterator sender = mNextSenders.get(thread);
          if (sender != null) {
            sender.waitNext();

          } else {
            sender = new SenderIterator(envelop.getSender(), options.threadOnly());
            sender.setIterator(mMemory.iterator());
            mNextSenders.put(thread, sender);
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
            fail(new Conflict(PlotStateException.getOrNew((Bounce) message)), context);

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
  }

  private static class ConflictStory<T> extends Story<T> {

    private final Actor mActor;

    private ConflictStory(@NotNull final Throwable incident) {
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

  private static class ConflictsStory<T> extends Story<T> {

    private final Actor mActor;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private ConflictsStory(@NotNull final Iterable<? extends Throwable> incidents) {
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

  private static class CrossOverEventuallyStory<T> extends Story<T> {

    private final Actor mActor;
    private final List<Actor> mInputActors;
    private final String mInputThread;
    private final Memory mMemory;
    private final HashSet<Actor> mNextActors = new HashSet<Actor>();
    private final HashMap<String, SenderIterator> mNextSenders =
        new HashMap<String, SenderIterator>();
    private final Options mOptions;

    private HashMap<String, Sender> mGetSenders = new HashMap<String, Sender>();

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private CrossOverEventuallyStory(@NotNull final Memory memory,
        @NotNull final Iterable<? extends Story<? extends T>> stories) {
      mMemory = ConstantConditions.notNull("memory", memory);
      final ArrayList<Actor> inputActors = new ArrayList<Actor>();
      for (final Story<? extends T> story : stories) {
        inputActors.add(story.getActor());
      }
      ConstantConditions.positive("stories size", inputActors.size()); // TODO: 10/02/2019 ???
      mInputActors = inputActors;
      final String actorId = (mActor = BackStage.newActor(new TrampolinePlayScript(Setting.get()) {

        @NotNull
        @Override
        public Behavior getBehavior(@NotNull final String id) {
          return new InitBehavior();
        }
      })).getId();
      mInputThread = actorId + ":input";
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
        if (!sender.tellNext(self)) {
          sender.getSender().tell(END, sender.getOptions(), self);
        }
      }
      context.setBehavior(new DoneBehavior(results, memory, nextSenders));
    }

    private void fail(@NotNull final Conflict conflict, @NotNull final Context context) {
      tellInputActors(BREAK, context);
      final Actor self = context.getSelf();
      for (final Sender sender : mGetSenders.values()) {
        sender.getSender().tell(conflict, sender.getOptions(), self);
      }
      mGetSenders = null;
      final Memory memory = mMemory;
      memory.put(conflict);
      final HashMap<String, SenderIterator> nextSenders = mNextSenders;
      for (final SenderIterator sender : nextSenders.values()) {
        if (!sender.tellNext(self)) {
          sender.getSender().tell(END, sender.getOptions(), self);
        }
      }
      context.setBehavior(new DoneBehavior(conflict, memory, nextSenders));
    }

    private void tellInputActors(final Object message, @NotNull final Context context) {
      final Actor self = context.getSelf();
      final StringBuilder builder = new StringBuilder();
      for (final Actor actor : mInputActors) {
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
          SenderIterator sender = mNextSenders.get(thread);
          if (sender != null) {
            sender.waitNext();

          } else {
            sender = new SenderIterator(envelop.getSender(), options.threadOnly());
            sender.setIterator(mMemory.iterator());
            mNextSenders.put(thread, sender);
          }
          sender.tellNext(context.getSelf());

        } else if (message == BREAK) {
          mNextSenders.remove(envelop.getOptions().getThread());

        } else {
          final String thread = envelop.getOptions().getThread();
          if ((thread != null) && thread.startsWith(mInputThread)) {
            final HashSet<Actor> nextActors = mNextActors;
            nextActors.add(envelop.getSender());
            if (nextActors.size() == mInputActors.size()) {
              fail(Conflict.ofCancel(), context);
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
          tellInputActors(NEXT, context);
          context.setBehavior(new InputBehavior());

        } else if (message == NEXT) {
          final Options options = envelop.getOptions().threadOnly();
          final SenderIterator sender = new SenderIterator(envelop.getSender(), options);
          sender.setIterator(mMemory.iterator());
          mNextSenders.put(options.getThread(), sender);
          tellInputActors(NEXT, context);
          context.setBehavior(new InputBehavior());

        } else if (message == CANCEL) {
          tellInputActors(CANCEL, context);
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
          SenderIterator sender = mNextSenders.get(thread);
          if (sender != null) {
            sender.waitNext();

          } else {
            sender = new SenderIterator(envelop.getSender(), options.threadOnly());
            sender.setIterator(mMemory.iterator());
            mNextSenders.put(thread, sender);
          }

          if (!sender.tellNext(context.getSelf()) && (mNextActors.size() == mInputActors.size())) {
            tellInputActors(NEXT, context);
          }

        } else if (message == BREAK) {
          mNextSenders.remove(envelop.getOptions().getThread());

        } else if (message == CANCEL) {
          tellInputActors(CANCEL, context);
          context.setBehavior(new CancelBehavior());

        } else {
          final String thread = envelop.getOptions().getThread();
          if ((thread != null) && thread.startsWith(mInputThread)) {
            if (message == END) {
              final Actor sender = envelop.getSender();
              sender.tell(BREAK, envelop.getOptions().threadOnly(), context.getSelf());
              final List<Actor> inputActors = mInputActors;
              inputActors.remove(sender);
              if (inputActors.isEmpty()) {
                done(context);
              }

            } else if (message instanceof Bounce) {
              final Conflict conflict = new Conflict(PlotStateException.getOrNew((Bounce) message));
              mMemory.put(conflict);
              final Actor self = context.getSelf();
              for (final SenderIterator sender : mNextSenders.values()) {
                sender.tellNext(self);
              }

              final List<Actor> inputActors = mInputActors;
              inputActors.remove(envelop.getSender());
              if (inputActors.isEmpty()) {
                done(context);
              }

            } else {
              // pass on conflicts
              mMemory.put(message);
              final Actor self = context.getSelf();
              for (final SenderIterator sender : mNextSenders.values()) {
                sender.tellNext(self);
              }

              if (!mGetSenders.isEmpty()) {
                final Actor sender = envelop.getSender();
                final StringBuilder builder = new StringBuilder();
                for (final Actor actor : mInputActors) {
                  builder.append('#');
                  if (actor.equals(sender)) {
                    final String threadId = mInputThread + builder.toString();
                    actor.tell(NEXT, mOptions.withThread(threadId), self);
                  }
                }

              } else {
                mNextActors.add(envelop.getSender());
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

  private static class CrossOverStory<T> extends Story<T> {

    private final Actor mActor;
    private final String mInputThread;
    private final Memory mMemory;
    private final HashMap<String, SenderIterator> mNextSenders =
        new HashMap<String, SenderIterator>();
    private final Options mOptions;
    private final Iterator<? extends Story<? extends T>> mStories;

    private HashMap<String, Sender> mGetSenders = new HashMap<String, Sender>();
    private Actor mInputActor;
    private int mInputCount;
    private String mInputThreadId;

    private CrossOverStory(@NotNull final Memory memory,
        @NotNull final Iterable<? extends Story<? extends T>> stories) {
      mMemory = ConstantConditions.notNull("memory", memory);
      mStories = stories.iterator();
      final String actorId = (mActor = BackStage.newActor(new TrampolinePlayScript(Setting.get()) {

        @NotNull
        @Override
        public Behavior getBehavior(@NotNull final String id) {
          return new InitBehavior();
        }
      })).getId();
      mInputThread = actorId + ":input";
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
        if (!sender.tellNext(self)) {
          sender.getSender().tell(END, sender.getOptions(), self);
        }
      }
      context.setBehavior(new DoneBehavior(results, memory, nextSenders));
    }

    private void fail(@NotNull final Conflict conflict, @NotNull final Context context) {
      final Actor inputActor = mInputActor;
      if (inputActor != null) {
        inputActor.tell(BREAK, mOptions.withThread(mInputThreadId), context.getSelf());
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
        if (!sender.tellNext(self)) {
          sender.getSender().tell(END, sender.getOptions(), self);
        }
      }
      context.setBehavior(new DoneBehavior(conflict, memory, nextSenders));
    }

    @Nullable
    private Actor nextInput(final int count) {
      mInputThreadId = mInputThread + "#" + count;
      final Iterator<? extends Story<? extends T>> stories = mStories;
      Actor inputActor = null;
      while (stories.hasNext()) {
        final Story<? extends T> story = stories.next();
        if (!isEmpty(story)) {
          inputActor = story.getActor();
          break;
        }
      }
      return (mInputActor = inputActor);
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
          SenderIterator sender = mNextSenders.get(thread);
          if (sender != null) {
            sender.waitNext();

          } else {
            sender = new SenderIterator(envelop.getSender(), options.threadOnly());
            sender.setIterator(mMemory.iterator());
            mNextSenders.put(thread, sender);
          }
          sender.tellNext(context.getSelf());

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
          final Actor inputActor = nextInput(mInputCount);
          if (inputActor != null) {
            inputActor.tell(NEXT, mOptions.withThread(mInputThreadId), context.getSelf());
            context.setBehavior(new InputBehavior());

          } else {
            done(context);
          }

        } else if (message == NEXT) {
          final Options options = envelop.getOptions().threadOnly();
          final SenderIterator sender = new SenderIterator(envelop.getSender(), options);
          sender.setIterator(mMemory.iterator());
          mNextSenders.put(options.getThread(), sender);
          final Actor inputActor = nextInput(mInputCount);
          if (inputActor != null) {
            inputActor.tell(NEXT, mOptions.withThread(mInputThreadId), context.getSelf());
            context.setBehavior(new InputBehavior());

          } else {
            done(context);
          }

        } else if (message == CANCEL) {
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
          SenderIterator sender = mNextSenders.get(thread);
          if (sender != null) {
            sender.waitNext();

          } else {
            sender = new SenderIterator(envelop.getSender(), options.threadOnly());
            sender.setIterator(mMemory.iterator());
            mNextSenders.put(thread, sender);
          }
          sender.tellNext(context.getSelf());

        } else if (message == BREAK) {
          mNextSenders.remove(envelop.getOptions().getThread());

        } else if (message == CANCEL) {
          mInputActor.tell(CANCEL, mOptions.withThread(mInputThreadId), context.getSelf());
          context.setBehavior(new CancelBehavior());

        } else if (mInputThreadId.equals(envelop.getOptions().getThread())) {
          if (message == END) {
            final Actor self = context.getSelf();
            mInputActor.tell(BREAK, envelop.getOptions().threadOnly(), self);
            final Actor inputActor = nextInput(++mInputCount);
            if (inputActor != null) {
              inputActor.tell(NEXT, mOptions.withThread(mInputThreadId), self);

            } else {
              done(context);
            }

          } else if (message instanceof Bounce) {
            fail(new Conflict(PlotStateException.getOrNew((Bounce) message)), context);

          } else {
            // pass on conflicts
            mMemory.put(message);
            final Actor self = context.getSelf();
            for (final SenderIterator sender : mNextSenders.values()) {
              sender.tellNext(self);
            }

            if (!mGetSenders.isEmpty()) {
              mInputActor.tell(NEXT, mOptions.withThread(mInputThreadId), self);

            } else {
              context.setBehavior(new NextBehavior());
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
          mInputActor.tell(NEXT, mOptions.withThread(mInputThreadId), context.getSelf());
          context.setBehavior(new InputBehavior());

        } else if (message == NEXT) {
          final Options options = envelop.getOptions();
          final String thread = options.getThread();
          SenderIterator sender = mNextSenders.get(thread);
          if (sender != null) {
            sender.waitNext();

          } else {
            sender = new SenderIterator(envelop.getSender(), options.threadOnly());
            sender.setIterator(mMemory.iterator());
            mNextSenders.put(thread, sender);
          }
          final Actor self = context.getSelf();
          if (!sender.tellNext(self)) {
            mInputActor.tell(NEXT, mOptions.withThread(mInputThreadId), self);
            context.setBehavior(new InputBehavior());
          }

        } else if (message == BREAK) {
          mNextSenders.remove(envelop.getOptions().getThread());

        } else if (message == CANCEL) {
          mInputActor.tell(CANCEL, mOptions.withThread(mInputThreadId), context.getSelf());
          fail(Conflict.ofCancel(), context);
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
        SenderIterator sender = mNextSenders.get(thread);
        if (sender == null) {
          sender = new SenderIterator(envelop.getSender(), options);
          sender.setIterator(mResults.iterator());
          mNextSenders.put(thread, sender);
        }
        sender.waitNext();
        if (!sender.tellNext(self)) {
          sender.getSender().tell(END, options, self);
        }

      } else if (message == BREAK) {
        mNextSenders.remove(envelop.getOptions().getThread());
      }
      envelop.preventReceipt();
    }
  }

  private static class FunctionStory<T> extends AbstractStory<T> {

    private static final NullaryFunction<Event<Boolean>> INDEFINITE_LOOP =
        new NullaryFunction<Event<Boolean>>() {

          public Event<Boolean> call() {
            return Event.ofTrue();
          }
        };

    private final NullaryFunction<? extends Story<T>> mStoryCreator;

    private List<Actor> mActors;

    private FunctionStory(@NotNull final Memory memory,
        @NotNull final NullaryFunction<? extends Story<T>> storyCreator) {
      super(memory, INDEFINITE_LOOP, 1);
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
          story = ofSingleConflict(t);
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

    private final List<Actor> mActors;
    private final UnaryFunction<? super List<T>, ? extends Story<R>> mResolutionHandler;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private GenericStory(@NotNull final Memory memory,
        @NotNull final Iterable<? extends Story<? extends T>> stories,
        @NotNull final NullaryFunction<? extends Event<? extends Boolean>> loopHandler,
        @NotNull final UnaryFunction<? super List<T>, ? extends Story<R>> resolutionHandler) {
      super(memory, loopHandler, Iterables.size(stories));
      final ArrayList<Actor> actors = new ArrayList<Actor>();
      for (final Story<? extends T> story : stories) {
        actors.add(story.getActor());
      }
      ConstantConditions.positive("stories size", actors.size());
      mActors = actors;
      mResolutionHandler = ConstantConditions.notNull("resolutionHandler", resolutionHandler);
    }

    @NotNull
    List<Actor> getInputActors() {
      return mActors;
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

  private static class LineUpEventuallyStory<T> extends Story<T> {

    private final Actor mActor;
    private final List<Actor> mInputActors;
    private final String mInputThread;
    private final Memory mMemory;
    private final HashMap<String, SenderIterator> mNextSenders =
        new HashMap<String, SenderIterator>();
    private final Options mOptions;

    private HashMap<String, Sender> mGetSenders = new HashMap<String, Sender>();
    private int mInputCount;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private LineUpEventuallyStory(@NotNull final Memory memory,
        @NotNull final Iterable<? extends Event<? extends T>> events) {
      mMemory = ConstantConditions.notNull("memory", memory);
      final ArrayList<Actor> inputActors = new ArrayList<Actor>();
      for (final Event<? extends T> event : events) {
        inputActors.add(event.getActor());
      }
      ConstantConditions.positive("events size", inputActors.size());
      mInputActors = inputActors;
      final String actorId = (mActor = BackStage.newActor(new TrampolinePlayScript(Setting.get()) {

        @NotNull
        @Override
        public Behavior getBehavior(@NotNull final String id) {
          return new InitBehavior();
        }
      })).getId();
      mInputThread = actorId + ":input";
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
        if (!sender.tellNext(self)) {
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
        if (!sender.tellNext(self)) {
          sender.getSender().tell(END, sender.getOptions(), self);
        }
      }
      context.setBehavior(new DoneBehavior(conflict, memory, nextSenders));
    }

    private void tellInputActors(final Object message, @NotNull final Context context) {
      final Actor self = context.getSelf();
      final StringBuilder builder = new StringBuilder();
      for (final Actor actor : mInputActors) {
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
          SenderIterator sender = mNextSenders.get(thread);
          if (sender != null) {
            sender.waitNext();

          } else {
            sender = new SenderIterator(envelop.getSender(), options.threadOnly());
            sender.setIterator(mMemory.iterator());
            mNextSenders.put(thread, sender);
          }
          sender.tellNext(context.getSelf());

        } else if (message == BREAK) {
          mNextSenders.remove(envelop.getOptions().getThread());

        } else {
          final String thread = envelop.getOptions().getThread();
          if ((thread != null) && thread.startsWith(mInputThread)) {
            if (++mInputCount == mInputActors.size()) {
              fail(Conflict.ofCancel(), context);
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
          tellInputActors(GET, context);
          context.setBehavior(new InputBehavior());

        } else if (message == NEXT) {
          final Options options = envelop.getOptions().threadOnly();
          final SenderIterator sender = new SenderIterator(envelop.getSender(), options);
          sender.setIterator(mMemory.iterator());
          mNextSenders.put(options.getThread(), sender);
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
          final Options options = envelop.getOptions().threadOnly();
          mGetSenders.put(options.getThread(), new Sender(envelop.getSender(), options));

        } else if (message == NEXT) {
          final Options options = envelop.getOptions();
          final String thread = options.getThread();
          SenderIterator sender = mNextSenders.get(thread);
          if (sender != null) {
            sender.waitNext();

          } else {
            sender = new SenderIterator(envelop.getSender(), options.threadOnly());
            sender.setIterator(mMemory.iterator());
            mNextSenders.put(thread, sender);
          }
          sender.tellNext(context.getSelf());

        } else if (message == BREAK) {
          mNextSenders.remove(envelop.getOptions().getThread());

        } else if (message == CANCEL) {
          tellInputActors(CANCEL, context);
          context.setBehavior(new CancelBehavior());

        } else {
          final String thread = envelop.getOptions().getThread();
          if ((thread != null) && thread.startsWith(mInputThread)) {
            if (message instanceof Bounce) {
              fail(new Conflict(PlotStateException.getOrNew((Bounce) message)), context);

            } else {
              // pass on conflicts
              mMemory.put(message);
              final Actor self = context.getSelf();
              for (final SenderIterator sender : mNextSenders.values()) {
                sender.tellNext(self);
              }

              if (++mInputCount == mInputActors.size()) {
                mInputCount = 0;
                done(context);
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

  private static class LineUpStory<T> extends Story<T> {

    private final Actor mActor;
    private final Iterator<? extends Event<? extends T>> mEvents;
    private final String mInputThread;
    private final Memory mMemory;
    private final HashMap<String, SenderIterator> mNextSenders =
        new HashMap<String, SenderIterator>();
    private final Options mOptions;

    private HashMap<String, Sender> mGetSenders = new HashMap<String, Sender>();
    private Actor mInputActor;
    private int mInputCount;
    private String mInputThreadId;

    private LineUpStory(@NotNull final Memory memory,
        @NotNull final Iterable<? extends Event<? extends T>> events) {
      mMemory = ConstantConditions.notNull("memory", memory);
      mEvents = events.iterator();
      final String actorId = (mActor = BackStage.newActor(new TrampolinePlayScript(Setting.get()) {

        @NotNull
        @Override
        public Behavior getBehavior(@NotNull final String id) {
          return new InitBehavior();
        }
      })).getId();
      mInputThread = actorId + ":input";
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
        if (!sender.tellNext(self)) {
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
        if (!sender.tellNext(self)) {
          sender.getSender().tell(END, sender.getOptions(), self);
        }
      }
      context.setBehavior(new DoneBehavior(conflict, memory, nextSenders));
    }

    @Nullable
    private Actor nextInput(final int count) {
      mInputThreadId = mInputThread + "#" + count;
      final Iterator<? extends Event<? extends T>> events = mEvents;
      return (mInputActor = events.hasNext() ? events.next().getActor() : null);
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
          SenderIterator sender = mNextSenders.get(thread);
          if (sender != null) {
            sender.waitNext();

          } else {
            sender = new SenderIterator(envelop.getSender(), options.threadOnly());
            sender.setIterator(mMemory.iterator());
            mNextSenders.put(thread, sender);
          }
          sender.tellNext(context.getSelf());

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
          final Actor inputActor = nextInput(mInputCount);
          if (inputActor != null) {
            inputActor.tell(GET, mOptions.withThread(mInputThreadId), context.getSelf());
            context.setBehavior(new InputBehavior());

          } else {
            done(context);
          }

        } else if (message == NEXT) {
          final Options options = envelop.getOptions().threadOnly();
          final SenderIterator sender = new SenderIterator(envelop.getSender(), options);
          sender.setIterator(mMemory.iterator());
          mNextSenders.put(options.getThread(), sender);
          final Actor inputActor = nextInput(mInputCount);
          if (inputActor != null) {
            inputActor.tell(GET, mOptions.withThread(mInputThreadId), context.getSelf());
            context.setBehavior(new InputBehavior());

          } else {
            done(context);
          }

        } else if (message == CANCEL) {
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
          SenderIterator sender = mNextSenders.get(thread);
          if (sender != null) {
            sender.waitNext();

          } else {
            sender = new SenderIterator(envelop.getSender(), options.threadOnly());
            sender.setIterator(mMemory.iterator());
            mNextSenders.put(thread, sender);
          }
          sender.tellNext(context.getSelf());

        } else if (message == BREAK) {
          mNextSenders.remove(envelop.getOptions().getThread());

        } else if (message == CANCEL) {
          mInputActor.tell(CANCEL, mOptions.withThread(mInputThreadId), context.getSelf());
          context.setBehavior(new CancelBehavior());

        } else if (mInputThreadId.equals(envelop.getOptions().getThread())) {
          if (message instanceof Bounce) {
            fail(new Conflict(PlotStateException.getOrNew((Bounce) message)), context);

          } else {
            // pass on conflicts
            mMemory.put(message);
            final Actor self = context.getSelf();
            for (final SenderIterator sender : mNextSenders.values()) {
              sender.tellNext(self);
            }

            final Actor inputActor = nextInput(++mInputCount);
            if (inputActor != null) {
              if (!mGetSenders.isEmpty()) {
                inputActor.tell(GET, mOptions.withThread(mInputThreadId), self);

              } else {
                context.setBehavior(new NextBehavior());
              }

            } else {
              done(context);
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
          mInputActor.tell(GET, mOptions.withThread(mInputThreadId), context.getSelf());
          context.setBehavior(new InputBehavior());

        } else if (message == NEXT) {
          final Options options = envelop.getOptions();
          final String thread = options.getThread();
          SenderIterator sender = mNextSenders.get(thread);
          if (sender != null) {
            sender.waitNext();

          } else {
            sender = new SenderIterator(envelop.getSender(), options.threadOnly());
            sender.setIterator(mMemory.iterator());
            mNextSenders.put(thread, sender);
          }
          final Actor self = context.getSelf();
          if (!sender.tellNext(self)) {
            mInputActor.tell(GET, mOptions.withThread(mInputThreadId), self);
            context.setBehavior(new InputBehavior());
          }

        } else if (message == BREAK) {
          mNextSenders.remove(envelop.getOptions().getThread());

        } else if (message == CANCEL) {
          mInputActor.tell(CANCEL, mOptions.withThread(mInputThreadId), context.getSelf());
          fail(Conflict.ofCancel(), context);
        }
        envelop.preventReceipt();
      }
    }

    @NotNull
    Actor getActor() {
      return mActor;
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

  private static class ResolutionStory<T> extends Story<T> {

    private final Actor mActor;

    private ResolutionStory(final T result) {
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

  private static class ResolutionsStory<T> extends Story<T> {

    private final Actor mActor;

    private ResolutionsStory(@NotNull final Iterable<T> results) {
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

  private static class ResolveStory<T> extends AbstractStory<T> {

    private final List<Actor> mActors;
    private final UnaryFunction<? super Throwable, ? extends Story<T>> mConflictHandler;
    private final Set<Class<? extends Throwable>> mConflictTypes;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private ResolveStory(@NotNull final Memory memory, @NotNull final Actor eventActor,
        @NotNull final Set<Class<? extends Throwable>> conflictTypes,
        @NotNull final NullaryFunction<? extends Event<? extends Boolean>> loopHandler,
        @NotNull final UnaryFunction<? super Throwable, ? extends Story<T>> conflictHandler) {
      super(memory, loopHandler, 1);
      mActors = Collections.singletonList(eventActor);
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

  private static class SenderIterator extends Sender {

    private Iterator<?> mIterator;
    private boolean mWaitNext = true;

    private SenderIterator(@NotNull final Actor sender, @NotNull final Options options) {
      super(sender, options);
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

    private final List<Actor> mActors;
    private final UnaryFunction<? super T1, ? extends Story<R>> mResolutionHandler;

    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    private UnaryStory(@NotNull final Memory memory, @NotNull final Story<? extends T1> firstStory,
        @NotNull final NullaryFunction<? extends Event<? extends Boolean>> loopHandler,
        @NotNull final UnaryFunction<? super T1, ? extends Story<R>> resolutionHandler) {
      super(memory, loopHandler, 1);
      mActors = Arrays.asList(firstStory.getActor());
      mResolutionHandler = ConstantConditions.notNull("resolutionHandler", resolutionHandler);
    }

    @NotNull
    List<Actor> getInputActors() {
      return mActors;
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
          final SenderIterator sender = mNextSenders.get(thread);
          if (sender != null) {
            sender.waitNext();

          } else {
            mNextSenders.put(thread, new SenderIterator(envelop.getSender(), options.threadOnly()));
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
          final SenderIterator sender = mNextSenders.get(thread);
          if (sender != null) {
            sender.waitNext();

          } else {
            mNextSenders.put(thread, new SenderIterator(envelop.getSender(), options.threadOnly()));
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
            fail(new Conflict(PlotStateException.getOrNew((Bounce) message)), context);

          } else {
            final Actor self = context.getSelf();
            final Iterable<?> results = (Iterable<?>) message;
            for (final Sender sender : mGetSenders.values()) {
              sender.getSender().tell(results, sender.getOptions(), self);
            }
            mGetSenders = null;
            for (final SenderIterator sender : mNextSenders.values()) {
              sender.setIterator(results.iterator());
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
