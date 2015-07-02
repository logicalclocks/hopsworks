package se.kth.bbc.jobs.adam;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * POJO representing an AdamCommand.
 * @author stig
 */
@XmlRootElement
public class AdamCommandDTO {
  
  private String command;
  private String description;
  private AdamArgumentDTO[] arguments;
  private AdamOptionDTO[] options;
  
  public AdamCommandDTO(){}
  
  public AdamCommandDTO(AdamCommand ac){
    this.command = ac.getCommand();
    this.description = ac.getDescription();
    this.arguments = new AdamArgumentDTO[ac.getArguments().length];
    for(int i=0;i< ac.getArguments().length;i++){
      this.arguments[i] = new AdamArgumentDTO(ac.getArguments()[i]);
    }
    this.options = new AdamOptionDTO[ac.getOptions().length];
    for(int i=0;i< ac.getOptions().length;i++){
      this.options[i] = new AdamOptionDTO(ac.getOptions()[i]);
    }
  }
  
  public AdamCommandDTO(AdamCommand ac, AdamInvocationArgument[] arguments, AdamInvocationOption[] options){
    this.command = ac.getCommand();
    this.description = ac.getDescription();
    this.arguments = new AdamArgumentDTO[ac.getArguments().length];
    for(int i=0;i< arguments.length;i++){
      this.arguments[i] = new AdamArgumentDTO(arguments[i]);
    }
    this.options = new AdamOptionDTO[options.length];
    for(int i=0;i< options.length;i++){
      this.options[i] = new AdamOptionDTO(options[i]);
    }
  }

  public String getCommand() {
    return command;
  }

  public void setCommand(String command) {
    this.command = command;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public AdamArgumentDTO[] getArguments() {
    return arguments;
  }

  public void setArguments(AdamArgumentDTO[] arguments) {
    this.arguments = arguments;
  }

  public AdamOptionDTO[] getOptions() {
    return options;
  }

  public void setOptions(AdamOptionDTO[] options) {
    this.options = options;
  }
  
  public AdamCommand toAdamCommand(){
    return AdamCommand.getFromCommand(command);
  }
  
  public static AdamCommandDTO[] values(){
    AdamCommand[] acs = AdamCommand.values();
    AdamCommandDTO[] ret = new AdamCommandDTO[acs.length];
    for(int i=0;i<acs.length;i++){
      ret[i] = new AdamCommandDTO(acs[i]);
    }    
    return ret;
  }
  
  
}
