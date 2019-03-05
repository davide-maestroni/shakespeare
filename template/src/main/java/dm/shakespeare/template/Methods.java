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

package dm.shakespeare.template;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 09/07/2018.
 */
class Methods {

  private Methods() {
    ConstantConditions.avoid();
  }

  /**
   * Makes the specified method accessible.
   *
   * @param method the method instance.
   * @return the method.
   */
  @NotNull
  public static Method makeAccessible(@NotNull final Method method) {
    if (!method.isAccessible()) {
      AccessController.doPrivileged(new SetAccessibleMethodAction(method));
    }
    return method;
  }

  /**
   * Privileged action used to grant accessibility to a method.
   */
  private static class SetAccessibleMethodAction implements PrivilegedAction<Void> {

    private final Method mMethod;

    /**
     * Constructor.
     *
     * @param method the method instance.
     */
    private SetAccessibleMethodAction(@NotNull final Method method) {
      mMethod = method;
    }

    public Void run() {
      mMethod.setAccessible(true);
      return null;
    }
  }
}
