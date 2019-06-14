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

import java.io.Serializable;
import java.util.concurrent.ExecutorService;

import dm.shakespeare.actor.Role;
import dm.shakespeare.log.Logger;
import dm.shakespeare.plot.config.BuildConfig;

/**
 * Created by davide-maestroni on 06/12/2019.
 */
public class Script implements Serializable {

  // TODO: 15/02/2019 serialization?
  // TODO: 28/02/2019 swagger converter

  private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

  @NotNull
  public ExecutorService getExecutorService() throws Exception {
    return Role.defaultExecutorService();
  }

  @NotNull
  public Logger getLogger() throws Exception {
    return Role.defaultLogger(this);
  }
}
