package io.hops.hopsworks.common.metadata.exception;

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
