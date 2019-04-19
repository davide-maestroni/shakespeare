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

import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Role;
import dm.shakespeare.template.actor.Behaviors;
import dm.shakespeare.template.config.BuildConfig;

/**
 * Created by davide-maestroni on 01/16/2019.
 */
public class PoisonableRole extends SerializableRoleWrapper {

  private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

  public PoisonableRole(@NotNull final Role role) {
    super(role);
  }

  @NotNull
  @Override
  public Behavior getBehavior(@NotNull final String id) throws Exception {
    return Behaviors.poisonable(super.getBehavior(id));
  }
}
