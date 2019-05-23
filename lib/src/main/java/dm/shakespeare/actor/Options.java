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
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

import dm.shakespeare.config.BuildConfig;

/**
 * Object containing the message delivery options.<br>
 * The options include a time offset (used to modify the send time), a thread ID (useful to
 * identify messages belonging to the same thread) and a receipt ID (indicating that the sender
 * wants to be notified of the message delivery).<br>
 * The instances of this class are immutable and, inherently, thread safe.
 */
public class Options implements Serializable {

  /**
   * Empty options instance.
   */
  public static final Options EMPTY = new Options();

  private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

  private final String receiptId;
  private final String threadId;
  private final long timeOffset;

  /**
   * Creates a new options instance with an empty configuration.
   */
  public Options() {
    threadId = null;
    receiptId = null;
    timeOffset = 0;
  }

  private Options(@Nullable final String threadId, final String receiptId, final long timeOffset) {
    this.threadId = threadId;
    this.receiptId = receiptId;
    this.timeOffset = timeOffset;
  }

  /**
   * Creates a new options instance with a modified time offset so that the message will result as
   * sent as the specified Epoch time in milliseconds.<br>
   * All the other configurations will be retained.
   *
   * @param timeMillis the timestamp in number of milliseconds.
   * @return the new options instance.
   */
  @NotNull
  public Options asSentAt(final long timeMillis) {
    return withTimeOffset(System.currentTimeMillis() - timeMillis + timeOffset);
  }

  /**
   * Returns the configured receipt ID.
   *
   * @return the receipt ID or {@code null}.
   */
  @Nullable
  public String getReceiptId() {
    return receiptId;
  }

  /**
   * Returns the configured thread ID.
   *
   * @return the thread ID or {@code null}.
   */
  @Nullable
  public String getThreadId() {
    return threadId;
  }

  /**
   * Returns the configured time offset.
   *
   * @return the time offset in number of milliseconds.
   */
  public long getTimeOffset() {
    return timeOffset;
  }

  /**
   * Creates a new options instance retaining only the configured thread ID.
   *
   * @return the new options instance.
   */
  @NotNull
  public Options threadOnly() {
    return new Options(threadId, null, 0);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return "Options{" + "receiptId='" + receiptId + '\'' + ", threadId='" + threadId + '\''
        + ", timeOffset=" + timeOffset + '}';
  }

  /**
   * Creates a new options instance configured with the specified receipt ID.<br>
   * All the other configurations will be retained.
   *
   * @param receiptId the new receipt ID.
   * @return the new options instance.
   */
  @NotNull
  public Options withReceiptId(@Nullable final String receiptId) {
    return new Options(threadId, receiptId, timeOffset);
  }

  /**
   * Creates a new options instance configured with the specified thread ID.<br>
   * All the other configurations will be retained.
   *
   * @param threadId the new thread ID.
   * @return the new options instance.
   */
  @NotNull
  public Options withThreadId(@Nullable final String threadId) {
    return new Options(threadId, receiptId, timeOffset);
  }

  /**
   * Creates a new options instance configured with the specified time offset.<br>
   * All the other configurations will be retained.
   *
   * @param offsetMillis the time offset in number of milliseconds.
   * @return the new options instance.
   */
  @NotNull
  public Options withTimeOffset(final long offsetMillis) {
    return new Options(threadId, receiptId, offsetMillis);
  }
}
