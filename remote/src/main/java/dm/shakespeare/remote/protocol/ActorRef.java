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
public class ActorRef implements Serializable {

  private String mHash;
  private String mId;

  public String getHash() {
    return mHash;
  }

  public void setHash(final String hash) {
    mHash = hash;
  }

  public String getId() {
    return mId;
  }

  public void setId(final String id) {
    mId = id;
  }

  @NotNull
  public ActorRef withHash(final String hash) {
    mHash = hash;
    return this;
  }

  @NotNull
  public ActorRef withId(final String actorId) {
    mId = actorId;
    return this;
  }
}
