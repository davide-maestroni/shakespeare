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

import dm.shakespeare.function.Tester;

/**
 * Annotation used to decorate a method handling messages matching a specific class or condition.
 * <br>
 * The annotate method may accept also parameters of type {@link dm.shakespeare.actor.Envelop
 * Envelop} and {@link dm.shakespeare.actor.Behavior.Agent Agent}. Such inputs will be injected int
 * the received objects so to match the parameters order.
 *
 * @see dm.shakespeare.template.behavior.AnnotationBehavior
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OnMessage {

  /**
   * Returns the classes of the matched messages.
   *
   * @return the message classes.
   */
  Class<?>[] messageClasses() default {};

  /**
   * Returns the class of the tester to instantiate.
   *
   * @return the tester class.
   */
  Class<? extends Tester<?>> testerClass() default VoidTester.class;

  /**
   * Returns the name of the method to be used as message tester.<br>
   * The method must accept a single parameter assignable from a message instance and a boolean
   * return type.
   *
   * @return the method name.
   */
  String testerName() default "";
}
