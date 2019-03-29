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

/**
 * {@link Receipt} implementation notifying that the sent message has been successfully delivered.
 */
public class Delivery extends Receipt {

  private static final long serialVersionUID = BuildConfig.VERSION_HASH_CODE;

  /**
   * Creates a new delivery message.
   *
   * @param message the delivered message.
   * @param options the original message delivery options.
   */
  public Delivery(final Object message, @NotNull final Options options) {
    super(message, options);
  }
}
