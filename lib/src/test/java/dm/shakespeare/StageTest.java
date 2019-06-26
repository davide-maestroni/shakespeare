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
import java.util.regex.Pattern;

import dm.shakespeare.actor.AbstractBehavior;
import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.ActorSet;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.Headers;
import dm.shakespeare.actor.Role;
import dm.shakespeare.function.Tester;
import dm.shakespeare.test.concurrent.TestExecutorService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link Stage} unit tests.
 */
public class StageTest {

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void addObserverNPE() {
    final Stage stage = new Stage();
    stage.addObserver(null);
  }

  @Test
  public void createActor() {
    final Stage stage = new Stage();
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = stage.createActor(testRole);
    assertThat(actor).isNotNull();
    assertThat(actor.getId()).isNotNull();
    actor.tell("test", Headers.NONE, Stage.STAND_IN);
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
    actor.tell("test", Headers.NONE, Stage.STAND_IN);
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly("test");
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void createActorIdNPE() {
    final Stage stage = new Stage();
    stage.createActor("id", null);
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void createActorNPE() {
    final Stage stage = new Stage();
    stage.createActor(null);
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void createActorNullIdNPE() {
    final Stage stage = new Stage();
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    stage.createActor(null, testRole);
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
    actor.tell("test", Headers.NONE, Stage.STAND_IN);
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly("test");
    stage.createActor(id, new TestRole(executorService));
  }

  @Test
  public void findAllPattern() {
    final Stage stage = new Stage();
    final TestExecutorService executorService = new TestExecutorService();
    for (int i = 0; i < 5; i++) {
      stage.createActor("a/" + i, new TestRole(executorService));
    }
    for (int i = 0; i < 5; i++) {
      stage.createActor("b/" + i, new TestRole(executorService));
    }
    final ActorSet a = stage.findAll(Pattern.compile("^a/.*$"));
    final ActorSet b = stage.findAll(Pattern.compile("^b/.*$"));
    assertThat(a).hasSize(5).doesNotContainAnyElementsOf(b);
    assertThat(b).hasSize(5);
    assertThat(stage.findAll(Pattern.compile("^./3$"))).hasSize(2);
    assertThat(stage.findAll(Pattern.compile(".*"))).hasSize(10).isEqualTo(stage.getAll());
  }

  @Test
  public void findAllPatternEmpty() {
    final Stage stage = new Stage();
    final ActorSet a = stage.findAll(Pattern.compile("^a/.*$"));
    final ActorSet b = stage.findAll(Pattern.compile("^b/.*$"));
    assertThat(a).isEmpty();
    assertThat(b).isEmpty();
    assertThat(stage.findAll(Pattern.compile("^./3$"))).isEmpty();
    assertThat(stage.findAll(Pattern.compile(".*"))).isEmpty();
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void findAllPatternNPE() {
    final Stage stage = new Stage();
    stage.findAll((Pattern) null);
  }

  @Test
  public void findAllPatternNotFound() {
    final Stage stage = new Stage();
    final TestExecutorService executorService = new TestExecutorService();
    for (int i = 0; i < 5; i++) {
      stage.createActor("a/" + i, new TestRole(executorService));
    }
    for (int i = 0; i < 5; i++) {
      stage.createActor("b/" + i, new TestRole(executorService));
    }
    assertThat(stage.findAll(Pattern.compile("^c/.*$"))).isEmpty();
  }

  @Test
  public void findAllTester() {
    final Stage stage = new Stage();
    final TestExecutorService executorService = new TestExecutorService();
    for (int i = 0; i < 5; i++) {
      stage.createActor("a/" + i, new TestRole(executorService));
    }
    for (int i = 0; i < 5; i++) {
      stage.createActor("b/" + i, new TestRole(executorService));
    }
    final ActorSet a = stage.findAll(new Tester<Actor>() {

      public boolean test(final Actor actor) {
        return actor.getId().startsWith("a/");
      }
    });
    final ActorSet b = stage.findAll(new Tester<Actor>() {

      public boolean test(final Actor actor) {
        return actor.getId().startsWith("b/");
      }
    });
    assertThat(a).hasSize(5).doesNotContainAnyElementsOf(b);
    assertThat(b).hasSize(5);
    assertThat(stage.findAll(new Tester<Actor>() {

      public boolean test(final Actor actor) {
        return actor.getId().endsWith("3");
      }
    })).hasSize(2);
    assertThat(stage.findAll(new Tester<Actor>() {

      public boolean test(final Actor actor) {
        return true;
      }
    })).hasSize(10).isEqualTo(stage.getAll());
  }

  @Test
  public void findAllTesterEmpty() {
    final Stage stage = new Stage();
    final ActorSet a = stage.findAll(new Tester<Actor>() {

      public boolean test(final Actor actor) {
        return actor.getId().startsWith("a/");
      }
    });
    final ActorSet b = stage.findAll(new Tester<Actor>() {

      public boolean test(final Actor actor) {
        return actor.getId().startsWith("b/");
      }
    });
    assertThat(a).isEmpty();
    assertThat(b).isEmpty();
    assertThat(stage.findAll(new Tester<Actor>() {

      public boolean test(final Actor actor) {
        return actor.getId().endsWith("3");
      }
    })).isEmpty();
    assertThat(stage.findAll(new Tester<Actor>() {

      public boolean test(final Actor actor) {
        return true;
      }
    })).isEmpty();
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void findAllTesterFailure() {
    final Stage stage = new Stage();
    final TestExecutorService executorService = new TestExecutorService();
    for (int i = 0; i < 5; i++) {
      stage.createActor("a/" + i, new TestRole(executorService));
    }
    for (int i = 0; i < 5; i++) {
      stage.createActor("b/" + i, new TestRole(executorService));
    }
    assertThat(stage.findAll(new Tester<Actor>() {

      public boolean test(final Actor actor) {
        throw new IndexOutOfBoundsException();
      }
    }));
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void findAllTesterNPE() {
    final Stage stage = new Stage();
    stage.findAll((Tester<? super Actor>) null);
  }

  @Test
  public void findAllTesterNotFound() {
    final Stage stage = new Stage();
    final TestExecutorService executorService = new TestExecutorService();
    for (int i = 0; i < 5; i++) {
      stage.createActor("a/" + i, new TestRole(executorService));
    }
    for (int i = 0; i < 5; i++) {
      stage.createActor("b/" + i, new TestRole(executorService));
    }
    assertThat(stage.findAll(new Tester<Actor>() {

      public boolean test(final Actor actor) {
        return actor.getId().startsWith("c/");
      }
    })).isEmpty();
  }

  @Test
  public void findAnyPattern() {
    final Stage stage = new Stage();
    final TestExecutorService executorService = new TestExecutorService();
    for (int i = 0; i < 5; i++) {
      stage.createActor("a/" + i, new TestRole(executorService));
    }
    for (int i = 0; i < 5; i++) {
      stage.createActor("b/" + i, new TestRole(executorService));
    }
    final Actor a = stage.findAny(Pattern.compile("^a/.*$"));
    final Actor b = stage.findAny(Pattern.compile("^b/.*$"));
    assertThat(a).isNotNull().isNotEqualTo(b);
    assertThat(b).isNotNull();
    assertThat(stage.findAny(Pattern.compile("^./3$"))).isNotNull();
    assertThat(stage.findAny(Pattern.compile(".*"))).isNotNull();
  }

  @Test
  public void findAnyPatternEmpty() {
    final Stage stage = new Stage();
    assertThat(stage.findAny(Pattern.compile("^./3$"))).isNull();
    assertThat(stage.findAny(Pattern.compile(".*"))).isNull();
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void findAnyPatternNPE() {
    final Stage stage = new Stage();
    stage.findAny((Pattern) null);
  }

  @Test
  public void findAnyPatternNotFound() {
    final Stage stage = new Stage();
    final TestExecutorService executorService = new TestExecutorService();
    for (int i = 0; i < 5; i++) {
      stage.createActor("a/" + i, new TestRole(executorService));
    }
    for (int i = 0; i < 5; i++) {
      stage.createActor("b/" + i, new TestRole(executorService));
    }
    assertThat(stage.findAny(Pattern.compile("^c/.*$"))).isNull();
  }

  @Test
  public void findAnyTester() {
    final Stage stage = new Stage();
    final TestExecutorService executorService = new TestExecutorService();
    for (int i = 0; i < 5; i++) {
      stage.createActor("a/" + i, new TestRole(executorService));
    }
    for (int i = 0; i < 5; i++) {
      stage.createActor("b/" + i, new TestRole(executorService));
    }
    final Actor a = stage.findAny(new Tester<Actor>() {

      public boolean test(final Actor actor) {
        return actor.getId().startsWith("a/");
      }
    });
    final Actor b = stage.findAny(new Tester<Actor>() {

      public boolean test(final Actor actor) {
        return actor.getId().startsWith("b/");
      }
    });
    assertThat(a).isNotNull().isNotEqualTo(b);
    assertThat(b).isNotNull();
    assertThat(stage.findAny(new Tester<Actor>() {

      public boolean test(final Actor actor) {
        return actor.getId().endsWith("3");
      }
    })).isNotNull();
    assertThat(stage.findAny(new Tester<Actor>() {

      public boolean test(final Actor actor) {
        return true;
      }
    })).isNotNull();
  }

  @Test
  public void findAnyTesterEmpty() {
    final Stage stage = new Stage();
    final Actor a = stage.findAny(new Tester<Actor>() {

      public boolean test(final Actor actor) {
        return actor.getId().startsWith("a/");
      }
    });
    final Actor b = stage.findAny(new Tester<Actor>() {

      public boolean test(final Actor actor) {
        return actor.getId().startsWith("b/");
      }
    });
    assertThat(a).isNull();
    assertThat(b).isNull();
    assertThat(stage.findAny(new Tester<Actor>() {

      public boolean test(final Actor actor) {
        return actor.getId().endsWith("3");
      }
    })).isNull();
    assertThat(stage.findAny(new Tester<Actor>() {

      public boolean test(final Actor actor) {
        return true;
      }
    })).isNull();
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void findAnyTesterFailure() {
    final Stage stage = new Stage();
    final TestExecutorService executorService = new TestExecutorService();
    for (int i = 0; i < 5; i++) {
      stage.createActor("a/" + i, new TestRole(executorService));
    }
    for (int i = 0; i < 5; i++) {
      stage.createActor("b/" + i, new TestRole(executorService));
    }
    assertThat(stage.findAny(new Tester<Actor>() {

      public boolean test(final Actor actor) {
        throw new IndexOutOfBoundsException();
      }
    }));
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void findAnyTesterNPE() {
    final Stage stage = new Stage();
    stage.findAny((Tester<? super Actor>) null);
  }

  @Test
  public void findAnyTesterNotFound() {
    final Stage stage = new Stage();
    final TestExecutorService executorService = new TestExecutorService();
    for (int i = 0; i < 5; i++) {
      stage.createActor("a/" + i, new TestRole(executorService));
    }
    for (int i = 0; i < 5; i++) {
      stage.createActor("b/" + i, new TestRole(executorService));
    }
    assertThat(stage.findAny(new Tester<Actor>() {

      public boolean test(final Actor actor) {
        return actor.getId().startsWith("c/");
      }
    })).isNull();
  }

  @Test
  public void get() {
    final Stage stage = new Stage();
    final TestExecutorService executorService = new TestExecutorService();
    for (int i = 0; i < 5; i++) {
      stage.createActor("a/" + i, new TestRole(executorService));
    }
    for (int i = 0; i < 5; i++) {
      stage.createActor("b/" + i, new TestRole(executorService));
    }
    final Actor a = stage.get("a/3");
    final Actor b = stage.get("b/3");
    assertThat(a).isNotNull().isNotEqualTo(b);
    assertThat(b).isNotNull();
  }

  @Test
  public void getAll() {
    final Stage stage = new Stage();
    final TestExecutorService executorService = new TestExecutorService();
    for (int i = 0; i < 5; i++) {
      stage.createActor("a/" + i, new TestRole(executorService));
    }
    for (int i = 0; i < 5; i++) {
      stage.createActor("b/" + i, new TestRole(executorService));
    }
    assertThat(stage.getAll()).hasSize(10);
  }

  @Test
  public void getAllEmpty() {
    final Stage stage = new Stage();
    assertThat(stage.getAll()).isEmpty();
  }

  @Test
  public void getEmpty() {
    final Stage stage = new Stage();
    final Actor a = stage.get("a/3");
    final Actor b = stage.get("b/3");
    assertThat(a).isNull();
    assertThat(b).isNull();
  }

  @Test
  public void getNotFound() {
    final Stage stage = new Stage();
    final TestExecutorService executorService = new TestExecutorService();
    for (int i = 0; i < 5; i++) {
      stage.createActor("a/" + i, new TestRole(executorService));
    }
    for (int i = 0; i < 5; i++) {
      stage.createActor("b/" + i, new TestRole(executorService));
    }
    assertThat(stage.get("c/3")).isNull();
  }

  @Test
  public void newActor() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = Stage.newActor(testRole);
    assertThat(actor).isNotNull();
    assertThat(actor.getId()).isNotNull();
    actor.tell("test", Headers.NONE, Stage.STAND_IN);
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
    actor.tell("test", Headers.NONE, Stage.STAND_IN);
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly("test");
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void newActorIdNPE() {
    Stage.newActor("id", null);
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void newActorNPE() {
    Stage.newActor(null);
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void newActorNullIdNPE() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    Stage.newActor(null, testRole);
  }

  @Test
  public void newActorSameId() {
    final String id = UUID.randomUUID().toString();
    final TestExecutorService executorService = new TestExecutorService();
    TestRole testRole = new TestRole(executorService);
    Actor actor = Stage.newActor(id, testRole);
    assertThat(actor).isNotNull();
    assertThat(actor.getId()).isEqualTo(id);
    actor.tell("test", Headers.NONE, Stage.STAND_IN);
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly("test");
    testRole = new TestRole(executorService);
    actor = Stage.newActor(id, testRole);
    assertThat(actor).isNotNull();
    assertThat(actor.getId()).isEqualTo(id);
    actor.tell("test", Headers.NONE, Stage.STAND_IN);
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
    actor.tell("test", Headers.NONE, Stage.STAND_IN);
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly("test");
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void removeObserverNPE() {
    final Stage stage = new Stage();
    stage.removeObserver(null);
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
