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

/**
 * Created by davide-maestroni on 04/11/2019.
 */
public class RemoteRecipient extends Remote {

  private static final long serialVersionUID = VERSION;

  private ActorRef mRecipientRef;

  public ActorRef getRecipientRef() {
    return mRecipientRef;
  }

  public void setRecipientRef(final ActorRef recipientRef) {
    mRecipientRef = recipientRef;
  }

  @NotNull
  public RemoteRecipient withRecipientRef(final ActorRef recipientRef) {
    mRecipientRef = recipientRef;
    return this;
  }

  @NotNull
  @Override
  public RemoteRecipient withSenderId(final String senderId) {
    super.withSenderId(senderId);
    return this;
  }
}
