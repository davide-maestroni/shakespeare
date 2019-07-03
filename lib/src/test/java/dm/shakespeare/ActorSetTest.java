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
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import dm.shakespeare.actor.AbstractBehavior;
import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.ActorSet;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.Headers;
import dm.shakespeare.actor.Role;
import dm.shakespeare.message.Bounce;
import dm.shakespeare.message.DeadLetter;
import dm.shakespeare.message.Delivery;
import dm.shakespeare.message.QuotaExceeded;
import dm.shakespeare.test.concurrent.TestExecutorService;
import dm.shakespeare.util.Iterables;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ActorSet} unit tests.
 */
public class ActorSetTest {

  @Test
  public void addObserver() {
    final Stage stage = new Stage();
    final TestExecutorService executorService = new TestExecutorService();
    stage.createActor(new TestRole(executorService));
    final ActorSet actors = stage.getAll();
    final TestRole observerRole = new TestRole(executorService);
    final Actor observer = stage.createActor(observerRole);
    assertThat(actors.addObserver(observer)).isTrue();
    actors.dismiss();
    executorService.consumeAll();
    assertThat(observerRole.getMessages()).hasSize(1);
    assertThat(observerRole.getMessages().get(0)).isInstanceOf(DeadLetter.class);
    assertThat(observerRole.getSenders()).hasSize(1);
    assertThat(observerRole.getSenders().get(0)).isSameAs(Iterables.first(actors));
  }

  @Test
  public void addObserverFailure() {
    final Stage stage = new Stage();
    final RejectingExecutorService rejectingExecutor = new RejectingExecutorService();
    stage.createActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return newBehavior().build();
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return rejectingExecutor;
      }
    });
    final ActorSet actors = stage.getAll();
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole observerRole = new TestRole(executorService);
    final Actor observer = stage.createActor(observerRole);
    rejectingExecutor.consumeAll();
    rejectingExecutor.setRejecting(true);
    assertThat(actors.addObserver(observer)).isFalse();
    actors.dismiss();
    executorService.consumeAll();
    assertThat(observerRole.getMessages()).isEmpty();
    assertThat(observerRole.getSenders()).isEmpty();
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void addObserverNPE() {
    final Stage stage = new Stage();
    final TestExecutorService executorService = new TestExecutorService();
    stage.createActor(new TestRole(executorService));
    final ActorSet actors = stage.getAll();
    actors.addObserver(null);
  }

  @Test
  public void dismiss() {
    final Stage stage = new Stage();
    final TestExecutorService executorService = new TestExecutorService();
    stage.createActor(new TestRole(executorService));
    final ActorSet actors = stage.getAll();
    final TestRole observerRole = new TestRole(executorService);
    final Actor observer = stage.createActor(observerRole);
    actors.tell("test", new Headers().withReceiptId("test"), observer);
    assertThat(actors.dismiss()).isTrue();
    executorService.consumeAll();
    assertThat(observerRole.getMessages()).hasSize(1);
    assertThat(observerRole.getMessages().get(0)).isInstanceOf(Bounce.class);
    assertThat(observerRole.getSenders()).hasSize(1);
    assertThat(observerRole.getSenders().get(0)).isSameAs(Iterables.first(actors));
  }

  @Test
  public void dismissFailure() {
    final Stage stage = new Stage();
    final RejectingExecutorService rejectingExecutor = new RejectingExecutorService();
    stage.createActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return newBehavior().build();
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return rejectingExecutor;
      }
    });
    final ActorSet actors = stage.getAll();
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole observerRole = new TestRole(executorService);
    final Actor observer = stage.createActor(observerRole);
    rejectingExecutor.consumeAll();
    rejectingExecutor.setRejecting(true);
    assertThat(actors.dismiss()).isFalse();
    rejectingExecutor.setRejecting(false);
    actors.tell("test", new Headers().withReceiptId("test"), observer);
    rejectingExecutor.consumeAll();
    executorService.consumeAll();
    assertThat(observerRole.getMessages()).hasSize(1);
    assertThat(observerRole.getMessages().get(0)).isInstanceOf(Delivery.class);
    assertThat(observerRole.getSenders()).hasSize(1);
    assertThat(observerRole.getSenders().get(0)).isSameAs(Iterables.first(actors));
  }

  @Test
  public void dismissLazy() {
    final Stage stage = new Stage();
    final TestExecutorService executorService = new TestExecutorService();
    stage.createActor(new TestRole(executorService));
    final ActorSet actors = stage.getAll();
    final TestRole observerRole = new TestRole(executorService);
    final Actor observer = stage.createActor(observerRole);
    actors.tell("test", new Headers().withReceiptId("test"), observer);
    assertThat(actors.dismissLazy()).isTrue();
    executorService.consumeAll();
    assertThat(observerRole.getMessages()).hasSize(1);
    assertThat(observerRole.getMessages().get(0)).isInstanceOf(Delivery.class);
    assertThat(observerRole.getSenders()).hasSize(1);
    assertThat(observerRole.getSenders().get(0)).isSameAs(Iterables.first(actors));
  }

  @Test
  public void dismissLazyFailure() {
    final Stage stage = new Stage();
    final RejectingExecutorService rejectingExecutor = new RejectingExecutorService();
    stage.createActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return newBehavior().build();
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return rejectingExecutor;
      }
    });
    final ActorSet actors = stage.getAll();
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole observerRole = new TestRole(executorService);
    final Actor observer = stage.createActor(observerRole);
    rejectingExecutor.consumeAll();
    rejectingExecutor.setRejecting(true);
    assertThat(actors.dismissLazy()).isFalse();
    rejectingExecutor.setRejecting(false);
    actors.tell("test", new Headers().withReceiptId("test"), observer);
    rejectingExecutor.consumeAll();
    executorService.consumeAll();
    assertThat(observerRole.getMessages()).hasSize(1);
    assertThat(observerRole.getMessages().get(0)).isInstanceOf(Delivery.class);
    assertThat(observerRole.getSenders()).hasSize(1);
    assertThat(observerRole.getSenders().get(0)).isSameAs(Iterables.first(actors));
  }

  @Test
  public void dismissNow() {
    final Stage stage = new Stage();
    final TestExecutorService executorService = new TestExecutorService();
    stage.createActor(new TestRole(executorService));
    final ActorSet actors = stage.getAll();
    final TestRole observerRole = new TestRole(executorService);
    final Actor observer = stage.createActor(observerRole);
    actors.tell("test", new Headers().withReceiptId("test"), observer);
    assertThat(actors.dismissNow()).isTrue();
    executorService.consumeAll();
    assertThat(observerRole.getMessages()).hasSize(1);
    assertThat(observerRole.getMessages().get(0)).isInstanceOf(Bounce.class);
    assertThat(observerRole.getSenders()).hasSize(1);
    assertThat(observerRole.getSenders().get(0)).isSameAs(Iterables.first(actors));
  }

  @Test
  public void dismissNowFailure() {
    final Stage stage = new Stage();
    final RejectingExecutorService rejectingExecutor = new RejectingExecutorService();
    stage.createActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return newBehavior().build();
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return rejectingExecutor;
      }
    });
    final ActorSet actors = stage.getAll();
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole observerRole = new TestRole(executorService);
    final Actor observer = stage.createActor(observerRole);
    rejectingExecutor.consumeAll();
    rejectingExecutor.setRejecting(true);
    assertThat(actors.dismissNow()).isFalse();
    rejectingExecutor.setRejecting(false);
    actors.tell("test", new Headers().withReceiptId("test"), observer);
    rejectingExecutor.consumeAll();
    executorService.consumeAll();
    assertThat(observerRole.getMessages()).hasSize(1);
    assertThat(observerRole.getMessages().get(0)).isInstanceOf(Delivery.class);
    assertThat(observerRole.getSenders()).hasSize(1);
    assertThat(observerRole.getSenders().get(0)).isSameAs(Iterables.first(actors));
  }

  @Test
  public void dismissNowInterrupt() throws InterruptedException {
    final Stage stage = new Stage();
    final TestExecutorService executorService = new TestExecutorService();
    stage.createActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return new AbstractBehavior() {

          public void onMessage(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) throws Exception {
            Thread.sleep(3000);
          }
        };
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return Executors.newSingleThreadExecutor();
      }
    });
    final ActorSet actors = stage.getAll();
    final TestRole observerRole = new TestRole(executorService);
    final Actor observer = stage.createActor(observerRole);
    actors.tell("test", new Headers().withReceiptId("test"), observer);
    Thread.sleep(1000);
    assertThat(actors.dismissNow()).isTrue();
    Thread.sleep(1000);
    executorService.consumeAll();
    assertThat(observerRole.getMessages()).hasSize(1);
    assertThat(observerRole.getMessages().get(0)).isInstanceOf(Bounce.class);
    assertThat(observerRole.getSenders()).hasSize(1);
    assertThat(observerRole.getSenders().get(0)).isSameAs(Iterables.first(actors));
  }

  @Test
  public void removeObserver() {
    final Stage stage = new Stage();
    final TestExecutorService executorService = new TestExecutorService();
    stage.createActor(new TestRole(executorService));
    final ActorSet actors = stage.getAll();
    final TestRole observerRole = new TestRole(executorService);
    final Actor observer = stage.createActor(observerRole);
    assertThat(actors.addObserver(observer)).isTrue();
    assertThat(actors.removeObserver(observer)).isTrue();
    executorService.consumeAll();
    actors.dismiss();
    executorService.consumeAll();
    assertThat(observerRole.getMessages()).isEmpty();
    assertThat(observerRole.getSenders()).isEmpty();
  }

  @Test
  public void removeObserverEmpty() {
    final Stage stage = new Stage();
    final TestExecutorService executorService = new TestExecutorService();
    stage.createActor(new TestRole(executorService));
    final ActorSet actors = stage.getAll();
    final TestRole observerRole = new TestRole(executorService);
    final Actor observer = stage.createActor(observerRole);
    assertThat(actors.removeObserver(observer)).isTrue();
    actors.dismiss();
    executorService.consumeAll();
    assertThat(observerRole.getMessages()).isEmpty();
    assertThat(observerRole.getSenders()).isEmpty();
  }

  @Test
  public void removeObserverFailure() {
    final Stage stage = new Stage();
    final RejectingExecutorService rejectingExecutor = new RejectingExecutorService();
    stage.createActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return newBehavior().build();
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return rejectingExecutor;
      }
    });
    final ActorSet actors = stage.getAll();
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole observerRole = new TestRole(executorService);
    final Actor observer = stage.createActor(observerRole);
    assertThat(actors.addObserver(observer)).isTrue();
    rejectingExecutor.consumeAll();
    rejectingExecutor.setRejecting(true);
    assertThat(actors.removeObserver(observer)).isFalse();
    rejectingExecutor.setRejecting(false);
    actors.dismiss();
    rejectingExecutor.consumeAll();
    executorService.consumeAll();
    assertThat(observerRole.getMessages()).hasSize(1);
    assertThat(observerRole.getMessages().get(0)).isInstanceOf(DeadLetter.class);
    assertThat(observerRole.getSenders()).hasSize(1);
    assertThat(observerRole.getSenders().get(0)).isSameAs(Iterables.first(actors));
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void removeObserverNPE() {
    final Stage stage = new Stage();
    final TestExecutorService executorService = new TestExecutorService();
    stage.createActor(new TestRole(executorService));
    final ActorSet actors = stage.getAll();
    actors.removeObserver(null);
  }

  @Test
  public void tell() {
    final Stage stage = new Stage();
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole role = new TestRole(executorService);
    stage.createActor(role);
    final ActorSet actors = stage.getAll();
    actors.tell("test", Headers.empty(), Stage.standIn());
    executorService.consumeAll();
    assertThat(role.getMessages()).containsExactly("test");
    assertThat(role.getSenders()).containsExactly(Stage.standIn());
  }

  @Test
  public void tellAll() {
    final Stage stage = new Stage();
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole role = new TestRole(executorService);
    stage.createActor(role);
    final ActorSet actors = stage.getAll();
    actors.tellAll(Arrays.asList("test1", "test2"), Headers.empty(), Stage.standIn());
    executorService.consumeAll();
    assertThat(role.getMessages()).containsExactly("test1", "test2");
    assertThat(role.getSenders()).containsExactly(Stage.standIn(), Stage.standIn());
  }

  @Test
  public void tellAllBounce() {
    final Stage stage = new Stage();
    final TestExecutorService executorService = new TestExecutorService();
    stage.createActor(new TestRole(executorService));
    final ActorSet actors = stage.getAll();
    final TestRole observerRole = new TestRole(executorService);
    final Actor observer = stage.createActor(observerRole);
    actors.dismiss();
    actors.tellAll(Arrays.asList("test1", "test2"), new Headers().withReceiptId("test"), observer);
    executorService.consumeAll();
    assertThat(observerRole.getMessages()).hasSize(2);
    assertThat(observerRole.getMessages().get(0)).isInstanceOf(Bounce.class);
    assertThat(observerRole.getMessages().get(1)).isInstanceOf(Bounce.class);
    assertThat(observerRole.getSenders()).hasSize(2);
    assertThat(observerRole.getSenders().get(0)).isSameAs(Iterables.first(actors));
    assertThat(observerRole.getSenders().get(1)).isSameAs(Iterables.first(actors));
  }

  @Test
  public void tellAllDelivery() {
    final Stage stage = new Stage();
    final TestExecutorService executorService = new TestExecutorService();
    final Actor actor = stage.createActor(new TestRole(executorService));
    final TestRole observerRole = new TestRole(executorService);
    final Actor observer = stage.createActor(observerRole);
    actor.tellAll(Arrays.asList("test1", "test2"), new Headers().withReceiptId("test"), observer);
    executorService.consumeAll();
    assertThat(observerRole.getMessages()).hasSize(2);
    assertThat(observerRole.getMessages().get(0)).isInstanceOf(Delivery.class);
    assertThat(observerRole.getMessages().get(1)).isInstanceOf(Delivery.class);
    assertThat(observerRole.getSenders()).hasSize(2);
    assertThat(observerRole.getSenders().get(0)).isSameAs(actor);
    assertThat(observerRole.getSenders().get(1)).isSameAs(actor);
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void tellAllHeadersNPE() {
    final Stage stage = new Stage();
    final TestExecutorService executorService = new TestExecutorService();
    stage.createActor(new TestRole(executorService));
    final ActorSet actors = stage.getAll();
    actors.tellAll(Collections.emptyList(), null, Stage.standIn());
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void tellAllNPE() {
    final Stage stage = new Stage();
    final TestExecutorService executorService = new TestExecutorService();
    stage.createActor(new TestRole(executorService));
    final ActorSet actors = stage.getAll();
    actors.tellAll(null, Headers.empty(), Stage.standIn());
  }

  @Test
  public void tellAllQuota() {
    final Stage stage = new Stage();
    final TestExecutorService executorService = new TestExecutorService();
    stage.createActor(new TestRole(executorService) {

      @Override
      public int getQuota(@NotNull final String id) {
        return 1;
      }
    });
    final ActorSet actors = stage.getAll();
    final TestRole observerRole = new TestRole(executorService);
    final Actor observer = stage.createActor(observerRole);
    actors.tellAll(Arrays.asList("test1", "test2"), Headers.empty(), observer);
    actors.tellAll(Arrays.asList("test1", "test2"), new Headers().withReceiptId("test"), observer);
    executorService.consumeAll();
    assertThat(observerRole.getMessages()).hasSize(2);
    assertThat(observerRole.getMessages().get(0)).isInstanceOf(QuotaExceeded.class);
    assertThat(observerRole.getMessages().get(1)).isInstanceOf(QuotaExceeded.class);
    assertThat(observerRole.getSenders()).hasSize(2);
    assertThat(observerRole.getSenders().get(0)).isSameAs(Iterables.first(actors));
    assertThat(observerRole.getSenders().get(1)).isSameAs(Iterables.first(actors));
  }

  @Test
  public void tellAllQuotaDelivery() {
    final Stage stage = new Stage();
    final TestExecutorService executorService = new TestExecutorService();
    stage.createActor(new TestRole(executorService) {

      @Override
      public int getQuota(@NotNull final String id) {
        return 1;
      }
    });
    final ActorSet actors = stage.getAll();
    final TestRole observerRole = new TestRole(executorService);
    final Actor observer = stage.createActor(observerRole);
    actors.tellAll(Arrays.asList("test1", "test2"), new Headers().withReceiptId("test"), observer);
    actors.tellAll(Arrays.asList("test1", "test2"), new Headers().withReceiptId("test"), observer);
    executorService.consumeAll();
    assertThat(observerRole.getMessages()).hasSize(4);
    assertThat(observerRole.getMessages().get(0)).isInstanceOf(QuotaExceeded.class);
    assertThat(observerRole.getMessages().get(1)).isInstanceOf(QuotaExceeded.class);
    assertThat(observerRole.getMessages().get(2)).isInstanceOf(Delivery.class);
    assertThat(observerRole.getMessages().get(3)).isInstanceOf(Delivery.class);
    assertThat(observerRole.getSenders()).hasSize(4);
    assertThat(observerRole.getSenders().get(0)).isSameAs(Iterables.first(actors));
    assertThat(observerRole.getSenders().get(1)).isSameAs(Iterables.first(actors));
    assertThat(observerRole.getSenders().get(2)).isSameAs(Iterables.first(actors));
    assertThat(observerRole.getSenders().get(3)).isSameAs(Iterables.first(actors));
  }

  @Test
  public void tellAllQuotaNoReceipt() {
    final Stage stage = new Stage();
    final TestExecutorService executorService = new TestExecutorService();
    stage.createActor(new TestRole(executorService) {

      @Override
      public int getQuota(@NotNull final String id) {
        return 1;
      }
    });
    final ActorSet actors = stage.getAll();
    final TestRole observerRole = new TestRole(executorService);
    final Actor observer = stage.createActor(observerRole);
    actors.tellAll(Arrays.asList("test1", "test2"), Headers.empty(), observer);
    actors.tellAll(Arrays.asList("test1", "test2"), Headers.empty(), observer);
    executorService.consumeAll();
    assertThat(observerRole.getMessages()).isEmpty();
    assertThat(observerRole.getSenders()).isEmpty();
  }

  @Test
  public void tellAllRejected() {
    final Stage stage = new Stage();
    final RejectingExecutorService rejectingExecutor = new RejectingExecutorService();
    stage.createActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return newBehavior().build();
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return rejectingExecutor;
      }
    });
    final ActorSet actors = stage.getAll();
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole observerRole = new TestRole(executorService);
    final Actor observer = stage.createActor(observerRole);
    rejectingExecutor.consumeAll();
    rejectingExecutor.setRejecting(true);
    actors.tellAll(Arrays.asList("test1", "test2"), new Headers().withReceiptId("test"), observer);
    executorService.consumeAll();
    assertThat(observerRole.getMessages()).hasSize(2);
    assertThat(observerRole.getMessages().get(0)).isInstanceOf(Bounce.class);
    assertThat(observerRole.getMessages().get(1)).isInstanceOf(Bounce.class);
    assertThat(observerRole.getSenders()).hasSize(2);
    assertThat(observerRole.getSenders().get(0)).isSameAs(Iterables.first(actors));
    assertThat(observerRole.getSenders().get(1)).isSameAs(Iterables.first(actors));
  }

  @Test
  public void tellAllRejectedNoReceipt() {
    final Stage stage = new Stage();
    final RejectingExecutorService rejectingExecutor = new RejectingExecutorService();
    stage.createActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return newBehavior().build();
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return rejectingExecutor;
      }
    });
    final ActorSet actors = stage.getAll();
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole observerRole = new TestRole(executorService);
    final Actor observer = stage.createActor(observerRole);
    rejectingExecutor.consumeAll();
    rejectingExecutor.setRejecting(true);
    actors.tellAll(Arrays.asList("test1", "test2"), Headers.empty(), observer);
    executorService.consumeAll();
    assertThat(observerRole.getMessages()).isEmpty();
    assertThat(observerRole.getSenders()).isEmpty();
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void tellAllSenderNPE() {
    final Stage stage = new Stage();
    final TestExecutorService executorService = new TestExecutorService();
    stage.createActor(new TestRole(executorService));
    final ActorSet actors = stage.getAll();
    actors.tellAll(Collections.emptyList(), Headers.empty(), null);
  }

  @Test
  public void tellBounce() {
    final Stage stage = new Stage();
    final TestExecutorService executorService = new TestExecutorService();
    stage.createActor(new TestRole(executorService));
    final ActorSet actors = stage.getAll();
    final TestRole observerRole = new TestRole(executorService);
    final Actor observer = stage.createActor(observerRole);
    actors.dismiss();
    actors.tell("test", new Headers().withReceiptId("test"), observer);
    executorService.consumeAll();
    assertThat(observerRole.getMessages()).hasSize(1);
    assertThat(observerRole.getMessages().get(0)).isInstanceOf(Bounce.class);
    assertThat(observerRole.getSenders()).hasSize(1);
    assertThat(observerRole.getSenders().get(0)).isSameAs(Iterables.first(actors));
  }

  @Test
  public void tellDelivery() {
    final Stage stage = new Stage();
    final TestExecutorService executorService = new TestExecutorService();
    stage.createActor(new TestRole(executorService));
    final ActorSet actors = stage.getAll();
    final TestRole observerRole = new TestRole(executorService);
    final Actor observer = stage.createActor(observerRole);
    actors.tell("test", new Headers().withReceiptId("test"), observer);
    executorService.consumeAll();
    assertThat(observerRole.getMessages()).hasSize(1);
    assertThat(observerRole.getMessages().get(0)).isInstanceOf(Delivery.class);
    assertThat(observerRole.getSenders()).hasSize(1);
    assertThat(observerRole.getSenders().get(0)).isSameAs(Iterables.first(actors));
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void tellHeadersNPE() {
    final Stage stage = new Stage();
    final TestExecutorService executorService = new TestExecutorService();
    stage.createActor(new TestRole(executorService));
    final ActorSet actors = stage.getAll();
    actors.tell(null, null, Stage.standIn());
  }

  @Test
  public void tellQuota() {
    final Stage stage = new Stage();
    final TestExecutorService executorService = new TestExecutorService();
    stage.createActor(new TestRole(executorService) {

      @Override
      public int getQuota(@NotNull final String id) {
        return 1;
      }
    });
    final ActorSet actors = stage.getAll();
    final TestRole observerRole = new TestRole(executorService);
    final Actor observer = stage.createActor(observerRole);
    actors.tell("test", Headers.empty(), observer);
    actors.tell("test", new Headers().withReceiptId("test"), observer);
    executorService.consumeAll();
    assertThat(observerRole.getMessages()).hasSize(1);
    assertThat(observerRole.getMessages().get(0)).isInstanceOf(QuotaExceeded.class);
    assertThat(observerRole.getSenders()).hasSize(1);
    assertThat(observerRole.getSenders().get(0)).isSameAs(Iterables.first(actors));
  }

  @Test
  public void tellQuotaDelivery() {
    final Stage stage = new Stage();
    final TestExecutorService executorService = new TestExecutorService();
    stage.createActor(new TestRole(executorService) {

      @Override
      public int getQuota(@NotNull final String id) {
        return 1;
      }
    });
    final ActorSet actors = stage.getAll();
    final TestRole observerRole = new TestRole(executorService);
    final Actor observer = stage.createActor(observerRole);
    actors.tell("test", new Headers().withReceiptId("test"), observer);
    actors.tell("test", new Headers().withReceiptId("test"), observer);
    executorService.consumeAll();
    assertThat(observerRole.getMessages()).hasSize(2);
    assertThat(observerRole.getMessages().get(0)).isInstanceOf(QuotaExceeded.class);
    assertThat(observerRole.getMessages().get(1)).isInstanceOf(Delivery.class);
    assertThat(observerRole.getSenders()).hasSize(2);
    assertThat(observerRole.getSenders().get(0)).isSameAs(Iterables.first(actors));
    assertThat(observerRole.getSenders().get(1)).isSameAs(Iterables.first(actors));
  }

  @Test
  public void tellQuotaNoReceipt() {
    final Stage stage = new Stage();
    final TestExecutorService executorService = new TestExecutorService();
    stage.createActor(new TestRole(executorService) {

      @Override
      public int getQuota(@NotNull final String id) {
        return 1;
      }
    });
    final ActorSet actors = stage.getAll();
    final TestRole observerRole = new TestRole(executorService);
    final Actor observer = stage.createActor(observerRole);
    actors.tell("test", Headers.empty(), observer);
    actors.tell("test", Headers.empty(), observer);
    executorService.consumeAll();
    assertThat(observerRole.getMessages()).isEmpty();
    assertThat(observerRole.getSenders()).isEmpty();
  }

  @Test
  public void tellRejected() {
    final Stage stage = new Stage();
    final RejectingExecutorService rejectingExecutor = new RejectingExecutorService();
    stage.createActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return newBehavior().build();
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return rejectingExecutor;
      }
    });
    final ActorSet actors = stage.getAll();
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole observerRole = new TestRole(executorService);
    final Actor observer = stage.createActor(observerRole);
    rejectingExecutor.consumeAll();
    rejectingExecutor.setRejecting(true);
    actors.tell("test", new Headers().withReceiptId("test"), observer);
    executorService.consumeAll();
    assertThat(observerRole.getMessages()).hasSize(1);
    assertThat(observerRole.getMessages().get(0)).isInstanceOf(Bounce.class);
    assertThat(observerRole.getSenders()).hasSize(1);
    assertThat(observerRole.getSenders().get(0)).isSameAs(Iterables.first(actors));
  }

  @Test
  public void tellRejectedNoReceipt() {
    final Stage stage = new Stage();
    final RejectingExecutorService rejectingExecutor = new RejectingExecutorService();
    stage.createActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return newBehavior().build();
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return rejectingExecutor;
      }
    });
    final ActorSet actors = stage.getAll();
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole observerRole = new TestRole(executorService);
    final Actor observer = stage.createActor(observerRole);
    rejectingExecutor.consumeAll();
    rejectingExecutor.setRejecting(true);
    actors.tell("test", Headers.empty(), observer);
    executorService.consumeAll();
    assertThat(observerRole.getMessages()).isEmpty();
    assertThat(observerRole.getSenders()).isEmpty();
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void tellSenderNPE() {
    final Stage stage = new Stage();
    final TestExecutorService executorService = new TestExecutorService();
    stage.createActor(new TestRole(executorService));
    final ActorSet actors = stage.getAll();
    actors.tell(null, Headers.empty(), null);
  }

  private static class RejectingExecutorService extends TestExecutorService {

    private boolean rejecting;

    public void execute(@NotNull final Runnable runnable) {
      if (rejecting) {
        throw new RejectedExecutionException();
      }
      super.execute(runnable);
    }

    void setRejecting(final boolean rejecting) {
      this.rejecting = rejecting;
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
