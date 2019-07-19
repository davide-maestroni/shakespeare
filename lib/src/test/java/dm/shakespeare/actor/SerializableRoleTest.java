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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import dm.shakespeare.Stage;
import dm.shakespeare.concurrent.ExecutorServices;
import dm.shakespeare.function.Mapper;
import dm.shakespeare.message.Bounce;
import dm.shakespeare.test.concurrent.TestExecutorService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SerializableRole} unit tests.
 */
public class SerializableRoleTest {

  @Test
  public void from() throws InterruptedException {
    final CountDownLatch latch = new CountDownLatch(1);
    final Actor actor =
        Stage.back().createActor(SerializableRole.from(new Mapper<String, Behavior>() {

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
    SerializableRole.from(null);
  }

  @Test
  public void fromSerialization() throws IOException, ClassNotFoundException, InterruptedException {
    final SerializableRole role = SerializableRole.from(new SerializableMapper());
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    final ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
    objectOutputStream.writeObject(role);
    objectOutputStream.close();
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
    final ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
    final SerializableRole deserialized = (SerializableRole) objectInputStream.readObject();
    final Actor actor = Stage.back().createActor(deserialized);
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole observerRole = new TestRole(executorService);
    final Actor observer = Stage.back().createActor(observerRole);
    actor.tell(3, Headers.empty(), observer);
    Thread.sleep(1000);
    executorService.consumeAll();
    assertThat(observerRole.getMessages()).containsExactly("3");
    assertThat(observerRole.getSenders()).containsExactly(actor);
  }

  @Test
  public void serializationBehavior() throws IOException, ClassNotFoundException {
    final SerializableBehaviorRole role = new SerializableBehaviorRole();
    Stage.back().createActor(role).tell(3, Headers.empty(), Stage.standIn());
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    final ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
    objectOutputStream.writeObject(role);
    objectOutputStream.close();
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
    final ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
    final SerializableRole deserialized = (SerializableRole) objectInputStream.readObject();
    final Actor actor = Stage.back().createActor(deserialized);
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole observerRole = new TestRole(executorService);
    final Actor observer = Stage.back().createActor(observerRole);
    actor.tell(3, Headers.empty(), observer);
    executorService.consumeAll();
    assertThat(observerRole.getMessages()).containsExactly("3");
    assertThat(observerRole.getSenders()).containsExactly(actor);
  }

  @Test
  public void serializationCreated() throws IOException, ClassNotFoundException {
    final SerializableCreatedRole role = new SerializableCreatedRole();
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    final ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
    objectOutputStream.writeObject(role);
    objectOutputStream.close();
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
    final ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
    final SerializableRole deserialized = (SerializableRole) objectInputStream.readObject();
    final Actor actor = Stage.back().createActor(deserialized);
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole observerRole = new TestRole(executorService);
    final Actor observer = Stage.back().createActor(observerRole);
    actor.tell(3, Headers.empty(), observer);
    executorService.consumeAll();
    assertThat(observerRole.getMessages()).containsExactly("3");
    assertThat(observerRole.getSenders()).containsExactly(actor);
  }

  @Test
  public void serializationDismissed() throws IOException, ClassNotFoundException {
    final SerializableDismissedRole role = new SerializableDismissedRole();
    final Actor roleActor = Stage.back().createActor(role);
    roleActor.tell(null, Headers.empty(), Stage.standIn());
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    final ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
    objectOutputStream.writeObject(role);
    objectOutputStream.close();
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
    final ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
    final SerializableRole deserialized = (SerializableRole) objectInputStream.readObject();
    final Actor actor = Stage.back().createActor(deserialized);
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole observerRole = new TestRole(executorService);
    final Actor observer = Stage.back().createActor(observerRole);
    actor.tell(3, Headers.empty().withReceiptId("test"), observer);
    executorService.consumeAll();
    assertThat(observerRole.getMessages()).hasSize(1);
    assertThat(observerRole.getMessages().get(0)).isExactlyInstanceOf(Bounce.class);
    assertThat(observerRole.getSenders()).containsExactly(actor);
  }

  @Test
  public void serializationStarted() throws IOException, ClassNotFoundException {
    final SerializableStartedRole role = new SerializableStartedRole();
    Stage.back().createActor(role).tell(3, Headers.empty(), Stage.standIn());
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    final ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
    objectOutputStream.writeObject(role);
    objectOutputStream.close();
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
    final ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
    final SerializableRole deserialized = (SerializableRole) objectInputStream.readObject();
    final Actor actor = Stage.back().createActor(deserialized);
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole observerRole = new TestRole(executorService);
    final Actor observer = Stage.back().createActor(observerRole);
    actor.tell(3, Headers.empty(), observer);
    executorService.consumeAll();
    assertThat(observerRole.getMessages()).containsExactly(1);
    assertThat(observerRole.getSenders()).containsExactly(actor);
  }

  @Test
  public void serializationStopped() throws IOException, ClassNotFoundException {
    final SerializableStoppedRole role = new SerializableStoppedRole();
    Stage.back().createActor(role).tell(null, Headers.empty(), Stage.standIn());
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    final ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
    objectOutputStream.writeObject(role);
    objectOutputStream.close();
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
    final ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
    final SerializableRole deserialized = (SerializableRole) objectInputStream.readObject();
    final Actor actor = Stage.back().createActor(deserialized);
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole observerRole = new TestRole(executorService);
    final Actor observer = Stage.back().createActor(observerRole);
    actor.tell(3, Headers.empty(), observer);
    executorService.consumeAll();
    assertThat(observerRole.getMessages()).containsExactly(1);
    assertThat(observerRole.getSenders()).containsExactly(actor);
  }

  @Test
  public void setBehaviorNPE() {
    final AtomicReference<Exception> exception = new AtomicReference<Exception>();
    final Actor actor = Stage.back().createActor(new SerializableRole() {

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return ExecutorServices.localExecutor();
      }

      @NotNull
      protected Behavior getSerializableBehavior(@NotNull final String id) {
        return new AbstractBehavior() {

          @SuppressWarnings("ConstantConditions")
          public void onMessage(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            try {
              agent.setBehavior(null);

            } catch (final Exception e) {
              exception.set(e);
            }
          }
        };
      }
    });
    actor.tell("test", Headers.empty(), Stage.standIn());
    assertThat(exception.get()).isExactlyInstanceOf(NullPointerException.class);
  }

  private static class SerializableBehaviorRole extends SerializableRole {

    @NotNull
    @Override
    public ExecutorService getExecutorService(@NotNull final String id) {
      return ExecutorServices.localExecutor();
    }

    @NotNull
    protected Behavior getSerializableBehavior(@NotNull final String id) {
      return new SerializableAbstractBehavior() {

        public void onMessage(final Object message, @NotNull final Envelop envelop,
            @NotNull final Agent agent) {
        }

        @Override
        public void onStart(@NotNull final Agent agent) {
          agent.setBehavior(new SerializableAbstractBehavior() {

            public void onMessage(final Object message, @NotNull final Envelop envelop,
                @NotNull final Agent agent) {
              envelop.getSender().tell(message.toString(), Headers.empty(), agent.getSelf());
            }
          });
        }
      };
    }
  }

  private static class SerializableCreatedRole extends SerializableRole {

    @NotNull
    @Override
    public ExecutorService getExecutorService(@NotNull final String id) {
      return ExecutorServices.localExecutor();
    }

    @NotNull
    protected Behavior getSerializableBehavior(@NotNull final String id) {
      return new SerializableAbstractBehavior() {

        public void onMessage(final Object message, @NotNull final Envelop envelop,
            @NotNull final Agent agent) {
          assertThat(getBehavior()).isSameAs(this);
          envelop.getSender().tell(message.toString(), Headers.empty(), agent.getSelf());
        }
      };
    }
  }

  private static class SerializableDismissedRole extends SerializableRole {

    @NotNull
    @Override
    public ExecutorService getExecutorService(@NotNull final String id) {
      return ExecutorServices.localExecutor();
    }

    @NotNull
    protected Behavior getSerializableBehavior(@NotNull final String id) {
      return new SerializableAbstractBehavior() {

        public void onMessage(final Object message, @NotNull final Envelop envelop,
            @NotNull final Agent agent) {
          envelop.getSender().tell(message.toString(), Headers.empty(), agent.getSelf());
        }

        @Override
        public void onStop(@NotNull final Agent agent) {
          assertThat(agent.isDismissed()).isTrue();
          assertThat(getState()).isEqualTo(RoleState.DISMISSED);
        }
      };
    }
  }

  private static class SerializableMapper implements Mapper<String, Behavior>, Serializable {

    public Behavior apply(final String id) {
      return new SerializableAbstractBehavior() {

        public void onMessage(final Object message, @NotNull final Envelop envelop,
            @NotNull final Agent agent) {
          envelop.getSender().tell(message.toString(), Headers.empty(), agent.getSelf());
        }
      };
    }
  }

  private static class SerializableStartedRole extends SerializableRole {

    @NotNull
    @Override
    public ExecutorService getExecutorService(@NotNull final String id) {
      return ExecutorServices.localExecutor();
    }

    @NotNull
    protected Behavior getSerializableBehavior(@NotNull final String id) {
      return new SerializableAbstractBehavior() {

        private int count;

        public void onMessage(final Object message, @NotNull final Envelop envelop,
            @NotNull final Agent agent) {
          envelop.getSender().tell(count, Headers.empty(), agent.getSelf());
        }

        @Override
        public void onStart(@NotNull final Agent agent) {
          ++count;
        }
      };
    }
  }

  private static class SerializableStoppedRole extends SerializableRole {

    private byte[] data;

    @NotNull
    @Override
    public ExecutorService getExecutorService(@NotNull final String id) {
      return ExecutorServices.localExecutor();
    }

    @NotNull
    protected Behavior getSerializableBehavior(@NotNull final String id) {
      final SerializableRole role = this;
      return new SerializableAbstractBehavior() {

        private int count;

        public void onMessage(final Object message, @NotNull final Envelop envelop,
            @NotNull final Agent agent) {
          envelop.getSender().tell(count, Headers.empty(), agent.getSelf());
          agent.restartBehavior();
        }

        @Override
        public void onStop(@NotNull final Agent agent) throws IOException {
          ++count;
          final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
          final ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
          objectOutputStream.writeObject(role);
          objectOutputStream.close();
          data = outputStream.toByteArray();
        }
      };
    }

    private Object readResolve() throws ObjectStreamException {
      if (data != null) {
        try {
          final ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
          final ObjectInputStream objectInputStream;
          objectInputStream = new ObjectInputStream(inputStream);
          return objectInputStream.readObject();

        } catch (IOException e) {
          throw new InvalidObjectException(e.getMessage());

        } catch (ClassNotFoundException e) {
          throw new InvalidClassException(e.getMessage());
        }
      }
      return this;
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
