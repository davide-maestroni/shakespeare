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

package dm.shakespeare.log;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Logger;

import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 08/06/2018.
 */
public class LogPrinters {

  private LogPrinters() {
    ConstantConditions.avoid();
  }

  @NotNull
  public static LogPrinter javaLoggingPrinter(@NotNull final String name) {
    return new JavaLogPrinter(Logger.getLogger(name));
  }

  @NotNull
  public static LogPrinter javaLoggingPrinter(@NotNull final String name, final int maxMessageSize,
      final int maxLineSize) {
    return new JavaLogPrinter(Logger.getLogger(name), maxMessageSize, maxLineSize);
  }

  @NotNull
  public static LogPrinter mergePrinters(@NotNull final LogPrinter... printers) {
    return mergePrinters(Arrays.asList(printers));
  }

  @NotNull
  private static LogPrinter mergePrinters(
      @NotNull final Collection<? extends LogPrinter> printers) {
    return new MultiPrinter(printers);
  }
}
