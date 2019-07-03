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
 * Object containing the message headers.<br>
 * The headers may include:
 * <ul>
 * <li>a time offset, used to modify the send time</li>
 * <li>a thread ID, useful to identify messages belonging to the same thread</li>
 * <li>a receipt ID, indicating that the sender wants to be notified of the message delivery</li>
 * </ul>
 * The instances of this class are immutable and, inherently, thread safe.
 */
public class Headers implements Serializable {

  private static final Headers EMPTY = new Headers();

  private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

  private final String receiptId;
  private final String threadId;
  private final long timeOffset;

  /**
   * Creates a new headers instance with an empty configuration.
   */
  public Headers() {
    threadId = null;
    receiptId = null;
    timeOffset = 0;
  }

  private Headers(@Nullable final String threadId, final String receiptId, final long timeOffset) {
    this.threadId = threadId;
    this.receiptId = receiptId;
    this.timeOffset = timeOffset;
  }

  /**
   * Returns the default headers instance.
   *
   * @return the headers instance.
   */
  @NotNull
  public static Headers empty() {
    return EMPTY;
  }

  /**
   * Creates a new headers instance with a modified time offset so that the message will result as
   * sent as the specified Epoch time in milliseconds.<br>
   * All the other configurations will be retained.
   *
   * @param timeMillis the timestamp in number of milliseconds.
   * @return the new headers instance.
   */
  @NotNull
  public Headers asSentAt(final long timeMillis) {
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
   * Creates a new headers instance retaining only the configured thread ID.
   *
   * @return the new headers instance.
   */
  @NotNull
  public Headers threadOnly() {
    return new Headers(threadId, null, 0);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return "Headers{" + "receiptId='" + receiptId + '\'' + ", threadId='" + threadId + '\''
        + ", timeOffset=" + timeOffset + '}';
  }

  /**
   * Creates a new headers instance configured with the specified receipt ID.<br>
   * All the other configurations will be retained.
   *
   * @param receiptId the new receipt ID.
   * @return the new headers instance.
   */
  @NotNull
  public Headers withReceiptId(@Nullable final String receiptId) {
    return new Headers(threadId, receiptId, timeOffset);
  }

  /**
   * Creates a new headers instance configured with the specified thread ID.<br>
   * All the other configurations will be retained.
   *
   * @param threadId the new thread ID.
   * @return the new headers instance.
   */
  @NotNull
  public Headers withThreadId(@Nullable final String threadId) {
    return new Headers(threadId, receiptId, timeOffset);
  }

  /**
   * Creates a new headers instance configured with the specified time offset.<br>
   * All the other configurations will be retained.
   *
   * @param offsetMillis the time offset in number of milliseconds.
   * @return the new headers instance.
   */
  @NotNull
  public Headers withTimeOffset(final long offsetMillis) {
    return new Headers(threadId, receiptId, offsetMillis);
  }
}
