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

package dm.shakespeare.template.typed;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;

import dm.shakespeare.Stage;
import dm.shakespeare.Stage.StageSignal;
import dm.shakespeare.actor.AbstractBehavior;
import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.ActorSet;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.Headers;
import dm.shakespeare.actor.Role;
import dm.shakespeare.concurrent.ExecutorServices;
import dm.shakespeare.function.Tester;
import dm.shakespeare.template.typed.actor.Script;
import dm.shakespeare.test.concurrent.TestExecutorService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link TypedStage} unit tests.
 */
public class TypedStageTest {

  @Test
  public void createActor() {
    final TypedStage stage = new TypedStage(new Stage());
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = stage.createActor(testRole);
    assertThat(actor).isNotNull();
    assertThat(actor.getId()).isNotNull();
    actor.tell("test", Headers.EMPTY, Stage.STAND_IN);
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly("test");
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void createActorFailure() {
    new TypedStage(new Stage()).createActor(new FailureRole(new IndexOutOfBoundsException()));
  }

  @Test
  public void createActorId() {
    final TypedStage stage = new TypedStage(new Stage());
    final String id = UUID.randomUUID().toString();
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = stage.createActor(id, testRole);
    assertThat(actor).isNotNull();
    assertThat(actor.getId()).isEqualTo(id);
    actor.tell("test", Headers.EMPTY, Stage.STAND_IN);
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly("test");
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void createActorIdNPE() {
    final TypedStage stage = new TypedStage(new Stage());
    stage.createActor("id", null);
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void createActorNPE() {
    final TypedStage stage = new TypedStage(new Stage());
    stage.createActor(null);
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void createActorNullIdNPE() {
    final TypedStage stage = new TypedStage(new Stage());
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    stage.createActor(null, testRole);
  }

  @Test(expected = IllegalStateException.class)
  public void createActorSameId() {
    final TypedStage stage = new TypedStage(new Stage());
    final String id = UUID.randomUUID().toString();
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = stage.createActor(id, testRole);
    assertThat(actor).isNotNull();
    assertThat(actor.getId()).isEqualTo(id);
    actor.tell("test", Headers.EMPTY, Stage.STAND_IN);
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly("test");
    stage.createActor(id, new TestRole(executorService));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void createTypedActor() {
    final TypedStage stage = new TypedStage(new Stage());
    final List<String> actor = stage.createActor(List.class, new Script() {

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return ExecutorServices.localExecutor();
      }

      @NotNull
      @Override
      public Object getRole(@NotNull final String id) {
        return new ArrayList<String>();
      }
    });
    assertThat(actor).isNotNull();
    assertThat(TypedStage.getActor(actor).getId()).isNotNull();
    assertThat(actor.add("test")).isTrue();
    assertThat(actor).containsExactly("test");
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void createTypedActorIdNPE() {
    final TypedStage stage = new TypedStage(new Stage());
    stage.createActor(List.class, "id", null);
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void createTypedActorNPE() {
    final TypedStage stage = new TypedStage(new Stage());
    stage.createActor(List.class, null);
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void createTypedActorNullIdNPE() {
    final TypedStage stage = new TypedStage(new Stage());
    stage.createActor(null, new Script() {

      @NotNull
      @Override
      public Object getRole(@NotNull final String id) {
        return new Object();
      }
    });
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void createTypedActorRoleNPE() {
    new TypedStage(new Stage()).createActor(List.class, "id", new Script() {

      @NotNull
      @Override
      public Object getRole(@NotNull final String id) {
        return null;
      }
    });
  }

  @Test(expected = IllegalStateException.class)
  @SuppressWarnings("unchecked")
  public void createTypedActorSameId() {
    final TypedStage stage = new TypedStage(new Stage());
    final String id = UUID.randomUUID().toString();
    final List<String> actor = stage.createActor(List.class, id, new Script() {

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return ExecutorServices.localExecutor();
      }

      @NotNull
      @Override
      public Object getRole(@NotNull final String id) {
        return new ArrayList<String>();
      }
    });
    assertThat(actor).isNotNull();
    assertThat(TypedStage.getActor(actor).getId()).isEqualTo(id);
    stage.createActor(id, new TestRole(new TestExecutorService()));
  }

  @Test
  public void findAllPattern() {
    final TypedStage stage = new TypedStage(new Stage());
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
    final TypedStage stage = new TypedStage(new Stage());
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
    final TypedStage stage = new TypedStage(new Stage());
    stage.findAll((Pattern) null);
  }

  @Test
  public void findAllPatternNotFound() {
    final TypedStage stage = new TypedStage(new Stage());
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
    final TypedStage stage = new TypedStage(new Stage());
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
    final TypedStage stage = new TypedStage(new Stage());
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
    final TypedStage stage = new TypedStage(new Stage());
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
    final TypedStage stage = new TypedStage(new Stage());
    stage.findAll((Tester<? super Actor>) null);
  }

  @Test
  public void findAllTesterNotFound() {
    final TypedStage stage = new TypedStage(new Stage());
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
    final TypedStage stage = new TypedStage(new Stage());
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
    final TypedStage stage = new TypedStage(new Stage());
    assertThat(stage.findAny(Pattern.compile("^./3$"))).isNull();
    assertThat(stage.findAny(Pattern.compile(".*"))).isNull();
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void findAnyPatternNPE() {
    final TypedStage stage = new TypedStage(new Stage());
    stage.findAny((Pattern) null);
  }

  @Test
  public void findAnyPatternNotFound() {
    final TypedStage stage = new TypedStage(new Stage());
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
    final TypedStage stage = new TypedStage(new Stage());
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
    final TypedStage stage = new TypedStage(new Stage());
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
    final TypedStage stage = new TypedStage(new Stage());
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
    final TypedStage stage = new TypedStage(new Stage());
    stage.findAny((Tester<? super Actor>) null);
  }

  @Test
  public void findAnyTesterNotFound() {
    final TypedStage stage = new TypedStage(new Stage());
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
    final TypedStage stage = new TypedStage(new Stage());
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

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void getActorNPE() {
    TypedStage.getActor(null);
  }

  @Test
  public void getAll() {
    final TypedStage stage = new TypedStage(new Stage());
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
    final TypedStage stage = new TypedStage(new Stage());
    assertThat(stage.getAll()).isEmpty();
  }

  @Test
  public void getEmpty() {
    final TypedStage stage = new TypedStage(new Stage());
    final Actor a = stage.get("a/3");
    final Actor b = stage.get("b/3");
    assertThat(a).isNull();
    assertThat(b).isNull();
  }

  @Test
  public void getNotFound() {
    final TypedStage stage = new TypedStage(new Stage());
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
    final Actor actor = TypedStage.newActor(testRole);
    assertThat(actor).isNotNull();
    assertThat(actor.getId()).isNotNull();
    actor.tell("test", Headers.EMPTY, Stage.STAND_IN);
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly("test");
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void newActorFailure() {
    TypedStage.newActor(new FailureRole(new IndexOutOfBoundsException()));
  }

  @Test
  public void newActorId() {
    final String id = UUID.randomUUID().toString();
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = TypedStage.newActor(id, testRole);
    assertThat(actor).isNotNull();
    assertThat(actor.getId()).isEqualTo(id);
    actor.tell("test", Headers.EMPTY, Stage.STAND_IN);
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly("test");
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void newActorIdNPE() {
    TypedStage.newActor("id", null);
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void newActorNPE() {
    TypedStage.newActor(null);
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void newActorNullIdNPE() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    TypedStage.newActor(null, testRole);
  }

  @Test
  public void newActorSameId() {
    final String id = UUID.randomUUID().toString();
    final TestExecutorService executorService = new TestExecutorService();
    TestRole testRole = new TestRole(executorService);
    Actor actor = TypedStage.newActor(id, testRole);
    assertThat(actor).isNotNull();
    assertThat(actor.getId()).isEqualTo(id);
    actor.tell("test", Headers.EMPTY, Stage.STAND_IN);
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly("test");
    testRole = new TestRole(executorService);
    actor = TypedStage.newActor(id, testRole);
    assertThat(actor).isNotNull();
    assertThat(actor.getId()).isEqualTo(id);
    actor.tell("test", Headers.EMPTY, Stage.STAND_IN);
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly("test");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void newTypedActor() {
    final List<String> actor = TypedStage.newActor(List.class, new Script() {

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return ExecutorServices.localExecutor();
      }

      @NotNull
      @Override
      public Object getRole(@NotNull final String id) {
        return new ArrayList<String>();
      }
    });
    assertThat(actor).isNotNull();
    assertThat(TypedStage.getActor(actor).getId()).isNotNull();
    assertThat(actor.add("test")).isTrue();
    assertThat(actor).containsExactly("test");
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void newTypedActorIdNPE() {
    TypedStage.newActor(List.class, "id", null);
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void newTypedActorNPE() {
    TypedStage.newActor(List.class, null);
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void newTypedActorNullIdNPE() {
    TypedStage.newActor(null, new Script() {

      @NotNull
      @Override
      public Object getRole(@NotNull final String id) {
        return new Object();
      }
    });
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void newTypedActorRoleNPE() {
    TypedStage.newActor(List.class, "id", new Script() {

      @NotNull
      @Override
      public Object getRole(@NotNull final String id) {
        return null;
      }
    });
  }

  @Test
  @SuppressWarnings("unchecked")
  public void newTypedActorSameId() {
    final String id = UUID.randomUUID().toString();
    final List<String> actor = TypedStage.newActor(List.class, id, new Script() {

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return ExecutorServices.localExecutor();
      }

      @NotNull
      @Override
      public Object getRole(@NotNull final String id) {
        return new ArrayList<String>();
      }
    });
    assertThat(actor).isNotNull();
    assertThat(TypedStage.getActor(actor).getId()).isEqualTo(id);
    final Actor roleActor = TypedStage.newActor(id, new TestRole(new TestExecutorService()));
    assertThat(roleActor).isNotNull();
    assertThat(roleActor.getId()).isEqualTo(id);
  }

  @Test
  public void observerCreate() {
    final TypedStage stage = new TypedStage(new Stage());
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = stage.createActor(testRole);
    stage.addObserver(actor);
    final Actor createdActor = stage.createActor(new TestRole(executorService));
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly(StageSignal.CREATE);
    assertThat(testRole.getSenders()).containsExactly(createdActor);
  }

  @Test
  public void observerCreateRemoved() {
    final TypedStage stage = new TypedStage(new Stage());
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
    final TypedStage stage = new TypedStage(new Stage());
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = stage.createActor(testRole);
    final Actor actorToDismiss = stage.createActor(new TestRole(executorService));
    stage.addObserver(actor);
    actorToDismiss.dismiss();
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly(StageSignal.DISMISS);
    assertThat(testRole.getSenders()).containsExactly(actorToDismiss);
  }

  @Test
  public void observerDismissRemoved() {
    final TypedStage stage = new TypedStage(new Stage());
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
    final TypedStage stage = new TypedStage(new Stage());
    try {
      stage.createActor(id, new FailureRole(new IndexOutOfBoundsException()));

    } catch (final IndexOutOfBoundsException ignored) {
    }
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = stage.createActor(id, testRole);
    assertThat(actor).isNotNull();
    assertThat(actor.getId()).isEqualTo(id);
    actor.tell("test", Headers.EMPTY, Stage.STAND_IN);
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly("test");
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void removeObserverNPE() {
    final TypedStage stage = new TypedStage(new Stage());
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
