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

import dm.shakespeare.actor.Options;
import dm.shakespeare.config.BuildConfig;
import dm.shakespeare.util.ConstantConditions;

/**
 * {@code Receipt} implementation notifying that the sent message has been rejected after a failure.
 */
public class Failure extends Bounce {

  private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

  private final Throwable mCause;

  /**
   * Creates a new failure message.
   *
   * @param message the bounced message.
   * @param options the original message delivery options.
   * @param cause   the cause of the failure.
   */
  public Failure(final Object message, @NotNull final Options options,
      @NotNull final Throwable cause) {
    super(message, options);
    mCause = ConstantConditions.notNull("cause", cause);
  }

  /**
   * Returns the cause of the failure.
   *
   * @return the throwable describing the failure reason.
   */
  @NotNull
  public final Throwable getCause() {
    return mCause;
  }
}
