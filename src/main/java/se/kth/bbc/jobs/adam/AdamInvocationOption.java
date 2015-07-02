package se.kth.bbc.jobs.adam;

/**
 *
 * @author stig
 */
public class AdamInvocationOption {

  private final AdamOption opt;
  private String stringValue;
  private boolean booleanValue = false;
  private final boolean usesBoolean;

  public AdamInvocationOption(AdamOption opt) {
    this.opt = opt;
    usesBoolean = opt.isFlag();
  }

  public String getStringValue() {
    if (usesBoolean) {
      throw new IllegalStateException(
              "Cannot get the String value of a boolean option.");
    }
    return stringValue;
  }

  public void setStringValue(String stringValue) {
    if (usesBoolean) {
      throw new IllegalStateException(
              "Cannot set the String value of a boolean option.");
    }
    this.stringValue = stringValue;
  }

  public boolean getBooleanValue() {
    if (!usesBoolean) {
      throw new IllegalStateException(
              "Cannot get the boolean value of a String option.");
    }
    return booleanValue;
  }

  public void setBooleanValue(boolean booleanValue) {
    if (!usesBoolean) {
      throw new IllegalStateException(
              "Cannot set the boolean value of a String option.");
    }
    this.booleanValue = booleanValue;
  }

  public AdamOption getOpt() {
    return opt;
  }

  @Override
  public String toString() {
    if (usesBoolean) {
      return "<" + opt + ":" + booleanValue + ">";
    } else {
      return "<" + opt + ":" + stringValue + ">";
    }
  }

}
