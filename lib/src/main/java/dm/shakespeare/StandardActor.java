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

import java.util.ArrayList;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Headers;
import dm.shakespeare.log.Logger;
import dm.shakespeare.message.Bounce;
import dm.shakespeare.message.QuotaExceeded;
import dm.shakespeare.util.ConstantConditions;

/**
 * Class implementing an actor.
 */
class StandardActor implements Actor {

  private static final QuotaHandler DUMMY_HANDLER = new QuotaHandler() {

    public boolean consumeQuota() {
      return true;
    }

    public void releaseQuota() {
    }
  };

  private final StandardAgent agent;
  private final String id;
  private final Logger logger;
  private final QuotaHandler quotaHandler;

  /**
   * Creates a new actor instance.
   *
   * @param id    the actor ID.
   * @param quota the actor inbox quota.
   * @param agent the agent instance.
   */
  StandardActor(@NotNull final String id, final int quota, @NotNull final StandardAgent agent) {
    this.id = ConstantConditions.notNull("id", id);
    this.agent = ConstantConditions.notNull("agent", agent);
    quotaHandler = (quota < Integer.MAX_VALUE) ? new DefaultQuotaHandler(
        ConstantConditions.positive("quota", quota)) : DUMMY_HANDLER;
    logger = agent.getLogger();
  }

  public boolean addObserver(@NotNull final Actor observer) {
    logger.dbg("[%s] adding observer: observer=%s", this, observer);
    try {
      agent.getActorExecutorService().execute(new Runnable() {

        public void run() {
          agent.addObserver(observer);
        }
      });
      return true;

    } catch (final RejectedExecutionException e) {
      logger.wrn(e, "[%s] failed to add observer: observer=%s", this, observer);
    }
    return false;
  }

  public boolean dismiss() {
    logger.dbg("[%s] dismissing", this);
    try {
      agent.dismiss();
      return true;

    } catch (final RejectedExecutionException e) {
      logger.wrn(e, "[%s] failed to dismiss actor", this);
    }
    return false;
  }

  public boolean dismissLazy() {
    logger.dbg("[%s] dismissing lazily", this);
    try {
      agent.dismissLazy();
      return true;

    } catch (final RejectedExecutionException e) {
      logger.wrn(e, "[%s] failed to dismiss actor", this);
    }
    return false;
  }

  public boolean dismissNow() {
    logger.dbg("[%s] dismissing immediately", this);
    try {
      agent.dismissNow();
      return true;

    } catch (final RejectedExecutionException e) {
      logger.wrn(e, "[%s] failed to dismiss actor", this);
    }
    return false;
  }

  @NotNull
  public String getId() {
    return id;
  }

  public boolean removeObserver(@NotNull final Actor observer) {
    logger.dbg("[%s] removing observer: observer=%s", this, observer);
    try {
      agent.getActorExecutorService().execute(new Runnable() {

        public void run() {
          agent.removeObserver(observer);
        }
      });
      return true;

    } catch (final RejectedExecutionException e) {
      logger.wrn(e, "[%s] failed to remove observer: observer=%s", this, observer);
    }
    return false;
  }

  public void tell(final Object message, @NotNull final Headers headers,
      @NotNull final Actor sender) {
    logger.dbg("[%s] sending: headers=%s - sender=%s - message=%s", this, headers, sender, message);
    if (quotaHandler.consumeQuota()) {
      try {
        agent.getActorExecutorService().execute(new StandardEnvelop(sender, headers) {

          void open() {
            quotaHandler.releaseQuota();
            agent.message(message, this);
          }
        });
        logger.dbg("[%s] sent: headers=%s - sender=%s - message=%s", this, headers, sender,
            message);

      } catch (final RejectedExecutionException e) {
        logger.wrn(e, "[%s] failed to send: headers=%s - sender=%s - message=%s", this, headers,
            sender, message);
        rejected(message, headers, sender);
      }

    } else {
      logger.wrn("[%s] quota exceeded: headers=%s - sender=%s - message=%s", this, headers, sender,
          message);
      quotaExceeded(message, headers, sender);
    }
  }

  public void tellAll(@NotNull final Iterable<?> messages, @NotNull final Headers headers,
      @NotNull final Actor sender) {
    logger.dbg("[%s] sending all: headers=%s - sender=%s - message=%s", this, headers, sender,
        messages);
    if (quotaHandler.consumeQuota()) {
      try {
        agent.getActorExecutorService().execute(new StandardEnvelop(sender, headers) {

          void open() {
            quotaHandler.releaseQuota();
            agent.messages(messages, this);
          }
        });
        logger.dbg("[%s] sent all: headers=%s - sender=%s - message=%s", this, headers, sender,
            messages);

      } catch (final RejectedExecutionException e) {
        logger.wrn(e, "[%s] failed to send all: headers=%s - sender=%s - message=%s", this, headers,
            sender, messages);
        rejected(messages, headers, sender);
      }

    } else {
      logger.wrn("[%s] quota exceeded all: headers=%s - sender=%s - message=%s", this, headers,
          sender, messages);
      quotaExceeded(messages, headers, sender);
    }
  }

  @Override
  public String toString() {
    return super.toString() + ":" + id;
  }

  private void quotaExceeded(@NotNull final Iterable<?> messages, @NotNull final Headers headers,
      @NotNull final Actor sender) {
    if (headers.getReceiptId() != null) {
      final ArrayList<Object> bounces = new ArrayList<Object>();
      for (final Object message : messages) {
        bounces.add(new QuotaExceeded(message, headers));
      }
      sender.tellAll(bounces, headers.threadOnly(), this);
    }
  }

  private void quotaExceeded(final Object message, @NotNull final Headers headers,
      @NotNull final Actor sender) {
    if (headers.getReceiptId() != null) {
      sender.tell(new QuotaExceeded(message, headers), headers.threadOnly(), this);
    }
  }

  private void rejected(@NotNull final Iterable<?> messages, @NotNull final Headers headers,
      @NotNull final Actor sender) {
    if (headers.getReceiptId() != null) {
      final ArrayList<Object> bounces = new ArrayList<Object>();
      for (final Object message : messages) {
        bounces.add(new Bounce(message, headers));
      }
      sender.tellAll(bounces, headers.threadOnly(), this);
    }
  }

  private void rejected(final Object message, @NotNull final Headers headers,
      @NotNull final Actor sender) {
    if (headers.getReceiptId() != null) {
      sender.tell(new Bounce(message, headers), headers.threadOnly(), this);
    }
  }

  private interface QuotaHandler {

    boolean consumeQuota();

    void releaseQuota();
  }

  private static class DefaultQuotaHandler implements QuotaHandler {

    private final AtomicInteger count = new AtomicInteger();
    private final int quota;

    private DefaultQuotaHandler(final int quota) {
      this.quota = quota;
    }

    public boolean consumeQuota() {
      final AtomicInteger count = this.count;
      if (count.incrementAndGet() > quota) {
        count.decrementAndGet();
        return false;
      }
      return true;
    }

    public void releaseQuota() {
      count.decrementAndGet();
    }
  }
}
