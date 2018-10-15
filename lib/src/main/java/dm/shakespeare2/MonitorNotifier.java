package dm.shakespeare2;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;

import dm.shakespeare2.actor.Actor;
import dm.shakespeare2.message.ActorResumedMessage;
import dm.shakespeare2.message.ActorStartedMessage;
import dm.shakespeare2.message.ActorStoppedMessage;
import dm.shakespeare2.message.ActorSuspendedMessage;
import dm.shakespeare2.util.ConstantConditions;

/**
 * Created by davide-maestroni on 10/03/2018.
 */
class MonitorNotifier {

  private static final Notifier EMPTY_NOTIFIER = new Notifier() {

    public void resume() {
    }

    public void start() {
    }

    public void stop() {
    }

    public void suspend() {
    }
  };

  private final DefaultNotifier mDefaultNotifier = new DefaultNotifier();
  private final Actor mSender;

  private HashSet<Actor> mMonitors = new HashSet<Actor>();
  private Notifier mNotifier = EMPTY_NOTIFIER;

  MonitorNotifier(@NotNull final Actor sender) {
    mSender = ConstantConditions.notNull("sender", sender);
  }

  void addMonitor(@NotNull final Actor monitor) {
    if (mMonitors.add(ConstantConditions.notNull("monitor", monitor))) {
      mNotifier = mDefaultNotifier;
    }
  }

  void removeMonitor(@NotNull final Actor monitor) {
    final HashSet<Actor> monitors = mMonitors;
    if (monitors.remove(monitor) && monitors.isEmpty()) {
      mNotifier = EMPTY_NOTIFIER;
      mMonitors = new HashSet<Actor>();
    }
  }

  void resume() {
    mNotifier.resume();
  }

  void start() {
    mNotifier.start();
  }

  void stop() {
    mNotifier.stop();
  }

  void suspend() {
    mNotifier.suspend();
  }

  private interface Notifier {

    void resume();

    void start();

    void stop();

    void suspend();
  }

  private class DefaultNotifier implements Notifier {

    public void resume() {
      final Actor sender = mSender;
      for (final Actor monitor : mMonitors) {
        monitor.tell(ActorResumedMessage.defaultInstance(), sender);
      }
    }

    public void start() {
      final Actor sender = mSender;
      for (final Actor monitor : mMonitors) {
        monitor.tell(ActorStartedMessage.defaultInstance(), sender);
      }
    }

    public void stop() {
      final Actor sender = mSender;
      for (final Actor monitor : mMonitors) {
        monitor.tell(ActorStoppedMessage.defaultInstance(), sender);
      }
    }

    public void suspend() {
      final Actor sender = mSender;
      for (final Actor monitor : mMonitors) {
        monitor.tell(ActorSuspendedMessage.defaultInstance(), sender);
      }
    }
  }
}
