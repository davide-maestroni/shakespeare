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

package dm.shakespeare;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import dm.shakespeare.actor.AbstractBehavior;
import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.Role;
import dm.shakespeare.test.concurrent.TestExecutorService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link Stage} unit tests.
 */
public class StageTest {

  @Test
  public void createActor() {
    final Stage stage = new Stage();
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = stage.createActor(testRole);
    assertThat(actor).isNotNull();
    assertThat(actor.getId()).isNotNull();
    actor.tell("test", null, Stage.STAND_IN);
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly("test");
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void createActorFailure() {
    new Stage().createActor(new FailureRole(new IndexOutOfBoundsException()));
  }

  @Test
  public void createActorId() {
    final Stage stage = new Stage();
    final String id = UUID.randomUUID().toString();
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = stage.createActor(id, testRole);
    assertThat(actor).isNotNull();
    assertThat(actor.getId()).isEqualTo(id);
    actor.tell("test", null, Stage.STAND_IN);
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly("test");
  }

  @Test(expected = IllegalStateException.class)
  public void createActorSameId() {
    final Stage stage = new Stage();
    final String id = UUID.randomUUID().toString();
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = stage.createActor(id, testRole);
    assertThat(actor).isNotNull();
    assertThat(actor.getId()).isEqualTo(id);
    actor.tell("test", null, Stage.STAND_IN);
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly("test");
    stage.createActor(id, new TestRole(executorService));
  }

  @Test
  public void newActor() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = Stage.newActor(testRole);
    assertThat(actor).isNotNull();
    assertThat(actor.getId()).isNotNull();
    actor.tell("test", null, Stage.STAND_IN);
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly("test");
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void newActorFailure() {
    Stage.newActor(new FailureRole(new IndexOutOfBoundsException()));
  }

  @Test
  public void newActorId() {
    final String id = UUID.randomUUID().toString();
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = Stage.newActor(id, testRole);
    assertThat(actor).isNotNull();
    assertThat(actor.getId()).isEqualTo(id);
    actor.tell("test", null, Stage.STAND_IN);
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly("test");
  }

  @Test
  public void newActorSameId() {
    final String id = UUID.randomUUID().toString();
    final TestExecutorService executorService = new TestExecutorService();
    TestRole testRole = new TestRole(executorService);
    Actor actor = Stage.newActor(id, testRole);
    assertThat(actor).isNotNull();
    assertThat(actor.getId()).isEqualTo(id);
    actor.tell("test", null, Stage.STAND_IN);
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly("test");
    testRole = new TestRole(executorService);
    actor = Stage.newActor(id, testRole);
    assertThat(actor).isNotNull();
    assertThat(actor.getId()).isEqualTo(id);
    actor.tell("test", null, Stage.STAND_IN);
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly("test");
  }

  @Test
  public void observerCreate() {
    final Stage stage = new Stage();
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = stage.createActor(testRole);
    stage.addObserver(actor);
    final Actor createdActor = stage.createActor(new TestRole(executorService));
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly(Stage.CREATE);
    assertThat(testRole.getSenders()).containsExactly(createdActor);
  }

  @Test
  public void observerCreateRemoved() {
    final Stage stage = new Stage();
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = stage.createActor(testRole);
    stage.addObserver(actor);
    stage.removeObserver(actor);
    stage.createActor(new TestRole(executorService));
    executorService.consumeAll();
    assertThat(testRole.getMessages()).isEmpty();
    assertThat(testRole.getSenders()).isEmpty();
  }

  @Test
  public void observerDismiss() {
    final Stage stage = new Stage();
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = stage.createActor(testRole);
    final Actor actorToDismiss = stage.createActor(new TestRole(executorService));
    stage.addObserver(actor);
    actorToDismiss.dismiss();
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly(Stage.DISMISS);
    assertThat(testRole.getSenders()).containsExactly(actorToDismiss);
  }

  @Test
  public void observerDismissRemoved() {
    final Stage stage = new Stage();
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = stage.createActor(testRole);
    final Actor actorToDismiss = stage.createActor(new TestRole(executorService));
    stage.addObserver(actor);
    stage.removeObserver(actor);
    actorToDismiss.dismiss();
    executorService.consumeAll();
    assertThat(testRole.getMessages()).isEmpty();
    assertThat(testRole.getSenders()).isEmpty();
  }

  @Test
  public void recreateActorId() {
    final String id = UUID.randomUUID().toString();
    final Stage stage = new Stage();
    try {
      stage.createActor(id, new FailureRole(new IndexOutOfBoundsException()));

    } catch (final IndexOutOfBoundsException ignored) {
    }
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = stage.createActor(id, testRole);
    assertThat(actor).isNotNull();
    assertThat(actor.getId()).isEqualTo(id);
    actor.tell("test", null, Stage.STAND_IN);
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly("test");
  }

  private static class FailureRole extends Role {

    private final Exception exception;

    private FailureRole(@NotNull final Exception exception) {
      this.exception = exception;
    }

    @NotNull
    public Behavior getBehavior(@NotNull final String id) throws Exception {
      throw exception;
    }
  }

  private static class TestRole extends Role {

    private final TestExecutorService executorService;
    private final ArrayList<Object> messages = new ArrayList<Object>();
    private final ArrayList<Actor> senders = new ArrayList<Actor>();

    private TestRole(@NotNull final TestExecutorService executorService) {
      this.executorService = executorService;
    }

    @NotNull
    public Behavior getBehavior(@NotNull final String id) {
      return new AbstractBehavior() {

        public void onMessage(final Object message, @NotNull final Envelop envelop,
            @NotNull final Agent agent) {
          messages.add(message);
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
    ArrayList<Object> getMessages() {
      return messages;
    }

    @NotNull
    ArrayList<Actor> getSenders() {
      return senders;
    }
  }
}
