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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by davide-maestroni on 04/09/2019.
 */
public class MessageContinue extends RemoteResponse {

  private static final long serialVersionUID = VERSION;

  private List<String> resourcePaths;

  @NotNull
  public MessageContinue addAllResourcePaths(
      @NotNull final Collection<? extends String> resourcePaths) {
    if (this.resourcePaths == null) {
      this.resourcePaths = new ArrayList<String>();
    }
    this.resourcePaths.addAll(resourcePaths);
    return this;
  }

  @NotNull
  public MessageContinue addResourcePath(final String resourcePath) {
    if (resourcePaths == null) {
      resourcePaths = new ArrayList<String>();
    }
    resourcePaths.add(resourcePath);
    return this;
  }

  public List<String> getResourcePaths() {
    return resourcePaths;
  }

  public void setResourcePaths(final List<String> resourcePaths) {
    this.resourcePaths = resourcePaths;
  }

  @NotNull
  public MessageContinue withResourcePaths(final List<String> resourcePaths) {
    this.resourcePaths = resourcePaths;
    return this;
  }
}
