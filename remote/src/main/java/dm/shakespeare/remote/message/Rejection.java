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

package dm.shakespeare.remote.message;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare.actor.Headers;
import dm.shakespeare.message.Bounce;
import dm.shakespeare.remote.config.BuildConfig;

/**
 * Created by davide-maestroni on 06/03/2019.
 */
public class Rejection extends Bounce {

  private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

  public Rejection() {
  }

  public Rejection(final Object message, @NotNull final Headers headers) {
    super(message, headers);
  }
}
