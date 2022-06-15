=begin
 Copyright (C) 2022, Logical Clocks AB. All rights reserved
=end

module TagHelper
  def string_schema()
    schema = { "type" => "string"}
    schema.to_json
  end

  def get_tags
    get "#{ENV['HOPSWORKS_API']}/tags"
  end

  def get_tag_schema_query(name)
    query = "#{ENV['HOPSWORKS_API']}/tags/#{name}"
    query
  end

  def get_tag_schema_full_query(name)
    query = "https://#{ENV['WEB_HOST']}:#{ENV['WEB_PORT']}"
    query = query << get_tag_schema_query(name)
    query
  end

  def get_tag(name)
    query = get_tag_schema_query(name)
    pp "get #{query}" if defined?(@debugOpt) && @debugOpt
    get query
  end

  def create_tag(name, schema)
    query_params = "name=#{name}"
    path = "#{ENV['HOPSWORKS_API']}/tags"
    pp "post #{path}?#{query_params}, #{schema}" if defined?(@debugOpt) && @debugOpt
    post "#{path}?#{query_params}", schema
  end

  def create_tag_checked(name, schema)
    create_tag(name, schema)
    expect_status_details(201)
  end

  def delete_tag(name)
    delete "#{ENV['HOPSWORKS_API']}/tags/#{name}"
  end

  def delete_tag_checked(name)
    delete_tag(name)
    expect_status_details(204)
  end

  def get_featuregroup_tags(project_id, featuregroup_id, featurestore_id: nil)
    featurestore_id = get_featurestore_id(project_id) if featurestore_id.nil?
    path = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/featuregroups/#{featuregroup_id}/tags"
    pp "get #{path}" if defined?(@debugOpt) && @debugOpt
    get path
    json_body.to_json
  end

  def get_featuregroup_tags_checked(project_id, fg_id, fs_id: nil)
    get_featuregroup_tags(project_id, fg_id, featurestore_id: fs_id)
    expect_status_details(200)
  end

  def get_featuregroup_tag_query(project_id, featurestore_id, featuregroup_id, name)
    "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/featuregroups/#{featuregroup_id}/tags/#{name}"
  end

  def get_featuregroup_tag_full_query(project_id, featurestore_id, featuregroup_id, name)
    query = "https://#{ENV['WEB_HOST']}:#{ENV['WEB_PORT']}"
    query = query << get_featuregroup_tag_query(project_id, featurestore_id, featuregroup_id, name)
    query
  end

  def get_featuregroup_tag(project_id, featuregroup_id, name, featurestore_id: nil)
    featurestore_id = get_featurestore_id(project_id) if featurestore_id.nil?
    path = get_featuregroup_tag_query(project_id, featurestore_id, featuregroup_id, name)
    pp path if defined?(@debugOpt) && @debugOpt
    get path
  end

  def get_featuregroup_tag_checked(project_id, featuregroup_id, name, fs_id: nil)
    get_featuregroup_tag(project_id, featuregroup_id, name, featurestore_id: fs_id)
    expect_status_details(200)
  end


  def add_featuregroup_tag_checked(project_id, featuregroup_id, name, value, featurestore_id: nil)
    add_featuregroup_tag(project_id, featuregroup_id, name, value, featurestore_id: featurestore_id)
    expect_status_details(201)
  end

  def update_featuregroup_tag_checked(project_id, featuregroup_id, name, value, featurestore_id: nil)
    add_featuregroup_tag(project_id, featuregroup_id, name, value, featurestore_id: featurestore_id)
    expect_status_details(200)
  end

  def add_featuregroup_tag(project_id, featuregroup_id, name, value, featurestore_id: nil)
    featurestore_id = get_featurestore_id(project_id) if featurestore_id.nil?
    path = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/featuregroups/#{featuregroup_id}/tags/#{name}"
    pp "put #{path}, #{value}" if defined?(@debugOpt) && @debugOpt
    put path, value
  end

  def delete_featuregroup_tag_checked(project_id, featuregroup_id, tag, featurestore_id: nil)
    delete_featuregroup_tag(project_id, featuregroup_id, tag, featurestore_id: featurestore_id)
    expect_status_details(204)
  end

  def delete_featuregroup_tag(project_id, featuregroup_id, tag, featurestore_id: nil)
    featurestore_id = get_featurestore_id(project_id) if featurestore_id.nil?
    path = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/featuregroups/#{featuregroup_id}/tags/#{tag}"
    pp "delete #{path}" if defined?(@debugOpt) && @debugOpt
    delete path
  end

  def get_training_dataset_tags(project_id, training_dataset_id, featurestore_id: nil)
    featurestore_id = get_featurestore_id(project_id) if featurestore_id.nil?
    path = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/trainingdatasets/#{training_dataset_id}/tags"
    pp "get #{path}" if defined?(@debugOpt) && @debugOpt
    get path
    json_body.to_json
  end

  def get_training_dataset_tags_checked(project_id, td_id, fs_id: nil)
    get_training_dataset_tags(project_id, td_id, featurestore_id: fs_id)
    expect_status_details(200)
  end

  def get_training_dataset_tag_query(project_id, featurestore_id, td_id, name)
    "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/trainingdatasets/#{td_id}/tags/#{name}"
  end

  def get_training_dataset_tag_full_query(project_id, featurestore_id, td_id, name)
    query = "https://#{ENV['WEB_HOST']}:#{ENV['WEB_PORT']}"
    query = query << get_training_dataset_tag_query(project_id, featurestore_id, td_id, name)
    query
  end

  def get_training_dataset_tag(project_id, td_id, name, featurestore_id: nil)
    featurestore_id = get_featurestore_id(project_id) if featurestore_id.nil?
    path = get_training_dataset_tag_query(project_id, featurestore_id, td_id, name)
    pp "get #{path}" if defined?(@debugOpt) && @debugOpt
    get path
  end

  def get_training_dataset_tag_checked(project_id, td_id, name, featurestore_id: nil)
    get_training_dataset_tag(project_id, td_id, name, featurestore_id: featurestore_id)
    expect_status_details(200)
  end

  def add_training_dataset_tag_checked(project_id, training_dataset_id, name, value, featurestore_id: nil)
    add_training_dataset_tag(project_id, training_dataset_id, name, value, featurestore_id: featurestore_id)
    expect_status_details(201)
  end

  def update_training_dataset_tag_checked(project_id, training_dataset_id, name, value, featurestore_id: nil)
    add_training_dataset_tag(project_id, training_dataset_id, name, value, featurestore_id: featurestore_id)
    expect_status_details(200)
  end

  def add_training_dataset_tag(project_id, training_dataset_id, name, value, featurestore_id: nil)
    featurestore_id = get_featurestore_id(project_id) if featurestore_id.nil?
    path = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/trainingdatasets/#{training_dataset_id}/tags/#{name}"
    pp "put #{path}, #{value}" if defined?(@debugOpt) && @debugOpt
    put path, value
  end

  def delete_training_dataset_tag(project_id, training_dataset_id, tag, featurestore_id: nil)
    featurestore_id = get_featurestore_id(project_id) if featurestore_id.nil?
    path = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/trainingdatasets/#{training_dataset_id}/tags/#{tag}"
    pp "delete #{path}" if defined?(@debugOpt) && @debugOpt
    delete path
  end

  def delete_training_dataset_tag_checked(project_id, training_dataset_id, tag, featurestore_id: nil)
    delete_training_dataset_tag(project_id, training_dataset_id, tag, featurestore_id: featurestore_id)
    expect_status_details(204)
  end

  def get_featureview_tags(project_id, featureview_name, featureview_version, featurestore_id: nil)
    featurestore_id = get_featurestore_id(project_id) if featurestore_id.nil?
    path = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/featureview/#{featureview_name}/version/#{featureview_version}/tags"
    pp "get #{path}" if defined?(@debugOpt) && @debugOpt
    get path
    json_body.to_json
  end

  def get_featureview_tags_checked(project_id, featureview_name, featureview_version, fs_id: nil)
    get_featureview_tags(project_id, featureview_name, featureview_version, featurestore_id: fs_id)
    expect_status_details(200)
  end

  def get_featureview_tag_query(project_id, featurestore_id, featureview_name, featureview_version, name)
    "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/featureview/#{featureview_name}/version/#{featureview_version}/tags/#{name}"
  end

  def get_featureview_tag_full_query(project_id, featurestore_id, featureview_name, featureview_version, name)
    query = "https://#{ENV['WEB_HOST']}:#{ENV['WEB_PORT']}"
    query = query << get_featureview_tag_query(project_id, featurestore_id, featureview_name, featureview_version, name)
    query
  end
  
  def get_featureview_tag(project_id, featureview_name, featureview_version, name, featurestore_id: nil)
    featurestore_id = get_featurestore_id(project_id) if featurestore_id.nil?
    path = get_featureview_tag_query(project_id, featurestore_id, featureview_name, featureview_version, name)
    pp path if defined?(@debugOpt) && @debugOpt
    get path
  end

  def get_featureview_tag_checked(project_id, featureview_name, featureview_version, name, fs_id: nil)
    get_featureview_tag(project_id, featureview_name, featureview_version, name, featurestore_id: fs_id)
    expect_status_details(200)
  end

  def add_featureview_tag_checked(project_id, featureview_name, featureview_version, name, value,
                                  featurestore_id: nil, featurestore_project_id: nil)
    add_featureview_tag(project_id, featureview_name, featureview_version, name, value,
                        featurestore_id: featurestore_id, featurestore_project_id: featurestore_project_id)
    expect_status_details(201)
  end

  def add_featureview_tag(project_id, featureview_name, featureview_version, name,
                          value, featurestore_id: nil, featurestore_project_id: nil)
    featurestore_project_id = project_id if featurestore_project_id.nil?
    featurestore_id = get_featurestore_id(featurestore_project_id) if featurestore_id.nil?
    path = get_featureview_tag_query(project_id, featurestore_id, featureview_name, featureview_version, name)
    pp "put #{path}, #{value}" if defined?(@debugOpt) && @debugOpt
    put path, value
  end

  def delete_featureview_tag_checked(project_id, featureview_name, featureview_version, tag, featurestore_id: nil)
    delete_featureview_tag(project_id, featureview_name, featureview_version, tag, featurestore_id: featurestore_id)
    expect_status_details(204)
  end

  def delete_featureview_tag(project_id, featureview_name, featureview_version, name, featurestore_id: nil)
    featurestore_id = get_featurestore_id(project_id) if featurestore_id.nil?
    path = get_featureview_tag_query(project_id, featurestore_id, featureview_name, featureview_version, name)
    pp "delete #{path}" if defined?(@debugOpt) && @debugOpt
    delete path
  end

  def get_featureview_training_dataset_tags(project_id, featureview_name, featureview_version, training_dataset_version, featurestore_id: nil)
    featurestore_id = get_featurestore_id(project_id) if featurestore_id.nil?
    path = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/featureview/#{featureview_name}/version/#{featureview_version}" +
      "/trainingdatasets/version/#{training_dataset_version}/tags"
    pp "get #{path}" if defined?(@debugOpt) && @debugOpt
    get path
    json_body.to_json
  end

  def get_featureview_training_dataset_tags_checked(project_id, featureview_name, featureview_version, training_dataset_version, featurestore_id: nil)
    get_featureview_training_dataset_tags(project_id, featureview_name, featureview_version, training_dataset_version, featurestore_id: featurestore_id)
    expect_status_details(200)
  end

  def get_featureview_training_dataset_tag_query(project_id, featurestore_id, featureview_name, featureview_version, training_dataset_version, name)
    "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/featureview/#{featureview_name}/version/#{featureview_version}" +
      "/trainingdatasets/version/#{training_dataset_version}/tags/#{name}"
  end

  def get_featureview_training_dataset_tag_full_query(project_id, featurestore_id, featureview_name, featureview_version, training_dataset_version, name)
    query = "https://#{ENV['WEB_HOST']}:#{ENV['WEB_PORT']}"
    query = query << get_training_dataset_tag_query(project_id, featurestore_id, featureview_name, featureview_version, training_dataset_version, name)
    query
  end

  def get_featureview_training_dataset_tag(project_id, featureview_name, featureview_version, training_dataset_version, name, featurestore_id: nil)
    featurestore_id = get_featurestore_id(project_id) if featurestore_id.nil?
    path = get_featureview_training_dataset_tag_query(project_id, featurestore_id, featureview_name, featureview_version, training_dataset_version, name)
    pp "get #{path}" if defined?(@debugOpt) && @debugOpt
    get path
  end

  def get_featureview_training_dataset_tag_checked(project_id, featureview_name, featureview_version, training_dataset_version, name, featurestore_id: nil)
    get_featureview_training_dataset_tag(project_id, featureview_name, featureview_version, training_dataset_version, name, featurestore_id: featurestore_id)
    expect_status_details(200)
  end

  def add_featureview_training_dataset_tag_checked(project_id, featureview_name, featureview_version, training_dataset_version, name, value, featurestore_id: nil)
    add_featureview_training_dataset_tag(project_id, featureview_name, featureview_version, training_dataset_version, name, value, featurestore_id: featurestore_id)
    expect_status_details(201)
  end

  def update_featureview_training_dataset_tag_checked(project_id, featureview_name, featureview_version, training_dataset_version, name, value, featurestore_id: nil)
    add_featureview_training_dataset_tag(project_id, featureview_name, featureview_version, training_dataset_version, name, value, featurestore_id: featurestore_id)
    expect_status_details(200)
  end

  def add_featureview_training_dataset_tag(project_id, featureview_name, featureview_version, training_dataset_version, name, value, featurestore_id: nil)
    featurestore_id = get_featurestore_id(project_id) if featurestore_id.nil?
    path = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/featureview/#{featureview_name}/version/#{featureview_version}" +
      "/trainingdatasets/version/#{training_dataset_version}/tags/#{name}"
    pp "put #{path}, #{value}" if defined?(@debugOpt) && @debugOpt
    put path, value
  end

  def delete_featureview_training_dataset_tag(project_id, featureview_name, featureview_version, training_dataset_version, tag, featurestore_id: nil)
    featurestore_id = get_featurestore_id(project_id) if featurestore_id.nil?
    path = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/featureview/#{featureview_name}/version/#{featureview_version}" +
      "/trainingdatasets/version/#{training_dataset_version}/tags/#{tag}"
    pp "delete #{path}" if defined?(@debugOpt) && @debugOpt
    delete path
  end

  def delete_featureview_training_dataset_tag_checked(project_id, featureview_name, featureview_version, training_dataset_version, tag, featurestore_id: nil)
    delete_featureview_training_dataset_tag(project_id, featureview_name, featureview_version, training_dataset_version, tag, featurestore_id: featurestore_id)
    expect_status_details(204)
  end

  def get_model_tags(project_id, registry_id, model_id)
    path = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/modelregistries/#{registry_id}/models/#{model_id}/tags"
    pp "get #{path}" if defined?(@debugOpt) && @debugOpt
    get path
    json_body.to_json
  end

  def get_model_tags_checked(project_id, registry_id, model_id)
    get_model_tags(project_id, registry_id, model_id)
    expect_status_details(200)
  end

  def get_model_tag_query(project_id, registry_id, model_id, name)
    "#{ENV['HOPSWORKS_API']}/project/#{project_id}/modelregistries/#{registry_id}/models/#{model_id}/tags/#{name}"
  end

  def get_model_tag(project_id, registry_id, model_id, name)
    path = get_model_tag_query(project_id, registry_id, model_id, name)
    pp "get #{path}" if defined?(@debugOpt) && @debugOpt
    get path
  end

  def get_model_tag_checked(project_id, registry_id, model_id, name)
    get_model_tag(project_id, registry_id, model_id, name)
    expect_status_details(200)
  end

  def add_model_tag(project_id, registry_id, model_id, name, value)
    path = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/modelregistries/#{registry_id}/models/#{model_id}/tags/#{name}"
    pp "put #{path}, #{value}" if defined?(@debugOpt) && @debugOpt
    put path, value
  end

  def add_model_tag_checked(project_id, registry_id, model_id, name, value)
    add_model_tag(project_id, registry_id, model_id, name, value)
    expect_status_details(201)
  end

  def delete_model_tag(project_id, registry_id, model_id, tag)
    path = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/modelregistries/#{registry_id}/models/#{model_id}/tags/#{tag}"
    pp "delete #{path}" if defined?(@debugOpt) && @debugOpt
    delete path
  end

  def delete_model_tag_checked(project_id, registry_id, model_id, tag)
    delete_model_tag(project_id, registry_id, model_id, tag)
    expect_status_details(204)
  end
end
