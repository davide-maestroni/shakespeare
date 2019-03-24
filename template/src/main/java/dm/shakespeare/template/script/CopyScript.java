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

package dm.shakespeare.template.script;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.Script;
import dm.shakespeare.template.config.BuildConfig;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 03/21/2019.
 */
public class CopyScript extends FolderScript {

  // TODO: 24/03/2019 makes no sense

  private static final long serialVersionUID = BuildConfig.VERSION_HASH_CODE;

  public CopyScript(@NotNull final Class<? extends Script> scriptClass) {
    super(scriptClass);
  }

  public CopyScript(@NotNull final Class<? extends Script> scriptClass,
      @NotNull final Serializable... scriptArgs) {
    super(scriptClass, scriptArgs);
  }

  @NotNull
  public Behavior getBehavior(@NotNull final String id) throws Exception {
    return new CopyBehavior(super.getBehavior(id));
  }

  private class CopyBehavior implements Behavior {

    private final Behavior mBehavior;

    private boolean mIsStopped;

    private CopyBehavior(@NotNull final Behavior behavior) {
      mBehavior = ConstantConditions.notNull("behavior", behavior);
    }

    public void onMessage(final Object message, @NotNull final Envelop envelop,
        @NotNull final Context context) throws Exception {
      mBehavior.onMessage(message, envelop, context);
    }

    public void onStart(@NotNull final Context context) throws Exception {
      if (mIsStopped) {
        mIsStopped = false;
        resetScriptInstance();
      }
      mBehavior.onStart(context);
    }

    public void onStop(@NotNull final Context context) throws Exception {
      mIsStopped = true;
      mBehavior.onStop(context);
    }
  }
}
