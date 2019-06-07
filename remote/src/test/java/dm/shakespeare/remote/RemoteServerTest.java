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
import org.junit.Test;

import java.io.File;
import java.io.FilePermission;
import java.nio.ByteBuffer;
import java.security.CodeSource;
import java.security.Permission;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import dm.shakespeare.Stage;
import dm.shakespeare.actor.AbstractBehavior;
import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.SerializableRole;
import dm.shakespeare.concurrent.ExecutorServices;
import dm.shakespeare.remote.config.LocalConfig;
import dm.shakespeare.remote.config.RemoteConfig;
import dm.shakespeare.remote.transport.Connector;
import dm.shakespeare.remote.transport.RemoteRequest;
import dm.shakespeare.remote.transport.RemoteResponse;
import dm.shakespeare.remote.util.Classes;
import dm.shakespeare.remote.util.SerializableData;

/**
 * Created by davide-maestroni on 04/18/2019.
 */
public class RemoteServerTest {

  @Test
  public void localTest() throws Exception {
    final ExecutorService executorService = ExecutorServices.localExecutor();
    final DirectConnector connector = new DirectConnector();
    final StageReceiver stageReceiver = new StageReceiver(
        new RemoteConfig().withConnector(connector)
            .withRemoteCreateEnable(true)
            .withSerializerWhitelist("java.lang.**,dm.shakespeare.**")
            .withExecutor(executorService), new Stage());
    final StageRef stage = new StageRef(new LocalConfig().withRemoteId("id")
        .withConnector(connector.localConnector(stageReceiver.connect()))
        .withExecutor(executorService));
    stage.connect();
    final Actor printActor = Stage.newActor(new PrintRole());
    final Actor actor = stage.createActor(new UpperRole());
    actor.tell("hello remote!", null, printActor);
  }

  @Test
  public void tes() throws Exception {
    {
      final byte[] bytes = SerializableData.wrapNoCache(
          RemoteServerTest.class.getResourceAsStream("/TestRole.class")).toByteArray();
      final Set<String> dependencies = Classes.getDependencies(ByteBuffer.wrap(bytes));
      System.out.println(dependencies);
    }
    {
      final byte[] bytes = SerializableData.wrapNoCache(
          RemoteServerTest.class.getResourceAsStream("/TestRole$1.class")).toByteArray();
      final Set<String> dependencies = Classes.getDependencies(ByteBuffer.wrap(bytes));
      System.out.println(dependencies);
    }
  }

  @Test
  public void test() throws Exception {
    final File container = new File(new File(System.getProperty("java.io.tmpdir")), "shakespeare");
    System.setSecurityManager(new SecurityManager() {

      @Override
      public void checkPermission(final Permission permission) {
        permission.getActions();
        //        if (permission.getName().equals(container.getPath()) && permission.getActions()
        //            .equals("read")) {
        //          super.checkPermission(permission);
        //        }
      }

      @Override
      public void checkPermission(final Permission permission, final Object o) {
        permission.getActions();
      }
    });
    final CodeSource codeSource =
        new CodeSource(new File(container, "-").toURI().toURL(), (Certificate[]) null);
    final Permissions permissions = new Permissions();
    permissions.add(new FilePermission(container.getPath(), "read,write,execute,delete"));
    permissions.add(
        new FilePermission(new File(container, "-").getPath(), "read,write,execute,delete"));
    final ProtectionDomain protectionDomain = new ProtectionDomain(codeSource, permissions);
    Thread.sleep(2000);
    final JavaSerializer javaSerializer = new JavaSerializer();
    //    connector.send(new CreateActorRequest().putResource("/dm/shakespeare/sample/TestRole
    //    .class",
    //        SerializableData.wrap(RemoteServerTest.class.getResourceAsStream("/TestRole.class")))
    //        .putResource("/dm/shakespeare/sample/TestRole$1.class",
    //            SerializableData.wrap(RemoteServerTest.class.getResourceAsStream("/TestRole$1
    //            .class")))
    //        .withRoleData(
    //            SerializableData.wrap(RemoteServerTest.class.getResourceAsStream("/TestRole
    //            .ser")))
    //        .withRecipientRef(actorRef)
    //        .withSenderId("senderId"));
    Thread.sleep(2000);
  }

  private static class DirectConnector implements Connector {

    private final Map<String, Receiver> receivers =
        Collections.synchronizedMap(new HashMap<String, Receiver>());

    @NotNull
    public Sender connect(@NotNull final Receiver receiver) {
      return new Sender() {

        public void disconnect() {
        }

        @NotNull
        public RemoteResponse send(@NotNull final RemoteRequest request,
            @NotNull final String receiverId) throws Exception {
          return receivers.get(receiverId).receive(request);
        }
      };
    }

    @NotNull
    public Connector localConnector(@NotNull final Receiver localReceiver) {
      final String id = UUID.randomUUID().toString();
      return new Connector() {

        @NotNull
        public Sender connect(@NotNull final Receiver receiver) {
          receivers.put(id, receiver);
          return new Sender() {

            public void disconnect() {
            }

            @NotNull
            public RemoteResponse send(@NotNull final RemoteRequest request,
                @NotNull final String receiverId) throws Exception {
              return localReceiver.receive(request.withSenderId(id));
            }
          };
        }
      };
    }
  }

  private static class PrintRole extends SerializableRole {

    @NotNull
    @Override
    public ExecutorService getExecutorService(@NotNull final String id) {
      return ExecutorServices.localExecutor();
    }

    @NotNull
    @Override
    protected Behavior getSerializableBehavior(@NotNull final String id) {
      return new AbstractBehavior() {

        public void onMessage(final Object message, @NotNull final Envelop envelop,
            @NotNull final Agent agent) {
          System.out.println("message: " + message);
        }
      };
    }
  }

  private static class UpperRole extends SerializableRole {

    @NotNull
    @Override
    public ExecutorService getExecutorService(@NotNull final String id) {
      return ExecutorServices.localExecutor();
    }

    @NotNull
    @Override
    protected Behavior getSerializableBehavior(@NotNull final String id) {
      return new AbstractBehavior() {

        public void onMessage(final Object message, @NotNull final Envelop envelop,
            @NotNull final Agent agent) {
          envelop.getSender()
              .tell(("" + message).toUpperCase(Locale.ENGLISH), null, agent.getSelf());
        }
      };
    }
  }
}
