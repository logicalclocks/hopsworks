=begin
 Copyright (C) 2020, Logical Clocks AB. All rights reserved
=end

module FeatureStoreTagHelper
  def getAllFeatureStoreTags
    get "#{ENV['HOPSWORKS_API']}/tags"
  end

  def getFeatureStoreTagByName(name)
    get "#{ENV['HOPSWORKS_API']}/tags/#{name}"
  end

  def createFeatureStoreTag(name, type)
    post "#{ENV['HOPSWORKS_API']}/tags?name=#{name}&type=#{type}"
  end

  def updateFeatureStoreTag(name, newName, type)
    put "#{ENV['HOPSWORKS_API']}/tags/#{name}?name=#{newName}&type=#{type}"
  end

  def deleteFeatureStoreTag(name)
    delete "#{ENV['HOPSWORKS_API']}/tags/#{name}"
  end

  def get_featuregroup_tags(project_id, featurestore_id, featuregroup_id)
    get "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s + "/tags"
    return json_body.to_json
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
    if value == nil
      put "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s + "/tags/" + name
    else
      put "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s + "/tags/" + name + "?value=" + value
    end
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
    if value == nil
      put "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/featurestores/" + featurestore_id.to_s + "/trainingdatasets/" + training_dataset_id.to_s + "/tags/" + name
    else
      put "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/featurestores/" + featurestore_id.to_s + "/trainingdatasets/" + training_dataset_id.to_s + "/tags/" + name + "?value=" + value
    end
  end

  def delete_training_dataset_tag(project_id, featurestore_id, training_dataset_id, tag)
    delete "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/featurestores/" + featurestore_id.to_s + "/trainingdatasets/" + training_dataset_id.to_s + "/tags/" + tag
  end

end
