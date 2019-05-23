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
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 04/10/2019.
 */
public class Spectator {

  static final Object CANCEL = new Object();
  static final Object CANCEL_AND_DISMISS = new Object();
  static final Object PAUSE = new Object();
  static final Object RESUME = new Object();

  private final Actor actor;

  Spectator(@NotNull final Actor actor) {
    this.actor = ConstantConditions.notNull("actor", actor);
  }

  void cancel(final boolean thenDismiss) {
    actor.tell(thenDismiss ? CANCEL_AND_DISMISS : CANCEL, null, Stage.STAND_IN);
  }

  void dismiss() {
    actor.dismiss(false);
  }

  void pause() {
    actor.tell(PAUSE, null, Stage.STAND_IN);
  }

  void resume() {
    actor.tell(RESUME, null, Stage.STAND_IN);
  }
}
