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

  def get_featurestores(project_id)
    endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/featurestores/"
    get endpoint
    JSON.parse(response.body)
  end

  def get_rule_definitions(query="")
    get "#{ENV['HOPSWORKS_API']}/rules/" + query
    JSON.parse(response.body)
  end

  def get_rule_definition_by_name(name)
    get "#{ENV['HOPSWORKS_API']}/rules/" + name
    JSON.parse(response.body)
  end

  def json_data_expectation
    {
      "description": "validate year correctness",
      "features": [
        "testfeature"
      ],
      "name": "exp0",
      "rules": [
        {
          "legalValues": [
            "a"
          ],
          "level": "ERROR",
          "min": 2018.0,
          "name": "HAS_MIN"
        },
        {
          "level": "WARNING",
          "max": 2021.0,
          "name": "HAS_MAX"
        }
      ]
    }
  end

  def json_data_validation
    {
      "validationTime": 1612285461346,
      "commitTime": 1612285461347,
      "expectationResults": [
        {
          "expectation": {
            "features": [
              "salary",
              "commission"
            ],
            "rules": [
              {
                "level": "WARNING",
                "min": 0.0,
                "name": "HAS_MIN"
              },
              {
                "level": "ERROR",
                "max": 1000000.0,
                "name": "HAS_MAX"
              }
            ],
            "description": "min and max sales limits",
            "name": "sales"
          },
          "results": [
            {
              "feature": "salary",
              "message": "Success",
              "rule": {
                "level": "ERROR",
                "max": 1000000.0,
                "name": "HAS_MAX"
              },
              "status": "SUCCESS",
              "value": "140893.765625"
            },
            {
              "feature": "commission",
              "message": "Success",
              "rule": {
                "level": "ERROR",
                "max": 1000000.0,
                "name": "HAS_MAX"
              },
              "status": "SUCCESS",
              "value": "52593.62890625"
            },
            {
              "feature": "salary",
              "message": "Success",
              "rule": {
                "level": "WARNING",
                "min": 0.0,
                "name": "HAS_MIN"
              },
              "status": "SUCCESS",
              "value": "20000.0"
            },
            {
              "feature": "commission",
              "message": "Success",
              "rule": {
                "level": "WARNING",
                "min": 0.0,
                "name": "HAS_MIN"
              },
              "status": "SUCCESS",
              "value": "0.0"
            }
          ],
          "status": "SUCCESS"
        }
      ]
    }
  end

  def create_expectation(project_id, featurestore_id, name, feature="testfeature")
    json_data = json_data_expectation
    json_data[:name] = name
    json_data[:features][0] = feature
    json_data = json_data.to_json
    endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/featurestores/" + featurestore_id.to_s + "/expectations"
    put endpoint, json_data
  end

  def delete_expectation(project_id, featurestore_id, name)
    delete "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/featurestores/" + featurestore_id.to_s + "/expectations/" + name
  end

  def get_feature_store_expectations(project_id, featurestore_id, name="", query="")
    get "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/featurestores/" + featurestore_id.to_s + "/expectations/" + name + query
    JSON.parse(response.body)
  end

  def get_feature_group_expectations(project_id, featurestore_id, featuregroup_id, name="", query="")
    get "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s + "/expectations/" + name + query
    JSON.parse(response.body)
  end

  def attach_expectation(project_id, featurestore_id, featuregroup_id, name)
    put "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s + "/expectations/" + name
  end

  def detach_expectation(project_id, featurestore_id, featuregroup_id, name)
    delete "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s + "/expectations/" + name
  end

  def create_validation(project_id, featurestore_id, featuregroup_id, validation_time=json_data_validation[:validationTime])
    json_data = json_data_validation
    json_data[:validationTime] = validation_time
    json_data = json_data.to_json
    endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s + "/validations"
    put endpoint, json_data
  end

  def get_validations(project_id, featurestore_id, featuregroup_id, id="")
    get "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s + "/validations/" + id.to_s
    JSON.parse(response.body)
  end

  def get_featurestores_checked(project_id)
    result = get_featurestores(project_id)
    expect_status_details(200)
    result
  end

  def get_featurestore(user_project_id, fs_project_id:nil)
    featurestores = get_featurestores_checked(user_project_id)
    fs_project_id = user_project_id if fs_project_id.nil?
    fs = featurestores.select{|fs| fs["projectId"] == fs_project_id}
    expect(fs.length).to eq(1)
    fs[0]
  end

  def get_featurestore_id(project_id)
    get "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/featurestores/"
    parsed_json = JSON.parse(response.body)
    parsed_json[0]["featurestoreId"]
  end

  def get_featurestore_name(project_id)
    get "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/featurestores/"
    parsed_json = JSON.parse(response.body)
    parsed_json[0]["featurestoreName"]
  end

  def create_cached_featuregroup_checked(project_id, featurestore_id, featuregroup_name, features: nil,
                                         featuregroup_description: nil)
    pp "create featuregroup:#{featuregroup_name}" if defined?(@debugOpt) && @debugOpt == true
    json_result, _ = create_cached_featuregroup(project_id, featurestore_id, featuregroup_name: featuregroup_name,
                                                features: features, featuregroup_description: featuregroup_description)
    expect_status_details(201)
    parsed_json = JSON.parse(json_result, :symbolize_names => true)
    parsed_json[:id]
  end

  def create_cached_featuregroup(project_id, featurestore_id, features: nil, featuregroup_name: nil, online:false,
                                 version: 1, featuregroup_description: nil, statistics_config: nil, time_travel_format:
                                     "NONE")
    type = "cachedFeaturegroupDTO"
    features = features == nil ? [{type: "INT", name: "testfeature", description: "testfeaturedescription",
                                   primary: true, onlineType: "INT", partition: false}] : features
    featuregroup_name = featuregroup_name == nil ? "featuregroup_#{random_id}" : featuregroup_name
    featuregroup_description = featuregroup_description == nil ? "testfeaturegroupdescription" : featuregroup_description
    json_data = {
        name: featuregroup_name,
        jobs: [],
        features: features,
        description: featuregroup_description,
        version: version,
        type: type,
        onlineEnabled: online,
        timeTravelFormat: time_travel_format
    }
    unless statistics_config == nil
      json_data[:statisticsConfig] = statistics_config
    end
    json_data = json_data.to_json

    create_featuregroup_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups"
    json_result = post create_featuregroup_endpoint, json_data
    return json_result, featuregroup_name
  end

  def create_on_demand_featuregroup(project_id, featurestore_id, jdbcconnectorId, name: nil, version: 1, query: nil,
                                    features: nil, data_format: nil, options: nil)
    type = "onDemandFeaturegroupDTO"
    featuregroupType = "ON_DEMAND_FEATURE_GROUP"
    create_featuregroup_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/featuregroups"
    featuregroup_name = name == nil ? "featuregroup_#{random_id}" : name
    query = query == nil ? "SELECT * FROM test" : query
    features = features == nil ?
                   [{type: "INT", name: "testfeature", description: "testfeaturedescription", primary: true}] :
                   features

    json_data = {
        name: featuregroup_name,
        features: features,
        description: "testfeaturegroupdescription",
        version: version,
        type: type,
        storageConnector: {
            id: jdbcconnectorId,
        },
        query: query,
        featuregroupType: featuregroupType
    }

    unless data_format == nil
      json_data[:dataFormat] = data_format
    end

    unless options == nil
      json_data[:options] = options
    end

    json_data = json_data.to_json
    json_result = post create_featuregroup_endpoint, json_data
    return json_result, featuregroup_name
  end

  def update_on_demand_featuregroup(project_id, featurestore_id, jdbcconnectorId, featuregroup_id,
                                    featuregroup_version, query: nil, featuregroup_name: nil, featuregroup_desc: nil)
    type = "onDemandFeaturegroupDTO"
    featuregroupType = "ON_DEMAND_FEATURE_GROUP"
    update_featuregroup_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/featuregroups/#{featuregroup_id}?updateMetadata=true"
    featuregroup_name = featuregroup_name == nil ?  "featuregroup_#{random_id}" :  featuregroup_name
    featuregroup_desc = featuregroup_desc == nil ? "description_#{random_id}" : featuregroup_desc
    query = query == nil ? "SELECT * FROM test" : query

    json_data = {
        name: featuregroup_name,
        jobs: [],
        features: [
            {
                type: "INT",
                name: "testfeature",
                description: "testfeaturedescription",
                primary: true
            }
        ],
        description: featuregroup_desc,
        version: featuregroup_version,
        type: type,
        storageConnector: {
            id: jdbcconnectorId,
        },
        query: query,
        featuregroupType: featuregroupType
    }
    json_data = json_data.to_json
    json_result = put update_featuregroup_endpoint, json_data
    return json_result, featuregroup_name
  end

  def update_cached_featuregroup_metadata(project_id, featurestore_id, featuregroup_id, featuregroup_version,
                                          featuregroup_name: nil, description: nil, features: nil)
    type = "cachedFeaturegroupDTO"
    featuregroupType = "CACHED_FEATURE_GROUP"
    update_featuregroup_metadata_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s + "?updateMetadata=true"
    default_features = [
        {
            type: "INT",
            name: "testfeature",
            description: "testfeaturedescription",
            primary: true,
            onlineType: "INT",
            partition: false
        },
    ]
    json_data = {
        name: featuregroup_name != nil ? featuregroup_name : "",
        jobs: [],
        features: features != nil ? features : default_features,
        description: description != nil ? description : "testfeaturegroupdescription",
        version: featuregroup_version,
        type: type,
        featuregroupType: featuregroupType
    }
    json_data = json_data.to_json
    put update_featuregroup_metadata_endpoint, json_data
  end

  def update_cached_featuregroup_stats_settings(project_id, featurestore_id, featuregroup_id, featuregroup_version,
                                                illegal: false, statisticColumns: ["testfeature"])
    type = "cachedFeaturegroupDTO"
    featuregroupType = "CACHED_FEATURE_GROUP"
    update_featuregroup_metadata_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/featuregroups/#{featuregroup_id}?updateStatsConfig=true"
    json_data = {
        type: type,
        featuregroupType: featuregroupType,
        version: featuregroup_version,
        statisticsConfig: {
            histograms: false,
            correlations: false,
            columns: statisticColumns,
            enabled: false
        }
    }
    if illegal
      json_data[:statisticsConfig][:enabled] = false
      json_data[:statisticsConfig][:histograms] = true
      json_data[:statisticsConfig][:correlations] = true
    end
    json_data = json_data.to_json
    json_result = put update_featuregroup_metadata_endpoint, json_data
    return json_result
  end

  def update_training_dataset_stats_config(project_id, featurestore_id, training_dataset_id, training_dataset_version,
                                                illegal: false, statisticColumns: ["testfeature"])
    update_trainingdataset_metadata_endpoint =
        "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/trainingdatasets/#{training_dataset_id}?updateStatsConfig=true"
    json_data = {
        version: training_dataset_version,
        statisticsConfig: {
            histograms: false,
            correlations: false,
            columns: statisticColumns,
            enabled: false
        }
    }
    if illegal
      json_data[:statisticsConfig][:enabled] = false
      json_data[:statisticsConfig][:histograms] = true
      json_data[:statisticsConfig][:correlations] = true
    end
    json_data = json_data.to_json
    json_result = put update_trainingdataset_metadata_endpoint, json_data
    return json_result
  end

  def enable_cached_featuregroup_online(project_id, featurestore_id, featuregroup_id)
    enable_featuregroup_online_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/featuregroups/#{featuregroup_id}?enableOnline=true"
    json_data = {
        features: [
            {
                type: "INT",
                name: "testfeature",
                description: "testfeaturedescription",
                primary: true,
                partition: false,
                onlineType: "INT(11)"
            }
        ],
        description: "",
        id: featuregroup_id,
        onlineEnabled: true,
        type: "cachedFeaturegroupDTO"
    }
    json_result = put enable_featuregroup_online_endpoint, json_data.to_json
    return json_result
  end

  def disable_cached_featuregroup_online(project_id, featurestore_id, featuregroup_id, featuregroup_version)
    type = "cachedFeaturegroupDTO"
    disable_featuregroup_online_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s + "?disableOnline=true"
    json_data = {
        name: "",
        jobs: [],
        features: [],
        description: "",
        version: featuregroup_version,
        onlineEnabled: false,
        type: type
    }
    json_data = json_data.to_json
    json_result = put disable_featuregroup_online_endpoint, json_data
    return json_result
  end

  def update_hopsfs_training_dataset_metadata(project_id, featurestore_id, training_dataset_id, dataFormat,
                                              hopsfs_connector, jobs: nil)
    trainingDatasetType = "HOPSFS_TRAINING_DATASET"
    update_training_dataset_metadata_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/featurestores/" + featurestore_id.to_s + "/trainingdatasets/" + training_dataset_id.to_s + "?updateMetadata=true"
    json_data = {
        name: "new_dataset_name",
        jobs: jobs,
        description: "new_testtrainingdatasetdescription",
        version: 1,
        dataFormat: dataFormat,
        trainingDatasetType: trainingDatasetType,
        storageConnector: {
            id: hopsfs_connector.id
        }
    }
    json_data = json_data.to_json
    json_result = put update_training_dataset_metadata_endpoint, json_data
    return json_result
  end

  def update_external_training_dataset_metadata(project_id, featurestore_id, training_dataset_id, name,
                                                description, s3_connector_id)
    trainingDatasetType = "EXTERNAL_TRAINING_DATASET"
    update_training_dataset_metadata_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/featurestores/" + featurestore_id.to_s + "/trainingdatasets/" + training_dataset_id.to_s + "?updateMetadata=true"
    json_data = {
        name: name,
        jobs: [],
        description: description,
        version: 1,
        dataFormat: "parquet",
        trainingDatasetType: trainingDatasetType,
        storageConnector: {
            id: s3_connector_id
        }
    }
    json_data = json_data.to_json
    json_result = put update_training_dataset_metadata_endpoint, json_data
    return json_result
  end

  def create_hopsfs_training_dataset_checked(project_id, featurestore_id, connector, name: nil, features: nil , description: nil)
    json_result, name_aux = create_hopsfs_training_dataset(project_id, featurestore_id, connector, name:name, features: features, description: description)
    pp "create training dataset:#{name_aux}" if defined?(@debugOpt) && @debugOpt
    expect_status_details(201)
    parsed_json = JSON.parse(json_result, :symbolize_names => true)
    pp parsed_json if defined?(@debugOpt) && @debugOpt
    return parsed_json, name_aux
  end

  def create_hopsfs_training_dataset(project_id, featurestore_id, hopsfs_connector, name:nil, data_format: nil,
                                     version: 1, splits: [], features: nil, description: nil, query: nil,
                                     statistics_config: nil)
    trainingDatasetType = "HOPSFS_TRAINING_DATASET"
    create_training_dataset_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/featurestores/" + featurestore_id.to_s + "/trainingdatasets"
    name = name == nil ? "training_dataset_#{random_id}" : name
    data_format = data_format == nil ? "tfrecords" : data_format
    description = description == nil ? "testtrainingdatasetdescription" : description
    if features == nil && query == nil
      features = [
          {
              type: "INT",
              name: "testfeature"
          },
          {
              type: "INT",
              name: "testfeature2"
          }
      ]
    end
    json_data = {
        name: name,
        jobs: [],
        description: description,
        version: version,
        dataFormat: data_format,
        trainingDatasetType: trainingDatasetType,
        features: features,
        splits: splits,
        seed: 1234,
        queryDTO: query
    }

    unless statistics_config == nil
      json_data[:statisticsConfig] = statistics_config
    end

    unless hopsfs_connector.nil?
      json_data["storageConnector"] = {
          id: hopsfs_connector.id
      }
    end

    json_result = post create_training_dataset_endpoint, json_data.to_json
    [json_result, name]
  end

  def create_external_training_dataset(project_id, featurestore_id, s3_connector_id, name: nil, location: "",
                                       splits:[], features: nil)
    trainingDatasetType = "EXTERNAL_TRAINING_DATASET"
    create_training_dataset_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/featurestores/" + featurestore_id.to_s + "/trainingdatasets"
    if name == nil
      training_dataset_name = "training_dataset_#{random_id}"
    else
      training_dataset_name = name
    end
    default_features = [
        {
            type: "INT",
            name: "testfeature"
        },
        {
            type: "INT",
            name: "testfeature2"
        }
    ]
    json_data = {
        name: training_dataset_name,
        jobs: [],
        description: "testtrainingdatasetdescription",
        version: 1,
        dataFormat: "tfrecords",
        location: location,
        trainingDatasetType: trainingDatasetType,
        features: features == nil ? default_features : features,
        splits: splits,
        seed: 1234
    }

    unless s3_connector_id.nil?
      json_data["storageConnector"] = {
          id: s3_connector_id
      }
    end

    json_data = json_data.to_json
    json_result = post create_training_dataset_endpoint, json_data
    return json_result, training_dataset_name
  end

  def get_featurestore_tour_job_name
    return "featurestore_tour_job"
  end

  def create_cached_featuregroup_with_partition(project_id, featurestore_id, time_travel_format: "NONE", online: false)
    type = "cachedFeaturegroupDTO"
    featuregroupType = "CACHED_FEATURE_GROUP"
    create_featuregroup_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups"

    featuregroup_name = "featuregroup_#{random_id}"
    json_data = {
        name: featuregroup_name,
        jobs: [],
        features: [
            {
                type: "INT",
                name: "testfeature",
                description: "testfeaturedescription",
                primary: true,
                partition: false
            },
            {
                type: "INT",
                name: "testfeature2",
                description: "testfeaturedescription2",
                primary: false,
                partition: true
            }
        ],
        description: "testfeaturegroupdescription",
        version: 1,
        type: type,
        featuregroupType: featuregroupType,
        timeTravelFormat: time_travel_format,
        onlineEnabled: online
    }
    json_data = json_data.to_json
    json_result = post create_featuregroup_endpoint, json_data
    return json_result, featuregroup_name
  end

  def get_featuregroups(project_id, fs_id, query="")
    get_fg_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{fs_id}/featuregroups?#{query}"
    pp "get #{get_fg_endpoint}" if defined?(@debugOpt) && @debugOpt
    result = get get_fg_endpoint
    JSON.parse(result)
  end

  def get_featuregroup(project_id, name, version: 1, fs_id: nil, fs_project_id: nil)
    fs_project_id = project_id if fs_project_id.nil?
    fs_id = get_featurestore(project_id, fs_project_id: fs_project_id)["featurestoreId"] if fs_id.nil?
    get_fg_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{fs_id}/featuregroups/#{name}?version=#{version}"
    pp "get #{get_fg_endpoint}" if defined?(@debugOpt) && @debugOpt
    result = get get_fg_endpoint
    JSON.parse(result)
  end

  def get_featuregroup_checked(project_id, name, version: 1, fs_id: nil, fs_project_id: nil)
    result = get_featuregroup(project_id, name, version: version, fs_id: fs_id, fs_project_id: fs_project_id)
    expect_status_details(200)
    result
  end

  def featuregroup_exists(project_id, name, version: 1, fs_id: nil, fs_project_id: nil)
    get_featuregroup(project_id, name, version: version, fs_id: fs_id, fs_project_id: fs_project_id)
    if response.code == resolve_status(200, response.code)
      true
    elsif response.code == resolve_status(404, response.code)  && json_body[:errorCode] == 270009
      false
    else
      expect_status_details(200)
    end
  end

  #eventual consistency - epipe+elastic - retry a number of times
  def check_featuregroup_usage(project_id, fg_id, check, fs_id: nil, fs_project_id: nil, type: [], retries: 10)
    wait_result = wait_for_me_time(retries) do
      result = featuregroup_usage(project_id, fg_id, fs_id: fs_id, fs_project_id: fs_project_id, type: type)
      pp result if defined?(@debugOpt) && @debugOpt
      begin
        expect(result["readLast"]).not_to be_nil if check["readLast"]
        expect(result["writeLast"]).not_to be_nil if check["writeLast"]
        expect(result["readHistory"].length).to eq(check["readHistory"]) if check["readHistory"]
        expect(result["writeHistory"].length).to eq(check["writeHistory"]) if check["writeHistory"]
        expect(result["readCurrent"].length).to eq(check["readCurrent"]) if check["readCurrent"]
        expect(result["writeCurrent"].length).to eq(check["writeCurrent"]) if check["writeCurrent"]
        { 'success' => true }
      rescue RSpec::Expectations::ExpectationNotMetError => e
        pp "rescued ex - retrying" if defined?(@debugOpt) && @debugOpt
        { 'success' => false, 'ex' => e }
      end
    end
    raise wait_result["ex"] unless wait_result["success"]
  end
  def featuregroup_usage(project_id, fg_id, fs_id: nil, fs_project_id: nil, type: [])
    fs_project_id = project_id if fs_project_id.nil?
    fs_id = get_featurestore(project_id, fs_project_id: fs_project_id)["featurestoreId"] if fs_id.nil?
    usage_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{fs_id}/featuregroups/#{fg_id}/provenance/usage"
    query = ''
    type.each do |t|
      query = query + '&type=' + t;
    end
    query[0] = '?'
    pp "#{usage_endpoint}#{query}" if defined?(@debugOpt) && @debugOpt
    result = get "#{usage_endpoint}#{query}"
    expect_status_details(200)
    JSON.parse(result)
  end

  def get_trainingdataset(project_id, name, version: 1, fs_id: nil, fs_project_id: nil)
    fs_project_id = project_id if fs_project_id.nil?
    fs_id = get_featurestore(project_id, fs_project_id: fs_project_id)["featurestoreId"] if fs_id.nil?
    get_trainingdataset_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{fs_id}/trainingdatasets/#{name}?version=#{version}"
    pp "get #{get_trainingdataset_endpoint}" if defined?(@debugOpt) && @debugOpt
    get get_trainingdataset_endpoint
  end

  def get_trainingdataset_checked(project_id, name, version: 1, fs_id: nil, fs_project_id: nil)
    result = get_trainingdataset(project_id, name, version: version, fs_id: fs_id, fs_project_id: fs_project_id)
    expect_status(200)
    parsed_result = JSON.parse(result)
    parsed_result[0]
  end

  def delete_featuregroup_checked(project_id, featurestore_id, fg_id)
    delete_featuregroup_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/featuregroups/#{fg_id}"
    pp "delete #{delete_featuregroup_endpoint}" if defined?(@debugOpt) && @debugOpt
    delete delete_featuregroup_endpoint
    expect_status_details(200)
  end

  def delete_trainingdataset_checked(project_id, featurestore_id, td_id)
    delete_trainingdataset_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/trainingdatasets/#{td_id}"
    pp "delete #{delete_trainingdataset_endpoint}" if defined?(@debugOpt) && @debugOpt
    delete delete_trainingdataset_endpoint
    expect_status_details(200)
  end

  def commit_cached_featuregroup(project_id, featurestore_id, featuregroup_id, commit_metadata: nil)
    endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/featuregroups/#{featuregroup_id}/commits"
    commit_metadata = commit_metadata == nil ?
                          {commitDateString:"20201024221125",rowsInserted:4,rowsUpdated:0,rowsDeleted:0} : commit_metadata
    post endpoint, commit_metadata.to_json
  end

  def trainingdataset_exists(project_id, name, version: 1, fs_id: nil, fs_project_id: nil)
    get_trainingdataset(project_id, name, version: version, fs_id: fs_id, fs_project_id: fs_project_id)
    if response.code == resolve_status(200, response.code)
      true
    elsif response.code == resolve_status(404, response.code) && json_body[:errorCode] == 270012
      false
    else
      expect_status_details(200)
    end
  end

  #eventual consistency - epipe+elastic - retry a number of times
  def check_trainingdataset_usage(project_id, td_id, check, fs_id: nil, fs_project_id: nil, type: [], retries: 10)
    wait_result = wait_for_me_time(retries) do
      result = trainingdataset_usage(project_id, td_id, fs_id: fs_id, fs_project_id: fs_project_id, type: type)
      pp result if defined?(@debugOpt) && @debugOpt
      begin
        expect(result["readLast"]).not_to be_nil if check["readLast"]
        expect(result["writeLast"]).not_to be_nil if check["writeLast"]
        expect(result["readHistory"].length).to eq(check["readHistory"]) if check["readHistory"]
        expect(result["writeHistory"].length).to eq(check["writeHistory"]) if check["writeHistory"]
        expect(result["readCurrent"].length).to eq(check["readCurrent"]) if check["readCurrent"]
        expect(result["writeCurrent"].length).to eq(check["writeCurrent"]) if check["writeCurrent"]
        { 'success' => true }
      rescue RSpec::Expectations::ExpectationNotMetError => e
        pp "rescued ex - retrying" if defined?(@debugOpt) && @debugOpt
        { 'success' => false, 'ex' => e }
      end
    end
    raise wait_result["ex"] unless wait_result["success"]
  end
  def trainingdataset_usage(project_id, td_id, fs_id: nil, fs_project_id: nil, type: [])
    fs_project_id = project_id if fs_project_id.nil?
    fs_id = get_featurestore(project_id, fs_project_id: fs_project_id)["featurestoreId"] if fs_id.nil?
    usage_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{fs_id}/trainingdatasets/#{td_id}/provenance/usage"
    query = ''
    type.each do |t|
      query = query + '&type=' + t;
    end
    query[0] = '?'
    pp "#{usage_endpoint}#{query}" if defined?(@debugOpt) && @debugOpt
    result = get "#{usage_endpoint}#{query}"
    expect_status_details(200)
    JSON.parse(result)
  end
end