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

import dm.shakespeare.message.Bounce;
import dm.shakespeare.plot.config.BuildConfig;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 01/25/2019.
 */
class Conflict implements Serializable {

  private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

  private final Throwable incident;

  Conflict(@NotNull final Throwable incident) {
    this.incident = ConstantConditions.notNull("incident", incident);
  }

  private Conflict() {
    this.incident = null;
  }

  @NotNull
  static Conflict ofBounce(@NotNull final Bounce message) {
    return new Conflict(PlotFailureException.getOrNew(message));
  }

  @NotNull
  static Conflict ofCancel() {
    return new Conflict(new PlotCancellationException());
  }

  @NotNull
  public Throwable getIncident() {
    return incident;
  }
}
