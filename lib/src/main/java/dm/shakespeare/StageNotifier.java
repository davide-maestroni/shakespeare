package dm.shakespeare;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import dm.shakespeare.actor.Actor;
import dm.shakespeare.message.ActorCreatedMessage;
import dm.shakespeare.message.ActorRemovedMessage;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 10/03/2018.
 */
class StageNotifier {

  private static final Notifier EMPTY_NOTIFIER = new Notifier() {

    public void create(@NotNull final String actorId) {
    }

    public void remove(@NotNull final String actorId) {
    }
  };

  private final DefaultNotifier mDefaultNotifier = new DefaultNotifier();
  private final Object mMutex = new Object();
  private final String mStageName;

  private Set<Actor> mMonitors = Collections.emptySet();
  private Notifier mNotifier = EMPTY_NOTIFIER;

  StageNotifier(@NotNull final String stageName) {
    mStageName = ConstantConditions.notNull("stageName", stageName);
  }

  void addMonitor(@NotNull final Actor monitor) {
    synchronized (mMutex) {
      final Set<Actor> monitors = mMonitors;
      if (!monitors.contains(ConstantConditions.notNull("monitor", monitor))) {
        mMonitors = new HashSet<Actor>(monitors);
        mMonitors.add(monitor);
        mNotifier = mDefaultNotifier;
      }
    }
  }

  void create(@NotNull final String actorId) {
    mNotifier.create(actorId);
  }

  void remove(@NotNull final String actorId) {
    mNotifier.remove(actorId);
  }

  void removeMonitor(@NotNull final Actor monitor) {
    synchronized (mMutex) {
      final Set<Actor> monitors = mMonitors;
      if (monitors.contains(monitor)) {
        mMonitors = new HashSet<Actor>(monitors);
        mMonitors.remove(monitor);
        if (mMonitors.isEmpty()) {
          mNotifier = EMPTY_NOTIFIER;
        }
      }
    }
  }

  private interface Notifier {

    void create(@NotNull String actorId);

    void remove(@NotNull String actorId);
  }

  private class DefaultNotifier implements Notifier {

    public void create(@NotNull final String actorId) {
      @SuppressWarnings("UnnecessaryLocalVariable") final Actor sender =
          StandInActor.defaultInstance();
      final ActorCreatedMessage message = new ActorCreatedMessage(mStageName, actorId);
      for (final Actor monitor : mMonitors) {
        monitor.tell(message, sender);
      }
    }

    public void remove(@NotNull final String actorId) {
      @SuppressWarnings("UnnecessaryLocalVariable") final Actor sender =
          StandInActor.defaultInstance();
      final ActorRemovedMessage message = new ActorRemovedMessage(mStageName, actorId);
      for (final Actor monitor : mMonitors) {
        monitor.tell(message, sender);
      }
    }
  }
}
