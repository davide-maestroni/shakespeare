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

package dm.shakespeare.template.typed.message;

import dm.shakespeare.template.config.BuildConfig;

/**
 * Message indicating that the invocation produced an output value.
 */
public class InvocationResult extends InvocationResponse {

  private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

  private final Object result;

  /**
   * Creates an empty message.<br>
   * Usually needed during deserialization.
   */
  public InvocationResult() {
    this(null, null);
  }

  /**
   * Creates a new result message.
   *
   * @param result the result instance.
   */
  public InvocationResult(final String invocationId, final Object result) {
    super(invocationId);
    this.result = result;
  }

  /**
   * Returns the invocation result.
   *
   * @return the result instance.
   */
  public Object getResult() {
    return result;
  }
}
