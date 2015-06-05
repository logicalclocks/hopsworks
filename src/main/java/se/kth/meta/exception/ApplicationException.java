package se.kth.meta.exception;

/**
 *
 * @author Vangelis
 */
public class ApplicationException extends ExceptionIntf {

  private String description;
  private String exception;
  private String Class;

  public ApplicationException(String exception) {
    super(exception);
    this.exception = exception;
    this.description = "";
  }

  public ApplicationException(String exception, String description) {
    this(exception);
    this.description = description;
  }

  public ApplicationException(String Class, String description, String exception) {
    this(exception, description);
    this.Class = Class;
  }

  public String getDescription() {
    return this.description;
  }

  @Override
  public String getMessage() {
    return this.exception;
  }
}
