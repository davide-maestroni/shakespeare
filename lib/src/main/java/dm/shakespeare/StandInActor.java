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

package dm.shakespeare;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Headers;
import dm.shakespeare.actor.Headers;
import dm.shakespeare.message.Delivery;

/**
 * Class implementing an actor reacting in no way to the sent messages.
 */
class StandInActor implements Actor {

  @NotNull
  public Actor addObserver(@NotNull final Actor observer) {
    return this;
  }

  public void dismiss(final boolean mayInterruptIfRunning) {
  }

  @NotNull
  public String getId() {
    return getClass().getName();
  }

  @NotNull
  public Actor removeObserver(@NotNull final Actor observer) {
    return this;
  }

  @NotNull
  public Actor tell(final Object message, @Nullable final Headers headers,
      @NotNull final Actor sender) {
    if ((headers != null) && (headers.getReceiptId() != null)) {
      sender.tell(new Delivery(message, headers), headers.threadOnly(), this);
    }
    return this;
  }

  @NotNull
  public Actor tellAll(@NotNull final Iterable<?> messages, @Nullable final Headers headers,
      @NotNull final Actor sender) {
    for (final Object message : messages) {
      tell(message, headers, sender);
    }
    return this;
  }
}
