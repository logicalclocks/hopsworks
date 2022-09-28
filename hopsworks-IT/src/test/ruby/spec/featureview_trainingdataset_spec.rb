# This file is part of Hopsworks
# Copyright (C) 2020, Logical Clocks AB. All rights reserved
#
# Hopsworks is free software: you can redistribute it and/or modify it under the terms of
# the GNU Affero General Public License as published by the Free Software Foundation,
# either version 3 of the License, or (at your option) any later version.
#
# Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
# without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
# PURPOSE.  See the GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License along with this program.
# If not, see <https://www.gnu.org/licenses/>.

require 'json'

describe "On #{ENV['OS']}" do
  after(:all) { clean_all_test_projects(spec: "featureview_trainingdataset") }

  describe "training dataset" do
    describe "internal" do
      context 'with valid project, featurestore service enabled' do
        before :all do
          with_valid_project
        end

        it "should be able to add a hopsfs training dataset to the featurestore" do
          featurestore_name = get_featurestore_name(@project.id)
          all_metadata = create_featureview_training_dataset_from_project(@project)
          parsed_json = all_metadata["response"]
          connector = all_metadata["connector"]
          featureview = all_metadata["featureView"]

          expect(parsed_json.key?("id")).to be true
          expect(parsed_json.key?("featurestoreName")).to be true
          expect(parsed_json.key?("name")).to be true
          expect(parsed_json["creator"].key?("email")).to be true
          expect(parsed_json.key?("location")).to be true
          expect(parsed_json.key?("version")).to be true
          expect(parsed_json.key?("dataFormat")).to be true
          expect(parsed_json.key?("trainingDatasetType")).to be true
          expect(parsed_json.key?("location")).to be true
          expect(parsed_json.key?("inodeId")).to be true
          expect(parsed_json.key?("seed")).to be true
          expect(parsed_json["featurestoreName"] == featurestore_name).to be true
          expect(parsed_json["name"] == "#{featureview['name']}_#{featureview['version']}").to be true
          expect(parsed_json["trainingDatasetType"] == "HOPSFS_TRAINING_DATASET").to be true
          expect(parsed_json["storageConnector"]["id"] == connector.id).to be true
          expect(parsed_json["seed"] == 1234).to be true

          # Make sure the location contains the scheme (hopsfs) and the authority
          uri = URI(parsed_json["location"])
          expect(uri.scheme).to eql("hopsfs")
          # If the port is available we can assume that the IP is as well.
          expect(uri.port).to eql(8020)
        end

        it "should not be able to add a hopsfs training dataset to the featurestore without specifying a data format" do
          all_metadata = create_featureview_training_dataset_from_project(@project, expected_status_code: 400, data_format: "not_exist")
          parsed_json = all_metadata["response"]
          expect(parsed_json.key?("errorCode")).to be true
          expect(parsed_json.key?("errorMsg")).to be true
          expect(parsed_json.key?("usrMsg")).to be true
          expect(parsed_json["errorCode"] == 270057).to be true
        end

        it "should not be able to add a hopsfs training dataset to the featurestore with an invalid version" do
          all_metadata = create_featureview_training_dataset_from_project(@project, expected_status_code: 400, version: -1)
          parsed_json = all_metadata["response"]
          expect(parsed_json.key?("errorCode")).to be true
          expect(parsed_json.key?("errorMsg")).to be true
          expect(parsed_json.key?("usrMsg")).to be true
          expect(parsed_json["errorCode"] == 270058).to be true
        end

        it "should be able to add a new hopsfs training dataset without version to the featurestore" do
          all_metadata = create_featureview_training_dataset_from_project(@project, version: nil)
          parsed_json = all_metadata["response"]
          expect(parsed_json["version"] == 1).to be true
        end

        it "should be able to add a new version of an existing hopsfs training dataset without version to the featurestore" do
          all_metadata = create_featureview_training_dataset_from_project(@project)
          featureview = all_metadata["featureView"]
          connector = all_metadata["connector"]

          # add second version
          json_result, _ = create_featureview_training_dataset(@project.id, featureview, connector,
                                                               version: nil)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          # version should be incremented to 2
          expect(parsed_json["version"] == 2).to be true
        end

        it "should be able to add a hopsfs training dataset to the featurestore with splits" do
          splits = [
            {
              name: "test_split",
              percentage: 0.8,
              splitType: "RANDOM_SPLIT"
            },
            {
              name: "train_split",
              percentage: 0.2,
              splitType: "RANDOM_SPLIT"
            }
          ]
          all_metadata = create_featureview_training_dataset_from_project(@project, splits: splits,
                                                                          train_split: "train_split")
          parsed_json = all_metadata["response"]
          expect(parsed_json.key?("splits")).to be true
          expect(parsed_json["splits"].length).to be 2
        end

        it "should be able to add a hopsfs training dataset to the featurestore with time series splits" do
          splits = [
            {
              name: "train",
              startTime: 1000,
              endTime: 2000,
              splitType: "TIME_SERIES_SPLIT"
            },
            {
              name: "test",
              startTime: 5000,
              endTime: 6000,
              splitType: "TIME_SERIES_SPLIT"
            },
            {
              name: "validation",
              startTime: 3000,
              endTime: 4000,
              splitType: "TIME_SERIES_SPLIT"
            }
          ]
          all_metadata = create_featureview_training_dataset_from_project(@project, splits: splits,
                                                                          train_split: "train")
          parsed_json = all_metadata["response"]
          expect(parsed_json.key?("splits")).to be true
          expect(parsed_json["splits"].length).to be 3
          sorted_splits = parsed_json["splits"].sort_by {|split| split["startTime"]}
          expect(sorted_splits[0]["splitType"]).to eql "TIME_SERIES_SPLIT"
          expect(sorted_splits[0]["name"]).to eql "train"
          expect(sorted_splits[0]["startTime"]).to eql 1000
          expect(sorted_splits[0]["endTime"]).to eql 2000
          expect(sorted_splits[1]["splitType"]).to eql "TIME_SERIES_SPLIT"
          expect(sorted_splits[1]["name"]).to eql "validation"
          expect(sorted_splits[1]["startTime"]).to eql 3000
          expect(sorted_splits[1]["endTime"]).to eql 4000
          expect(sorted_splits[2]["splitType"]).to eql "TIME_SERIES_SPLIT"
          expect(sorted_splits[2]["name"]).to eql "test"
          expect(sorted_splits[2]["startTime"]).to eql 5000
          expect(sorted_splits[2]["endTime"]).to eql 6000
        end

        it "should not be able to add a hopsfs training dataset to the featurestore with incorrect time series splits" do
          splits = [
            {
              name: "train",
              startTime: 2000,
              endTime: 1000,
              splitType: "TIME_SERIES_SPLIT"
            },
            {
              name: "test",
              startTime: 2000,
              endTime: 1000,
              splitType: "TIME_SERIES_SPLIT"
            },
            {
              name: "validation",
              startTime: 20000,
              endTime: 10000,
              splitType: "TIME_SERIES_SPLIT"
            }
          ]
          all_metadata = create_featureview_training_dataset_from_project(@project, splits: splits,
                                                                          train_split: "train",
                                                                          expected_status_code: 400)
        end

        it "should not be able to add a hopsfs training dataset to the featurestore with a non numeric split percentage" do
          split = [{ name: "train", percentage: "wrong", splitType: "RANDOM_SPLIT"}]
          create_featureview_training_dataset_from_project(@project, expected_status_code: 400, splits: split, train_split: "train")
          expect_status(400)
          # error is unexpected token at 'Cannot deserialize value of type `java.lang.Float` from String "wrong"
          # parsed_json = all_metadata["response"]
          # expect(parsed_json.key?("errorCode")).to be true
          # expect(parsed_json.key?("errorMsg")).to be true
          # expect(parsed_json.key?("usrMsg")).to be true
          # expect(parsed_json["errorCode"]).to eql(270099)
        end

        it "should not be able to add a hopsfs training dataset to the featurestore with a illegal split name" do
          split = [{ name: "ILLEGALNAME!!!", percentage: 0.8 }]
          all_metadata = create_featureview_training_dataset_from_project(@project, expected_status_code: 400, splits: split)
          parsed_json = all_metadata["response"]
          expect(parsed_json.key?("errorCode")).to be true
          expect(parsed_json.key?("errorMsg")).to be true
          expect(parsed_json.key?("usrMsg")).to be true
          expect(parsed_json["errorCode"] == 270098).to be true
        end

        it "should not be able to add a hopsfs training dataset to the featurestore with splits of duplicate split
          names" do
          splits = [
            {
              name: "test_split",
              percentage: 0.8,
              splitType: "RANDOM_SPLIT"
            },
            {
              name: "test_split",
              percentage: 0.2,
              splitType: "RANDOM_SPLIT"
            }
          ]
          all_metadata = create_featureview_training_dataset_from_project(@project, expected_status_code: 400, splits: splits, train_split: "test_split")
          parsed_json = all_metadata["response"]
          expect(parsed_json.key?("errorCode")).to be true
          expect(parsed_json.key?("errorMsg")).to be true
          expect(parsed_json.key?("usrMsg")).to be true
          expect(parsed_json["errorCode"] == 270106).to be true
        end

        it "should not be able to create a training dataset with the same name and version" do
          all_metadata = create_featureview_training_dataset_from_project(@project)
          parsed_json = all_metadata["response"]
          featureview = all_metadata["featureView"]
          connector = all_metadata["connector"]

          create_featureview_training_dataset(@project.id, featureview, connector, version: parsed_json["version"])
          expect_status(400)
        end

        it "should be able to add a hopsfs training dataset to the featurestore without specifying a hopsfs connector" do
          featurestore_id = get_featurestore_id(@project.id)
          featuregroup_suffix = short_random_id
          query = make_sample_query(@project, featurestore_id, featuregroup_suffix: featuregroup_suffix)
          json_result = create_feature_view(@project.id, featurestore_id, query)
          expect_status(201)
          featureview = JSON.parse(json_result)
          td = create_featureview_training_dataset(@project.id, featureview, nil)
          parsed_json = JSON.parse(td)
          expect(parsed_json["storageConnector"]["name"] == "#{@project['projectname']}_Training_Datasets")
        end

        it "should be able to delete a hopsfs training dataset" do
          all_metadata = create_featureview_training_dataset_from_project(@project)
          parsed_json = all_metadata["response"]
          featureview = all_metadata["featureView"]

          delete_featureview_training_dataset(@project, featureview, version: parsed_json["version"])
        end

        it "should be able to delete all hopsfs training dataset" do
          all_metadata = create_featureview_training_dataset_from_project(@project)
          parsed_json = all_metadata["response"]
          featureview = all_metadata["featureView"]
          connector = all_metadata["connector"]
          create_featureview_training_dataset(@project.id, featureview, connector, version: nil)

          delete_featureview_training_dataset(@project, featureview)
        end

        it "should be able to delete a hopsfs training dataset (data only)" do
          all_metadata = create_featureview_training_dataset_from_project(@project)
          parsed_json = all_metadata["response"]
          featureview = all_metadata["featureView"]

          delete_featureview_training_dataset_data_only(@project, featureview, version: parsed_json["version"])
        end

        it "should not be able to delete a in-memory training dataset" do
          all_metadata = create_featureview_training_dataset_from_project(@project, td_type: "IN_MEMORY_TRAINING_DATASET")
          parsed_json = all_metadata["response"]
          featureview = all_metadata["featureView"]

          delete_featureview_training_dataset_data_only(@project, featureview, version: parsed_json["version"], expected_status: 400)
        end

        it "should be able to delete all hopsfs training dataset (data only)" do
          all_metadata = create_featureview_training_dataset_from_project(@project)
          featureview = all_metadata["featureView"]
          connector = all_metadata["connector"]
          create_featureview_training_dataset(@project.id, featureview, connector, version: nil)

          delete_featureview_training_dataset_data_only(@project, featureview)
        end

        it "should be able to delete all hopsfs training dataset (data only) except in-memory dataset" do
          all_metadata = create_featureview_training_dataset_from_project(@project, td_type: "IN_MEMORY_TRAINING_DATASET")
          featureview = all_metadata["featureView"]
          connector = all_metadata["connector"]
          create_featureview_training_dataset(@project.id, featureview, connector, version: nil, td_type: "IN_MEMORY_TRAINING_DATASET")

          delete_featureview_training_dataset_data_only(@project, featureview, expected_status: 400)
        end

        it "should not be able to update the metadata of a hopsfs training dataset from the featurestore" do
          all_metadata = create_featureview_training_dataset_from_project(@project)
          parsed_json = all_metadata["response"]
          featureview = all_metadata["featureView"]

          json_data = {
            name: "new_testtrainingdatasetname",
            dataFormat: "petastorm",
            type: "trainingDatasetDTO"
          }

          json_result2 = update_featureview_training_dataset_metadata(@project, featureview, parsed_json["version"], json_data)
          parsed_json2 = JSON.parse(json_result2)
          expect_status(200)
          expect(parsed_json2.key?("id")).to be true
          expect(parsed_json2.key?("name")).to be true
          expect(parsed_json2["creator"].key?("email")).to be true
          expect(parsed_json2.key?("location")).to be true
          expect(parsed_json2.key?("version")).to be true
          expect(parsed_json2.key?("dataFormat")).to be true
          expect(parsed_json2.key?("trainingDatasetType")).to be true
          expect(parsed_json2.key?("inodeId")).to be true

          expect(parsed_json2["version"]).to eql(parsed_json["version"])
          # make sure the dataformat didn't change
          expect(parsed_json2["dataFormat"] == "tfrecords").to be true
        end

		it "should not be able to update the name of a training dataset" do
          all_metadata = create_featureview_training_dataset_from_project(@project)
          parsed_json = all_metadata["response"]
          featureview = all_metadata["featureView"]

          json_data = {
            name: "new_testtrainingdatasetname",
            type: "trainingDatasetDTO"
          }

          json_result2 = update_featureview_training_dataset_metadata(@project, featureview, parsed_json["version"], json_data)
          parsed_json2 = JSON.parse(json_result2)
          expect_status(200)

          expect(parsed_json2["version"]).to eql(parsed_json["version"])
          # make sure the name didn't change
          expect(parsed_json2["name"]).to eql(parsed_json["name"])
        end

        it "should be able to update the description of a training dataset" do
          all_metadata = create_featureview_training_dataset_from_project(@project)
          parsed_json = all_metadata["response"]
          featureview = all_metadata["featureView"]

          json_data = {
            name: "new_testtrainingdatasetname",
            description: "new_testtrainingdatasetdescription",
            type: "trainingDatasetDTO"
          }

          json_result2 = update_featureview_training_dataset_metadata(@project, featureview, parsed_json["version"], json_data)
          parsed_json2 = JSON.parse(json_result2)
          expect_status(200)

          expect(parsed_json2["description"]).to eql("new_testtrainingdatasetdescription")
          expect(parsed_json2["version"]).to eql(parsed_json["version"])
          # make sure the name didn't change
          expect(parsed_json2["name"]).to eql(parsed_json["name"])
        end

        it "should be able to get a list of training dataset versions based on the version" do
          all_metadata = create_featureview_training_dataset_from_project(@project)
          parsed_json = all_metadata["response"]
          featureview = all_metadata["featureView"]
          connector = all_metadata["connector"]
          create_featureview_training_dataset(@project.id, featureview, connector, version: nil)

          json_result = get_featureview_training_dataset(@project, featureview)
          expect_status(200)
          parsed_json = JSON.parse(json_result)
          expect(parsed_json["count"]).to eq 2
        end

        it "should be able to get a training dataset based on version" do
          all_metadata = create_featureview_training_dataset_from_project(@project)
          parsed_json = all_metadata["response"]
          featureview = all_metadata["featureView"]
          connector = all_metadata["connector"]
          create_featureview_training_dataset(@project.id, featureview, connector, version: nil)

          json_result = get_featureview_training_dataset(@project, featureview, version: 1)
          parsed_json = JSON.parse(json_result)
          expect(parsed_json['version']).to be 1
          expect(parsed_json['name']).to eq "#{featureview['name']}_#{featureview['version']}"

          json_result = get_featureview_training_dataset(@project, featureview, version: 2)
          parsed_json = JSON.parse(json_result)
          expect(parsed_json['version']).to be 2
          expect(parsed_json['name']).to eq "#{featureview['name']}_#{featureview['version']}"
        end

        it "should be able to attach keywords" do
          featurestore_id = get_featurestore_id(@project.id)
          all_metadata = create_featureview_training_dataset_from_project(@project)
          parsed_json = all_metadata["response"]
          featureview = all_metadata["featureView"]

          post "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/featureview/#{featureview["name"]}/version/#{featureview["version"]}" +
            "/trainingdatasets/version/#{parsed_json["version"]}/keywords", {keywords: ['hello', 'this', 'keyword123', 'CAPITAL_LETTERS']}.to_json
          expect_status_details(200)

          json_result = get "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/featureview/#{featureview["name"]}/version/#{featureview["version"]}" +
            "/trainingdatasets/version/#{parsed_json["version"]}/keywords"
          expect_status_details(200)
          parsed_json = JSON.parse(json_result)
          expect(parsed_json['keywords']).to include('hello')
          expect(parsed_json['keywords']).to include('this')
          expect(parsed_json['keywords']).to include('keyword123')
          expect(parsed_json['keywords']).to include('CAPITAL_LETTERS')
        end

        it "should fail to attach invalid keywords" do
          featurestore_id = get_featurestore_id(@project.id)
          all_metadata = create_featureview_training_dataset_from_project(@project)
          parsed_json = all_metadata["response"]
          featureview = all_metadata["featureView"]

          post "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/featureview/#{featureview["name"]}/version/#{featureview["version"]}" +
            "/trainingdatasets/version/#{parsed_json["version"]}/keywords", {keywords: ['hello', 'this', '@#!@#^(&$']}
          expect_status(400)
        end

        it "should be able to remove keyword" do
          featurestore_id = get_featurestore_id(@project.id)
          all_metadata = create_featureview_training_dataset_from_project(@project)
          parsed_td = all_metadata["response"]
          featureview = all_metadata["featureView"]

          post "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/featureview/#{featureview["name"]}/version/#{featureview["version"]}" +
            "/trainingdatasets/version/#{parsed_td["version"]}/keywords", {keywords: ['hello', 'this', 'keyword123']}.to_json

          json_result = get "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/featureview/#{featureview["name"]}/version/#{featureview["version"]}" +
            "/trainingdatasets/version/#{parsed_td["version"]}/keywords"
          expect_status(200)
          parsed_json = JSON.parse(json_result)
          expect(parsed_json['keywords']).to include('hello')
          expect(parsed_json['keywords']).to include('this')
          expect(parsed_json['keywords']).to include('keyword123')

          delete "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/featureview/#{featureview["name"]}/version/#{featureview["version"]}" +
            "/trainingdatasets/version/#{parsed_td["version"]}/keywords?keyword=hello"
          expect_status(200)

          json_result = get "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/featureview/#{featureview["name"]}/version/#{featureview["version"]}" +
            "/trainingdatasets/version/#{parsed_td["version"]}/keywords"
          expect_status(200)
          parsed_json = JSON.parse(json_result)
          expect(parsed_json['keywords']).not_to include('hello')
          expect(parsed_json['keywords']).to include('this')
          expect(parsed_json['keywords']).to include('keyword123')
        end

        it "should be able to create a training dataset without statistics settings to test the defaults" do
          all_metadata = create_featureview_training_dataset_from_project(@project)
          parsed_json = all_metadata["response"]
          expect(parsed_json.key?("statisticsConfig")).to be true
          expect(parsed_json["statisticsConfig"].key?("histograms")).to be true
          expect(parsed_json["statisticsConfig"].key?("correlations")).to be true
          expect(parsed_json["statisticsConfig"].key?("exactUniqueness")).to be true
          expect(parsed_json["statisticsConfig"].key?("enabled")).to be true
          expect(parsed_json["statisticsConfig"].key?("columns")).to be true
          expect(parsed_json["statisticsConfig"]["columns"].length).to eql(0)
          expect(parsed_json["statisticsConfig"]["enabled"]).to be true
          expect(parsed_json["statisticsConfig"]["correlations"]).to be false
          expect(parsed_json["statisticsConfig"]["exactUniqueness"]).to be false
          expect(parsed_json["statisticsConfig"]["histograms"]).to be false
        end

        it "should be able to create a training dataset with statistics settings and retrieve them back" do
          stats_config = { enabled: false, histograms: false, correlations: false, exactUniqueness: false, columns:
            ["a_testfeature"] }
          all_metadata = create_featureview_training_dataset_from_project(@project, statistics_config: stats_config)
          parsed_json = all_metadata["response"]
          expect(parsed_json["statisticsConfig"]["columns"].length).to eql(1)
          expect(parsed_json["statisticsConfig"]["columns"][0]).to eql("a_testfeature")
          expect(parsed_json["statisticsConfig"]["enabled"]).to be false
          expect(parsed_json["statisticsConfig"]["correlations"]).to be false
          expect(parsed_json["statisticsConfig"]["exactUniqueness"]).to be false
          expect(parsed_json["statisticsConfig"]["histograms"]).to be false
        end

        it "should not be possible to add a training dataset with non-existing statistic column" do
          stats_config = { enabled: false, histograms: false, correlations: false, exactUniqueness: false, columns: ["wrongname"] }
          all_metadata = create_featureview_training_dataset_from_project(
            @project, statistics_config: stats_config, expected_status_code: 400)
          parsed_json = all_metadata["response"]
          expect(parsed_json.key?("errorCode")).to be true
          expect(parsed_json.key?("errorMsg")).to be true
          expect(parsed_json.key?("usrMsg")).to be true
          expect(parsed_json["errorCode"]).to eql(270108)
        end

        it "should be able to update the statistics config of a training dataset" do
          all_metadata = create_featureview_training_dataset_from_project(@project)
          parsed_json = all_metadata["response"]
          featureview = all_metadata["featureView"]

          json_data = {
            statisticsConfig: {
              histograms: false,
              correlations: false,
              exactUniqueness: false,
              columns: ["a_testfeature"],
              enabled: false
            },
            type: "trainingDatasetDTO"
          }

          json_result2 = update_featureview_training_dataset_stats_config(@project, featureview, parsed_json["version"], json_data)
          parsed_json2 = JSON.parse(json_result2)
          expect_status(200)
          expect(parsed_json2["statisticsConfig"]["columns"].length).to eql(1)
          expect(parsed_json2["statisticsConfig"]["columns"][0]).to eql("a_testfeature")
          expect(parsed_json2["statisticsConfig"]["enabled"]).to be false
          expect(parsed_json2["statisticsConfig"]["correlations"]).to be false
          expect(parsed_json2["statisticsConfig"]["exactUniqueness"]).to be false
          expect(parsed_json2["statisticsConfig"]["histograms"]).to be false
        end

        it "should be able to add extra filter to td" do
          featurestore_id = get_featurestore_id(@project.id)
          featuregroup_suffix = short_random_id
          query = make_sample_query(@project, featurestore_id, featuregroup_suffix: featuregroup_suffix)
          fv_json, _ = create_feature_view(@project.id, featurestore_id, query)
          fv = JSON.parse(fv_json)
          extra_filter = {
            type: "SINGLE",
            leftFilter: {
              feature: {
                name: "a_testfeature1",
                featureGroupId: query[:leftFeatureGroup][:id]
              },
              condition: "LESS_THAN",
              value: "100"
            }
          }
          td_result = create_featureview_training_dataset(@project.id, fv, nil, extra_filter: extra_filter)
          td = JSON.parse(td_result)
          query_result = get "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}" +
                               "/featureview/#{fv['name']}/version/#{fv['version']}/query/batch" +
                               "?td_version=#{td['version']}"
          expect_status_details(200)
          parsed_query_result = JSON.parse(query_result)
          expect(parsed_query_result["featureStoreId"]).to eql(featurestore_id)
          expect(parsed_query_result["leftFeatureGroup"]["id"]).to eql(query[:leftFeatureGroup][:id])
          expect(parsed_query_result["leftFeatures"][0]["name"]).to eql(query[:leftFeatures][0][:name])
          expect(parsed_query_result["leftFeatures"][1]["name"]).to eql(query[:leftFeatures][1][:name])
          expect(parsed_query_result["joins"][0]["query"]["leftFeatureGroup"]["id"]).to eql(query[:joins][0][:query][:leftFeatureGroup][:id])
          expect(parsed_query_result["joins"][0]["query"]["leftFeatures"][0]["name"]).to eql(query[:joins][0][:query][:leftFeatures][1][:name])
          expect(parsed_query_result["joins"][0]["query"]["leftFeatures"][0]["name"]).to eql(query[:joins][0][:query][:leftFeatures][1][:name])
          expect(parsed_query_result["filter"]["type"]).to eql("AND")
          expect(parsed_query_result["filter"]["leftLogic"]["type"]).to eql(query[:filter][:type])
          expect(parsed_query_result["filter"]["leftLogic"]["leftFilter"]["feature"]["name"]).to eql(query[:filter][:leftFilter][:feature][:name])
          expect(parsed_query_result["filter"]["leftLogic"]["leftFilter"]["condition"]).to eql(query[:filter][:leftFilter][:condition])
          expect(parsed_query_result["filter"]["leftLogic"]["leftFilter"]["value"]).to eql(query[:filter][:leftFilter][:value])
          expect(parsed_query_result["filter"]["rightLogic"]["type"]).to eql(extra_filter[:type])
          expect(parsed_query_result["filter"]["rightLogic"]["leftFilter"]["feature"]["name"]).to eql(extra_filter[:leftFilter][:feature][:name])
          expect(parsed_query_result["filter"]["rightLogic"]["leftFilter"]["condition"]).to eql(extra_filter[:leftFilter][:condition])
          expect(parsed_query_result["filter"]["rightLogic"]["leftFilter"]["value"]).to eql(extra_filter[:leftFilter][:value])
        end

        it "should be able to add extra filter logic to td" do
          featurestore_id = get_featurestore_id(@project.id)
          featuregroup_suffix = short_random_id
          query = make_sample_query(@project, featurestore_id, featuregroup_suffix: featuregroup_suffix)
          fv_json, _ = create_feature_view(@project.id, featurestore_id, query)
          fv = JSON.parse(fv_json)
          extra_filter = {
            type: "OR",
            leftLogic: {
              type: "SINGLE",
              leftFilter: {
                feature: {
                  name: "a_testfeature1",
                  featureGroupId: query[:leftFeatureGroup][:id]
                },
                condition: "LESS_THAN",
                value: "10"
              }
            },
            rightLogic: {
              type: "SINGLE",
              leftFilter: {
                feature: {
                  name: "b_testfeature1",
                  featureGroupId: query[:joins][0][:query][:leftFeatureGroup][:id]
                },
                condition: "LESS_THAN",
                value: "10"
              }
            }
          }
          td_result = create_featureview_training_dataset(@project.id, fv, nil, extra_filter: extra_filter)
          td = JSON.parse(td_result)
          query_result = get "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}" +
                               "/featureview/#{fv['name']}/version/#{fv['version']}/query/batch" +
                               "?td_version=#{td['version']}"
          expect_status_details(200)
          parsed_query_result = JSON.parse(query_result)
          expect(parsed_query_result["featureStoreId"]).to eql(featurestore_id)
          expect(parsed_query_result["leftFeatureGroup"]["id"]).to eql(query[:leftFeatureGroup][:id])
          expect(parsed_query_result["leftFeatures"][0]["name"]).to eql(query[:leftFeatures][0][:name])
          expect(parsed_query_result["leftFeatures"][1]["name"]).to eql(query[:leftFeatures][1][:name])
          expect(parsed_query_result["joins"][0]["query"]["leftFeatureGroup"]["id"]).to eql(query[:joins][0][:query][:leftFeatureGroup][:id])
          expect(parsed_query_result["joins"][0]["query"]["leftFeatures"][0]["name"]).to eql(query[:joins][0][:query][:leftFeatures][1][:name])
          expect(parsed_query_result["joins"][0]["query"]["leftFeatures"][0]["name"]).to eql(query[:joins][0][:query][:leftFeatures][1][:name])
          expect(parsed_query_result["filter"]["type"]).to eql("AND")
          expect(parsed_query_result["filter"]["leftLogic"]["type"]).to eql(query[:filter][:type])
          expect(parsed_query_result["filter"]["leftLogic"]["leftFilter"]["feature"]["name"]).to eql(query[:filter][:leftFilter][:feature][:name])
          expect(parsed_query_result["filter"]["leftLogic"]["leftFilter"]["condition"]).to eql(query[:filter][:leftFilter][:condition])
          expect(parsed_query_result["filter"]["leftLogic"]["leftFilter"]["value"]).to eql(query[:filter][:leftFilter][:value])
          expect(parsed_query_result["filter"]["rightLogic"]["type"]).to eql(extra_filter[:type])
          expect(parsed_query_result["filter"]["rightLogic"]["leftFilter"]["feature"]["name"]).to eql(extra_filter[:leftLogic][:leftFilter][:feature][:name])
          expect(parsed_query_result["filter"]["rightLogic"]["leftFilter"]["condition"]).to eql(extra_filter[:leftLogic][:leftFilter][:condition])
          expect(parsed_query_result["filter"]["rightLogic"]["leftFilter"]["value"]).to eql(extra_filter[:leftLogic][:leftFilter][:value])
          expect(parsed_query_result["filter"]["rightLogic"]["rightFilter"]["feature"]["name"]).to eql(extra_filter[:rightLogic][:leftFilter][:feature][:name])
          expect(parsed_query_result["filter"]["rightLogic"]["rightFilter"]["condition"]).to eql(extra_filter[:rightLogic][:leftFilter][:condition])
          expect(parsed_query_result["filter"]["rightLogic"]["rightFilter"]["value"]).to eql(extra_filter[:rightLogic][:leftFilter][:value])
        end

        it "should not be able to add extra filter to td where required field is not available" do
          featurestore_id = get_featurestore_id(@project.id)
          featuregroup_suffix = short_random_id
          query = make_sample_query(@project, featurestore_id, featuregroup_suffix: featuregroup_suffix)
          fv_json, _ = create_feature_view(@project.id, featurestore_id, query)
          fv = JSON.parse(fv_json)
          features_a = [
            { type: "INT", name: "c_testfeature", primary: true },
            { type: "INT", name: "c_testfeature1" },
            { type: "BIGINT", name: "ts" },
          ]
          fg_id = create_cached_featuregroup_checked(@project.id, featurestore_id, "test_fg_c#{featuregroup_suffix}",
                                                     features: features_a,
                                                     event_time: "ts")
          extra_filter = {
            type: "SINGLE",
            leftFilter: {
              feature: {
                name: "c_testfeature1",
                featureGroupId: fg_id
              },
              condition: "LESS_THAN",
              value: "100"
            }
          }
          create_featureview_training_dataset(@project.id, fv, nil, extra_filter: extra_filter)
          expect_status_details(404)
        end
      end
    end

    describe "external" do
      context 'with valid project, s3 connector, and featurestore service enabled' do
        before :all do
          with_valid_project
          with_s3_connector(@project[:id])
        end

        it "should be able to add an external training dataset to the featurestore" do
          connector = make_connector_dto(get_s3_connector_id)
          all_metadata = create_featureview_training_dataset_from_project(@project, connector: connector, is_internal: false)
          featureview = all_metadata["featureView"]
          parsed_json = all_metadata["response"]
          expect(parsed_json.key?("id")).to be true
          expect(parsed_json.key?("featurestoreName")).to be true
          expect(parsed_json.key?("name")).to be true
          expect(parsed_json["creator"].key?("email")).to be true
          expect(parsed_json.key?("location")).to be true
          expect(parsed_json.key?("version")).to be true
          expect(parsed_json.key?("dataFormat")).to be true
          expect(parsed_json.key?("trainingDatasetType")).to be true
          expect(parsed_json.key?("description")).to be true
          expect(parsed_json.key?("seed")).to be true
          expect(parsed_json["featurestoreName"] == @project.projectname.downcase + "_featurestore").to be true
          expect(parsed_json["name"] == "#{featureview['name']}_#{featureview['version']}").to be true
          expect(parsed_json["trainingDatasetType"] == "EXTERNAL_TRAINING_DATASET").to be true
          expect(parsed_json["storageConnector"]["id"] == connector[:id]).to be true
          expect(parsed_json["seed"] == 1234).to be true
        end

        it "should not be able to add an external training dataset to the featurestore without specifying a s3 connector" do
          featurestore_id = get_featurestore_id(@project.id)
          featuregroup_suffix = short_random_id
          query = make_sample_query(@project, featurestore_id, featuregroup_suffix: featuregroup_suffix)
          json_result = create_feature_view(@project.id, featurestore_id, query)
          expect_status(201)
          featureview = JSON.parse(json_result)
          create_featureview_training_dataset(@project.id, featureview, nil, is_internal: false)
          expect_status(404)
        end

        it "should be able to add an external training dataset to the featurestore with splits" do
          splits = [
            {
              name: "test_split",
              percentage: 0.8
            },
            {
              name: "train_split",
              percentage: 0.2
            }
          ]
          connector = make_connector_dto(get_s3_connector_id)
          all_metadata = create_featureview_training_dataset_from_project(
            @project, connector: connector, is_internal: false, splits: splits, train_split: "train_split")
          parsed_json = all_metadata["response"]
          expect(parsed_json.key?("splits")).to be true
          expect(parsed_json["splits"].length).to be 2
        end

        it "should be able to add a external training dataset to the featurestore with time series splits" do
          splits = [
            {
              name: "train",
              startTime: 1000,
              endTime: 2000,
              splitType: "TIME_SERIES_SPLIT"
            },
            {
              name: "test",
              startTime: 5000,
              endTime: 6000,
              splitType: "TIME_SERIES_SPLIT"
            },
            {
              name: "validation",
              startTime: 3000,
              endTime: 4000,
              splitType: "TIME_SERIES_SPLIT"
            }
          ]
          connector = make_connector_dto(get_s3_connector_id)
          all_metadata = create_featureview_training_dataset_from_project(
            @project, connector: connector, is_internal: false, splits: splits, train_split: "train")
          parsed_json = all_metadata["response"]
          expect(parsed_json.key?("splits")).to be true
          expect(parsed_json["splits"].length).to be 3
          sorted_splits = parsed_json["splits"].sort_by {|split| split["startTime"]}
          expect(sorted_splits[0]["splitType"]).to eql "TIME_SERIES_SPLIT"
          expect(sorted_splits[0]["name"]).to eql "train"
          expect(sorted_splits[0]["startTime"]).to eql 1000
          expect(sorted_splits[0]["endTime"]).to eql 2000
          expect(sorted_splits[1]["splitType"]).to eql "TIME_SERIES_SPLIT"
          expect(sorted_splits[1]["name"]).to eql "validation"
          expect(sorted_splits[1]["startTime"]).to eql 3000
          expect(sorted_splits[1]["endTime"]).to eql 4000
          expect(sorted_splits[2]["splitType"]).to eql "TIME_SERIES_SPLIT"
          expect(sorted_splits[2]["name"]).to eql "test"
          expect(sorted_splits[2]["startTime"]).to eql 5000
          expect(sorted_splits[2]["endTime"]).to eql 6000
        end

        it "should not be able to add a hopsfs training dataset to the featurestore with incorrect time series splits" do
          splits = [
            {
              name: "train",
              startTime: 2000,
              endTime: 1000,
              splitType: "TIME_SERIES_SPLIT"
            },
            {
              name: "test",
              startTime: 2000,
              endTime: 1000,
              splitType: "TIME_SERIES_SPLIT"
            },
            {
              name: "validation",
              startTime: 20000,
              endTime: 10000,
              splitType: "TIME_SERIES_SPLIT"
            }
          ]
          connector = make_connector_dto(get_s3_connector_id)
          all_metadata = create_featureview_training_dataset_from_project(
            @project, connector: connector, is_internal: false, splits: splits, train_split: "train",
            expected_status_code: 400)
        end

        it "should not be able to add an external training dataset to the featurestore with a non numeric split percentage" do
          splits = [{ name: "train_split", percentage: "wrong", splitType: "RANDOM_SPLIT"}]
          connector = make_connector_dto(get_s3_connector_id)
          create_featureview_training_dataset_from_project(
            @project, connector: connector, is_internal: false, splits: splits, expected_status_code: 400, train_split: "train")
          expect_status(400)
          # error is unexpected token at 'Cannot deserialize value of type `java.lang.Float` from String "wrong"
          # expect(parsed_json.key?("errorCode")).to be true
          # expect(parsed_json.key?("errorMsg")).to be true
          # expect(parsed_json.key?("usrMsg")).to be true
          # expect(parsed_json["errorCode"]).to eql(270099)
        end

        it "should not be able to add an external training dataset to the featurestore with splits of duplicate split names" do
          splits = [
            {
              name: "test_split",
              percentage: 0.8,
              splitType: "RANDOM_SPLIT"
            },
            {
              name: "test_split",
              percentage: 0.2,
              splitType: "RANDOM_SPLIT"
            }
          ]
          connector = make_connector_dto(get_s3_connector_id)
          all_metadata = create_featureview_training_dataset_from_project(
            @project, connector: connector, is_internal: false, splits: splits, train_split: "test_split", expected_status_code: 400)
          parsed_json = all_metadata["response"]

          expect(parsed_json.key?("errorCode")).to be true
          expect(parsed_json.key?("errorMsg")).to be true
          expect(parsed_json.key?("usrMsg")).to be true
          expect(parsed_json["errorCode"] == 270106).to be true
        end

        it "should be able to delete a training dataset" do
          connector = make_connector_dto(get_s3_connector_id)
          all_metadata = create_featureview_training_dataset_from_project(@project, connector: connector, is_internal: false)
          parsed_json = all_metadata["response"]
          featureview = all_metadata["featureView"]

          delete_featureview_training_dataset(@project, featureview, version: parsed_json["version"])
        end

        it "should be able to delete all training dataset" do
          connector = make_connector_dto(get_s3_connector_id)
          all_metadata = create_featureview_training_dataset_from_project(@project, connector: connector, is_internal: false)
          featureview = all_metadata["featureView"]
          connector = all_metadata["connector"]
          create_featureview_training_dataset(@project.id, featureview, connector, version: nil)

          delete_featureview_training_dataset(@project, featureview)
        end

        it "should not be able to delete a training dataset (data only)" do
          connector = make_connector_dto(get_s3_connector_id)
          all_metadata = create_featureview_training_dataset_from_project(@project, connector: connector, is_internal: false)
          parsed_json = all_metadata["response"]
          featureview = all_metadata["featureView"]

          delete_featureview_training_dataset_data_only(@project, featureview, version: parsed_json["version"], expected_status: 400)
        end

        it "should not be able to delete all training dataset (data only)" do
          connector = make_connector_dto(get_s3_connector_id)
          all_metadata = create_featureview_training_dataset_from_project(@project, connector: connector, is_internal: false)
          featureview = all_metadata["featureView"]
          connector = all_metadata["connector"]
          create_featureview_training_dataset(@project.id, featureview, connector, version: nil)

          delete_featureview_training_dataset_data_only(@project, featureview, expected_status: 400)
        end

        it "should be able to update the metadata (description) of an external training dataset from the featurestore" do
          connector = make_connector_dto(get_s3_connector_id)
          all_metadata = create_featureview_training_dataset_from_project(@project, connector: connector, is_internal: false)
          parsed_json = all_metadata["response"]
          featureview = all_metadata["featureView"]

          json_data = {
            name: "new_testtrainingdatasetname",
            description: "new_testtrainingdatasetdescription",
            type: "trainingDatasetDTO"
          }

          json_result2 = update_featureview_training_dataset_metadata(@project, featureview, parsed_json["version"], json_data)
          parsed_json2 = JSON.parse(json_result2)
          expect_status(200)
          expect(parsed_json2.key?("id")).to be true
          expect(parsed_json2.key?("featurestoreName")).to be true
          expect(parsed_json2.key?("name")).to be true
          expect(parsed_json2["creator"].key?("email")).to be true
          expect(parsed_json2.key?("location")).to be true
          expect(parsed_json2.key?("version")).to be true
          expect(parsed_json2.key?("dataFormat")).to be true
          expect(parsed_json2.key?("trainingDatasetType")).to be true
          expect(parsed_json2.key?("description")).to be true
          expect(parsed_json2["featurestoreName"] == @project.projectname.downcase + "_featurestore").to be true
          expect(parsed_json2["description"] == "new_testtrainingdatasetdescription").to be true
          expect(parsed_json2["trainingDatasetType"] == "EXTERNAL_TRAINING_DATASET").to be true
          expect(parsed_json2["version"]).to eql(parsed_json["version"])
        end

        it "should not be able do change the storage connector" do
          connector_id = get_s3_connector_id
          connector = make_connector_dto(connector_id)
          all_metadata = create_featureview_training_dataset_from_project(@project, connector: connector, is_internal: false)
          parsed_json = all_metadata["response"]
          featureview = all_metadata["featureView"]

          json_new_connector, _ = create_s3_connector(@project[:id], featureview["featurestoreId"], access_key: "test", secret_key: "test")
          new_connector = JSON.parse(json_new_connector)

          json_data = {
            name: "new_testtrainingdatasetname",
            storageConnector: {
              id: new_connector['id']
            },
            type: "trainingDatasetDTO"
          }

          json_result2 = update_featureview_training_dataset_metadata(@project, featureview, parsed_json["version"], json_data)
          parsed_json2 = JSON.parse(json_result2)
          expect_status(200)

          expect(parsed_json2["version"]).to eql(parsed_json["version"])
          # make sure the name didn't change
          expect(parsed_json2["storageConnector"]["id"]).to be connector_id
        end

        it "should store and return the correct path within the bucket" do
          connector = make_connector_dto(get_s3_connector_id)
          all_metadata = create_featureview_training_dataset_from_project(@project, connector: connector, location: "/inner/location", is_internal: false)
          parsed_json = all_metadata["response"]
          featureview = all_metadata["featureView"]
          expect(parsed_json['location']).to eql("s3://testbucket/inner/location/#{featureview['name']}_#{featureview['version']}_1")
        end

        it "should be able to create a training dataset using ADLS connector" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector = create_adls_connector(project.id, featurestore_id)
          connector = { "id": JSON.parse(connector)['id'] }
          all_metadata = create_featureview_training_dataset_from_project(@project, connector: connector, is_internal: false, location: "/inner/location/")
          parsed_json = all_metadata["response"]
          featureview = all_metadata["featureView"]
          expect(parsed_json['location']).to eql("abfss://containerName@accountName.dfs.core.windows.net/inner/location/#{featureview['name']}_#{featureview['version']}_1")
        end

        it "should not be able to create a training dataset using a SNOWFLAKE connector" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector = create_snowflake_connector(project.id, featurestore_id)
          connector = JSON.parse(connector)
          create_featureview_training_dataset_from_project(@project, connector: connector, is_internal: false, expected_status_code: 404)
        end

        it "should not be able to create a training dataset using a REDSHIFT connector" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector, _ = create_redshift_connector(project.id, featurestore_id, databasePassword: "pwdf")
          connector = JSON.parse(connector)
          create_featureview_training_dataset_from_project(@project, connector: connector, is_internal: false, expected_status_code: 404)
        end

        it "should not be able to create a training dataset using a JDBC connector" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector, _ = create_jdbc_connector(project.id, featurestore_id)
          connector = JSON.parse(connector)
          create_featureview_training_dataset_from_project(@project, connector: connector, is_internal: false, expected_status_code: 404)
        end
      end
    end
  end
end