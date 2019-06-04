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
public class ActorID implements Serializable {

  private static final long serialVersionUID = RemoteRequest.VERSION;

  private String actorId;
  private String instanceId;

  public String getActorId() {
    return actorId;
  }

  public void setActorId(final String actorId) {
    this.actorId = actorId;
  }

  public String getInstanceId() {
    return instanceId;
  }

  public void setInstanceId(final String instanceId) {
    this.instanceId = instanceId;
  }

  @Override
  public int hashCode() {
    int result = actorId != null ? actorId.hashCode() : 0;
    result = 31 * result + (instanceId != null ? instanceId.hashCode() : 0);
    return result;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }

    if ((o == null) || getClass() != o.getClass()) {
      return false;
    }

    final ActorID actorID = (ActorID) o;
    return ((actorId != null) ? actorId.equals(actorID.actorId) : actorID.actorId == null) && (
        (instanceId != null) ? instanceId.equals(actorID.instanceId) : actorID.instanceId == null);
  }

  @NotNull
  public ActorID withActorId(final String actorId) {
    this.actorId = actorId;
    return this;
  }

  @NotNull
  public ActorID withInstanceId(final String instanceId) {
    this.instanceId = instanceId;
    return this;
  }
}
