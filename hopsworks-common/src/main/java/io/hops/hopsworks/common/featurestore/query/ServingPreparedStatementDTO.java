/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with this program.
 *  If not, see <https://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.common.featurestore.query;

import io.hops.hopsworks.common.api.RestDTO;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class ServingPreparedStatementDTO  extends RestDTO<ServingPreparedStatementDTO> {
  private Integer preparedStatementIndex;
  private List<PreparedStatementParameterDTO> preparedStatementParameters;
  private String queryOnline;

  public ServingPreparedStatementDTO() {
  }

  public ServingPreparedStatementDTO(Integer preparedStatementIndex,
                                     List<PreparedStatementParameterDTO> preparedStatementParameters,
                                     String queryOnline) {
    this.preparedStatementIndex = preparedStatementIndex;
    this.queryOnline = queryOnline;
    this.preparedStatementParameters = preparedStatementParameters;
  }

  public Integer getPreparedStatementIndex() {
    return preparedStatementIndex;
  }

  public void setPreparedStatementIndex(Integer preparedStatementIndex) {
    this.preparedStatementIndex = preparedStatementIndex;
  }

  public String getQueryOnline() {
    return queryOnline;
  }

  public List<PreparedStatementParameterDTO> getPreparedStatementParameters() {
    return preparedStatementParameters;
  }

  public void setPreparedStatementParameters(List<PreparedStatementParameterDTO> preparedStatementParameters) {
    this.preparedStatementParameters = preparedStatementParameters;
  }

  public void setQueryOnline(String queryOnline) {
    this.queryOnline = queryOnline;
  }
}
