/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.datavalidation;

import io.hops.hopsworks.common.api.RestDTO;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation.AcceptedType;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation.Name;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation.Predicate;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class RuleDTO extends RestDTO<RuleDTO> {

  @XmlElement(required = true)
  private Name name;
  @XmlElement(required = true)
  private Predicate predicate;
  @XmlElement(required = true)
  private AcceptedType acceptedType;
  @XmlElement(required = true)
  private String description;
  
  public Name getName() {
    return name;
  }
  
  public void setName(Name name) {
    this.name = name;
  }
  
  public String getDescription() {
    return description;
  }
  
  public void setDescription(String description) {
    this.description = description;
  }

  public Predicate getPredicate() {
    return predicate;
  }

  public void setPredicate(Predicate predicate) {
    this.predicate = predicate;
  }

  public AcceptedType getAcceptedType() {
    return acceptedType;
  }

  public void setAcceptedType(AcceptedType acceptedType) {
    this.acceptedType = acceptedType;
  }

}
