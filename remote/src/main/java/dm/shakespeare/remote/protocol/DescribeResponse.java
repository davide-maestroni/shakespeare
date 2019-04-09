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

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by davide-maestroni on 04/09/2019.
 */
public class DescribeResponse extends DescribeRequest {

  @NotNull
  @Override
  public DescribeResponse addActors(final ActorRef actor) {
    super.addActors(actor);
    return this;
  }

  @NotNull
  @Override
  public DescribeResponse addAllActors(@NotNull final Collection<? extends ActorRef> actors) {
    super.addAllActors(actors);
    return this;
  }

  @NotNull
  @Override
  public DescribeResponse putAllCapabilities(
      @NotNull final Map<? extends String, ? extends String> capabilities) {
    super.putAllCapabilities(capabilities);
    return this;
  }

  @NotNull
  @Override
  public DescribeResponse putCapabilities(final String capabilityKey,
      final String capabilityValue) {
    super.putCapabilities(capabilityKey, capabilityValue);
    return this;
  }

  @NotNull
  @Override
  public DescribeResponse withActors(final List<ActorRef> actors) {
    super.withActors(actors);
    return this;
  }

  @NotNull
  @Override
  public DescribeResponse withCapabilities(final Map<String, String> capabilities) {
    super.withCapabilities(capabilities);
    return this;
  }

  @NotNull
  @Override
  public DescribeResponse withProtocolVersion(final int protocolVersion) {
    super.withProtocolVersion(protocolVersion);
    return this;
  }
}
