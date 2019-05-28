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

import java.io.Serializable;

/**
 * Created by davide-maestroni on 05/28/2019.
 */
public class ActorUUID implements Serializable {

  private static final long serialVersionUID = RemoteRequest.VERSION;

  private String actorId;
  private String actorUid;

  public String getActorId() {
    return actorId;
  }

  public void setActorId(final String actorId) {
    this.actorId = actorId;
  }

  public String getActorUid() {
    return actorUid;
  }

  public void setActorUid(final String actorUid) {
    this.actorUid = actorUid;
  }

  @NotNull
  public ActorUUID withActorId(final String actorId) {
    this.actorId = actorId;
    return this;
  }

  @NotNull
  public ActorUUID withActorUid(final String actorUid) {
    this.actorUid = actorUid;
    return this;
  }
}
