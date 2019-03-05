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

package dm.shakespeare.plot.narrator;

import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide-maestroni on 03/02/2019.
 */
public interface Narrator<T> extends Closeable {

  void close();

  boolean report(@NotNull Throwable incident, long timeout, @NotNull TimeUnit unit) throws
      InterruptedException;

  boolean tell(T effect, long timeout, @NotNull TimeUnit unit) throws InterruptedException;
}
