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

import java.io.Serializable;
import java.util.List;

import dm.shakespeare.remote.config.BuildConfig;
import dm.shakespeare.remote.transport.message.RemoteResponse;

/**
 * Created by davide-maestroni on 08/09/2019.
 */
public class ClientResponse implements Serializable {

  private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

  private List<ServerRequest> requests;
  private RemoteResponse response;

  public List<ServerRequest> getRequests() {
    return requests;
  }

  public void setRequests(final List<ServerRequest> requests) {
    this.requests = requests;
  }

  public RemoteResponse getResponse() {
    return response;
  }

  public void setResponse(final RemoteResponse response) {
    this.response = response;
  }

  @NotNull
  public ClientResponse withRequests(final List<ServerRequest> requests) {
    this.requests = requests;
    return this;
  }

  @NotNull
  public ClientResponse withResponse(final RemoteResponse response) {
    this.response = response;
    return this;
  }
}
