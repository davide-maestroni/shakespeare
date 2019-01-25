package dm.shakespeare.plot;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ExecutorService;

import dm.shakespeare.actor.Stage;
import dm.shakespeare.log.Logger;
import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare.util.DoubleQueue;

/**
 * Created by davide-maestroni on 01/25/2019.
 */
class PlayContext {

  private static final ThreadLocal<DoubleQueue<PlayContext>> sLocalContext =
      new ThreadLocal<DoubleQueue<PlayContext>>() {

        @Override
        protected DoubleQueue<PlayContext> initialValue() {
          return new DoubleQueue<PlayContext>();
        }
      };

  private final ExecutorService mExecutor;
  private final Logger mLogger;
  private final Stage mStage;

  PlayContext(@NotNull final Stage stage, @Nullable final ExecutorService executor,
      @Nullable final Logger logger) {
    mStage = ConstantConditions.notNull("stage", stage);
    mExecutor = executor;
    mLogger = logger;
  }

  @NotNull
  static PlayContext get() {
    try {
      return sLocalContext.get().get(0);

    } catch (final IndexOutOfBoundsException e) {
      throw new IllegalStateException("code is not running inside a Play scope");
    }
  }

  static void set(@NotNull final PlayContext context) {
    sLocalContext.get().add(ConstantConditions.notNull("context", context));
  }

  static void unset() {
    sLocalContext.get().removeLast();
  }

  @Nullable
  ExecutorService getExecutor() {
    return mExecutor;
  }

  @Nullable
  Logger getLogger() {
    return mLogger;
  }

  @NotNull
  Stage getStage() {
    return mStage;
  }
}
