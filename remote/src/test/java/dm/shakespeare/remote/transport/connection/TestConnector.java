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
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import dm.shakespeare.remote.transport.message.RemoteRequest;
import dm.shakespeare.remote.transport.message.RemoteResponse;
import dm.shakespeare.remote.transport.connection.Connector;

/**
 * Created by davide-maestroni on 07/18/2019.
 */
public class TestConnector implements Connector {

  private final Map<String, Receiver> receivers =
      Collections.synchronizedMap(new HashMap<String, Receiver>());

  @NotNull
  public Sender connect(@NotNull final Receiver receiver) {
    return new Sender() {

      public void disconnect() {
      }

      @NotNull
      public RemoteResponse send(@NotNull final RemoteRequest request,
          @Nullable final String receiverId) throws Exception {
        return receivers.get(receiverId).receive(request);
      }
    };
  }

  @NotNull
  public Connector localConnector(@NotNull final Receiver localReceiver) {
    final String id = UUID.randomUUID().toString();
    return new Connector() {

      @NotNull
      public Sender connect(@NotNull final Receiver receiver) {
        receivers.put(id, receiver);
        return new Sender() {

          public void disconnect() {
          }

          @NotNull
          public RemoteResponse send(@NotNull final RemoteRequest request,
              @Nullable final String receiverId) throws Exception {
            return localReceiver.receive(request.withSenderId(id));
          }
        };
      }
    };
  }
}
