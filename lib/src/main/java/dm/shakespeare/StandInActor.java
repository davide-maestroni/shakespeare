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

import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Headers;
import dm.shakespeare.message.Delivery;
import dm.shakespeare.util.ConstantConditions;

/**
 * Class implementing an actor reacting in no way to the sent messages.
 */
class StandInActor implements Actor {

  public boolean addObserver(@NotNull final Actor observer) {
    ConstantConditions.notNull("observer", observer);
    return true;
  }

  public boolean dismiss() {
    return true;
  }

  public boolean dismissLazy() {
    return true;
  }

  public boolean dismissNow() {
    return true;
  }

  @NotNull
  public String getId() {
    return getClass().getName();
  }

  public boolean removeObserver(@NotNull final Actor observer) {
    ConstantConditions.notNull("observer", observer);
    return true;
  }

  public void tell(final Object message, @NotNull final Headers headers,
      @NotNull final Actor sender) {
    if (headers.getReceiptId() != null) {
      sender.tell(new Delivery(message, headers), headers.threadOnly(), this);

    } else {
      ConstantConditions.notNull("sender", sender);
    }
  }

  public void tellAll(@NotNull final Iterable<?> messages, @NotNull final Headers headers,
      @NotNull final Actor sender) {
    for (final Object message : messages) {
      tell(message, headers, sender);
    }
    ConstantConditions.notNull("headers", headers);
    ConstantConditions.notNull("sender", sender);
  }
}
