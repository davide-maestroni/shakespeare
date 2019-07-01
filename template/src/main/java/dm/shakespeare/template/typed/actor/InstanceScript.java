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

package dm.shakespeare.template.typed.actor;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare.template.config.BuildConfig;
import dm.shakespeare.util.ConstantConditions;

/**
 * {@code Script} implementation wrapping a role instance.
 */
public class InstanceScript extends SerializableScript {

  private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

  private final Object role;

  /**
   * Creates a new script wrapping the specified role instance.
   *
   * @param role the role instance.
   */
  public InstanceScript(@NotNull final Object role) {
    this.role = ConstantConditions.notNull("role", role);
  }

  /**
   * Creates a dummy script.<br>
   * Usually needed during deserialization.
   */
  private InstanceScript() {
    role = new Object();
  }

  /**
   * Returns the wrapped role instance.<br>
   * Usually needed during serialization.
   *
   * @return the role instance.
   */
  @NotNull
  public Object getRole() {
    return role;
  }

  /**
   * {@inheritDoc}
   */
  @NotNull
  public Object getRole(@NotNull final String id) throws Exception {
    return role;
  }
}
