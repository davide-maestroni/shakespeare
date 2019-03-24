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

package dm.shakespeare.actor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.RejectedExecutionException;

/**
 * Created by davide-maestroni on 01/17/2019.
 */
public abstract class AbstractBehavior implements Behavior {

  protected static void safeTell(@NotNull final Actor actor, final Object message,
      @Nullable final Options options, @NotNull final Context context) {
    try {
      actor.tell(message, options, context.getSelf());

    } catch (final RejectedExecutionException e) {
      context.getLogger().err(e, "ignoring exception");
    }
  }

  public void onStart(@NotNull final Context context) throws Exception {
  }

  public void onStop(@NotNull final Context context) throws Exception {
  }
}
