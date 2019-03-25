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
import java.util.concurrent.ExecutorService;

import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Script;
import dm.shakespeare.actor.SerializableScript;
import dm.shakespeare.log.Logger;
import dm.shakespeare.template.config.BuildConfig;
import dm.shakespeare.template.util.Reflections;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 03/21/2019.
 */
public class FolderScript extends SerializableScript {

  private static final Object[] NO_ARGS = new Object[0];
  private static final long serialVersionUID = BuildConfig.VERSION_HASH_CODE;

  private final Serializable[] mScriptArgs;
  private final Class<? extends Script> mScriptClass;

  private transient Script mScript;

  public FolderScript(@NotNull final Class<? extends Script> scriptClass) {
    mScriptClass = ConstantConditions.notNull("scriptClass", scriptClass);
    mScriptArgs = null;
  }

  public FolderScript(@NotNull final Class<? extends Script> scriptClass,
      @NotNull final Serializable... scriptArgs) {
    mScriptClass = ConstantConditions.notNull("scriptClass", scriptClass);
    mScriptArgs = ConstantConditions.notNull("scriptArgs", scriptArgs).clone();
  }

  @NotNull
  public Behavior getBehavior(@NotNull final String id) throws Exception {
    return getScriptInstance().getBehavior(id);
  }

  @NotNull
  @Override
  public ExecutorService getExecutor(@NotNull final String id) throws Exception {
    return getScriptInstance().getExecutor(id);
  }

  @NotNull
  @Override
  public Logger getLogger(@NotNull final String id) throws Exception {
    return getScriptInstance().getLogger(id);
  }

  @Override
  public int getQuota(@NotNull final String id) throws Exception {
    return getScriptInstance().getQuota(id);
  }

  @NotNull
  protected Script newScriptInstance() {
    final Serializable[] scriptArgs = mScriptArgs;
    return Reflections.newInstance(mScriptClass,
        ((scriptArgs != null) && (scriptArgs.length > 0)) ? scriptArgs : NO_ARGS);
  }

  @NotNull
  private Script getScriptInstance() {
    if (mScript == null) {
      mScript = newScriptInstance();
    }
    return mScript;
  }
}
