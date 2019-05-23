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

package dm.shakespeare.message;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

import dm.shakespeare.actor.Options;
import dm.shakespeare.config.BuildConfig;
import dm.shakespeare.util.ConstantConditions;

/**
 * Base receipt message sent in response to a received message when the delivery options contains
 * a receipt ID.
 *
 * @see Options
 */
public class Receipt implements Serializable {

  private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

  private final Object message;
  private final Options options;

  /**
   * Creates a new receipt message.
   *
   * @param message the received message.
   * @param options the original message delivery options.
   */
  public Receipt(final Object message, @NotNull final Options options) {
    this.message = message;
    this.options = ConstantConditions.notNull("options", options);
  }

  /**
   * Verifies that a message is a receipt with the specified ID.
   *
   * @param message   the message to check.
   * @param receiptId the receipt ID.
   * @return {@code true} if the message is a receipt and it's ID is equal to the specified one.
   */
  public static boolean isReceipt(@Nullable final Object message, @NotNull final String receiptId) {
    return (message instanceof Receipt) && receiptId.equals(
        ((Receipt) message).getOptions().getReceiptId());
  }

  /**
   * Returns the original message.
   *
   * @return the message object.
   */
  public final Object getMessage() {
    return message;
  }

  /**
   * Returns the original message delivery options.
   *
   * @return the delivery options.
   */
  @NotNull
  public final Options getOptions() {
    return options;
  }
}
