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

/**
 * Defines the library logging abstraction layer.<br>
 * The logging layer is used internally to abstract the underlying logging framework, so to be
 * easily integrable with popular libraries (like
 * <a href="https://logging.apache.org/log4j/2.x/">log4j</a> or
 * <a href="https://www.slf4j.org/">slf4j</a>).<br>
 * By default, the {@link dm.shakespeare.log.Logger} instances will employ a
 * {@link dm.shakespeare.log.LogPrinter} implementation leveraging the built-in Java logging
 * classes.<br>
 * Utilities are exposed through the {@link dm.shakespeare.log.LogPrinters} class.
 */
package dm.shakespeare.log;
