=begin
 This file is part of Hopsworks
 Copyright (C) 2021, Logical Clocks AB. All rights reserved

 Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 the GNU Affero General Public License as published by the Free Software Foundation,
 either version 3 of the License, or (at your option) any later version.

 Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 PURPOSE.  See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along with this program.
 If not, see <https://www.gnu.org/licenses/>.
=end

module ProjectServiceAlertHelper
  @@project_alert_resource = "#{ENV['HOPSWORKS_API']}/project/%{projectId}/service/alerts"

  @@project_alert_success = {"service": "FEATURESTORE", "status": "VALIDATION_SUCCESS", "receiver": "global-receiver__email", "severity": "INFO"}
  @@project_alert_warning = {"service": "FEATURESTORE", "status": "VALIDATION_WARNING", "receiver": "global-receiver__slack", "severity": "WARNING"}
  @@project_alert_failure = {"service": "FEATURESTORE", "status": "VALIDATION_FAILURE", "receiver": "global-receiver__email", "severity": "CRITICAL"}

  @@project_alert_finished = {"service": "JOBS", "status": "JOB_FINISHED", "receiver": "global-receiver__email", "severity": "INFO"}
  @@project_alert_failed = {"service": "JOBS", "status": "JOB_FAILED", "receiver": "global-receiver__slack", "severity": "WARNING"}
  @@project_alert_killed = {"service": "JOBS", "status": "JOB_KILLED", "receiver": "global-receiver__pagerduty", "severity": "CRITICAL"}

  @@project_alert_fm_detected = { "service": "FEATURESTORE", "status": "FEATURE_MONITOR_SHIFT_DETECTED", "receiver":
    "global-receiver__pagerduty","severity": "CRITICAL" }
  @@project_alert_fm_undetected = { "service": "FEATURESTORE", "status": "FEATURE_MONITOR_SHIFT_UNDETECTED",
                                    "receiver":"global-receiver__slack","severity": "INFO" }

  def get_project_alert_finished(project)
    finished = @@project_alert_finished.clone
    finished[:receiver] = "#{project[:projectname]}__email"
    return finished
  end

  def get_project_alert_failed(project)
    failed = @@project_alert_failed.clone
    failed[:receiver] = "#{project[:projectname]}__slack"
    return failed
  end

  def get_project_alert_killed(project)
    killed = @@project_alert_killed.clone
    killed[:receiver] = "#{project[:projectname]}__pagerduty"
    return killed
  end

  def get_project_alert_success(project)
    success = @@project_alert_success.clone
    success[:receiver] = "#{project[:projectname]}__email"
    return success
  end

  def get_project_alert_warning(project)
    warning = @@project_alert_warning.clone
    warning[:receiver] = "#{project[:projectname]}__slack"
    return warning
  end

  def get_project_alert_failure(project)
    failure = @@project_alert_failure.clone
    failure[:receiver] = "#{project[:projectname]}__pagerduty"
    return failure
  end

  def get_project_alerts(project, query: "")
    get "#{@@project_alert_resource}#{query}" % {projectId: project[:id]}
  end

  def get_project_alerts_available_services(project)
    get "#{@@project_alert_resource}/available-services" % {projectId: project[:id]}
  end

  def get_project_alert(project, id)
    get "#{@@project_alert_resource}/#{id}" % {projectId: project[:id]}
  end

  def update_project_alert(project, id, alert)
    put "#{@@project_alert_resource}/#{id}" % {projectId: project[:id]}, alert.to_json
  end

  def create_project_alert(project, alert)
    post "#{@@project_alert_resource}" % {projectId: project[:id]}, alert.to_json
  end

  def delete_project_alert(project, id)
    delete "#{@@project_alert_resource}/#{id}" % {projectId: project[:id]}
  end

  def create_project_alerts(project)
    with_receivers(project)
    create_project_alert(project, get_project_alert_finished(project))
    create_project_alert(project, get_project_alert_killed(project))
    create_project_alert(project, get_project_alert_success(project))
    create_project_alert(project, get_project_alert_failure(project))
    create_project_alert(project, get_project_alert_fm_detected(project))
    create_project_alert(project, get_project_alert_fm_undetected(project))
  end

  def create_project_alerts_global(project)
    create_project_alert(project, @@project_alert_finished.clone)
    create_project_alert(project, @@project_alert_killed.clone)
    create_project_alert(project, @@project_alert_success.clone)
    create_project_alert(project, @@project_alert_failure.clone)
  end

  def get_project_alert_fm_detected(project)
    failure = @@project_alert_fm_detected.clone
    failure[:receiver] = "#{project[:projectname]}__pagerduty1"
    return failure
  end

  def get_project_alert_fm_undetected(project)
    alert = @@project_alert_fm_undetected.clone
    alert[:receiver] = "#{project[:projectname]}__slack2"
    return alert
  end
end