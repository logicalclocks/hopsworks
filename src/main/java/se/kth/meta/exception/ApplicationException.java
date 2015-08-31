package se.kth.meta.exception;

/**
 *
 * @author Vangelis
 */
public class ApplicationException extends Exception {

  public ApplicationException(String message) {
    super(message);
  }

  public ApplicationException(String message, Throwable cause) {
    super(message, cause);
  }

}
