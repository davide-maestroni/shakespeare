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

package dm.shakespeare.function;

/**
 * Function observing a value.
 *
 * @param <T> the input value type.
 */
public interface Observer<T> {

  /**
   * Accepts the specified value.
   *
   * @param value the input value.
   * @throws Exception when an unexpected error occurs.
   */
  void accept(T value) throws Exception;
}
