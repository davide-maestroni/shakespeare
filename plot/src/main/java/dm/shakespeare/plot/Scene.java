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
import java.util.concurrent.TimeUnit;

import dm.shakespeare.log.Logger;
import dm.shakespeare.plot.Event.EventNarrator;
import dm.shakespeare.plot.Story.StoryNarrator;
import dm.shakespeare.plot.function.NullaryFunction;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 01/25/2019.
 */
public class Scene {

  // TODO: 31/03/2019 generate function wrapper
  // TODO: 05/02/2019 Story PROGRESS???
  // TODO: 15/02/2019 untriggered actors, serialization
  // TODO: 28/02/2019 Story.isBoundless()?, swagger converter

  private final Setting mSetting;

  public Scene() {
    mSetting = new Setting(null, null);
  }

  public Scene(@NotNull final ExecutorService executor) {
    mSetting = new Setting(ConstantConditions.notNull("executor", executor), null);
  }

  public Scene(@NotNull final ExecutorService executor, @NotNull final Logger logger) {
    mSetting = new Setting(ConstantConditions.notNull("executor", executor),
        ConstantConditions.notNull("logger", logger));
  }

  public Scene(@NotNull final Logger logger) {
    mSetting = new Setting(null, ConstantConditions.notNull("logger", logger));
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <T> Event<T> includeEvent(
      @NotNull final NullaryFunction<? extends Event<? extends T>> eventCreator) {
    Setting.set(mSetting);
    try {
      return (Event<T>) eventCreator.call();

    } catch (final Throwable t) {
      if (t instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      return Event.ofIncident(t);

    } finally {
      Setting.unset();
    }
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <T> Story<T> includeStory(
      @NotNull final NullaryFunction<? extends Story<? extends T>> storyCreator) {
    Setting.set(mSetting);
    try {
      return (Story<T>) storyCreator.call();

    } catch (final Throwable t) {
      if (t instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      return Story.ofSingleIncident(t);

    } finally {
      Setting.unset();
    }
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <T> EventNarrator<T> narrateEvent(
      @NotNull final NullaryFunction<? extends EventNarrator<? extends T>> eventCreator) {
    Setting.set(mSetting);
    try {
      return (EventNarrator<T>) eventCreator.call();

    } catch (final Throwable t) {
      if (t instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }

      try {
        final EventNarrator<T> eventNarrator = Event.ofNarration();
        eventNarrator.report(t, 0, TimeUnit.MILLISECONDS);
        eventNarrator.close();
        return eventNarrator;

      } catch (final InterruptedException e) {
        // it should never happen...
        throw new IllegalStateException(e);
      }

    } finally {
      Setting.unset();
    }
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <T> StoryNarrator<T> narrateStory(
      @NotNull final NullaryFunction<? extends StoryNarrator<? extends T>> storyCreator) {
    Setting.set(mSetting);
    try {
      return (StoryNarrator<T>) storyCreator.call();

    } catch (final Throwable t) {
      if (t instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }

      try {
        final StoryNarrator<T> storyNarrator = Story.ofNarrations();
        storyNarrator.report(t, 0, TimeUnit.MILLISECONDS);
        storyNarrator.close();
        return storyNarrator;

      } catch (final InterruptedException e) {
        // it should never happen...
        throw new IllegalStateException(e);
      }

    } finally {
      Setting.unset();
    }
  }
}
