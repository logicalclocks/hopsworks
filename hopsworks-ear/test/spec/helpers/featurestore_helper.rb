=begin
 This file is part of Hopsworks
 Copyright (C) 2018, Logical Clocks AB. All rights reserved

 Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 the GNU Affero General Public License as published by the Free Software Foundation,
 either version 3 of the License, or (at your option) any later version.

 Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 PURPOSE.  See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along with this program.
 If not, see <https://www.gnu.org/licenses/>.
=end

module FeaturestoreHelper

  def get_featurestore_id(project_id)
    list_project_featurestores_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/featurestores/"
    get list_project_featurestores_endpoint
    parsed_json = JSON.parse(response.body)
    featurestore_id = parsed_json[0]["featurestoreId"]
    return featurestore_id
  end

  def create_featuregroup(project_id, featurestore_id)
    create_featuregroup_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups"
    featuregroup_name = "featuregroup_#{random_id}"
    json_data = {
        name: featuregroup_name,
        dependencies: [],
        jobName: nil,
        features: [
            {
                type: "INT",
                name: "testfeature",
                description: "testfeaturedescription",
                primary: true
            }
        ],
        description: "testfeaturegroupdescription",
        version: 1
    }
    json_data = json_data.to_json
    json_result = post create_featuregroup_endpoint, json_data
    return json_result, featuregroup_name
  end

  def update_version_featuregroup(project_id, featurestore_id, featuregroup_name, featuregroup_version)
    update_featuregroup_version_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups"
    json_data = {
        name: featuregroup_name,
        dependencies: [],
        jobName: nil,
        features: [
            {
                type: "INT",
                name: "testfeature_newversion",
                description: "testfeaturedescription_newversion",
                primary: true
            }
        ],
        description: "testfeaturegroupdescription_newversion",
        version: featuregroup_version
    }
    json_data = json_data.to_json
    json_result = post update_featuregroup_version_endpoint, json_data
    return json_result
  end

  def update_featuregroup_metadata(project_id, featurestore_id, featuregroup_id, featuregroup_version)
    update_featuregroup_metadata_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s
    json_data = {
        name: "",
        dependencies: [],
        jobName: nil,
        features: [
        ],
        description: "",
        version: featuregroup_version
    }
    json_data = json_data.to_json
    json_result = put update_featuregroup_metadata_endpoint, json_data
    return json_result
  end

  def update_training_dataset_metadata(project_id, featurestore_id, training_dataset_id, training_dataset_version)
    update_training_dataset_metadata_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/featurestores/" + featurestore_id.to_s + "/trainingdatasets/" + training_dataset_id.to_s
    json_data = {
        name: "new_dataset_name",
        dependencies: [],
        jobName: nil,
        description: "new_testtrainingdatasetdescription",
        version: training_dataset_version,
        dataFormat: "tfrecords"
    }
    json_data = json_data.to_json
    json_result = put update_training_dataset_metadata_endpoint, json_data
    return json_result
  end

  def create_training_dataset(project_id, featurestore_id)
    create_training_dataset_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/featurestores/" + featurestore_id.to_s + "/trainingdatasets"
    training_dataset_name = "training_dataset_#{random_id}"
    json_data = {
        name: training_dataset_name,
        dependencies: [],
        jobName: nil,
        description: "testtrainingdatasetdescription",
        version: 1,
        dataFormat: "tfrecords"
    }
    json_data = json_data.to_json
    json_result = post create_training_dataset_endpoint, json_data
    return json_result, training_dataset_name
  end

  def update_version_training_dataset(project_id, featurestore_id, training_dataset_name, version)
    create_training_dataset_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/featurestores/" + featurestore_id.to_s + "/trainingdatasets"
    json_data = {
        name: training_dataset_name,
        dependencies: [],
        jobName: nil,
        description: "testtrainingdatasetdescription2",
        version: version,
        dataFormat: "parquet"
    }
    json_data = json_data.to_json
    json_result = post create_training_dataset_endpoint, json_data
    return json_result, training_dataset_name
  end

  def get_featurestore_tour_job_name
    return "featurestore_tour_job"
  end

end