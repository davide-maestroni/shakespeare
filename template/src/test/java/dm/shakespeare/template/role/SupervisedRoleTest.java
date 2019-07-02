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
import dm.shakespeare.template.behavior.SupervisedBehavior.SupervisedFailure;
import dm.shakespeare.template.behavior.SupervisedBehavior.SupervisedRecovery.RecoveryType;
import dm.shakespeare.template.behavior.SupervisedBehavior.SupervisedSignal;
import dm.shakespeare.test.concurrent.TestExecutorService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SupervisedRole} unit tests.
 */
public class SupervisedRoleTest {

  @Test
  public void dismissWhileFailed() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = Stage.newActor(new SupervisedRole(testRole));
    final TestRole supervisorRole = new TestRole(executorService);
    final Actor supervisor = Stage.newActor(supervisorRole);
    actor.tell(SupervisedSignal.SUPERVISE, Headers.EMPTY, supervisor);
    actor.tell("fail", Headers.EMPTY.withReceiptId("test"), Stage.STAND_IN);
    actor.dismissLazy();
    executorService.consumeAll();
    assertThat(testRole.getMessages()).isEmpty();
    assertThat(supervisorRole.getMessages()).hasSize(1);
    assertThat(supervisorRole.getMessages().get(0)).isExactlyInstanceOf(SupervisedFailure.class);
  }

  @Test
  public void replace() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = Stage.newActor(new SupervisedRole(testRole));
    final TestRole supervisorRole = new TestRole(executorService);
    actor.tell(SupervisedSignal.SUPERVISE, Headers.EMPTY, Stage.newActor(supervisorRole));
    actor.tell(SupervisedSignal.SUPERVISE, Headers.EMPTY, Stage.newActor(supervisorRole));
    executorService.consumeAll();
    assertThat(testRole.getMessages()).isEmpty();
    assertThat(supervisorRole.getMessages()).containsExactly(SupervisedSignal.REPLACE_SUPERVISOR);
  }

  @Test
  public void replaceWhileFailed() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = Stage.newActor(new SupervisedRole(testRole));
    final TestRole supervisorRole = new TestRole(executorService);
    actor.tell(SupervisedSignal.SUPERVISE, Headers.EMPTY, Stage.newActor(supervisorRole));
    actor.tell("fail", Headers.EMPTY.withReceiptId("test"), Stage.STAND_IN);
    actor.tell(SupervisedSignal.SUPERVISE, Headers.EMPTY, Stage.newActor(supervisorRole));
    executorService.consumeAll();
    assertThat(testRole.getMessages()).isEmpty();
    assertThat(supervisorRole.getMessages()).hasSize(3);
    assertThat(supervisorRole.getMessages().get(0)).isExactlyInstanceOf(SupervisedFailure.class);
    assertThat(supervisorRole.getMessages().get(1)).isEqualTo(SupervisedSignal.REPLACE_SUPERVISOR);
    assertThat(supervisorRole.getMessages().get(0)).isExactlyInstanceOf(SupervisedFailure.class);
  }

  @Test
  public void supervise() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = Stage.newActor(new SupervisedRole(testRole));
    actor.tell(SupervisedSignal.SUPERVISE, Headers.EMPTY, Stage.STAND_IN);
    executorService.consumeAll();
    assertThat(testRole.getMessages()).isEmpty();
  }

  @Test
  public void supervisedDismiss() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = Stage.newActor(new SupervisedRole(testRole));
    actor.tell(SupervisedSignal.SUPERVISE, Headers.EMPTY, Stage.newActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return new AbstractBehavior() {

          public void onMessage(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            if (message instanceof SupervisedFailure) {
              envelop.getSender()
                  .tell(((SupervisedFailure) message).recover(RecoveryType.DISMISS),
                      envelop.getHeaders().threadOnly(), agent.getSelf());
            }
          }
        };
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return ExecutorServices.localExecutor();
      }
    }));
    actor.tell("fail", Headers.EMPTY.withReceiptId("test"), Stage.STAND_IN);
    actor.tell("test", Headers.EMPTY.withReceiptId("test"), Stage.STAND_IN);
    executorService.consumeAll();
    assertThat(testRole.getMessages()).isEmpty();
    actor.tell("test", Headers.EMPTY.withReceiptId("test"), Stage.STAND_IN);
    executorService.consumeAll();
    assertThat(testRole.getMessages()).isEmpty();
  }

  @Test
  public void supervisedFailure() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = Stage.newActor(new SupervisedRole(testRole));
    final TestRole supervisorRole = new TestRole(executorService);
    actor.tell(SupervisedSignal.SUPERVISE, Headers.EMPTY, Stage.newActor(supervisorRole));
    actor.tell("fail", Headers.EMPTY.withReceiptId("test"), Stage.STAND_IN);
    executorService.consumeAll();
    assertThat(testRole.getMessages()).isEmpty();
    assertThat(supervisorRole.getMessages()).hasSize(1);
    assertThat(supervisorRole.getMessages().get(0)).isExactlyInstanceOf(SupervisedFailure.class);
  }

  @Test
  public void supervisedRestart() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = Stage.newActor(new SupervisedRole(testRole));
    actor.tell(SupervisedSignal.SUPERVISE, Headers.EMPTY, Stage.newActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return new AbstractBehavior() {

          public void onMessage(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            if (message instanceof SupervisedFailure) {
              envelop.getSender()
                  .tell(((SupervisedFailure) message).recover(RecoveryType.RESTART),
                      envelop.getHeaders().threadOnly(), agent.getSelf());
            }
          }
        };
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return ExecutorServices.localExecutor();
      }
    }));
    actor.tell("fail", Headers.EMPTY.withReceiptId("test"), Stage.STAND_IN);
    actor.tell("test", Headers.EMPTY.withReceiptId("test"), Stage.STAND_IN);
    executorService.consumeAll();
    assertThat(testRole.getMessages()).isEmpty();
    actor.tell("test", Headers.EMPTY.withReceiptId("test"), Stage.STAND_IN);
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly("test");
  }

  @Test
  public void supervisedRestartAndResume() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = Stage.newActor(new SupervisedRole(testRole));
    actor.tell(SupervisedSignal.SUPERVISE, Headers.EMPTY, Stage.newActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return new AbstractBehavior() {

          public void onMessage(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            if (message instanceof SupervisedFailure) {
              envelop.getSender()
                  .tell(((SupervisedFailure) message).recover(RecoveryType.RESTART_AND_RESUME),
                      envelop.getHeaders().threadOnly(), agent.getSelf());
            }
          }
        };
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return ExecutorServices.localExecutor();
      }
    }));
    actor.tell("fail", Headers.EMPTY.withReceiptId("test"), Stage.STAND_IN);
    actor.tell("test", Headers.EMPTY.withReceiptId("test"), Stage.STAND_IN);
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly("test");
    actor.tell("test", Headers.EMPTY.withReceiptId("test"), Stage.STAND_IN);
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly("test", "test");
  }

  @Test
  public void supervisedRestartAndRetry() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = Stage.newActor(new SupervisedRole(testRole));
    actor.tell(SupervisedSignal.SUPERVISE, Headers.EMPTY, Stage.newActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return new AbstractBehavior() {

          private boolean failed = false;

          public void onMessage(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            if (message instanceof SupervisedFailure) {
              if (failed) {
                envelop.getSender()
                    .tell(((SupervisedFailure) message).recover(RecoveryType.RESTART_AND_RESUME),
                        envelop.getHeaders().threadOnly(), agent.getSelf());

              } else {
                failed = true;
                envelop.getSender()
                    .tell(((SupervisedFailure) message).recover(RecoveryType.RESTART_AND_RETRY),
                        envelop.getHeaders().threadOnly(), agent.getSelf());
              }
            }
          }
        };
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return ExecutorServices.localExecutor();
      }
    }));
    actor.tell("fail", Headers.EMPTY.withReceiptId("test"), Stage.STAND_IN);
    actor.tell("test", Headers.EMPTY.withReceiptId("test"), Stage.STAND_IN);
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly("test");
    actor.tell("test", Headers.EMPTY.withReceiptId("test"), Stage.STAND_IN);
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly("test", "test");
  }

  @Test
  public void supervisedResume() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = Stage.newActor(new SupervisedRole(testRole));
    actor.tell(SupervisedSignal.SUPERVISE, Headers.EMPTY, Stage.newActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return new AbstractBehavior() {

          public void onMessage(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            if (message instanceof SupervisedFailure) {
              envelop.getSender()
                  .tell(((SupervisedFailure) message).recover(RecoveryType.RESUME),
                      envelop.getHeaders().threadOnly(), agent.getSelf());
            }
          }
        };
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return ExecutorServices.localExecutor();
      }
    }));
    actor.tell("fail", Headers.EMPTY.withReceiptId("test"), Stage.STAND_IN);
    actor.tell("test", Headers.EMPTY.withReceiptId("test"), Stage.STAND_IN);
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly("test");
    actor.tell("test", Headers.EMPTY.withReceiptId("test"), Stage.STAND_IN);
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly("test", "test");
  }

  @Test
  public void supervisedRetry() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = Stage.newActor(new SupervisedRole(testRole));
    actor.tell(SupervisedSignal.SUPERVISE, Headers.EMPTY, Stage.newActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return new AbstractBehavior() {

          private boolean failed = false;

          public void onMessage(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            if (message instanceof SupervisedFailure) {
              if (failed) {
                envelop.getSender()
                    .tell(((SupervisedFailure) message).recover(RecoveryType.RESUME),
                        envelop.getHeaders().threadOnly(), agent.getSelf());

              } else {
                failed = true;
                envelop.getSender()
                    .tell(((SupervisedFailure) message).recover(RecoveryType.RETRY),
                        envelop.getHeaders().threadOnly(), agent.getSelf());
              }
            }
          }
        };
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return ExecutorServices.localExecutor();
      }
    }));
    actor.tell("fail", Headers.EMPTY.withReceiptId("test"), Stage.STAND_IN);
    actor.tell("test", Headers.EMPTY.withReceiptId("test"), Stage.STAND_IN);
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly("test");
    actor.tell("test", Headers.EMPTY.withReceiptId("test"), Stage.STAND_IN);
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly("test", "test");
  }

  @Test
  public void unsupervise() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = Stage.newActor(new SupervisedRole(testRole));
    final TestRole supervisorRole = new TestRole(executorService);
    final Actor supervisor = Stage.newActor(supervisorRole);
    actor.tell(SupervisedSignal.SUPERVISE, Headers.EMPTY, supervisor);
    actor.tell(SupervisedSignal.UNSUPERVISE, Headers.EMPTY, supervisor);
    actor.tell("fail", Headers.EMPTY.withReceiptId("test"), Stage.STAND_IN);
    executorService.consumeAll();
    assertThat(testRole.getMessages()).isEmpty();
    assertThat(supervisorRole.getMessages()).isEmpty();
  }

  @Test
  public void unsuperviseWhileFailed() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = Stage.newActor(new SupervisedRole(testRole));
    final TestRole supervisorRole = new TestRole(executorService);
    final Actor supervisor = Stage.newActor(supervisorRole);
    actor.tell(SupervisedSignal.SUPERVISE, Headers.EMPTY, supervisor);
    actor.tell("fail", Headers.EMPTY.withReceiptId("test"), Stage.STAND_IN);
    actor.tell(SupervisedSignal.UNSUPERVISE, Headers.EMPTY, supervisor);
    executorService.consumeAll();
    assertThat(testRole.getMessages()).isEmpty();
    assertThat(supervisorRole.getMessages()).hasSize(1);
    assertThat(supervisorRole.getMessages().get(0)).isExactlyInstanceOf(SupervisedFailure.class);
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
          if ("fail".equals(message)) {
            throw new IndexOutOfBoundsException();
          }
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
}
