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

package dm.shakespeare.remote;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import dm.shakespeare.actor.Envelop;

/**
 * Created by davide-maestroni on 10/01/2018.
 */
public interface RemoteAdapter {

  @NotNull
  String createActor(@Nullable String actorId, @NotNull InputStream data) throws Exception;

  @NotNull
  StageDescription describeStage() throws Exception;

  @NotNull
  Set<String> getCodeEntries(@NotNull Map<String, String> hashes) throws Exception;

  void sendCodeEntries(@NotNull InputStream data) throws Exception;

  void sendMessage(@NotNull String actorId, Object message, @NotNull Envelop envelop) throws
      Exception;

  interface StageDescription {

    Collection<String> getActors();

    Map<String, String> getCapabilities();
  }
}
