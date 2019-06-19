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

package dm.shakespeare.template.typed.background;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import dm.shakespeare.actor.Role;
import dm.shakespeare.log.Logger;

/**
 * Created by davide-maestroni on 06/17/2019.
 */
public class Background {

  private static final long DEFAULT_TIMEOUT = TimeUnit.SECONDS.toMillis(20);

  @NotNull
  public ExecutorService getExecutorService(@NotNull final String id) throws Exception {
    return Role.defaultExecutorService();
  }

  @NotNull
  public Logger getLogger(@NotNull final String id) throws Exception {
    return Role.defaultLogger(this);
  }

  public int getQuota(@NotNull final String id) throws Exception {
    return Integer.MAX_VALUE;
  }

  public Long getTimeoutMillis(@NotNull final String id, @NotNull final Method method) throws
      Exception {
    final Class<?> returnType = method.getReturnType();
    return ((returnType != void.class) && (returnType != Void.class)) ? DEFAULT_TIMEOUT : null;
  }
}
