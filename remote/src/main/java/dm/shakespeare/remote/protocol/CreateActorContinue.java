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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by davide-maestroni on 04/09/2019.
 */
public class CreateActorContinue extends Remote {

  private ActorRef mActor;
  private List<String> mClassPaths;

  @NotNull
  public CreateActorContinue addAllClassPaths(
      @NotNull final Collection<? extends String> classPaths) {
    if (mClassPaths == null) {
      mClassPaths = new ArrayList<String>();
    }
    mClassPaths.addAll(classPaths);
    return this;
  }

  @NotNull
  public CreateActorContinue addClassPaths(final String classPath) {
    if (mClassPaths == null) {
      mClassPaths = new ArrayList<String>();
    }
    mClassPaths.add(classPath);
    return this;
  }

  public ActorRef getActor() {
    return mActor;
  }

  public void setActor(final ActorRef actor) {
    mActor = actor;
  }

  public List<String> getClassPaths() {
    return mClassPaths;
  }

  @NotNull
  public CreateActorContinue withActor(final ActorRef actor) {
    mActor = actor;
    return this;
  }

  @NotNull
  public CreateActorContinue withClassPaths(final List<String> classPaths) {
    mClassPaths = classPaths;
    return this;
  }
}
