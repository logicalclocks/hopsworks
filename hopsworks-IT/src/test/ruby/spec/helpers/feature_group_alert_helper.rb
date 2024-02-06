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
  @@alert_success = {"status": "SUCCESS", "receiver": "global-receiver__email", "severity": "INFO"}
  @@alert_warning = {"status": "WARNING", "receiver": "global-receiver__slack", "severity": "WARNING"}
  @@alert_failed = {"status": "FAILURE", "receiver": "global-receiver__pagerduty", "severity": "CRITICAL"}
  # feature monitor specific
  @@featureview_alert_resource = "#{ENV['HOPSWORKS_API']}/project/%{projectId}/featurestores/%{fsId}/featureview/%{name}/version/%{version}/alerts"
  @@fm_alert_success = {"status": "FEATURE_MONITOR_SHIFT_UNDETECTED", "receiver": "global-receiver__email", "severity":
    "INFO"}
  @@fm_alert_failed = {"status": "FEATURE_MONITOR_SHIFT_DETECTED", "receiver": "global-receiver__pagerduty", "severity": "CRITICAL"}
  def get_fg_alert_success(project)
    success = @@alert_success.clone
    success[:receiver] = "#{project[:projectname]}__email"
    return success
  end

  def get_fg_alert_warning(project)
    warning = @@alert_warning.clone
    warning[:receiver] = "#{project[:projectname]}__slack"
    return warning
  end

  def get_fg_alert_failure(project)
    failed = @@alert_failed.clone
    failed[:receiver] = "#{project[:projectname]}__pagerduty"
    return failed
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
    with_receivers(project)
    create_fg_alert(project, featuregroup, get_fg_alert_success(project))
    create_fg_alert(project, featuregroup, get_fg_alert_failure(project))
  end

  def create_fg_alerts_global(project, featuregroup)
    with_global_receivers
    create_fg_alert(project, featuregroup, @@alert_success.clone)
    create_fg_alert(project, featuregroup, @@alert_warning.clone)
    create_fg_alert(project, featuregroup, @@alert_failed.clone)
    create_fg_alert(project, featuregroup, @@fm_alert_failed.clone)
    create_fg_alert(project, featuregroup, @@fm_alert_success.clone)
  end

  def with_valid_fg(project)
    featurestore_id = get_featurestore_id(project[:id])
    json_result, featuregroup_name = create_cached_featuregroup(project[:id], featurestore_id)
    return JSON.parse(json_result)
  end

  def get_fm_alert_failure(project)
    failed = @@fm_alert_failed.clone
    failed[:receiver] = "#{project[:projectname]}__pagerduty"
    return failed
  end

  def get_fm_alert_success(project)
    success = @@fm_alert_success.clone
    success[:receiver] = "#{project[:projectname]}__email"
    return success
  end

  def create_fm_alerts(project, featuregroup, featureview)
    create_fg_alert(project, featuregroup, get_fm_alert_failure(project))
    create_feature_view_alert(project, featureview, get_fm_alert_failure(project))
  end

  def create_feature_view_alert(project, featureview, alert)
    post "#{@@featureview_alert_resource}" % { projectId: project[:id],
                                               fsId: featureview["featurestoreId"],
                                               name: featureview["name"],
                                               version: featureview["version"]}, alert.to_json
  end

  def update_featureview_alert(project, featureview, id, alert)
    put "#{@@featureview_alert_resource}/#{id}" % { projectId: project[:id],
                                                    fsId: featureview["featurestoreId"],
                                                    name: featureview["name"],
                                                    version: featureview["version"]}, alert.to_json
  end

  def get_featureview_alerts(project, featureview)
    get "#{@@featureview_alert_resource}" % {projectId: project[:id],
                                            fsId: featureview["featurestoreId"],
                                            name: featureview["name"],
                                            version: featureview["version"]}
  end


end