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

package dm.shakespeare.actor;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import dm.shakespeare.Stage;
import dm.shakespeare.actor.BehaviorBuilder.Handler;
import dm.shakespeare.function.Mapper;
import dm.shakespeare.function.Observer;
import dm.shakespeare.test.concurrent.TestExecutorService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link Role} unit tests.
 */
public class RoleTest {

  @Test
  public void accept() {
    final AtomicReference<String> msg = new AtomicReference<String>();
    final TestExecutorService executorService = new TestExecutorService();
    final Handler<String> handler = Role.accept(new Observer<String>() {

      public void accept(final String value) {
        msg.set(value);
      }
    });
    final Actor actor = Stage.back().createActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return newBehavior().onMessage(String.class, handler).build();
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return executorService;
      }
    });
    final TestRole observerRole = new TestRole(executorService);
    final Actor observer = Stage.back().createActor(observerRole);
    actor.tell("test", Headers.empty(), observer);
    executorService.consumeAll();
    assertThat(observerRole.getMessages()).isEmpty();
    assertThat(observerRole.getSenders()).isEmpty();
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings({"ConstantConditions"})
  public void acceptNPE() {
    Role.accept(null);
  }

  @Test
  public void apply() {
    final TestExecutorService executorService = new TestExecutorService();
    final Handler<String> handler = Role.apply(new Mapper<String, String>() {

      public String apply(final String value) {
        return value.toUpperCase(Locale.ENGLISH);
      }
    });
    final Actor actor = Stage.back().createActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return newBehavior().onMessage(String.class, handler).build();
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return executorService;
      }
    });
    final TestRole observerRole = new TestRole(executorService);
    final Actor observer = Stage.back().createActor(observerRole);
    actor.tell("test", Headers.empty(), observer);
    executorService.consumeAll();
    assertThat(observerRole.getMessages()).containsExactly("TEST");
    assertThat(observerRole.getSenders()).containsExactly(actor);
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings({"ConstantConditions"})
  public void applyNPE() {
    Role.apply(null);
  }

  @Test
  public void from() throws InterruptedException {
    final CountDownLatch latch = new CountDownLatch(1);
    final Actor actor = Stage.back().createActor(Role.from(new Mapper<String, Behavior>() {

      public Behavior apply(final String id) {
        return new AbstractBehavior() {

          public void onMessage(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            latch.countDown();
          }
        };
      }
    }));
    actor.tell("test", Headers.empty(), Stage.standIn());
    assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings({"ConstantConditions", "ResultOfMethodCallIgnored"})
  public void fromNPE() {
    Role.from(null);
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
