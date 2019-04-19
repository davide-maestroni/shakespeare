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
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.io.File;
import java.io.FilePermission;
import java.nio.ByteBuffer;
import java.security.CodeSource;
import java.security.Permission;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.Set;

import dm.shakespeare.remote.config.Capabilities;
import dm.shakespeare.remote.config.RemoteConfig;
import dm.shakespeare.remote.protocol.ActorRef;
import dm.shakespeare.remote.protocol.CreateActorRequest;
import dm.shakespeare.remote.protocol.Remote;
import dm.shakespeare.remote.protocol.RemoteMessage;
import dm.shakespeare.remote.util.Classes;
import dm.shakespeare.remote.util.SerializableData;

/**
 * Created by davide-maestroni on 04/18/2019.
 */
public class RemoteServerTest {

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
    final TestConnector connector = new TestConnector();
    final CodeSource codeSource =
        new CodeSource(new File(container, "-").toURI().toURL(), (Certificate[]) null);
    final Permissions permissions = new Permissions();
    permissions.add(new FilePermission(container.getPath(), "read,write,execute,delete"));
    permissions.add(
        new FilePermission(new File(container, "-").getPath(), "read,write,execute,delete"));
    final ProtectionDomain protectionDomain = new ProtectionDomain(codeSource, permissions);
    final Capabilities capabilities = new Capabilities().putValue(Capabilities.CREATE_REMOTE, true)
        .putValue(Capabilities.LOAD_REMOTE, true);
    final RemoteServer remoteServer = new RemoteServer(new RemoteConfig(connector) {

      @Nullable
      @Override
      public Capabilities getCapabilities(@Nullable final String senderId) {
        return capabilities;
      }

      @Nullable
      @Override
      public ProtectionDomain getProtectionDomain(@Nullable final String senderId) {
        return protectionDomain;
      }
    });
    remoteServer.start();
    Thread.sleep(2000);
    final JavaSerializer javaSerializer = new JavaSerializer();
    final ActorRef actorRef = new ActorRef().withId("123").withHash("123");
    connector.send(new CreateActorRequest().putResource("/dm/shakespeare/sample/TestRole.class",
        SerializableData.wrap(RemoteServerTest.class.getResourceAsStream("/TestRole.class")))
        .putResource("/dm/shakespeare/sample/TestRole$1.class",
            SerializableData.wrap(RemoteServerTest.class.getResourceAsStream("/TestRole$1.class")))
        .withRoleData(
            SerializableData.wrap(RemoteServerTest.class.getResourceAsStream("/TestRole.ser")))
        .withRecipientRef(actorRef)
        .withSenderId("senderId"));
    connector.send(
        new RemoteMessage().withMessageData(SerializableData.wrap(javaSerializer.serialize("ciao")))
            .withRecipientRef(actorRef)
            .withSenderRef(actorRef)
            .withSenderId("senderId"));
    Thread.sleep(2000000);
  }

  private static class TestConnector implements Connector {

    private Receiver mReceiver;

    @NotNull
    public Sender connect(@NotNull final Receiver receiver) {
      mReceiver = receiver;
      return new Sender() {

        public void disconnect() {

        }

        public void send(@NotNull final Iterable<? extends Remote> remotes,
            @NotNull final String receiverId) throws Exception {

        }

        public void send(@NotNull final Remote remote, @NotNull final String receiverId) throws
            Exception {
          remote.getSenderId();
        }
      };
    }

    public void send(@NotNull final Remote remote) throws Exception {
      mReceiver.receive(remote);
    }
  }
}
