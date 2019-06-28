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
import dm.shakespeare.actor.SerializableRole;
import dm.shakespeare.template.behavior.AnnotationBehavior;
import dm.shakespeare.template.config.BuildConfig;
import dm.shakespeare.util.ConstantConditions;

/**
 * Implementation of a {@link dm.shakespeare.actor.Role} wrapping an object whose methods has
 * been decorated so to act as handlers of messages.
 *
 * @see AnnotationBehavior
 */
public class AnnotatedRole extends SerializableRole {

  private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

  private final Object object;

  /**
   * Creates a new role instance.<br>
   * By default use itself as decorated object.
   */
  public AnnotatedRole() {
    object = this;
  }

  /**
   * Creates a new role instance wrapping the specified annotated object.
   *
   * @param object the object instance.
   */
  public AnnotatedRole(@NotNull final Object object) {
    this.object = ConstantConditions.notNull("object", object);
  }

  /**
   * Returns the wrapped object.<br>
   * Usually needed during serialization.
   *
   * @return the object instance.
   */
  @NotNull
  public Object getObject() {
    return object;
  }

  /**
   * {@inheritDoc}
   */
  @NotNull
  protected Behavior getSerializableBehavior(@NotNull final String id) throws Exception {
    return new AnnotationBehavior(object);
  }
}
