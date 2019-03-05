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

package dm.shakespeare.template.annotation;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare.actor.Behavior.Context;
import dm.shakespeare.actor.BehaviorBuilder.Matcher;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 09/06/2018.
 */
public class VoidMatcher implements Matcher<Object> {

  private VoidMatcher() {
    ConstantConditions.avoid();
  }

  public boolean match(final Object message, @NotNull final Envelop envelop,
      @NotNull final Context context) {
    return false;
  }
}
