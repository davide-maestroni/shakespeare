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

package dm.shakespeare.remote.protocol;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import dm.shakespeare.remote.util.SerializableData;

/**
 * Created by davide-maestroni on 04/09/2019.
 */
public class CreateActorRequest extends RemoteRecipient {

  private static final long serialVersionUID = VERSION;

  private Map<String, SerializableData> mResources;
  private SerializableData mRoleData;

  public Map<String, SerializableData> getResources() {
    return mResources;
  }

  public void setResources(final Map<String, SerializableData> resources) {
    mResources = resources;
  }

  public SerializableData getRoleData() {
    return mRoleData;
  }

  public void setRoleData(final SerializableData roleData) {
    mRoleData = roleData;
  }

  @NotNull
  public CreateActorRequest putAllResources(
      @NotNull final Map<? extends String, ? extends SerializableData> resources) {
    if (mResources == null) {
      mResources = new HashMap<String, SerializableData>();
    }
    mResources.putAll(resources);
    return this;
  }

  @NotNull
  public CreateActorRequest putResource(final String path, final SerializableData data) {
    if (mResources == null) {
      mResources = new HashMap<String, SerializableData>();
    }
    mResources.put(path, data);
    return this;
  }

  @NotNull
  @Override
  public CreateActorRequest withRecipientRef(final ActorRef recipientRef) {
    super.withRecipientRef(recipientRef);
    return this;
  }

  @NotNull
  @Override
  public CreateActorRequest withSenderId(final String senderId) {
    super.withSenderId(senderId);
    return this;
  }

  @NotNull
  public CreateActorRequest withResources(final Map<String, SerializableData> resources) {
    mResources = resources;
    return this;
  }

  @NotNull
  public CreateActorRequest withRoleData(final SerializableData roleData) {
    mRoleData = roleData;
    return this;
  }
}
