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
import dm.shakespeare.template.util.Reflections;
import dm.shakespeare.util.ConstantConditions;

/**
 * {@code Script} implementation instantiating the role object via reflection.
 */
public class ClassScript extends SerializableScript {

  private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

  private final Object[] roleArgs;
  private final Class<?> roleType;

  /**
   * Creates a new script instantiating a role of the specified type with the specified arguments.
   *
   * @param roleType the type of the role object to be instantiated.
   * @param roleArgs the role object constructor arguments.
   */
  public ClassScript(@NotNull final Class<?> roleType, @NotNull final Object... roleArgs) {
    this.roleType = ConstantConditions.notNull("roleType", roleType);
    this.roleArgs = ConstantConditions.notNull("args", roleArgs).clone();
  }

  /**
   * Creates a dummy script.<br>
   * Usually needed during deserialization.
   */
  private ClassScript() {
    this.roleType = Object.class;
    this.roleArgs = new Object[0];
  }

  /**
   * {@inheritDoc}
   */
  @NotNull
  public Object getRole(@NotNull final String id) throws Exception {
    return Reflections.newInstance(roleType, roleArgs);
  }

  /**
   * Returns a copy of the constructor arguments array.<br>
   * Usually needed during serialization.
   *
   * @return the role constructor arguments.
   */
  @NotNull
  public Object[] getRoleArgs() {
    return roleArgs.clone();
  }

  /**
   * Returns the type of the role to be instantiated.<br>
   * Usually needed during serialization.
   *
   * @return the role type.
   */
  @NotNull
  public Class<?> getRoleType() {
    return roleType;
  }
}
