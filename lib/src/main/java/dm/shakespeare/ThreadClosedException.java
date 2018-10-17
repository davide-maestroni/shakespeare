package dm.shakespeare;

/**
 * Created by davide-maestroni on 07/11/2018.
 */
class ThreadClosedException extends IllegalStateException {

  ThreadClosedException() {
    super("thread closed");
  }
}
