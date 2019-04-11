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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by davide-maestroni on 04/09/2019.
 */
public class DescribeResponse extends Remote {

  private List<ActorRef> mActors = new ArrayList<ActorRef>();
  private Map<String, String> mCapabilities = new HashMap<String, String>();
  private int mProtocolVersion = Remote.VERSION;

  @NotNull
  public DescribeResponse addActors(final ActorRef actor) {
    if (mActors == null) {
      mActors = new ArrayList<ActorRef>();
    }
    mActors.add(actor);
    return this;
  }

  @NotNull
  public DescribeResponse addAllActors(@NotNull final Collection<? extends ActorRef> actors) {
    if (mActors == null) {
      mActors = new ArrayList<ActorRef>();
    }
    mActors.addAll(actors);
    return this;
  }

  public List<ActorRef> getActors() {
    return mActors;
  }

  public void setActors(final List<ActorRef> actors) {
    mActors = actors;
  }

  public Map<String, String> getCapabilities() {
    return mCapabilities;
  }

  public void setCapabilities(final Map<String, String> capabilities) {
    mCapabilities = capabilities;
  }

  public int getProtocolVersion() {
    return mProtocolVersion;
  }

  public void setProtocolVersion(final int protocolVersion) {
    mProtocolVersion = protocolVersion;
  }

  @NotNull
  public DescribeResponse putAllCapabilities(
      @NotNull final Map<? extends String, ? extends String> capabilities) {
    if (mCapabilities == null) {
      mCapabilities = new HashMap<String, String>();
    }
    mCapabilities.putAll(capabilities);
    return this;
  }

  @NotNull
  public DescribeResponse putCapabilities(final String capabilityKey,
      final String capabilityValue) {
    if (mCapabilities == null) {
      mCapabilities = new HashMap<String, String>();
    }
    mCapabilities.put(capabilityKey, capabilityValue);
    return this;
  }

  @NotNull
  public DescribeResponse withActors(final List<ActorRef> actors) {
    mActors = actors;
    return this;
  }

  @NotNull
  public DescribeResponse withCapabilities(final Map<String, String> capabilities) {
    mCapabilities = capabilities;
    return this;
  }

  @NotNull
  public DescribeResponse withProtocolVersion(final int protocolVersion) {
    mProtocolVersion = protocolVersion;
    return this;
  }
}
