package dm.shakespeare.plot;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
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
    return null;
  }

  @NotNull
  public static <T1, R> Story<R> when(@NotNull final Story<? extends T1> firstStory,
      @NotNull final NullaryFunction<? extends Story<? extends Boolean>> loopHandler,
      @NotNull final UnaryFunction<? super T1, ? extends Story<? extends R>> resolutionHandler,
      @NotNull final NullaryFunction<? extends Story<? extends R>> endHandler) {
    return null;
  }

  @NotNull
  public static <T1, R> Story<R> when(@NotNull final Story<? extends T1> firstStory,
      @NotNull final StoryHandler<? super T1, R> storyHandler) {
    return null;
  }

  public void observeAll(@Nullable final Observer<? super T> resolutionObserver,
      @Nullable final Observer<? super Throwable> incidentObserver) {
    observeAll(new DefaultEventObserver<T>(resolutionObserver, incidentObserver));
  }

  public void observeAll(@NotNull final EventObserver<? super T> eventObserver) {
    final Actor actor = BackStage.newActor(new StoryObserverScript<T>(eventObserver));
    getActor().tell(NEXT, new Options().withReceiptId(actor.getId()), actor);
  }

  @NotNull
  public <R> Story<R> resolve(@NotNull final StoryResolver<? super T, R> storyResolver) {
    return null;
  }

  @NotNull
  public <R> Story<R> resolve(
      @NotNull final NullaryFunction<? extends Story<? extends Boolean>> loopHandler,
      @NotNull final UnaryFunction<? super T, ? extends Story<? extends R>> resolutionHandler,
      @NotNull final UnaryFunction<? super Throwable, ? extends Story<? extends R>> incidentHandler,
      @NotNull final NullaryFunction<? extends Story<? extends R>> endHandler) {
    return null;
  }

  @NotNull
  public <R> Story<R> then(@NotNull final StoryHandler<? super T, R> storyHandler) {
    return when(this, storyHandler);
  }

  @NotNull
  public <R> Story<R> then(
      @NotNull final NullaryFunction<? extends Story<? extends Boolean>> loopHandler,
      @NotNull final UnaryFunction<? super T, ? extends Story<? extends R>> messageHandler,
      @NotNull final NullaryFunction<? extends Story<? extends R>> endHandler) {
    return when(this, loopHandler, messageHandler, endHandler);
  }

  public interface StoryHandler<T, R> {

    Story<? extends Boolean> nextEvent() throws Exception;

    Story<R> onEnd() throws Exception;

    Story<R> onResolution(T result) throws Exception;
  }

  public interface StoryResolver<T, R> extends StoryHandler<T, R> {

    Story<R> onIncident(@NotNull Throwable obstacle) throws Exception;
  }

  private static class EventStory<T> extends Story<T> {

    private final Actor mActor;
    private final Actor mEventActor;
    private final HashMap<String, Actor> mGetSenders = new HashMap<String, Actor>();
    private final HashMap<String, Actor> mNextSenders = new HashMap<String, Actor>();

    private EventStory(@NotNull final Event<T> event) {
      mEventActor = event.getActor();
      mActor = BackStage.newActor(new PlayScript(PlayContext.get()) {

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

    private void fail(@NotNull final Incident incident, @NotNull final Context context) {
      final Actor self = context.getSelf();
      for (final Entry<String, Actor> entry : mGetSenders.entrySet()) {
        entry.getValue().tell(incident, new Options().withThread(entry.getKey()), self);
      }
      for (final Entry<String, Actor> entry : mNextSenders.entrySet()) {
        final Options options = new Options().withThread(entry.getKey());
        entry.getValue().tell(incident, options, self).tell(END, options, self);
      }
      context.setBehavior(new IncidentBehavior(incident.getCause()));
    }

    private class InitBehavior extends AbstractBehavior {

      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == GET) {
          final Actor actor = mActor;
          mGetSenders.put(envelop.getOptions().getThread(), envelop.getSender());
          mEventActor.tell(GET,
              new Options().withReceiptId(actor.getId()).withThread(actor.getId()), actor);
          context.setBehavior(new InputBehavior());

        } else if (message == NEXT) {
          final Actor actor = mActor;
          mNextSenders.put(envelop.getOptions().getThread(), envelop.getSender());
          mEventActor.tell(GET,
              new Options().withReceiptId(actor.getId()).withThread(actor.getId()), actor);
          context.setBehavior(new InputBehavior());
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

        } else if (message instanceof Incident) {
          fail((Incident) message, context);

        } else if (message instanceof Bounce) {
          fail(new Incident(PlotStateException.getOrNew((Bounce) message)), context);

        } else {
          final Actor self = context.getSelf();
          final String actorId = self.getId();
          final String thread = envelop.getOptions().getThread();
          if ((thread != null) && thread.startsWith(actorId)) {
            for (final Entry<String, Actor> entry : mGetSenders.entrySet()) {
              entry.getValue().tell(message, new Options().withThread(entry.getKey()), self);
            }
            for (final Entry<String, Actor> entry : mNextSenders.entrySet()) {
              final Options options = new Options().withThread(entry.getKey());
              entry.getValue().tell(message, options, self).tell(END, options, self);
            }
          }
          context.setBehavior(new ResolutionBehavior(message));
        }
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
      final Actor self = context.getSelf();
      final Options options = envelop.getOptions().threadOnly();
      if (message == GET) {
        envelop.getSender().tell(mIncident, options, self);

      } else if (message == NEXT) {
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
      mActor = BackStage.newActor(new PlayScript(PlayContext.get()) {

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

              } else if (message == BREAK) {
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
      final Actor self = context.getSelf();
      final Options options = envelop.getOptions().threadOnly();
      if (message == GET) {
        envelop.getSender().tell(mResult, options, self);

      } else if (message == NEXT) {
        envelop.getSender().tell(mResult, options, self).tell(END, options, self);
      }
    }
  }

  private static class ResolutionStory<T> extends Story<T> {

    private final Actor mActor;

    private ResolutionStory(@NotNull final Iterable<T> results) {
      final List<T> list = Collections.unmodifiableList(Iterables.toList(results));
      mActor = BackStage.newActor(new PlayScript(PlayContext.get()) {

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

              } else if (message == BREAK) {
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
}
