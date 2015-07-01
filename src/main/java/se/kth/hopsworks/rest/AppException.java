package se.kth.hopsworks.rest;

/**
 *
 * @author André<amore@kth.se>
 * @author Ermias<ermiasg@kth.se>
 */
public class AppException extends Exception {

  /**
   * contains the HTTP status of the response sent back to the
   * client in case of error,
   */
  Integer status;

  /**
   * Constructs an instance of <code>AppException</code> with the specified
   * detail message.
   *
   * @param status HTTP status
   * @param msg the detail message.
   */
  public AppException(int status, String msg) {
    super(msg);
    this.status = status;
  }

  public Integer getStatus() {
    return status;
  }

  public void setStatus(Integer status) {
    this.status = status;
  }

}
