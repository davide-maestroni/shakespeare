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

import dm.shakespeare.actor.Options;
import dm.shakespeare.remote.util.SerializableData;

/**
 * Created by davide-maestroni on 04/09/2019.
 */
public class RemoteMessage extends RemoteRecipient {

  private static final long serialVersionUID = VERSION;

  private SerializableData mMessageData;
  private Options mOptions;
  private Map<String, SerializableData> mResources;
  private ActorRef mSenderRef;
  private long mSentTimestamp = System.currentTimeMillis();

  public SerializableData getMessageData() {
    return mMessageData;
  }

  public void setMessageData(final SerializableData messageData) {
    mMessageData = messageData;
  }

  public Options getOptions() {
    return mOptions;
  }

  public void setOptions(final Options options) {
    mOptions = options;
  }

  public Map<String, SerializableData> getResources() {
    return mResources;
  }

  public void setResources(final Map<String, SerializableData> resources) {
    mResources = resources;
  }

  public ActorRef getSenderRef() {
    return mSenderRef;
  }

  public void setSenderRef(final ActorRef senderRef) {
    mSenderRef = senderRef;
  }

  public long getSentTimestamp() {
    return mSentTimestamp;
  }

  public void setSentTimestamp(final long sentTimestamp) {
    mSentTimestamp = sentTimestamp;
  }

  @NotNull
  public RemoteMessage putAllResources(
      @NotNull final Map<? extends String, ? extends SerializableData> resources) {
    if (mResources == null) {
      mResources = new HashMap<String, SerializableData>();
    }
    mResources.putAll(resources);
    return this;
  }

  @NotNull
  public RemoteMessage putResource(final String path, final SerializableData data) {
    if (mResources == null) {
      mResources = new HashMap<String, SerializableData>();
    }
    mResources.put(path, data);
    return this;
  }

  @NotNull
  public RemoteMessage withMessageData(final SerializableData message) {
    mMessageData = message;
    return this;
  }

  @NotNull
  public RemoteMessage withOptions(final Options options) {
    mOptions = options;
    return this;
  }

  @NotNull
  @Override
  public RemoteMessage withRecipientRef(final ActorRef recipientRef) {
    super.withRecipientRef(recipientRef);
    return this;
  }

  @NotNull
  @Override
  public RemoteMessage withSenderId(final String senderId) {
    super.withSenderId(senderId);
    return this;
  }

  @NotNull
  public RemoteMessage withResources(final Map<String, SerializableData> resources) {
    mResources = resources;
    return this;
  }

  @NotNull
  public RemoteMessage withSenderRef(final ActorRef senderRef) {
    mSenderRef = senderRef;
    return this;
  }

  @NotNull
  public RemoteMessage withSentTimestamp(final long sentTimestamp) {
    mSentTimestamp = sentTimestamp;
    return this;
  }
}
