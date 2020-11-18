=begin
 Copyright (C) 2020, Logical Clocks AB. All rights reserved
=end
module ProvOpsHelper
  def prov_ops_get(project, ml_id: nil,  ml_type: nil, operation: nil, inode_id: nil, app_id: nil,
                   timestamp_sort: nil, limit: nil,  return_type: "LIST")
    resource = "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/provenance/ops"
    query_params = "?return_type=#{return_type}"
    query_params = "#{query_params}&limit=#{limit}" unless limit.nil?
    query_params = "#{query_params}&filter_by=ML_ID:#{ml_id}" unless ml_id.nil?
    query_params = "#{query_params}&filter_by=ML_TYPE:#{ml_type}" unless ml_type.nil?
    query_params = "#{query_params}&filter_by=FILE_OPERATION:#{operation}" unless operation.nil?
    query_params = "#{query_params}&filter_by=FILE_I_ID:#{inode_id}" unless inode_id.nil?
    query_params = "#{query_params}&filter_by=APP_ID:#{app_id}" unless app_id.nil?
    query_params = "#{query_params}&sort_by=TIMESTAMP:#{timestamp_sort}" unless timestamp_sort.nil?
    pp "#{resource}#{query_params}" if defined?(@debugOpt) && @debugOpt
    result = get "#{resource}#{query_params}"
    expect_status_details(200)
    JSON.parse(result)
  end

  #type:FEATURE/TRAININGDATASET/EXPERIMENT/MODEL
  def prov_ops_last_used(project, ml_id: nil)
    prov_ops_get(project, return_type: "LIST", ml_id: ml_id, operation: "ACCESS_DATA", limit: 1, timestamp_sort: "DESC")
  end

  def prov_ops_currently_in_use_by(project, ml_id)
    result = prov_ops_get(project, return_type: "LIST", ml_id: ml_id, operation: "ACCESS_DATA", timestamp_sort: "DESC")
    result["items"]
        .map { |item| item["appId"] }
        .select { |app| app != "none" }
        .uniq
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
    expected_in_artifact = artifacts[0]["in"]["entry"].select do |i| i["value"]["mlId"] == in_artifact end
    expect(expected_in_artifact.length).to eq(1)
    expected_out_artifact = artifacts[0]["out"]["entry"].select do |i| i["value"]["mlId"] == out_artifact end
    expect(expected_out_artifact.length).to eq(1)
  end
end