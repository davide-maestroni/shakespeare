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

  private static final long serialVersionUID = VERSION;

  private CreateActorRequest mOriginalRequest;
  private List<String> mResourcePaths;

  @NotNull
  public CreateActorContinue addAllResourcePaths(
      @NotNull final Collection<? extends String> resourcePaths) {
    if (mResourcePaths == null) {
      mResourcePaths = new ArrayList<String>();
    }
    mResourcePaths.addAll(resourcePaths);
    return this;
  }

  @NotNull
  public CreateActorContinue addResourcePath(final String resourcePath) {
    if (mResourcePaths == null) {
      mResourcePaths = new ArrayList<String>();
    }
    mResourcePaths.add(resourcePath);
    return this;
  }

  public CreateActorRequest getOriginalRequest() {
    return mOriginalRequest;
  }

  public void setOriginalRequest(final CreateActorRequest originalRequest) {
    mOriginalRequest = originalRequest;
  }

  public List<String> getResourcePaths() {
    return mResourcePaths;
  }

  public void setResourcePaths(final List<String> resourcePaths) {
    mResourcePaths = resourcePaths;
  }

  @NotNull
  public CreateActorContinue withClassPaths(final List<String> resourcePaths) {
    mResourcePaths = resourcePaths;
    return this;
  }

  @NotNull
  public CreateActorContinue withOriginalRequest(final CreateActorRequest originalRequest) {
    mOriginalRequest = originalRequest;
    return this;
  }

  @NotNull
  @Override
  public CreateActorContinue withSenderId(final String senderId) {
    super.withSenderId(senderId);
    return this;
  }
}
