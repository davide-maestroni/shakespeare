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

import java.io.File;
import java.security.ProtectionDomain;

import dm.shakespeare.log.Logger;
import dm.shakespeare.remote.Connector;
import dm.shakespeare.remote.Serializer;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 04/16/2019.
 */
public class RemoteConfig {

  private Connector mConnector;
  private Logger mLogger;
  private ProtectionDomain mProtectionDomain;
  private File mResourceContainer;
  private Serializer mSerializer;

  public RemoteConfig(@NotNull final Connector connector) {
    mConnector = ConstantConditions.notNull("connector", connector);
  }

  @NotNull
  public Connector getConnector() {
    return mConnector;
  }

  @Nullable
  public Logger getLogger() {
    return mLogger;
  }

  @Nullable
  public ProtectionDomain getProtectionDomain() {
    return mProtectionDomain;
  }

  @Nullable
  public File getResourceContainer() {
    return mResourceContainer;
  }

  @Nullable
  public Serializer getSerializer() {
    return mSerializer;
  }

  @NotNull
  public RemoteConfig withLogger(@Nullable final Logger logger) {
    mLogger = logger;
    return this;
  }

  @NotNull
  public RemoteConfig withProtectionDomain(@Nullable final ProtectionDomain protectionDomain) {
    mProtectionDomain = protectionDomain;
    return this;
  }

  @NotNull
  public RemoteConfig withResourceContainer(final File resourceContainer) {
    mResourceContainer = resourceContainer;
    return this;
  }

  @NotNull
  public RemoteConfig withSerializer(@Nullable final Serializer serializer) {
    mSerializer = serializer;
    return this;
  }
}
