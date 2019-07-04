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

package dm.shakespeare.typed;

import java.io.Serializable;

import dm.shakespeare.typed.actor.Script;
import dm.shakespeare.typed.config.BuildConfig;
import dm.shakespeare.util.ConstantConditions;

/**
 * Invocation argument used to transfer typed actor references.
 */
class InvocationArg implements Serializable {

  private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

  private final Script script;
  private final Class<?> type;

  InvocationArg(final Class<?> type, final Script script) {
    this.type = ConstantConditions.notNull("type", type);
    this.script = ConstantConditions.notNull("script", script);
  }

  private InvocationArg() {
    type = null;
    script = null;
  }

  public Script getScript() {
    return script;
  }

  public Class<?> getType() {
    return type;
  }
}