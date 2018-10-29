package dm.shakespeare;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.BehaviorBuilder;
import dm.shakespeare.actor.Stage;
import dm.shakespeare.actor.ThreadMessage;
import dm.shakespeare.executor.ExecutorServices;
import dm.shakespeare.function.Mapper;
import dm.shakespeare.function.Provider;
import dm.shakespeare.function.Tester;
import dm.shakespeare.log.LogPrinters;
import dm.shakespeare.log.Logger;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 09/10/2018.
 */
public abstract class ActorTemplate implements Actor {

  private static final ActorCreator DEFAULT_ACTOR_CREATOR = new ActorCreator() {

    @NotNull
    public Actor create(@NotNull final ActorTemplate template) {
      return template.mActor;
    }
  };
  private static final ActorCreator INIT_ACTOR_CREATOR = new ActorCreator() {

    @NotNull
    public Actor create(@NotNull final ActorTemplate template) throws Exception {
      template.init();
      final Actor actor = template.mActor =
          template.mStage.newActor().id(template.mId).behavior(new Provider<Behavior>() {

            private boolean mIsReset;

            public Behavior get() throws Exception {
              final ActorTemplate actor = mIsReset ? template.reset() : template;
              mIsReset = true;
              return actor.buildBehavior();
            }
          }).executor(new Mapper<String, ExecutorService>() {

            public ExecutorService apply(final String id) throws Exception {
              return template.buildExecutor();
            }
          }).mayInterruptIfRunning(new Tester<String>() {

            public boolean test(final String value) throws Exception {
              return template.mayInterruptIfRunning();
            }
          }).preventDefault(new Tester<String>() {

            public boolean test(final String value) throws Exception {
              return template.preventDefault();
            }
          }).quota(new Mapper<String, Integer>() {

            public Integer apply(final String value) throws Exception {
              return template.quota();
            }
          }).logger(new Mapper<String, Logger>() {

            public Logger apply(final String value) throws Exception {
              return template.buildLogger();
            }
          }).build();
      template.mActorCreator = DEFAULT_ACTOR_CREATOR;
      return actor;
    }
  };

  private static final AtomicLong sCount = new AtomicLong(Long.MIN_VALUE);
  private static final ExecutorService sExecutor =
      ExecutorServices.withThrottling(1, ExecutorServices.trampolineExecutor());
  private static final Logger sLogger =
      Logger.newLogger(LogPrinters.javaLoggingPrinter(ActorTemplate.class.getName()));

  private final String mId;
  private final Stage mStage;

  private volatile Actor mActor;
  private ActorCreator mActorCreator = INIT_ACTOR_CREATOR;

  // temp actor
  public ActorTemplate() {
    this(BackStage.defaultInstance());
  }

  // temp actor
  public ActorTemplate(@NotNull final String id) {
    this(BackStage.defaultInstance(), id);
  }

  // stage actor
  public ActorTemplate(@NotNull final Stage stage) {
    mStage = ConstantConditions.notNull("stage", stage);
    mId = getClass().getName() + "#" + sCount.getAndIncrement();
  }

  // stage actor
  public ActorTemplate(@NotNull final Stage stage, @NotNull final String id) {
    mStage = ConstantConditions.notNull("stage", stage);
    mId = ConstantConditions.notNull("id", id);
  }

  @NotNull
  protected static BehaviorBuilder newBehavior() {
    return new DefaultBehaviorBuilder();
  }

  @NotNull
  public ActorTemplate forward(final Object message, @NotNull final Envelop envelop,
      @NotNull final Actor sender) {
    sExecutor.execute(new ActorRunnable() {

      void runSafe() throws Exception {
        mActorCreator.create(ActorTemplate.this).forward(message, envelop, sender);
      }
    });
    return this;
  }

  @NotNull
  public final String getId() {
    return mId;
  }

  public void remove() {
    sExecutor.execute(new ActorRunnable() {

      void runSafe() throws Exception {
        mActorCreator.create(ActorTemplate.this).remove();
      }
    });
  }

  @NotNull
  public ActorTemplate tell(final Object message, @NotNull final Actor sender) {
    sExecutor.execute(new ActorRunnable() {

      void runSafe() throws Exception {
        mActorCreator.create(ActorTemplate.this).tell(message, sender);
      }
    });
    return this;
  }

  @NotNull
  public ActorTemplate tellAll(@NotNull final Iterable<?> messages, @NotNull final Actor sender) {
    sExecutor.execute(new ActorRunnable() {

      void runSafe() throws Exception {
        mActorCreator.create(ActorTemplate.this).tellAll(messages, sender);
      }
    });
    return this;
  }

  @NotNull
  public <T> Conversation<T> thread(@NotNull final String threadId, @NotNull final Actor sender,
      @NotNull final Collection<? extends Class<? extends ThreadMessage>> messageFilters) {
    return new ConversationTemplate<T>(threadId, sender, messageFilters);
  }

  @NotNull
  public final Stage getStage() {
    return mStage;
  }

  @NotNull
  public final ActorTemplate start() {
    sExecutor.execute(new ActorRunnable() {

      void runSafe() throws Exception {
        mActorCreator.create(ActorTemplate.this);
      }
    });
    return this;
  }

  @NotNull
  protected Behavior buildBehavior() throws Exception {
    return DefaultStage.defaultBehaviorProvider().get();
  }

  @NotNull
  protected ExecutorService buildExecutor() throws Exception {
    return DefaultStage.defaultExecutorMapper().apply(mId);
  }

  @NotNull
  protected Logger buildLogger() throws Exception {
    return DefaultStage.defaultLoggerMapper().apply(mId);
  }

  protected void init() throws Exception {
  }

  protected boolean mayInterruptIfRunning() throws Exception {
    return DefaultStage.defaultInterruptTester().test(mId);
  }

  protected boolean preventDefault() throws Exception {
    return DefaultStage.defaultPreventTester().test(mId);
  }

  protected int quota() throws Exception {
    return DefaultStage.defaultQuotaMapper().apply(mId);
  }

  @NotNull
  protected ActorTemplate reset() throws Exception {
    return ConstantConditions.unsupported("please provide your own implementation");
  }

  private interface ActorCreator {

    @NotNull
    Actor create(@NotNull ActorTemplate template) throws Exception;
  }

  private interface ConversationCreator<T> {

    @NotNull
    Conversation<T> create(@NotNull ActorTemplate template, @NotNull String threadId,
        @NotNull Actor sender,
        @NotNull Collection<? extends Class<? extends ThreadMessage>> messageFilters) throws
        Exception;
  }

  private abstract class ActorRunnable implements Runnable {

    public final void run() {
      try {
        runSafe();

      } catch (final Exception e) {
        sLogger.err(e);
        mActor = Shakespeare.backStage().newActor().id(mId).behavior(new Provider<Behavior>() {

          public Behavior get() throws Exception {
            throw e;
          }
        }).executor(DefaultStage.defaultExecutorMapper()).build();
      }
    }

    abstract void runSafe() throws Exception;
  }

  private class ConversationTemplate<T> implements Conversation<T> {

    private final ArrayList<Class<? extends ThreadMessage>> mMessageFilters;
    private final SingleActorSet mRecipients;
    private final Actor mSender;
    private final String mThreadId;

    private Conversation<T> mConversation;
    private ConversationCreator<T> mConversationCreator;

    private ConversationTemplate(@NotNull final String threadId, @NotNull final Actor sender,
        @NotNull final Collection<? extends Class<? extends ThreadMessage>> messageFilters) {
      mThreadId = ConstantConditions.notNull("threadId", threadId);
      mSender = ConstantConditions.notNull("sender", sender);
      mMessageFilters = new ArrayList<Class<? extends ThreadMessage>>(messageFilters);
      mRecipients = new SingleActorSet(ActorTemplate.this);
      mConversationCreator = new ConversationCreator<T>() {

        @NotNull
        @SuppressWarnings("unchecked")
        public Conversation<T> create(@NotNull final ActorTemplate template,
            @NotNull final String threadId, @NotNull final Actor sender,
            @NotNull final Collection<? extends Class<? extends ThreadMessage>> messageFilters)
            throws
            Exception {
          mConversation =
              template.mActorCreator.create(template).thread(threadId, sender, messageFilters);
          mConversationCreator = new ConversationCreator() {

            @NotNull
            public Conversation create(@NotNull final ActorTemplate template,
                @NotNull final String threadId, @NotNull final Actor sender,
                @NotNull final Collection messageFilters) {
              return mConversation;
            }
          };
          return mConversation;
        }
      };
    }

    public void abort() {
      sExecutor.execute(new ActorRunnable() {

        void runSafe() throws Exception {
          mConversationCreator.create(ActorTemplate.this, mThreadId, mSender, mMessageFilters)
              .abort();
        }
      });
    }

    public void close() {
      sExecutor.execute(new ActorRunnable() {

        void runSafe() throws Exception {
          mConversationCreator.create(ActorTemplate.this, mThreadId, mSender, mMessageFilters)
              .close();
        }
      });
    }

    @NotNull
    public Conversation forward(final Object message, @NotNull final Envelop envelop) {
      sExecutor.execute(new ActorRunnable() {

        void runSafe() throws Exception {
          mConversationCreator.create(ActorTemplate.this, mThreadId, mSender, mMessageFilters)
              .forward(message, envelop);
        }
      });
      return this;
    }

    @NotNull
    public ActorSet getRecipients() {
      return mRecipients;
    }

    @NotNull
    public String getThreadId() {
      return mThreadId;
    }

    @NotNull
    public Conversation<T> tell(final T message) {
      sExecutor.execute(new ActorRunnable() {

        void runSafe() throws Exception {
          mConversationCreator.create(ActorTemplate.this, mThreadId, mSender, mMessageFilters)
              .tell(message);
        }
      });
      return this;
    }

    @NotNull
    public Conversation<T> tellAll(@NotNull final Iterable<? extends T> messages) {
      sExecutor.execute(new ActorRunnable() {

        void runSafe() throws Exception {
          mConversationCreator.create(ActorTemplate.this, mThreadId, mSender, mMessageFilters)
              .tellAll(messages);
        }
      });
      return this;
    }
  }
}
