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

package dm.shakespeare.template.actor;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 03/24/2019.
 */
public class PoisonableBehavior implements Behavior {

  public static final Object POISON_PILL = new Object();

  private final ContextWrapper mContext;

  private Behavior mBehavior;

  PoisonableBehavior(@NotNull final Behavior behavior) {
    mBehavior = ConstantConditions.notNull("behavior", behavior);
    mContext = new ContextWrapper() {

      @Override
      public void setBehavior(@NotNull final Behavior behavior) {
        mBehavior = ConstantConditions.notNull("behavior", behavior);
      }
    };
  }

  public void onMessage(final Object message, @NotNull final Envelop envelop,
      @NotNull final Context context) throws Exception {
    if (message == POISON_PILL) {
      context.dismissSelf();
      return;
    }
    mBehavior.onMessage(message, envelop, mContext.withContext(context));
  }

  public void onStart(@NotNull final Context context) throws Exception {
    mBehavior.onStart(mContext.withContext(context));
  }

  public void onStop(@NotNull final Context context) throws Exception {
    mBehavior.onStop(mContext.withContext(context));
  }
}
