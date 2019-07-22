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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

/**
 * Created by davide-maestroni on 06/27/2019.
 */
public class FileDataStore implements DataStore {

  private final HashMap<String, RawData> data = new HashMap<String, RawData>();
  private final File root;

  @SuppressWarnings("ConstantConditions")
  public FileDataStore(@NotNull final File root) {
    if (!root.isDirectory()) {
      throw new IllegalArgumentException("container is not a directory");
    }
    this.root = root;
    final File[] files = root.listFiles();
    if (files != null) {
      for (final File file : files) {
        if (file.isFile()) {
          data.put(file.getName(), RawData.wrap(file));
        }
      }
    }
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  public void clear() {
    for (final String key : data.keySet()) {
      new File(root, key).delete();
    }
    data.clear();
  }

  public void delete(@NotNull final String key) throws IOException {
    if (data.remove(key) != null) {
      final File file = new File(root, key);
      if (!file.delete()) {
        throw new IOException("cannot delete file: " + file.getAbsolutePath());
      }
    }
  }

  @Nullable
  public RawData get(@NotNull final String key) {
    return data.get(key);
  }

  @Nullable
  public URL getURL(@NotNull final String key) throws IOException {
    if (data.containsKey(key)) {
      return new File(root, key).toURI().toURL();
    }
    return null;
  }

  public boolean isEmpty() {
    return data.isEmpty();
  }

  @NotNull
  public RawData save(@NotNull final String key, @NotNull final RawData data) throws IOException {
    final File file = new File(root, key);
    data.copyTo(file);
    final RawData rawData = RawData.wrap(file);
    this.data.put(key, rawData);
    return rawData;
  }

  public int size() {
    return data.size();
  }

  @NotNull
  public Iterator<DataEntry> iterator() {
    return new FileDataIterator();
  }

  private class FileDataEntry implements DataEntry {

    private final Entry<String, RawData> entry;

    private FileDataEntry(@NotNull final Entry<String, RawData> entry) {
      this.entry = entry;
    }

    @NotNull
    public RawData getData() {
      return entry.getValue();
    }

    @NotNull
    public String getKey() {
      return entry.getKey();
    }

    @NotNull
    public RawData setData(@NotNull final RawData data) throws IOException {
      return save(entry.getKey(), data);
    }
  }

  private class FileDataIterator implements Iterator<DataEntry> {

    private final Iterator<Entry<String, RawData>> iterator = data.entrySet().iterator();

    private Entry<String, RawData> lastEntry;

    public boolean hasNext() {
      return iterator.hasNext();
    }

    public DataEntry next() {
      lastEntry = iterator.next();
      return new FileDataEntry(lastEntry);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void remove() {
      iterator.remove();
      new File(root, lastEntry.getKey()).delete();
    }
  }
}
