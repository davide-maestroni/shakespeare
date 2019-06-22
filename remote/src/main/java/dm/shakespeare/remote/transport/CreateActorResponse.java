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

/**
 * Created by davide-maestroni on 04/09/2019.
 */
public class CreateActorResponse extends RemoteResponse {

  private static final long serialVersionUID = VERSION;

  private ActorID actorID;
  private Throwable error;

  public ActorID getActorID() {
    return actorID;
  }

  public void setActorID(final ActorID actorID) {
    this.actorID = actorID;
  }

  public Throwable getError() {
    return error;
  }

  public void setError(final Throwable error) {
    this.error = error;
  }

  @NotNull
  public CreateActorResponse withActorID(final ActorID actorID) {
    this.actorID = actorID;
    return this;
  }

  @NotNull
  public CreateActorResponse withError(final Throwable error) {
    this.error = error;
    return this;
  }
}
