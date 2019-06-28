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

package dm.shakespeare.template.typed.actor;

import java.io.Serializable;

import dm.shakespeare.template.config.BuildConfig;

/**
 * Serializable implementation of a {@code Script}.
 */
public class SerializableScript extends Script implements Serializable {

  private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;
}
