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
import org.jetbrains.annotations.Nullable;

/**
 * Created by davide-maestroni on 01/08/2019.
 */
public interface Actor {

  @NotNull
  Actor addObserver(@NotNull Actor observer);

  void dismiss(boolean mayInterruptIfRunning);

  @NotNull
  String getId();

  @NotNull
  Actor removeObserver(@NotNull Actor observer);

  @NotNull
  Actor tell(Object message, @Nullable Options options, @NotNull Actor sender);

  @NotNull
  Actor tellAll(@NotNull Iterable<?> messages, @Nullable Options options, @NotNull Actor sender);
}
