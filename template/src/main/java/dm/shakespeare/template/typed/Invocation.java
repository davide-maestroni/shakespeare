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

package dm.shakespeare.template.typed;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

import dm.shakespeare.template.config.BuildConfig;

/**
 * Created by davide-maestroni on 06/17/2019.
 */
class Invocation implements Serializable {

  private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

  private final Object[] arguments;
  private final String id;
  private final String methodName;
  private final Class<?>[] parameterTypes;

  Invocation() {
    this.id = "";
    methodName = "toString";
    parameterTypes = new Class[0];
    arguments = new Object[0];
  }

  Invocation(@NotNull final String id, @NotNull final String methodName,
      @NotNull final Class<?>[] parameterTypes, @NotNull final Object... arguments) {
    this.id = id;
    this.methodName = methodName;
    this.parameterTypes = parameterTypes;
    this.arguments = arguments;
  }

  @NotNull
  public Object[] getArguments() {
    return arguments;
  }

  @NotNull
  public String getId() {
    return id;
  }

  @NotNull
  public String getMethodName() {
    return methodName;
  }

  @NotNull
  public Class<?>[] getParameterTypes() {
    return parameterTypes;
  }
}
