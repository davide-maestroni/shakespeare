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

package dm.shakespeare.remote.transport;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import dm.shakespeare.remote.util.SerializableData;

/**
 * Created by davide-maestroni on 04/09/2019.
 */
public class UploadRequest extends RemoteRequest {

  private static final long serialVersionUID = VERSION;

  private Map<String, SerializableData> resources;

  public Map<String, SerializableData> getResources() {
    return resources;
  }

  public void setResources(final Map<String, SerializableData> resources) {
    this.resources = resources;
  }

  @NotNull
  public UploadRequest putAllResources(
      @NotNull final Map<? extends String, ? extends SerializableData> resources) {
    if (this.resources == null) {
      this.resources = new HashMap<String, SerializableData>();
    }
    this.resources.putAll(resources);
    return this;
  }

  @NotNull
  public UploadRequest putResource(final String path, final SerializableData data) {
    if (resources == null) {
      resources = new HashMap<String, SerializableData>();
    }
    resources.put(path, data);
    return this;
  }

  @NotNull
  public UploadRequest withResources(final Map<String, SerializableData> resources) {
    this.resources = resources;
    return this;
  }

  @NotNull
  @Override
  public UploadRequest withSenderId(final String senderId) {
    super.withSenderId(senderId);
    return this;
  }
}
