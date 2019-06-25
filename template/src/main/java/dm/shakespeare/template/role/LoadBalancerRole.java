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
import dm.shakespeare.template.behavior.LoadBalancerBehavior;
import dm.shakespeare.template.config.BuildConfig;

/**
 * Created by davide-maestroni on 06/25/2019.
 */
public class LoadBalancerRole extends SerializableRole {

  private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

  @NotNull
  protected Behavior getSerializableBehavior(@NotNull final String id) {
    return new LoadBalancerBehavior();
  }
}
