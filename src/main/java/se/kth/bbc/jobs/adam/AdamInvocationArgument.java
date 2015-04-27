package se.kth.bbc.jobs.adam;

/**
 *
 * @author stig
 */
public class AdamInvocationArgument {

  private final AdamArgument arg;
  private String value;

  public AdamInvocationArgument(AdamArgument arg) {
    this.arg = arg;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public AdamArgument getArg() {
    return arg;
  }

  @Override
  public String toString() {
    return "<" + arg + ":" + value + ">";
  }

}
