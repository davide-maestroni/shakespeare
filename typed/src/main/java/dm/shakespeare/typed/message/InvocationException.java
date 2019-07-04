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

package dm.shakespeare.typed.message;

import dm.shakespeare.typed.config.BuildConfig;

/**
 * Message indicating that the invocation failed with an exception.
 */
public class InvocationException extends InvocationResponse {

  private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

  private final Throwable cause;

  /**
   * Creates an empty message.<br>
   * Usually needed during deserialization.
   */
  public InvocationException() {
    this(null, null);
  }

  /**
   * Creates a new exception message.
   *
   * @param cause the cause of the failure.
   */
  public InvocationException(final String invocationId, final Throwable cause) {
    super(invocationId);
    this.cause = cause;
  }

  /**
   * Returns the cause of the failure.
   *
   * @return the throwable instance.
   */
  public Throwable getCause() {
    return cause;
  }
}
