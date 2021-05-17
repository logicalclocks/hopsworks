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

module FeatureGroupAlertHelper
  @@fg_alert_resource = "#{ENV['HOPSWORKS_API']}/project/%{projectId}/featurestores/%{fsId}/featuregroups/%{fgId}/alerts"

  @@alert_success = {"status": "SUCCESS", "alertType": "PROJECT_ALERT", "severity": "INFO"}
  @@alert_warning = {"status": "WARNING", "alertType": "PROJECT_ALERT", "severity": "WARNING"}
  @@alert_failed = {"status": "FAILURE", "alertType": "GLOBAL_ALERT_EMAIL", "severity": "CRITICAL"}

  def get_fg_alert_success
    return @@alert_success.clone
  end

  def get_fg_alert_warning
    return @@alert_warning.clone
  end

  def get_fg_alert_failure
    return @@alert_failed.clone
  end

  def get_fg_alerts(project, featuregroup, query: "")
    get "#{@@fg_alert_resource}#{query}" % {projectId: project[:id],
                                            fsId: featuregroup["featurestoreId"],
                                            fgId: featuregroup["id"]}
  end

  def get_fg_alert(project, featuregroup, id)
    get "#{@@fg_alert_resource}/#{id}" % {projectId: project[:id],
                                          fsId: featuregroup["featurestoreId"],
                                          fgId: featuregroup["id"]}
  end

  def update_fg_alert(project, featuregroup, id, alert)
    put "#{@@fg_alert_resource}/#{id}" % {projectId: project[:id],
                                          fsId: featuregroup["featurestoreId"],
                                          fgId: featuregroup["id"]}, alert.to_json
  end

  def create_fg_alert(project, featuregroup, alert)
    post "#{@@fg_alert_resource}" % {projectId: project[:id],
                                     fsId: featuregroup["featurestoreId"],
                                     fgId: featuregroup["id"]}, alert.to_json
  end

  def delete_fg_alert(project, featuregroup, id)
    delete "#{@@fg_alert_resource}/#{id}" % {projectId: project[:id],
                                             fsId: featuregroup["featurestoreId"],
                                             fgId: featuregroup["id"]}
  end

  def create_fg_alerts(project, featuregroup)
    create_fg_alert(project, featuregroup, get_fg_alert_success)
    create_fg_alert(project, featuregroup, get_fg_alert_failure)
  end

  def with_valid_fg(project)
    featurestore_id = get_featurestore_id(project[:id])
    json_result, featuregroup_name = create_cached_featuregroup(project[:id], featurestore_id)
    return JSON.parse(json_result)
  end
end
