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

package dm.shakespeare.template.role;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

import dm.shakespeare.Stage;
import dm.shakespeare.actor.AbstractBehavior;
import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.Headers;
import dm.shakespeare.actor.Role;
import dm.shakespeare.concurrent.ExecutorServices;
import dm.shakespeare.message.Delivery;
import dm.shakespeare.template.behavior.AbstractProxyBehavior.ProxySignal;
import dm.shakespeare.test.concurrent.TestExecutorService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ProxyRole} unit tests.
 */
public class ProxyRoleTest {

  @Test
  public void proxy() {
    final TestExecutorService executorService = new TestExecutorService();
    final Actor proxy = Stage.back().createActor(new LocalProxy());
    proxy.tell("test0", Headers.empty(), Stage.standIn());
    final TestRole testRole = new TestRole(executorService);
    proxy.tell(ProxySignal.ADD_PROXIED, Headers.empty(), Stage.back().createActor(testRole));
    proxy.tell("test1", Headers.empty(), Stage.standIn());
    proxy.tell("test2", Headers.empty(), Stage.back().createActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return newBehavior().build();
      }
    }));
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly("test1", "test2");
  }

  @Test
  public void proxyReceipt() {
    final TestExecutorService executorService = new TestExecutorService();
    final Actor proxy = Stage.back().createActor(new LocalProxy());
    final TestRole testRole1 = new TestRole(executorService);
    proxy.tell(ProxySignal.ADD_PROXIED, Headers.empty(), Stage.back().createActor(testRole1));
    final TestRole testRole2 = new TestRole(executorService);
    proxy.tell("test", Headers.empty().withReceiptId("test"), Stage.back().createActor(testRole2));
    executorService.consumeAll();
    assertThat(testRole1.getMessages()).containsExactly("test");
    assertThat(testRole2.getMessages()).hasSize(1);
    assertThat(testRole2.getMessages().get(0)).isExactlyInstanceOf(Delivery.class);
  }

  @Test
  public void proxyRemove() {
    final TestExecutorService executorService = new TestExecutorService();
    final Actor proxy = Stage.back().createActor(new LocalProxy());
    final TestRole testRole1 = new TestRole(executorService);
    proxy.tell(ProxySignal.ADD_PROXIED, Headers.empty(), Stage.back().createActor(testRole1));
    proxy.tell("test1", Headers.empty(), Stage.standIn());
    final TestRole testRole2 = new TestRole(executorService);
    final Actor actor = Stage.back().createActor(testRole2);
    proxy.tell(ProxySignal.ADD_PROXIED, Headers.empty(), actor);
    proxy.tell(ProxySignal.REMOVE_PROXIED, Headers.empty(), actor);
    proxy.tell("test2", Headers.empty(), Stage.standIn());
    executorService.consumeAll();
    assertThat(testRole1.getMessages()).containsExactly("test1");
    assertThat(testRole2.getMessages()).isEmpty();
  }

  @Test
  public void proxySender() {
    final TestExecutorService executorService = new TestExecutorService();
    final Actor proxy = Stage.back().createActor(new LocalProxy());
    proxy.tell("test0", Headers.empty(), Stage.standIn());
    final TestRole testRole1 = new TestRole(executorService);
    proxy.tell(ProxySignal.ADD_PROXIED, Headers.empty(), Stage.back().createActor(testRole1));
    proxy.tell("test1", Headers.empty(), Stage.standIn());
    final TestRole testRole2 = new TestRole(executorService);
    proxy.tell(ProxySignal.ADD_PROXIED, Headers.empty(), Stage.back().createActor(testRole2));
    proxy.tell("test2", Headers.empty(), Stage.standIn());
    executorService.consumeAll();
    assertThat(testRole1.getMessages()).containsExactly("test1");
    assertThat(testRole2.getMessages()).containsExactly("test2");
  }

  @SuppressWarnings("unused")
  public static class TestRole extends Role {

    private final TestExecutorService executorService;
    private final ArrayList<Headers> headers = new ArrayList<Headers>();
    private final ArrayList<Object> messages = new ArrayList<Object>();
    private final ArrayList<Actor> senders = new ArrayList<Actor>();

    TestRole(@NotNull final TestExecutorService executorService) {
      this.executorService = executorService;
    }

    @NotNull
    public Behavior getBehavior(@NotNull final String id) {
      return new AbstractBehavior() {

        public void onMessage(final Object message, @NotNull final Envelop envelop,
            @NotNull final Agent agent) {
          messages.add(message);
          headers.add(envelop.getHeaders());
          senders.add(envelop.getSender());
        }
      };
    }

    @NotNull
    @Override
    public ExecutorService getExecutorService(@NotNull final String id) {
      return executorService;
    }

    @NotNull
    ArrayList<Headers> getHeaders() {
      return headers;
    }

    @NotNull
    ArrayList<Object> getMessages() {
      return messages;
    }

    @NotNull
    ArrayList<Actor> getSenders() {
      return senders;
    }
  }

  private static class LocalProxy extends ProxyRole {

    @NotNull
    @Override
    public ExecutorService getExecutorService(@NotNull final String id) {
      return ExecutorServices.localExecutor();
    }
  }
}
