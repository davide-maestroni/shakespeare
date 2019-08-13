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

import dm.shakespeare.remote.config.BuildConfig;
import dm.shakespeare.remote.transport.message.RemoteRequest;
import dm.shakespeare.remote.transport.message.RemoteResponse;

/**
 * Created by davide-maestroni on 08/09/2019.
 */
public class ServerResponse extends ClientRequest {

  private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

  private String requestId;
  private RemoteResponse response;

  public String getRequestId() {
    return requestId;
  }

  public void setRequestId(final String requestId) {
    this.requestId = requestId;
  }

  public RemoteResponse getResponse() {
    return response;
  }

  public void setResponse(final RemoteResponse response) {
    this.response = response;
  }

  @NotNull
  public ServerResponse withMaxIncludedRequests(final int maxIncludedRequests) {
    super.withMaxIncludedRequests(maxIncludedRequests);
    return this;
  }

  @NotNull
  public ServerResponse withRequest(final RemoteRequest request) {
    super.withRequest(request);
    return this;
  }

  @NotNull
  public ServerResponse withRequestId(final String requestId) {
    this.requestId = requestId;
    return this;
  }

  @NotNull
  public ServerResponse withResponse(final RemoteResponse response) {
    this.response = response;
    return this;
  }
}
