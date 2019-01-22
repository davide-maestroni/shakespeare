package dm.shakespeare.message;

/**
 * Created by davide-maestroni on 01/17/2019.
 */
public class IllegalRecipientException extends RuntimeException {

  public IllegalRecipientException() {
  }

  public IllegalRecipientException(final String message) {
    super(message);
  }
}
