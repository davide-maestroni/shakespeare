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

package dm.shakespeare.actor;

import org.jetbrains.annotations.NotNull;

/**
 * Interface defining a message envelop.<br>
 * Sent message objects might be the very same instance, but the relative envelop will be unique
 * for each one.
 */
public interface Envelop {

  /**
   * Returns the delivery options.
   *
   * @return the options instance.
   */
  @NotNull
  Options getOptions();

  /**
   * Returns the Epoch time in milliseconds when the message has been received.
   *
   * @return the timestamp in number of milliseconds.
   */
  long getReceivedAt();

  /**
   * Returns the sender actor.
   *
   * @return the actor who sent the message.
   */
  @NotNull
  Actor getSender();

  /**
   * Returns the Epoch time in milliseconds when the message was sent.<br>
   * The returned value might be corrected by the offset specified in the delivery options.
   *
   * @return the timestamp in number of milliseconds.
   */
  long getSentAt();

  /**
   * Verifies whether the delivery of the receipt is prevented.
   *
   * @return {@code true} if no receipt will be automatically sent.
   */
  boolean isPreventReceipt();

  /**
   * Prevents the delivery of the message receipt.<br>
   * The result of calling this method cannot be reverted, and calling it more than once will have
   * no effect.
   */
  void preventReceipt();
}
