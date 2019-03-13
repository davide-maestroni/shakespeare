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

package dm.shakespeare.concurrent;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;

/**
 * Interface defining an {@link ExecutorService} maintaining an internal queue of commands.
 */
public interface QueuedExecutorService extends ExecutorService {

  /**
   * Adds the specified command to the head of the internal queue so that it will be the next to be
   * executed.
   *
   * @param command the command.
   */
  void executeNext(@NotNull Runnable command);
}
