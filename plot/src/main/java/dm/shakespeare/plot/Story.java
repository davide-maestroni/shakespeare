package dm.shakespeare.plot;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

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
import dm.shakespeare.plot.function.NullaryFunction;
import dm.shakespeare.plot.function.UnaryFunction;
import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare.util.Iterables;

/**
 * Created by davide-maestroni on 01/22/2019.
 */
public abstract class Story<T> extends Event<Iterable<T>> {

  static final Object BREAK = new Object();
  static final Object END = new Object();
  static final Object NEXT = new Object();

  private static final Story<?> EMPTY_STORY = ofResolutions(Collections.emptyList());

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
  @SuppressWarnings("unchecked")
  public static <T> Story<T> ofEmpty() {
    return (Story<T>) EMPTY_STORY;
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
    return new ResolutionStory<T>(results);
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
    return null;
  }

  @NotNull
  public static <T1, R> Story<R> when(@NotNull final Story<? extends T1> firstStory,
      @NotNull final NullaryFunction<? extends Event<? extends Boolean>> loopHandler,
      @NotNull final UnaryFunction<? super T1, ? extends Story<R>> resolutionHandler) {
    return null;
  }

  public void observeAll(@Nullable final Observer<? super T> resolutionObserver,
      @Nullable final Observer<? super Throwable> incidentObserver) {
    observeAll(new DefaultEventObserver<T>(resolutionObserver, incidentObserver));
  }

  public void observeAll(@NotNull final EventObserver<? super T> eventObserver) {
    final Actor actor = BackStage.newActor(new StoryObserverScript<T>(eventObserver));
    final String threadId = actor.getId();
    getActor().tell(NEXT, new Options().withReceiptId(threadId).withThread(threadId), actor);
  }

  @NotNull
  public <T1, R> Story<R> resolve(@NotNull final Class<? extends T1> firstType,
      @NotNull final NullaryFunction<? extends Event<? extends Boolean>> loopHandler,
      @NotNull final UnaryFunction<? super T1, ? extends Story<? extends R>> resolutionHandler,
      @NotNull final UnaryFunction<? super Throwable, ? extends Story<? extends R>> incidentHandler) {
    return null;
  }

  @NotNull
  public <T extends Throwable, R> Story<R> resolve(
      @NotNull final Iterable<? extends Class<? extends T>> incidentTypes,
      @NotNull final NullaryFunction<? extends Event<? extends Boolean>> loopHandler,
      @NotNull final UnaryFunction<? super T, ? extends Story<? extends R>> resolutionHandler,
      @NotNull final UnaryFunction<? super Throwable, ? extends Story<? extends R>> incidentHandler) {
    return null;
  }

  @NotNull
  public <R> Story<R> then(
      @NotNull final NullaryFunction<? extends Event<? extends Boolean>> loopHandler,
      @NotNull final UnaryFunction<? super T, ? extends Story<R>> messageHandler) {
    return when(this, loopHandler, messageHandler);
  }

  public interface Cache<T> {

    void add(T value);

    @NotNull
    Iterable<T> getAll();

    boolean hasNext(int offset);

    T nextFrom(int offset);
  }

  private abstract static class AbstractStory<T> extends Story<T> {

    private final Actor mActor;
    private final Cache<? extends T> mCache;
    private final Object[] mInputs;
    private final NullaryFunction<? extends Event<? extends Boolean>> mLoopHandler;
    private final PlayContext mPlayContext;

    private HashMap<String, Actor> mGetSenders = new HashMap<String, Actor>();
    private int mInputCount;
    private HashMap<String, SenderOffset> mNextSenders = new HashMap<String, SenderOffset>();
    private int mOutputCount;
    private String mOutputThread;

    private AbstractStory(@NotNull final Cache<? extends T> cache,
        @NotNull final NullaryFunction<? extends Event<? extends Boolean>> loopHandler,
        final int numInputs) {
      mCache = ConstantConditions.notNull("cache", cache);
      mLoopHandler = ConstantConditions.notNull("loopHandler", loopHandler);
      mInputs = new Object[numInputs];
      final PlayContext playContext = (mPlayContext = PlayContext.get());
      mActor = BackStage.newActor(new PlayScript(playContext) {

        @NotNull
        @Override
        public Behavior getBehavior(@NotNull final String id) {
          return new InitBehavior();
        }
      });
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

    private void end(@NotNull final Context context) {
      final Actor self = context.getSelf();
      final Iterable<? extends T> results = mCache.getAll();
      for (final Entry<String, Actor> entry : mGetSenders.entrySet()) {
        entry.getValue().tell(results, new Options().withThread(entry.getKey()), self);
      }
      mGetSenders = null;
      for (final Entry<String, SenderOffset> entry : mNextSenders.entrySet()) {
        entry.getValue().getSender().tell(END, new Options().withThread(entry.getKey()), self);
      }
      mNextSenders.clear();
      context.setBehavior(new CachedResolutionBehavior());
    }

    private void fail(@NotNull final Incident incident, @NotNull final Context context) {
      final Actor self = context.getSelf();
      for (final Entry<String, Actor> entry : mGetSenders.entrySet()) {
        entry.getValue().tell(incident, new Options().withThread(entry.getKey()), self);
      }
      mGetSenders = null;
      for (final Entry<String, SenderOffset> entry : mNextSenders.entrySet()) {
        entry.getValue().getSender().tell(incident, new Options().withThread(entry.getKey()), self);
      }
      context.setBehavior(new CachedIncidentBehavior(incident));
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
          final String thread = envelop.getOptions().getThread();
          final Options options = envelop.getOptions().threadOnly();
          // TODO: 01/02/2019 WTF???? 
          SenderOffset senderOffset = mNextSenders.get(thread);
          if (senderOffset != null) {
            senderOffset.getSender().tell(END, options, self);

          } else {
            senderOffset = new SenderOffset(envelop.getSender());
            if (senderOffset.tellNext(mCache)) {
              mNextSenders.put(thread, senderOffset);

            } else {
              senderOffset.getSender().tell(mIncident, options, self);
            }
          }
        }
      }
    }

    private class CachedResolutionBehavior extends AbstractBehavior {

      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          envelop.getSender()
              .tell(mCache.getAll(), envelop.getOptions().threadOnly(), context.getSelf());

        } else if (message == NEXT) {
          final Actor self = context.getSelf();
          final String thread = envelop.getOptions().getThread();
          final Options options = envelop.getOptions().threadOnly();
          SenderOffset senderOffset = mNextSenders.get(thread);
          if (senderOffset != null) {
            senderOffset.getSender().tell(END, options, self);

          } else {
            senderOffset = new SenderOffset(envelop.getSender());
            if (senderOffset.tellNext(mCache)) {
              mNextSenders.put(thread, senderOffset);

            } else {
              senderOffset.getSender().tell(END, options, self);
            }
          }

        } else if (message == CANCEL) {
          context.setBehavior(
              new CachedIncidentBehavior(new Incident(new PlotCancelledException())));
        }
      }
    }

    private class InitBehavior extends AbstractBehavior {

      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          final String thread = envelop.getOptions().getThread();
          mGetSenders.put(thread, envelop.getSender());
          try {
            final Event<? extends Boolean> event = mLoopHandler.call();
            if (event != null) {
              final Actor self = context.getSelf();
              final String outputThread = (mOutputThread = self.getId() + "#" + mOutputCount);
              event.getActor()
                  .tell(GET, new Options().withReceiptId(outputThread).withThread(outputThread),
                      self);
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

        } else if (message == NEXT) {
          final String thread = envelop.getOptions().getThread();
          mNextSenders.put(thread, new SenderOffset(envelop.getSender()));
          try {
            final Event<? extends Boolean> event = mLoopHandler.call();
            if (event != null) {
              final Actor self = context.getSelf();
              final String outputThread = (mOutputThread = self.getId() + "#" + mOutputCount);
              event.getActor()
                  .tell(GET, new Options().withReceiptId(outputThread).withThread(outputThread),
                      self);
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
          final String thread = envelop.getOptions().getThread();
          mGetSenders.put(thread, envelop.getSender());

        } else if (message == NEXT) {
          final String thread = envelop.getOptions().getThread();
          mNextSenders.put(thread, new SenderOffset(envelop.getSender()));

        } else if (message == BREAK) {
          mNextSenders.remove(envelop.getOptions().getThread());

        } else if (message == CANCEL) {
          fail(new Incident(new PlotCancelledException()), context);

        } else if (message instanceof Incident) {
          try {
            final Actor incidentActor = getIncidentActor((Incident) message);
            if (incidentActor != null) {
              final Actor self = context.getSelf();
              final String threadId = mOutputThread;
              incidentActor.tell(NEXT, new Options().withReceiptId(threadId).withThread(threadId),
                  self);
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
              final String threadId = mOutputThread;
              incidentActor.tell(NEXT, new Options().withReceiptId(threadId).withThread(threadId),
                  self);
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

        } else if (!(message instanceof Receipt)) {
          final Actor self = context.getSelf();
          final String outputThread = mOutputThread;
          final String thread = envelop.getOptions().getThread();
          if ((thread != null) && thread.startsWith(outputThread)) {
            final int index = thread.length() - outputThread.length() - 1;
            final Object[] inputs = mInputs;
            if ((index >= 0) && (index < inputs.length)) {
              inputs[index] = message;
              if (++mInputCount == inputs.length) {
                try {
                  final Actor outputActor = getOutputActor(inputs);
                  if (outputActor != null) {
                    outputActor.tell(NEXT,
                        new Options().withReceiptId(outputThread).withThread(outputThread), self);
                    context.setBehavior(new OutputBehavior());

                  } else {
                    @SuppressWarnings("UnnecessaryLocalVariable") final Cache<? extends T> cache =
                        mCache;
                    for (final Entry<String, SenderOffset> entry : mNextSenders.entrySet()) {
                      final SenderOffset senderOffset = entry.getValue();
                      if (senderOffset.tellNext(cache)) {
                        senderOffset.getSender()
                            .tell(inputs[0], new Options().withThread(entry.getKey()), self);
                      }
                    }
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
    }

    private class LoopBehavior extends AbstractBehavior {

      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          final String thread = envelop.getOptions().getThread();
          mGetSenders.put(thread, envelop.getSender());

        } else if (message == NEXT) {
          final String thread = envelop.getOptions().getThread();
          mNextSenders.put(thread, new SenderOffset(envelop.getSender()));

        } else if (message == BREAK) {
          mNextSenders.remove(envelop.getOptions().getThread());

        } else if (message == CANCEL) {
          fail(new Incident(new PlotCancelledException()), context);

        } else if (message instanceof Incident) {
          fail((Incident) message, context);

        } else if (message instanceof Bounce) {
          final Incident incident = new Incident(PlotStateException.getOrNew((Bounce) message));
          fail(incident, context);

        } else if (!(message instanceof Receipt)) {
          final Actor self = context.getSelf();
          final String thread = envelop.getOptions().getThread();
          final String outputThread = mOutputThread;
          if (outputThread.equals(thread) && Boolean.TRUE.equals(message)) {
            final StringBuilder builder = new StringBuilder();
            for (final Actor actor : getInputActors()) {
              final String threadId = outputThread + builder.append('#').toString();
              actor.tell(NEXT, new Options().withReceiptId(threadId).withThread(threadId), self);
            }
            context.setBehavior(new InputBehavior());
          }
        }
      }
    }

    private class NextBehavior extends AbstractBehavior {

      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          final String thread = envelop.getOptions().getThread();
          mGetSenders.put(thread, envelop.getSender());

        } else if (message == NEXT) {
          final String thread = envelop.getOptions().getThread();
          mNextSenders.put(thread, new SenderOffset(envelop.getSender()));

        } else if (message == BREAK) {
          mNextSenders.remove(envelop.getOptions().getThread());

        } else if (message == CANCEL) {
          fail(new Incident(new PlotCancelledException()), context);

        } else if (message instanceof Incident) {
          fail((Incident) message, context);

        } else if (message instanceof Bounce) {
          final Incident incident = new Incident(PlotStateException.getOrNew((Bounce) message));
          fail(incident, context);

        } else if (!(message instanceof Receipt)) {
          final Actor self = context.getSelf();
          final String thread = envelop.getOptions().getThread();
          final String outputThread = mOutputThread;
          if (outputThread.equals(thread) && Boolean.TRUE.equals(message)) {
            // TODO: 01/02/2019 mOutputActor.NEXT
            context.setBehavior(new OutputBehavior());
          }
        }
      }
    }

    private class OutputBehavior extends AbstractBehavior {

      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          final String thread = envelop.getOptions().getThread();
          mGetSenders.put(thread, envelop.getSender());

        } else if (message == NEXT) {
          final String thread = envelop.getOptions().getThread();
          mNextSenders.put(thread, new SenderOffset(envelop.getSender()));

        } else if (message == BREAK) {
          mNextSenders.remove(envelop.getOptions().getThread());

        } else if (message == CANCEL) {
          fail(new Incident(new PlotCancelledException()), context);

        } else if (message instanceof Incident) {
          fail((Incident) message, context);

        } else if (message instanceof Bounce) {
          final Incident incident = new Incident(PlotStateException.getOrNew((Bounce) message));
          fail(incident, context);

        } else if (!(message instanceof Receipt)) {
          final Actor self = context.getSelf();
          final String thread = envelop.getOptions().getThread();
          if (mOutputThread.equals(thread)) {
            if (message == END) {
              end(context);

            } else {
              @SuppressWarnings("UnnecessaryLocalVariable") final Cache<? extends T> cache = mCache;
              for (final Entry<String, SenderOffset> entry : mNextSenders.entrySet()) {
                final SenderOffset senderOffset = entry.getValue();
                if (senderOffset.tellNext(cache)) {
                  senderOffset.getSender()
                      .tell(message, new Options().withThread(entry.getKey()), self);
                }
              }

              // TODO: 01/02/2019 when to break?

              final String outputThread = (mOutputThread = self.getId() + "#" + ++mOutputCount);
              try {
                final Event<? extends Boolean> event = mLoopHandler.call();
                if (event != null) {
                  event.getActor()
                      .tell(GET, new Options().withReceiptId(outputThread).withThread(outputThread),
                          self);
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
          }
        }
      }
    }
  }

  private static class EventStory<T> extends Story<T> {

    private final Actor mActor;
    private final Actor mEventActor;

    private HashMap<String, Actor> mGetSenders = new HashMap<String, Actor>();
    private HashMap<String, Actor> mNextSenders = new HashMap<String, Actor>();

    private EventStory(@NotNull final Event<T> event) {
      mEventActor = event.getActor();
      mActor = BackStage.newActor(new PlayScript(CONSTANT_CONTEXT) {

        @NotNull
        @Override
        public Behavior getBehavior(@NotNull final String id) {
          return new InitBehavior();
        }
      });
    }

    private void fail(@NotNull final Incident incident, @NotNull final Context context) {
      final Actor self = context.getSelf();
      for (final Entry<String, Actor> entry : mGetSenders.entrySet()) {
        entry.getValue().tell(incident, new Options().withThread(entry.getKey()), self);
      }
      mGetSenders = null;
      for (final Entry<String, Actor> entry : mNextSenders.entrySet()) {
        final Options options = new Options().withThread(entry.getKey());
        // TODO: 01/02/2019 FIX END
        entry.getValue().tell(incident, options, self).tell(END, options, self);
      }
      mNextSenders = null;
      context.setBehavior(new IncidentBehavior(incident.getCause()));
    }

    private class InitBehavior extends AbstractBehavior {

      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          final Actor self = context.getSelf();
          final String threadId = self.getId();
          mGetSenders.put(envelop.getOptions().getThread(), envelop.getSender());
          mEventActor.tell(GET, new Options().withReceiptId(threadId).withThread(threadId), self);
          context.setBehavior(new InputBehavior());

        } else if (message == NEXT) {
          final Actor self = context.getSelf();
          final String threadId = self.getId();
          mNextSenders.put(envelop.getOptions().getThread(), envelop.getSender());
          mEventActor.tell(GET, new Options().withReceiptId(threadId).withThread(threadId), self);
          context.setBehavior(new InputBehavior());

        } else if (message == CANCEL) {
          final Actor eventActor = mEventActor;
          if (!envelop.getSender().equals(eventActor)) {
            final Actor self = context.getSelf();
            eventActor.tell(CANCEL, new Options().withThread(self.getId()), self);
          }
          fail(new Incident(new PlotCancelledException()), context);
        }
      }
    }

    private class InputBehavior extends AbstractBehavior {

      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          mGetSenders.put(envelop.getOptions().getThread(), envelop.getSender());

        } else if (message == NEXT) {
          mNextSenders.put(envelop.getOptions().getThread(), envelop.getSender());

        } else if (message == CANCEL) {
          final Actor eventActor = mEventActor;
          if (!envelop.getSender().equals(eventActor)) {
            final Actor self = context.getSelf();
            eventActor.tell(GET, new Options().withThread(self.getId()), self);
          }
          fail(new Incident(new PlotCancelledException()), context);

        } else if (message instanceof Incident) {
          fail((Incident) message, context);

        } else if (message instanceof Bounce) {
          fail(new Incident(PlotStateException.getOrNew((Bounce) message)), context);

        } else if (!(message instanceof Receipt)) {
          final Actor self = context.getSelf();
          final String actorId = self.getId();
          final String thread = envelop.getOptions().getThread();
          if ((thread != null) && thread.startsWith(actorId)) {
            for (final Entry<String, Actor> entry : mGetSenders.entrySet()) {
              entry.getValue().tell(message, new Options().withThread(entry.getKey()), self);
            }
            mGetSenders = null;
            for (final Entry<String, Actor> entry : mNextSenders.entrySet()) {
              final Options options = new Options().withThread(entry.getKey());
              // TODO: 01/02/2019 FIX END
              entry.getValue().tell(message, options, self).tell(END, options, self);
            }
            mNextSenders = null;
          }
          context.setBehavior(new ResolutionBehavior(message));
        }
      }
    }

    @NotNull
    Actor getActor() {
      return mActor;
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

      } else if (message == NEXT) {
        final Actor self = context.getSelf();
        final Options options = envelop.getOptions().threadOnly();
        // TODO: 01/02/2019 FIX END
        envelop.getSender().tell(mIncident, options, self).tell(END, options, self);
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
      mActor = BackStage.newActor(new PlayScript(CONSTANT_CONTEXT) {

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
                final Options options = envelop.getOptions();
                final String thread = options.getThread();
                Iterator<Incident> iterator = threads.get(thread);
                if (iterator == null) {
                  iterator = incidents.iterator();
                  threads.put(thread, iterator);
                }
                envelop.getSender()
                    .tell(iterator.hasNext() ? iterator.next() : END, options.threadOnly(),
                        context.getSelf());

              } else if ((message == CANCEL) || (message == BREAK)) {
                mThreads.remove(envelop.getOptions().getThread());
              }
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

  private static class ResolutionBehavior extends AbstractBehavior {

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
        final Options options = envelop.getOptions().threadOnly();
        // TODO: 01/02/2019 FIX END
        envelop.getSender().tell(mResult, options, self).tell(END, options, self);
      }
    }
  }

  private static class ResolutionStory<T> extends Story<T> {

    private final Actor mActor;

    private ResolutionStory(@NotNull final Iterable<T> results) {
      final List<T> list = Collections.unmodifiableList(Iterables.toList(results));
      mActor = BackStage.newActor(new PlayScript(CONSTANT_CONTEXT) {

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
                final Options options = envelop.getOptions();
                final String thread = options.getThread();
                Iterator<T> iterator = threads.get(thread);
                if (iterator == null) {
                  iterator = list.iterator();
                  threads.put(thread, iterator);
                }
                envelop.getSender()
                    .tell(iterator.hasNext() ? iterator.next() : END, options.threadOnly(),
                        context.getSelf());

              } else if ((message == CANCEL) || (message == BREAK)) {
                mThreads.remove(envelop.getOptions().getThread());
              }
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

  private static class SenderOffset {

    private final Actor mSender;

    private int mCount = 0;

    private SenderOffset(@NotNull final Actor sender) {
      mSender = sender;
    }

    @NotNull
    public Actor getSender() {
      return mSender;
    }

    public boolean tellNext(@NotNull final Cache<?> cache) {
      if (cache.hasNext(mCount)) {
        ++mCount;
        return true;
      }
      return false;
    }
  }

  private static class UnaryStory<T1, R> extends AbstractStory<R> {

    private final List<Actor> mActors;
    private final UnaryFunction<? super T1, ? extends Story<R>> mResolutionHandler;

    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    private UnaryStory(@NotNull final Cache<? extends R> cache,
        @NotNull final Story<? extends T1> firstStory,
        @NotNull final NullaryFunction<? extends Event<? extends Boolean>> loopHandler,
        @NotNull final UnaryFunction<? super T1, ? extends Story<R>> resolutionHandler) {
      super(cache, loopHandler, 1);
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
      PlayContext.set(getContext());
      try {
        final Story<R> story = mResolutionHandler.call((T1) inputs[0]);
        return ((story != null) ? story : ofEmpty()).getActor();

      } finally {
        PlayContext.unset();
      }
    }
  }

  private static class UnfoldStory<T> extends Story<T> {

    private final Actor mActor;
    private final Actor mEventActor;
    private final HashMap<String, Iterator<? extends T>> mThreads =
        new HashMap<String, Iterator<? extends T>>();

    private HashMap<String, Actor> mGetSenders = new HashMap<String, Actor>();
    private HashMap<String, Actor> mNextSenders = new HashMap<String, Actor>();
    private Iterable<? extends T> mResults;

    private UnfoldStory(@NotNull final Event<? extends Iterable<T>> event) {
      mEventActor = event.getActor();
      mActor = BackStage.newActor(new PlayScript(CONSTANT_CONTEXT) {

        @NotNull
        @Override
        public Behavior getBehavior(@NotNull final String id) {
          return new InitBehavior();
        }
      });
    }

    private void fail(@NotNull final Incident incident, @NotNull final Context context) {
      final Actor self = context.getSelf();
      for (final Entry<String, Actor> entry : mGetSenders.entrySet()) {
        entry.getValue().tell(incident, new Options().withThread(entry.getKey()), self);
      }
      mGetSenders = null;
      for (final Entry<String, Actor> entry : mNextSenders.entrySet()) {
        final Options options = new Options().withThread(entry.getKey());
        // TODO: 01/02/2019 FIX END
        entry.getValue().tell(incident, options, self).tell(END, options, self);
      }
      mNextSenders = null;
      context.setBehavior(new IncidentBehavior(incident.getCause()));
    }

    private class InitBehavior extends AbstractBehavior {

      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          final Actor self = context.getSelf();
          final String threadId = self.getId();
          mGetSenders.put(envelop.getOptions().getThread(), envelop.getSender());
          mEventActor.tell(GET, new Options().withReceiptId(threadId).withThread(threadId), self);
          context.setBehavior(new InputBehavior());

        } else if (message == NEXT) {
          final Actor self = context.getSelf();
          final String threadId = self.getId();
          mNextSenders.put(envelop.getOptions().getThread(), envelop.getSender());
          mEventActor.tell(GET, new Options().withReceiptId(threadId).withThread(threadId), self);
          context.setBehavior(new InputBehavior());

        } else if (message == CANCEL) {
          final Actor eventActor = mEventActor;
          if (!envelop.getSender().equals(eventActor)) {
            final Actor self = context.getSelf();
            eventActor.tell(CANCEL, new Options().withThread(self.getId()), self);
          }
          fail(new Incident(new PlotCancelledException()), context);
        }
      }
    }

    private class InputBehavior extends AbstractBehavior {

      @SuppressWarnings("unchecked")
      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          mGetSenders.put(envelop.getOptions().getThread(), envelop.getSender());

        } else if (message == NEXT) {
          mNextSenders.put(envelop.getOptions().getThread(), envelop.getSender());

        } else if (message == CANCEL) {
          final Actor eventActor = mEventActor;
          if (!envelop.getSender().equals(eventActor)) {
            final Actor self = context.getSelf();
            eventActor.tell(GET, new Options().withThread(self.getId()), self);
          }
          fail(new Incident(new PlotCancelledException()), context);

        } else if (message == BREAK) {
          mNextSenders.remove(envelop.getOptions().getThread());

        } else if (message instanceof Incident) {
          fail((Incident) message, context);

        } else if (message instanceof Bounce) {
          fail(new Incident(PlotStateException.getOrNew((Bounce) message)), context);

        } else if (!(message instanceof Receipt)) {
          final Actor self = context.getSelf();
          final String actorId = self.getId();
          final String thread = envelop.getOptions().getThread();
          if ((thread != null) && thread.startsWith(actorId)) {
            final Iterable<? extends T> results = (mResults = (Iterable<? extends T>) message);
            for (final Entry<String, Actor> entry : mGetSenders.entrySet()) {
              entry.getValue().tell(message, new Options().withThread(entry.getKey()), self);
            }
            mGetSenders = null;
            @SuppressWarnings(
                "UnnecessaryLocalVariable") final HashMap<String, Iterator<? extends T>> threads =
                mThreads;
            for (final Entry<String, Actor> entry : mNextSenders.entrySet()) {
              final Options options = new Options().withThread(entry.getKey());
              final Iterator<? extends T> iterator = results.iterator();
              threads.put(entry.getKey(), iterator);
              entry.getValue()
                  .tell(iterator.hasNext() ? iterator.next() : END, options, context.getSelf());
            }
            mNextSenders = null;
          }
          context.setBehavior(new OutputBehavior());
        }
      }
    }

    private class OutputBehavior extends AbstractBehavior {

      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          envelop.getSender().tell(mResults, envelop.getOptions().threadOnly(), context.getSelf());

        } else if (message == NEXT) {
          final HashMap<String, Iterator<? extends T>> threads = mThreads;
          final Options options = envelop.getOptions();
          final String thread = options.getThread();
          Iterator<? extends T> iterator = threads.get(thread);
          if (iterator == null) {
            iterator = mResults.iterator();
            threads.put(thread, iterator);
          }
          envelop.getSender()
              .tell(iterator.hasNext() ? iterator.next() : END, options.threadOnly(),
                  context.getSelf());

        } else if (message == CANCEL) {
          fail(new Incident(new PlotCancelledException()), context);

        } else if (message == BREAK) {
          mThreads.remove(envelop.getOptions().getThread());
        }
      }
    }

    @NotNull
    Actor getActor() {
      return mActor;
    }
  }
}
