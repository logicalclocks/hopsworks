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

/**
 * POJO representing an AdamOption, for REST. Also includes a value binding.
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

  public AdamOption toAdamOption() {
    return new AdamOption(name, description, valueIsPath, flag, isOutputPath);
  }

}
