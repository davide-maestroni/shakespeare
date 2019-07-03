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
import dm.shakespeare.template.behavior.TopicBehavior.Subscribe;
import dm.shakespeare.template.behavior.TopicBehavior.Unsubscribe;
import dm.shakespeare.test.concurrent.TestExecutorService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link TopicRole} unit tests.
 */
public class TopicRoleTest {

  @Test
  public void subscription() {
    final TestExecutorService executorService = new TestExecutorService();
    final Actor actor = Stage.back().createActor(new LocalTopicRole());
    final TestRole testRole = new TestRole(executorService);
    actor.tell(new Subscribe(), Headers.empty(), Stage.back().createActor(testRole));
    actor.tell("test", Headers.empty(), Stage.standIn());
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly("test");
  }

  @Test
  public void subscriptionFilter() {
    final TestExecutorService executorService = new TestExecutorService();
    final Actor actor = Stage.back().createActor(new LocalTopicRole());
    final TestRole testRole = new TestRole(executorService);
    actor.tell(new Subscribe() {

      @Override
      public boolean accept(final Object message, @NotNull final Envelop envelop) {
        return message instanceof String;
      }
    }, Headers.empty(), Stage.back().createActor(testRole));
    actor.tell(1, Headers.empty(), Stage.standIn());
    actor.tell("test", Headers.empty(), Stage.standIn());
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly("test");
  }

  @Test
  public void unsubscription() {
    final TestExecutorService executorService = new TestExecutorService();
    final Actor actor = Stage.back().createActor(new LocalTopicRole());
    final TestRole testRole = new TestRole(executorService);
    final Actor testActor = Stage.back().createActor(testRole);
    actor.tell(new Subscribe(), Headers.empty(), testActor);
    actor.tell(new Unsubscribe(), Headers.empty(), testActor);
    actor.tell("test", Headers.empty(), Stage.standIn());
    executorService.consumeAll();
    assertThat(testRole.getMessages()).isEmpty();
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

  private static class LocalTopicRole extends TopicRole {

    @NotNull
    @Override
    public ExecutorService getExecutorService(@NotNull final String id) {
      return ExecutorServices.localExecutor();
    }
  }
}
