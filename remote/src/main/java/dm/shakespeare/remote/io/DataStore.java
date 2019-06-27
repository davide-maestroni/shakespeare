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

package dm.shakespeare.remote.io;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URL;

import dm.shakespeare.remote.io.DataStore.DataEntry;

/**
 * Created by davide-maestroni on 06/27/2019.
 */
public interface DataStore extends Iterable<DataEntry> {

  void clear();

  void delete(@NotNull String key) throws IOException;

  @Nullable
  RawData get(@NotNull String key) throws IOException;

  @Nullable
  URL getURL(@NotNull String key) throws IOException;

  boolean isEmpty();

  @NotNull
  RawData save(@NotNull String key, @NotNull RawData data) throws IOException;

  int size();

  interface DataEntry {

    @NotNull
    RawData getData() throws IOException;

    @NotNull
    String getKey();

    @NotNull
    RawData setData(@NotNull RawData data) throws IOException;
  }
}
