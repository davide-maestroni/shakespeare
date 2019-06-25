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

import org.assertj.core.data.Offset;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

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
import dm.shakespeare.message.Receipt;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by davide-maestroni on 06/25/2019.
 */
public class AgentTest {

  @Test
  public void dismissSelf() {
    final AtomicBoolean startCalled = new AtomicBoolean();
    final AtomicBoolean stopCalled = new AtomicBoolean();
    final Actor actor = Stage.newActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return new Behavior() {

          public void onMessage(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            agent.getSelf().dismiss();
          }

          public void onStart(@NotNull final Agent agent) {
            startCalled.set(true);
          }

          public void onStop(@NotNull final Agent agent) {
            assertThat(agent.isDismissed()).isTrue();
            stopCalled.set(true);
          }
        };
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return ExecutorServices.localExecutor();
      }
    });
    actor.tell("test", null, Stage.STAND_IN);
    assertThat(startCalled.get()).isTrue();
    assertThat(stopCalled.get()).isTrue();
  }

  @Test
  public void envelop() {
    final AtomicBoolean called = new AtomicBoolean();
    final AtomicLong sent = new AtomicLong();
    final Actor actor = Stage.newActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return new AbstractBehavior() {

          public void onMessage(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            final long received = System.currentTimeMillis();
            assertThat(envelop.getSentAt()).isCloseTo(sent.get(), Offset.offset(10L));
            assertThat(envelop.getReceivedAt()).isCloseTo(received, Offset.offset(10L));
            assertThat(envelop.getHeaders().getThreadId()).isEqualTo("test");
            assertThat(envelop.toString()).isNotNull();
            called.set(true);
          }
        };
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return ExecutorServices.localExecutor();
      }
    });
    sent.set(System.currentTimeMillis());
    actor.tell("test", new Headers().withThreadId("test"), Stage.STAND_IN);
    assertThat(called.get()).isTrue();
  }

  @Test
  public void envelopOffset() {
    final AtomicBoolean called = new AtomicBoolean();
    final long sent = System.currentTimeMillis();
    final Actor actor = Stage.newActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return new AbstractBehavior() {

          public void onMessage(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            final long received = System.currentTimeMillis();
            assertThat(envelop.getSentAt()).isCloseTo(sent, Offset.offset(10L));
            assertThat(envelop.getReceivedAt()).isCloseTo(received, Offset.offset(10L));
            called.set(true);
          }
        };
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return ExecutorServices.localExecutor();
      }
    });
    actor.tell("test", new Headers().asSentAt(sent), Stage.STAND_IN);
    assertThat(called.get()).isTrue();
  }

  @Test
  public void envelopPreventReceipt() {
    final AtomicBoolean called = new AtomicBoolean();
    final Actor actor = Stage.newActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return new AbstractBehavior() {

          public void onMessage(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            envelop.preventReceipt();
            assertThat(envelop.isPreventReceipt()).isTrue();
            called.set(true);
          }
        };
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return ExecutorServices.localExecutor();
      }
    });
    final AtomicReference<Object> msg = new AtomicReference<Object>();
    final Actor observer = Stage.newActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return new AbstractBehavior() {

          public void onMessage(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            if (Receipt.isReceipt(message, "test")) {
              msg.set(message);
            }
          }
        };
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return ExecutorServices.localExecutor();
      }
    });
    actor.tell("test", new Headers().withReceiptId("test"), observer);
    assertThat(called.get()).isTrue();
    assertThat(msg.get()).isNull();
  }

  @Test
  public void executorService() {
    final AtomicBoolean called = new AtomicBoolean();
    final Actor actor = Stage.newActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return new AbstractBehavior() {

          public void onMessage(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            agent.getExecutorService().execute(new Runnable() {

              public void run() {
                called.set(true);
              }
            });
          }
        };
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return ExecutorServices.localExecutor();
      }
    });
    actor.tell("test", null, Stage.STAND_IN);
    assertThat(called.get()).isTrue();
  }

  @Test
  public void executorServiceInvokeAll() {
    final AtomicReference<Exception> exception = new AtomicReference<Exception>();
    final Actor actor = Stage.newActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return new AbstractBehavior() {

          public void onMessage(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            try {
              agent.getExecutorService().invokeAll(Collections.singleton(new Callable<Object>() {

                public Object call() {
                  return null;
                }
              }));

            } catch (Exception e) {
              exception.set(e);
            }
          }
        };
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return ExecutorServices.localExecutor();
      }
    });
    actor.tell("test", null, Stage.STAND_IN);
    assertThat(exception.get()).isExactlyInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  public void executorServiceInvokeAllTimeout() {
    final AtomicReference<Exception> exception = new AtomicReference<Exception>();
    final Actor actor = Stage.newActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return new AbstractBehavior() {

          public void onMessage(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            try {
              agent.getExecutorService().invokeAll(Collections.singleton(new Callable<Object>() {

                public Object call() {
                  return null;
                }
              }), 100, TimeUnit.MILLISECONDS);

            } catch (Exception e) {
              exception.set(e);
            }
          }
        };
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return ExecutorServices.localExecutor();
      }
    });
    actor.tell("test", null, Stage.STAND_IN);
    assertThat(exception.get()).isExactlyInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  public void executorServiceInvokeAny() {
    final AtomicReference<Exception> exception = new AtomicReference<Exception>();
    final Actor actor = Stage.newActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return new AbstractBehavior() {

          public void onMessage(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            try {
              agent.getExecutorService().invokeAny(Collections.singleton(new Callable<Object>() {

                public Object call() {
                  return null;
                }
              }));

            } catch (Exception e) {
              exception.set(e);
            }
          }
        };
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return ExecutorServices.localExecutor();
      }
    });
    actor.tell("test", null, Stage.STAND_IN);
    assertThat(exception.get()).isExactlyInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  public void executorServiceInvokeAnyTimeout() {
    final AtomicReference<Exception> exception = new AtomicReference<Exception>();
    final Actor actor = Stage.newActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return new AbstractBehavior() {

          public void onMessage(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            try {
              agent.getExecutorService().invokeAny(Collections.singleton(new Callable<Object>() {

                public Object call() {
                  return null;
                }
              }), 100, TimeUnit.MILLISECONDS);

            } catch (Exception e) {
              exception.set(e);
            }
          }
        };
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return ExecutorServices.localExecutor();
      }
    });
    actor.tell("test", null, Stage.STAND_IN);
    assertThat(exception.get()).isExactlyInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  public void executorServiceShutdown() {
    final AtomicBoolean called = new AtomicBoolean();
    final Actor actor = Stage.newActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return new AbstractBehavior() {

          public void onMessage(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) throws InterruptedException {
            final ExecutorService executorService = agent.getExecutorService();
            executorService.shutdown();
            assertThat(executorService.isShutdown()).isTrue();
            assertThat(executorService.awaitTermination(100, TimeUnit.MILLISECONDS)).isTrue();
            assertThat(executorService.isTerminated()).isTrue();
            called.set(true);
          }
        };
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return ExecutorServices.newTrampolineExecutor();
      }
    });
    actor.tell("test", null, Stage.STAND_IN);
    assertThat(called.get()).isTrue();
  }

  @Test
  public void executorServiceShutdownNow() {
    final AtomicBoolean called = new AtomicBoolean();
    final Actor actor = Stage.newActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return new AbstractBehavior() {

          public void onMessage(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) throws InterruptedException {
            final ExecutorService executorService = agent.getExecutorService();
            assertThat(executorService.shutdownNow()).isEmpty();
            assertThat(executorService.isShutdown()).isTrue();
            assertThat(executorService.awaitTermination(100, TimeUnit.MILLISECONDS)).isTrue();
            assertThat(executorService.isTerminated()).isTrue();
            called.set(true);
          }
        };
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return ExecutorServices.newTrampolineExecutor();
      }
    });
    actor.tell("test", null, Stage.STAND_IN);
    assertThat(called.get()).isTrue();
  }

  @Test
  public void executorServiceSubmitCallable() {
    final AtomicBoolean called = new AtomicBoolean();
    final Actor actor = Stage.newActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return new AbstractBehavior() {

          public void onMessage(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            agent.getExecutorService().submit(new Callable<Object>() {

              public Object call() {
                called.set(true);
                return null;
              }
            });
          }
        };
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return ExecutorServices.localExecutor();
      }
    });
    actor.tell("test", null, Stage.STAND_IN);
    assertThat(called.get()).isTrue();
  }

  @Test
  public void executorServiceSubmitRunnable() {
    final AtomicBoolean called = new AtomicBoolean();
    final Actor actor = Stage.newActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return new AbstractBehavior() {

          public void onMessage(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            agent.getExecutorService().submit(new Runnable() {

              public void run() {
                called.set(true);
              }
            });
          }
        };
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return ExecutorServices.localExecutor();
      }
    });
    actor.tell("test", null, Stage.STAND_IN);
    assertThat(called.get()).isTrue();
  }

  @Test
  public void executorServiceSubmitRunnableResult() {
    final AtomicBoolean called = new AtomicBoolean();
    final Actor actor = Stage.newActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return new AbstractBehavior() {

          public void onMessage(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            agent.getExecutorService().submit(new Runnable() {

              public void run() {
                called.set(true);
              }
            }, null);
          }
        };
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return ExecutorServices.localExecutor();
      }
    });
    actor.tell("test", null, Stage.STAND_IN);
    assertThat(called.get()).isTrue();
  }

  @Test
  public void getSelf() {
    final AtomicReference<Actor> actor = new AtomicReference<Actor>();
    actor.set(Stage.newActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return new AbstractBehavior() {

          public void onMessage(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            assertThat(agent.getSelf()).isEqualTo(actor.get());
          }
        };
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return ExecutorServices.localExecutor();
      }
    }));
    actor.get().tell("test", null, Stage.STAND_IN);
  }

  @Test
  public void messageFailure() {
    final Actor actor = Stage.newActor(new Role() {

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
    final AtomicReference<Object> msg = new AtomicReference<Object>();
    final Actor observer = Stage.newActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return new AbstractBehavior() {

          public void onMessage(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            if (Receipt.isReceipt(message, "test")) {
              msg.set(message);
            }
          }
        };
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return ExecutorServices.localExecutor();
      }
    });
    actor.tell("test", new Headers().withReceiptId("test"), observer);
    assertThat(msg.get()).isExactlyInstanceOf(Failure.class);
    assertThat(((Failure) msg.get()).getCause()).isExactlyInstanceOf(
        IndexOutOfBoundsException.class);
    actor.tell("test", new Headers().withReceiptId("test"), observer);
    assertThat(msg.get()).isExactlyInstanceOf(Bounce.class);
  }

  @Test
  public void restartSelf() {
    final AtomicBoolean startCalled = new AtomicBoolean();
    final AtomicBoolean stopCalled = new AtomicBoolean();
    final Actor actor = Stage.newActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return new Behavior() {

          public void onMessage(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            agent.restartSelf();
          }

          public void onStart(@NotNull final Agent agent) {
            startCalled.set(true);
          }

          public void onStop(@NotNull final Agent agent) {
            assertThat(agent.isDismissed()).isFalse();
            stopCalled.set(true);
          }
        };
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return ExecutorServices.localExecutor();
      }
    });
    actor.tell("test", null, Stage.STAND_IN);
    assertThat(startCalled.get()).isTrue();
    assertThat(stopCalled.get()).isTrue();
  }

  @Test
  public void scheduledExecutorService() throws InterruptedException {
    final AtomicBoolean called = new AtomicBoolean();
    final Actor actor = Stage.newActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return new AbstractBehavior() {

          public void onMessage(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            agent.getScheduledExecutorService().schedule(new Runnable() {

              public void run() {
                called.set(true);
              }
            }, 100, TimeUnit.MILLISECONDS);
          }
        };
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return ExecutorServices.localExecutor();
      }
    });
    actor.tell("test", null, Stage.STAND_IN);
    Thread.sleep(1000);
    assertThat(called.get()).isTrue();
  }

  @Test
  public void scheduledExecutorServiceCancel() throws InterruptedException {
    final AtomicBoolean called = new AtomicBoolean();
    final Actor actor = Stage.newActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return new AbstractBehavior() {

          public void onMessage(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            agent.getScheduledExecutorService().schedule(new Runnable() {

              public void run() {
                called.set(true);
              }
            }, 1000, TimeUnit.MILLISECONDS);
          }
        };
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return ExecutorServices.localExecutor();
      }
    });
    actor.tell("test", null, Stage.STAND_IN);
    actor.dismiss();
    Thread.sleep(2000);
    assertThat(called.get()).isFalse();
  }

  @Test
  public void setBehavior() {
    final AtomicBoolean called = new AtomicBoolean();
    final Actor actor = Stage.newActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return new AbstractBehavior() {

          public void onMessage(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            called.set(false);
          }

          @Override
          public void onStart(@NotNull final Agent agent) {
            agent.setBehavior(new AbstractBehavior() {

              public void onMessage(final Object message, @NotNull final Envelop envelop,
                  @NotNull final Agent agent) {
                called.set(true);
              }
            });
          }
        };
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return ExecutorServices.localExecutor();
      }
    });
    actor.tell("test", null, Stage.STAND_IN);
    assertThat(called.get()).isTrue();
  }

  @Test
  public void startFailure() {
    final Actor actor = Stage.newActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return new AbstractBehavior() {

          public void onMessage(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
          }

          @Override
          public void onStart(@NotNull final Agent agent) {
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
    final AtomicReference<Object> msg = new AtomicReference<Object>();
    final Actor observer = Stage.newActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return new AbstractBehavior() {

          public void onMessage(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            if (Receipt.isReceipt(message, "test")) {
              msg.set(message);
            }
          }
        };
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return ExecutorServices.localExecutor();
      }
    });
    actor.tell("test", new Headers().withReceiptId("test"), observer);
    assertThat(msg.get()).isExactlyInstanceOf(Bounce.class);
  }

  @Test
  public void stopFailure() {
    final Actor actor = Stage.newActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return new AbstractBehavior() {

          public void onMessage(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            agent.restartSelf();
          }

          @Override
          public void onStop(@NotNull final Agent agent) {
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
    final AtomicReference<Object> msg = new AtomicReference<Object>();
    final Actor observer = Stage.newActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return new AbstractBehavior() {

          public void onMessage(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            if (Receipt.isReceipt(message, "test")) {
              msg.set(message);
            }
          }
        };
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return ExecutorServices.localExecutor();
      }
    });
    actor.tell("test", new Headers().withReceiptId("test"), observer);
    assertThat(msg.get()).isExactlyInstanceOf(Delivery.class);
    actor.tell("test", new Headers().withReceiptId("test"), observer);
    assertThat(msg.get()).isExactlyInstanceOf(Bounce.class);
  }
}
