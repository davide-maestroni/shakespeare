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

import java.util.concurrent.ScheduledExecutorService;

/**
 * Specialized scheduled executor service implementation used by Actors to ensure that code
 * execution is sequential.
 */
public class ActorScheduledExecutorService extends ThrottledScheduledExecutorService {

  /**
   * Creates a new executor service wrapping the specified instance.
   *
   * @param executorService the executor service to wrap.
   */
  ActorScheduledExecutorService(@NotNull final ScheduledExecutorService executorService) {
    super(executorService, 1);
  }

  @Override
  public void executeNext(@NotNull final Runnable command) {
    super.executeNext(command);
  }
}
