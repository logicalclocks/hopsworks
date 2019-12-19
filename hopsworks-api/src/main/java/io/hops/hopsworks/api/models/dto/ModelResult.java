/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
 */

package io.hops.hopsworks.api.models.dto;

import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;

import java.util.HashMap;

@XmlRootElement
public class ModelResult {

  @XmlAnyAttribute
  private HashMap<QName, Double> attributes;

  public ModelResult() {
    //Needed for JAXB
  }

  public HashMap<QName, Double> getAttributes() {
    return this.attributes;
  }

  public void setAttributes(HashMap<QName, Double> attributes) {
    this.attributes = attributes;
  }
}
