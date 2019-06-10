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

package dm.shakespeare.plot;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;

import dm.shakespeare.log.Logger;
import dm.shakespeare.plot.function.NullaryFunction;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 01/25/2019.
 */
public class Plot {

  // TODO: 16/04/2019 memoryFactory(NullaryFunction<Memory>)
  // TODO: 15/02/2019 serialization?
  // TODO: 28/02/2019 swagger converter

  private final Setting setting;

  public Plot() {
    setting = new Setting(null, null);
  }

  public Plot(@NotNull final ExecutorService executor) {
    setting = new Setting(ConstantConditions.notNull("executor", executor), null);
  }

  public Plot(@NotNull final ExecutorService executor, @NotNull final Logger logger) {
    setting = new Setting(ConstantConditions.notNull("executor", executor),
        ConstantConditions.notNull("logger", logger));
  }

  public Plot(@NotNull final Logger logger) {
    setting = new Setting(null, ConstantConditions.notNull("logger", logger));
  }

  public <E extends Event<?>> E include(@NotNull final NullaryFunction<? extends E> creator) throws
      Exception {
    Setting.set(setting);
    try {
      return creator.call();

    } finally {
      Setting.unset();
    }
  }
}
