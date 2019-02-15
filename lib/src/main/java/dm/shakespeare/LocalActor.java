package dm.shakespeare;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.Options;
import dm.shakespeare.log.Logger;
import dm.shakespeare.message.QuotaExceeded;
import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare.util.Iterables;

/**
 * Created by davide-maestroni on 08/03/2018.
 */
class LocalActor implements Actor {

  private static final QuotaNotifier DUMMY_NOTIFIER = new QuotaNotifier() {

    public void consume() {
    }

    public boolean exceedsQuota(final int size) {
      return false;
    }

    public void quotaExceeded(final Object message, @NotNull final Envelop envelop) {
    }

    public void quotaExceeded(@NotNull final Iterable<?> messages, @NotNull final Envelop envelop) {
    }
  };

  private final LocalContext mContext;
  private final String mId;
  private final Logger mLogger;
  private final int mQuota;
  private final QuotaNotifier mQuotaNotifier;

  LocalActor(@NotNull final String id, final int quota, @NotNull final LocalContext context) {
    mId = ConstantConditions.notNull("id", id);
    mContext = ConstantConditions.notNull("context", context);
    mQuotaNotifier = ((mQuota = ConstantConditions.positive("quota", quota)) < Integer.MAX_VALUE)
        ? new DefaultQuotaNotifier() : DUMMY_NOTIFIER;
    mLogger = mContext.getLogger();
  }

  @NotNull
  public Actor addObserver(@NotNull final Actor observer) {
    mLogger.dbg("[%s] adding observer: %s", this, observer);
    mContext.getActorExecutor().executeNext(new Runnable() {

      public void run() {
        mContext.addObserver(observer);
      }
    });
    return this;
  }

  public void dismiss(final boolean mayInterruptIfRunning) {
    mLogger.dbg("[%s] dismissing: %s", this, mayInterruptIfRunning);
    mContext.dismiss(mayInterruptIfRunning);
  }

  @NotNull
  public String getId() {
    return mId;
  }

  @NotNull
  public Actor removeObserver(@NotNull final Actor observer) {
    mLogger.dbg("[%s] removing observer: %s", this, observer);
    mContext.getActorExecutor().executeNext(new Runnable() {

      public void run() {
        mContext.removeObserver(observer);
      }
    });
    return this;
  }

  @NotNull
  public Actor tell(final Object message, @Nullable final Options options,
      @NotNull final Actor sender) {
    mLogger.dbg("[%s] sending: %s - options: %s - sender: %s", this, message, options, sender);
    final QuotaNotifier quotaNotifier = mQuotaNotifier;
    if (quotaNotifier.exceedsQuota(1)) {
      mLogger.wrn("[%s] quota exceeded: %s - options: %s - sender: %s", this, message, options,
          sender);
      quotaNotifier.quotaExceeded(message, new BounceEnvelop(sender, options));

    } else {
      try {
        mContext.getActorExecutor().execute(new DefaultEnvelop(sender, options) {

          void open() {
            mQuotaNotifier.consume();
            mContext.message(message, this);
          }
        });
        mLogger.dbg("[%s] sent: %s - options: %s - sender: %s", this, message, options, sender);

      } catch (final RejectedExecutionException e) {
        mLogger.wrn(e, "[%s] failed to send: %s - options: %s - sender: %s", this, message, options,
            sender);
        quotaNotifier.quotaExceeded(message, new BounceEnvelop(sender, options));
      }
    }
    return this;
  }

  @NotNull
  public Actor tellAll(@NotNull final Iterable<?> messages, @Nullable final Options options,
      @NotNull final Actor sender) {
    mLogger.dbg("[%s] sending all: %s - options: %s - sender: %s", this, messages, options, sender);
    final QuotaNotifier quotaNotifier = mQuotaNotifier;
    if (quotaNotifier.exceedsQuota(Iterables.size(messages))) {
      mLogger.wrn("[%s] quota exceeded all: %s - options: %s - sender: %s", this, messages, options,
          sender);
      quotaNotifier.quotaExceeded(messages, new BounceEnvelop(sender, options));

    } else {
      try {
        mContext.getActorExecutor().execute(new DefaultEnvelop(sender, options) {

          void open() {
            mQuotaNotifier.consume();
            mContext.messages(messages, this);
          }
        });
        mLogger.dbg("[%s] sent all: %s - options: %s - sender: %s", this, messages, options,
            sender);

      } catch (final RejectedExecutionException e) {
        mLogger.wrn(e, "[%s] failed to send all: %s - options: %s - sender: %s", this, messages,
            options, sender);
        quotaNotifier.quotaExceeded(messages, new BounceEnvelop(sender, options));
      }
    }
    return this;
  }

  private interface QuotaNotifier {

    void consume();

    boolean exceedsQuota(int size);

    void quotaExceeded(Object message, @NotNull Envelop envelop);

    void quotaExceeded(@NotNull Iterable<?> messages, @NotNull Envelop envelop);
  }

  private static class BounceEnvelop extends DefaultEnvelop {

    BounceEnvelop(@NotNull final Actor sender, @Nullable final Options options) {
      super(sender, options);
    }

    void open() {
    }
  }

  private class DefaultQuotaNotifier implements QuotaNotifier {

    private AtomicInteger mCount = new AtomicInteger();

    public void quotaExceeded(final Object message, @NotNull final Envelop envelop) {
      final Options options = envelop.getOptions();
      if (options.getReceiptId() != null) {
        mContext.reply(envelop.getSender(), new QuotaExceeded(message, options),
            options.threadOnly());
      }
    }

    public void quotaExceeded(@NotNull final Iterable<?> messages, @NotNull final Envelop envelop) {
      final Options options = envelop.getOptions();
      if (options.getReceiptId() != null) {
        final ArrayList<Object> bounces = new ArrayList<Object>();
        for (final Object message : messages) {
          bounces.add(new QuotaExceeded(message, options));
        }
        mContext.replyAll(envelop.getSender(), bounces, options.threadOnly());
      }
    }

    public void consume() {
      mCount.decrementAndGet();
    }

    public boolean exceedsQuota(final int size) {
      final AtomicInteger count = mCount;
      if (count.addAndGet(size) > mQuota) {
        count.addAndGet(-size);
        return true;
      }
      return false;
    }
  }
}
