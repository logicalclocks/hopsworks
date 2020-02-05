=begin
 This file is part of Hopsworks
 Copyright (C) 2019, Logical Clocks AB. All rights reserved

 Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 the GNU Affero General Public License as published by the Free Software Foundation,
 either version 3 of the License, or (at your option) any later version.

 Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 PURPOSE.  See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along with this program.
 If not, see <https://www.gnu.org/licenses/>.
=end
module ProvStateHelper
  def project_index_cleanup(session_email)
    reset_session
    with_admin_session

    #pp "cleaning up indices"
    tries = 0
    loop do
      query = "#{ENV['HOPSWORKS_TESTING']}/test/provenance/cleanup?size=100"
      pp "#{query}" if @debugOpt == true
      result = post "#{query}"
      expect_status(200)
      parsed_result = JSON.parse(result)
      tries = tries+1
      break if parsed_result["result"]["value"] == 0
      break if tries == 10
    end
    expect(tries).to be <= 10
    reset_session
    create_session(session_email, "Pass123")
  end

  def setup_cluster_prov(provenance_type, prov_archive_size)
    reset_session
    with_admin_session
    old_provenance_type = getVar("provenance_type")["value"]
    old_provenance_archive_size = getVar("provenance_archive_size")["value"]

    setVar("provenance_type", provenance_type) if (old_provenance_type != provenance_type)
    setVar("provenance_archive_size", prov_archive_size) if (old_provenance_archive_size != prov_archive_size)

    new_provenance_type = getVar("provenance_type")["value"]
    expect(new_provenance_type).to eq provenance_type
    new_provenance_archive_size = getVar("provenance_archive_size")["value"]
    expect(new_provenance_archive_size).to eq prov_archive_size

    reset_session
    return old_provenance_type, old_provenance_archive_size
  end

  def restore_cluster_prov(provenance_type, prov_archive_size, old_provenance_type, old_prov_archive_size)
    @cookies = with_admin_session

    setVar("provenance_type", old_provenance_type) if (old_provenance_type != provenance_type)
    setVar("provenance_archive_size", old_prov_archive_size) if (old_prov_archive_size != prov_archive_size)

    @cookies = nil
  end

  def stop_epipe
    execute_remotely ENV['EPIPE_HOST'], "sudo systemctl stop epipe"
  end

  def restart_epipe
    execute_remotely ENV['EPIPE_HOST'], "sudo systemctl restart epipe"
  end

  def check_epipe_service
    #pp "checking epipe status"
    prov_wait_for_epipe
  end

  def prov_create_dir(project, dirname)
    create_dir(project, dirname, "")
    expect_status(201)
  end

  def prov_delete_dir(project, path)
    delete "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/#{path}"
    expect_status(204)
  end

  def prov_create_experiment(project, experiment_name) 
    #pp "create experiment #{experiment_name} in project #{project[:inode_name]}"
    prov_create_dir(project, "Experiments/#{experiment_name}")
  end

  def prov_create_model(project, model_name) 
    #pp "create model #{model_name} in project #{project[:inode_name]}"
    models = "Models"
    prov_create_dir(project, "#{models}/#{model_name}")
  end

  def prov_create_model_version(project, model_name, model_version) 
    #pp "create model #{model_name}_#{model_version} in project #{project[:inode_name]}"
    models = "Models"
    prov_create_dir(project, "#{models}/#{model_name}/#{model_version}")
  end

  def prov_create_td(project, td_name, td_version) 
    #pp "create training dataset #{td_name}_#{td_version} in project #{project[:inode_name]}"
    training_datasets = "#{project[:inode_name]}_Training_Datasets"
    prov_create_dir(project, "#{training_datasets}/#{td_name}_#{td_version}")
  end

  def prov_delete_experiment(project, experiment_name) 
    #pp "delete experiment #{experiment_name} in project #{project[:inode_name]}"
    experiments = "Experiments"
    prov_delete_dir(project, "#{experiments}/#{experiment_name}")
  end

  def prov_delete_model(project, model_name) 
    #pp "delete model #{model_name} in project #{project[:inode_name]}"
    models = "Models"
    prov_delete_dir(project, "#{models}/#{model_name}")
  end

  def prov_delete_td(project, td_name, td_version) 
    #pp "delete training dataset #{td_name}_#{td_version} in project #{project[:inode_name]}"
    training_datasets = "#{project[:inode_name]}_Training_Datasets"
    prov_delete_dir(project, "#{training_datasets}/#{td_name}_#{td_version}")
  end

  def prov_experiment_id(experiment_name)
    "#{experiment_name}"
  end

  def prov_model_id(model_name, model_version)
    "#{model_name}_#{model_version}"
  end

  def prov_td_id(td_name, td_version)
    "#{td_name}_#{td_version}"
  end

  def prov_get_td_record(project, td_name, td_version) 
    training_datasets = "#{project[:inode_name]}_Training_Datasets"
    training_dataset = prov_td_id(td_name, td_version)
    FileProv.where("project_name": project["inode_name"], "i_parent_name": training_datasets, "i_name": training_dataset)
  end

  def prov_get_experiment_record(project, experiment_name) 
    experiment_parent = "Experiments"
    FileProv.where("project_name": project["inode_name"], "i_parent_name": experiment_parent, "i_name": experiment_name)
  end

  def prov_add_xattr(original, xattr_name, xattr_value, xattr_op, increment)
#     pp original
    xattr_record = original.dup
#     pp xattrRecord
    xattr_record[:inode_id] = original[:inode_id]
    xattr_record[:inode_operation] = xattr_op
    xattr_record[:io_logical_time] = original[:io_logical_time]+increment
    xattr_record[:io_timestamp] = original[:io_timestamp]+increment
    xattr_record[:io_app_id] = original[:io_app_id]
    xattr_record[:io_user_id] = original[:io_user_id]
#     pp xattrRecord
    xattr_record["i_xattr_name"] = xattr_name
    xattr_record["io_logical_time_batch"] = original["io_logical_time_batch"]+increment
    xattr_record["io_timestamp_batch"] = original["io_timestamp_batch"]+increment
#     pp xattrRecord
    xattr_record.save!

    FileProvXAttr.create(inode_id: xattr_record["inode_id"], namespace: 5, name: xattr_name, inode_logical_time: xattr_record["io_logical_time"], value: xattr_value)
  end

  def prov_add_app_states1(app_id, user)
    timestamp = Time.now
    AppProv.create(id: app_id, state: "null", timestamp: timestamp, name: app_id, user: user, submit_time: timestamp-10, start_time: timestamp-5, finish_time: 0)
    AppProv.create(id: app_id, state: "null", timestamp: timestamp+5, name: app_id, user: user, submit_time: timestamp-10, start_time: timestamp-5, finish_time: 0)
  end
  def prov_add_app_states2(app_id, user)
    timestamp = Time.now
    AppProv.create(id: app_id, state: "null", timestamp: timestamp, name: app_id, user: user, submit_time: timestamp-10, start_time: timestamp-5, finish_time: 0)
    AppProv.create(id: app_id, state: "null", timestamp: timestamp+5, name: app_id, user: user, submit_time: timestamp-10, start_time: timestamp-5, finish_time: 0)
    AppProv.create(id: app_id, state: "FINISHED", timestamp: timestamp+10, name: app_id, user: user, submit_time: timestamp-10, start_time: timestamp-5, finish_time: timestamp+50)
  end

  def prov_wait_for_epipe() 
    #pp "waiting"
    restart_epipe
    sleep_counter1 = 0
    sleep_counter2 = 0
    until FileProv.all.count == 0 || sleep_counter1 == 5 do
      sleep(10)
      sleep_counter1 += 1

    end
    until AppProv.all.count == 0 || sleep_counter2 == 5 do
      sleep(10)
      sleep_counter2 += 1
    end
    expect(sleep_counter1).to be < 5
    expect(sleep_counter2).to be < 5
    #pp "done waiting"
  end

  def prov_check_experiment3(experiments, experiment_id, current_state)
    experiment = experiments.select { |e| e["mlId"] == experiment_id }
    expect(experiment.length).to eq 1

    #pp experiment[0]["appState"]
    expect(experiment[0]["appState"]["currentState"]).to eq current_state
  end

  def prov_check_asset_with_id(assets, asset_id) 
    asset = assets.select {|a| a["mlId"] == asset_id }
    expect(asset.length).to eq 1
    #pp asset
  end 

  def prov_check_asset_with_xattrs(assets, asset_id, xattrs)
    asset = assets.select {|a| a["mlId"] == asset_id }
    expect(asset.length).to eq 1
    #pp model
    expect(asset[0]["xattrs"]["entry"].length).to eq xattrs.length
    xattrs.each do |key, value|
      #pp model[0]["xattrs"]["entry"]
      xattr = asset[0]["xattrs"]["entry"].select do |e|
        e["key"] == key && e["value"] == value
      end
      expect(xattr.length).to eq 1
      #pp xattr
    end
  end 

  def get_ml_asset_in_project(project, ml_type, with_app_state, expected, debug)
    resource = "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/provenance/states"
    query_params = "?filter_by=ML_TYPE:#{ml_type}"
    if with_app_state
      query_params = query_params + "&expand=APP"
    end
    if(debug)
      pp "#{resource}#{query_params}"
    end
    result = get "#{resource}#{query_params}"
    expect_status(200)
    parsed_result = JSON.parse(result)
    if(debug)
      pp parsed_result
    end
    expect(parsed_result["items"].length).to eq expected
    expect(parsed_result["count"]).to eq expected
    parsed_result
  end

  def ml_type(type)
    ->(url) { "#{url}filter_by=ML_TYPE:#{type}" }
  end

  def get_ml_asset_in_project_page(project, ml_type, with_app_state, offset, limit)
    resource = "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/provenance/states"
    query_params = "?filter_by=ML_TYPE:#{ml_type}&offset=#{offset}&limit=#{limit}"
    if with_app_state
      query_params = query_params + "&expand=APP"
    end
    # pp "#{resource}#{query_params}"
    result = get "#{resource}#{query_params}"
    expect_status(200)
    parsed_result = JSON.parse(result)
    parsed_result
  end

  def check_no_ml_asset_by_id(project, ml_type, ml_id, with_app_state)
    resource = "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/provenance/states"
    query_params = "?filter_by=ML_TYPE:#{ml_type}&filter_by=ML_ID:#{ml_id}"
    if with_app_state
      query_params = query_params + "&expand=APP"
    end
    # pp "#{resource}#{query_params}"
    result = get "#{resource}#{query_params}"
    expect_status(200)
    parsed_result = JSON.parse(result)
    expect(parsed_result["items"].length).to eq 0
    expect(parsed_result["count"]).to eq 0
  end

  def get_ml_asset_by_id(project, ml_type, ml_id, with_app_state)
    resource = "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/provenance/states"
    query_params = "?filter_by=ML_TYPE:#{ml_type}&filter_by=ML_ID:#{ml_id}"
    if with_app_state
      query_params = query_params + "&expand=APP"
    end
    # pp "#{resource}#{query_params}"
    result = get "#{resource}#{query_params}"
    expect_status(200)
    parsed_result = JSON.parse(result)
    expect(parsed_result["items"].length).to eq 1
    expect(parsed_result["count"]).to eq 1
    parsed_result["items"][0]
  end

  def get_ml_asset_like_name(project, ml_type, term)
    resource = "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/provenance/states"
    query_params = "?filter_by=ML_TYPE:#{ml_type}&filter_by=FILE_NAME_LIKE:#{term}"
    # pp "#{resource}#{query_params}"
    result = get "#{resource}#{query_params}"
    expect_status(200)
    parsed_result = JSON.parse(result)
    parsed_result["items"]
  end

  def get_ml_in_create_range(project, ml_type, from, to, expected)
    resource = "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/provenance/states"
    query_params = "?filter_by=ML_TYPE:#{ml_type}&filter_by=CREATE_TIMESTAMP_LT:#{to}&filter_by=CREATE_TIMESTAMP_GT:#{from}"
    # pp "#{resource}#{query_params}"
    result = get "#{resource}#{query_params}"
    expect_status(200)
    parsed_result = JSON.parse(result)
    #pp parsed_result
    expect(parsed_result["items"].length).to eq expected
    expect(parsed_result["count"]).to eq expected
    parsed_result["items"]
  end

  def get_ml_asset_by_xattr(project, ml_type, xattr_key, xattr_val)
    resource = "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/provenance/states"
    query_params = "?filter_by=ML_TYPE:#{ml_type}&xattr_filter_by=#{xattr_key}:#{xattr_val}"
    # pp "#{resource}#{query_params}"
    result = get "#{resource}#{query_params}"
    expect_status(200)
    parsed_result = JSON.parse(result)
    parsed_result["items"]
  end

  def get_ml_asset_by_xattr_count(project, ml_type, xattr_key, xattr_val, count)
    resource = "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/provenance/states"
    query_params = "?filter_by=ML_TYPE:#{ml_type}&xattr_filter_by=#{xattr_key}:#{xattr_val}&return_type=COUNT"
    # pp "#{resource}#{query_params}"
    result = get "#{resource}#{query_params}"
    expect_status(200)
    parsed_result = JSON.parse(result)
    expect(parsed_result["result"]["value"]).to eq count
    parsed_result["result"]
  end

  def get_ml_asset_like_xattr(project, ml_type, xattr_key, xattr_val)
    resource = "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/provenance/states"
    query_params = "?filter_by=ML_TYPE:#{ml_type}&xattr_like=#{xattr_key}:#{xattr_val}"
    # pp "#{resource}#{query_params}"
    result = get "#{resource}#{query_params}"
    expect_status(200)
    parsed_result = JSON.parse(result)
    parsed_result["items"]
  end
    
  def get_ml_td_count_using_feature_project(project, feature_name) 
    resource = "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/provenance/states"
    query_params = "?filter_by=ML_TYPE:TRAINING_DATASET&xattr_filter_by=features.name:#{feature_name}&return_type=COUNT"
    # pp "#{resource}#{query_params}"
    result = get "#{resource}#{query_params}"
    expect_status(200)
    parsed_result = JSON.parse(result)
    parsed_result["items"]
  end

  def get_ml_td_count_using_feature_global(feature_name) 
    resource = "#{ENV['HOPSWORKS_API']}/provenance/states"
    query_params = "?filter_by=ML_TYPE:TRAINING_DATASET&xattr_filter_by==features.name:#{feature_name}&return_type=COUNT"
    # pp "#{resource}#{query_params}"
    result = get "#{resource}#{query_params}"
    expect_status(200)
    parsed_result = JSON.parse(result)
    parsed_result["items"]
  end

  def attach_app_id_xattr(project, inode_id, app_id)
    target = "#{ENV['HOPSWORKS_TESTING']}/test/project/#{project[:id]}/provenance/xattr"
    param = "?inodeId=#{inode_id}&xattrName=app_id&xattrValue=#{app_id}"
    # pp "#{target}#{param}"
    result = post "#{target}#{param}"
    expect_status(200)
  end

  def prov_with_retries(max_retries, wait_time, expected_code, &block)
    response = nil
    max_retries.times {
      response = block.call
      break if response.code == expected_code
      sleep(wait_time)
    }
    expect(response.code).to eq expected_code
    return response
  end

  def check_epipe_is_active()
    output = execute_remotely ENV['EPIPE_HOST'], "systemctl is-active epipe"
    expect(output.strip).to eq("active"), "epipe is down"
  end

  def wait_for_project_prov(project)
    check_epipe_is_active
    wait_for do
      FileProv.where("project_name": project["inode_name"]).empty?
    end
  end
end