package se.kth.bbc.jobs.adam;

/**
 * POJO representing an AdamArgument, for REST. It also contains a bound value.
 * <p/>
 * @author stig
 */
public class AdamArgumentDTO {

  private String name, description, value;
  private boolean path, required, outputPath;

  public AdamArgumentDTO() {
  }

  public AdamArgumentDTO(AdamArgument aa) {
    this(aa.getName(), aa.getDescription(), null, aa.isPath(), aa.isRequired(),
            aa.isOutputPath());
  }

  public AdamArgumentDTO(AdamInvocationArgument aia) {
    this(aia.getArg().getName(), aia.getArg().getDescription(), aia.getValue(),
            aia.getArg().isPath(), aia.getArg().isRequired(), aia.getArg().
            isOutputPath());
  }

  private AdamArgumentDTO(String name, String description, String value,
          boolean path, boolean required, boolean outputPath) {
    this.name = name;
    this.description = description;
    this.value = value;
    this.path = path;
    this.required = required;
    this.outputPath = outputPath;
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

  public boolean isPath() {
    return path;
  }

  public void setPath(boolean path) {
    this.path = path;
  }

  public boolean isRequired() {
    return required;
  }

  public void setRequired(boolean required) {
    this.required = required;
  }

  public boolean isOutputPath() {
    return outputPath;
  }

  public void setOutputPath(boolean outputPath) {
    this.outputPath = outputPath;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public AdamArgument toAdamArgument() {
    return new AdamArgument(name, description, path, outputPath, required);
  }

}
