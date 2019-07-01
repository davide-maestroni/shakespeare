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

package dm.shakespeare.template.behavior.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import dm.shakespeare.actor.BehaviorBuilder.Matcher;

/**
 * Annotation used to decorate a method handling messages matching a specific condition.<br>
 * The annotate method may accept also parameters of type {@link dm.shakespeare.actor.Envelop
 * Envelop} and {@link dm.shakespeare.actor.Behavior.Agent Agent}. Such inputs will be injected int
 * the received objects so to match the parameters order.<br>
 * The method returned value, if any, will be sent back to the sender of the message.
 *
 * @see dm.shakespeare.template.behavior.AnnotationBehavior
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OnMatch {

  /**
   * Returns the class of the matcher to instantiate.
   *
   * @return the matcher class.
   */
  Class<? extends Matcher<?>> matcherClass() default VoidMatcher.class;

  /**
   * Returns the name of the method to be used as message matcher.<br>
   * The method must accept a parameter assignable from a message instance, one assignable from
   * {@link dm.shakespeare.actor.Envelop Envelop}, one assignable from an
   * {@link dm.shakespeare.actor.Behavior.Agent Agent}, and a boolean return type.
   *
   * @return the method name.
   */
  String matcherName() default "";
}
