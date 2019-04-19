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

/**
 * Created by davide-maestroni on 04/19/2019.
 */
public class RemoteServerConfig extends RemoteConfig {

  public RemoteServerConfig(@NotNull final Connector connector) {
    super(connector);
  }

  @Nullable
  public Capabilities getCapabilities(@Nullable final String senderId) {
    return null;
  }

  @Nullable
  public Logger getLogger(@Nullable final String senderId) {
    return null;
  }

  @Nullable
  public ProtectionDomain getProtectionDomain(@Nullable final String senderId) {
    return null;
  }

  @Nullable
  public File getResourceContainer(@Nullable final String senderId) {
    final File root = new File(new File(System.getProperty("java.io.tmpdir")), "shakespeare");
    if (root.isDirectory() || root.mkdir()) {
      final File file =
          new File(root, Integer.toHexString((senderId != null) ? senderId.hashCode() : 0));
      if (file.isDirectory() || file.mkdir()) {
        return file;
      }
    }
    return null;
  }
}
