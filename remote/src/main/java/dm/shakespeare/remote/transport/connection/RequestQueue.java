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

package dm.shakespeare.remote.transport.connection;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 08/09/2019.
 */
class RequestQueue {

  private final long expirationMillis;
  private final int maxSize;
  private final Object mutex = new Object();
  private final LinkedList<TimedRequest> queue = new LinkedList<TimedRequest>();

  RequestQueue(final int maxSize, final long expiration, @NotNull final TimeUnit unit) {
    this.maxSize = ConstantConditions.notNegative("maxSize", maxSize);
    this.expirationMillis = unit.toMillis(ConstantConditions.notNegative("expiration", expiration));
  }

  @NotNull
  List<ServerRequest> dequeue(final String clientId, final int maxLength) {
    if (maxLength > 0) {
      final ArrayList<ServerRequest> requests = new ArrayList<ServerRequest>();
      synchronized (mutex) {
        final long minTimeMillis = System.currentTimeMillis() - expirationMillis;
        final Iterator<TimedRequest> iterator = queue.iterator();
        if (clientId != null) {
          while ((requests.size() < maxLength) && iterator.hasNext()) {
            final TimedRequest timedRequest = iterator.next();
            if (timedRequest.getTime() < minTimeMillis) {
              iterator.remove();
              continue;
            }

            final String requestClientId = timedRequest.getClientId();
            if (clientId.equals(requestClientId)) {
              requests.add(timedRequest.getRequest());
              iterator.remove();
            }
          }

        } else {
          while ((requests.size() < maxLength) && iterator.hasNext()) {
            final TimedRequest timedRequest = iterator.next();
            if (timedRequest.getTime() < minTimeMillis) {
              iterator.remove();
              continue;
            }

            final String requestClientId = timedRequest.getClientId();
            if (requestClientId == null) {
              requests.add(timedRequest.getRequest());
              iterator.remove();
            }
          }
        }
      }
      return requests;
    }
    return Collections.emptyList();
  }

  void enqueue(final String clientId, @NotNull final ServerRequest request) {
    synchronized (mutex) {
      queue.add(new TimedRequest(clientId, ConstantConditions.notNull("request", request)));
      innerPurge();
    }
  }

  void purge() {
    synchronized (mutex) {
      innerPurge();
    }
  }

  private void innerPurge() {
    final LinkedList<TimedRequest> queue = this.queue;
    final long minTimeMillis = System.currentTimeMillis() - expirationMillis;
    final Iterator<TimedRequest> iterator = queue.iterator();
    int toRemove = Math.max(0, queue.size() - maxSize);
    while (iterator.hasNext()) {
      final TimedRequest timedRequest = iterator.next();
      if ((timedRequest.getTime() < minTimeMillis) || (toRemove > 0)) {
        iterator.remove();
        --toRemove;

      } else {
        break;
      }
    }
  }

  private static class TimedRequest {

    private final String clientId;
    private final ServerRequest request;
    private final long timestamp = System.currentTimeMillis();

    private TimedRequest(final String clientId, @NotNull final ServerRequest request) {
      this.clientId = clientId;
      this.request = request;
    }

    String getClientId() {
      return clientId;
    }

    @NotNull
    ServerRequest getRequest() {
      return request;
    }

    long getTime() {
      return timestamp;
    }
  }
}
