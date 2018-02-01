/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
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
}
