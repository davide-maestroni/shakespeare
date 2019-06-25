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
import java.util.Map.Entry;
import java.util.WeakHashMap;

import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.Headers;
import dm.shakespeare.actor.SerializableAbstractBehavior;
import dm.shakespeare.template.config.BuildConfig;

/**
 * Created by davide-maestroni on 06/25/2019.
 */
public class TopicBehavior extends SerializableAbstractBehavior {

  private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

  private transient final WeakHashMap<Actor, Subscribe> subscriptions =
      new WeakHashMap<Actor, Subscribe>();

  public void onMessage(final Object message, @NotNull final Envelop envelop,
      @NotNull final Agent agent) {
    if (message instanceof Subscribe) {
      subscriptions.put(envelop.getSender(), (Subscribe) message);

    } else if (message instanceof Unsubscribe) {
      subscriptions.remove(envelop.getSender());

    } else {
      final Actor self = agent.getSelf();
      final Headers headers = envelop.getHeaders().threadOnly();
      for (final Entry<Actor, Subscribe> entry : subscriptions.entrySet()) {
        try {
          if (entry.getValue().accept(message, envelop)) {
            entry.getKey().tell(message, headers, self);
          }

        } catch (final Exception e) {
          agent.getLogger().wrn(e, "subscription error");
        }
      }
    }
  }

  public static class Subscribe implements Serializable {

    private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

    public boolean accept(final Object message, @NotNull final Envelop envelop) throws Exception {
      return true;
    }
  }

  public static class Unsubscribe implements Serializable {

    private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;
  }
}
