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
 * {@link RoundRobinRole} unit tests.
 */
public class RoundRobinRoleTest {

  @Test
  public void roundRobin() {
    final TestExecutorService executorService = new TestExecutorService();
    final Actor roundRobin = Stage.newActor(new LocalRoundRobin());
    roundRobin.tell("test0", Headers.EMPTY, Stage.STAND_IN);
    final TestRole testRole1 = new TestRole(executorService);
    roundRobin.tell(ProxySignal.ADD_PROXIED, Headers.EMPTY, Stage.newActor(testRole1));
    final TestRole testRole2 = new TestRole(executorService);
    roundRobin.tell(ProxySignal.ADD_PROXIED, Headers.EMPTY, Stage.newActor(testRole2));
    roundRobin.tell("test1", Headers.EMPTY, Stage.STAND_IN);
    roundRobin.tell("test2", Headers.EMPTY, Stage.newActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return newBehavior().build();
      }
    }));
    executorService.consumeAll();
    assertThat(testRole1.getMessages()).containsExactly("test1");
    assertThat(testRole2.getMessages()).containsExactly("test2");
  }

  @Test
  public void roundRobinReceipt() {
    final TestExecutorService executorService = new TestExecutorService();
    final Actor roundRobin = Stage.newActor(new LocalRoundRobin());
    final TestRole testRole1 = new TestRole(executorService);
    roundRobin.tell(ProxySignal.ADD_PROXIED, Headers.EMPTY, Stage.newActor(testRole1));
    final TestRole testRole2 = new TestRole(executorService);
    roundRobin.tell("test", Headers.EMPTY.withReceiptId("test"), Stage.newActor(testRole2));
    executorService.consumeAll();
    assertThat(testRole1.getMessages()).containsExactly("test");
    assertThat(testRole2.getMessages()).hasSize(1);
    assertThat(testRole2.getMessages().get(0)).isExactlyInstanceOf(Delivery.class);
  }

  @Test
  public void roundRobinRemove() {
    final TestExecutorService executorService = new TestExecutorService();
    final Actor roundRobin = Stage.newActor(new LocalRoundRobin());
    final TestRole testRole1 = new TestRole(executorService);
    roundRobin.tell(ProxySignal.ADD_PROXIED, Headers.EMPTY, Stage.newActor(testRole1));
    roundRobin.tell("test1", Headers.EMPTY, Stage.STAND_IN);
    final TestRole testRole2 = new TestRole(executorService);
    final Actor actor = Stage.newActor(testRole2);
    roundRobin.tell(ProxySignal.ADD_PROXIED, Headers.EMPTY, actor);
    roundRobin.tell(ProxySignal.REMOVE_PROXIED, Headers.EMPTY, actor);
    roundRobin.tell("test2", Headers.EMPTY, Stage.STAND_IN);
    executorService.consumeAll();
    assertThat(testRole1.getMessages()).containsExactly("test1", "test2");
    assertThat(testRole2.getMessages()).isEmpty();
  }

  @Test
  public void roundRobinSender() {
    final TestExecutorService executorService = new TestExecutorService();
    final Actor roundRobin = Stage.newActor(new LocalRoundRobin());
    roundRobin.tell("test0", Headers.EMPTY, Stage.STAND_IN);
    final TestRole testRole1 = new TestRole(executorService);
    roundRobin.tell(ProxySignal.ADD_PROXIED, Headers.EMPTY, Stage.newActor(testRole1));
    roundRobin.tell("test1", Headers.EMPTY, Stage.STAND_IN);
    final TestRole testRole2 = new TestRole(executorService);
    roundRobin.tell(ProxySignal.ADD_PROXIED, Headers.EMPTY, Stage.newActor(testRole2));
    roundRobin.tell("test2", Headers.EMPTY, Stage.STAND_IN);
    executorService.consumeAll();
    assertThat(testRole1.getMessages()).containsExactly("test1", "test2");
    assertThat(testRole2.getMessages()).isEmpty();
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

  private static class LocalRoundRobin extends RoundRobinRole {

    @NotNull
    @Override
    public ExecutorService getExecutorService(@NotNull final String id) {
      return ExecutorServices.localExecutor();
    }
  }
}
