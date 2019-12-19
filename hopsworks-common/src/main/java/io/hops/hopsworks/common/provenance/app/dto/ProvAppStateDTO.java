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
package io.hops.hopsworks.common.provenance.app.dto;

import io.hops.hopsworks.common.provenance.core.Provenance;
import io.hops.hopsworks.common.util.DateUtils;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.restutils.RESTCodes;
import java.util.logging.Level;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ProvAppStateDTO {

  private Provenance.AppState currentState;
  private Long submitTime = null;
  private String readableSubmitTime = "";
  private Long startTime = null;
  private String readableStartTime = "";
  private Long finishTime = null;
  private String readableFinishTime = "";

  public ProvAppStateDTO() {
  }
  
  public static ProvAppStateDTO unknown() {
    ProvAppStateDTO state = new ProvAppStateDTO();
    state.setCurrentState(Provenance.AppState.UNKNOWN);
    return state;
  }

  public Provenance.AppState getCurrentState() {
    return currentState;
  }

  public void setCurrentState(Provenance.AppState currentState) {
    this.currentState = currentState;
  }

  public Long getSubmitTime() {
    return submitTime;
  }

  public void setSubmitTime(Long submitTime) {
    this.submitTime = submitTime;
  }

  public String getReadableSubmitTime() {
    return readableSubmitTime;
  }

  public void setReadableSubmitTime(String readableSubmitTime) {
    this.readableSubmitTime = readableSubmitTime;
  }

  public Long getStartTime() {
    return startTime;
  }

  public void setStartTime(Long startTime) {
    this.startTime = startTime;
  }

  public String getReadableStartTime() {
    return readableStartTime;
  }

  public void setReadableStartTime(String readableStartTime) {
    this.readableStartTime = readableStartTime;
  }

  public Long getFinishTime() {
    return finishTime;
  }

  public void setFinishTime(Long finishTime) {
    this.finishTime = finishTime;
  }

  public String getReadableFinishTime() {
    return readableFinishTime;
  }

  public void setReadableFinishTime(String readableFinishTime) {
    this.readableFinishTime = readableFinishTime;
  }


  public void setAppState(Provenance.AppState appState, Long time) throws ProvenanceException {
    switch (appState) {
      case SUBMITTED:
        currentState = Provenance.AppState.SUBMITTED;
        submitTime = time;
        readableSubmitTime = DateUtils.millis2LocalDateTime(time).toString();
        break;
      case RUNNING:
        currentState = Provenance.AppState.RUNNING;
        startTime = time;
        readableStartTime = DateUtils.millis2LocalDateTime(time).toString();
        break;
      case FINISHED:
      case FAILED:
      case KILLED:
        currentState = appState;
        finishTime = time;
        readableFinishTime = DateUtils.millis2LocalDateTime(time).toString();
        break;
      default:
        throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.INTERNAL_ERROR, Level.WARNING,
          "unknown app state from elastic:" + appState);
    }
  }
}
