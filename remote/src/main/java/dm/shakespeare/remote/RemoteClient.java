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

package dm.shakespeare.remote;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import dm.shakespeare.AbstractStage;
import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Role;
import dm.shakespeare.log.LogPrinters;
import dm.shakespeare.log.Logger;
import dm.shakespeare.remote.config.RemoteClientConfig;

/**
 * Created by davide-maestroni on 04/18/2019.
 */
public class RemoteClient extends AbstractStage {

  private static final Object sMutex = new Object();

  private static Map<String, File> sResources = Collections.emptyMap();

  private final RemoteClientConfig mConfig;
  private final Connector mConnector;
  private final Logger mLogger;
  private final Serializer mSerializer;

  public RemoteClient(@NotNull final RemoteClientConfig config) {
    mConfig = config;
    mConnector = config.getConnector();
    final Serializer serializer = config.getSerializer();
    mSerializer = (serializer != null) ? serializer : new JavaSerializer();
    final Logger logger = config.getLogger();
    mLogger = (logger != null) ? logger
        : Logger.newLogger(LogPrinters.javaLoggingPrinter(getClass().getName()));
  }

  private static void registerFile(@NotNull final File root, @NotNull final File file,
      @NotNull final HashMap<String, File> fileMap) {
    if (file.isDirectory()) {
      final File[] files = file.listFiles();
      if (files != null) {
        for (final File child : files) {
          registerFile(root, child, fileMap);
        }
      }
    } else {
      fileMap.put(file.getPath().substring(root.getPath().length() + 1), file);
    }
  }

  private static void registerFiles() throws IOException {
    synchronized (sMutex) {
      if (sResources.isEmpty()) {
        final HashMap<String, File> fileMap = new HashMap<String, File>();
        final Enumeration<URL> resources = RemoteClient.class.getClassLoader().getResources("");
        while (resources.hasMoreElements()) {
          final URL url = resources.nextElement();
          final File root = new File(url.getPath());
          registerFile(root, root, fileMap);
        }
        sResources = fileMap;
      }
    }
  }

  @NotNull
  protected Actor createActor(@NotNull final String id, @NotNull final Role role) throws Exception {
    return null;
  }
}
