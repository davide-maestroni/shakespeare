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

import java.util.concurrent.ExecutorService;

import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.SerializableAbstractBehavior;
import dm.shakespeare.actor.SerializableRole;
import dm.shakespeare.concurrent.ExecutorServices;

/**
 * Dummy role implementation.
 */
class DummyRole extends SerializableRole {

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
    };
  }
}
