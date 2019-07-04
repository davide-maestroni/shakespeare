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
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutorService;

import dm.shakespeare.Stage;
import dm.shakespeare.actor.AbstractBehavior;
import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Behavior.Agent;
import dm.shakespeare.actor.BehaviorBuilder.Matcher;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.Headers;
import dm.shakespeare.function.Tester;
import dm.shakespeare.template.behavior.annotation.OnAny;
import dm.shakespeare.template.behavior.annotation.OnEnvelop;
import dm.shakespeare.template.behavior.annotation.OnMatch;
import dm.shakespeare.template.behavior.annotation.OnMessage;
import dm.shakespeare.template.behavior.annotation.OnNoMatch;
import dm.shakespeare.template.behavior.annotation.OnParams;
import dm.shakespeare.template.behavior.annotation.OnStart;
import dm.shakespeare.template.behavior.annotation.OnStop;
import dm.shakespeare.test.concurrent.TestExecutorService;
import dm.shakespeare.util.Reflections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link AnnotatedRole} unit tests.
 */
@SuppressWarnings("unused")
public class AnnotatedRoleTest {

  @Test
  public void annotatedObject() throws NoSuchMethodException {
    final TestExecutorService executorService = new TestExecutorService();
    final AnnotatedList list = new AnnotatedList();
    Reflections.makeAccessible(list.getClass().getDeclaredMethod("add", Object.class));
    final AnnotatedRole testRole = new AnnotatedRole(list) {

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return executorService;
      }
    };
    final Actor actor = Stage.back().createActor(testRole);
    actor.tell("test", Headers.empty(), Stage.standIn());
    executorService.consumeAll();
    assertThat(list).containsExactly("test");
    assertThat(testRole.getObject()).isSameAs(list);
  }

  @Test
  public void onAny() {
    final TestExecutorService executorService = new TestExecutorService();
    final OnAnyRole testRole = new OnAnyRole(executorService);
    final Actor actor = Stage.back().createActor(testRole);
    actor.tell("test", Headers.empty(), Stage.standIn());
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly("test");
  }

  @Test(expected = IllegalArgumentException.class)
  public void onAnyInvalidMethod() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService) {

      @OnAny
      public void add() {
      }
    };
    Stage.back().createActor(testRole);
  }

  @Test(expected = IllegalArgumentException.class)
  public void onAnyInvalidMethodParams() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService) {

      @OnAny
      public void add(final Object object, final int size) {
      }
    };
    Stage.back().createActor(testRole);
  }

  @Test
  public void onAnyResult() {
    final TestExecutorService executorService = new TestExecutorService();
    final OnAnyResultRole testRole = new OnAnyResultRole(executorService);
    final TestRole observerRole = new TestRole(executorService) {

      @NotNull
      @Override
      protected Behavior getSerializableBehavior(@NotNull final String id) {
        return new AbstractBehavior() {

          public void onMessage(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            getMessages().add(message);
          }
        };
      }
    };
    final Actor actor = Stage.back().createActor(testRole);
    actor.tell("test", Headers.empty(), Stage.back().createActor(observerRole));
    executorService.consumeAll();
    assertThat(testRole.getMessages()).isEmpty();
    assertThat(observerRole.getMessages()).containsExactly(actor.getId());
  }

  @Test
  public void onEnvelop() {
    final TestExecutorService executorService = new TestExecutorService();
    final OnEnvelopRole testRole = new OnEnvelopRole(executorService);
    final Actor actor = Stage.back().createActor(testRole);
    actor.tell("test", Headers.empty(), Stage.standIn());
    executorService.consumeAll();
    assertThat(testRole.getMessages()).isEmpty();
    actor.tell("test", Headers.empty().withThreadId("test"), Stage.standIn());
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly("test");
  }

  @Test(expected = IllegalArgumentException.class)
  public void onEnvelopInvalidAnnotation() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService) {

      @OnEnvelop(testerName = "filter", testerClass = EnvelopTester.class)
      public void add(final Object object) {
        getMessages().add(object);
      }
    };
    Stage.back().createActor(testRole);
  }

  @Test(expected = IllegalArgumentException.class)
  public void onEnvelopInvalidMethod() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService) {

      @OnEnvelop(testerClass = EnvelopTester.class)
      public void add() {
      }
    };
    Stage.back().createActor(testRole);
  }

  @Test(expected = IllegalArgumentException.class)
  public void onEnvelopInvalidMethodParams() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService) {

      @OnEnvelop(testerClass = EnvelopTester.class)
      public void add(final Object object, final int size) {
      }
    };
    Stage.back().createActor(testRole);
  }

  @Test(expected = IllegalArgumentException.class)
  public void onEnvelopInvalidName() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService) {

      @OnEnvelop(testerName = "filter")
      public void add(final Object object) {
        getMessages().add(object);
      }
    };
    Stage.back().createActor(testRole);
  }

  @Test(expected = IllegalArgumentException.class)
  public void onEnvelopInvalidTester() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService) {

      @OnEnvelop(testerName = "filter")
      public void add(final Object object) {
        getMessages().add(object);
      }

      public void filter() {
      }
    };
    Stage.back().createActor(testRole);
  }

  @Test
  public void onEnvelopResult() {
    final TestExecutorService executorService = new TestExecutorService();
    final OnEnvelopResultRole testRole = new OnEnvelopResultRole(executorService);
    final TestRole observerRole = new TestRole(executorService) {

      @NotNull
      @Override
      protected Behavior getSerializableBehavior(@NotNull final String id) {
        return new AbstractBehavior() {

          public void onMessage(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            getMessages().add(message);
          }
        };
      }
    };
    final Actor actor = Stage.back().createActor(testRole);
    actor.tell("test", Headers.empty().withThreadId("TEST"),
        Stage.back().createActor(observerRole));
    executorService.consumeAll();
    assertThat(testRole.getMessages()).isEmpty();
    assertThat(observerRole.getMessages()).containsExactly("TEST");
  }

  @Test
  public void onEnvelopTester() {
    final TestExecutorService executorService = new TestExecutorService();
    final OnEnvelopTesterRole testRole = new OnEnvelopTesterRole(executorService);
    final Actor actor = Stage.back().createActor(testRole);
    actor.tell("test", Headers.empty(), Stage.standIn());
    executorService.consumeAll();
    assertThat(testRole.getMessages()).isEmpty();
    actor.tell("test", Headers.empty().withThreadId("test"), Stage.standIn());
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly("test");
  }

  @Test
  public void onMatch() {
    final TestExecutorService executorService = new TestExecutorService();
    final OnMatchRole testRole = new OnMatchRole(executorService);
    final Actor actor = Stage.back().createActor(testRole);
    actor.tell("test", Headers.empty(), Stage.standIn());
    executorService.consumeAll();
    assertThat(testRole.getMessages()).isEmpty();
    actor.tell("test", Headers.empty().withThreadId("test"), Stage.standIn());
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly("test");
  }

  @Test(expected = IllegalArgumentException.class)
  public void onMatchInvalidAnnotation() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService) {

      @OnMatch(matcherName = "filter", matcherClass = MessageMatcher.class)
      public void add(final Object object) {
        getMessages().add(object);
      }
    };
    Stage.back().createActor(testRole);
  }

  @Test(expected = IllegalArgumentException.class)
  public void onMatchInvalidMatcher() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService) {

      @OnMatch(matcherName = "filter")
      public void add(final Object object) {
        getMessages().add(object);
      }

      public void filter() {
      }
    };
    Stage.back().createActor(testRole);
  }

  @Test(expected = IllegalArgumentException.class)
  public void onMatchInvalidMethod() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService) {

      @OnMatch(matcherClass = MessageMatcher.class)
      public void add() {
      }
    };
    Stage.back().createActor(testRole);
  }

  @Test(expected = IllegalArgumentException.class)
  public void onMatchInvalidMethodParams() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService) {

      @OnMatch(matcherClass = MessageMatcher.class)
      public void add(final Object object, final int size) {
      }
    };
    Stage.back().createActor(testRole);
  }

  @Test(expected = IllegalArgumentException.class)
  public void onMatchInvalidName() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService) {

      @OnMatch(matcherName = "filter")
      public void add(final Object object) {
        getMessages().add(object);
      }
    };
    Stage.back().createActor(testRole);
  }

  @Test
  public void onMatchMatcher() {
    final TestExecutorService executorService = new TestExecutorService();
    final OnMatchMatcherRole testRole = new OnMatchMatcherRole(executorService);
    final Actor actor = Stage.back().createActor(testRole);
    actor.tell("TEST", Headers.empty(), Stage.standIn());
    executorService.consumeAll();
    assertThat(testRole.getMessages()).isEmpty();
    actor.tell("test", Headers.empty(), Stage.standIn());
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly("test");
  }

  @Test
  public void onMatchResult() {
    final TestExecutorService executorService = new TestExecutorService();
    final OnMatchResultRole testRole = new OnMatchResultRole(executorService);
    final TestRole observerRole = new TestRole(executorService) {

      @NotNull
      @Override
      protected Behavior getSerializableBehavior(@NotNull final String id) {
        return new AbstractBehavior() {

          public void onMessage(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            getMessages().add(message);
          }
        };
      }
    };
    final Actor actor = Stage.back().createActor(testRole);
    actor.tell("test", Headers.empty(), Stage.back().createActor(observerRole));
    executorService.consumeAll();
    assertThat(testRole.getMessages()).isEmpty();
    assertThat(observerRole.getMessages()).containsExactly(actor.getId());
  }

  @Test
  public void onMessage() {
    final TestExecutorService executorService = new TestExecutorService();
    final OnMessageRole testRole = new OnMessageRole(executorService);
    final Actor actor = Stage.back().createActor(testRole);
    actor.tell("TEST", Headers.empty(), Stage.standIn());
    executorService.consumeAll();
    assertThat(testRole.getMessages()).isEmpty();
    actor.tell("test", Headers.empty(), Stage.standIn());
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly("test");
  }

  @Test
  public void onMessageClass() {
    final TestExecutorService executorService = new TestExecutorService();
    final OnMessageClassRole testRole = new OnMessageClassRole(executorService);
    final Actor actor = Stage.back().createActor(testRole);
    actor.tell(1, Headers.empty(), Stage.standIn());
    executorService.consumeAll();
    assertThat(testRole.getMessages()).isEmpty();
    actor.tell("test", Headers.empty(), Stage.standIn());
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly("test");
  }

  @Test
  public void onMessageClasses() {
    final TestExecutorService executorService = new TestExecutorService();
    final OnMessageClassesRole testRole = new OnMessageClassesRole(executorService);
    final Actor actor = Stage.back().createActor(testRole);
    actor.tell(1, Headers.empty(), Stage.standIn());
    actor.tell("test", Headers.empty(), Stage.standIn());
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly(1, "test");
  }

  @Test(expected = IllegalArgumentException.class)
  public void onMessageInvalidAnnotation() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService) {

      @OnMessage(testerName = "filter", messageClasses = MessageMatcher.class,
          testerClass = MessageTester.class)
      public void add(final Object object) {
        getMessages().add(object);
      }
    };
    Stage.back().createActor(testRole);
  }

  @Test(expected = IllegalArgumentException.class)
  public void onMessageInvalidMethod() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService) {

      @OnMessage(messageClasses = String.class)
      public void add() {
      }
    };
    Stage.back().createActor(testRole);
  }

  @Test(expected = IllegalArgumentException.class)
  public void onMessageInvalidMethodParams() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService) {

      @OnMessage(messageClasses = String.class)
      public void add(final Object object, final int size) {
      }
    };
    Stage.back().createActor(testRole);
  }

  @Test(expected = IllegalArgumentException.class)
  public void onMessageInvalidName() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService) {

      @OnMessage(testerName = "filter")
      public void add(final Object object) {
        getMessages().add(object);
      }
    };
    Stage.back().createActor(testRole);
  }

  @Test(expected = IllegalArgumentException.class)
  public void onMessageInvalidTester() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService) {

      @OnMessage(testerName = "filter")
      public void add(final Object object) {
        getMessages().add(object);
      }

      public void filter() {
      }
    };
    Stage.back().createActor(testRole);
  }

  @Test
  public void onMessageResult() {
    final TestExecutorService executorService = new TestExecutorService();
    final OnMessageResultRole testRole = new OnMessageResultRole(executorService);
    final TestRole observerRole = new TestRole(executorService) {

      @NotNull
      @Override
      protected Behavior getSerializableBehavior(@NotNull final String id) {
        return new AbstractBehavior() {

          public void onMessage(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            getMessages().add(message);
          }
        };
      }
    };
    final Actor actor = Stage.back().createActor(testRole);
    actor.tell("test", Headers.empty().withThreadId("THREAD"),
        Stage.back().createActor(observerRole));
    executorService.consumeAll();
    assertThat(testRole.getMessages()).isEmpty();
    assertThat(observerRole.getMessages()).containsExactly("THREAD");
  }

  @Test
  public void onMessageTester() {
    final TestExecutorService executorService = new TestExecutorService();
    final OnMessageTesterRole testRole = new OnMessageTesterRole(executorService);
    final Actor actor = Stage.back().createActor(testRole);
    actor.tell("TEST", Headers.empty(), Stage.standIn());
    executorService.consumeAll();
    assertThat(testRole.getMessages()).isEmpty();
    actor.tell("test", Headers.empty(), Stage.standIn());
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly("test");
  }

  @Test
  public void onNoMatch() {
    final TestExecutorService executorService = new TestExecutorService();
    final OnNoMatchRole testRole = new OnNoMatchRole(executorService);
    final Actor actor = Stage.back().createActor(testRole);
    actor.tell(1, Headers.empty(), Stage.standIn());
    executorService.consumeAll();
    assertThat(testRole.getMessages()).isEmpty();
    actor.tell("test", Headers.empty(), Stage.standIn());
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly("test");
  }

  @Test(expected = IllegalArgumentException.class)
  public void onNoMatchInvalidMethod() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService) {

      @OnNoMatch
      public void add() {
      }
    };
    Stage.back().createActor(testRole);
  }

  @Test(expected = IllegalArgumentException.class)
  public void onNoMatchInvalidMethodParams() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService) {

      @OnNoMatch
      public void add(final Object object, final int size) {
      }
    };
    Stage.back().createActor(testRole);
  }

  @Test
  public void onNoMatchResult() {
    final TestExecutorService executorService = new TestExecutorService();
    final OnNoMatchResultRole testRole = new OnNoMatchResultRole(executorService);
    final TestRole observerRole = new TestRole(executorService) {

      @NotNull
      @Override
      protected Behavior getSerializableBehavior(@NotNull final String id) {
        return new AbstractBehavior() {

          public void onMessage(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            getMessages().add(message);
          }
        };
      }
    };
    final Actor actor = Stage.back().createActor(testRole);
    actor.tell("test", Headers.empty(), Stage.back().createActor(observerRole));
    executorService.consumeAll();
    assertThat(testRole.getMessages()).isEmpty();
    assertThat(observerRole.getMessages()).containsExactly(actor.getId());
  }

  @Test
  public void onParams() {
    final TestExecutorService executorService = new TestExecutorService();
    final OnParamsRole testRole = new OnParamsRole(executorService);
    final Actor actor = Stage.back().createActor(testRole);
    actor.tell(Arrays.asList("test", 3), Headers.empty(), Stage.standIn());
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly("test", 3);
  }

  @Test
  public void onParamsEmpty() {
    final TestExecutorService executorService = new TestExecutorService();
    final OnParamsRole testRole = new OnParamsRole(executorService);
    final Actor actor = Stage.back().createActor(testRole);
    actor.tell(Collections.emptyList(), Headers.empty(), Stage.standIn());
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactly("TEST");
  }

  @Test
  public void onParamsResult() {
    final TestExecutorService executorService = new TestExecutorService();
    final OnParamsResultRole testRole = new OnParamsResultRole(executorService);
    final TestRole observerRole = new TestRole(executorService) {

      @NotNull
      @Override
      protected Behavior getSerializableBehavior(@NotNull final String id) {
        return new AbstractBehavior() {

          public void onMessage(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            getMessages().add(message);
          }
        };
      }
    };
    final Actor actor = Stage.back().createActor(testRole);
    actor.tell(Arrays.asList("test", 3), Headers.empty().withThreadId("test3"),
        Stage.back().createActor(observerRole));
    actor.tell(Collections.emptyList(), Headers.empty().withThreadId("test3"),
        Stage.back().createActor(observerRole));
    executorService.consumeAll();
    assertThat(testRole.getMessages()).isEmpty();
    assertThat(observerRole.getMessages()).containsExactly("test3", actor.getId());
  }

  @Test
  public void onStart() {
    final TestExecutorService executorService = new TestExecutorService();
    final OnStartRole testRole = new OnStartRole(executorService);
    final Actor actor = Stage.back().createActor(testRole);
    actor.tell(1, Headers.empty(), Stage.standIn());
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactlyInAnyOrder("test", actor.getId());
  }

  @Test
  public void onStop() {
    final TestExecutorService executorService = new TestExecutorService();
    final OnStopRole testRole = new OnStopRole(executorService);
    final Actor actor = Stage.back().createActor(testRole);
    actor.tell(1, Headers.empty(), Stage.standIn());
    actor.dismissLazy();
    executorService.consumeAll();
    assertThat(testRole.getMessages()).containsExactlyInAnyOrder("test", actor.getId());
  }

  public static class AnnotatedList extends ArrayList<Object> {

    @OnAny
    @Override
    public boolean add(final Object o) {
      return super.add(o);
    }
  }

  public static class EnvelopTester implements Tester<Envelop> {

    public boolean test(final Envelop envelop) {
      return envelop.getHeaders().getThreadId() != null;
    }
  }

  public static class MessageMatcher implements Matcher<Object> {

    public boolean match(final Object message, @NotNull final Envelop envelop,
        @NotNull final Agent agent) {
      return "test".equals(message);
    }
  }

  public static class MessageTester implements Tester<Object> {

    public boolean test(final Object value) {
      return "test".equals(value);
    }
  }

  public static class OnAnyResultRole extends TestRole {

    OnAnyResultRole(@NotNull final TestExecutorService executorService) {
      super(executorService);
    }

    @OnAny
    public String add(final Object object, final Agent agent) {
      return agent.getSelf().getId();
    }
  }

  public static class OnAnyRole extends TestRole {

    OnAnyRole(@NotNull final TestExecutorService executorService) {
      super(executorService);
    }

    @OnAny
    public void add(final Object object) {
      getMessages().add(object);
    }
  }

  public static class OnEnvelopResultRole extends TestRole {

    OnEnvelopResultRole(@NotNull final TestExecutorService executorService) {
      super(executorService);
    }

    @OnEnvelop(testerName = "filter")
    public String add(final Object object, final Agent agent, final Envelop envelop) {
      return envelop.getHeaders().getThreadId();
    }

    public boolean filter(final Envelop envelop) {
      return true;
    }
  }

  public static class OnEnvelopRole extends TestRole {

    OnEnvelopRole(@NotNull final TestExecutorService executorService) {
      super(executorService);
    }

    @OnEnvelop(testerName = "filter")
    public void add(final Object object) {
      getMessages().add(object);
    }

    public boolean filter(final Envelop envelop) {
      return envelop.getHeaders().getThreadId() != null;
    }
  }

  public static class OnEnvelopTesterRole extends TestRole {

    OnEnvelopTesterRole(@NotNull final TestExecutorService executorService) {
      super(executorService);
    }

    @OnEnvelop(testerClass = EnvelopTester.class)
    public void add(final Object object) {
      getMessages().add(object);
    }
  }

  public static class OnMatchMatcherRole extends TestRole {

    OnMatchMatcherRole(@NotNull final TestExecutorService executorService) {
      super(executorService);
    }

    @OnMatch(matcherClass = MessageMatcher.class)
    public void add(final Object object) {
      getMessages().add(object);
    }
  }

  public static class OnMatchResultRole extends TestRole {

    OnMatchResultRole(@NotNull final TestExecutorService executorService) {
      super(executorService);
    }

    @OnMatch(matcherName = "filter")
    public String add(final Object object, final Agent agent, final Envelop envelop) {
      return agent.getSelf().getId();
    }

    public boolean filter(final Object message, final Envelop envelop, final Agent agent) {
      return true;
    }
  }

  public static class OnMatchRole extends TestRole {

    OnMatchRole(@NotNull final TestExecutorService executorService) {
      super(executorService);
    }

    @OnMatch(matcherName = "filter")
    public void add(final Object object) {
      getMessages().add(object);
    }

    public boolean filter(final Object message, final Envelop envelop, final Agent agent) {
      return envelop.getHeaders().getThreadId() != null;
    }
  }

  public static class OnMessageClassRole extends TestRole {

    OnMessageClassRole(@NotNull final TestExecutorService executorService) {
      super(executorService);
    }

    @OnMessage(messageClasses = String.class)
    public void add(final Object object) {
      getMessages().add(object);
    }
  }

  public static class OnMessageClassesRole extends TestRole {

    OnMessageClassesRole(@NotNull final TestExecutorService executorService) {
      super(executorService);
    }

    @OnMessage(messageClasses = {Integer.class, String.class})
    public void add(final Object object) {
      getMessages().add(object);
    }
  }

  public static class OnMessageResultRole extends TestRole {

    OnMessageResultRole(@NotNull final TestExecutorService executorService) {
      super(executorService);
    }

    @OnMessage(testerName = "filter")
    public String add(final Object object, final Envelop envelop) {
      return envelop.getHeaders().getThreadId();
    }

    public boolean filter(final Object message) {
      return true;
    }
  }

  public static class OnMessageRole extends TestRole {

    OnMessageRole(@NotNull final TestExecutorService executorService) {
      super(executorService);
    }

    @OnMessage(testerName = "filter")
    public void add(final Object object) {
      getMessages().add(object);
    }

    public boolean filter(final Object message) {
      return "test".equals(message);
    }
  }

  public static class OnMessageTesterRole extends TestRole {

    OnMessageTesterRole(@NotNull final TestExecutorService executorService) {
      super(executorService);
    }

    @OnMessage(testerClass = MessageTester.class)
    public void add(final Object object) {
      getMessages().add(object);
    }
  }

  public static class OnNoMatchResultRole extends TestRole {

    OnNoMatchResultRole(@NotNull final TestExecutorService executorService) {
      super(executorService);
    }

    @OnNoMatch
    public String add(final Object object, final Agent agent) {
      return agent.getSelf().getId();
    }

    @OnMessage(messageClasses = Integer.class)
    public void filter(final Object object) {
    }
  }

  public static class OnNoMatchRole extends TestRole {

    OnNoMatchRole(@NotNull final TestExecutorService executorService) {
      super(executorService);
    }

    @OnNoMatch
    public void add(final Object object) {
      getMessages().add(object);
    }

    @OnMessage(messageClasses = Integer.class)
    public void filter(final Object object) {
    }
  }

  public static class OnParamsResultRole extends TestRole {

    OnParamsResultRole(@NotNull final TestExecutorService executorService) {
      super(executorService);
    }

    @OnParams
    public String add(final String name, final int size, final Agent agent, final Envelop envelop) {
      return envelop.getHeaders().getThreadId();
    }

    @OnParams
    public String add(final Agent agent, final Envelop envelop) {
      return agent.getSelf().getId();
    }
  }

  public static class OnParamsRole extends TestRole {

    OnParamsRole(@NotNull final TestExecutorService executorService) {
      super(executorService);
    }

    @OnParams
    public void add(final String name, final int size) {
      getMessages().add(name);
      getMessages().add(size);
    }

    @OnParams
    public void add() {
      getMessages().add("TEST");
    }
  }

  public static class OnStartRole extends TestRole {

    OnStartRole(@NotNull final TestExecutorService executorService) {
      super(executorService);
    }

    @OnStart
    public void add(final Agent agent) {
      getMessages().add(agent.getSelf().getId());
    }

    @OnStart
    public void add() {
      getMessages().add("test");
    }
  }

  public static class OnStopRole extends TestRole {

    OnStopRole(@NotNull final TestExecutorService executorService) {
      super(executorService);
    }

    @OnStop
    public void add(final Agent agent) {
      getMessages().add(agent.getSelf().getId());
    }

    @OnStop
    public void add() {
      getMessages().add("test");
    }
  }

  @SuppressWarnings("unused")
  public static class TestRole extends AnnotatedRole {

    private final TestExecutorService executorService;
    private final ArrayList<Headers> headers = new ArrayList<Headers>();
    private final ArrayList<Object> messages = new ArrayList<Object>();
    private final ArrayList<Actor> senders = new ArrayList<Actor>();

    TestRole(@NotNull final TestExecutorService executorService) {
      this.executorService = executorService;
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
