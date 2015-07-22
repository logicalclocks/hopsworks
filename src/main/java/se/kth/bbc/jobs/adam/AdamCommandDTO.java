package se.kth.bbc.jobs.adam;

import com.google.common.base.Strings;
import javax.xml.bind.annotation.XmlRootElement;
import se.kth.bbc.jobs.DatabaseJsonObject;
import se.kth.bbc.jobs.JsonReducable;

/**
 * POJO representing an AdamCommand.
 * <p>
 * @author stig
 */
@XmlRootElement
public class AdamCommandDTO implements JsonReducable {

  private String command;
  private String description;
  private AdamArgumentDTO[] arguments;
  private AdamOptionDTO[] options;

  public AdamCommandDTO() {
  }

  public AdamCommandDTO(AdamCommand ac) {
    this.command = ac.getCommand();
    this.description = ac.getDescription();
    this.arguments = new AdamArgumentDTO[ac.getArguments().length];
    for (int i = 0; i < ac.getArguments().length; i++) {
      this.arguments[i] = new AdamArgumentDTO(ac.getArguments()[i]);
    }
    this.options = new AdamOptionDTO[ac.getOptions().length];
    for (int i = 0; i < ac.getOptions().length; i++) {
      this.options[i] = new AdamOptionDTO(ac.getOptions()[i]);
    }
  }

  public AdamCommandDTO(AdamCommand ac, AdamInvocationArgument[] arguments,
          AdamInvocationOption[] options) {
    this.command = ac.getCommand();
    this.description = ac.getDescription();
    this.arguments = new AdamArgumentDTO[ac.getArguments().length];
    for (int i = 0; i < arguments.length; i++) {
      this.arguments[i] = new AdamArgumentDTO(arguments[i]);
    }
    this.options = new AdamOptionDTO[options.length];
    for (int i = 0; i < options.length; i++) {
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

  public AdamCommand toAdamCommand() {
    return AdamCommand.getFromCommand(command);
  }

  public static String[] getAllCommandNames() {
    AdamCommand[] acs = AdamCommand.values();
    String[] ret = new String[acs.length];
    for (int i = 0; i < acs.length; i++) {
      ret[i] = acs[i].getCommand();
    }
    return ret;
  }

  @Override
  public DatabaseJsonObject getReducedJsonObject() {
    DatabaseJsonObject obj = new DatabaseJsonObject();
    obj.set("command", command);
    //Create a JSON object "arguments" and set it to the builder.
    DatabaseJsonObject args = new DatabaseJsonObject();
    for (AdamArgumentDTO arg : arguments) {
      //Only set if the argument is non-empty.
      if (!Strings.isNullOrEmpty(arg.getValue())) {
        args.set(arg.getName(), arg.getValue());
      }
    }
    obj.set("arguments", args);
    //Create a JSON object "options" and set it to the builder.
    DatabaseJsonObject opts = new DatabaseJsonObject();
    for (AdamOptionDTO opt : options) {
      //If a flag: only set if set
      if (opt.isFlag()) {
        if (opt.getSet()) {
          opts.set(opt.getName(), "true");
        }
      } //If not a flag: only set if not empty
      else if (!Strings.isNullOrEmpty(opt.getValue())) {
        opts.set(opt.getName(), opt.getValue());
      }
    }
    obj.set("options", opts);
    return obj;
  }

  @Override
  public void updateFromJson(DatabaseJsonObject json) throws
          IllegalArgumentException {
    String jsonCommand;
    AdamCommand ac;
    DatabaseJsonObject jsonArgs, jsonOpts;
    try {
      jsonCommand = json.getString("command");
      ac = AdamCommand.getFromCommand(jsonCommand);
      jsonArgs = json.getJsonObject("arguments");
      jsonOpts = json.getJsonObject("options");

      //Count the number of arguments found in the JSON
      int cnt = 0;
      for (AdamArgument aa : ac.getArguments()) {
        if (jsonArgs.containsKey(aa.getName())) {
          cnt++;
        }
      }
      if (cnt != jsonArgs.size()) {
        throw new IllegalArgumentException(
                "Some of the arguments in the JSON object are not valid for the given command.");
      }
      //Count the number of options found in the JSON
      cnt = 0;
      for (AdamOption ao : ac.getOptions()) {
        if (jsonOpts.containsKey(ao.getName())) {
          cnt++;
        }
      }
      if (cnt != jsonOpts.size()) {
        throw new IllegalArgumentException(
                "Some of the options in the JSON object are not valid for the given command.");
      }
    } catch (Exception e) {
      throw new IllegalArgumentException(
              "JSON cannot be converted to AdamCommandDTO", e);
    }

    //Now that we're certain it's ok: fill in the fields.
    this.command = jsonCommand;
    this.description = ac.getDescription();
    //Fill in the arguments
    this.arguments = new AdamArgumentDTO[ac.getArguments().length];
    for (int i = 0; i < ac.getArguments().length; i++) {
      this.arguments[i] = new AdamArgumentDTO(ac.getArguments()[i]);
      String key = this.arguments[i].getName();
      //Check if this argument is in the json
      if (jsonArgs.containsKey(key)) {
        //If so: set the value.
        this.arguments[i].setValue(jsonArgs.getString(key));
      }
    }
    //Fill in the options
    this.options = new AdamOptionDTO[ac.getOptions().length];
    for (int i = 0; i < ac.getOptions().length; i++) {
      this.options[i] = new AdamOptionDTO(ac.getOptions()[i]);
      String key = this.options[i].getName();
      //Check if this option is in the json
      if (jsonOpts.containsKey(key)) {
        //If so: set the value.
        this.options[i].setValue(jsonOpts.getString(key));
      }
    }
  }
}
