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

package dm.shakespeare.remote;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Pattern;

import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 04/16/2019.
 */
public abstract class AbstractSerializer implements Serializer {

  private final CopyOnWriteArraySet<Pattern> blacklistPatterns = new CopyOnWriteArraySet<Pattern>();
  private final CopyOnWriteArraySet<Pattern> whitelistPatterns = new CopyOnWriteArraySet<Pattern>();

  @NotNull
  private static Pattern toPattern(@NotNull final String className) {
    String regex = className.replaceAll("\\.", "\\.")
        .replaceAll("\\$", "\\$")
        .replaceAll("\\*\\*", "[a-zA-Z0-9.]+")
        .replaceAll("\\*", "[a-zA-Z0-9]+");
    return Pattern.compile("^" + regex + "$");
  }

  public void blacklist(@NotNull final Collection<String> classNames) {
    for (final String className : ConstantConditions.notNullElements("classNames", classNames)) {
      final Pattern pattern = toPattern(className);
      if (!whitelistPatterns.remove(pattern)) {
        blacklistPatterns.add(pattern);
      }
    }
  }

  public void whitelist(@NotNull final Collection<String> classNames) {
    for (final String className : ConstantConditions.notNullElements("classNames", classNames)) {
      final Pattern pattern = toPattern(className);
      if (!blacklistPatterns.remove(pattern)) {
        whitelistPatterns.add(pattern);
      }
    }
  }

  protected boolean isBlackListed(@NotNull final String className) {
    for (final Pattern pattern : blacklistPatterns) {
      if (pattern.matcher(className).matches()) {
        return true;
      }
    }
    return false;
  }

  protected boolean isWhiteListed(@NotNull final String className) {
    for (final Pattern pattern : whitelistPatterns) {
      if (pattern.matcher(className).matches()) {
        return true;
      }
    }
    return false;
  }
}
