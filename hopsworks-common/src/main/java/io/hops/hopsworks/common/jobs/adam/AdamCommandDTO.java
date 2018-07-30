/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.hops.hopsworks.common.jobs.adam;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.jobs.JsonReduceable;
import javax.xml.bind.annotation.XmlRootElement;
import io.hops.hopsworks.common.jobs.MutableJsonObject;

/**
 * POJO representing an AdamCommand.
 */
@XmlRootElement
public class AdamCommandDTO implements JsonReduceable {

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

  @Override
  public MutableJsonObject getReducedJsonObject() {
    MutableJsonObject obj = new MutableJsonObject();
    obj.set("command", command);
    //Create a JSON object "arguments" and set it to the builder.
    MutableJsonObject args = new MutableJsonObject();
    for (AdamArgumentDTO arg : arguments) {
      //Only set if the argument is non-empty.
      if (!Strings.isNullOrEmpty(arg.getValue())) {
        args.set(arg.getName(), arg.getValue());
      }
    }
    obj.set("arguments", args);
    //Create a JSON object "options" and set it to the builder.
    MutableJsonObject opts = new MutableJsonObject();
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
  public void updateFromJson(MutableJsonObject json) throws
          IllegalArgumentException {
    String jsonCommand;
    AdamCommand ac;
    MutableJsonObject jsonArgs;
    MutableJsonObject jsonOpts;
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
