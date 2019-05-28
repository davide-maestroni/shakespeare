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

import dm.shakespeare.actor.Options;
import dm.shakespeare.remote.util.SerializableData;

/**
 * Created by davide-maestroni on 04/09/2019.
 */
public class MessageRequest extends RemoteRequest {

  private static final long serialVersionUID = VERSION;

  private ActorUUID actorUUID;
  private SerializableData messageData;
  private Options options;
  private Map<String, SerializableData> resources;
  private long sentTimestamp = System.currentTimeMillis();

  public ActorUUID getActorUUID() {
    return actorUUID;
  }

  public void setActorUUID(final ActorUUID actorUUID) {
    this.actorUUID = actorUUID;
  }

  public SerializableData getMessageData() {
    return messageData;
  }

  public void setMessageData(final SerializableData messageData) {
    this.messageData = messageData;
  }

  public Options getOptions() {
    return options;
  }

  public void setOptions(final Options options) {
    this.options = options;
  }

  public Map<String, SerializableData> getResources() {
    return resources;
  }

  public void setResources(final Map<String, SerializableData> resources) {
    this.resources = resources;
  }

  public long getSentTimestamp() {
    return sentTimestamp;
  }

  public void setSentTimestamp(final long sentTimestamp) {
    this.sentTimestamp = sentTimestamp;
  }

  @NotNull
  public MessageRequest putAllResources(
      @NotNull final Map<? extends String, ? extends SerializableData> resources) {
    if (this.resources == null) {
      this.resources = new HashMap<String, SerializableData>();
    }
    this.resources.putAll(resources);
    return this;
  }

  @NotNull
  public MessageRequest putResource(final String path, final SerializableData data) {
    if (resources == null) {
      resources = new HashMap<String, SerializableData>();
    }
    resources.put(path, data);
    return this;
  }

  @NotNull
  public MessageRequest withActorId(final ActorUUID actorUUID) {
    this.actorUUID = actorUUID;
    return this;
  }

  @NotNull
  public MessageRequest withMessageData(final SerializableData messageData) {
    this.messageData = messageData;
    return this;
  }

  @NotNull
  public MessageRequest withOptions(final Options options) {
    this.options = options;
    return this;
  }

  @NotNull
  public MessageRequest withResources(final Map<String, SerializableData> resources) {
    this.resources = resources;
    return this;
  }

  @NotNull
  @Override
  public MessageRequest withSenderId(final String senderId) {
    super.withSenderId(senderId);
    return this;
  }

  @NotNull
  public MessageRequest withSentTimestamp(final long sentTimestamp) {
    this.sentTimestamp = sentTimestamp;
    return this;
  }
}
