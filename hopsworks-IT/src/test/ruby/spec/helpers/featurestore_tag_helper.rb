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

  def get_featurestore_tag(name)
    get "#{ENV['HOPSWORKS_API']}/tags/#{name}"
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

  def get_featuregroup_tags(project_id, featurestore_id, featuregroup_id)
    get "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s + "/tags"
    return json_body.to_json
  end

  def get_featuregroup_tags_checked(project_id, fg_id, fs_id: nil)
    fs_id = get_featurestore_id(project_id) if fs_id.nil?
    get_featuregroup_tags(project_id, fs_id, fg_id)
    expect_status_details(200)
  end

  def get_featuregroup_tag(project_id, featurestore_id, featuregroup_id, name)
    get "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s + "/tags/" + name
  end

  def add_featuregroup_tag_checked(project_id, featurestore_id, featuregroup_id, name, value: nil)
    add_featuregroup_tag(project_id, featurestore_id, featuregroup_id, name, value: value)
    expect_status_details(201)
  end

  def update_featuregroup_tag_checked(project_id, featurestore_id, featuregroup_id, name, value: nil)
    add_featuregroup_tag(project_id, featurestore_id, featuregroup_id, name, value: value)
    expect_status_details(200)
  end

  def add_featuregroup_tag(project_id, featurestore_id, featuregroup_id, name, value: nil)
    path = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/featuregroups/#{featuregroup_id}/tags/#{name}"
    pp "put #{path}, #{value}" if defined?(@debugOpt) && @debugOpt
    put path, value
  end

  def delete_featuregroup_tag_checked(project_id, featurestore_id, featuregroup_id, tag)
    delete_featuregroup_tag(project_id, featurestore_id, featuregroup_id, tag)
    expect_status_details(204)
  end

  def delete_featuregroup_tag(project_id, featurestore_id, featuregroup_id, tag)
    delete "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s + "/tags/" + tag
  end

  def get_training_dataset_tags(project_id, featurestore_id, training_dataset_id)
    get "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/featurestores/" + featurestore_id.to_s + "/trainingdatasets/" + training_dataset_id.to_s + "/tags"
    return json_body.to_json
  end

  def get_training_dataset_tags_checked(project_id, td_id, fs_id: nil)
    fs_id = get_featurestore_id(project_id) if fs_id.nil?
    get_training_dataset_tags(project_id, fs_id, td_id)
    expect_status_details(200)
  end

  def get_training_dataset_tag(project_id, featurestore_id, training_dataset_id, name)
    get "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/featurestores/" + featurestore_id.to_s + "/trainingdatasets/" + training_dataset_id.to_s + "/tags/" + name
  end

  def add_training_dataset_tag_checked(project_id, featurestore_id, training_dataset_id, name, value:nil)
    add_training_dataset_tag(project_id, featurestore_id, training_dataset_id, name, value: value)
    expect_status_details(201)
  end

  def update_training_dataset_tag_checked(project_id, featurestore_id, training_dataset_id, name, value:nil)
    add_training_dataset_tag(project_id, featurestore_id, training_dataset_id, name, value: value)
    expect_status_details(200)
  end

  def add_training_dataset_tag(project_id, featurestore_id, training_dataset_id, name, value: nil)
    path = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/trainingdatasets/#{training_dataset_id}/tags/#{name}"
    pp "put #{path}, #{value}" if defined?(@debugOpt) && @debugOpt
    put path, value
  end

  def delete_training_dataset_tag(project_id, featurestore_id, training_dataset_id, tag)
    delete "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/featurestores/" + featurestore_id.to_s + "/trainingdatasets/" + training_dataset_id.to_s + "/tags/" + tag
  end

  def delete_training_dataset_tag_checked(project_id, featurestore_id, training_dataset_id, tag)
    delete_training_dataset_tag(project_id, featurestore_id, training_dataset_id, tag)
    expect_status_details(204)
  end
end
