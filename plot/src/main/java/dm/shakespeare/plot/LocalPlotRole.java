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

package dm.shakespeare.plot;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;

import dm.shakespeare.actor.Role;
import dm.shakespeare.log.Logger;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 01/25/2019.
 */
abstract class LocalPlotRole extends Role {

  private final SettingInfo settingInfo;

  LocalPlotRole(@NotNull final SettingInfo settingInfo) {
    this.settingInfo = ConstantConditions.notNull("settingInfo", settingInfo);
  }

  @NotNull
  @Override
  public ExecutorService getExecutorService(@NotNull final String id) {
    return settingInfo.getLocalExecutor();
  }

  @NotNull
  @Override
  public Logger getLogger(@NotNull final String id) throws Exception {
    return settingInfo.getLogger();
  }
}
