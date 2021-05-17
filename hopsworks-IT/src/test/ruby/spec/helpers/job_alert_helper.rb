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

module JobAlertHelper
  @@job_alert_resource = "#{ENV['HOPSWORKS_API']}/project/%{projectId}/jobs/%{jobName}/alerts"

  @@alert_finished = {"status": "FINISHED", "alertType": "PROJECT_ALERT", "severity": "INFO"}
  @@alert_failed = {"status": "FAILED", "alertType": "PROJECT_ALERT", "severity": "WARNING"}
  @@alert_killed = {"status": "KILLED", "alertType": "GLOBAL_ALERT_EMAIL", "severity": "CRITICAL"}

  def get_job_alert_finished
    return @@alert_finished.clone
  end

  def get_job_alert_failed
    return @@alert_failed.clone
  end

  def get_job_alert_killed
    return @@alert_killed.clone
  end

  def get_job_alerts(project, job, query: "")
    get "#{@@job_alert_resource}#{query}" % {projectId: project[:id], jobName: job[:name]}
  end

  def get_job_alert(project, job, id)
    get "#{@@job_alert_resource}/#{id}" % {projectId: project[:id], jobName: job[:name]}
  end

  def update_job_alert(project, job, id, alert)
    put "#{@@job_alert_resource}/#{id}" % {projectId: project[:id], jobName: job[:name]}, alert.to_json
  end

  def create_job_alert(project, job, alert)
    post "#{@@job_alert_resource}" % {projectId: project[:id], jobName: job[:name]}, alert.to_json
  end

  def delete_job_alert(project, job, id)
    delete "#{@@job_alert_resource}/#{id}" % {projectId: project[:id], jobName: job[:name]}
  end

  def create_job_alerts(project, job)
    create_job_alert(project, job, get_job_alert_finished)
    create_job_alert(project, job, get_job_alert_killed)
  end

  def with_valid_alert_job
    with_valid_tour_project("spark")
    job_name = "alert_test_job"
    create_sparktour_job(@project, job_name, "jar", nil)
    json_body
  end
end
