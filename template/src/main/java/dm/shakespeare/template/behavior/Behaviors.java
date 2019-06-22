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

import java.lang.ref.WeakReference;

import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 03/24/2019.
 */
public class Behaviors {

  private Behaviors() {
    ConstantConditions.avoid();
  }

  @NotNull
  public static Behavior annotated(@NotNull final Object object) throws Exception {
    return new AnnotationBehavior(object);
  }

  @NotNull
  public static ProxyBehavior proxy(@NotNull final Actor actor) {
    return new ProxyBehavior(new WeakReference<Actor>(ConstantConditions.notNull("actor", actor)));
  }

  @NotNull
  public static SupervisedBehavior supervised(@NotNull final Behavior behavior) {
    return new SupervisedBehavior(behavior);
  }
}
