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
public class RemoteBounce extends Remote {

  private static final long serialVersionUID = VERSION;

  private Throwable mError;
  private Remote mMessage;
  private List<String> mResourcePaths;

  @NotNull
  public RemoteBounce addAllResourcePaths(
      @NotNull final Collection<? extends String> resourcePaths) {
    if (mResourcePaths == null) {
      mResourcePaths = new ArrayList<String>();
    }
    mResourcePaths.addAll(resourcePaths);
    return this;
  }

  @NotNull
  public RemoteBounce addResourcePath(final String resourcePath) {
    if (mResourcePaths == null) {
      mResourcePaths = new ArrayList<String>();
    }
    mResourcePaths.add(resourcePath);
    return this;
  }

  public Throwable getError() {
    return mError;
  }

  public void setError(final Throwable error) {
    mError = error;
  }

  public Remote getMessage() {
    return mMessage;
  }

  public void setMessage(final Remote message) {
    mMessage = message;
  }

  public List<String> getResourcePaths() {
    return mResourcePaths;
  }

  public void setResourcePaths(final List<String> resourcePaths) {
    mResourcePaths = resourcePaths;
  }

  @NotNull
  public RemoteBounce withError(final Throwable error) {
    mError = error;
    return this;
  }

  @NotNull
  public RemoteBounce withMessage(final Remote message) {
    mMessage = message;
    return this;
  }

  @NotNull
  public RemoteBounce withResourcePaths(final List<String> resourcePaths) {
    mResourcePaths = resourcePaths;
    return this;
  }

  @NotNull
  @Override
  public RemoteBounce withSenderId(final String senderId) {
    super.withSenderId(senderId);
    return this;
  }
}
