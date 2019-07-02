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

import java.lang.reflect.Constructor;
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
import dm.shakespeare.message.Bounce;
import dm.shakespeare.message.Delivery;
import dm.shakespeare.message.Failure;
import dm.shakespeare.template.behavior.SupervisedBehavior.SupervisedFailure;
import dm.shakespeare.template.behavior.SupervisedBehavior.SupervisedRecovery;
import dm.shakespeare.template.behavior.SupervisedBehavior.SupervisedRecovery.RecoveryType;
import dm.shakespeare.template.behavior.SupervisedBehavior.SupervisedSignal;
import dm.shakespeare.template.util.Reflections;
import dm.shakespeare.test.concurrent.TestExecutorService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SupervisedRole} unit tests.
 */
public class SupervisedRoleTest {

  @NotNull
  private static SupervisedRecovery newRecovery(@NotNull final String failureId,
      @NotNull final RecoveryType recoveryType) throws Exception {
    final Constructor<?> constructor = Reflections.makeAccessible(
        SupervisedRecovery.class.getDeclaredConstructor(String.class, RecoveryType.class));
    return (SupervisedRecovery) constructor.newInstance(failureId, recoveryType);
  }

  @Test
  public void dismissWhileFailed() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = Stage.back().createActor(new SupervisedRole(testRole));
    final TestRole supervisorRole = new TestRole(executorService);
    final Actor supervisor = Stage.back().createActor(supervisorRole);
    actor.tell(SupervisedSignal.SUPERVISE, Headers.EMPTY, supervisor);
    actor.tell("fail", Headers.EMPTY.withReceiptId("test"), Stage.standIn());
    actor.dismissLazy();
    executorService.consumeAll();
    assertThat(testRole.getMessages()).isEmpty();
    assertThat(supervisorRole.getMessages()).hasSize(1);
    assertThat(supervisorRole.getMessages().get(0)).isExactlyInstanceOf(SupervisedFailure.class);
  }

  @Test
  public void recoveryInvalid() throws Exception {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = Stage.back().createActor(new SupervisedRole(testRole));
    final TestRole supervisorRole = new TestRole(executorService);
    final Actor supervisor = Stage.back().createActor(supervisorRole);
    actor.tell(SupervisedSignal.SUPERVISE, Headers.EMPTY, supervisor);
    actor.tell("fail", Headers.EMPTY.withReceiptId("test"), Stage.standIn());
    executorService.consumeAll();
    assertThat(testRole.getMessages()).isEmpty();
    assertThat(supervisorRole.getMessages()).hasSize(1);
    assertThat(supervisorRole.getMessages().get(0)).isExactlyInstanceOf(SupervisedFailure.class);
    supervisorRole.getMessages().clear();
    actor.tell(newRecovery("test", RecoveryType.DISMISS), Headers.EMPTY.withReceiptId("test"),
        Stage.back().createActor(supervisorRole));
    executorService.consumeAll();
    assertThat(testRole.getMessages()).isEmpty();
    assertThat(supervisorRole.getMessages()).hasSize(1);
    assertThat(supervisorRole.getMessages().get(0)).isExactlyInstanceOf(Failure.class);
  }

  @Test
  public void recoveryInvalidId() throws Exception {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = Stage.back().createActor(new SupervisedRole(testRole));
    final TestRole supervisorRole = new TestRole(executorService);
    final Actor supervisor = Stage.back().createActor(supervisorRole);
    actor.tell(SupervisedSignal.SUPERVISE, Headers.EMPTY, supervisor);
    actor.tell("fail", Headers.EMPTY.withReceiptId("test"), Stage.standIn());
    executorService.consumeAll();
    assertThat(testRole.getMessages()).isEmpty();
    assertThat(supervisorRole.getMessages()).hasSize(1);
    assertThat(supervisorRole.getMessages().get(0)).isExactlyInstanceOf(SupervisedFailure.class);
    supervisorRole.getMessages().clear();
    actor.tell(newRecovery("test", RecoveryType.DISMISS), Headers.EMPTY.withReceiptId("test"),
        supervisor);
    executorService.consumeAll();
    assertThat(testRole.getMessages()).isEmpty();
    assertThat(supervisorRole.getMessages()).hasSize(1);
    assertThat(supervisorRole.getMessages().get(0)).isExactlyInstanceOf(Failure.class);
  }

  @Test
  public void recoveryInvalidNotFailure() throws Exception {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = Stage.back().createActor(new SupervisedRole(testRole));
    final TestRole supervisorRole = new TestRole(executorService);
    final Actor supervisor = Stage.back().createActor(supervisorRole);
    actor.tell(SupervisedSignal.SUPERVISE, Headers.EMPTY, supervisor);
    actor.tell(newRecovery("TEST", RecoveryType.DISMISS), Headers.EMPTY.withReceiptId("test"),
        actor);
    executorService.consumeAll();
    assertThat(testRole.getMessages()).hasSize(1);
    assertThat(testRole.getMessages().get(0)).isExactlyInstanceOf(Failure.class);
    assertThat(supervisorRole.getMessages()).isEmpty();
  }

  @Test
  public void recoveryNotFailure() throws Exception {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = Stage.back().createActor(new SupervisedRole(testRole));
    final TestRole supervisorRole = new TestRole(executorService);
    final Actor supervisor = Stage.back().createActor(supervisorRole);
    actor.tell(SupervisedSignal.SUPERVISE, Headers.EMPTY, supervisor);
    actor.tell(newRecovery("test", RecoveryType.RESUME), Headers.EMPTY.withReceiptId("test"),
        supervisor);
    executorService.consumeAll();
    assertThat(testRole.getMessages()).isEmpty();
    assertThat(supervisorRole.getMessages()).hasSize(1);
    assertThat(supervisorRole.getMessages().get(0)).isExactlyInstanceOf(Delivery.class);
  }

  @Test
  public void replace() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = Stage.back().createActor(new SupervisedRole(testRole));
    final TestRole supervisorRole = new TestRole(executorService);
    actor.tell(SupervisedSignal.SUPERVISE, Headers.EMPTY, Stage.back().createActor(supervisorRole));
    actor.tell(SupervisedSignal.SUPERVISE, Headers.EMPTY, Stage.back().createActor(supervisorRole));
    executorService.consumeAll();
    assertThat(testRole.getMessages()).isEmpty();
    assertThat(supervisorRole.getMessages()).containsExactly(SupervisedSignal.REPLACE_SUPERVISOR);
  }

  @Test
  public void replaceWhileFailed() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = Stage.back().createActor(new SupervisedRole(testRole));
    final TestRole supervisorRole = new TestRole(executorService);
    actor.tell(SupervisedSignal.SUPERVISE, Headers.EMPTY, Stage.back().createActor(supervisorRole));
    actor.tell("fail", Headers.EMPTY.withReceiptId("test"), Stage.standIn());
    actor.tell(SupervisedSignal.SUPERVISE, Headers.EMPTY, Stage.back().createActor(supervisorRole));
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
    final Actor actor = Stage.back().createActor(new SupervisedRole(testRole));
    actor.tell(SupervisedSignal.SUPERVISE, Headers.EMPTY, Stage.standIn());
    executorService.consumeAll();
    assertThat(testRole.getMessages()).isEmpty();
  }

  @Test
  public void superviseSelf() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = Stage.back().createActor(new SupervisedRole(testRole));
    actor.tell(SupervisedSignal.SUPERVISE, Headers.EMPTY.withReceiptId("test"), actor);
    executorService.consumeAll();
    assertThat(testRole.getMessages()).hasSize(1);
    assertThat(testRole.getMessages().get(0)).isExactlyInstanceOf(Failure.class);
  }

  @Test
  public void superviseSelfWhileFailed() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = Stage.back().createActor(new SupervisedRole(testRole));
    final TestRole supervisorRole = new TestRole(executorService);
    actor.tell(SupervisedSignal.SUPERVISE, Headers.EMPTY.withReceiptId("test"),
        Stage.back().createActor(supervisorRole));
    actor.tell("fail", Headers.EMPTY.withReceiptId("test"), Stage.standIn());
    actor.tell(SupervisedSignal.SUPERVISE, Headers.EMPTY.withReceiptId("test"), actor);
    executorService.consumeAll();
    assertThat(testRole.getMessages()).isEmpty();
    assertThat(supervisorRole.getMessages()).hasSize(2);
    assertThat(supervisorRole.getMessages().get(0)).isExactlyInstanceOf(Delivery.class);
    assertThat(supervisorRole.getMessages().get(1)).isExactlyInstanceOf(SupervisedFailure.class);
  }

  @Test
  public void supervisedBounce() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = Stage.back().createActor(new SupervisedRole(testRole));
    final Actor supervisor = Stage.back().createActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return new AbstractBehavior() {

          public void onMessage(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            throw new IndexOutOfBoundsException();
          }
        };
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return ExecutorServices.localExecutor();
      }
    });
    actor.tell(SupervisedSignal.SUPERVISE, Headers.EMPTY, supervisor);
    actor.tell("fail", Headers.EMPTY.withReceiptId("test"), Stage.standIn());
    final TestRole otherRole = new TestRole(executorService);
    actor.tell("test", Headers.EMPTY.withReceiptId("test"), Stage.back().createActor(otherRole));
    executorService.consumeAll();
    assertThat(testRole.getMessages()).isEmpty();
    assertThat(otherRole.getMessages()).hasSize(1);
    assertThat(otherRole.getMessages().get(0)).isExactlyInstanceOf(Bounce.class);
  }

  @Test
  public void supervisedDeadLetter() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = Stage.back().createActor(new SupervisedRole(testRole));
    final TestRole supervisorRole = new TestRole(executorService);
    final Actor supervisor = Stage.back().createActor(supervisorRole);
    actor.tell(SupervisedSignal.SUPERVISE, Headers.EMPTY, supervisor);
    actor.tell("fail", Headers.EMPTY.withReceiptId("test"), Stage.standIn());
    supervisor.dismiss();
    final TestRole otherRole = new TestRole(executorService);
    actor.tell("test", Headers.EMPTY.withReceiptId("test"), Stage.back().createActor(otherRole));
    executorService.consumeAll();
    assertThat(testRole.getMessages()).isEmpty();
    assertThat(supervisorRole.getMessages()).isEmpty();
    assertThat(otherRole.getMessages()).hasSize(1);
    assertThat(otherRole.getMessages().get(0)).isExactlyInstanceOf(Bounce.class);
  }

  @Test
  public void supervisedDismiss() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = Stage.back().createActor(new SupervisedRole(testRole));
    actor.tell(SupervisedSignal.SUPERVISE, Headers.EMPTY, Stage.back().createActor(new Role() {

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
    actor.tell("fail", Headers.EMPTY.withReceiptId("test"), Stage.standIn());
    actor.tell("test", Headers.EMPTY.withReceiptId("test"), Stage.standIn());
    executorService.consumeAll();
    assertThat(testRole.getMessages()).isEmpty();
    actor.tell("test", Headers.EMPTY.withReceiptId("test"), Stage.standIn());
    executorService.consumeAll();
    assertThat(testRole.getMessages()).isEmpty();
  }

  @Test
  public void supervisedFailure() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = Stage.back().createActor(new SupervisedRole(testRole));
    final TestRole supervisorRole = new TestRole(executorService);
    actor.tell(SupervisedSignal.SUPERVISE, Headers.EMPTY, Stage.back().createActor(supervisorRole));
    actor.tell("fail", Headers.EMPTY.withReceiptId("test"), Stage.standIn());
    executorService.consumeAll();
    assertThat(testRole.getMessages()).isEmpty();
    assertThat(supervisorRole.getMessages()).hasSize(1);
    assertThat(supervisorRole.getMessages().get(0)).isExactlyInstanceOf(SupervisedFailure.class);
  }

  @Test
  public void supervisedRestart() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = Stage.back().createActor(new SupervisedRole(testRole));
    actor.tell(SupervisedSignal.SUPERVISE, Headers.EMPTY, Stage.back().createActor(new Role() {

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
    actor.tell("fail", Headers.EMPTY.withReceiptId("test"), Stage.standIn());
    actor.tell("test", Headers.EMPTY.withReceiptId("test"), Stage.standIn());
    executorService.consumeAll();
    assertThat(testRole.getMessages()).isEmpty();
    actor.tell("test", Headers.EMPTY.withReceiptId("test"), Stage.standIn());
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly("test");
  }

  @Test
  public void supervisedRestartAndResume() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = Stage.back().createActor(new SupervisedRole(testRole));
    actor.tell(SupervisedSignal.SUPERVISE, Headers.EMPTY, Stage.back().createActor(new Role() {

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
    actor.tell("fail", Headers.EMPTY.withReceiptId("test"), Stage.standIn());
    actor.tell("test", Headers.EMPTY.withReceiptId("test"), Stage.standIn());
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly("test");
    actor.tell("test", Headers.EMPTY.withReceiptId("test"), Stage.standIn());
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly("test", "test");
  }

  @Test
  public void supervisedRestartAndRetry() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = Stage.back().createActor(new SupervisedRole(testRole));
    actor.tell(SupervisedSignal.SUPERVISE, Headers.EMPTY, Stage.back().createActor(new Role() {

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
    actor.tell("fail", Headers.EMPTY.withReceiptId("test"), Stage.standIn());
    actor.tell("test", Headers.EMPTY.withReceiptId("test"), Stage.standIn());
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly("test");
    actor.tell("test", Headers.EMPTY.withReceiptId("test"), Stage.standIn());
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly("test", "test");
  }

  @Test
  public void supervisedResume() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = Stage.back().createActor(new SupervisedRole(testRole));
    actor.tell(SupervisedSignal.SUPERVISE, Headers.EMPTY, Stage.back().createActor(new Role() {

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
    actor.tell("fail", Headers.EMPTY.withReceiptId("test"), Stage.standIn());
    actor.tell("test", Headers.EMPTY.withReceiptId("test"), Stage.standIn());
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly("test");
    actor.tell("test", Headers.EMPTY.withReceiptId("test"), Stage.standIn());
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly("test", "test");
  }

  @Test
  public void supervisedRetry() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = Stage.back().createActor(new SupervisedRole(testRole));
    actor.tell(SupervisedSignal.SUPERVISE, Headers.EMPTY, Stage.back().createActor(new Role() {

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
    actor.tell("fail", Headers.EMPTY.withReceiptId("test"), Stage.standIn());
    actor.tell("test", Headers.EMPTY.withReceiptId("test"), Stage.standIn());
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly("test");
    actor.tell("test", Headers.EMPTY.withReceiptId("test"), Stage.standIn());
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly("test", "test");
  }

  @Test
  public void unsupervise() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = Stage.back().createActor(new SupervisedRole(testRole));
    final TestRole supervisorRole = new TestRole(executorService);
    final Actor supervisor = Stage.back().createActor(supervisorRole);
    actor.tell(SupervisedSignal.SUPERVISE, Headers.EMPTY, supervisor);
    actor.tell(SupervisedSignal.UNSUPERVISE, Headers.EMPTY, supervisor);
    actor.tell("fail", Headers.EMPTY.withReceiptId("test"), Stage.standIn());
    executorService.consumeAll();
    assertThat(testRole.getMessages()).isEmpty();
    assertThat(supervisorRole.getMessages()).isEmpty();
  }

  @Test
  public void unsuperviseInvalid() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = Stage.back().createActor(new SupervisedRole(testRole));
    final TestRole supervisorRole = new TestRole(executorService);
    final Actor supervisor = Stage.back().createActor(supervisorRole);
    actor.tell(SupervisedSignal.SUPERVISE, Headers.EMPTY, supervisor);
    actor.tell(SupervisedSignal.UNSUPERVISE, Headers.EMPTY.withReceiptId("test"), actor);
    executorService.consumeAll();
    assertThat(testRole.getMessages()).hasSize(1);
    assertThat(testRole.getMessages().get(0)).isExactlyInstanceOf(Failure.class);
    assertThat(supervisorRole.getMessages()).isEmpty();
  }

  @Test
  public void unsuperviseInvalidWhileFailed() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = Stage.back().createActor(new SupervisedRole(testRole));
    final TestRole supervisorRole = new TestRole(executorService);
    final Actor supervisor = Stage.back().createActor(supervisorRole);
    actor.tell(SupervisedSignal.SUPERVISE, Headers.EMPTY, supervisor);
    actor.tell("fail", Headers.EMPTY.withReceiptId("test"), Stage.standIn());
    final TestRole otherRole = new TestRole(executorService);
    actor.tell(SupervisedSignal.UNSUPERVISE, Headers.EMPTY.withReceiptId("test"),
        Stage.back().createActor(otherRole));
    executorService.consumeAll();
    assertThat(testRole.getMessages()).isEmpty();
    assertThat(otherRole.getMessages()).hasSize(1);
    assertThat(otherRole.getMessages().get(0)).isExactlyInstanceOf(Failure.class);
    assertThat(supervisorRole.getMessages()).hasSize(1);
    assertThat(supervisorRole.getMessages().get(0)).isExactlyInstanceOf(SupervisedFailure.class);
  }

  @Test
  public void unsuperviseWhileFailed() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = Stage.back().createActor(new SupervisedRole(testRole));
    final TestRole supervisorRole = new TestRole(executorService);
    final Actor supervisor = Stage.back().createActor(supervisorRole);
    actor.tell(SupervisedSignal.SUPERVISE, Headers.EMPTY, supervisor);
    actor.tell("fail", Headers.EMPTY.withReceiptId("test"), Stage.standIn());
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
