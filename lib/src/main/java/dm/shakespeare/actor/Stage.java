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

package dm.shakespeare.actor;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

import dm.shakespeare.function.Tester;

/**
 * Created by davide-maestroni on 09/10/2018.
 */
public interface Stage {

  @NotNull
  ActorSet findAll(@NotNull Pattern idPattern);

  @NotNull
  ActorSet findAll(@NotNull Tester<? super Actor> tester);

  @NotNull
  Actor findAny(@NotNull Pattern idPattern);

  @NotNull
  Actor findAny(@NotNull Tester<? super Actor> tester);

  @NotNull
  Actor get(@NotNull String id);

  @NotNull
  ActorSet getAll();

  @NotNull
  Actor newActor(@NotNull Script script);

  @NotNull
  Actor newActor(@NotNull String id, @NotNull Script script);
}
