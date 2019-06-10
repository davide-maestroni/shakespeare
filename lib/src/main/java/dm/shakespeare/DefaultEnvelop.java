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

import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.Headers;
import dm.shakespeare.actor.Headers;
import dm.shakespeare.util.ConstantConditions;

/**
 * Default implementation of an {@code Envelop}.
 */
abstract class DefaultEnvelop implements Envelop, Runnable {

  private final Headers headers;
  private final Actor sender;
  private final long sentAt;
  private boolean preventReceipt;
  private long receivedAt = -1;

  /**
   * Creates a new envelop.
   *
   * @param sender  the sender actor.
   * @param headers the message headers.
   */
  DefaultEnvelop(@NotNull final Actor sender, @Nullable final Headers headers) {
    this.sender = ConstantConditions.notNull("sender", sender);
    this.headers = (headers != null) ? headers : Headers.EMPTY;
    sentAt = System.currentTimeMillis() - this.headers.getTimeOffset();
  }

  @NotNull
  public Headers getHeaders() {
    return headers;
  }

  public long getReceivedAt() {
    return receivedAt;
  }

  @NotNull
  public Actor getSender() {
    return sender;
  }

  public long getSentAt() {
    return sentAt;
  }

  public boolean isPreventReceipt() {
    return preventReceipt;
  }

  public void preventReceipt() {
    preventReceipt = true;
  }

  public void run() {
    receivedAt = System.currentTimeMillis();
    open();
  }

  @Override
  public String toString() {
    return "DefaultEnvelop{" + "headers=" + headers + ", sender=" + sender + ", sentAt=" + sentAt
        + ", preventReceipt=" + preventReceipt + ", receivedAt=" + receivedAt + '}';
  }

  abstract void open();
}
