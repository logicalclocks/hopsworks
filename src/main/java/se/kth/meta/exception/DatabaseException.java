package se.kth.meta.exception;

/**
 *
 * @author Vangelis
 */
public class DatabaseException extends Exception {

  public DatabaseException(String message) {
    super(message);
  }

  public DatabaseException(String message, Throwable cause) {
    super(message, cause);
  }
}
