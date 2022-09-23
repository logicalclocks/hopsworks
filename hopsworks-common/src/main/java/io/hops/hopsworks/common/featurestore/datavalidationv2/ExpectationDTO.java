/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.datavalidationv2;


import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.Expectation;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ExpectationDTO {
  private Integer id;
  private String expectationType;
  private String kwargs;
  private String meta = "{}";

  public ExpectationDTO() {}

  public ExpectationDTO(String expectationType, String kwargs, String meta) {
    this.expectationType = expectationType;
    this.kwargs = kwargs;
    this.meta = meta;
  }

  public ExpectationDTO(Expectation expectation) {
    this.id = expectation.getId();
    this.expectationType = expectation.getExpectationType();
    this.kwargs = expectation.getKwargs();
    this.meta = expectation.getMeta();
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getExpectationType() {
    return expectationType;
  }

  public void setExpectationType(String expectationType) {
    this.expectationType = expectationType;
  }

  public String getKwargs() {
    return kwargs;
  }

  public void setKwargs(String kwargs) {
    this.kwargs = kwargs;
  }

  public String getMeta() {
    return meta;
  }

  public void setMeta(String meta) {
    this.meta = meta;
  }

  @Override
  public String toString() {
    return "Expectation{"
      + "id: " + id
      + ", expectationType: " + expectationType
      + ", kwargs: " + kwargs
      + ", meta: " + meta
      + "}";
  }
}
