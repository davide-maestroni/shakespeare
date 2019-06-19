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

package dm.shakespeare.template.typed;

import java.io.Serializable;

import dm.shakespeare.template.config.BuildConfig;
import dm.shakespeare.template.typed.background.Background;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 06/17/2019.
 */
class InvocationArg implements Serializable {

  private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

  private final Background background;
  private final Class<?> type;

  InvocationArg(final Class<?> type, final Background background) {
    this.type = ConstantConditions.notNull("type", type);
    this.background = ConstantConditions.notNull("background", background);
  }

  public Background getBackground() {
    return background;
  }

  public Class<?> getType() {
    return type;
  }
}