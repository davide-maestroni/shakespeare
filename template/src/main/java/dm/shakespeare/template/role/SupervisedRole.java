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
import dm.shakespeare.actor.Role;
import dm.shakespeare.actor.SerializableRole;
import dm.shakespeare.log.Logger;
import dm.shakespeare.template.behavior.SupervisedBehavior;
import dm.shakespeare.template.config.BuildConfig;
import dm.shakespeare.util.ConstantConditions;

/**
 * Implementation of a {@link dm.shakespeare.actor.Role} adding supervision functionalities to
 * another role.
 *
 * @see SupervisedBehavior
 */
public class SupervisedRole extends SerializableRole {

  private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

  private final Role role;

  /**
   * Creates a new role wrapping the specified one.
   *
   * @param role the wrapped role.
   */
  public SupervisedRole(@NotNull final Role role) {
    this.role = ConstantConditions.notNull("role", role);
  }

  /**
   * Creates a dummy role instance.<br>
   * Usually needed during deserialization.
   */
  private SupervisedRole() {
    role = null;
  }

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  public ExecutorService getExecutorService(@NotNull final String id) throws Exception {
    return role.getExecutorService(id);
  }

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  public Logger getLogger(@NotNull final String id) throws Exception {
    return role.getLogger(id);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getQuota(@NotNull final String id) throws Exception {
    return role.getQuota(id);
  }

  /**
   * Returns the wrapped role.<br>
   * Usually needed during serialization.
   *
   * @return the role instance.
   */
  public Role getRole() {
    return role;
  }

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  public Behavior getSerializableBehavior(@NotNull final String id) throws Exception {
    return new SupervisedBehavior(role.getBehavior(id));
  }
}
