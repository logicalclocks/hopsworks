package se.kth.bbc.jobs.adam;

/**
 * POJO representing an AdamOption, for REST. Also includes a value binding.
 * <p/>
 * @author stig
 */
public class AdamOptionDTO {

  private String name, description, value;
  private boolean valueIsPath, flag, isOutputPath, isSet;

  public AdamOptionDTO() {
  }

  public AdamOptionDTO(AdamOption ao) {
    this(ao.getName(), ao.getDescription(), null, ao.isValuePath(), ao.isFlag(),
            ao.isOutputPath(), false);
  }

  public AdamOptionDTO(AdamInvocationOption aio) {
    this(aio.getOpt().getName(), aio.getOpt().getDescription(), null, aio.
            getOpt().isValuePath(), aio.getOpt().isFlag(), aio.getOpt().
            isOutputPath(), false);
    if (flag) {
      this.isSet = aio.getBooleanValue();
    } else {
      this.value = aio.getStringValue();
    }
  }

  private AdamOptionDTO(String name, String description, String value,
          boolean valueIsPath, boolean flag, boolean isOutputPath, boolean isSet) {
    this.name = name;
    this.description = description;
    this.value = value;
    this.valueIsPath = valueIsPath;
    this.flag = flag;
    this.isOutputPath = isOutputPath;
    this.isSet = isSet;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public boolean isValueIsPath() {
    return valueIsPath;
  }

  public void setValueIsPath(boolean valueIsPath) {
    this.valueIsPath = valueIsPath;
  }

  public boolean isFlag() {
    return flag;
  }

  public void setFlag(boolean flag) {
    this.flag = flag;
  }

  public boolean isOutputPath() {
    return isOutputPath;
  }

  public void setOutputPath(boolean isOutputPath) {
    this.isOutputPath = isOutputPath;
  }

  public String getValue() {
    if (flag) {
      return isSet ? "true" : "false";
    } else {
      return value;
    }
  }

  public boolean getSet() {
    if (!flag) {
      throw new IllegalStateException(
              "Cannot query the set status of a non-flag option.");
    } else {
      return isSet;
    }
  }

  public void setValue(String value) {
    if (flag) {
      if (value == null || value.isEmpty() || !(value.equalsIgnoreCase("true")
              || value.equalsIgnoreCase("false"))) {
        throw new IllegalStateException(
                "The value of a flag option must be true or false.");
      } else {
        isSet = value.equalsIgnoreCase("true");
      }
    } else {
      this.value = value;
    }
  }

  public void setValue(boolean value) {
    if (!flag) {
      throw new IllegalStateException(
              "Cannot set a boolean value for a String field.");
    } else {
      isSet = value;
    }
  }

  public AdamOption toAdamOption() {
    return new AdamOption(name, description, valueIsPath, flag, isOutputPath);
  }

}
