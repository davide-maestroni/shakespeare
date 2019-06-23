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

  private Set<ActorID> actorIDs;

  @NotNull
  public FindResponse addActorID(final ActorID actorID) {
    if (this.actorIDs == null) {
      this.actorIDs = new HashSet<ActorID>();
    }
    this.actorIDs.add(actorID);
    return this;
  }

  @NotNull
  public FindResponse addAllActorIDs(final Collection<? extends ActorID> actorUUIDs) {
    if (this.actorIDs == null) {
      this.actorIDs = new HashSet<ActorID>();
    }
    this.actorIDs.addAll(actorUUIDs);
    return this;
  }

  public Set<ActorID> getActorIDs() {
    return actorIDs;
  }

  public void setActorIDs(final Set<ActorID> actorIDs) {
    this.actorIDs = actorIDs;
  }

  @NotNull
  public FindResponse withActorIDs(final Set<ActorID> actorIDs) {
    this.actorIDs = actorIDs;
    return this;
  }
}
