/*
 * Copyright 2019 Davide Maestroni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

/**
 * Class implementing a local actor.
 */
class LocalActor implements Actor {

  private static final QuotaHandler DUMMY_HANDLER = new QuotaHandler() {

    public boolean consumeQuota() {
      return true;
    }

    public void releaseQuota() {
    }
  };

  private final LocalAgent mAgent;
  private final String mId;
  private final Logger mLogger;
  private final QuotaHandler mQuotaHandler;

  /**
   * Creates a new actor instance.
   *
   * @param id    the actor ID.
   * @param quota the actor inbox quota.
   * @param agent the agent instance.
   */
  LocalActor(@NotNull final String id, final int quota, @NotNull final LocalAgent agent) {
    mId = ConstantConditions.notNull("id", id);
    mAgent = ConstantConditions.notNull("agent", agent);
    mQuotaHandler = (quota < Integer.MAX_VALUE) ? new DefaultQuotaHandler(
        ConstantConditions.positive("quota", quota)) : DUMMY_HANDLER;
    mLogger = mAgent.getLogger();
  }

  @NotNull
  public Actor addObserver(@NotNull final Actor observer) {
    mLogger.dbg("[%s] adding observer: observer=%s", this, observer);
    mAgent.getActorExecutorService().executeNext(new Runnable() {

      public void run() {
        mAgent.addObserver(observer);
      }
    });
    return this;
  }

  public void dismiss(final boolean mayInterruptIfRunning) {
    mLogger.dbg("[%s] dismissing: mayInterruptIfRunning=%s", this, mayInterruptIfRunning);
    mAgent.dismiss(mayInterruptIfRunning);
  }

  @NotNull
  public String getId() {
    return mId;
  }

  @NotNull
  public Actor removeObserver(@NotNull final Actor observer) {
    mLogger.dbg("[%s] removing observer: observer=%s", this, observer);
    mAgent.getActorExecutorService().executeNext(new Runnable() {

      public void run() {
        mAgent.removeObserver(observer);
      }
    });
    return this;
  }

  @NotNull
  public Actor tell(final Object message, @Nullable final Options options,
      @NotNull final Actor sender) {
    mLogger.dbg("[%s] sending: options=%s - sender=%s - message=%s", this, options, sender,
        message);
    if (mQuotaHandler.consumeQuota()) {
      try {
        mAgent.getActorExecutorService().execute(new DefaultEnvelop(sender, options) {

          void open() {
            mQuotaHandler.releaseQuota();
            mAgent.message(message, this);
          }
        });
        mLogger.dbg("[%s] sent: options=%s - sender=%s - message=%s", this, options, sender,
            message);

      } catch (final RejectedExecutionException e) {
        mLogger.wrn(e, "[%s] failed to send: options=%s - sender=%s - message=%s", this, options,
            sender, message);
        quotaExceeded(message, new BounceEnvelop(sender, options));
      }

    } else {
      mLogger.wrn("[%s] quota exceeded: options=%s - sender=%s - message=%s", this, options, sender,
          message);
      quotaExceeded(message, new BounceEnvelop(sender, options));
    }
    return this;
  }

  @NotNull
  public Actor tellAll(@NotNull final Iterable<?> messages, @Nullable final Options options,
      @NotNull final Actor sender) {
    mLogger.dbg("[%s] sending all: options=%s - sender=%s - message=%s", this, options, sender,
        messages);
    if (mQuotaHandler.consumeQuota()) {
      try {
        mAgent.getActorExecutorService().execute(new DefaultEnvelop(sender, options) {

          void open() {
            mQuotaHandler.releaseQuota();
            mAgent.messages(messages, this);
          }
        });
        mLogger.dbg("[%s] sent all: options=%s - sender=%s - message=%s", this, options, sender,
            messages);

      } catch (final RejectedExecutionException e) {
        mLogger.wrn(e, "[%s] failed to send all: options=%s - sender=%s - message=%s", this,
            options, sender, messages);
        quotaExceeded(messages, new BounceEnvelop(sender, options));
      }

    } else {
      mLogger.wrn("[%s] quota exceeded all: options=%s - sender=%s - message=%s", this, options,
          sender, messages);
      quotaExceeded(messages, new BounceEnvelop(sender, options));
    }
    return this;
  }

  @Override
  public String toString() {
    return super.toString() + ":" + mId;
  }

  private void quotaExceeded(@NotNull final Iterable<?> messages, @NotNull final Envelop envelop) {
    final Options options = envelop.getOptions();
    if (options.getReceiptId() != null) {
      final ArrayList<Object> bounces = new ArrayList<Object>();
      for (final Object message : messages) {
        bounces.add(new QuotaExceeded(message, options));
      }
      mAgent.replyAll(envelop.getSender(), bounces, options.threadOnly());
    }
  }

  private void quotaExceeded(final Object message, @NotNull final Envelop envelop) {
    final Options options = envelop.getOptions();
    if (options.getReceiptId() != null) {
      mAgent.reply(envelop.getSender(), new QuotaExceeded(message, options), options.threadOnly());
    }
  }

  private interface QuotaHandler {

    boolean consumeQuota();

    void releaseQuota();
  }

  private static class BounceEnvelop extends DefaultEnvelop {

    BounceEnvelop(@NotNull final Actor sender, @Nullable final Options options) {
      super(sender, options);
    }

    void open() {
    }
  }

  private static class DefaultQuotaHandler implements QuotaHandler {

    private final AtomicInteger mCount = new AtomicInteger();
    private final int mQuota;

    private DefaultQuotaHandler(final int quota) {
      mQuota = quota;
    }

    public boolean consumeQuota() {
      final AtomicInteger count = mCount;
      if (count.incrementAndGet() > mQuota) {
        count.decrementAndGet();
        return false;
      }
      return true;
    }

    public void releaseQuota() {
      mCount.decrementAndGet();
    }
  }
}
