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

package dm.shakespeare.template.behavior;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import dm.shakespeare.actor.BehaviorBuilder;

/**
 * Interface defining an annotation handler used to build {@link AnnotationBehavior} instances.
 */
interface AnnotationHandler<T extends Annotation> extends Serializable {

  /**
   * Handles a specific annotation.
   *
   * @param builder    the behavior builder instance.
   * @param object     the annotated object.
   * @param method     the annotated method.
   * @param annotation the annotation instance.
   * @throws Exception when an unexpected error occurs.
   */
  void handle(@NotNull BehaviorBuilder builder, @NotNull Object object, @NotNull Method method,
      @NotNull T annotation) throws Exception;
}
