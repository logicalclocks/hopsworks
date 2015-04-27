package se.kth.bbc.jobs.adam;

/**
 * An optional argument to an ADAM command.
 * <p>
 * @author stig
 */
public final class AdamOption {

  private final String name;
  private final String description;
  private final boolean valueIsPath;
  private final boolean isFlag; //If is flag: no value required. If it is no flag, a value should be provided
  private final boolean isOutputPath;

  public AdamOption(String name, String description, boolean valueIsPath,
          boolean isFlag) {
    this(name, description, valueIsPath, isFlag, false);
  }

  public AdamOption(String name, String description, boolean valueIsPath,
          boolean isFlag, boolean isOutputPath) {
    if (isFlag && (valueIsPath || isOutputPath)) {
      throw new IllegalArgumentException(
              "An option cannot be both a path and a flag.");
    } else if (!valueIsPath && isOutputPath) {
      throw new IllegalArgumentException(
              "An option cannot be an output path but not a path.");
    }
    this.name = name;
    this.valueIsPath = valueIsPath;
    this.description = description;
    this.isFlag = isFlag;
    this.isOutputPath = isOutputPath;
  }

  public String getName() {
    return name;
  }

  public boolean isValuePath() {
    return valueIsPath;
  }

  public String getDescription() {
    return description;
  }

  public boolean isFlag() {
    return isFlag;
  }

  public boolean isOutputPath() {
    return isOutputPath;
  }

  public String getCliVal() {
    return "-" + name;
  }

  @Override
  public String toString() {
    StringBuilder ret = new StringBuilder();
    ret.append(name).append("\t:\t");
    ret.append(description);
    if (valueIsPath) {
      ret.append("[PATH]");
    }
    if (isFlag) {
      ret.append("[FLAG]");
    }
    return ret.toString();
  }

}
