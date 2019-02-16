package dm.shakespeare.plot;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;

import dm.shakespeare.log.Logger;
import dm.shakespeare.plot.function.NullaryFunction;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 01/25/2019.
 */
public class Scene {

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
  public <T> Event<T> buildEvent(@NotNull final NullaryFunction<? extends Event<T>> eventCreator) {
    Setting.set(mSetting);
    try {
      return Event.ofEvent(eventCreator);

    } finally {
      Setting.unset();
    }
  }

  @NotNull
  public <T> Story<T> buildStory(@NotNull final NullaryFunction<? extends Story<T>> storyCreator) {
    Setting.set(mSetting);
    try {
      return Story.ofStory(storyCreator);

    } finally {
      Setting.unset();
    }
  }
}
