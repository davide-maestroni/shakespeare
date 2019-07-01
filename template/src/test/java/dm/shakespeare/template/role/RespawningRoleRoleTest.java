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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import dm.shakespeare.Stage;
import dm.shakespeare.actor.AbstractBehavior;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.Headers;
import dm.shakespeare.actor.Role;
import dm.shakespeare.concurrent.ExecutorServices;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by davide-maestroni on 04/06/2019.
 */
public class RespawningRoleRoleTest {

  @Test
  public void respawningArgs() {
    TestRole.resetCount();
    final LocalRespawningRole role = new LocalRespawningRole(TestRole.class, 3);
    Stage.newActor(role).tell("test", Headers.EMPTY, Stage.STAND_IN);
    assertThat(TestRole.getCount()).isEqualTo(6);
  }

  @Test
  public void respawningClass() {
    TestRole.resetCount();
    final LocalRespawningRole role = new LocalRespawningRole(TestRole.class);
    Stage.newActor(role).tell("test", Headers.EMPTY, Stage.STAND_IN);
    assertThat(TestRole.getCount()).isEqualTo(2);
  }

  @SuppressWarnings("unused")
  public static class TestRole extends Role {

    private static final AtomicInteger count = new AtomicInteger();

    public TestRole() {
      this(1);
    }

    public TestRole(final int toAdd) {
      count.addAndGet(toAdd);
    }

    static int getCount() {
      return count.get();
    }

    static void resetCount() {
      count.set(0);
    }

    @NotNull
    public Behavior getBehavior(@NotNull final String id) {
      return new AbstractBehavior() {

        public void onMessage(final Object message, @NotNull final Envelop envelop,
            @NotNull final Agent agent) {
          agent.restartBehavior();
        }
      };
    }

    @NotNull
    @Override
    public ExecutorService getExecutorService(@NotNull final String id) {
      return ExecutorServices.localExecutor();
    }
  }

  private static class LocalRespawningRole extends RespawningRole {

    public LocalRespawningRole(@NotNull final Class<? extends Role> roleClass) {
      super(roleClass);
    }

    public LocalRespawningRole(@NotNull final Class<? extends Role> roleClass,
        @NotNull final Object... roleArgs) {
      super(roleClass, roleArgs);
    }

    @NotNull
    @Override
    public ExecutorService getExecutorService(@NotNull final String id) {
      return ExecutorServices.localExecutor();
    }
  }
}
