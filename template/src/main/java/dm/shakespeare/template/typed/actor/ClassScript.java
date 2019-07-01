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
 * Created by davide-maestroni on 06/29/2019.
 */
public class ClassScript extends SerializableScript {

  private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

  private final Object[] roleArgs;
  private final Class<?> roleType;

  /**
   *
   *
   * @param roleType the type of the role object to be instantiated.
   * @param roleArgs the role object constructor arguments.
   */
  public ClassScript(@NotNull final Class<?> roleType, @NotNull final Object... roleArgs) {
    this.roleType = ConstantConditions.notNull("roleType", roleType);
    this.roleArgs = ConstantConditions.notNull("args", roleArgs).clone();
  }

  private ClassScript() {
    this.roleType = Object.class;
    this.roleArgs = new Object[0];
  }

  @NotNull
  public Object getRole(@NotNull final String id) throws Exception {
    return Reflections.newInstance(roleType, roleArgs);
  }

  @NotNull
  public Object[] getRoleArgs() {
    return roleArgs.clone();
  }

  @NotNull
  public Class<?> getRoleType() {
    return roleType;
  }
}
