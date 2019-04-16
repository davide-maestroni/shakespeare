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

package dm.shakespeare.remote;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare.remote.protocol.Remote;

/**
 * Created by davide-maestroni on 04/16/2019.
 */
public interface Connector {

  @NotNull
  Sender connect(@NotNull Receiver receiver);

  interface Receiver {

    void receive(@NotNull Remote remote) throws Exception;
  }

  interface Sender {

    void disconnect();

    void send(@NotNull Iterable<? extends Remote> remotes, @NotNull String receiverId) throws
        Exception;

    void send(@NotNull Remote remote, @NotNull String receiverId) throws Exception;
  }
}
