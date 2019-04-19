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

package dm.shakespeare.remote.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dm.shakespeare.log.Logger;
import dm.shakespeare.remote.Connector;
import dm.shakespeare.remote.Serializer;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 04/16/2019.
 */
public class RemoteConfig {

  private Connector mConnector;

  public RemoteConfig(@NotNull final Connector connector) {
    mConnector = ConstantConditions.notNull("connector", connector);
  }

  @NotNull
  public final Connector getConnector() {
    return mConnector;
  }

  @Nullable
  public Logger getLogger() {
    return null;
  }

  @Nullable
  public Serializer getSerializer() {
    return null;
  }
}
