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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dm.shakespeare.AbstractStage;
import dm.shakespeare.BackStage;
import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.Options;
import dm.shakespeare.actor.Role;
import dm.shakespeare.log.LogMessage;
import dm.shakespeare.log.LogPrinters;
import dm.shakespeare.log.Logger;
import dm.shakespeare.remote.Connector.Receiver;
import dm.shakespeare.remote.Connector.Sender;
import dm.shakespeare.remote.config.RemoteClientConfig;
import dm.shakespeare.remote.protocol.ActorRef;
import dm.shakespeare.remote.protocol.CreateActorRequest;
import dm.shakespeare.remote.protocol.CreateActorResponse;
import dm.shakespeare.remote.protocol.DescribeRequest;
import dm.shakespeare.remote.protocol.DescribeResponse;
import dm.shakespeare.remote.protocol.Remote;
import dm.shakespeare.remote.protocol.RemoteBounce;
import dm.shakespeare.remote.protocol.RemoteMessage;

/**
 * Created by davide-maestroni on 04/18/2019.
 */
public class RemoteClient extends AbstractStage {

  private static final Object sMutex = new Object();

  private static Map<String, File> sResources = Collections.emptyMap();

  private final RemoteClientConfig mConfig;
  private final Connector mConnector;
  private final Logger mLogger;
  private final Object mMutex = new Object();
  private final Serializer mSerializer;

  private Actor mActor;
  private Options mOptions;
  private Sender mSender;

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

  public void start() {
    synchronized (mMutex) {
      if (mActor != null) {
        return;
      }
      mActor = BackStage.newActor(new Role() {

        @NotNull
        @Override
        public Behavior getBehavior(@NotNull final String id) {
          return new Behavior() {

            public void onMessage(final Object message, @NotNull final Envelop envelop,
                @NotNull final Agent agent) {
              if (message instanceof DescribeResponse) {
                final DescribeResponse response = (DescribeResponse) message;
                final List<ActorRef> actors = response.getActors();
                // TODO: 19/04/2019 create actors
                // TODO: 19/04/2019 sync...

              } else if (message instanceof CreateActorRequest) {
                safeSend((CreateActorRequest) message, "TODO");

              } else if (message instanceof CreateActorResponse) {

              } else if (message instanceof RemoteMessage) {

              } else if (message instanceof RemoteBounce) {

              }
            }

            public void onStart(@NotNull final Agent agent) {
              try {
                registerFiles();

              } catch (final IOException e) {
                mLogger.err(e, "failed to scan jar files");
              }
              mSender = mConnector.connect(new Receiver() {

                public void receive(@NotNull final Remote remote) {
                  mActor.tell(remote, null, BackStage.STAND_IN);
                }
              });
              safeSend(new DescribeRequest(), "TODO");
            }

            public void onStop(@NotNull final Agent agent) {
              agent.getExecutorService().shutdown();
              mSender.disconnect();
            }
          };
        }

        @NotNull
        @Override
        public ExecutorService getExecutorService(@NotNull final String id) {
          return Executors.newSingleThreadExecutor();
        }
      }).tell(null, null, BackStage.STAND_IN);
    }
  }

  public void stop() {
    synchronized (mMutex) {
      if (mActor == null) {
        throw new IllegalStateException("not started");
      }
    }
    mActor.dismiss(false);
  }

  @NotNull
  protected Actor createActor(@NotNull final String id, @NotNull final Role role) throws Exception {
    return null;
  }

  private void safeSend(@NotNull final Remote remote, @NotNull final String recipientId) {
    try {
      mSender.send(remote, recipientId);

    } catch (final Throwable t) {
      mLogger.err(t, "failed to send remote message: %s to %s",
          LogMessage.abbreviate(remote.toString(), 2048), recipientId);
    }
  }
}
