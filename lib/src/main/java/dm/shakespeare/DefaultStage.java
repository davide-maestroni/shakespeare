package dm.shakespeare;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Actor.ActorSet;
import dm.shakespeare.actor.Actor.Envelop;
import dm.shakespeare.actor.ActorBuilder;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Stage;
import dm.shakespeare.executor.ExecutorServices;
import dm.shakespeare.function.Mapper;
import dm.shakespeare.function.Provider;
import dm.shakespeare.function.Tester;
import dm.shakespeare.log.LogPrinters;
import dm.shakespeare.log.Logger;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 09/12/2018.
 */
class DefaultStage implements Stage {

  private static final Behavior DEFAULT_BEHAVIOR = new Behavior() {

    public void message(final Object message, @NotNull final Envelop envelop,
        @NotNull final Context context) {
    }

    public void start(@NotNull final Context context) {
    }

    public void stop(@NotNull final Context context) {
    }
  };
  private static final Provider<? extends Behavior> DEFAULT_BEHAVIOR_PROVIDER =
      new Provider<Behavior>() {

        public Behavior get() {
          return DEFAULT_BEHAVIOR;
        }
      };
  private static final ScheduledExecutorService DEFAULT_EXECUTOR =
      ExecutorServices.newDynamicScheduledThreadPool(new ThreadFactory() {

        private final AtomicLong mCount = new AtomicLong();

        public Thread newThread(@NotNull final Runnable runnable) {
          return new Thread(runnable, "shakespeare-thread-" + mCount.getAndIncrement());
        }
      });
  private static final Mapper<? super String, ? extends ExecutorService> DEFAULT_EXECUTOR_MAPPER =
      new Mapper<String, ExecutorService>() {

        public ExecutorService apply(final String value) {
          return DEFAULT_EXECUTOR;
        }
      };
  private static final Mapper<? super String, ? extends Logger> DEFAULT_LOGGER_MAPPER =
      new Mapper<String, Logger>() {

        public Logger apply(final String value) {
          return Logger.newLogger(LogPrinters.javaLoggingPrinter(value));
        }
      };
  private static final Mapper<? super String, ? extends Integer> DEFAULT_QUOTA_MAPPER =
      new Mapper<String, Integer>() {

        public Integer apply(final String value) {
          return Integer.MAX_VALUE;
        }
      };
  private static final Tester<? super String> DEFAULT_TESTER = new Tester<String>() {

    public boolean test(final String value) {
      return false;
    }
  };

  private final Map<String, Actor> mActors;
  private final Object mMutex = new Object();
  private final String mName;
  private final StageNotifier mStageNotifier;

  private long mCount = Long.MIN_VALUE;

  DefaultStage(@NotNull final String name) {
    this(name, new HashMap<String, Actor>());
  }

  DefaultStage(@NotNull final String name, @NotNull final Map<String, Actor> actorMap) {
    mName = ConstantConditions.notNull("name", name);
    mActors = ConstantConditions.notNull("actorMap", actorMap);
    mStageNotifier = new StageNotifier(name);
  }

  @NotNull
  static Provider<? extends Behavior> defaultBehaviorProvider() {
    return DEFAULT_BEHAVIOR_PROVIDER;
  }

  @NotNull
  static Mapper<? super String, ? extends ExecutorService> defaultExecutorMapper() {
    return DEFAULT_EXECUTOR_MAPPER;
  }

  @NotNull
  static Tester<? super String> defaultInterruptTester() {
    return DEFAULT_TESTER;
  }

  @NotNull
  static Mapper<? super String, ? extends Logger> defaultLoggerMapper() {
    return DEFAULT_LOGGER_MAPPER;
  }

  @NotNull
  static Tester<? super String> defaultPreventTester() {
    return DEFAULT_TESTER;
  }

  @NotNull
  static Mapper<? super String, ? extends Integer> defaultQuotaMapper() {
    return DEFAULT_QUOTA_MAPPER;
  }

  public void addMonitor(@NotNull final Actor monitor) {
    mStageNotifier.addMonitor(monitor);
  }

  @NotNull
  public ActorSet findAll(@NotNull final Pattern idPattern) {
    return findAll(new PatternTester(ConstantConditions.notNull("idPattern", idPattern)));
  }

  @NotNull
  public ActorSet findAll(@NotNull final Tester<? super Actor> tester) {
    ConstantConditions.notNull("tester", tester);
    final HashSet<Actor> actors;
    synchronized (mMutex) {
      actors = new HashSet<Actor>(mActors.values());
    }

    try {
      final Iterator<Actor> iterator = actors.iterator();
      while (iterator.hasNext()) {
        final Actor actor = iterator.next();
        if ((actor != null) && !tester.test(actor)) {
          iterator.remove();
        }
      }

    } catch (final RuntimeException e) {
      throw e;

    } catch (final Exception e) {
      throw new RuntimeException(e);
    }

    return new DefaultActorSet(actors);
  }

  @NotNull
  public Actor findAny(@NotNull final Pattern idPattern) {
    return findAny(new PatternTester(ConstantConditions.notNull("idPattern", idPattern)));
  }

  @NotNull
  public Actor findAny(@NotNull final Tester<? super Actor> tester) {
    ConstantConditions.notNull("tester", tester);
    final ArrayList<Actor> actors;
    synchronized (mMutex) {
      actors = new ArrayList<Actor>(mActors.values());
    }

    try {
      for (final Actor actor : actors) {
        if ((actor != null) && tester.test(actor)) {
          return actor;
        }
      }

    } catch (final RuntimeException e) {
      throw e;

    } catch (final Exception e) {
      throw new RuntimeException(e);
    }

    throw new IllegalArgumentException("cannot find an actor satisfying the tester: " + tester);
  }

  @NotNull
  public Actor get(@NotNull final String id) {
    ConstantConditions.notNull("id", id);
    final Actor actor;
    synchronized (mMutex) {
      actor = mActors.get(id);
    }

    if (actor == null) {
      throw new IllegalArgumentException("cannot find an actor with ID: " + id);
    }

    return actor;
  }

  @NotNull
  public ActorSet getAll() {
    final HashSet<Actor> actors;
    synchronized (mMutex) {
      actors = new HashSet<Actor>(mActors.values());
    }

    return new DefaultActorSet(actors);
  }

  @NotNull
  public String getName() {
    return mName;
  }

  @NotNull
  public ActorBuilder newActor() {
    return new DefaultActorBuilder();
  }

  public void removeMonitor(@NotNull final Actor monitor) {
    mStageNotifier.removeMonitor(monitor);
  }

  void removeActor(@NotNull final String id) {
    final Actor removed;
    synchronized (mMutex) {
      removed = mActors.remove(id);
    }

    if (removed != null) {
      mStageNotifier.remove(id);
    }
  }

  private static class PatternTester implements Tester<Actor> {

    private final Pattern mPattern;

    PatternTester(@NotNull final Pattern idPattern) {
      mPattern = idPattern;
    }

    public boolean test(final Actor actor) {
      return mPattern.matcher(actor.getId()).matches();
    }
  }

  private class DefaultActorBuilder implements ActorBuilder {

    private String mActorId;
    private Provider<? extends Behavior> mBehaviorProvider = DEFAULT_BEHAVIOR_PROVIDER;
    private Mapper<? super String, ? extends ExecutorService> mExecutorMapper =
        DEFAULT_EXECUTOR_MAPPER;
    private Tester<? super String> mInterruptTester = DEFAULT_TESTER;
    private Mapper<? super String, ? extends Logger> mLoggerMapper = DEFAULT_LOGGER_MAPPER;
    private Tester<? super String> mPreventTester = DEFAULT_TESTER;
    private Mapper<? super String, ? extends Integer> mQuotaMapper = DEFAULT_QUOTA_MAPPER;

    @NotNull
    public ActorBuilder behavior(@NotNull final Provider<? extends Behavior> provider) {
      mBehaviorProvider = ConstantConditions.notNull("provider", provider);
      return this;
    }

    @NotNull
    public Actor build() {
      final Map<String, Actor> actors = mActors;
      String actorId;
      while (true) {
        if (mActorId != null) {
          actorId = mActorId;

        } else {
          synchronized (mMutex) {
            actorId = Actor.class.getName() + "#" + mCount++;
          }
        }

        final DefaultActor actor;
        final DefaultContext context;
        try {
          final boolean mayInterruptIfRunning = mInterruptTester.test(actorId);
          final boolean preventDefault = mPreventTester.test(actorId);
          final Integer quota = mQuotaMapper.apply(actorId);
          final ExecutorService executor = mExecutorMapper.apply(actorId);
          final Logger logger = mLoggerMapper.apply(actorId);
          actor = new DefaultActor(actorId);
          context =
              new DefaultContext(DefaultStage.this, actor, mBehaviorProvider, mayInterruptIfRunning,
                  preventDefault, quota, executor, logger);
          actor.setContext(context);

        } catch (final RuntimeException e) {
          throw e;

        } catch (final Exception e) {
          throw new RuntimeException(e);
        }

        synchronized (mMutex) {
          if (actors.containsKey(actorId)) {
            if (mActorId != null) {
              throw new IllegalStateException(
                  "an actor with the same ID already exists: " + actorId);
            }

          } else {
            actors.put(actorId, actor);
            mStageNotifier.create(actorId);
            return actor;
          }
        }
      }
    }

    @NotNull
    public ActorBuilder executor(
        @NotNull final Mapper<? super String, ? extends ExecutorService> mapper) {
      mExecutorMapper = ConstantConditions.notNull("mapper", mapper);
      return this;
    }

    @NotNull
    public ActorBuilder id(@NotNull final String id) {
      mActorId = ConstantConditions.notNull("mapper", id);
      return this;
    }

    @NotNull
    public ActorBuilder logger(@NotNull final Mapper<? super String, ? extends Logger> mapper) {
      mLoggerMapper = ConstantConditions.notNull("mapper", mapper);
      return this;
    }

    @NotNull
    public ActorBuilder mayInterruptIfRunning(@NotNull final Tester<? super String> tester) {
      mInterruptTester = ConstantConditions.notNull("tester", tester);
      return this;
    }

    @NotNull
    public ActorBuilder preventDefault(@NotNull final Tester<? super String> tester) {
      mPreventTester = ConstantConditions.notNull("tester", tester);
      return this;
    }

    @NotNull
    public ActorBuilder quota(@NotNull final Mapper<? super String, ? extends Integer> mapper) {
      mQuotaMapper = ConstantConditions.notNull("mapper", mapper);
      return this;
    }
  }
}
