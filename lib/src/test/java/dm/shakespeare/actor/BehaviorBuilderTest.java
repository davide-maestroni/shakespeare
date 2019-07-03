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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;

import dm.shakespeare.Stage;
import dm.shakespeare.actor.Behavior.Agent;
import dm.shakespeare.actor.BehaviorBuilder.Handler;
import dm.shakespeare.actor.BehaviorBuilder.Matcher;
import dm.shakespeare.function.Observer;
import dm.shakespeare.function.Tester;
import dm.shakespeare.test.concurrent.TestExecutorService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link BehaviorBuilder} unit tests.
 */
public class BehaviorBuilderTest {

  @Test
  public void onAny() {
    final ArrayList<Object> messages = new ArrayList<Object>();
    final TestExecutorService executorService = new TestExecutorService();
    final Actor actor = Stage.back().createActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return newBehavior().onAny(new Handler<Object>() {

          public void handle(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            messages.add(message);
          }
        }).build();
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return executorService;
      }
    });
    actor.tellAll(Arrays.asList("test", 3), Headers.empty(), Stage.standIn());
    executorService.consumeAll();
    assertThat(messages).containsExactly("test", 3);
  }

  @Test(expected = NullPointerException.class)
  public void onAnyNPE() {
    Stage.back().createActor(new Role() {

      @NotNull
      @Override
      @SuppressWarnings("ConstantConditions")
      public Behavior getBehavior(@NotNull final String id) {
        return newBehavior().onAny(null).build();
      }
    });
  }

  @Test
  public void onAnyOrder() {
    final ArrayList<Object> messages = new ArrayList<Object>();
    final TestExecutorService executorService = new TestExecutorService();
    final Actor actor = Stage.back().createActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return newBehavior().onAny(new Handler<Object>() {

          public void handle(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            messages.add(message);
          }
        }).onAny(new Handler<Object>() {

          public void handle(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            messages.add(message.toString());
          }
        }).build();
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return executorService;
      }
    });
    actor.tellAll(Arrays.asList("test", 3), Headers.empty(), Stage.standIn());
    executorService.consumeAll();
    assertThat(messages).containsExactly("test", "test", 3, "3");
  }

  @Test
  public void onEnvelop() {
    final ArrayList<Object> messages = new ArrayList<Object>();
    final TestExecutorService executorService = new TestExecutorService();
    final Actor actor = Stage.back().createActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return newBehavior().onEnvelop(new Tester<Envelop>() {

          public boolean test(final Envelop envelop) {
            return envelop.getHeaders().getThreadId() == null;
          }
        }, new Handler<Object>() {

          public void handle(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            messages.add(message);
          }
        }).build();
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return executorService;
      }
    });
    actor.tell("test", new Headers().withThreadId("test"), Stage.standIn());
    actor.tell(3, Headers.empty(), Stage.standIn());
    executorService.consumeAll();
    assertThat(messages).containsExactly(3);
  }

  @Test(expected = NullPointerException.class)
  public void onEnvelopHandlerNPE() {
    Stage.back().createActor(new Role() {

      @NotNull
      @Override
      @SuppressWarnings("ConstantConditions")
      public Behavior getBehavior(@NotNull final String id) {
        return newBehavior().onEnvelop(new Tester<Envelop>() {

          public boolean test(final Envelop value) {
            return false;
          }
        }, null).build();
      }
    });
  }

  @Test
  public void onEnvelopOrder() {
    final ArrayList<Object> messages = new ArrayList<Object>();
    final TestExecutorService executorService = new TestExecutorService();
    final Actor actor = Stage.back().createActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return newBehavior().onEnvelop(new Tester<Envelop>() {

          public boolean test(final Envelop envelop) {
            return envelop.getHeaders().getThreadId() == null;
          }
        }, new Handler<Object>() {

          public void handle(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            messages.add(message);
          }
        }).onEnvelop(new Tester<Envelop>() {

          public boolean test(final Envelop envelop) {
            return envelop.getHeaders().getThreadId() == null;
          }
        }, new Handler<Object>() {

          public void handle(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            messages.add(message.toString());
          }
        }).build();
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return executorService;
      }
    });
    actor.tell("test", new Headers().withThreadId("test"), Stage.standIn());
    actor.tell(3, Headers.empty(), Stage.standIn());
    executorService.consumeAll();
    assertThat(messages).containsExactly(3, "3");
  }

  @Test(expected = NullPointerException.class)
  public void onEnvelopTesterNPE() {
    Stage.back().createActor(new Role() {

      @NotNull
      @Override
      @SuppressWarnings("ConstantConditions")
      public Behavior getBehavior(@NotNull final String id) {
        return newBehavior().onEnvelop(null, new Handler<Object>() {

          public void handle(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
          }
        }).build();
      }
    });
  }

  @Test
  public void onEqualTo() {
    final ArrayList<Object> messages = new ArrayList<Object>();
    final TestExecutorService executorService = new TestExecutorService();
    final Actor actor = Stage.back().createActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return newBehavior().onEqualTo("test", new Handler<Object>() {

          public void handle(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            messages.add(message);
          }
        }).build();
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return executorService;
      }
    });
    actor.tell("test", new Headers().withThreadId("test"), Stage.standIn());
    actor.tell(3, Headers.empty(), Stage.standIn());
    executorService.consumeAll();
    assertThat(messages).containsExactly("test");
  }

  @Test(expected = NullPointerException.class)
  public void onEqualToNPE() {
    Stage.back().createActor(new Role() {

      @NotNull
      @Override
      @SuppressWarnings("ConstantConditions")
      public Behavior getBehavior(@NotNull final String id) {
        return newBehavior().onEnvelop(null, null).build();
      }
    });
  }

  @Test
  public void onEqualToNull() {
    final ArrayList<Object> messages = new ArrayList<Object>();
    final TestExecutorService executorService = new TestExecutorService();
    final Actor actor = Stage.back().createActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return newBehavior().onEqualTo(null, new Handler<Object>() {

          public void handle(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            messages.add(message);
          }
        }).build();
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return executorService;
      }
    });
    actor.tell("test", new Headers().withThreadId("test"), Stage.standIn());
    actor.tell(null, Headers.empty(), Stage.standIn());
    executorService.consumeAll();
    assertThat(messages).containsExactly((String) null);
  }

  @Test
  public void onEqualToOrder() {
    final ArrayList<Object> messages = new ArrayList<Object>();
    final TestExecutorService executorService = new TestExecutorService();
    final Actor actor = Stage.back().createActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return newBehavior().onEqualTo("test", new Handler<Object>() {

          public void handle(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            messages.add(message);
          }
        }).onEqualTo("test", new Handler<Object>() {

          public void handle(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            messages.add(message.toString().toUpperCase(Locale.ENGLISH));
          }
        }).build();
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return executorService;
      }
    });
    actor.tell("test", new Headers().withThreadId("test"), Stage.standIn());
    actor.tell(3, Headers.empty(), Stage.standIn());
    executorService.consumeAll();
    assertThat(messages).containsExactly("test", "TEST");
  }

  @Test
  public void onMatch() {
    final ArrayList<Object> messages = new ArrayList<Object>();
    final TestExecutorService executorService = new TestExecutorService();
    final Actor actor = Stage.back().createActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return newBehavior().onMatch(new Matcher<Object>() {

          public boolean match(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            return "test".equals(message) && (envelop.getHeaders().getThreadId() == null);
          }
        }, new Handler<Object>() {

          public void handle(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            messages.add(message);
          }
        }).build();
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return executorService;
      }
    });
    actor.tell("test", new Headers().withThreadId("test"), Stage.standIn());
    actor.tell(3, Headers.empty(), Stage.standIn());
    executorService.consumeAll();
    assertThat(messages).isEmpty();
    actor.tell("test", Headers.empty(), Stage.standIn());
    executorService.consumeAll();
    assertThat(messages).containsExactly("test");
  }

  @Test(expected = NullPointerException.class)
  public void onMatchHandlerNPE() {
    Stage.back().createActor(new Role() {

      @NotNull
      @Override
      @SuppressWarnings("ConstantConditions")
      public Behavior getBehavior(@NotNull final String id) {
        return newBehavior().onMatch(new Matcher<Object>() {

          public boolean match(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            return false;
          }
        }, null).build();
      }
    });
  }

  @Test(expected = NullPointerException.class)
  public void onMatchMatcherNPE() {
    Stage.back().createActor(new Role() {

      @NotNull
      @Override
      @SuppressWarnings("ConstantConditions")
      public Behavior getBehavior(@NotNull final String id) {
        return newBehavior().onMatch(null, new Handler<Object>() {

          public void handle(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
          }
        }).build();
      }
    });
  }

  @Test
  public void onMatchOrder() {
    final ArrayList<Object> messages = new ArrayList<Object>();
    final TestExecutorService executorService = new TestExecutorService();
    final Actor actor = Stage.back().createActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return newBehavior().onMatch(new Matcher<Object>() {

          public boolean match(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            return "test".equals(message) && (envelop.getHeaders().getThreadId() == null);
          }
        }, new Handler<Object>() {

          public void handle(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            messages.add(message);
          }
        }).onMatch(new Matcher<Object>() {

          public boolean match(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            return "test".equals(message) && (envelop.getHeaders().getThreadId() == null);
          }
        }, new Handler<Object>() {

          public void handle(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            messages.add(message.toString().toUpperCase(Locale.ENGLISH));
          }
        }).build();
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return executorService;
      }
    });
    actor.tell("test", new Headers().withThreadId("test"), Stage.standIn());
    actor.tell(3, Headers.empty(), Stage.standIn());
    executorService.consumeAll();
    assertThat(messages).isEmpty();
    actor.tell("test", Headers.empty(), Stage.standIn());
    executorService.consumeAll();
    assertThat(messages).containsExactly("test", "TEST");
  }

  @Test
  public void onMessageClass() {
    final ArrayList<Object> messages = new ArrayList<Object>();
    final TestExecutorService executorService = new TestExecutorService();
    final Actor actor = Stage.back().createActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return newBehavior().onMessage(String.class, new Handler<Object>() {

          public void handle(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            messages.add(message);
          }
        }).build();
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return executorService;
      }
    });
    actor.tell("test", new Headers().withThreadId("test"), Stage.standIn());
    actor.tell(3, Headers.empty(), Stage.standIn());
    executorService.consumeAll();
    assertThat(messages).containsExactly("test");
  }

  @Test(expected = NullPointerException.class)
  public void onMessageClassHandlerNPE() {
    Stage.back().createActor(new Role() {

      @NotNull
      @Override
      @SuppressWarnings("ConstantConditions")
      public Behavior getBehavior(@NotNull final String id) {
        return newBehavior().onMessage(Void.class, null).build();
      }
    });
  }

  @Test(expected = NullPointerException.class)
  public void onMessageClassNPE() {
    Stage.back().createActor(new Role() {

      @NotNull
      @Override
      @SuppressWarnings("ConstantConditions")
      public Behavior getBehavior(@NotNull final String id) {
        return newBehavior().onMessage((Class<?>) null, new Handler<Object>() {

          public void handle(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
          }
        }).build();
      }
    });
  }

  @Test
  public void onMessageClassOrder() {
    final ArrayList<Object> messages = new ArrayList<Object>();
    final TestExecutorService executorService = new TestExecutorService();
    final Actor actor = Stage.back().createActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return newBehavior().onMessage(String.class, new Handler<Object>() {

          public void handle(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            messages.add(message);
          }
        }).onMessage(String.class, new Handler<Object>() {

          public void handle(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            messages.add(message.toString().toUpperCase(Locale.ENGLISH));
          }
        }).build();
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return executorService;
      }
    });
    actor.tell("test", new Headers().withThreadId("test"), Stage.standIn());
    actor.tell(3, Headers.empty(), Stage.standIn());
    executorService.consumeAll();
    assertThat(messages).containsExactly("test", "TEST");
  }

  @Test
  public void onMessageClasses() {
    final ArrayList<Object> messages = new ArrayList<Object>();
    final TestExecutorService executorService = new TestExecutorService();
    final Actor actor = Stage.back().createActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return newBehavior().onMessage(Arrays.asList(Void.class, String.class),
            new Handler<Object>() {

              public void handle(final Object message, @NotNull final Envelop envelop,
                  @NotNull final Agent agent) {
                messages.add(message);
              }
            }).build();
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return executorService;
      }
    });
    actor.tell("test", new Headers().withThreadId("test"), Stage.standIn());
    actor.tell(3, Headers.empty(), Stage.standIn());
    executorService.consumeAll();
    assertThat(messages).containsExactly("test");
  }

  @Test(expected = NullPointerException.class)
  public void onMessageClassesElementNPE() {
    Stage.back().createActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return newBehavior().onMessage(Arrays.asList(Void.class, null), new Handler<Object>() {

          public void handle(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
          }
        }).build();
      }
    });
  }

  @Test(expected = NullPointerException.class)
  public void onMessageClassesHandlerNPE() {
    Stage.back().createActor(new Role() {

      @NotNull
      @Override
      @SuppressWarnings("ConstantConditions")
      public Behavior getBehavior(@NotNull final String id) {
        return newBehavior().onMessage(Collections.singleton(Void.class), null).build();
      }
    });
  }

  @Test(expected = NullPointerException.class)
  public void onMessageClassesNPE() {
    Stage.back().createActor(new Role() {

      @NotNull
      @Override
      @SuppressWarnings("ConstantConditions")
      public Behavior getBehavior(@NotNull final String id) {
        return newBehavior().onMessage((List<Class<?>>) null, new Handler<Object>() {

          public void handle(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
          }
        }).build();
      }
    });
  }

  @Test
  public void onMessageClassesOrder() {
    final ArrayList<Object> messages = new ArrayList<Object>();
    final TestExecutorService executorService = new TestExecutorService();
    final Actor actor = Stage.back().createActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return newBehavior().onMessage(Arrays.asList(Void.class, String.class),
            new Handler<Object>() {

              public void handle(final Object message, @NotNull final Envelop envelop,
                  @NotNull final Agent agent) {
                messages.add(message);
              }
            }).onMessage(Arrays.asList(Void.class, String.class), new Handler<Object>() {

          public void handle(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            messages.add(message.toString().toUpperCase(Locale.ENGLISH));
          }
        }).build();
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return executorService;
      }
    });
    actor.tell("test", new Headers().withThreadId("test"), Stage.standIn());
    actor.tell(3, Headers.empty(), Stage.standIn());
    executorService.consumeAll();
    assertThat(messages).containsExactly("test", "TEST");
  }

  @Test
  public void onMessageTester() {
    final ArrayList<Object> messages = new ArrayList<Object>();
    final TestExecutorService executorService = new TestExecutorService();
    final Actor actor = Stage.back().createActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return newBehavior().onMessage(new Tester<Object>() {

          public boolean test(final Object value) {
            return (value instanceof String);
          }
        }, new Handler<Object>() {

          public void handle(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            messages.add(message);
          }
        }).build();
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return executorService;
      }
    });
    actor.tell("test", new Headers().withThreadId("test"), Stage.standIn());
    actor.tell(3, Headers.empty(), Stage.standIn());
    executorService.consumeAll();
    assertThat(messages).containsExactly("test");
  }

  @Test(expected = NullPointerException.class)
  public void onMessageTesterHandlerNPE() {
    Stage.back().createActor(new Role() {

      @NotNull
      @Override
      @SuppressWarnings("ConstantConditions")
      public Behavior getBehavior(@NotNull final String id) {
        return newBehavior().onMessage(new Tester<Object>() {

          public boolean test(final Object value) {
            return false;
          }
        }, null).build();
      }
    });
  }

  @Test(expected = NullPointerException.class)
  public void onMessageTesterNPE() {
    Stage.back().createActor(new Role() {

      @NotNull
      @Override
      @SuppressWarnings("ConstantConditions")
      public Behavior getBehavior(@NotNull final String id) {
        return newBehavior().onMessage((Tester<Object>) null, new Handler<Object>() {

          public void handle(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
          }
        }).build();
      }
    });
  }

  @Test
  public void onMessageTesterOrder() {
    final ArrayList<Object> messages = new ArrayList<Object>();
    final TestExecutorService executorService = new TestExecutorService();
    final Actor actor = Stage.back().createActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return newBehavior().onMessage(new Tester<Object>() {

          public boolean test(final Object value) {
            return (value instanceof String);
          }
        }, new Handler<Object>() {

          public void handle(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            messages.add(message);
          }
        }).onMessage(new Tester<Object>() {

          public boolean test(final Object value) {
            return (value instanceof String);
          }
        }, new Handler<Object>() {

          public void handle(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            messages.add(message.toString().toUpperCase(Locale.ENGLISH));
          }
        }).build();
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return executorService;
      }
    });
    actor.tell("test", new Headers().withThreadId("test"), Stage.standIn());
    actor.tell(3, Headers.empty(), Stage.standIn());
    executorService.consumeAll();
    assertThat(messages).containsExactly("test", "TEST");
  }

  @Test
  public void onNoMatch() {
    final ArrayList<Object> messages = new ArrayList<Object>();
    final ArrayList<Object> matches = new ArrayList<Object>();
    final TestExecutorService executorService = new TestExecutorService();
    final Actor actor = Stage.back().createActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return newBehavior().onNoMatch(new Handler<Object>() {

          public void handle(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            matches.add(message);
          }
        }).onMessage(String.class, new Handler<Object>() {

          public void handle(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            messages.add(message);
          }
        }).onNoMatch(new Handler<Object>() {

          public void handle(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            matches.add(message);
          }
        }).build();
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return executorService;
      }
    });
    actor.tell("test", new Headers().withThreadId("test"), Stage.standIn());
    actor.tell(3, Headers.empty(), Stage.standIn());
    executorService.consumeAll();
    assertThat(messages).containsExactly("test");
    assertThat(matches).containsExactly(3, 3);
  }

  @Test
  public void onNoMatchEmpty() {
    final ArrayList<Object> messages = new ArrayList<Object>();
    final ArrayList<Object> matches = new ArrayList<Object>();
    final TestExecutorService executorService = new TestExecutorService();
    final Actor actor = Stage.back().createActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return newBehavior().onAny(new Handler<Object>() {

          public void handle(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            messages.add(message);
          }
        }).onNoMatch(new Handler<Object>() {

          public void handle(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            matches.add(message);
          }
        }).build();
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return executorService;
      }
    });
    actor.tell("test", new Headers().withThreadId("test"), Stage.standIn());
    actor.tell(3, Headers.empty(), Stage.standIn());
    executorService.consumeAll();
    assertThat(messages).containsExactly("test", 3);
    assertThat(matches).isEmpty();
  }

  @Test(expected = NullPointerException.class)
  public void onNoMatchNPE() {
    Stage.back().createActor(new Role() {

      @NotNull
      @Override
      @SuppressWarnings("ConstantConditions")
      public Behavior getBehavior(@NotNull final String id) {
        return newBehavior().onNoMatch(null).build();
      }
    });
  }

  @Test
  public void onNoMatchOnly() {
    final ArrayList<Object> matches = new ArrayList<Object>();
    final TestExecutorService executorService = new TestExecutorService();
    final Actor actor = Stage.back().createActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return newBehavior().onNoMatch(new Handler<Object>() {

          public void handle(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            matches.add(message);
          }
        }).build();
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return executorService;
      }
    });
    actor.tell("test", new Headers().withThreadId("test"), Stage.standIn());
    actor.tell(3, Headers.empty(), Stage.standIn());
    executorService.consumeAll();
    assertThat(matches).containsExactly("test", 3);
  }

  @Test
  public void onNoMatchOnlyOrder() {
    final ArrayList<Object> matches = new ArrayList<Object>();
    final TestExecutorService executorService = new TestExecutorService();
    final Actor actor = Stage.back().createActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return newBehavior().onNoMatch(new Handler<Object>() {

          public void handle(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            matches.add(message);
          }
        }).onNoMatch(new Handler<Object>() {

          public void handle(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            matches.add(message.toString());
          }
        }).build();
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return executorService;
      }
    });
    actor.tell("test", new Headers().withThreadId("test"), Stage.standIn());
    actor.tell(3, Headers.empty(), Stage.standIn());
    executorService.consumeAll();
    assertThat(matches).containsExactly("test", "test", 3, "3");
  }

  @Test
  public void onNoMatchOrder() {
    final ArrayList<Object> messages = new ArrayList<Object>();
    final ArrayList<Object> matches = new ArrayList<Object>();
    final TestExecutorService executorService = new TestExecutorService();
    final Actor actor = Stage.back().createActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return newBehavior().onMessage(String.class, new Handler<Object>() {

          public void handle(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            messages.add(message);
          }
        }).onNoMatch(new Handler<Object>() {

          public void handle(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            matches.add(message);
          }
        }).onNoMatch(new Handler<Object>() {

          public void handle(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            matches.add(message.toString());
          }
        }).build();
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return executorService;
      }
    });
    actor.tell("test", new Headers().withThreadId("test"), Stage.standIn());
    actor.tell(3, Headers.empty(), Stage.standIn());
    executorService.consumeAll();
    assertThat(messages).containsExactly("test");
    assertThat(matches).containsExactly(3, "3");
  }

  @Test
  public void onStart() {
    final ArrayList<Object> ids = new ArrayList<Object>();
    final TestExecutorService executorService = new TestExecutorService();
    final Actor actor = Stage.back().createActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return newBehavior().onStart(new Observer<Agent>() {

          public void accept(final Agent agent) {
            ids.add(agent.getSelf().getId());
          }
        }).build();
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return executorService;
      }
    });
    actor.tell(3, Headers.empty(), Stage.standIn());
    executorService.consumeAll();
    assertThat(ids).containsExactly(actor.getId());
  }

  @Test(expected = NullPointerException.class)
  public void onStartNPE() {
    Stage.back().createActor(new Role() {

      @NotNull
      @Override
      @SuppressWarnings("ConstantConditions")
      public Behavior getBehavior(@NotNull final String id) {
        return newBehavior().onStart(null).build();
      }
    });
  }

  @Test
  public void onStartOrder() {
    final ArrayList<Object> ids = new ArrayList<Object>();
    final TestExecutorService executorService = new TestExecutorService();
    final Actor actor = Stage.back().createActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return newBehavior().onStart(new Observer<Agent>() {

          public void accept(final Agent agent) {
            ids.add(agent.getSelf().getId());
          }
        }).onStart(new Observer<Agent>() {

          public void accept(final Agent agent) {
            ids.add(agent.getSelf().getId().toUpperCase(Locale.ENGLISH));
          }
        }).build();
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return executorService;
      }
    });
    actor.tell(3, Headers.empty(), Stage.standIn());
    executorService.consumeAll();
    assertThat(ids).containsExactly(actor.getId(), actor.getId().toUpperCase(Locale.ENGLISH));
  }

  @Test
  public void onStop() {
    final ArrayList<Object> ids = new ArrayList<Object>();
    final TestExecutorService executorService = new TestExecutorService();
    final Actor actor = Stage.back().createActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return newBehavior().onStop(new Observer<Agent>() {

          public void accept(final Agent agent) {
            ids.add(agent.getSelf().getId());
          }
        }).build();
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return executorService;
      }
    });
    actor.tell(3, Headers.empty(), Stage.standIn());
    executorService.consumeAll();
    assertThat(ids).isEmpty();
    actor.dismiss();
    executorService.consumeAll();
    assertThat(ids).containsExactly(actor.getId());
  }

  @Test(expected = NullPointerException.class)
  public void onStopNPE() {
    Stage.back().createActor(new Role() {

      @NotNull
      @Override
      @SuppressWarnings("ConstantConditions")
      public Behavior getBehavior(@NotNull final String id) {
        return newBehavior().onStop(null).build();
      }
    });
  }

  @Test
  public void onStopOrder() {
    final ArrayList<Object> ids = new ArrayList<Object>();
    final TestExecutorService executorService = new TestExecutorService();
    final Actor actor = Stage.back().createActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return newBehavior().onStop(new Observer<Agent>() {

          public void accept(final Agent agent) {
            ids.add(agent.getSelf().getId());
          }
        }).onStop(new Observer<Agent>() {

          public void accept(final Agent agent) {
            ids.add(agent.getSelf().getId().toUpperCase(Locale.ENGLISH));
          }
        }).build();
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return executorService;
      }
    });
    actor.tell(3, Headers.empty(), Stage.standIn());
    executorService.consumeAll();
    assertThat(ids).isEmpty();
    actor.dismiss();
    executorService.consumeAll();
    assertThat(ids).containsExactly(actor.getId(), actor.getId().toUpperCase(Locale.ENGLISH));
  }
}
