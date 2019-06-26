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

import dm.shakespeare.actor.AbstractBehavior;
import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.Headers;
import dm.shakespeare.actor.Role;
import dm.shakespeare.message.Delivery;
import dm.shakespeare.test.concurrent.TestExecutorService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link Stage#STAND_IN} unit tests.
 */
public class StandInActorTest {

  @Test
  public void addObserver() {
    final Actor actor = Stage.STAND_IN;
    assertThat(actor.addObserver(actor)).isTrue();
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void addObserverNPE() {
    final Actor actor = Stage.STAND_IN;
    actor.addObserver(null);
  }

  @Test
  public void dismiss() {
    final Actor actor = Stage.STAND_IN;
    assertThat(actor.dismiss()).isTrue();
  }

  @Test
  public void dismissLazy() {
    final Actor actor = Stage.STAND_IN;
    assertThat(actor.dismissLazy()).isTrue();
  }

  @Test
  public void dismissNow() {
    final Actor actor = Stage.STAND_IN;
    assertThat(actor.dismissNow()).isTrue();
  }

  @Test
  public void getId() {
    final Actor actor = Stage.STAND_IN;
    assertThat(actor.getId()).isNotNull();
  }

  @Test
  public void removeObserver() {
    final Actor actor = Stage.STAND_IN;
    assertThat(actor.removeObserver(actor)).isTrue();
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void removeObserverNPE() {
    final Actor actor = Stage.STAND_IN;
    actor.removeObserver(null);
  }

  @Test
  public void tellAllDelivery() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = Stage.newActor(testRole);
    Stage.STAND_IN.tellAll(Arrays.asList("test1", "test2"), new Headers().withReceiptId("test"),
        actor);
    executorService.consumeAll();
    assertThat(testRole.getMessages()).hasSize(2);
    assertThat(testRole.getMessages().get(0)).isInstanceOf(Delivery.class);
    assertThat(testRole.getMessages().get(1)).isInstanceOf(Delivery.class);
    assertThat(testRole.getSenders()).hasSize(2);
    assertThat(testRole.getSenders().get(0)).isSameAs(Stage.STAND_IN);
    assertThat(testRole.getSenders().get(0)).isSameAs(Stage.STAND_IN);
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void tellAllHeadersNPE() {
    final Actor actor = Stage.STAND_IN;
    actor.tellAll(Collections.emptyList(), null, Stage.STAND_IN);
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void tellAllNPE() {
    final Actor actor = Stage.STAND_IN;
    actor.tellAll(null, Headers.EMPTY, Stage.STAND_IN);
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void tellAllSenderNPE() {
    final Actor actor = Stage.STAND_IN;
    actor.tellAll(Collections.emptyList(), Headers.EMPTY, null);
  }

  @Test
  public void tellDelivery() {
    final TestExecutorService executorService = new TestExecutorService();
    final TestRole testRole = new TestRole(executorService);
    final Actor actor = Stage.newActor(testRole);
    Stage.STAND_IN.tell(null, new Headers().withReceiptId("test"), actor);
    executorService.consumeAll();
    assertThat(testRole.getMessages()).hasSize(1);
    assertThat(testRole.getMessages().get(0)).isInstanceOf(Delivery.class);
    assertThat(testRole.getSenders()).hasSize(1);
    assertThat(testRole.getSenders().get(0)).isSameAs(Stage.STAND_IN);
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void tellHeadersNPE() {
    final Actor actor = Stage.STAND_IN;
    actor.tell(null, null, Stage.STAND_IN);
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void tellSenderNPE() {
    final Actor actor = Stage.STAND_IN;
    actor.tell(null, Headers.EMPTY, null);
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
