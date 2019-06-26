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

import dm.shakespeare.Stage;
import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Headers;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 04/10/2019.
 */
public class Spectator {

  private final Actor actor;

  Spectator(@NotNull final Actor actor) {
    this.actor = ConstantConditions.notNull("actor", actor);
  }

  void cancel(final boolean thenDismiss) {
    actor.tell(thenDismiss ? SpectatorSignal.CANCEL_AND_DISMISS : SpectatorSignal.CANCEL,
        Headers.EMPTY, Stage.STAND_IN);
  }

  void dismiss() {
    actor.dismiss();
  }

  void pause() {
    actor.tell(SpectatorSignal.PAUSE, Headers.EMPTY, Stage.STAND_IN);
  }

  void resume() {
    actor.tell(SpectatorSignal.RESUME, Headers.EMPTY, Stage.STAND_IN);
  }

  enum SpectatorSignal {
    CANCEL, CANCEL_AND_DISMISS, PAUSE, RESUME
  }
}
