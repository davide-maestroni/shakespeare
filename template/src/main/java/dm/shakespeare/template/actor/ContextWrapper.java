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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Behavior.Context;
import dm.shakespeare.log.Logger;

/**
 * Created by davide-maestroni on 01/14/2019.
 */
public class ContextWrapper implements Context {

  private Context mContext;

  public void dismissSelf() {
    mContext.dismissSelf();
  }

  @NotNull
  public ExecutorService getExecutorService() {
    return mContext.getExecutorService();
  }

  @NotNull
  public Logger getLogger() {
    return mContext.getLogger();
  }

  @NotNull
  public ScheduledExecutorService getScheduledExecutorService() {
    return mContext.getScheduledExecutorService();
  }

  @NotNull
  public Actor getSelf() {
    return mContext.getSelf();
  }

  public boolean isDismissed() {
    return mContext.isDismissed();
  }

  public void restartSelf() {
    mContext.restartSelf();
  }

  public void setBehavior(@NotNull final Behavior behavior) {
    mContext.setBehavior(behavior);
  }

  @NotNull
  public ContextWrapper withContext(final Context context) {
    mContext = context;
    return this;
  }
}
