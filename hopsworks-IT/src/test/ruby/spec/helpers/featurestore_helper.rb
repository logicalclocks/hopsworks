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

  def create_cached_featuregroup_checked2(project_id, featurestore_id: nil, featuregroup_name: nil, features: nil,
                                          featuregroup_description: nil, event_time: nil, parents: nil)
    featurestore_id = get_featurestore_id(project_id) if featurestore_id.nil?
    result,_ = create_cached_featuregroup(project_id, featurestore_id, featuregroup_name: featuregroup_name,
                                                features: features, featuregroup_description: featuregroup_description,
                                                event_time: event_time, parents: parents)
    expect_status_details(201)
    JSON.parse(result)
  end
  def create_cached_featuregroup_checked(project_id, featurestore_id, featuregroup_name, features: nil,
                                         featuregroup_description: nil, event_time: nil, parents: nil)
    pp "create featuregroup:#{featuregroup_name}" if defined?(@debugOpt) && @debugOpt == true
    json_result, _ = create_cached_featuregroup(project_id, featurestore_id, featuregroup_name: featuregroup_name,
                                                features: features, featuregroup_description: featuregroup_description,
                                                event_time: event_time, parents: parents)
    expect_status_details(201)
    parsed_json = JSON.parse(json_result, :symbolize_names => true)
    parsed_json[:id]
  end

  def create_cached_featuregroup_checked_return_fg(project_id, featurestore_id, featuregroup_name, features: nil,
                                         featuregroup_description: nil, event_time: nil)
    pp "create featuregroup:#{featuregroup_name}" if defined?(@debugOpt) && @debugOpt == true
    json_result, _ = create_cached_featuregroup(project_id, featurestore_id, featuregroup_name: featuregroup_name,
                                                features: features, featuregroup_description: featuregroup_description,
                                                event_time: event_time)
    expect_status_details(201)
    parsed_json = JSON.parse(json_result, :symbolize_names => true)
    parsed_json
  end

  def create_cached_featuregroup_checked2(project_id,  name: nil, version: 1, features: nil, description: nil, event_time: nil,
                                          featurestore_id: nil, featurestore_project_id: nil,
                                          expected_status: 201, parents: nil)
    featurestore_project_id = project_id if featurestore_project_id.nil?
    featurestore_id = get_featurestore_id(featurestore_project_id) if featurestore_id.nil?
    result,_ = create_cached_featuregroup(project_id, featurestore_id,
                                          featuregroup_name: name, version: version, featuregroup_description: description,
                                          features: features, event_time: event_time, parents: parents)
    expect_status_details(expected_status)
    result = JSON.parse(result) if expected_status == 201
    result
  end

  def create_cached_featuregroup(project_id, featurestore_id, features: nil, featuregroup_name: nil, online:false,
                                 version: 1, featuregroup_description: nil, statistics_config: nil, time_travel_format:
                                 "NONE", event_time: nil, expectation_suite: nil, parents: nil)
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
        timeTravelFormat: time_travel_format,
        eventTime: event_time,
        parents: parents
    }
    unless statistics_config == nil
      json_data[:statisticsConfig] = statistics_config
    end
    unless expectation_suite == nil
      json_data[:expectationSuite] = expectation_suite
    end
    json_data = json_data.to_json

    create_featuregroup_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups"
    json_result = post create_featuregroup_endpoint, json_data
    return json_result, featuregroup_name
  end

  def create_stream_featuregroup(project_id, featurestore_id, features: nil, featuregroup_name: nil,
                                 version: 1, featuregroup_description: nil, statistics_config: nil,
                                 event_time: nil, deltaStreamerJobConf: nil, backfill_offline: false,
                                 commit_time: nil, online_enabled: true)
    type = "streamFeatureGroupDTO"
    featuregroupType = "STREAM_FEATURE_GROUP"
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
        featuregroupType: featuregroupType,
        eventTime: event_time,
        deltaStreamerJobConf: deltaStreamerJobConf,
        onlineEnabled: online_enabled
    }
    unless statistics_config == nil
      json_data[:statisticsConfig] = statistics_config
    end

    if deltaStreamerJobConf.nil?
      deltaStreamerJobConf = {
        "writeOptions" => [],
        "sparkJobConfiguration" => {
            "type" => "sparkJobConfiguration",
            "spark.executor.instances" => 2,
            "spark.executor.cores" => 2,
            "spark.executor.memory" => 1500,
            "spark.dynamicAllocation.enabled" => true,
            "spark.dynamicAllocation.minExecutors" => 2,
            "spark.dynamicAllocation.maxExecutors" => 10,
            "spark.dynamicAllocation.initialExecutors" => 1
        }
      }
      json_data[:deltaStreamerJobConf] = deltaStreamerJobConf
    end
    json_data = json_data.to_json

    create_featuregroup_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups"
    json_result = post create_featuregroup_endpoint, json_data

    if backfill_offline
      # commit to offline fg
      parsed_json = JSON.parse(json_result)
      featuregroup_id = parsed_json["id"]
      featuregroup_version = parsed_json["version"]
      backfill_stream_featuregroup(featurestore_id, featuregroup_id, commit_time)
    end

    return json_result, featuregroup_name
  end

  def backfill_stream_featuregroup(featurestore_id, featuregroup_id, commit_time)
      if commit_time.nil?
        time = Time.now
        commit_time = time.to_i * 1000
        commit_date_str = Time.at(time.to_i).strftime("%Y%m%d%H%M%S")
      else
        commit_date_str = Time.at(commit_time / 1000).strftime("%Y%m%d%H%M%S")
      end

      commit_metadata = {commitDateString:commit_date_str,commitTime:commit_time,rowsInserted:4,rowsUpdated:0,rowsDeleted:0}
      commit_cached_featuregroup(@project[:id], featurestore_id, featuregroup_id, commit_metadata: commit_metadata)
  end

  def create_on_demand_featuregroup(project_id, featurestore_id, jdbcconnectorId, name: nil, version: 1, query: nil,
                                    features: nil, data_format: nil, options: nil, event_time: nil,
                                    online_enabled: false)
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
        featuregroupType: featuregroupType,
        eventTime: event_time,
        onlineEnabled: online_enabled
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
                                    featuregroup_version, query: nil, featuregroup_name: nil, featuregroup_desc: nil,
                                    features: nil)
    type = "onDemandFeaturegroupDTO"
    featuregroupType = "ON_DEMAND_FEATURE_GROUP"
    update_featuregroup_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/featuregroups/#{featuregroup_id}?updateMetadata=true"
    featuregroup_name = featuregroup_name == nil ?  "featuregroup_#{random_id}" :  featuregroup_name
    featuregroup_desc = featuregroup_desc == nil ? "description_#{random_id}" : featuregroup_desc
    query = query == nil ? "SELECT * FROM test" : query
    default_features =  [
        {
            type: "INT",
            name: "testfeature",
            description: "testfeaturedescription",
            primary: true
        }
    ]

    json_data = {
        name: featuregroup_name,
        jobs: [],
        features: features != nil ? features : default_features,
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
                                          featuregroup_name: nil, description: nil, features: nil, type: nil,
                                          featuregroupType: nil)
    type = type == nil ? "cachedFeaturegroupDTO" : type
    featuregroupType = featuregroupType == nil ? "CACHED_FEATURE_GROUP" : featuregroupType
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
            exactUniqueness: false,
            columns: statisticColumns,
            enabled: false
        }
    }
    if illegal
      json_data[:statisticsConfig][:enabled] = false
      json_data[:statisticsConfig][:histograms] = true
      json_data[:statisticsConfig][:correlations] = true
      json_data[:statisticsConfig][:exactUniqueness] = true
    end
    json_data = json_data.to_json
    json_result = put update_featuregroup_metadata_endpoint, json_data
    return json_result
  end

  def update_stream_featuregroup_metadata(project_id, featurestore_id, featuregroup_id, featuregroup_version,
                                          featuregroup_name: nil, description: nil, features: nil)
    type = "streamFeatureGroupDTO"
    featuregroupType = "STREAM_FEATURE_GROUP"
    update_cached_featuregroup_metadata(project_id, featurestore_id, featuregroup_id, featuregroup_version,
    featuregroup_name: featuregroup_name, description: description, features: features, type: type, featuregroupType: featuregroupType)
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
            exactUniqueness: false,
            columns: statisticColumns,
            enabled: false
        },
        type: "trainingDatasetDTO"
    }
    if illegal
      json_data[:statisticsConfig][:enabled] = false
      json_data[:statisticsConfig][:histograms] = true
      json_data[:statisticsConfig][:correlations] = true
      json_data[:statisticsConfig][:exactUniqueness] = true
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
        },
        type: "trainingDatasetDTO"
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
        },
        type: "trainingDatasetDTO"
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
                                     statistics_config: nil, train_split: nil)
    type = "trainingDatasetDTO"
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
        queryDTO: query,
        trainSplit: train_split,
        type: type
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

  def get_feature_view(project_id, name, version: 1, feature_store_id: nil,
                       feature_store_project_id: nil, expected_status: 200)
    feature_store_project_id = project_id if feature_store_project_id.nil?
    feature_store_id = get_featurestore_id(feature_store_project_id) if feature_store_id.nil?
    endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{feature_store_id}/featureview/#{name}/version/#{version}"
    pp endpoint if defined?(@debugOpt) && @debugOpt
    result = get endpoint
    expect_status_details(expected_status)
    pp "#{expected_status} #{expected_status == 200}"
    result = JSON.parse(result) if expected_status == 200
    result
  end

  def create_feature_view_checked(project_id, name: nil, featurestore_id: nil, query: nil, fg: nil,
                                  version: 1, features:nil, description: nil)
    featurestore_id = get_featurestore_id(project_id) if featurestore_id.nil?
    if (query.nil? && fg.nil?) || !(query.nil? || fg.nil?)
      raise "exactly one of query and fg params has to be defined"
    end
    if query.nil?
      query = {
          leftFeatureGroup: {
              id: fg["id"],
              type: fg["type"]
          },
          leftFeatures: fg["features"],
          joins: []
      }
    end
    result,_ = create_feature_view(project_id, featurestore_id, query, name: name, version: version, features: features, description: description)
    expect_status_details(201)
    JSON.parse(result)
  end
  def create_feature_view(project_id, featurestore_id, query, name: nil,
                          version: 1, features: nil, description: nil)
    type = "featureViewDTO"
    name = name == nil ? "feature_view_#{random_id}" : name
    description = description == nil ? "testfeatureviewdescription" : description

    json_data = {
      name: name,
      version: version,
      description: description,
      features: features,
      query: query,
      type: type,
    }

    create_feature_view_with_json(project_id, featurestore_id, json_data)
  end

  def create_feature_view_with_json(project_id, featurestore_id, json_data)
    create_featureview_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id.to_s}/featurestores/#{featurestore_id.to_s}/featureview"
    pp create_featureview_endpoint if defined?(@debugOpt) && @debugOpt
    pp json_data.to_json if defined?(@debugOpt) && @debugOpt
    json_result = post create_featureview_endpoint, json_data.to_json
    pp JSON.parse(json_result) if defined?(@debugOpt) && @debugOpt
    json_result
  end

  def create_feature_view_from_feature_group2(project_id, fg, name: nil, version: 1, description: nil,
                                             featurestore_project_id: nil, featurestore_id: nil,
                                             expected_status: 201)
    featurestore_project_id = project_id if featurestore_project_id.nil?
    featurestore_id = get_featurestore_id(featurestore_project_id) if featurestore_id.nil?

    result = create_feature_view_from_feature_group(project_id, featurestore_id, fg,
                                                      name: name, version: version, description: description)
    expect_status_details(expected_status)
    result = JSON.parse(result) if expected_status == 201
    result
  end

  def create_feature_view_from_feature_group(project_id, featurestore_id, fg, name: nil, version: 1, description: nil)
    query = {
      leftFeatureGroup: {
        id: fg["id"],
        type: fg["type"],
      },
      leftFeatures: fg["features"],
      joins: []
    }

    create_feature_view(project_id, featurestore_id, query, name: name, version: version, description: description)
  end

  def delete_feature_view(project_id, featurestore_id, name)
    delete_featureview_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id.to_s}/featurestores" +
      "/#{featurestore_id.to_s}/featureview/#{name}"
    json_result = delete delete_featureview_endpoint
    return json_result
  end

  def update_feature_view(project_id, featurestore_id, json_data, name, version)
    update_featureview_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id.to_s}/featurestores" +
      "/#{featurestore_id.to_s}/featureview/#{name}/version/#{version}"
    json_result = put update_featureview_endpoint, json_data.to_json
    return json_result
  end

  def delete_feature_view(project_id, name, version: 1, feature_store_id: nil,
                       feature_store_project_id: nil, expected_status: 200)
    feature_store_project_id = project_id if feature_store_project_id.nil?
    feature_store_id = get_featurestore_id(feature_store_project_id) if feature_store_id.nil?
    endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{feature_store_id}/featureview/#{name}/version/#{version}"
    pp endpoint if defined?(@debugOpt) && @debugOpt
    delete endpoint
    expect_status_details(expected_status)
  end

  def get_feature_views(project_id, featurestore_id)
    get_featureview_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id.to_s}/featurestores" +
      "/#{featurestore_id.to_s}/featureview"
    json_result = get get_featureview_endpoint
    return json_result
  end
  
  def get_feature_view_by_name(project_id, featurestore_id, name)
    get_featureview_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id.to_s}/featurestores" +
      "/#{featurestore_id.to_s}/featureview/#{name}"
    json_result = get get_featureview_endpoint
    return json_result
  end
  
  def get_feature_view_by_name_and_version(project_id, featurestore_id, name, version)
    get_featureview_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id.to_s}/featurestores" +
      "/#{featurestore_id.to_s}/featureview/#{name}/version/#{version}"
    json_result = get get_featureview_endpoint
    return json_result
  end

  def create_featureview_training_dataset_from_project(project, expected_status_code: 201, data_format: "tfrecords",
                                                       version: 1, splits: [], description: "testtrainingdatasetdescription",
                                                       statistics_config: nil, train_split: nil, is_internal: true,
                                                       connector: nil, location: nil, td_type: "HOPSFS_TRAINING_DATASET")
    featurestore_id = get_featurestore_id(project.id)
    featuregroup_suffix = short_random_id
    query = make_sample_query(project, featurestore_id, featuregroup_suffix: featuregroup_suffix)
    json_result = create_feature_view(project.id, featurestore_id, query)
    expect_status_details(201)
    featureview = JSON.parse(json_result)

    if connector == nil
      connector = get_hopsfs_training_datasets_connector(project[:projectname])
    end

    json_result  = create_featureview_training_dataset(
      project.id, featureview, connector, version: version, splits: splits, description: description,
      statistics_config: statistics_config, train_split: train_split, data_format: data_format,
      is_internal: is_internal, location: location, td_type: td_type)
    expect_status_details(expected_status_code)

    begin
      parsed_json = JSON.parse(json_result)
    rescue JSON::ParserError
      parsed_json = json_result
    end

    {"response" => parsed_json, "connector" => connector, "featureView" => featureview}
  end

  def create_featureview_training_dataset_checked(project, featureview, hopsfs_connector: nil, data_format: "tfrecords",
                                                  version: 1, splits: [], description: "testtrainingdatasetdescription",
                                                  statistics_config: nil, train_split: nil, query_param: nil, is_internal: true, location: nil)
    hopsfs_connector = get_hopsfs_training_datasets_connector(project[:projectname]) if hopsfs_connector == nil
    result = create_featureview_training_dataset(
        project.id, featureview, hopsfs_connector, version: version, splits: splits, description: description,
        statistics_config: statistics_config, train_split: train_split, data_format: data_format,
        is_internal: is_internal, location: location)
    expect_status_details(201)
    JSON.parse(result)
  end

  def create_featureview_training_dataset(project_id, featureview, hopsfs_connector, data_format: "tfrecords",
                                          version: 1, splits: [], description: "testtrainingdatasetdescription",
                                          statistics_config: nil, train_split: nil, query_param: nil, is_internal: true,
                                          location: nil, td_type: "HOPSFS_TRAINING_DATASET", extra_filter: nil)
    type = "trainingDatasetDTO"
    trainingDatasetType = is_internal ? td_type: "EXTERNAL_TRAINING_DATASET"
    create_training_dataset_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id.to_s}/featurestores/#{featureview["featurestoreId"].to_s}" +
      "/featureview/#{featureview["name"]}/version/#{featureview["version"].to_s}/trainingdatasets"
    unless query_param != nil
      create_training_dataset_endpoint = create_training_dataset_endpoint + "?#{query_param}"
    end
    json_data = {
      description: description,
      version: version,
      dataFormat: data_format,
      trainingDatasetType: trainingDatasetType,
      splits: splits,
      seed: 1234,
      trainSplit: train_split,
      location: location,
      extraFilter: extra_filter,
      type: type,
    }
    unless statistics_config == nil
      json_data[:statisticsConfig] = statistics_config
    end

    unless hopsfs_connector.nil?
	    json_data["storageConnector"] = {id: hopsfs_connector[:id]}
    end
    json_result = post create_training_dataset_endpoint, json_data.to_json
    json_result
  end

  def get_featureview_training_dataset(project, featureview, version: nil, expected_status_code: 200)
    training_dataset_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project.id.to_s}" +
      "/featurestores/#{featureview["featurestoreId"].to_s}/featureview/#{featureview["name"]}/version/#{featureview["version"].to_s}/trainingdatasets"
    unless version == nil
      training_dataset_endpoint = training_dataset_endpoint + "/version/#{version.to_s}"
    end
    training_datasets = get training_dataset_endpoint
    expect_status_details(expected_status_code)
    training_datasets
  end

  def delete_featureview_training_dataset(project, featureview, version: nil)
    training_datasets = JSON.parse(get_featureview_training_dataset(project, featureview, version: version))
    training_dataset_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project.id.to_s}" +
      "/featurestores/#{featureview["featurestoreId"].to_s}/featureview/#{featureview["name"]}/version/#{featureview["version"].to_s}/trainingdatasets"
    unless version == nil
      training_dataset_endpoint = training_dataset_endpoint + "/version/#{version.to_s}"
    end
    json_result2 = delete training_dataset_endpoint
    expect_status_details(200)

    unless version == nil
      training_datasets = {"items" => [training_datasets]}
    end

    # Make sure that the directory has been removed correctly
    training_datasets["items"].each { |training_dataset|
      get_datasets_in_path(project,
			   "#{project[:projectname]}_Training_Datasets/#{featureview['name']}_#{training_dataset['version'].to_s}",
                           query: "&type=DATASET")
      expect_status_details(400)
    }
  end

  def delete_featureview_training_dataset_data_only(project, featureview, version: nil, expected_status: 200)
    training_datasets = JSON.parse(get_featureview_training_dataset(project, featureview, version: version))
    training_dataset_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project.id.to_s}" +
      "/featurestores/#{featureview["featurestoreId"].to_s}/featureview/#{featureview["name"]}/version/#{featureview["version"].to_s}/trainingdatasets"
    if version == nil
      training_dataset_endpoint = training_dataset_endpoint + "/data"
    else
      training_dataset_endpoint = training_dataset_endpoint + "/version/#{version.to_s}/data"
    end
    json_result2 = delete training_dataset_endpoint
    expect_status_details(expected_status)

    unless version == nil
      training_datasets = {"items" => [training_datasets]}
    end

    training_datasets["items"].each { |training_dataset|
      get_datasets_in_path(project,
			   "#{project[:projectname]}_Training_Datasets/#{featureview['name']}_#{featureview['version']}_#{training_dataset['version'].to_s}",
                           query: "&type=DATASET")
      expect_status_details(200)
    }

    # should be able to retrieve metadata
    get_featureview_training_dataset(project, featureview, version: version)
  end

  def update_featureview_training_dataset_metadata(project, featureview, version, json_data)
    training_dataset_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project.id.to_s}" +
      "/featurestores/#{featureview["featurestoreId"].to_s}/featureview/#{featureview["name"]}/version/#{featureview["version"].to_s}/trainingdatasets/version/#{version.to_s}?updateMetadata=true"
    json_result = put training_dataset_endpoint, json_data.to_json
    return json_result
  end

  def update_featureview_training_dataset_stats_config(project, featureview, version, json_data)
    training_dataset_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project.id.to_s}" +
      "/featurestores/#{featureview["featurestoreId"].to_s}/featureview/#{featureview["name"]}/version/#{featureview["version"].to_s}/trainingdatasets/version/#{version.to_s}?updateStatsConfig=true"
    json_result = put training_dataset_endpoint, json_data.to_json
    return json_result
  end

  def create_external_training_dataset(project_id, featurestore_id, connector_id, name: nil, location: "",
                                       splits:[], features: nil, train_split: nil)
    type = "trainingDatasetDTO"
    trainingDatasetType = "EXTERNAL_TRAINING_DATASET"
    create_training_dataset_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/trainingdatasets"
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
        seed: 1234,
        trainSplit: train_split,
        type: type,
    }

    unless connector_id.nil?
      json_data["storageConnector"] = {
          id: connector_id
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

  #eventual consistency - epipe+opensearch - retry a number of times
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
    expect_status_details(200)
    parsed_result = JSON.parse(result)
    parsed_result[0]
  end

  def delete_featuregroup(project_id, featurestore_id, fg_id)
    delete_featuregroup_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/featuregroups/#{fg_id}"
    pp "delete #{delete_featuregroup_endpoint}" if defined?(@debugOpt) && @debugOpt
    delete delete_featuregroup_endpoint
  end

  def delete_featuregroup_checked(project_id, featurestore_id, fg_id)
    delete_featuregroup(project_id, featurestore_id, fg_id)
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
                          {commitDateString:Time.at(Time.now.to_i).strftime("%Y%m%d%H%M%S"),rowsInserted:4,rowsUpdated:0,rowsDeleted:0} : commit_metadata
    post endpoint, commit_metadata.to_json
    expect_status_details(200)
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

  #eventual consistency - epipe+opensearch - retry a number of times
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

  def register_transformation_fn(project_id, featurestore_id, transformation_fn_metadata: nil)
      plus_one = {
            "name": "plus_one",
            "version": 1,
            "sourceCodeContent": "{
              \"module_imports\": \"import numpy as np\\nimport pandas as pd\\nfrom datetime import datetime\",
              \"transformer_code\": \"def plus_one(value):\\n    return value + 1\\n\"
            }",
            "outputType": "FLOAT"
      }

      transformation_fn_metadata = transformation_fn_metadata == nil ? plus_one : transformation_fn_metadata

      json_data = transformation_fn_metadata.to_json
      endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/transformationfunctions"
      post endpoint, json_data
  end

  def make_sample_query(project, featurestore_id, featuregroup_suffix: "")
    features_a = [
      { type: "INT", name: "a_testfeature", primary: true },
      { type: "INT", name: "a_testfeature1" },
      { type: "BIGINT", name: "ts" },
    ]
    fg = create_cached_featuregroup_checked_return_fg(project.id, featurestore_id, "test_fg_a#{featuregroup_suffix}",
                                               features: features_a,
                                               event_time: "ts")
    # create second feature group
    features_b = [
      { type: "INT", name: "a_testfeature", primary: true },
      { type: "INT", name: "b_testfeature1" },
      { type: "BIGINT", name: "ts" },
    ]
    fg_b = create_cached_featuregroup_checked_return_fg(project.id, featurestore_id, "test_fg_b#{featuregroup_suffix}",
                                                 features: features_b, event_time: "ts")
    query = {
      leftFeatureGroup: {
        id: fg[:id],
        type: fg[:type],
      },
      leftFeatures: [{ name: 'a_testfeature' }, { name: 'a_testfeature1' }],
      joins: [{
                query: {
                  leftFeatureGroup: {
                    id: fg_b[:id],
                    type: fg_b[:type],
                  },
                  leftFeatures: [{ name: 'a_testfeature' }, { name: 'b_testfeature1' }]
                }
              }
      ],
      filter: {
        type: "SINGLE",
        leftFilter: {
          feature: {
            name: "a_testfeature1",
            featureGroupId: fg[:id]
          },
          condition: "GREATER_THAN",
          value: "0"
        }
      }
    }
    query
  end

  def get_feature_group_links(project_id, id, feature_store_id: nil, feature_store_project_id: nil,
                              upstreamLvls: 1, downstreamLvls: 1, expected_status: 200)
    feature_store_project_id = project_id if feature_store_project_id.nil?
    feature_store_id = get_featurestore(project_id, fs_project_id: feature_store_project_id)["featurestoreId"] if feature_store_id.nil?
    artifactPath = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{feature_store_id}/featuregroups/#{id}"
    get_featurestore_provenance_explicit_links(artifactPath, upstreamLvls: upstreamLvls, downstreamLvls: downstreamLvls, expected_status: expected_status)
  end

  def get_feature_view_links(project_id, name, version: 1, feature_store_id: nil, feature_store_project_id: nil,
                             upstreamLvls: 1, downstreamLvls: 1, expected_status: 200)
    feature_store_project_id = project_id if feature_store_project_id.nil?
    feature_store_id = get_featurestore(project_id, fs_project_id: feature_store_project_id)["featurestoreId"] if feature_store_id.nil?
    artifactPath = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{feature_store_id}/featureview/#{name}/version/#{version}"
    get_featurestore_provenance_explicit_links(artifactPath, upstreamLvls: upstreamLvls, downstreamLvls: downstreamLvls, expected_status: expected_status)
  end

  def get_training_dataset_links(project_id, feature_view_name, feature_view_version, version: 1,
                                 feature_store_id: nil, feature_store_project_id: nil,
                                 upstreamLvls: 1, downstreamLvls: 1, expected_status: 200)
    feature_store_project_id = project_id if feature_store_project_id.nil?
    feature_store_id = get_featurestore(project_id, fs_project_id: feature_store_project_id)["featurestoreId"] if feature_store_id.nil?
    artifactPath = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{feature_store_id}/featureview/#{feature_view_name}/version/#{feature_view_version}/trainingdatasets/version/#{version}"
    get_featurestore_provenance_explicit_links(artifactPath, upstreamLvls: upstreamLvls, downstreamLvls: downstreamLvls, expected_status: expected_status)
  end

  def get_featurestore_provenance_explicit_links(artifactPath, upstreamLvls: 1, downstreamLvls: 1, expected_status: 200)
    endpoint = "#{artifactPath}/provenance/links?upstreamLvls=#{upstreamLvls}&downstreamLvls=#{downstreamLvls}"
    pp endpoint if defined?(@debugOpt) && @debugOpt
    result = get endpoint
    expect_status_details(expected_status)
    result = JSON.parse(result) if expected_status == 200
    result
  end
end
