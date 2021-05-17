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

  @@project_alert_success = {"service": "FEATURESTORE", "status": "VALIDATION_SUCCESS", "alertType": "PROJECT_ALERT", "severity": "INFO"}
  @@project_alert_warning = {"service": "FEATURESTORE", "status": "VALIDATION_WARNING", "alertType": "PROJECT_ALERT", "severity": "WARNING"}
  @@project_alert_failure = {"service": "FEATURESTORE", "status": "VALIDATION_FAILURE", "alertType": "GLOBAL_ALERT_EMAIL", "severity": "CRITICAL"}

  @@project_alert_finished = {"service": "JOBS", "status": "JOB_FINISHED", "alertType": "PROJECT_ALERT", "severity": "INFO"}
  @@project_alert_failed = {"service": "JOBS", "status": "JOB_FAILED", "alertType": "PROJECT_ALERT", "severity": "WARNING"}
  @@project_alert_killed = {"service": "JOBS", "status": "JOB_KILLED", "alertType": "GLOBAL_ALERT_EMAIL", "severity": "CRITICAL"}

  def get_project_alert_finished
    return @@project_alert_finished.clone
  end

  def get_project_alert_failed
    return @@project_alert_failed.clone
  end

  def get_project_alert_killed
    return @@project_alert_killed.clone
  end

  def get_project_alert_success
    return @@project_alert_success.clone
  end

  def get_project_alert_warning
    return @@project_alert_warning.clone
  end

  def get_project_alert_failure
    return @@project_alert_failure.clone
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
    create_project_alert(project, get_project_alert_finished)
    create_project_alert(project, get_project_alert_killed)
    create_project_alert(project, get_project_alert_success)
    create_project_alert(project, get_project_alert_failure)
  end
end
