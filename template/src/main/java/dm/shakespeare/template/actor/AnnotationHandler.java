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

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import dm.shakespeare.actor.BehaviorBuilder;

/**
 * Created by davide-maestroni on 09/07/2018.
 */
interface AnnotationHandler<T extends Annotation> extends Serializable {

  void handle(@NotNull BehaviorBuilder builder, @NotNull Object object, @NotNull Method method,
      @NotNull T annotation) throws Exception;
}
