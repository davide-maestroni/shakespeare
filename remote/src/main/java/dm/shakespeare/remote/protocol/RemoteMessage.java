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

import dm.shakespeare.actor.Options;

/**
 * Created by davide-maestroni on 04/09/2019.
 */
public class RemoteMessage extends Remote {

  private ActorRef mActor;
  private Object mMessage;
  private Options mOptions;
  private String mSenderId;
  private long mSentTimestamp = System.currentTimeMillis();

  public ActorRef getActor() {
    return mActor;
  }

  public void setActor(final ActorRef actor) {
    mActor = actor;
  }

  public Object getMessage() {
    return mMessage;
  }

  public void setMessage(final Object message) {
    mMessage = message;
  }

  public Options getOptions() {
    return mOptions;
  }

  public void setOptions(final Options options) {
    mOptions = options;
  }

  public String getSenderId() {
    return mSenderId;
  }

  public void setSenderId(final String senderId) {
    mSenderId = senderId;
  }

  public long getSentTimestamp() {
    return mSentTimestamp;
  }

  public void setSentTimestamp(final long sentTimestamp) {
    mSentTimestamp = sentTimestamp;
  }

  @NotNull
  public RemoteMessage withActor(final ActorRef actor) {
    mActor = actor;
    return this;
  }

  @NotNull
  public RemoteMessage withMessage(final Object message) {
    mMessage = message;
    return this;
  }

  @NotNull
  public RemoteMessage withOptions(final Options options) {
    mOptions = options;
    return this;
  }

  @NotNull
  public RemoteMessage withSenderId(final String senderId) {
    mSenderId = senderId;
    return this;
  }

  @NotNull
  public RemoteMessage withSentTimestamp(final long sentTimestamp) {
    mSentTimestamp = sentTimestamp;
    return this;
  }
}
