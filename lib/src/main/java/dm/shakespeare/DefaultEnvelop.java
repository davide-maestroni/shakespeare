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
import dm.shakespeare.actor.Options;
import dm.shakespeare.util.ConstantConditions;

/**
 * Default implementation of an {@code Envelop}.
 */
abstract class DefaultEnvelop implements Envelop, Runnable {

  private final Options mOptions;
  private final Actor mSender;
  private final long mSentAt;
  private boolean mPreventReceipt;
  private long mReceivedAt = -1;

  /**
   * Creates a new envelop.
   *
   * @param sender  the sender actor.
   * @param options the delivery options.
   */
  DefaultEnvelop(@NotNull final Actor sender, @Nullable final Options options) {
    mSender = ConstantConditions.notNull("sender", sender);
    mOptions = (options != null) ? options : Options.EMPTY;
    mSentAt = System.currentTimeMillis() - mOptions.getTimeOffset();
  }

  @NotNull
  public Options getOptions() {
    return mOptions;
  }

  public long getReceivedAt() {
    return mReceivedAt;
  }

  @NotNull
  public Actor getSender() {
    return mSender;
  }

  public long getSentAt() {
    return mSentAt;
  }

  public boolean isPreventReceipt() {
    return mPreventReceipt;
  }

  public void preventReceipt() {
    mPreventReceipt = true;
  }

  public void run() {
    mReceivedAt = System.currentTimeMillis();
    open();
  }

  @Override
  public String toString() {
    return "DefaultEnvelop{" + "options=" + mOptions + ", sender=" + mSender + ", sentAt=" + mSentAt
        + ", preventReceipt=" + mPreventReceipt + ", receivedAt=" + mReceivedAt + '}';
  }

  abstract void open();
}
