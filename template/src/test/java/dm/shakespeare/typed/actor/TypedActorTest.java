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

package dm.shakespeare.typed.actor;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import dm.shakespeare.Stage;
import dm.shakespeare.actor.AbstractBehavior;
import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.Headers;
import dm.shakespeare.actor.Role;
import dm.shakespeare.concurrent.ExecutorServices;
import dm.shakespeare.message.Delivery;
import dm.shakespeare.template.typed.InvocationMismatchException;
import dm.shakespeare.template.typed.InvocationTimeoutException;
import dm.shakespeare.template.typed.TypedStage;
import dm.shakespeare.template.typed.actor.Script;
import dm.shakespeare.template.typed.annotation.ActorFrom;
import dm.shakespeare.template.typed.annotation.HeadersFrom;
import dm.shakespeare.template.typed.message.InvocationResult;
import dm.shakespeare.test.concurrent.TestExecutorService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by davide-maestroni on 06/28/2019.
 */
public class TypedActorTest {

  @Test
  public void classInvocation() {
    final TypedItf actor =
        TypedStage.newActor(new LocalScript(), TypedItf.class, TypedRole.class, new Object[0]);
    actor.setValue("test");
    assertThat(actor.getValue()).isEqualTo("test");
  }

  @Test(expected = InvocationMismatchException.class)
  @SuppressWarnings("unchecked")
  public void classInvocationException() {
    final List<String> actor =
        TypedStage.newActor(new LocalScript(), List.class, TypedRole.class, new Object[0]);
    actor.size();
  }

  @Test
  public void classInvocationResult() {
    final TypedItf actor =
        TypedStage.newActor(new LocalScript(), TypedItf.class, TypedRole.class, "test");
    assertThat(actor.getValue()).isEqualTo("test");
  }

  @Test(expected = InvocationTimeoutException.class)
  public void classInvocationTimeout() {
    final TypedItf actor = TypedStage.newActor(new Script() {

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return new TestExecutorService();
      }

      @Override
      public Long getResultTimeoutMillis(@NotNull final String id, @NotNull final Method method) {
        return 0L;
      }
    }, TypedItf.class, TypedRole.class, new Object[0]);
    actor.getValue();
  }

  @Test
  public void fromActor() {
    final TypedItf actor = TypedStage.newActor(new LocalScript() {

      @Override
      public Long getResultTimeoutMillis(@NotNull final String id, @NotNull final Method method) {
        return null;
      }
    }, TypedItf.class, TypedRole.class, "test");
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    assertThat(actor.getValue(Stage.newActor(testRole))).isNull();
    executorService.consumeAll();
    assertThat(testRole.getMessages()).hasSize(1);
    assertThat(testRole.getMessages().get(0)).isExactlyInstanceOf(InvocationResult.class);
    assertThat(((InvocationResult) testRole.getMessages().get(0)).getResult()).isEqualTo("test");
  }

  @Test
  public void fromActorAndHeaders() {
    final TypedItf actor = TypedStage.newActor(new LocalScript() {

      @Override
      public Long getResultTimeoutMillis(@NotNull final String id, @NotNull final Method method) {
        return null;
      }
    }, TypedItf.class, TypedRole.class, "test");
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Headers headers = new Headers().withThreadId("test");
    assertThat(actor.getValue(Stage.newActor(testRole), headers)).isNull();
    executorService.consumeAll();
    assertThat(testRole.getMessages()).hasSize(1);
    assertThat(testRole.getMessages().get(0)).isExactlyInstanceOf(InvocationResult.class);
    assertThat(((InvocationResult) testRole.getMessages().get(0)).getResult()).isEqualTo("test");
    assertThat(testRole.getHeaders()).hasSize(1);
    assertThat(testRole.getHeaders().get(0).getThreadId()).isEqualTo("test");
  }

  @Test
  public void fromActorAndHeadersWithReceipt() {
    final TypedItf actor = TypedStage.newActor(new LocalScript() {

      @Override
      public Long getResultTimeoutMillis(@NotNull final String id, @NotNull final Method method) {
        return null;
      }
    }, TypedItf.class, TypedRole.class, "test");
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Headers headers = new Headers().withThreadId("test").withReceiptId("test");
    assertThat(actor.getValue(Stage.newActor(testRole), headers)).isNull();
    executorService.consumeAll();
    assertThat(testRole.getMessages()).hasSize(2);
    assertThat(testRole.getMessages().get(0)).isExactlyInstanceOf(InvocationResult.class);
    assertThat(((InvocationResult) testRole.getMessages().get(0)).getResult()).isEqualTo("test");
    assertThat(testRole.getMessages().get(1)).isExactlyInstanceOf(Delivery.class);
    assertThat(((Delivery) testRole.getMessages().get(1)).getHeaders().getReceiptId()).isEqualTo(
        "test");
    assertThat(testRole.getHeaders()).hasSize(2);
    assertThat(testRole.getHeaders().get(0).getThreadId()).isEqualTo("test");
  }

  @Test
  public void fromHeaders() {
    final TypedItf actor =
        TypedStage.newActor(new LocalScript(), TypedItf.class, TypedRole.class, "test");
    final Headers headers = new Headers().withThreadId("test").withReceiptId("test");
    assertThat(actor.getValue(headers)).isEqualTo("test");
  }

  @Test
  public void getTyped() {
    final TypedItf actor =
        TypedStage.newActor(new LocalScript(), TypedItf.class, TypedRole.class, "test");
    final TypedItf typed = TypedStage.newActor(new LocalScript(), TypedItf.class, new TypedRole());
    actor.setValue(typed);
    assertThat(typed.getValue()).isEqualTo("test");
  }

  @Test
  public void invocationAwait() {
    final TypedRole testRole = new TypedRole();
    final TypedItf actor = TypedStage.newActor("test", new Script(), TypedItf.class, testRole);
    actor.setValue("test");
    assertThat(actor.getValue()).isEqualTo("test");
  }

  @Test(expected = InvocationMismatchException.class)
  @SuppressWarnings("unchecked")
  public void invocationAwaitException() {
    final TypedRole testRole = new TypedRole();
    final List<String> actor = TypedStage.newActor("test", new Script() {

      @Override
      public Long getResultTimeoutMillis(@NotNull final String id, @NotNull final Method method) {
        return 2000L;
      }
    }, List.class, testRole);
    actor.clear();
  }

  @Test
  public void objectInvocation() {
    final TypedRole testRole = new TypedRole();
    final TypedItf actor = TypedStage.newActor(new LocalScript(), TypedItf.class, testRole);
    actor.setValue("test");
    assertThat(testRole.getValue()).isEqualTo("test");
  }

  @Test(expected = InvocationMismatchException.class)
  @SuppressWarnings("unchecked")
  public void objectInvocationException() {
    final TypedRole testRole = new TypedRole();
    testRole.setValue("test");
    final List<String> actor = TypedStage.newActor(new LocalScript(), List.class, testRole);
    actor.size();
  }

  @Test
  public void objectInvocationResult() {
    final TypedRole testRole = new TypedRole();
    testRole.setValue("test");
    final TypedItf actor = TypedStage.newActor(new LocalScript(), TypedItf.class, testRole);
    actor.getValue();
  }

  @Test(expected = InvocationTimeoutException.class)
  public void objectInvocationTimeout() {
    final TypedRole testRole = new TypedRole();
    testRole.setValue("test");
    final TypedItf actor = TypedStage.newActor(new Script() {

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return new TestExecutorService();
      }

      @Override
      public Long getResultTimeoutMillis(@NotNull final String id, @NotNull final Method method) {
        return 0L;
      }
    }, TypedItf.class, testRole);
    actor.getValue();
  }

  @Test
  public void setFromActor() {
    final TypedItf actor =
        TypedStage.newActor(new LocalScript(), TypedItf.class, TypedRole.class, new Object[0]);
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    actor.setValue("test", Stage.newActor(testRole));
    executorService.consumeAll();
    assertThat(testRole.getMessages()).hasSize(1);
    assertThat(testRole.getMessages().get(0)).isExactlyInstanceOf(InvocationResult.class);
    assertThat(((InvocationResult) testRole.getMessages().get(0)).getResult()).isNull();
  }

  @Test
  public void setFromActorAndHeaders() {
    final TypedItf actor =
        TypedStage.newActor(new LocalScript(), TypedItf.class, TypedRole.class, new Object[0]);
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Headers headers = new Headers().withThreadId("test");
    actor.setValue(headers, "test", Stage.newActor(testRole));
    executorService.consumeAll();
    assertThat(testRole.getMessages()).hasSize(1);
    assertThat(testRole.getMessages().get(0)).isExactlyInstanceOf(InvocationResult.class);
    assertThat(((InvocationResult) testRole.getMessages().get(0)).getResult()).isNull();
    assertThat(testRole.getHeaders()).hasSize(1);
    assertThat(testRole.getHeaders().get(0).getThreadId()).isEqualTo("test");
  }

  @Test
  public void setFromActorAndHeadersWithReceipt() {
    final TypedItf actor = TypedStage.newActor(new LocalScript(), TypedItf.class, new TypedRole());
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Headers headers = new Headers().withThreadId("test").withReceiptId("test");
    actor.setValue(headers, "test", Stage.newActor(testRole));
    executorService.consumeAll();
    assertThat(testRole.getMessages()).hasSize(2);
    assertThat(testRole.getMessages().get(0)).isExactlyInstanceOf(InvocationResult.class);
    assertThat(((InvocationResult) testRole.getMessages().get(0)).getResult()).isNull();
    assertThat(testRole.getMessages().get(1)).isExactlyInstanceOf(Delivery.class);
    assertThat(((Delivery) testRole.getMessages().get(1)).getHeaders().getReceiptId()).isEqualTo(
        "test");
    assertThat(testRole.getHeaders()).hasSize(2);
    assertThat(testRole.getHeaders().get(0).getThreadId()).isEqualTo("test");
  }

  @Test
  public void setFromHeaders() {
    final TypedItf actor = TypedStage.newActor(new LocalScript(), TypedItf.class, new TypedRole());
    final Headers headers = new Headers().withThreadId("test").withReceiptId("test");
    actor.setValue(headers, "test");
    assertThat(actor.getValue()).isEqualTo("test");
  }

  @Test
  public void setTyped() {
    final TypedItf actor = TypedStage.newActor(new Script(), TypedItf.class, new TypedRole());
    final TypedItf typed =
        TypedStage.newActor(new LocalScript(), TypedItf.class, TypedRole.class, "test");
    assertThat(actor.getValue(typed)).isEqualTo("test");
    assertThat(actor.getValue()).isNull();
  }

  @Test(expected = NullPointerException.class)
  public void typedNPE() {
    final TypedItf actor = TypedStage.newActor(new LocalScript(), TypedItf.class, new TypedRole());
    actor.getValue((TypedItf) null);
  }

  private interface TypedItf {

    Object getValue();

    void setValue(Object value);

    void setValue(TypedItf typed);

    Object getValue(@ActorFrom Actor sender);

    Object getValue(@ActorFrom Actor sender, @HeadersFrom Headers headers);

    Object getValue(@HeadersFrom Headers headers);

    Object getValue(TypedItf typed);

    void setValue(@HeadersFrom Headers headers, Object value, @ActorFrom Actor sender);

    void setValue(@HeadersFrom Headers headers, Object value);

    void setValue(Object value, @ActorFrom Actor sender);
  }

  public static class TypedRole {

    private Object value;

    public TypedRole() {
    }

    public TypedRole(final Object value) {
      this.value = value;
    }

    public Object getValue() {
      return value;
    }

    public void setValue(final Object value) {
      this.value = value;
    }

    public void setValue(TypedItf typed) {
      typed.setValue(value);
    }

    public Object getValue(TypedItf typed) {
      return typed.getValue();
    }
  }

  private static class LocalScript extends Script {

    @NotNull
    @Override
    public ExecutorService getExecutorService(@NotNull final String id) {
      return ExecutorServices.localExecutor();
    }
  }

  private static class TestRole extends Role {

    private final TestExecutorService executorService;
    private final ArrayList<Headers> headers = new ArrayList<Headers>();
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
