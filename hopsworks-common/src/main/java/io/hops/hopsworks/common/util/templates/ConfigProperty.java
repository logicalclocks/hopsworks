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
package io.hops.hopsworks.common.util.templates;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class that represents a configuration property for Jupyter or Zeppelin configuration template.
 */
public class ConfigProperty {
  private static final Logger LOG = Logger.getLogger(ConfigProperty.class.getName());
  
  private final String replacementPattern;
  private final ConfigReplacementPolicy replacementPolicy;
  private String value;
  
  /**
   *
   * @param replacementPattern The pattern that will be replaced with the value in the configuration template.
   * @param replacementPolicy Replacement policy, Overwrite, Append or Ignore
   * @param value The default/system value
   */
  public ConfigProperty(String replacementPattern, ConfigReplacementPolicy replacementPolicy, String value) {
    this.replacementPattern = replacementPattern;
    this.replacementPolicy = replacementPolicy;
    this.value = value;
  }
  
  public String getValue() {
    return value;
  }
  
  public String getReplacementPattern() {
    return replacementPattern;
  }
  
  public void replaceValue(String newValue) {
    if (replacementPolicy != null) {
      LOG.log(Level.FINE, "Replace policy: " + replacementPolicy.getClass().getName() + " "
          + value + " -> " + newValue);
      this.value = replacementPolicy.replace(value, newValue);
    } else {
      LOG.log(Level.WARNING, "Replacement policy is NULL");
    }
  }
  
  @Override
  public String toString() {
    return "ConfigProperty{" +
        "replacementPattern='" + replacementPattern + '\'' +
        ", replacementPolicy=" + replacementPolicy +
        ", value='" + value + '\'' +
        '}';
  }
}
