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

import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Role;

/**
 * {@link dm.shakespeare.actor.Stage Stage} implementation managing local actor instances.
 */
public class LocalStage extends AbstractStage {

  @NotNull
  @Override
  protected Actor createActor(@NotNull final String id, @NotNull final Role role) throws Exception {
    return BackStage.createActor(id, role);
  }
}
