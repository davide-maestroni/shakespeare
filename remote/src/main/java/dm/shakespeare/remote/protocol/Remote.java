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

import java.io.Serializable;

/**
 * Created by davide-maestroni on 04/09/2019.
 */
public class Remote implements Serializable {

  // TODO: 11/04/2019 serialID

  public static final int VERSION = 1;

  private String mSenderUri;

  public String getSenderId() {
    return mSenderUri;
  }

  public void setSenderId(final String senderId) {
    mSenderUri = senderId;
  }

  @NotNull
  public Remote withSenderId(final String senderId) {
    mSenderUri = senderId;
    return this;
  }
}
