package se.kth.meta.exception;

/**
 *
 * @author Vangelis
 */
public class ApplicationException extends Exception {

  private String description;
  private String exception;
  private String Class;

  public ApplicationException(String exception) {
    super(exception);
    this.exception = exception;
    this.description = "";
  }

  public ApplicationException(String exception, Throwable throwable) {
    super(exception, throwable);
  }

  public ApplicationException(String exception, String description){
    this(exception);
    this.description = description;
  }
  
  public String getDescription() {
    return this.description;
  }

  @Override
  public String getMessage() {
    return this.exception;
  }
}