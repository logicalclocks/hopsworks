package se.kth.hopsworks.meta.exception;

/**
 *
 * @author Vangelis
 */
public class DatabaseException extends Exception {

  private String exception;
  
  public DatabaseException(String exception) {
    super(exception);
  }

  public DatabaseException(String exception, Throwable throwable) {
    super(exception, throwable);
  }

  @Override
  public String getMessage() {
    return this.exception;
  }
}