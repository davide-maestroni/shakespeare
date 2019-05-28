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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by davide-maestroni on 05/24/2019.
 */
public class FindResponse extends RemoteResponse {

  private static final long serialVersionUID = VERSION;

  private Set<ActorUUID> actorUUIDs;

  public static long getSerialVersionUID() {
    return serialVersionUID;
  }

  @NotNull
  public FindResponse addActorUUID(final ActorUUID actorUUID) {
    if (this.actorUUIDs == null) {
      this.actorUUIDs = new HashSet<ActorUUID>();
    }
    this.actorUUIDs.add(actorUUID);
    return this;
  }

  @NotNull
  public FindResponse addAllActorUUIDs(final Collection<? extends ActorUUID> actorUUIDs) {
    if (this.actorUUIDs == null) {
      this.actorUUIDs = new HashSet<ActorUUID>();
    }
    this.actorUUIDs.addAll(actorUUIDs);
    return this;
  }

  public Set<ActorUUID> getActorUUIDs() {
    return actorUUIDs;
  }

  public void setActorUUIDs(final Set<ActorUUID> actorUUIDs) {
    this.actorUUIDs = actorUUIDs;
  }

  @NotNull
  public FindResponse withActorUUIDs(final Set<ActorUUID> actorUUIDs) {
    this.actorUUIDs = actorUUIDs;
    return this;
  }
}
