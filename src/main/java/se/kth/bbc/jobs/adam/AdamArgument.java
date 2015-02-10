package se.kth.bbc.jobs.adam;

/**
 * A non-optional argument to an ADAM command.
 * @author stig
 */
public final class AdamArgument{
  
  private final String name;
  private final String description;
  private final boolean isPath;
  private final boolean required;
  private final boolean isOutputPath;

  public AdamArgument(String name, String description, boolean isPath) {
    this(name, description, isPath, false);
  }
  
  public AdamArgument(String name, String description, boolean isPath, boolean isOutputPath){
    this(name,description,isPath,isOutputPath,true);
  }
  
  public AdamArgument(String name, String description, boolean isPath, boolean isOutputPath, boolean required){
    if(isOutputPath && !isPath){
      throw new IllegalArgumentException("Argument cannot be an output path if it is not a path.");
    }
    this.name = name;
    this.description = description;
    this.isPath = isPath;
    this.required = required;
    this.isOutputPath = isOutputPath;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public boolean isPath() {
    return isPath;
  }
  
  public boolean isRequired(){
    return required;
  }
  
  public boolean isOutputPath(){
    return isOutputPath;
  }
  
  @Override
  public String toString(){
    StringBuilder ret = new StringBuilder();
    ret.append(name).append("\t:\t");
    ret.append(description);
    if(isPath){
      ret.append("[PATH]");
    }
    if(required){
      ret.append("[REQUIRED]");
    }
    return ret.toString();
  }
  
}
