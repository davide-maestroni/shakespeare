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

import dm.shakespeare.actor.Headers;
import dm.shakespeare.remote.io.RawData;

/**
 * Created by davide-maestroni on 04/09/2019.
 */
public class MessageRequest extends RemoteRequest {

  private static final long serialVersionUID = VERSION;

  private ActorID actorID;
  private Headers headers;
  private RawData messageData;
  private Map<String, RawData> resources;
  private ActorID senderActorID;
  private long sentTimestamp = System.currentTimeMillis();

  @NotNull
  public MessageResponse buildResponse() {
    return new MessageResponse();
  }

  @NotNull
  @Override
  public MessageRequest withSenderId(final String senderId) {
    super.withSenderId(senderId);
    return this;
  }

  public ActorID getActorID() {
    return actorID;
  }

  public void setActorID(final ActorID actorID) {
    this.actorID = actorID;
  }

  public Headers getHeaders() {
    return headers;
  }

  public void setHeaders(final Headers headers) {
    this.headers = headers;
  }

  public RawData getMessageData() {
    return messageData;
  }

  public void setMessageData(final RawData messageData) {
    this.messageData = messageData;
  }

  public Map<String, RawData> getResources() {
    return resources;
  }

  public void setResources(final Map<String, RawData> resources) {
    this.resources = resources;
  }

  public ActorID getSenderActorID() {
    return senderActorID;
  }

  public void setSenderActorID(final ActorID senderActorID) {
    this.senderActorID = senderActorID;
  }

  public long getSentTimestamp() {
    return sentTimestamp;
  }

  public void setSentTimestamp(final long sentTimestamp) {
    this.sentTimestamp = sentTimestamp;
  }

  @NotNull
  public MessageRequest putAllResources(
      @NotNull final Map<? extends String, ? extends RawData> resources) {
    if (this.resources == null) {
      this.resources = new HashMap<String, RawData>();
    }
    this.resources.putAll(resources);
    return this;
  }

  @NotNull
  public MessageRequest putResource(final String path, final RawData data) {
    if (resources == null) {
      resources = new HashMap<String, RawData>();
    }
    resources.put(path, data);
    return this;
  }

  @NotNull
  public MessageRequest withActorID(final ActorID actorID) {
    this.actorID = actorID;
    return this;
  }

  @NotNull
  public MessageRequest withHeaders(final Headers headers) {
    this.headers = headers;
    return this;
  }

  @NotNull
  public MessageRequest withMessageData(final RawData messageData) {
    this.messageData = messageData;
    return this;
  }

  @NotNull
  public MessageRequest withResources(final Map<String, RawData> resources) {
    this.resources = resources;
    return this;
  }

  @NotNull
  public MessageRequest withSenderActorID(final ActorID senderActorID) {
    this.senderActorID = senderActorID;
    return this;
  }

  @NotNull
  public MessageRequest withSentTimestamp(final long sentTimestamp) {
    this.sentTimestamp = sentTimestamp;
    return this;
  }
}
