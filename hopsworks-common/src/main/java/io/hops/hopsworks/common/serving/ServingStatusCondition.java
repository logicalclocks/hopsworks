/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Hopsworks AB. All rights reserved
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

package io.hops.hopsworks.common.serving;

public class ServingStatusCondition {
  
  private ServingStatusConditionEnum type;
  private Boolean status; // True (succeeded), False (failed) or Null (in progress)
  private String reason;
  
  // Condition messages
  
  public static final String ScheduledInProgress = "Waiting for being scheduled";
  public static final String ScheduledFailed = "Failed to be scheduled: ";
  public static final String InitializedInProgress = "Initializing deployment";
  public static final String InitializedFailed = "Failed to initialize: ";
  public static final String StartedInProgress = "Deployment is starting";
  public static final String StartedFailed = "Failed to run: ";
  public static final String ReadyInProgress = "Setting up connectivity";
  public static final String ReadySuccess = "Deployment is ready";
  public static final String ReadyFailed = "Failed to setup connectivity: ";
  
  public static final String UnscheduledInProgress = "Waiting for being unscheduled";
  public static final String StoppedInProgress = "Stopping deployment";
  public static final String StoppedSuccess = "Deployment is not running";
  
  public ServingStatusCondition() {
  }
  
  public ServingStatusCondition(ServingStatusConditionEnum type, String reason) {
    this.type = type;
    this.status = null;
    this.reason = reason;
  }
  
  public ServingStatusCondition(ServingStatusConditionEnum type, Boolean status, String reason) {
    this.type = type;
    this.status = status;
    this.reason = reason;
  }
  
  public Boolean getStatus() {
    return status;
  }
  
  public void setStatus(Boolean status) {
    this.status = status;
  }
  
  public ServingStatusConditionEnum getType() {
    return type;
  }
  
  public void setType(ServingStatusConditionEnum type) {
    this.type = type;
  }
  
  public String getReason() {
    return reason;
  }
  
  public void setReason(String reason) {
    this.reason = reason;
  }
  
  // Factory
  
  public static ServingStatusCondition getScheduledInProgressCondition() {
    return new ServingStatusCondition(ServingStatusConditionEnum.SCHEDULED, ScheduledInProgress);
  }
  
  public static ServingStatusCondition getScheduledFailedCondition(String errorMsg) {
    return new ServingStatusCondition(ServingStatusConditionEnum.SCHEDULED, false, ScheduledFailed + errorMsg);
  }
  
  public static ServingStatusCondition getInitializedInProgressCondition() {
    return new ServingStatusCondition(ServingStatusConditionEnum.INITIALIZED, InitializedInProgress);
  }
  
  public static ServingStatusCondition getInitializedFailedCondition(String errorMsg) {
    return new ServingStatusCondition(ServingStatusConditionEnum.INITIALIZED, false, InitializedFailed + errorMsg);
  }
  
  public static ServingStatusCondition getStartedInProgressCondition() {
    return new ServingStatusCondition(ServingStatusConditionEnum.STARTED, StartedInProgress);
  }
  
  public static ServingStatusCondition getStartedFailedCondition(String errorMsg) {
    return new ServingStatusCondition(ServingStatusConditionEnum.STARTED, false, StartedFailed + errorMsg);
  }
  
  public static ServingStatusCondition getReadyInProgressCondition() {
    return new ServingStatusCondition(ServingStatusConditionEnum.READY, ReadyInProgress);
  }
  
  public static ServingStatusCondition getReadyFailedCondition(String errorMsg) {
    return new ServingStatusCondition(ServingStatusConditionEnum.READY, false, ReadyFailed + errorMsg);
  }
  
  public static ServingStatusCondition getReadySuccessCondition() {
    return getReadySuccessCondition(ReadySuccess);
  }
  
  public static ServingStatusCondition getReadySuccessCondition(String successMsg) {
    return new ServingStatusCondition(ServingStatusConditionEnum.READY, true, successMsg);
  }
  
  public static ServingStatusCondition getUnscheduledInProgressCondition() {
    return new ServingStatusCondition(ServingStatusConditionEnum.SCHEDULED, UnscheduledInProgress);
  }
  
  public static ServingStatusCondition getStoppedInProgressCondition() {
    return new ServingStatusCondition(ServingStatusConditionEnum.STOPPED, StoppedInProgress);
  }
  
  public static ServingStatusCondition getStoppedSuccessCondition() {
    return new ServingStatusCondition(ServingStatusConditionEnum.STOPPED, true, StoppedSuccess);
  }
}
