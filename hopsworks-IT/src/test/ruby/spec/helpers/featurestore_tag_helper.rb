=begin
 Copyright (C) 2020, Logical Clocks AB. All rights reserved
=end

module FeatureStoreTagHelper
  def string_schema()
    schema = { "type" => "string"}
    schema.to_json
  end

  def get_featurestore_tags
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

  def get_featurestore_tag(name)
    query = get_tag_schema_query(name)
    pp "get #{query}" if defined?(@debugOpt) && @debugOpt
    get query
  end

  def create_featurestore_tag(name, schema)
    query_params = "name=#{name}"
    path = "#{ENV['HOPSWORKS_API']}/tags"
    pp "post #{path}?#{query_params}, #{schema}" if defined?(@debugOpt) && @debugOpt
    post "#{path}?#{query_params}", schema
  end

  def create_featurestore_tag_checked(name, schema)
    create_featurestore_tag(name, schema)
    expect_status_details(201)
  end

  def delete_featurestore_tag(name)
    delete "#{ENV['HOPSWORKS_API']}/tags/#{name}"
  end

  def delete_featurestore_tag_checked(name)
    delete_featurestore_tag(name)
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
end
