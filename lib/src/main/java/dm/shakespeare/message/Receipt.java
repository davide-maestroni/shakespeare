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

package dm.shakespeare.message;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

import dm.shakespeare.actor.Options;
import dm.shakespeare.config.BuildConfig;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 01/09/2019.
 */
public class Receipt implements Serializable {

  private static final long serialVersionUID = BuildConfig.VERSION_HASH_CODE;

  private final Object mMessage;
  private final Options mOptions;

  public Receipt(final Object message, @NotNull final Options options) {
    mMessage = message;
    mOptions = ConstantConditions.notNull("options", options);
  }

  public static boolean isReceipt(@Nullable final Object message, @NotNull final String receiptId) {
    return (message instanceof Receipt) && receiptId.equals(
        ((Receipt) message).getOptions().getReceiptId());
  }

  public final Object getMessage() {
    return mMessage;
  }

  @NotNull
  public final Options getOptions() {
    return mOptions;
  }
}
