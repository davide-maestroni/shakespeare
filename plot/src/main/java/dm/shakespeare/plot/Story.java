package dm.shakespeare.plot;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import dm.shakespeare.plot.function.Action;
import dm.shakespeare.plot.function.NullaryFunction;
import dm.shakespeare.plot.function.UnaryFunction;
import dm.shakespeare.plot.memory.UnboundMemory;
import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare.util.Iterables;

/**
 * Created by davide-maestroni on 01/22/2019.
 */
public abstract class Story<T> extends Event<Iterable<T>> {

  static final Object BREAK = new Object();
  static final Object END = new Object();
  static final Object NEXT = new Object();

  private static final Action NO_OP = new Action() {

    public void run() {
    }
  };

  @NotNull
  public static <T> Story<T> crossOver(
      @NotNull final Iterable<? extends Story<? extends T>> stories) {
    return null;
  }

  @NotNull
  public static <T> Story<T> crossOverEventually(
      @NotNull final Iterable<? extends Story<? extends T>> stories) {
    return null;
  }

  @NotNull
  public static <T> Story<T> crossOverGreedily(
      @NotNull final Iterable<? extends Story<? extends T>> stories) {
    return null;
  }

  @NotNull
  public static <T> Story<T> lineUp(@NotNull final Iterable<? extends Event<? extends T>> events) {
    return null;
  }

  @NotNull
  public static <T> Story<T> lineUpEventually(
      @NotNull final Iterable<? extends Event<? extends T>> events) {
    return null;
  }

  @NotNull
  public static <T> Story<T> lineUpGreedily(
      @NotNull final Iterable<? extends Event<? extends T>> events) {
    return null;
  }

  @NotNull
  public static <T> Story<T> ofEmpty() {
    return ofResolutions(Collections.<T>emptyList());
  }

  @NotNull
  public static <T> Story<T> ofEvent(@NotNull final Event<T> event) {
    return new EventStory<T>(event);
  }

  @NotNull
  public static <T> Story<T> ofIncidents(@NotNull final Iterable<? extends Throwable> obstacles) {
    return new IncidentStory<T>(obstacles);
  }

  @NotNull
  public static <T> Story<T> ofResolutions(@NotNull final Iterable<T> results) {
    final Setting setting = Setting.get();
    Story<T> story = setting.get(results);
    if (story == null) {
      story = new ResolutionStory<T>(results);
      setting.put(results, story);
    }
    return story;
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
    return new GenericStory<T, R>(new UnboundMemory(), stories, loopHandler, resolutionHandler);
  }

  @NotNull
  public static <T1, R> Story<R> when(@NotNull final Story<? extends T1> firstStory,
      @NotNull final NullaryFunction<? extends Event<? extends Boolean>> loopHandler,
      @NotNull final UnaryFunction<? super T1, ? extends Story<R>> resolutionHandler) {
    return new UnaryStory<T1, R>(new UnboundMemory(), firstStory, loopHandler, resolutionHandler);
  }

  public void observeAll(@Nullable final Observer<? super T> resolutionObserver,
      @Nullable final Observer<? super Throwable> incidentObserver,
      @Nullable final Action endAction) {
    observeAll(new DefaultStoryObserver<T>(resolutionObserver, incidentObserver, endAction));
  }

  public void observeAll(@NotNull final EventObserver<? super T> eventObserver) {
    final Actor actor =
        BackStage.newActor(new StoryObserverScript<T>(new EventStoryObserver<T>(eventObserver)));
    final String threadId = actor.getId();
    getActor().tell(NEXT, new Options().withReceiptId(threadId).withThread(threadId), actor);
  }

  public void observeAll(@NotNull final StoryObserver<? super T> storyObserver) {
    final Actor actor = BackStage.newActor(new StoryObserverScript<T>(storyObserver));
    final String threadId = actor.getId();
    getActor().tell(NEXT, new Options().withReceiptId(threadId).withThread(threadId), actor);
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <E1 extends Throwable> Story<T> resolve(@NotNull final Class<? extends E1> firstType,
      @NotNull final NullaryFunction<? extends Event<? extends Boolean>> loopHandler,
      @NotNull final UnaryFunction<? super Throwable, ? extends Story<? extends T>> incidentHandler) {
    final HashSet<Class<? extends Throwable>> types = new HashSet<Class<? extends Throwable>>();
    types.add(firstType);
    return new ResolveStory<T>(new UnboundMemory(), getActor(), types, loopHandler,
        (UnaryFunction<? super Throwable, ? extends Story<T>>) incidentHandler);
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <E extends Throwable> Story<T> resolve(
      @NotNull final Iterable<? extends Class<? extends E>> incidentTypes,
      @NotNull final NullaryFunction<? extends Event<? extends Boolean>> loopHandler,
      @NotNull final UnaryFunction<? super Throwable, ? extends Story<? extends T>> incidentHandler) {
    return new ResolveStory<T>(new UnboundMemory(), getActor(),
        Iterables.<Class<? extends Throwable>>toSet(incidentTypes), loopHandler,
        (UnaryFunction<? super Throwable, ? extends Story<T>>) incidentHandler);
  }

  @NotNull
  public <R> Story<R> then(
      @NotNull final NullaryFunction<? extends Event<? extends Boolean>> loopHandler,
      @NotNull final UnaryFunction<? super T, ? extends Story<R>> messageHandler) {
    return when(this, loopHandler, messageHandler);
  }

  public interface Memory {

    Object get(int index);

    @NotNull
    Iterable<Object> getAll();

    boolean has(int index);

    int next(int index);

    void put(Object value);
  }

  public interface StoryObserver<T> extends EventObserver<T> {

    void onEnd() throws Exception;
  }

  static class DefaultStoryObserver<T> extends DefaultEventObserver<T> implements StoryObserver<T> {

    private final Action mEndAction;

    DefaultStoryObserver(@Nullable final Observer<? super T> resolutionObserver,
        @Nullable final Observer<? super Throwable> incidentObserver,
        @Nullable final Action endAction) {
      super(resolutionObserver, incidentObserver);
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

    public void onIncident(@NotNull final Throwable obstacle) throws Exception {
      mEventObserver.onIncident(obstacle);
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
    private final Memory mMemory;
    private final Options mOptions;
    private final String mOutputThread;
    private final Setting mSetting;

    private HashMap<String, Sender> mGetSenders = new HashMap<String, Sender>();
    private int mInputCount;
    private int mLoopCount;
    private String mLoopThreadId;
    private HashMap<String, SenderOffset> mNextSenders = new HashMap<String, SenderOffset>();
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
    Actor getIncidentActor(@NotNull final Incident incident) throws Exception {
      return null;
    }

    @NotNull
    abstract List<Actor> getInputActors();

    @Nullable
    Actor getLoopActor() throws Exception {
      Setting.set(getSetting());
      try {
        final Event<? extends Boolean> event = mLoopHandler.call();
        return (event != null) ? event.getActor() : null;

      } finally {
        Setting.unset();
      }
    }

    @Nullable
    Actor getOutputActor(@NotNull final Object[] inputs) throws Exception {
      return null;
    }

    @NotNull
    Setting getSetting() {
      return mSetting;
    }

    private void end(@NotNull final Context context) {
      final Memory memory = mMemory;
      final Actor self = context.getSelf();
      final Iterable<Object> results = memory.getAll();
      for (final Sender sender : mGetSenders.values()) {
        sender.getSender().tell(results, sender.getOptions(), self);
      }
      mGetSenders = null;
      for (final SenderOffset sender : mNextSenders.values()) {
        if (!sender.tellNext(memory, self)) {
          sender.getSender().tell(END, sender.getOptions(), self);
        }
      }
      context.setBehavior(new CachedResolutionBehavior());
    }

    private void fail(@NotNull final Incident incident, @NotNull final Context context) {
      final Actor self = context.getSelf();
      for (final Sender sender : mGetSenders.values()) {
        sender.getSender().tell(incident, sender.getOptions(), self);
      }
      mGetSenders = null;
      final Memory memory = mMemory;
      memory.put(incident);
      for (final SenderOffset sender : mNextSenders.values()) {
        if (!sender.tellNext(memory, self)) {
          sender.getSender().tell(END, sender.getOptions(), self);
        }
      }
      context.setBehavior(new CachedIncidentBehavior(incident));
    }

    private void incident(@NotNull final Incident incident, @NotNull final Context context) {
      try {
        final Actor incidentActor = getIncidentActor(incident);
        if (incidentActor != null) {
          (mOutputActor = incidentActor).tell(NEXT, mOptions.withThread(mOutputThreadId),
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

    private void loop(final int loopCount, @NotNull final Context context) {
      try {
        final Actor loopActor = getLoopActor();
        if (loopActor != null) {
          setThreadIds(loopCount);
          loopActor.tell(GET, mOptions.withThread(mLoopThreadId), context.getSelf());
          context.setBehavior(new LoopBehavior());

        } else {
          end(context);
        }

      } catch (final Throwable t) {
        fail(new Incident(t), context);
        if (t instanceof InterruptedException) {
          Thread.currentThread().interrupt();
        }
      }
    }

    private void setThreadIds(final int count) {
      mLoopThreadId = mLoopThread + "#" + count;
      mOutputThreadId = mOutputThread + "#" + count;
    }

    private class CachedIncidentBehavior extends AbstractBehavior {

      private final Incident mIncident;

      private CachedIncidentBehavior(@NotNull final Incident incident) {
        mIncident = incident;
      }

      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          envelop.getSender().tell(mIncident, envelop.getOptions().threadOnly(), context.getSelf());

        } else if (message == NEXT) {
          final Actor self = context.getSelf();
          final Options options = envelop.getOptions().threadOnly();
          final String thread = options.getThread();
          SenderOffset sender = mNextSenders.get(thread);
          if (sender == null) {
            sender = new SenderOffset(envelop.getSender(), options);
            mNextSenders.put(thread, sender);
          }

          sender.waitNext();
          if (!sender.tellNext(mMemory, self)) {
            sender.getSender().tell(END, options, self);
          }

        } else if (message == BREAK) {
          mNextSenders.remove(envelop.getOptions().getThread());
        }
        envelop.preventReceipt();
      }
    }

    private class CachedResolutionBehavior extends AbstractBehavior {

      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          envelop.getSender()
              .tell(mMemory.getAll(), envelop.getOptions().threadOnly(), context.getSelf());

        } else if (message == NEXT) {
          final Actor self = context.getSelf();
          final Options options = envelop.getOptions().threadOnly();
          final String thread = options.getThread();
          SenderOffset sender = mNextSenders.get(thread);
          if (sender == null) {
            sender = new SenderOffset(envelop.getSender(), options);
            mNextSenders.put(thread, sender);
          }

          sender.waitNext();
          if (!sender.tellNext(mMemory, self)) {
            sender.getSender().tell(END, options, self);
          }

        } else if (message == BREAK) {
          mNextSenders.remove(envelop.getOptions().getThread());

        } else if (message == CANCEL) {
          context.setBehavior(
              new CachedIncidentBehavior(new Incident(new PlotCancelledException())));
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
          loop(mLoopCount, context);

        } else if (message == NEXT) {
          final Options options = envelop.getOptions().threadOnly();
          mNextSenders.put(options.getThread(), new SenderOffset(envelop.getSender(), options));
          loop(mLoopCount, context);

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
          final Options options = envelop.getOptions().threadOnly();
          mGetSenders.put(options.getThread(), new Sender(envelop.getSender(), options));

        } else if (message == NEXT) {
          final Options options = envelop.getOptions();
          final String thread = options.getThread();
          final SenderOffset sender = mNextSenders.get(thread);
          if (sender != null) {
            sender.waitNext();

          } else {
            mNextSenders.put(thread, new SenderOffset(envelop.getSender(), options.threadOnly()));
          }

        } else if (message == BREAK) {
          mNextSenders.remove(envelop.getOptions().getThread());

        } else if (message == CANCEL) {
          fail(new Incident(new PlotCancelledException()), context);

        } else {
          final String inputThread = mInputThread;
          final String thread = envelop.getOptions().getThread();
          if ((thread != null) && thread.startsWith(inputThread)) {
            if (message == END) {
              end(context);

            } else if (message instanceof Incident) {
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
                  mInputCount = 0;
                  try {
                    final Actor self = context.getSelf();
                    final Actor outputActor = getOutputActor(inputs);
                    if (outputActor != null) {
                      (mOutputActor = outputActor).tell(NEXT, mOptions.withThread(mOutputThreadId),
                          self);
                      context.setBehavior(new OutputBehavior());

                    } else {
                      final Memory memory = mMemory;
                      memory.put(inputs[0]);
                      for (final SenderOffset sender : mNextSenders.values()) {
                        sender.tellNext(memory, self);
                      }
                      loop(++mLoopCount, context);
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

    private class LoopBehavior extends AbstractBehavior {

      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          final Options options = envelop.getOptions().threadOnly();
          mGetSenders.put(options.getThread(), new Sender(envelop.getSender(), options));

        } else if (message == NEXT) {
          final Options options = envelop.getOptions();
          final String thread = options.getThread();
          final SenderOffset sender = mNextSenders.get(thread);
          if (sender != null) {
            sender.waitNext();

          } else {
            mNextSenders.put(thread, new SenderOffset(envelop.getSender(), options.threadOnly()));
          }

        } else if (message == BREAK) {
          mNextSenders.remove(envelop.getOptions().getThread());

        } else if (message == CANCEL) {
          fail(new Incident(new PlotCancelledException()), context);

        } else {
          final String thread = envelop.getOptions().getThread();
          final String loopThread = mLoopThreadId;
          if (loopThread.equals(thread)) {
            if (message instanceof Incident) {
              fail((Incident) message, context);

            } else if (message instanceof Bounce) {
              final Incident incident = new Incident(PlotStateException.getOrNew((Bounce) message));
              fail(incident, context);

            } else if (!(message instanceof Receipt)) {
              if (Boolean.TRUE.equals(message)) {
                final Actor self = context.getSelf();
                final StringBuilder builder = new StringBuilder();
                for (final Actor actor : getInputActors()) {
                  final String threadId = mInputThread + builder.append('#').toString();
                  actor.tell(NEXT, mOptions.withThread(threadId), self);
                }
                context.setBehavior(new InputBehavior());

              } else {
                end(context);
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

        } else if (message == NEXT) {
          final Options options = envelop.getOptions();
          final String thread = options.getThread();
          SenderOffset sender = mNextSenders.get(thread);
          if (sender != null) {
            sender.waitNext();

          } else {
            sender = new SenderOffset(envelop.getSender(), options.threadOnly());
            mNextSenders.put(thread, sender);
          }

          final Actor self = context.getSelf();
          if (!sender.tellNext(mMemory, self)) {
            mOutputActor.tell(NEXT, mOptions.withThread(mOutputThreadId), self);
          }

        } else if (message == BREAK) {
          mNextSenders.remove(envelop.getOptions().getThread());

        } else if (message == CANCEL) {
          fail(new Incident(new PlotCancelledException()), context);

        } else if (mOutputThreadId.equals(envelop.getOptions().getThread())) {
          // pass on incidents
          if (message == END) {
            mOutputActor.tell(BREAK, new Options().withThread(mOutputThreadId), context.getSelf());
            mOutputActor = null;
            loop(++mLoopCount, context);

          } else if (message instanceof Bounce) {
            final Incident incident = new Incident(PlotStateException.getOrNew((Bounce) message));
            fail(incident, context);

          } else if (!(message instanceof Receipt)) {
            final Memory memory = mMemory;
            memory.put(message);
            final Actor self = context.getSelf();
            for (final SenderOffset sender : mNextSenders.values()) {
              sender.tellNext(memory, self);
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
          SenderOffset sender = mNextSenders.get(thread);
          if (sender != null) {
            sender.waitNext();

          } else {
            sender = new SenderOffset(envelop.getSender(), options.threadOnly());
            mNextSenders.put(thread, sender);
          }
          sender.tellNext(mMemory, context.getSelf());

        } else if (message == BREAK) {
          mNextSenders.remove(envelop.getOptions().getThread());

        } else if (message == CANCEL) {
          fail(new Incident(new PlotCancelledException()), context);

        } else if (mOutputThreadId.equals(envelop.getOptions().getThread())) {
          // pass on incidents
          if (message == END) {
            mOutputActor.tell(BREAK, new Options().withThread(mOutputThreadId), context.getSelf());
            mOutputActor = null;
            loop(++mLoopCount, context);

          } else if (message instanceof Bounce) {
            final Incident incident = new Incident(PlotStateException.getOrNew((Bounce) message));
            fail(incident, context);

          } else if (!(message instanceof Receipt)) {
            final Memory memory = mMemory;
            memory.put(message);
            final Actor self = context.getSelf();
            for (final SenderOffset sender : mNextSenders.values()) {
              sender.tellNext(memory, self);
            }
            context.setBehavior(new NextBehavior());
          }
        }
        envelop.preventReceipt();
      }
    }
  }

  private static class EventStory<T> extends Story<T> {

    private final Actor mActor;
    private final Actor mEventActor;
    private final Options mOptions;

    private HashMap<String, Sender> mGetSenders = new HashMap<String, Sender>();
    private HashMap<String, Sender> mNextSenders = new HashMap<String, Sender>();

    private EventStory(@NotNull final Event<T> event) {
      mEventActor = event.getActor();
      final String actorId = (mActor = BackStage.newActor(new TrampolinePlayScript(Setting.get()) {

        @NotNull
        @Override
        public Behavior getBehavior(@NotNull final String id) {
          return new InitBehavior();
        }
      })).getId();
      mOptions = new Options().withReceiptId(actorId).withThread(actorId);
    }

    private void fail(@NotNull final Incident incident, @NotNull final Context context) {
      final Actor self = context.getSelf();
      for (final Sender sender : mGetSenders.values()) {
        sender.getSender().tell(incident, sender.getOptions(), self);
      }
      mGetSenders = null;
      for (final Sender sender : mNextSenders.values()) {
        sender.getSender().tell(incident, sender.getOptions(), self);
      }
      context.setBehavior(new ResolutionBehavior(incident));
    }

    private class InitBehavior extends AbstractBehavior {

      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          final Options options = envelop.getOptions().threadOnly();
          mGetSenders.put(options.getThread(), new Sender(envelop.getSender(), options));
          mEventActor.tell(GET, mOptions, context.getSelf());
          context.setBehavior(new InputBehavior());

        } else if (message == NEXT) {
          final Options options = envelop.getOptions().threadOnly();
          mNextSenders.put(options.getThread(), new Sender(envelop.getSender(), options));
          mEventActor.tell(GET, mOptions, context.getSelf());
          context.setBehavior(new InputBehavior());

        } else if (message == CANCEL) {
          // TODO: 03/02/2019 ???
          final Actor eventActor = mEventActor;
          if (!envelop.getSender().equals(eventActor)) {
            eventActor.tell(CANCEL, mOptions.threadOnly(), context.getSelf());
          }
          fail(new Incident(new PlotCancelledException()), context);
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
          mNextSenders.put(options.getThread(), new Sender(envelop.getSender(), options));

        } else if (message == BREAK) {
          mNextSenders.remove(envelop.getOptions().getThread());

        } else if (message == CANCEL) {
          fail(new Incident(new PlotCancelledException()), context);

        } else {
          final Actor self = context.getSelf();
          if (self.getId().equals(envelop.getOptions().getThread())) {
            if (message instanceof Incident) {
              fail((Incident) message, context);

            } else if (message instanceof Bounce) {
              fail(new Incident(PlotStateException.getOrNew((Bounce) message)), context);

            } else if (!(message instanceof Receipt)) {
              for (final Sender sender : mGetSenders.values()) {
                sender.getSender().tell(message, sender.getOptions(), self);
              }
              mGetSenders = null;
              for (final Sender sender : mNextSenders.values()) {
                sender.getSender().tell(message, sender.getOptions(), self);
              }
              context.setBehavior(new ResolutionBehavior(message));
            }
          }
        }
        envelop.preventReceipt();
      }
    }

    private class ResolutionBehavior extends AbstractBehavior {

      private final Object mResult;

      private ResolutionBehavior(final Object result) {
        mResult = result;
      }

      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          envelop.getSender().tell(mResult, envelop.getOptions().threadOnly(), context.getSelf());

        } else if (message == NEXT) {
          final Actor self = context.getSelf();
          final String thread = envelop.getOptions().getThread();
          final Sender sender = mNextSenders.get(thread);
          if (sender != null) {
            sender.getSender().tell(END, sender.getOptions(), self);

          } else {
            final Options options = envelop.getOptions().threadOnly();
            mNextSenders.put(thread, new Sender(envelop.getSender(), options));
            envelop.getSender().tell(mResult, options, self);
          }

        } else if (message == BREAK) {
          mNextSenders.remove(envelop.getOptions().getThread());
        }
        envelop.preventReceipt();
      }
    }

    @NotNull
    Actor getActor() {
      return mActor;
    }
  }

  private static class GenericStory<T, R> extends AbstractStory<R> {

    private final List<Actor> mActors;
    private final UnaryFunction<? super List<T>, ? extends Story<R>> mResolutionHandler;

    private GenericStory(@NotNull final Memory memory,
        @NotNull final Iterable<? extends Story<? extends T>> stories,
        @NotNull final NullaryFunction<? extends Event<? extends Boolean>> loopHandler,
        @NotNull final UnaryFunction<? super List<T>, ? extends Story<R>> resolutionHandler) {
      super(memory, loopHandler, Iterables.size(stories));
      final ArrayList<Actor> actors = new ArrayList<Actor>();
      for (final Story<? extends T> story : stories) {
        actors.add(story.getActor());
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
        final Story<R> story = mResolutionHandler.call(inputList);
        return ((story != null) ? story : ofEmpty()).getActor();

      } finally {
        Setting.unset();
      }
    }
  }

  private static class IncidentStory<T> extends Story<T> {

    private final Actor mActor;

    private IncidentStory(@NotNull final Iterable<? extends Throwable> obstacles) {
      final ArrayList<Incident> incidents = new ArrayList<Incident>();
      for (final Throwable obstacle : obstacles) {
        incidents.add(new Incident(obstacle));
      }
      ConstantConditions.positive("obstacles size", incidents.size());
      mActor = BackStage.newActor(new TrampolinePlayScript(Setting.get()) {

        private final HashMap<String, Iterator<Incident>> mThreads =
            new HashMap<String, Iterator<Incident>>();

        @NotNull
        @Override
        public Behavior getBehavior(@NotNull final String id) {
          return new AbstractBehavior() {

            public void onMessage(final Object message, @NotNull final Envelop envelop,
                @NotNull final Context context) {
              if (message == GET) {
                envelop.getSender()
                    .tell(Iterables.first(incidents), envelop.getOptions().threadOnly(),
                        context.getSelf());

              } else if (message == NEXT) {
                final HashMap<String, Iterator<Incident>> threads = mThreads;
                final Options options = envelop.getOptions().threadOnly();
                final String thread = options.getThread();
                Iterator<Incident> iterator = threads.get(thread);
                if (iterator == null) {
                  iterator = incidents.iterator();
                  threads.put(thread, iterator);
                }
                envelop.getSender()
                    .tell(iterator.hasNext() ? iterator.next() : END, options, context.getSelf());

              } else if ((message == CANCEL) || (message == BREAK)) {
                // TODO: 01/02/2019 CANCEL != BREAK
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

  private static class ResolutionStory<T> extends Story<T> {

    private final Actor mActor;

    private ResolutionStory(@NotNull final Iterable<T> results) {
      final List<T> list = Collections.unmodifiableList(Iterables.toList(results));
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
                    .tell(list, envelop.getOptions().threadOnly(), context.getSelf());

              } else if (message == NEXT) {
                final HashMap<String, Iterator<T>> threads = mThreads;
                final Options options = envelop.getOptions().threadOnly();
                final String thread = options.getThread();
                Iterator<T> iterator = threads.get(thread);
                if (iterator == null) {
                  iterator = list.iterator();
                  threads.put(thread, iterator);
                }
                envelop.getSender()
                    .tell(iterator.hasNext() ? iterator.next() : END, options, context.getSelf());

              } else if ((message == CANCEL) || (message == BREAK)) {
                // TODO: 01/02/2019 CANCEL != BREAK
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
    private final UnaryFunction<? super Throwable, ? extends Story<T>> mIncidentHandler;
    private final Set<Class<? extends Throwable>> mIncidentTypes;

    private ResolveStory(@NotNull final Memory memory, @NotNull final Actor eventActor,
        @NotNull final Set<Class<? extends Throwable>> incidentTypes,
        @NotNull final NullaryFunction<? extends Event<? extends Boolean>> loopHandler,
        @NotNull final UnaryFunction<? super Throwable, ? extends Story<T>> incidentHandler) {
      super(memory, loopHandler, 1);
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
            final Story<T> story = mIncidentHandler.call(obstacle);
            return ((story != null) ? story : ofEmpty()).getActor();

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

  private static class SenderIterator extends Sender {

    private Iterator<?> mIterator;
    private boolean mWaitNext = true;

    private SenderIterator(@NotNull final Actor sender, @NotNull final Options options) {
      super(sender, options);
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

  private static class SenderOffset extends Sender {

    private int mCount = 0;
    private boolean mWaitNext = true;

    private SenderOffset(@NotNull final Actor sender, @NotNull final Options options) {
      super(sender, options);
    }

    boolean tellNext(@NotNull final Memory memory, @NotNull final Actor self) {
      final int count = mCount;
      if (mWaitNext && memory.has(count)) {
        mWaitNext = false;
        getSender().tell(memory.get(count), getOptions(), self);
        mCount = memory.next(count);
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

    @NotNull
    @SuppressWarnings("unchecked")
    Actor getOutputActor(@NotNull final Object[] inputs) throws Exception {
      Setting.set(getSetting());
      try {
        final Story<R> story = mResolutionHandler.call((T1) inputs[0]);
        return ((story != null) ? story : ofEmpty()).getActor();

      } finally {
        Setting.unset();
      }
    }
  }

  private static class UnfoldStory<T> extends Story<T> {

    private final Actor mActor;
    private final Actor mEventActor;
    private final Options mOptions;

    private HashMap<String, Sender> mGetSenders = new HashMap<String, Sender>();
    private HashMap<String, SenderIterator> mNextSenders = new HashMap<String, SenderIterator>();
    private Iterable<? extends T> mResults;

    private UnfoldStory(@NotNull final Event<? extends Iterable<T>> event) {
      mEventActor = event.getActor();
      final String actorId = (mActor = BackStage.newActor(new TrampolinePlayScript(Setting.get()) {

        @NotNull
        @Override
        public Behavior getBehavior(@NotNull final String id) {
          return new InitBehavior();
        }
      })).getId();
      mOptions = new Options().withReceiptId(actorId).withThread(actorId);
    }

    private void fail(@NotNull final Incident incident, @NotNull final Context context) {
      final Actor self = context.getSelf();
      for (final Sender sender : mGetSenders.values()) {
        sender.getSender().tell(incident, sender.getOptions(), self);
      }
      mGetSenders = null;
      final Iterator<Object> iterator = Collections.emptyIterator();
      for (final SenderIterator sender : mNextSenders.values()) {
        sender.setIterator(iterator);
        sender.getSender().tell(incident, sender.getOptions(), self);
      }
      context.setBehavior(new IncidentBehavior(incident));
    }

    private class IncidentBehavior extends AbstractBehavior {

      private final Incident mIncident;

      private IncidentBehavior(@NotNull final Incident incident) {
        mIncident = incident;
      }

      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          envelop.getSender().tell(mIncident, envelop.getOptions().threadOnly(), context.getSelf());

        } else if (message == NEXT) {
          final Options options = envelop.getOptions();
          final String thread = options.getThread();
          SenderIterator sender = mNextSenders.get(thread);
          if (sender != null) {
            sender.getSender().tell(END, sender.getOptions(), context.getSelf());

          } else {
            sender = new SenderIterator(envelop.getSender(), options.threadOnly());
            mNextSenders.put(thread, sender);
            sender.getSender().tell(mIncident, sender.getOptions(), context.getSelf());
          }

        } else if (message == CANCEL) {
          fail(new Incident(new PlotCancelledException()), context);

        } else if (message == BREAK) {
          mNextSenders.remove(envelop.getOptions().getThread());
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
          mEventActor.tell(GET, mOptions, context.getSelf());
          context.setBehavior(new InputBehavior());

        } else if (message == NEXT) {
          final Options options = envelop.getOptions().threadOnly();
          mNextSenders.put(options.getThread(), new SenderIterator(envelop.getSender(), options));
          mEventActor.tell(GET, mOptions, context.getSelf());
          context.setBehavior(new InputBehavior());

        } else if (message == CANCEL) {
          final Actor eventActor = mEventActor;
          if (!envelop.getSender().equals(eventActor)) {
            final Actor self = context.getSelf();
            eventActor.tell(CANCEL, new Options().withThread(self.getId()), self);
          }
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
            mNextSenders.put(thread, sender);
          }

        } else if (message == CANCEL) {
          final Actor eventActor = mEventActor;
          if (!envelop.getSender().equals(eventActor)) {
            final Actor self = context.getSelf();
            eventActor.tell(GET, new Options().withThread(self.getId()), self);
          }
          fail(new Incident(new PlotCancelledException()), context);

        } else if (message == BREAK) {
          mNextSenders.remove(envelop.getOptions().getThread());

        } else {
          final Actor self = context.getSelf();
          final String actorId = self.getId();
          if (actorId.equals(envelop.getOptions().getThread())) {
            if (message instanceof Incident) {
              fail((Incident) message, context);

            } else if (message instanceof Bounce) {
              fail(new Incident(PlotStateException.getOrNew((Bounce) message)), context);

            } else if (!(message instanceof Receipt)) {
              final Iterable<? extends T> results = (mResults = (Iterable<? extends T>) message);
              for (final Sender sender : mGetSenders.values()) {
                sender.getSender().tell(results, sender.getOptions(), self);
              }
              mGetSenders = null;
              for (final SenderIterator sender : mNextSenders.values()) {
                sender.setIterator(results.iterator());
                if (!sender.tellNext(self)) {
                  sender.getSender().tell(END, sender.getOptions(), self);
                }
              }
              context.setBehavior(new OutputBehavior());
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
          envelop.getSender().tell(mResults, envelop.getOptions().threadOnly(), context.getSelf());

        } else if (message == NEXT) {
          final Options options = envelop.getOptions();
          final String thread = options.getThread();
          SenderIterator sender = mNextSenders.get(thread);
          if (sender != null) {
            sender.waitNext();

          } else {
            sender = new SenderIterator(envelop.getSender(), options.threadOnly());
            mNextSenders.put(thread, sender);
          }
          final Actor self = context.getSelf();
          if (!sender.tellNext(self)) {
            sender.getSender().tell(END, sender.getOptions(), self);
          }

        } else if (message == CANCEL) {
          fail(new Incident(new PlotCancelledException()), context);

        } else if (message == BREAK) {
          mNextSenders.remove(envelop.getOptions().getThread());
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
