=begin
 This file is part of Hopsworks
 Copyright (C) 2020, Logical Clocks AB. All rights reserved

 Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 the GNU Affero General Public License as published by the Free Software Foundation,
 either version 3 of the License, or (at your option) any later version.

 Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 PURPOSE.  See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along with this program.
 If not, see <https://www.gnu.org/licenses/>.
=end
module ProvOpsHelper
  def prov_ops_get(project, inode_id: nil, app_id: nil, return_type: "LIST")
    resource = "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/provenance/ops"
    query_params = "?return_type=#{return_type}"
    query_params = "#{query_params}&filter_by=FILE_I_ID:#{inode_id}" unless inode_id.nil?
    query_params = "#{query_params}&filter_by=APP_ID:#{app_id}" unless app_id.nil?
    pp "#{resource}#{query_params}" if defined?(@debugOpt) && @debugOpt
    result = get "#{resource}#{query_params}"
    expect_status_details(200)
    parsed_result = JSON.parse(result)
    pp parsed_result if defined?(@debugOpt) && @debugOpt
    parsed_result
  end

  def prov_links_get(project, app_id: nil, in_artifact: nil, out_artifact: nil)
    resource = "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/provenance/links"
    query_params = "?only_apps=true&full_link=true"
    query_params = "#{query_params}&filter_by=APP_ID:#{app_id}" unless app_id.nil?
    query_params = "#{query_params}&filter_by=IN_ARTIFACT:#{in_artifact}" unless in_artifact.nil?
    query_params = "#{query_params}&filter_by=OUT_ARTIFACT:#{out_artifact}" unless out_artifact.nil?
    pp "#{resource}#{query_params}" if defined?(@debugOpt) && @debugOpt
    result = get "#{resource}#{query_params}"
    expect_status_details(200)
    parsed_result = JSON.parse(result)
    pp parsed_result if defined?(@debugOpt) && @debugOpt
    parsed_result
  end

  def prov_verify_link(result, app_id, in_artifact, out_artifact)
    artifacts = result["items"].select do | a | a["appId"] == app_id end
    expect(artifacts.length).to eq(1)
    expect(artifacts[0]["in"]["entry"][0]["value"]["mlId"]).to eq(in_artifact)
    expect(artifacts[0]["out"]["entry"][0]["value"]["mlId"]).to eq(out_artifact)
  end
end