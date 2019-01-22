package dm.shakespeare.template;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Behavior.Context;
import dm.shakespeare.log.Logger;

/**
 * Created by davide-maestroni on 01/14/2019.
 */
public class ContextWrapper implements Context {

  private Context mContext;

  public void dismissSelf() {
    mContext.dismissSelf();
  }

  @NotNull
  public ExecutorService getExecutor() {
    return mContext.getExecutor();
  }

  @NotNull
  public Logger getLogger() {
    return mContext.getLogger();
  }

  @NotNull
  public ScheduledExecutorService getScheduledExecutor() {
    return mContext.getScheduledExecutor();
  }

  @NotNull
  public Actor getSelf() {
    return mContext.getSelf();
  }

  public boolean isDismissed() {
    return mContext.isDismissed();
  }

  public void restartSelf() {
    mContext.restartSelf();
  }

  public void setBehavior(@NotNull final Behavior behavior) {
    mContext.setBehavior(behavior);
  }

  @NotNull
  public ContextWrapper withContext(final Context context) {
    mContext = context;
    return this;
  }
}
