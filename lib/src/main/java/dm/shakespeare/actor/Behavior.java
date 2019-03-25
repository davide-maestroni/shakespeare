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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import dm.shakespeare.log.Logger;

/**
 * Created by davide-maestroni on 09/10/2018.
 */
public interface Behavior {

  void onMessage(Object message, @NotNull Envelop envelop, @NotNull Context context) throws
      Exception;

  void onStart(@NotNull Context context) throws Exception;

  void onStop(@NotNull Context context) throws Exception;

  interface Context {

    void dismissSelf();

    @NotNull
    ExecutorService getExecutorService();

    @NotNull
    Logger getLogger();

    @NotNull
    ScheduledExecutorService getScheduledExecutorService();

    @NotNull
    Actor getSelf();

    boolean isDismissed();

    void restartSelf();

    void setBehavior(@NotNull Behavior behavior);
  }
}
