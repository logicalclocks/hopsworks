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
  after(:all) {clean_all_test_projects(spec: "trainingdataset")}

  describe "training dataset" do
    describe "internal" do
      context 'with valid project, featurestore service enabled' do
        before :all do
          with_valid_project
        end

        it "should be able to add a hopsfs training dataset to the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])

          features = [
              {type: "int", name: "testfeature"},
              {type: "int", name: "testfeature1"}
          ]
          json_result, training_dataset_name = create_hopsfs_training_dataset(project.id, featurestore_id, connector, features: features)
          expect_status(201)
          parsed_json = JSON.parse(json_result)

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
          expect(parsed_json.key?("features")).to be true
          expect(parsed_json.key?("seed")).to be true
          expect(parsed_json["featurestoreName"] == project.projectname.downcase + "_featurestore").to be true
          expect(parsed_json["name"] == training_dataset_name).to be true
          expect(parsed_json["trainingDatasetType"] == "HOPSFS_TRAINING_DATASET").to be true
          expect(parsed_json["storageConnector"]["id"] == connector.id).to be true
          expect(parsed_json["seed"] == 1234).to be true
          expect(parsed_json["fromQuery"]).to be false

          td_features = parsed_json['features']
          expect(td_features.length).to be 2

          expect(td_features.select{|feature| feature['index'] == 0}[0]['name']).to eql("testfeature")
          expect(td_features.select{|feature| feature['index'] == 0}[0]['featuregroup']).to be nil
          expect(td_features.select{|feature| feature['index'] == 0}[0]['type']).to eql("int")
          expect(td_features.select{|feature| feature['index'] == 0}[0]['label']).to be false

          expect(td_features.select{|feature| feature['index'] == 1}[0]['name']).to eql("testfeature1")
          expect(td_features.select{|feature| feature['index'] == 1}[0]['featuregroup']).to be nil
          expect(td_features.select{|feature| feature['index'] == 1}[0]['type']).to eql("int")
          expect(td_features.select{|feature| feature['index'] == 1}[0]['label']).to be false

          # Make sure the location contains the scheme (hopsfs) and the authority
          uri = URI(parsed_json["location"])
          expect(uri.scheme).to eql("hopsfs")
          # If the port is available we can assume that the IP is as well.
          expect(uri.port).to eql(8020)
        end

        it "should not be able to add a hopsfs training dataset to the featurestore with upper case characters" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
          training_dataset_name = "TEST_training_dataset"
          json_result, training_dataset_name = create_hopsfs_training_dataset(project.id, featurestore_id, connector, name:training_dataset_name)
          parsed_json = JSON.parse(json_result)
          expect_status(400)
          expect(parsed_json.key?("errorCode")).to be true
          expect(parsed_json.key?("errorMsg")).to be true
          expect(parsed_json.key?("usrMsg")).to be true
          expect(parsed_json["errorCode"] == 270091).to be true
        end

        it "should not be able to add a hopsfs training dataset to the featurestore without specifying a data format" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
          json_result, training_dataset_name = create_hopsfs_training_dataset(project.id, featurestore_id, connector, data_format: "")
          parsed_json = JSON.parse(json_result)
          expect_status(400)
          expect(parsed_json.key?("errorCode")).to be true
          expect(parsed_json.key?("errorMsg")).to be true
          expect(parsed_json.key?("usrMsg")).to be true
          expect(parsed_json["errorCode"] == 270057).to be true
        end

        it "should not be able to add a hopsfs training dataset to the featurestore with an invalid version" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
          json_result, training_dataset_name = create_hopsfs_training_dataset(project.id, featurestore_id, connector,
                                                                              version: -1)
          parsed_json = JSON.parse(json_result)
          expect_status(400)
          expect(parsed_json.key?("errorCode")).to be true
          expect(parsed_json.key?("errorMsg")).to be true
          expect(parsed_json.key?("usrMsg")).to be true
          expect(parsed_json["errorCode"] == 270058).to be true
        end

        it "should be able to add a new hopsfs training dataset without version to the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
          json_result, training_dataset_name = create_hopsfs_training_dataset(project.id, featurestore_id, connector,
                                                                              name: "no_version_td", version: nil)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          expect(parsed_json["version"] == 1).to be true
        end

        it "should be able to add a new version of an existing hopsfs training dataset without version to the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
          json_result, training_dataset_name = create_hopsfs_training_dataset(project.id, featurestore_id, connector,
                                                                              name: "no_version_td_add")
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          # add second version
          json_result, training_dataset_name = create_hopsfs_training_dataset(project.id, featurestore_id, connector,
                                                                              name: "no_version_td_add", version: nil)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          # version should be incremented to 2
          expect(parsed_json["version"] == 2).to be true
        end

        it "should be able to add a hopsfs training dataset to the featurestore with splits" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
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
          json_result, training_dataset_name = create_hopsfs_training_dataset(project.id, featurestore_id, connector,
                                                                              splits: splits, train_split:
                                                                                "train_split")
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          expect(parsed_json.key?("splits")).to be true
          expect(parsed_json["splits"].length).to be 2
        end

        it "should not be able to add a hopsfs training dataset to the featurestore with a non numeric split percentage" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
          split = [{name: "train", percentage: "wrong", splitType: "RANDOM_SPLIT"}]
          json_result, training_dataset_name = create_hopsfs_training_dataset(project.id, featurestore_id, connector, splits: split, train_split: "train")
          parsed_json = JSON.parse(json_result)
          expect_status(400)
          expect(parsed_json.key?("errorCode")).to be true
          expect(parsed_json.key?("errorMsg")).to be true
          expect(parsed_json.key?("usrMsg")).to be true
          expect(parsed_json["errorCode"]).to eql(270099)
        end

        it "should not be able to add a hopsfs training dataset to the featurestore with a illegal split name" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
          split = [{name: "ILLEGALNAME!!!", percentage: 0.8}]
          json_result, training_dataset_name = create_hopsfs_training_dataset(project.id, featurestore_id, connector, splits: split)
          parsed_json = JSON.parse(json_result)
          expect_status(400)
          expect(parsed_json.key?("errorCode")).to be true
          expect(parsed_json.key?("errorMsg")).to be true
          expect(parsed_json.key?("usrMsg")).to be true
          expect(parsed_json["errorCode"] == 270098).to be true
        end

        it "should not be able to add a hopsfs training dataset to the featurestore with splits of duplicate split
          names" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
          splits = [
              {
                  name: "test_split",
                  percentage: 0.8
              },
              {
                  name: "test_split",
                  percentage: 0.2
              }
          ]
          json_result, training_dataset_name = create_hopsfs_training_dataset(project.id, featurestore_id, connector,
                                                                              splits: splits, train_split: "test_split")
          parsed_json = JSON.parse(json_result)
          expect_status(400)
          expect(parsed_json.key?("errorCode")).to be true
          expect(parsed_json.key?("errorMsg")).to be true
          expect(parsed_json.key?("usrMsg")).to be true
          expect(parsed_json["errorCode"] == 270106).to be true
        end

        it "should not be able to create a training dataset with the same name and version" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
          json_result, training_dataset_name = create_hopsfs_training_dataset(project.id, featurestore_id, connector)
          parsed_json = JSON.parse(json_result)
          expect_status(201)

          create_hopsfs_training_dataset(project.id, featurestore_id, connector, name: training_dataset_name)
          expect_status(400)
        end

        it "should be able to add a hopsfs training dataset to the featurestore without specifying a hopsfs connector" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, _ = create_hopsfs_training_dataset(project.id, featurestore_id, nil)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          expect(parsed_json["storageConnector"]["name"] == "#{project['projectname']}_Training_Datasets")
        end

        it "should be able to add a new hopsfs training dataset with a single feature label to the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
          features = [
              {
                  type: "INT",
                  name: "testfeature",
                  label: true
              },
              {
                  type: "INT",
                  name: "testfeature2",
                  label: false
              }
          ]
          json_result, training_dataset_name = create_hopsfs_training_dataset(project.id, featurestore_id, connector,
                                                                              name: "no_version_td", version: nil,
                                                                              features: features)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          td_features = parsed_json['features']
          expect(td_features.length).to be 2
          expect(td_features.select{|feature| feature['index'] == 0}[0]['label']).to be true
          expect(td_features.select{|feature| feature['index'] == 1}[0]['label']).to be false
        end

        it "should be able to add a new hopsfs training dataset with a multi feature label to the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
          features = [
              {
                  type: "INT",
                  name: "testfeature",
                  label: true
              },
              {
                  type: "INT",
                  name: "testfeature2",
                  label: true
              }
          ]
          json_result, training_dataset_name = create_hopsfs_training_dataset(project.id, featurestore_id, connector,
                                                                              name: "no_version_td", version: nil,
                                                                              features: features)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          td_features = parsed_json['features']
          expect(td_features.length).to be 2
          expect(td_features.select{|feature| feature['index'] == 0}[0]['label']).to be true
          expect(td_features.select{|feature| feature['index'] == 1}[0]['label']).to be true
        end

        it "should be able to delete a hopsfs training dataset from the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
          json_result1, _ = create_hopsfs_training_dataset(project.id, featurestore_id, connector)
          parsed_json1 = JSON.parse(json_result1)
          expect_status(201)
          training_dataset_id = parsed_json1["id"]
          delete_training_dataset_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s +
              "/featurestores/" + featurestore_id.to_s + "/trainingdatasets/" + training_dataset_id.to_s
          json_result2 = delete delete_training_dataset_endpoint
          expect_status(200)

          # Make sure that the directory has been removed correctly
          get_datasets_in_path(project,
                               "#{project[:projectname]}_Training_Datasets/#{parsed_json1['name']}_#{parsed_json1['version']}",
                               query: "&type=DATASET")
          expect_status(400)
        end

        it "should not be able to update the metadata of a hopsfs training dataset from the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
          json_result1, training_dataset_name = create_hopsfs_training_dataset(project.id, featurestore_id, connector)
          parsed_json1 = JSON.parse(json_result1)
          expect_status(201)
          training_dataset_id = parsed_json1["id"]
          json_result2 = update_hopsfs_training_dataset_metadata(project.id, featurestore_id, training_dataset_id, "petastorm", connector)
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

          # make sure the dataformat didn't change
          expect(parsed_json2["dataFormat"] == "tfrecords").to be true
        end

        it "should not be able to update the name of a training dataset" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
          json_result1, training_dataset_name = create_hopsfs_training_dataset(project.id, featurestore_id, connector)
          parsed_json1 = JSON.parse(json_result1)
          expect_status(201)

          training_dataset_id = parsed_json1["id"]
          json_result2 = update_hopsfs_training_dataset_metadata(project.id, featurestore_id,
                                                                 training_dataset_id, "tfrecords", connector)
          parsed_json2 = JSON.parse(json_result2)
          expect_status(200)

          # make sure the name didn't change
          expect(parsed_json2["name"]).to eql(training_dataset_name)
        end

        it "should be able to update the description of a training dataset" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
          json_result1, training_dataset_name = create_hopsfs_training_dataset(project.id, featurestore_id, connector)
          parsed_json1 = JSON.parse(json_result1)
          expect_status(201)

          training_dataset_id = parsed_json1["id"]
          json_result2 = update_hopsfs_training_dataset_metadata(project.id, featurestore_id,
                                                                 training_dataset_id, "tfrecords", connector)
          parsed_json2 = JSON.parse(json_result2)
          expect_status(200)

          expect(parsed_json2["description"]).to eql("new_testtrainingdatasetdescription")
          # make sure the name didn't change
          expect(parsed_json2["name"]).to eql(training_dataset_name)
        end

        it "should be able to get a list of training dataset versions based on the name" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
          json_result, training_dataset_name = create_hopsfs_training_dataset(project.id, featurestore_id, connector)
          expect_status(201)

          json_result, training_dataset_name = create_hopsfs_training_dataset(project.id, featurestore_id, connector, name: training_dataset_name, version: 2)
          expect_status(201)

          # Get the list
          get_training_datasets_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project.id}/featurestores/#{featurestore_id}/trainingdatasets/#{training_dataset_name}"
          get get_training_datasets_endpoint
          parsed_json = JSON.parse(response.body)
          expect_status(200)
          expect(parsed_json.size).to eq 2
        end

        it "should be able to get a training dataset based on name and version" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
          json_result, training_dataset_name = create_hopsfs_training_dataset(project.id, featurestore_id, connector)
          expect_status(201)

          json_result, training_dataset_name = create_hopsfs_training_dataset(project.id, featurestore_id, connector, name: training_dataset_name, version: 2)
          expect_status(201)

          # Get the first version
          get_training_datasets_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project.id}/featurestores/#{featurestore_id}/trainingdatasets/#{training_dataset_name}?version=1"
          get get_training_datasets_endpoint
          parsed_json = JSON.parse(response.body)
          expect_status(200)
          expect(parsed_json[0]['version']).to be 1
          expect(parsed_json[0]['name']).to eq training_dataset_name

          get_training_datasets_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project.id}/featurestores/#{featurestore_id}/trainingdatasets/#{training_dataset_name}?version=2"
          get get_training_datasets_endpoint
          expect_status(200)
          parsed_json = JSON.parse(response.body)
          expect(parsed_json[0]['version']).to be 2
          expect(parsed_json[0]['name']).to eq training_dataset_name
        end

        it "should fail to get a training dataset with a name that does not exists" do
          # Get the list
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          get_training_datasets_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project.id}/featurestores/#{featurestore_id}/trainingdatasets/doesnotexists/"
          get get_training_datasets_endpoint
          expect_status(404)
        end

        it "should fail to create a training dataset with no features and no query" do
          featurestore_id = get_featurestore_id(@project.id)
          json_data = {
              name: "no_features_no_query",
              version: 1,
              dataFormat: "csv",
              trainingDatasetType: "HOPSFS_TRAINING_DATASET",
          }
          post "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/trainingdatasets", json_data.to_json
          expect_status_details(400)
        end

        it "should be able to create a training dataset from a query object - 1" do
          # create feature group
          featurestore_id = get_featurestore_id(@project.id)
          features = [
              {type: "INT", name: "testfeature", primary: true},
              {type: "INT", name: "testfeature1"},
          ]
          fg_id = create_cached_featuregroup_checked(@project.id, featurestore_id, "test_fg_#{short_random_id}", features: features)
          # create queryDTO object
          query = {
              leftFeatureGroup: {
                  id: fg_id
              },
              leftFeatures: [{name: 'testfeature'}, {name: 'testfeature1'}]
          }
          json_result, _ = create_hopsfs_training_dataset(@project.id, featurestore_id, nil, query:query)
          expect_status_details(201)
          training_dataset = JSON.parse(json_result)
          expect(training_dataset['fromQuery']).to be true
          td_features = training_dataset['features']
          expect(td_features.count).to eql(2)
          expect(td_features.select{|feature| feature['index'] == 0}[0]['name']).to eql("testfeature")
          expect(td_features.select{|feature| feature['index'] == 0}[0]['featuregroup']['id']).to eql(fg_id)
          expect(td_features.select{|feature| feature['index'] == 1}[0]['name']).to eql("testfeature1")
          expect(td_features.select{|feature| feature['index'] == 1}[0]['featuregroup']['id']).to eql(fg_id)
        end

        it "should be able to create a training dataset from a query object with label" do
          # create feature group
          featurestore_id = get_featurestore_id(@project.id)
          features = [
              {type: "INT", name: "testfeature", primary: true},
              {type: "INT", name: "testfeature1"},
          ]
          fg_id = create_cached_featuregroup_checked(@project.id, featurestore_id, "test_fg_#{short_random_id}", features: features)
          # create queryDTO object
          query = {
              leftFeatureGroup: {
                  id: fg_id
              },
              leftFeatures: [{name: 'testfeature'}, {name: 'testfeature1'}]
          }
          features = [
              {type: "INT", name: "testfeature", label: true},
              {type: "INT", name: "testfeature1", label: false},
          ]
          json_result, _ = create_hopsfs_training_dataset(@project.id, featurestore_id, nil, query: query, features:
              features)
          expect_status_details(201)
          training_dataset = JSON.parse(json_result)
          expect(training_dataset['fromQuery']).to be true
          td_features = training_dataset['features']
          expect(td_features.count).to eql(2)
          expect(td_features.select{|feature| feature['index'] == 0}[0]['label']).to be true
          expect(td_features.select{|feature| feature['index'] == 1}[0]['label']).to be false
        end

        it "should be able to create a training dataset from a query object with missing label" do
          # create feature group
          featurestore_id = get_featurestore_id(@project.id)
          features = [
            {type: "INT", name: "testfeature", primary: true},
            {type: "INT", name: "testfeature1"},
          ]
          fg_id = create_cached_featuregroup_checked(@project.id, featurestore_id, "test_fg_#{short_random_id}", features: features)
          # create queryDTO object
          query = {
            leftFeatureGroup: {
              id: fg_id
            },
            leftFeatures: [{name: 'testfeature'}, {name: 'testfeature1'}]
          }
          features = [
            {type: "INT", name: "does_not_exists", label: true},
          ]
          create_hopsfs_training_dataset(@project.id, featurestore_id, nil, query: query, features: features)
          expect_status_details(404)
        end

        it "should fail to create a training dataset with invalid query" do
          # create feature group
          featurestore_id = get_featurestore_id(@project.id)
          features = [
              {type: "INT", name: "testfeature", primary: true},
              {type: "INT", name: "testfeature1"},
          ]
          fg_id = create_cached_featuregroup_checked(@project.id, featurestore_id, "test_fg_#{short_random_id}", features: features)
          # create queryDTO object
          query = {
              leftFeatureGroup: {
                  id: fg_id
              },
              leftFeatures: [{name: 'does_not_exists'}]
          }
          create_hopsfs_training_dataset(@project.id, featurestore_id, nil, query: query)
          expect_status_details(400)
        end

        # we have unit tests for the query generation, so here we are testing just that it integrates
        # correctly with training datasets
        it "should be able to create a training dataset from a query object - 2" do
          # create first feature group
          featurestore_id = get_featurestore_id(@project.id)
          features = [
              {type: "INT", name: "a_testfeature", primary: true},
              {type: "INT", name: "a_testfeature1"},
          ]
          fg_id = create_cached_featuregroup_checked(@project.id, featurestore_id, "test_fg_#{short_random_id}", features: features)
          # create second feature group
          features = [
              {type: "INT", name: "a_testfeature", primary: true},
              {type: "INT", name: "b_testfeature1"},
          ]
          fg_id_b = create_cached_featuregroup_checked(@project.id, featurestore_id, "test_fg_b_#{short_random_id}", features: features)
          # create queryDTO object
          query = {
              leftFeatureGroup: {
                  id: fg_id
              },
              leftFeatures: [{name: 'a_testfeature'}, {name: 'a_testfeature1'}],
              joins: [{
                       query: {
                           leftFeatureGroup: {
                               id: fg_id_b
                           },
                           leftFeatures: [{name: 'a_testfeature'}, {name: 'b_testfeature1'}]
                       }
                  }
              ]
          }

          json_result, _ = create_hopsfs_training_dataset(@project.id, featurestore_id, nil, query:query)
          expect_status_details(201)
          training_dataset = JSON.parse(json_result)
          expect(training_dataset['fromQuery']).to be true

          td_features = training_dataset['features']
          expect(td_features.length).to eql(3)
          # check that all the features are indexed correctly and that they are picked from the correct feature group
          expect(td_features.select{|feature| feature['index'] == 0}[0]['name']).to eql("a_testfeature")
          expect(td_features.select{|feature| feature['index'] == 0}[0]['featuregroup']['id']).to eql(fg_id)
          expect(td_features.select{|feature| feature['index'] == 1}[0]['name']).to eql("a_testfeature1")
          expect(td_features.select{|feature| feature['index'] == 1}[0]['featuregroup']['id']).to eql(fg_id)
          expect(td_features.select{|feature| feature['index'] == 2}[0]['name']).to eql("b_testfeature1")
          expect(td_features.select{|feature| feature['index'] == 2}[0]['featuregroup']['id']).to eql(fg_id_b)
        end

        it "should fail to replay the query from a feature based training dataset" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])

          features = [
              {type: "int", name: "testfeature"},
              {type: "int", name: "testfeature2"}
          ]

          json_result, _ = create_hopsfs_training_dataset(project.id, featurestore_id, connector, features: features)
          expect_status_details(201)
          training_dataset = JSON.parse(json_result)
          get "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/trainingdatasets/#{training_dataset['id']}/query"
          expect_status_details(400)
        end

        it "should succeed to replay the query from a query based training dataset" do
          project_name = @project.projectname
          featurestore_id = get_featurestore_id(@project.id)
          featurestore_name = get_featurestore_name(@project.id)
          features = [
              {type: "INT", name: "a_testfeature", primary: true},
              {type: "INT", name: "a_testfeature1"},
          ]
          fg_a_name = "test_fg_#{short_random_id}"
          fg_id = create_cached_featuregroup_checked(@project.id, featurestore_id, fg_a_name, features: features)
          # create second feature group
          features = [
              {type: "INT", name: "a_testfeature", primary: true},
              {type: "INT", name: "b_testfeature1"},
          ]
          fg_b_name = "test_fg_#{short_random_id}"
          fg_id_b = create_cached_featuregroup_checked(@project.id, featurestore_id, fg_b_name, features: features)
          # create queryDTO object
          query = {
              leftFeatureGroup: {
                  id: fg_id
              },
              leftFeatures: [{name: 'a_testfeature'}, {name: 'a_testfeature1'}],
              joins: [{
                       query: {
                           leftFeatureGroup: {
                               id: fg_id_b
                           },
                           leftFeatures: [{name: 'a_testfeature'}, {name: 'b_testfeature1'}]
                       }
                  }
              ]
          }

          json_result, _ = create_hopsfs_training_dataset(@project.id, featurestore_id, nil, query:query)
          expect_status_details(201)
          training_dataset = JSON.parse(json_result)

          json_result = get "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/trainingdatasets/#{training_dataset['id']}/query"
          expect_status_details(200)
          query = JSON.parse(json_result)

          expect(query['query']).to eql("SELECT `fg0`.`a_testfeature` `a_testfeature`, `fg0`.`a_testfeature1` `a_testfeature1`, `fg1`.`b_testfeature1` `b_testfeature1`\n" +
           "FROM `#{featurestore_name}`.`#{fg_a_name}_1` `fg0`\n" +
           "INNER JOIN `#{featurestore_name}`.`#{fg_b_name}_1` `fg1` ON `fg0`.`a_testfeature` = `fg1`.`a_testfeature`")

          expect(query['queryOnline']).to eql("SELECT `fg0`.`a_testfeature` `a_testfeature`, `fg0`.`a_testfeature1` `a_testfeature1`, `fg1`.`b_testfeature1` `b_testfeature1`\n" +
           "FROM `#{project_name.downcase}`.`#{fg_a_name}_1` `fg0`\n" +
           "INNER JOIN `#{project_name.downcase}`.`#{fg_b_name}_1` `fg1` ON `fg0`.`a_testfeature` = `fg1`.`a_testfeature`")
        end

        it "should succeed to replay the query from a query based training dataset with hive query flag" do
          project_name = @project.projectname
          featurestore_id = get_featurestore_id(@project.id)
          featurestore_name = get_featurestore_name(@project.id)
          features = [
            {type: "INT", name: "a_testfeature", primary: true},
            {type: "INT", name: "a_testfeature1"},
          ]
          fg_a_name = "test_fg_#{short_random_id}"
          fg_id = create_cached_featuregroup_checked(@project.id, featurestore_id, fg_a_name, features: features)
          # create second feature group
          features = [
            {type: "INT", name: "a_testfeature", primary: true},
            {type: "INT", name: "b_testfeature1"},
          ]
          fg_b_name = "test_fg_#{short_random_id}"
          fg_id_b = create_cached_featuregroup_checked(@project.id, featurestore_id, fg_b_name, features: features)
          # create queryDTO object
          query = {
            leftFeatureGroup: {
              id: fg_id
            },
            leftFeatures: [{name: 'a_testfeature'}, {name: 'a_testfeature1'}],
            joins: [{
                      query: {
                        leftFeatureGroup: {
                          id: fg_id_b
                        },
                        leftFeatures: [{name: 'a_testfeature'}, {name: 'b_testfeature1'}]
                      }
                    }
            ]
          }

          json_result, _ = create_hopsfs_training_dataset(@project.id, featurestore_id, nil, query:query)
          expect_status_details(201)
          training_dataset = JSON.parse(json_result)

          json_result = get "#{ENV['HOPSWORKS_API']}/project/#{@project
                                                                 .id}/featurestores/#{featurestore_id}/trainingdatasets/#{training_dataset['id']}/query?hiveQuery=true"
          expect_status_details(200)
          query = JSON.parse(json_result)

          expect(query['query']).to eql("SELECT `fg0`.`a_testfeature` `a_testfeature`, `fg0`.`a_testfeature1` `a_testfeature1`, `fg1`.`b_testfeature1` `b_testfeature1`\n" +
                                          "FROM `#{featurestore_name}`.`#{fg_a_name}_1` `fg0`\n" +
                                          "INNER JOIN `#{featurestore_name}`.`#{fg_b_name}_1` `fg1` ON `fg0`.`a_testfeature` = `fg1`.`a_testfeature`")

          expect(query['queryOnline']).to eql("SELECT `fg0`.`a_testfeature` `a_testfeature`, `fg0`.`a_testfeature1` `a_testfeature1`, `fg1`.`b_testfeature1` `b_testfeature1`\n" +
                                                "FROM `#{project_name.downcase}`.`#{fg_a_name}_1` `fg0`\n" +
                                                "INNER JOIN `#{project_name.downcase}`.`#{fg_b_name}_1` `fg1` ON `fg0`.`a_testfeature` = `fg1`.`a_testfeature`")
        end

        it "should succeed to replay a training dataset query using on-demand and cached feature groups" do
          featurestore_id = get_featurestore_id(@project[:id])
          featurestore_name = get_featurestore_name(@project.id)
          with_jdbc_connector(@project[:id])
          features = [{type: "INT", name: "testfeature", description: "testfeaturedescription", primary: true}]
          json_result, _ = create_on_demand_featuregroup(@project[:id], featurestore_id, @jdbc_connector_id, features: features)
          expect_status(201)
          parsed_json = JSON.parse(json_result)
          fg_ond_id = parsed_json["id"]

          features = [{type: "INT", name: "testfeature", description: "testfeaturedescription", primary: true},
                      {type: "INT", name: "anotherfeature", primary: false}]
          json_result, fg_name = create_cached_featuregroup(@project[:id], featurestore_id, features: features)
          parsed_json = JSON.parse(json_result)
          fg_cached_id = parsed_json["id"]

          query = {
              leftFeatureGroup: {id: fg_cached_id},
              leftFeatures: [{name: 'anotherfeature'}],
              joins: [{query: {
                  leftFeatureGroup: {id: fg_ond_id},
                  leftFeatures: [{name: 'testfeature'}]
              }}]}

          td_schema = [
              {type: "INT", name: "anotherfeature", label: false},
              {type: "INT", name: "testfeature", label: true}
          ]

          json_result, _ = create_hopsfs_training_dataset(@project.id, featurestore_id, nil, query: query, features: td_schema)
          expect_status_details(201)
          training_dataset = JSON.parse(json_result)

          # with Label
          json_result = get "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/trainingdatasets/#{training_dataset['id']}/query?withLabel=true"
          expect_status_details(200)
          query = JSON.parse(json_result)

          expect(query['query']).to eql("SELECT `fg0`.`anotherfeature` `anotherfeature`, `fg1`.`testfeature` `testfeature`\n" +
                                        "FROM `#{featurestore_name}`.`#{fg_name}_1` `fg0`\n" +
                                        "INNER JOIN `fg1` ON `fg0`.`testfeature` = `fg1`.`testfeature`")
          expect(query.key?('onDemandFeatureGroups')).to be true
          expect(query.key?("queryOnline")).to be false
        end

        it "should succeed to replay the query from a query based training dataset with and without label" do
          project_name = @project.projectname
          featurestore_id = get_featurestore_id(@project.id)
          featurestore_name = get_featurestore_name(@project.id)
          features = [
              {type: "INT", name: "a_testfeature", primary: true},
              {type: "INT", name: "a_testfeature1"},
          ]
          fg_a_name = "test_fg_#{short_random_id}"
          fg_id = create_cached_featuregroup_checked(@project.id, featurestore_id, fg_a_name, features: features)
          # create second feature group
          features = [
              {type: "INT", name: "a_testfeature", primary: true},
              {type: "INT", name: "b_testfeature1"},
          ]
          fg_b_name = "test_fg_#{short_random_id}"
          fg_id_b = create_cached_featuregroup_checked(@project.id, featurestore_id, fg_b_name, features: features)
          # create queryDTO object
          query = {
              leftFeatureGroup: {
                  id: fg_id
              },
              leftFeatures: [{name: 'a_testfeature'}, {name: 'a_testfeature1'}],
              joins: [{
                          query: {
                              leftFeatureGroup: {
                                  id: fg_id_b
                              },
                              leftFeatures: [{name: 'a_testfeature'}, {name: 'b_testfeature1'}]
                          }
                      }
              ]
          }

          td_schema = [
              {type: "INT", name: "a_testfeature", label: false},
              {type: "INT", name: "a_testfeature1", label: false},
              {type: "INT", name: "b_testfeature1", label: true},
          ]

          json_result, _ = create_hopsfs_training_dataset(@project.id, featurestore_id, nil, query: query, features:
              td_schema)
          expect_status_details(201)
          training_dataset = JSON.parse(json_result)

          # with Label
          json_result = get "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/trainingdatasets/#{training_dataset['id']}/query?withLabel=true"
          expect_status_details(200)
          query = JSON.parse(json_result)

          expect(query['query']).to eql("SELECT `fg0`.`a_testfeature` `a_testfeature`, `fg0`.`a_testfeature1` `a_testfeature1`, `fg1`.`b_testfeature1` `b_testfeature1`\n" +
                                            "FROM `#{featurestore_name}`.`#{fg_a_name}_1` `fg0`\n" +
                                            "INNER JOIN `#{featurestore_name}`.`#{fg_b_name}_1` `fg1` ON `fg0`.`a_testfeature` = `fg1`.`a_testfeature`")

          expect(query['queryOnline']).to eql("SELECT `fg0`.`a_testfeature` `a_testfeature`, `fg0`.`a_testfeature1` `a_testfeature1`, `fg1`.`b_testfeature1` `b_testfeature1`\n" +
                                                  "FROM `#{project_name.downcase}`.`#{fg_a_name}_1` `fg0`\n" +
                                                  "INNER JOIN `#{project_name.downcase}`.`#{fg_b_name}_1` `fg1` ON `fg0`.`a_testfeature` = `fg1`.`a_testfeature`")

          # Without Label
          json_result = get "#{ENV['HOPSWORKS_API']}/project/#{@project
                                                                   .id}/featurestores/#{featurestore_id}/trainingdatasets/#{training_dataset['id']}/query?withLabel=false"
          expect_status_details(200)
          query = JSON.parse(json_result)

          expect(query['query']).to eql("SELECT `fg0`.`a_testfeature` `a_testfeature`, `fg0`.`a_testfeature1` `a_testfeature1`\n" +
                                            "FROM `#{featurestore_name}`.`#{fg_a_name}_1` `fg0`\n" +
                                            "INNER JOIN `#{featurestore_name}`.`#{fg_b_name}_1` `fg1` ON `fg0`.`a_testfeature` = `fg1`.`a_testfeature`")

          expect(query['queryOnline']).to eql("SELECT `fg0`.`a_testfeature` `a_testfeature`, `fg0`.`a_testfeature1` `a_testfeature1`\n" +
                                                  "FROM `#{project_name.downcase}`.`#{fg_a_name}_1` `fg0`\n" +
                                                  "INNER JOIN `#{project_name.downcase}`.`#{fg_b_name}_1` `fg1` ON `fg0`.`a_testfeature` = `fg1`.`a_testfeature`")
        end

        it "should succeed to replay the query from a query based training dataset with default values" do
          project_name = @project.projectname
          featurestore_id = get_featurestore_id(@project.id)
          featurestore_name = get_featurestore_name(@project.id)
          features = [
              {type: "INT", name: "a_testfeature", primary: true},
              {type: "INT", name: "a_testfeature1"},
          ]
          fg_a_name = "test_fg_#{short_random_id}"
          fg_id = create_cached_featuregroup_checked(@project.id, featurestore_id, fg_a_name, features: features)
          # append column with default
          features.push(
              {
                  type: "DOUBLE",
                  name: "a_testfeature_appended",
                  defaultValue: "10.0"
              },
          )
          json_result = update_cached_featuregroup_metadata(@project.id, featurestore_id, fg_id, 1,
                                                            featuregroup_name: fg_a_name, features: features)
          expect_status_details(200)
          # create second feature group
          features = [
              {type: "INT", name: "a_testfeature", primary: true},
              {type: "INT", name: "b_testfeature1"},
          ]
          fg_b_name = "test_fg_#{short_random_id}"
          fg_id_b = create_cached_featuregroup_checked(@project.id, featurestore_id, fg_b_name, features: features)
          # create queryDTO object
          query = {
              leftFeatureGroup: {
                  id: fg_id
              },
              leftFeatures: [{name: 'a_testfeature1'}, {name: 'a_testfeature_appended'}],
              joins: [{
                          query: {
                              leftFeatureGroup: {
                                  id: fg_id_b
                              },
                              leftFeatures: [{name: 'a_testfeature'}, {name: 'b_testfeature1'}]
                          },
                          leftOn: [{name: "a_testfeature_appended"}],
                          rightOn: [{name: "a_testfeature"}],
                          type: "INNER"
                      }
              ]
          }

          json_result, _ = create_hopsfs_training_dataset(@project.id, featurestore_id, nil, query:query)
          expect_status_details(201)
          training_dataset = JSON.parse(json_result)

          json_result = get "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/trainingdatasets/#{training_dataset['id']}/query"
          expect_status_details(200)
          query = JSON.parse(json_result)

          expect(query['query']).to eql("SELECT `fg0`.`a_testfeature1` `a_testfeature1`, " +
                                        "CASE WHEN `fg0`.`a_testfeature_appended` IS NULL THEN 10.0 ELSE `fg0`.`a_testfeature_appended` END `a_testfeature_appended`, " +
                                        "`fg1`.`a_testfeature` `a_testfeature`, " +
                                        "`fg1`.`b_testfeature1` `b_testfeature1`\n" +
                                        "FROM `#{featurestore_name}`.`#{fg_a_name}_1` `fg0`\n" +
                                        "INNER JOIN `#{featurestore_name}`.`#{fg_b_name}_1` `fg1` " +
                                            "ON CASE WHEN `fg0`.`a_testfeature_appended` IS NULL THEN 10.0 ELSE `fg0`.`a_testfeature_appended` END = `fg1`.`a_testfeature`")

          expect(query['queryOnline']).to eql("SELECT `fg0`.`a_testfeature1` `a_testfeature1`, `fg0`.`a_testfeature_appended` `a_testfeature_appended`, `fg1`.`a_testfeature` `a_testfeature`, `fg1`.`b_testfeature1` `b_testfeature1`\n" +
                                                  "FROM `#{project_name.downcase}`.`#{fg_a_name}_1` `fg0`\n" +
                                                  "INNER JOIN `#{project_name.downcase}`.`#{fg_b_name}_1` `fg1` ON `fg0`.`a_testfeature_appended` = `fg1`.`a_testfeature`")
        end

        it "should be able to replay a query for a dataset with a feature group joined with itself" do
          project_name = @project.projectname
          featurestore_id = get_featurestore_id(@project.id)
          featurestore_name = get_featurestore_name(@project.id)
          features = [
              {type: "INT", name: "a_testfeature", primary: true},
              {type: "INT", name: "a_testfeature1"},
          ]
          fg_name = "test_fg_#{short_random_id}"
          fg_id = create_cached_featuregroup_checked(@project.id, featurestore_id, fg_name, features: features)
          # create queryDTO object
          query = {
              leftFeatureGroup: {
                  id: fg_id
              },
              leftFeatures: [{name: 'a_testfeature'}],
              joins: [{
                          query: {
                              leftFeatureGroup: {
                                  id: fg_id
                              },
                              leftFeatures: [{name: 'a_testfeature1'}]
                          },
                          type: "LEFT"
                      }
              ]
          }

          json_result, _ = create_hopsfs_training_dataset(@project.id, featurestore_id, nil, query:query)
          expect_status_details(201)
          training_dataset = JSON.parse(json_result)

          json_result = get "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/trainingdatasets/#{training_dataset['id']}/query"
          expect_status_details(200)
          query = JSON.parse(json_result)

          expect(query['query']).to eql("SELECT `fg0`.`a_testfeature` `a_testfeature`, `fg1`.`a_testfeature1` `a_testfeature1`\n" +
              "FROM `#{featurestore_name}`.`#{fg_name}_1` `fg0`\n" +
              "LEFT JOIN `#{featurestore_name}`.`#{fg_name}_1` `fg1` ON `fg0`.`a_testfeature` = `fg1`.`a_testfeature`")

          expect(query['queryOnline']).to eql("SELECT `fg0`.`a_testfeature` `a_testfeature`, `fg1`.`a_testfeature1` `a_testfeature1`\n" +
              "FROM `#{project_name.downcase}`.`#{fg_name}_1` `fg0`\n" +
              "LEFT JOIN `#{project_name.downcase}`.`#{fg_name}_1` `fg1` ON `fg0`.`a_testfeature` = `fg1`.`a_testfeature`")
        end

        it "should create a training dataset with a large amount of features (3k)" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])

          features = Array.new(3000) {|i| {type:"int", name: "ft#{i}"}}

          json_result, _ = create_hopsfs_training_dataset(project.id, featurestore_id, connector, features: features)
          expect_status_details(201)
          training_dataset = JSON.parse(json_result)

          expect(training_dataset['features'].length).to eql(3000)

          delete "#{ENV['HOPSWORKS_API']}/project/#{project.id}/featurestores/#{featurestore_id}/trainingdatasets/#{training_dataset['id']}"
          expect_status_details(200)
        end

        it "should create a query based training dataset with a large amount of features (3k)" do
          project_name = @project.projectname
          featurestore_id = get_featurestore_id(@project.id)
          featurestore_name = get_featurestore_name(@project.id)

          fg_ids = []
          (0..3).each { |fg|
            features = Array.new(1000) { |i| {type: "int", name: "fg_#{fg}_ft#{i}"} }
            features = features << {type: "int", name: "jk", primary: true}
            fg_ids << create_cached_featuregroup_checked(@project.id, featurestore_id, "test_fg_#{short_random_id}", features: features)
          }

          # create queryDTO object
          query = {
              leftFeatureGroup: {
                  id: fg_ids[0]
              },
              leftFeatures: [{name: '*'}],
              joins: [{
                          query: {
                              leftFeatureGroup: {
                                  id: fg_ids[1]
                              },
                              leftFeatures: [{name: '*'}]
                          },
                      },
                      {
                          query: {
                              leftFeatureGroup: {
                                  id: fg_ids[2]
                              },
                              leftFeatures: [{name: '*'}]
                          },
                      }
              ]
          }

          json_result, _ = create_hopsfs_training_dataset(@project.id, featurestore_id, nil, query:query)
          expect_status_details(201)
          training_dataset = JSON.parse(json_result)

          expect(training_dataset['features'].length).to eql(3001)

          get "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/trainingdatasets/#{training_dataset['id']}/query"
          expect_status_details(200)
        end

        it "should return a proper error when building the query if a feature group was deleted" do
          featurestore_id = get_featurestore_id(@project.id)
          features = [
              {type: "INT", name: "a_testfeature", primary: true},
              {type: "INT", name: "a_testfeature1"},
          ]
          fg_id = create_cached_featuregroup_checked(@project.id, featurestore_id, "test_fg_#{short_random_id}", features: features)
          # create second feature group
          features = [
              {type: "INT", name: "a_testfeature", primary: true},
              {type: "INT", name: "b_testfeature1"},
          ]
          fg_id_b = create_cached_featuregroup_checked(@project.id, featurestore_id, "test_fg_#{short_random_id}", features: features)
          # create queryDTO object
          query = {
              leftFeatureGroup: {
                  id: fg_id
              },
              leftFeatures: [{name: 'a_testfeature'}, {name: 'a_testfeature1'}],
              joins: [{
                        query: {
                            leftFeatureGroup: {
                                id: fg_id_b
                            },
                            leftFeatures: [{name: 'a_testfeature'}, {name: 'b_testfeature1'}]
                        }
                   }
              ]
          }

          json_result, training_dataset_name = create_hopsfs_training_dataset(@project.id, featurestore_id, nil, query:query)
          expect_status_details(201)
          training_dataset = JSON.parse(json_result)

          # delete feature group
          delete_featuregroup_checked(@project.id, featurestore_id, fg_id_b)

          training_dataset = get_trainingdataset_checked(@project.id, training_dataset_name)
          # without the feature group we cannot re-create the query
          expect(training_dataset['fromQuery']).to be true

          features = training_dataset['features']
          # features should still be returned even if the original feature group was deleted
          expect(features[2]['name']).to eql("b_testfeature1")
          expect(features[2]['type']).to eql("int")

          json_result = get "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/trainingdatasets/#{training_dataset['id']}/query"
          # this is returning 200
          expect_status_details(200)
          parsed_json = JSON.parse(json_result)
          expect(parsed_json["query"]).to eql("Parent feature groups of the following features are not available anymore: b_testfeature1")
        end

        it "should be able to attach keywords" do
          featurestore_id = get_featurestore_id(@project[:id])
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
          json_result, _ = create_hopsfs_training_dataset(@project[:id], featurestore_id, connector)
          parsed_json = JSON.parse(json_result)
          training_dataset_id = parsed_json["id"]

          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/#{featurestore_id}/trainingdatasets/#{training_dataset_id}/keywords",
               {keywords: ['hello', 'this', 'keyword123']}.to_json

          json_result = get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/#{featurestore_id}/trainingdatasets/#{training_dataset_id}/keywords"
          expect_status(200)
          parsed_json = JSON.parse(json_result)
          expect(parsed_json['keywords']).to include('hello')
          expect(parsed_json['keywords']).to include('this')
          expect(parsed_json['keywords']).to include('keyword123')
        end

        it "should fail to attach invalid keywords" do
          featurestore_id = get_featurestore_id(@project[:id])
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
          json_result, _ = create_hopsfs_training_dataset(@project[:id], featurestore_id, connector)
          parsed_json = JSON.parse(json_result)
          training_dataset_id = parsed_json["id"]

          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/#{featurestore_id}/trainingdatasets/#{training_dataset_id}/keywords",
               {keywords: ['hello', 'this', '@#!@#^(&$']}
          expect_status(400)
        end

        it "should be able to remove keyword" do
          featurestore_id = get_featurestore_id(@project[:id])
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
          json_result, _ = create_hopsfs_training_dataset(@project[:id], featurestore_id, connector)
          parsed_json = JSON.parse(json_result)
          training_dataset_id = parsed_json["id"]

          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/#{featurestore_id}/trainingdatasets/#{training_dataset_id}/keywords",
               {keywords: ['hello', 'this', 'keyword123']}.to_json

          json_result = get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/#{featurestore_id}/trainingdatasets/#{training_dataset_id}/keywords"
          expect_status(200)
          parsed_json = JSON.parse(json_result)
          expect(parsed_json['keywords']).to include('hello')
          expect(parsed_json['keywords']).to include('this')
          expect(parsed_json['keywords']).to include('keyword123')

          delete "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/#{featurestore_id}/trainingdatasets/#{training_dataset_id}/keywords?keyword=hello"
          expect_status(200)

          json_result = get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/#{featurestore_id}/trainingdatasets/#{training_dataset_id}/keywords"
          expect_status(200)
          parsed_json = JSON.parse(json_result)
          expect(parsed_json['keywords']).not_to include('hello')
          expect(parsed_json['keywords']).to include('this')
          expect(parsed_json['keywords']).to include('keyword123')
        end

        it "should be able to create a training dataset without statistics settings to test the defaults" do
          featurestore_id = get_featurestore_id(@project[:id])
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
          json_result, _ = create_hopsfs_training_dataset(@project[:id], featurestore_id, connector)
          expect_status_details(201)
          parsed_json = JSON.parse(json_result)
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
          featurestore_id = get_featurestore_id(@project[:id])
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
          stats_config = {enabled: false, histograms: false, correlations: false, exactUniqueness: false, columns:
          ["testfeature"]}
          json_result, _ = create_hopsfs_training_dataset(@project[:id], featurestore_id, connector,
                                                          statistics_config: stats_config)
          expect_status_details(201)
          parsed_json = JSON.parse(json_result)
          expect(parsed_json["statisticsConfig"]["columns"].length).to eql(1)
          expect(parsed_json["statisticsConfig"]["columns"][0]).to eql("testfeature")
          expect(parsed_json["statisticsConfig"]["enabled"]).to be false
          expect(parsed_json["statisticsConfig"]["correlations"]).to be false
          expect(parsed_json["statisticsConfig"]["exactUniqueness"]).to be false
          expect(parsed_json["statisticsConfig"]["histograms"]).to be false
        end

        it "should not be possible to add a training dataset with non-existing statistic column" do
          featurestore_id = get_featurestore_id(@project[:id])
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
          stats_config = {enabled: false, histograms: false, correlations: false, exactUniqueness: false, columns: ["wrongname"]}
          json_result, _ = create_hopsfs_training_dataset(@project[:id], featurestore_id, connector,
                                                          statistics_config: stats_config)
          expect_status_details(400)
          parsed_json = JSON.parse(json_result)
          expect(parsed_json.key?("errorCode")).to be true
          expect(parsed_json.key?("errorMsg")).to be true
          expect(parsed_json.key?("usrMsg")).to be true
          expect(parsed_json["errorCode"]).to eql(270108)
        end

        it "should be able to update the statistics config of a training dataset" do
          featurestore_id = get_featurestore_id(@project[:id])
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
          json_result, _ = create_hopsfs_training_dataset(@project[:id], featurestore_id, connector)
          expect_status_details(201)
          parsed_json = JSON.parse(json_result)
          training_dataset_id = parsed_json["id"]
          training_dataset_version = parsed_json["version"]
          json_result = update_training_dataset_stats_config(@project[:id], featurestore_id, training_dataset_id,
                                                             training_dataset_version)
          expect_status_details(200)
          parsed_json = JSON.parse(json_result)
          expect(parsed_json["statisticsConfig"]["columns"].length).to eql(1)
          expect(parsed_json["statisticsConfig"]["columns"][0]).to eql("testfeature")
          expect(parsed_json["statisticsConfig"]["enabled"]).to be false
          expect(parsed_json["statisticsConfig"]["correlations"]).to be false
          expect(parsed_json["statisticsConfig"]["exactUniqueness"]).to be false
          expect(parsed_json["statisticsConfig"]["histograms"]).to be false
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
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector_id = get_s3_connector_id
          json_result, training_dataset_name = create_external_training_dataset(project.id, featurestore_id, connector_id)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          expect(parsed_json.key?("id")).to be true
          expect(parsed_json.key?("featurestoreName")).to be true
          expect(parsed_json.key?("name")).to be true
          expect(parsed_json["creator"].key?("email")).to be true
          expect(parsed_json.key?("location")).to be true
          expect(parsed_json.key?("version")).to be true
          expect(parsed_json.key?("dataFormat")).to be true
          expect(parsed_json.key?("trainingDatasetType")).to be true
          expect(parsed_json.key?("description")).to be true
          expect(parsed_json.key?("features")).to be true
          expect(parsed_json.key?("seed")).to be true
          expect(parsed_json["featurestoreName"] == project.projectname.downcase + "_featurestore").to be true
          expect(parsed_json["name"] == training_dataset_name).to be true
          expect(parsed_json["trainingDatasetType"] == "EXTERNAL_TRAINING_DATASET").to be true
          expect(parsed_json["storageConnector"]["id"] == connector_id).to be true
          expect(parsed_json["features"].length).to be 2
          expect(parsed_json["seed"] == 1234).to be true

          # make sure inode is created
          path = "/Projects/#{project['projectname']}/#{project['projectname']}_Training_Datasets/#{training_dataset_name}_1"
          expect(test_dir(path)).to be true
        end

        it "should not be able to add an external training dataset to the featurestore with upper case characters" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector_id = get_s3_connector_id
          training_dataset_name = "TEST_training_dataset"
          json_result, training_dataset_name = create_external_training_dataset(project.id, featurestore_id, connector_id,
                                                                                name:training_dataset_name)
          parsed_json = JSON.parse(json_result)
          expect_status(400)
          expect(parsed_json.key?("errorCode")).to be true
          expect(parsed_json.key?("errorMsg")).to be true
          expect(parsed_json.key?("usrMsg")).to be true
          expect(parsed_json["errorCode"] == 270091).to be true
        end

        it "should not be able to add an external training dataset to the featurestore without specifying a s3 connector" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, training_dataset_name = create_external_training_dataset(project.id, featurestore_id, nil)
          expect_status(404)
        end

        it "should be able to add an external training dataset to the featurestore with splits" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector_id = get_s3_connector_id
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
          json_result, training_dataset_name = create_external_training_dataset(project.id, featurestore_id,
                                                                                connector_id, splits: splits,
                                                                                train_split: "train_split")

          parsed_json = JSON.parse(json_result)
          expect_status(201)
          expect(parsed_json.key?("splits")).to be true
          expect(parsed_json["splits"].length).to be 2
        end

        it "should not be able to add an external training dataset to the featurestore with a non numeric split percentage" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector_id = get_s3_connector_id
          splits = [{name: "train", percentage: "wrong", splitType: "RANDOM_SPLIT"}]
          json_result, training_dataset_name = create_external_training_dataset(project.id, featurestore_id,
                                                                                connector_id, splits: splits,
                                                                                train_split: "train")
          parsed_json = JSON.parse(json_result)
          expect_status(400)
          expect(parsed_json.key?("errorCode")).to be true
          expect(parsed_json.key?("errorMsg")).to be true
          expect(parsed_json.key?("usrMsg")).to be true
          expect(parsed_json["errorCode"]).to eql(270099)
        end

        it "should not be able to add an external training dataset to the featurestore with a illegal split name" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector_id = get_s3_connector_id
          splits = [{name: "ILLEGALNAME!!!", percentage: 0.8}]
          json_result, training_dataset_name = create_external_training_dataset(project.id, featurestore_id,
                                                                                connector_id, splits: splits)
          parsed_json = JSON.parse(json_result)
          expect_status(400)
          expect(parsed_json.key?("errorCode")).to be true
          expect(parsed_json.key?("errorMsg")).to be true
          expect(parsed_json.key?("usrMsg")).to be true
          expect(parsed_json["errorCode"] == 270098).to be true
        end

        it "should not be able to add an external training dataset to the featurestore with splits of
        duplicate split names" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector_id = get_s3_connector_id
          splits = [
              {
                  name: "test_split",
                  percentage: 0.8
              },
              {
                  name: "test_split",
                  percentage: 0.2
              }
          ]
          json_result, training_dataset_name = create_external_training_dataset(project.id, featurestore_id,
                                                                                connector_id, splits: splits,
                                                                                train_split: "test_split")

          parsed_json = JSON.parse(json_result)
          expect_status(400)
          expect(parsed_json.key?("errorCode")).to be true
          expect(parsed_json.key?("errorMsg")).to be true
          expect(parsed_json.key?("usrMsg")).to be true
          expect(parsed_json["errorCode"] == 270106).to be true
        end

        it "should be able to add an external training dataset with label to the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector_id = get_s3_connector_id
          features = [
              {
                  type: "INT",
                  name: "testfeature",
                  label: true
              },
              {
                  type: "INT",
                  name: "testfeature2",
                  label: false
              }
          ]
          json_result, training_dataset_name = create_external_training_dataset(project.id, featurestore_id,
                                                                                connector_id, features: features)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          td_features = parsed_json['features']
          expect(td_features.count).to eql(2)
          expect(td_features.select{|feature| feature['index'] == 0}[0]['label']).to be true
          expect(td_features.select{|feature| feature['index'] == 1}[0]['label']).to be false
        end

        it "should be able to delete an external training dataset from the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector_id = get_s3_connector_id
          json_result1, training_dataset_name = create_external_training_dataset(project.id, featurestore_id, connector_id)
          parsed_json1 = JSON.parse(json_result1)
          expect_status(201)
          training_dataset_id = parsed_json1["id"]
          delete_training_dataset_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s +
              "/featurestores/" + featurestore_id.to_s + "/trainingdatasets/" + training_dataset_id.to_s
          delete delete_training_dataset_endpoint
          expect_status(200)

          # make sure inode is deleted
          path = "/Projects/#{project['projectname']}/#{project['projectname']}_Training_Datasets/#{training_dataset_name}_1"
          expect(test_dir(path)).to be false
        end

        it "should be able to update the metadata (description) of an external training dataset from the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector_id = get_s3_connector_id
          json_result1, training_dataset_name = create_external_training_dataset(project.id, featurestore_id, connector_id)
          parsed_json1 = JSON.parse(json_result1)
          expect_status(201)
          training_dataset_id = parsed_json1["id"]
          json_result2 = update_external_training_dataset_metadata(project.id, featurestore_id, training_dataset_id,
                                                                   training_dataset_name, "new description",
                                                                   connector_id)
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
          expect(parsed_json2["featurestoreName"] == project.projectname.downcase + "_featurestore").to be true
          expect(parsed_json2["description"] == "new description").to be true
          expect(parsed_json2["trainingDatasetType"] == "EXTERNAL_TRAINING_DATASET").to be true
        end

        it "should not be able do change the storage connector" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector_id = get_s3_connector_id
          json_result1, training_dataset_name = create_external_training_dataset(project.id, featurestore_id, connector_id)
          parsed_json1 = JSON.parse(json_result1)
          expect_status(201)

          training_dataset_id = parsed_json1["id"]
          json_new_connector, _ = create_s3_connector(project.id, featurestore_id)
          new_connector = JSON.parse(json_new_connector)

          json_result2 = update_external_training_dataset_metadata(project.id, featurestore_id,
                                                                   training_dataset_id, training_dataset_name, "desc",
                                                                   new_connector['id'])
          parsed_json2 = JSON.parse(json_result2)
          expect_status(200)

          # make sure the name didn't change
          expect(parsed_json2["storageConnector"]["id"]).to be connector_id
        end

        it "should store and return the correct path within the bucket" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector_id = get_s3_connector_id
          json_result1, training_dataset_name = create_external_training_dataset(project.id, featurestore_id,
                                                                                 connector_id,
                                                                                 location: "/inner/location")
          parsed_json1 = JSON.parse(json_result1)
          expect_status(201)
          expect(parsed_json1['location']).to eql("s3://testbucket/inner/location/#{training_dataset_name}_1")
        end

        it "should be able to create a training dataset using ADLS connector" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector = create_adls_connector(project.id, featurestore_id)
          connector = JSON.parse(connector)
          json_result1, training_dataset_name = create_external_training_dataset(project.id, featurestore_id,
                                                                                 connector["id"],
                                                                                 location: "/inner/location/")
          parsed_json1 = JSON.parse(json_result1)
          expect_status_details(201)
          expect(parsed_json1['location']).to eql("abfss://containerName@accountName.dfs.core.windows.net/inner/location/#{training_dataset_name}_1")
        end

        it "should not be able to create a training dataset using a SNOWFLAKE connector" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector = create_snowflake_connector(project.id, featurestore_id)
          connector = JSON.parse(connector)
          json_result1, training_dataset_name = create_external_training_dataset(project.id, featurestore_id,
                                                                                 connector["id"],
                                                                                 location: "/inner/location")
          expect_status_details(400)
        end

        it "should not be able to create a training dataset using a REDSHIFT connector" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector, _ = create_redshift_connector(project.id, featurestore_id, databasePassword: "pwdf")
          connector = JSON.parse(connector)
          json_result1, training_dataset_name = create_external_training_dataset(project.id, featurestore_id,
                                                                                 connector["id"],
                                                                                 location: "/inner/location")
          expect_status_details(400)
        end

        it "should not be able to create a training dataset using a JDBC connector" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector, _ = create_jdbc_connector(project.id, featurestore_id)
          connector = JSON.parse(connector)
          json_result1, training_dataset_name = create_external_training_dataset(project.id, featurestore_id,
                                                                                 connector["id"],
                                                                                 location: "/inner/location")
          expect_status_details(400)
        end

        it "should be able to create a training dataset using a GCS connector" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          create_test_files
          key_path = "/Projects/#{@project['projectname']}/Resources/sampleKey.json"
          bucket = 'testbucket'
          json_result, connector_name = create_gcs_connector(project.id, featurestore_id, key_path, bucket)
          connector = JSON.parse(json_result)
          json_result1, training_dataset_name = create_external_training_dataset(project.id, featurestore_id,
                                                                                 connector["id"],
                                                                                 location: "/inner/location")
          parsed_json1 = JSON.parse(json_result1)
          expect_status_details(201)
          expect(parsed_json1['location']).to eql("gs://#{bucket}/inner/location/#{training_dataset_name}_1")
        end

      end
    end

    describe "with quotas enabled" do
      before :all do
        setVar("quotas_training_datasets", "1")
      end
      after :all do
        setVar("quotas_training_datasets", "-1")
      end
      it "should fail to created training datasets if quota has been reached" do
        project = create_project
        featurestore_id = get_featurestore_id(project.id)
        connector = get_hopsfs_training_datasets_connector(project.projectname)
        ## This should go through
        create_hopsfs_training_dataset(project.id, featurestore_id, connector)
        expect_status(201)

        ## This request should fail because quota has been reached
        result, _ = create_hopsfs_training_dataset(project.id, featurestore_id, connector)
        expect_status(500)
        parsed = JSON.parse(result)
        expect(parsed['devMsg']).to include("quota")
      end
    end

    describe "list" do
      context 'with valid project, featurestore service enabled' do
        before :all do
          with_valid_project
        end

        it "should be able to list all training datasets of the project's featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
          get_training_datasets_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/trainingdatasets"
          json_result1 = get get_training_datasets_endpoint
          parsed_json1 = JSON.parse(response.body)
          expect_status(200)
          expect(parsed_json1.length == 0).to be true
          json_result2, training_dataset_name = create_hopsfs_training_dataset(project.id, featurestore_id, connector)
          expect_status(201)
          json_result3 = get get_training_datasets_endpoint
          parsed_json2 = JSON.parse(json_result3)
          expect_status(200)
          expect(parsed_json2.length == 1).to be true
          expect(parsed_json2[0].key?("id")).to be true
          expect(parsed_json2[0].key?("featurestoreName")).to be true
          expect(parsed_json2[0].key?("name")).to be true
          expect(parsed_json2[0]["featurestoreName"] == project.projectname.downcase + "_featurestore").to be true
          expect(parsed_json2[0]["name"] == training_dataset_name).to be true
        end

        it "should be able to get a training dataset with a particular id" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
          json_result1, training_dataset_name = create_hopsfs_training_dataset(project.id, featurestore_id, connector)
          expect_status(201)
          parsed_json1 = JSON.parse(json_result1)
          expect_status(201)
          training_dataset_id = parsed_json1["id"]
          get_training_dataset_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/trainingdatasets/" + training_dataset_id.to_s
          json_result2 = get get_training_dataset_endpoint
          parsed_json2 = JSON.parse(json_result2)
          expect_status(200)
          expect(parsed_json2.key?("id")).to be true
          expect(parsed_json2.key?("featurestoreName")).to be true
          expect(parsed_json2.key?("featurestoreId")).to be true
          expect(parsed_json2.key?("name")).to be true
          expect(parsed_json2["featurestoreId"] == featurestore_id).to be true
          expect(parsed_json2["featurestoreName"] == project.projectname.downcase + "_featurestore").to be true
          expect(parsed_json2["name"] == training_dataset_name).to be true
          expect(parsed_json2["id"] == training_dataset_id).to be true
        end

        it "should be able to get a training dataset serving vector in correct order" do
          # create first feature group
          featurestore_id = get_featurestore_id(@project.id)
          project_name = @project.projectname
          features = ['a', 'b', 'c', 'd'].map do  |feat_name|
              {type: "INT", name: feat_name}
          end
          features[0]['primary'] = true
          json_result, fg_name = create_cached_featuregroup(@project.id, featurestore_id, features: features,
                                                            featuregroup_name: "test_fg_a_#{short_random_id}", online:true)
          parsed_json = JSON.parse(json_result)
          fg_id = parsed_json["id"]
          # create queryDTO object
          query = {
            leftFeatureGroup: {
              id: fg_id
            },
            leftFeatures: ['d', 'c', 'a', 'b'].map do |feat_name|
              {name: feat_name}
            end,
            joins: []
          }

          json_result, _ = create_hopsfs_training_dataset(@project.id, featurestore_id, nil, query:query)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          training_dataset_id = parsed_json["id"]
          json_result = get "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/trainingdatasets/#{training_dataset_id}/preparedstatements"
          expect_status_details(200)
          parsed_json = JSON.parse(json_result)
          expect(parsed_json["items"].first["preparedStatementParameters"].first["index"]).to eql(1)
          expect(parsed_json["items"].first["preparedStatementParameters"].first["name"]).to eql("a")
          expect(parsed_json["items"].first["queryOnline"]).to eql("SELECT `fg0`.`d` AS `d`, `fg0`.`c` AS `c`, `fg0`.`a` AS `a`, `fg0`.`b` AS `b`\n"+
                                                                   "FROM `#{project_name.downcase}`.`#{fg_name}_1` AS `fg0`\n" +
                                                                   "WHERE `fg0`.`a` = ?")
        end

        it "should be able to get a training dataset serving vector in correct order" do
          # create first feature group
          featurestore_id = get_featurestore_id(@project.id)
          project_name = @project.projectname
          features = [
              {type: "INT", name: "a_testfeature", primary: true},
              {type: "INT", name: "a_testfeature1"},
          ]
          json_result, fg_name = create_cached_featuregroup(@project.id, featurestore_id, features: features, featuregroup_name: "test_fg_a_#{short_random_id}", online:true)
          parsed_json = JSON.parse(json_result)
          fg_id = parsed_json["id"]
          # create second feature group
          features = [
              {type: "INT", name: "a_testfeature", primary: true},
              {type: "INT", name: "b_testfeature1"},
          ]
          json_result_b, fg_name_b = create_cached_featuregroup(@project.id, featurestore_id, features: features, featuregroup_name: "test_fg_b_#{short_random_id}", online:true)
          parsed_json_b = JSON.parse(json_result_b)
          fg_id_b = parsed_json_b["id"]
          # create queryDTO object
          query = {
              leftFeatureGroup: {
                  id: fg_id
              },
              leftFeatures: [{name: 'a_testfeature1'}],
              joins: [{
                       query: {
                           leftFeatureGroup: {
                               id: fg_id_b
                           },
                           leftFeatures: [{name: 'b_testfeature1'}]
                       }
                  }
              ]
          }

          json_result, _ = create_hopsfs_training_dataset(@project.id, featurestore_id, nil, query:query)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          training_dataset_id = parsed_json["id"]
          json_result = get "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/trainingdatasets/#{training_dataset_id}/preparedstatements"
          parsed_json = JSON.parse(json_result)
          expect(parsed_json["items"].first["preparedStatementParameters"].first["index"]).to eql(1)
          expect(parsed_json["items"].first["preparedStatementParameters"].first["name"]).to eql("a_testfeature")
          expect(parsed_json["items"].first["queryOnline"]).to eql("SELECT `fg0`.`a_testfeature1` AS `a_testfeature1`\nFROM `#{project_name.downcase}`.`#{fg_name}_1` AS `fg0`\nWHERE `fg0`.`a_testfeature` = ?")
          expect(parsed_json["items"].second["preparedStatementParameters"].first["index"]).to eql(1)
          expect(parsed_json["items"].second["preparedStatementParameters"].first["name"]).to eql("a_testfeature")
          expect(parsed_json["items"].second["queryOnline"]).to eql("SELECT `fg0`.`b_testfeature1` AS `b_testfeature1`\nFROM `#{project_name.downcase}`.`#{fg_name_b}_1` AS `fg0`\nWHERE `fg0`.`a_testfeature` = ?")
          expect_status(200)
        end

        it "should be able to get a training dataset serving vector in correct order and remove feature group with only primary key and label" do
          # create first feature group
          featurestore_id = get_featurestore_id(@project.id)
          project_name = @project.projectname
          features = [
              {type: "INT", name: "a_testfeature", primary: true},
              {type: "INT", name: "a_testfeature1"},
          ]
          json_result, fg_name = create_cached_featuregroup(@project.id, featurestore_id, features: features, featuregroup_name: "test_fg_a_#{short_random_id}", online:true)
          parsed_json = JSON.parse(json_result)
          fg_id = parsed_json["id"]
          # create second feature group
          features = [
              {type: "INT", name: "a_testfeature", primary: true},
              {type: "INT", name: "b_testfeature1"},
          ]
          json_result_b, fg_name_b = create_cached_featuregroup(@project.id, featurestore_id, features: features, featuregroup_name: "test_fg_b_#{short_random_id}", online:true)
          parsed_json_b = JSON.parse(json_result_b)
          fg_id_b = parsed_json_b["id"]

          # create third feature group
          features = [
              {type: "INT", name: "a_testfeature", primary: true},
              {type: "INT", name: "c_testfeature1",label: true},
          ]
          json_result_c, fg_name_c = create_cached_featuregroup(@project.id, featurestore_id, features: features, featuregroup_name: "test_fg_c_#{short_random_id}", online:true)
          parsed_json_c = JSON.parse(json_result_c)
          fg_id_c = parsed_json_c["id"]
          # create queryDTO object
          query = {
              leftFeatureGroup: {
                  id: fg_id
              },
              leftFeatures: [{name: 'a_testfeature1'}],
              joins: [{
                          query: {
                              leftFeatureGroup: {
                                  id: fg_id_c
                              },
                              leftFeatures: [{name: 'c_testfeature1'}]
                          },
                      },
                      {
                          query: {
                              leftFeatureGroup: {
                                  id: fg_id_b
                              },
                              leftFeatures: [{name: 'b_testfeature1'}]
                          },
                      }
              ]
          }

          td_schema = [
              {type: "INT", name: "a_testfeature1", label: false},
              {type: "INT", name: "b_testfeature1", label: false},
              {type: "INT", name: "c_testfeature1", label: true}
          ]

          json_result, _ = create_hopsfs_training_dataset(@project.id, featurestore_id, nil, query: query, features: td_schema)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)
          training_dataset_id = parsed_json["id"]
          get_prep_statement_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/trainingdatasets/#{training_dataset_id}/preparedstatements?batch=false"
          json_result = get get_prep_statement_endpoint
          parsed_json = JSON.parse(json_result)
          expect(parsed_json["items"].length).to eql(2)
          expect(parsed_json["items"].first["preparedStatementParameters"].first["index"]).to eql(1)
          expect(parsed_json["items"].first["preparedStatementParameters"].first["name"]).to eql("a_testfeature")
          expect(parsed_json["items"].first["queryOnline"]).to eql("SELECT `fg0`.`a_testfeature1` AS `a_testfeature1`\nFROM `#{project_name.downcase}`.`#{fg_name}_1` AS `fg0`\nWHERE `fg0`.`a_testfeature` = ?")
          expect(parsed_json["items"].second["preparedStatementParameters"].first["index"]).to eql(1)
          expect(parsed_json["items"].second["preparedStatementParameters"].first["name"]).to eql("a_testfeature")
          expect(parsed_json["items"].second["queryOnline"]).to eql("SELECT `fg0`.`b_testfeature1` AS `b_testfeature1`\nFROM `#{project_name.downcase}`.`#{fg_name_b}_1` AS `fg0`\nWHERE `fg0`.`a_testfeature` = ?")
          expect_status_details(200)
        end

        it "should add primary keys to the prepared statement in batch mode" do
          # create first feature group
          featurestore_id = get_featurestore_id(@project.id)
          project_name = @project.projectname
          features = [
            {type: "INT", name: "a_testfeature", primary: true},
            {type: "INT", name: "a_testfeature1"},
          ]
          json_result, fg_name = create_cached_featuregroup(@project.id, featurestore_id, features: features, featuregroup_name: "test_fg_a_#{short_random_id}", online:true)
          parsed_json = JSON.parse(json_result)
          fg_id = parsed_json["id"]
          # create second feature group
          features = [
            {type: "INT", name: "b_testfeature", primary: true},
            {type: "INT", name: "b_testfeature1"},
          ]
          json_result_b, fg_name_b = create_cached_featuregroup(@project.id, featurestore_id, features: features, featuregroup_name: "test_fg_b_#{short_random_id}", online:true)
          parsed_json_b = JSON.parse(json_result_b)
          fg_id_b = parsed_json_b["id"]

          # create queryDTO object
          query = {
            leftFeatureGroup: {
              id: fg_id
            },
            leftFeatures: [{name: 'a_testfeature1'}],
            joins: [{
                      query: {
                        leftFeatureGroup: {
                          id: fg_id_b
                        },
                        leftFeatures: [{name: 'b_testfeature1'}]
                      },
                      leftOn: [{name: 'a_testfeature'}],
                      rightOn: [{name: 'b_testfeature'}]
                    }]
          }

          td_schema = [
            {type: "INT", name: "a_testfeature1", label: false},
            {type: "INT", name: "b_testfeature1", label: false},
          ]

          json_result, _ = create_hopsfs_training_dataset(@project.id, featurestore_id, nil, query: query, features: td_schema)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)
          training_dataset_id = parsed_json["id"]
          get_prep_statement_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/trainingdatasets/#{training_dataset_id}/preparedstatements?batch=true"
          json_result = get get_prep_statement_endpoint
          parsed_json = JSON.parse(json_result)
          expect(parsed_json["items"].length).to eql(2)
          expect(parsed_json["items"].first["preparedStatementParameters"].first["index"]).to eql(1)
          expect(parsed_json["items"].first["preparedStatementParameters"].first["name"]).to eql("a_testfeature")
          expect(parsed_json["items"].first["queryOnline"]).to eql("SELECT `fg0`.`a_testfeature1` AS `a_testfeature1`, `fg0`.`a_testfeature` AS `a_testfeature`\nFROM `#{project_name.downcase}`.`#{fg_name}_1` AS `fg0`\nWHERE `fg0`.`a_testfeature` IN ?")
          expect(parsed_json["items"].second["preparedStatementParameters"].first["index"]).to eql(1)
          expect(parsed_json["items"].second["preparedStatementParameters"].first["name"]).to eql("b_testfeature")
          expect(parsed_json["items"].second["queryOnline"]).to eql("SELECT `fg0`.`b_testfeature1` AS `b_testfeature1`, `fg0`.`b_testfeature` AS `b_testfeature`\nFROM `#{project_name.downcase}`.`#{fg_name_b}_1` AS `fg0`\nWHERE `fg0`.`b_testfeature` IN ?")
          expect_status_details(200)
        end

        it "should fail when calling get serving vector from training dataset created from offline fg" do
          # create first feature group
          featurestore_id = get_featurestore_id(@project.id)
          project_name = @project.projectname

          features = [
              {type: "INT", name: "a_testfeature", primary: true},
              {type: "INT", name: "a_testfeature1"},
          ]
          json_result, fg_name_a = create_cached_featuregroup(@project.id, featurestore_id, features: features, featuregroup_name: "test_fg_a_#{short_random_id}", online:true)
          parsed_json = JSON.parse(json_result)
          fg_id = parsed_json["id"]
          # create second feature group
          features = [
              {type: "INT", name: "a_testfeature", primary: true},
              {type: "INT", name: "b_testfeature1"},
          ]
          json_result_b, fg_name_b = create_cached_featuregroup(@project.id, featurestore_id, features: features, featuregroup_name: "test_fg_b_#{short_random_id}", online:false)
          parsed_json_b = JSON.parse(json_result_b)
          fg_id_b = parsed_json_b["id"]
          # create queryDTO object
          query = {
              leftFeatureGroup: {
                  id: fg_id
              },
              leftFeatures: [{name: 'a_testfeature1'}],
              joins: [{
                       query: {
                           leftFeatureGroup: {
                               id: fg_id_b
                           },
                           leftFeatures: [{name: 'b_testfeature1'}]
                       }
                  }
              ]
          }

          json_result, _ = create_hopsfs_training_dataset(@project.id, featurestore_id, nil, query:query)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          training_dataset_id = parsed_json["id"]
          get "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/trainingdatasets/#{training_dataset_id}/preparedstatements"
          expect_status(400)
        end

        it "should fail when calling get serving vector if a feature group was deleted" do
          # create first feature group
          featurestore_id = get_featurestore_id(@project.id)
          project_name = @project.projectname

          features = [
            {type: "INT", name: "a_testfeature", primary: true},
            {type: "INT", name: "a_testfeature1"},
          ]
          json_result, fg_name_a = create_cached_featuregroup(@project.id, featurestore_id, features: features, featuregroup_name: "test_fg_a_#{short_random_id}", online:true)
          parsed_json = JSON.parse(json_result)
          fg_id = parsed_json["id"]
          # create second feature group
          features = [
            {type: "INT", name: "a_testfeature", primary: true},
            {type: "INT", name: "b_testfeature1"},
          ]
          json_result_b, fg_name_b = create_cached_featuregroup(@project.id, featurestore_id, features: features, featuregroup_name: "test_fg_b_#{short_random_id}", online:false)
          parsed_json_b = JSON.parse(json_result_b)
          fg_id_b = parsed_json_b["id"]
          # create queryDTO object
          query = {
            leftFeatureGroup: {
              id: fg_id
            },
            leftFeatures: [{name: 'a_testfeature1'}],
            joins: [{
                      query: {
                        leftFeatureGroup: {
                          id: fg_id_b
                        },
                        leftFeatures: [{name: 'b_testfeature1'}]
                      }
                    }
            ]
          }

          json_result, _ = create_hopsfs_training_dataset(@project.id, featurestore_id, nil, query:query)
          parsed_json = JSON.parse(json_result)
          training_dataset_id = parsed_json["id"]
          expect_status_details(201)

          # delete the second feature group
          delete_featuregroup_checked(@project.id, featurestore_id, fg_id_b)

          get "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/trainingdatasets/#{training_dataset_id}/preparedstatements"
          expect_status_details(400)
        end

        it "should fail when calling get serving vector from training dataset created from fg without primary key" do
          # create feature group without primary key
          featurestore_id = get_featurestore_id(@project.id)
          project_name = @project.projectname
          features = [
              {type: "INT", name: "a_testfeature"},
              {type: "INT", name: "a_testfeature1"},
          ]
          json_result, fg_name = create_cached_featuregroup(@project.id, featurestore_id, features: features, featuregroup_name: "test_fg_a_#{short_random_id}", online:true)
          parsed_json = JSON.parse(json_result)
          fg_id = parsed_json["id"]


          # create queryDTO object
          query = {
              leftFeatureGroup: {
                  id: fg_id
              },
              leftFeatures: [{name: 'a_testfeature1'}],
          }

          json_result, _ = create_hopsfs_training_dataset(@project.id, featurestore_id, nil, query:query)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          training_dataset_id = parsed_json["id"]
          get "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/trainingdatasets/#{training_dataset_id}/preparedstatements"
          expect_status(400)
        end

        it "should be able to create and then delete transformation function" do
          featurestore_id = get_featurestore_id(@project.id)
          json_result = register_transformation_fn(@project.id, featurestore_id)
          parsed_json = JSON.parse(json_result)
          expect_status(200)
          transformation_function_id = parsed_json["id"]
          endpoint = "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/transformationfunctions/#{transformation_function_id}"
          delete endpoint
          expect_status(200)
        end


        it "should be able to get a training dataset serving vector in correct order from stream feature groups" do
          # create first feature group
          featurestore_id = get_featurestore_id(@project.id)
          project_name = @project.projectname
          features = [
              {type: "INT", name: "a_testfeature", primary: true},
              {type: "INT", name: "a_testfeature1"},
          ]
          json_result, fg_name = create_stream_featuregroup(@project.id, featurestore_id, features: features, featuregroup_name: "test_fg_a_#{short_random_id}", backfill_offline: true)
          parsed_json = JSON.parse(json_result)
          fg_id = parsed_json["id"]
          # create second feature group
          features = [
              {type: "INT", name: "a_testfeature", primary: true},
              {type: "INT", name: "b_testfeature1"},
          ]
          json_result_b, fg_name_b = create_stream_featuregroup(@project.id, featurestore_id, features: features, featuregroup_name: "test_fg_b_#{short_random_id}", backfill_offline: true)
          parsed_json_b = JSON.parse(json_result_b)
          fg_id_b = parsed_json_b["id"]
          # create queryDTO object
          query = {
              leftFeatureGroup: {
                  id: fg_id
              },
              leftFeatures: [{name: 'a_testfeature1'}],
              joins: [{
                       query: {
                           leftFeatureGroup: {
                               id: fg_id_b
                           },
                           leftFeatures: [{name: 'b_testfeature1'}]
                       }
                  }
              ]
          }

          json_result, _ = create_hopsfs_training_dataset(@project.id, featurestore_id, nil, query:query)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          training_dataset_id = parsed_json["id"]
          json_result = get "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/trainingdatasets/#{training_dataset_id}/preparedstatements"
          parsed_json = JSON.parse(json_result)
          expect(parsed_json["items"].first["preparedStatementParameters"].first["index"]).to eql(1)
          expect(parsed_json["items"].first["preparedStatementParameters"].first["name"]).to eql("a_testfeature")
          expect(parsed_json["items"].first["queryOnline"]).to eql("SELECT `fg0`.`a_testfeature1` AS `a_testfeature1`\nFROM `#{project_name.downcase}`.`#{fg_name}_1` AS `fg0`\nWHERE `fg0`.`a_testfeature` = ?")
          expect(parsed_json["items"].second["preparedStatementParameters"].first["index"]).to eql(1)
          expect(parsed_json["items"].second["preparedStatementParameters"].first["name"]).to eql("a_testfeature")
          expect(parsed_json["items"].second["queryOnline"]).to eql("SELECT `fg0`.`b_testfeature1` AS `b_testfeature1`\nFROM `#{project_name.downcase}`.`#{fg_name_b}_1` AS `fg0`\nWHERE `fg0`.`a_testfeature` = ?")
          expect_status(200)
        end

        it "should not be able to get a training dataset serving vector containing offline only stream feature group" do
          # create first feature group
          featurestore_id = get_featurestore_id(@project.id)
          project_name = @project.projectname
          features = [
            {type: "INT", name: "a_testfeature", primary: true},
            {type: "INT", name: "a_testfeature1"},
          ]
          json_result, fg_name = create_stream_featuregroup(@project.id, featurestore_id, features: features,
                                                            featuregroup_name: "test_fg_a_#{short_random_id}",
                                                            backfill_offline: true, online_enabled: false)
          parsed_json = JSON.parse(json_result)
          fg_id = parsed_json["id"]
          # create second feature group
          features = [
            {type: "INT", name: "a_testfeature", primary: true},
            {type: "INT", name: "b_testfeature1"},
          ]
          json_result_b, fg_name_b = create_stream_featuregroup(@project.id, featurestore_id, features: features, featuregroup_name: "test_fg_b_#{short_random_id}", backfill_offline: true)
          parsed_json_b = JSON.parse(json_result_b)
          fg_id_b = parsed_json_b["id"]
          # create queryDTO object
          query = {
            leftFeatureGroup: {
              id: fg_id
            },
            leftFeatures: [{name: 'a_testfeature1'}],
            joins: [{
                      query: {
                        leftFeatureGroup: {
                          id: fg_id_b
                        },
                        leftFeatures: [{name: 'b_testfeature1'}]
                      }
                    }
            ]
          }

          json_result, _ = create_hopsfs_training_dataset(@project.id, featurestore_id, nil, query:query)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)
          training_dataset_id = parsed_json["id"]
          json_result = get "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/trainingdatasets/#{training_dataset_id}/preparedstatements"
          parsed_json = JSON.parse(json_result)
          expect_status_details(400)
        end

        it "should be able to get a training dataset serving vector in correct order and remove stream feature group with only primary key and label" do
          # create first feature group
          featurestore_id = get_featurestore_id(@project.id)
          project_name = @project.projectname
          features = [
              {type: "INT", name: "a_testfeature", primary: true},
              {type: "INT", name: "a_testfeature1"},
          ]
          json_result, fg_name = create_stream_featuregroup(@project.id, featurestore_id, features: features, featuregroup_name: "test_fg_a_#{short_random_id}", backfill_offline: true)
          parsed_json = JSON.parse(json_result)
          fg_id = parsed_json["id"]
          # create second feature group
          features = [
              {type: "INT", name: "a_testfeature", primary: true},
              {type: "INT", name: "b_testfeature1"},
          ]
          json_result_b, fg_name_b = create_stream_featuregroup(@project.id, featurestore_id, features: features, featuregroup_name: "test_fg_b_#{short_random_id}", backfill_offline: true)
          parsed_json_b = JSON.parse(json_result_b)
          fg_id_b = parsed_json_b["id"]

          # create third feature group
          features = [
              {type: "INT", name: "a_testfeature", primary: true},
              {type: "INT", name: "c_testfeature1",label: true},
          ]
          json_result_c, fg_name_c = create_stream_featuregroup(@project.id, featurestore_id, features: features, featuregroup_name: "test_fg_c_#{short_random_id}", backfill_offline: true)
          parsed_json_c = JSON.parse(json_result_c)
          fg_id_c = parsed_json_c["id"]
          # create queryDTO object
          query = {
              leftFeatureGroup: {
                  id: fg_id
              },
              leftFeatures: [{name: 'a_testfeature1'}],
              joins: [{
                          query: {
                              leftFeatureGroup: {
                                  id: fg_id_c
                              },
                              leftFeatures: [{name: 'c_testfeature1'}]
                          },
                      },
                      {
                          query: {
                              leftFeatureGroup: {
                                  id: fg_id_b
                              },
                              leftFeatures: [{name: 'b_testfeature1'}]
                          },
                      }
              ]
          }

          td_schema = [
              {type: "INT", name: "a_testfeature1", label: false},
              {type: "INT", name: "b_testfeature1", label: false},
              {type: "INT", name: "c_testfeature1", label: true}
          ]

          json_result, _ = create_hopsfs_training_dataset(@project.id, featurestore_id, nil, query: query, features: td_schema)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)
          training_dataset_id = parsed_json["id"]
          get_prep_statement_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/trainingdatasets/#{training_dataset_id}/preparedstatements?batch=false"
          json_result = get get_prep_statement_endpoint
          parsed_json = JSON.parse(json_result)
          expect(parsed_json["items"].length).to eql(2)
          expect(parsed_json["items"].first["preparedStatementParameters"].first["index"]).to eql(1)
          expect(parsed_json["items"].first["preparedStatementParameters"].first["name"]).to eql("a_testfeature")
          expect(parsed_json["items"].first["queryOnline"]).to eql("SELECT `fg0`.`a_testfeature1` AS `a_testfeature1`\nFROM `#{project_name.downcase}`.`#{fg_name}_1` AS `fg0`\nWHERE `fg0`.`a_testfeature` = ?")
          expect(parsed_json["items"].second["preparedStatementParameters"].first["index"]).to eql(1)
          expect(parsed_json["items"].second["preparedStatementParameters"].first["name"]).to eql("a_testfeature")
          expect(parsed_json["items"].second["queryOnline"]).to eql("SELECT `fg0`.`b_testfeature1` AS `b_testfeature1`\nFROM `#{project_name.downcase}`.`#{fg_name_b}_1` AS `fg0`\nWHERE `fg0`.`a_testfeature` = ?")
          expect_status_details(200)
        end

        it "should be able to retrieve built-in transformation registered during project creation" do
          featurestore_id = get_featurestore_id(@project.id)
          endpoint = "#{ENV['HOPSWORKS_API']}/project/#{@project
          .id}/featurestores/#{featurestore_id}/transformationfunctions"
          json_result = get endpoint
          expect_status_details(200)
          parsed_json = JSON.parse(json_result)

         feature_transformation = parsed_json["items"].select{ |f| f["name"] == "min_max_scaler"}.first
          expect(feature_transformation["name"]).to eql("min_max_scaler")
          expect(feature_transformation["outputType"]).to eql("DoubleType()")

          feature_transformation = parsed_json["items"].select{ |f| f["name"] == "standard_scaler"}.first
          expect(feature_transformation["name"]).to eql("standard_scaler")
          expect(feature_transformation["outputType"]).to eql("DoubleType()")

          feature_transformation = parsed_json["items"].select{ |f| f["name"] == "robust_scaler"}.first
          expect(feature_transformation["name"]).to eql("robust_scaler")
          expect(feature_transformation["outputType"]).to eql("DoubleType()")

          feature_transformation = parsed_json["items"].select{ |f| f["name"] == "label_encoder"}.first
          expect(feature_transformation["name"]).to eql("label_encoder")
          expect(feature_transformation["outputType"]).to eql("IntegerType()")
        end


        it "should be able to attach transformation function to training_dataset" do
          # featurestore id
          featurestore_id = get_featurestore_id(@project.id)

          # create transformation function
          json_result = register_transformation_fn(@project.id, featurestore_id)
          transformation_function = JSON.parse(json_result)
          expect_status(200)

          # create first feature group
          project_name = @project.projectname
          features = [
              {type: "INT", name: "a_testfeature", primary: true},
              {type: "INT", name: "a_testfeature1"},
          ]
          json_result, fg_name = create_cached_featuregroup(@project.id, featurestore_id, features: features, featuregroup_name: "test_fg_a_#{short_random_id}", online:true)
          parsed_json = JSON.parse(json_result)
          fg_id = parsed_json["id"]
          # create second feature group
          features = [
              {type: "INT", name: "a_testfeature", primary: true},
              {type: "INT", name: "b_testfeature1"},
          ]
          json_result_b, fg_name_b = create_cached_featuregroup(@project.id, featurestore_id, features: features, featuregroup_name: "test_fg_b_#{short_random_id}", online:true)
          parsed_json_b = JSON.parse(json_result_b)
          fg_id_b = parsed_json_b["id"]

          # create queryDTO object
          query = {
              leftFeatureGroup: {
                  id: fg_id
              },
              leftFeatures: [{name: 'a_testfeature1'}],
              joins: [{
                          query: {
                              leftFeatureGroup: {
                                  id: fg_id_b
                              },
                              leftFeatures: [{name: 'b_testfeature1'}]
                          },
                      }
              ]
          }

          td_schema = [
              {type: "INT", name: "a_testfeature1", featureGroupFeatureName: "a_testfeature1", label: false, transformationFunction: transformation_function},
              {type: "INT", name: "b_testfeature1", featureGroupFeatureName: "b_testfeature1", label: false, transformationFunction: transformation_function}
          ]

          json_result, training_dataset_name = create_hopsfs_training_dataset(@project.id, featurestore_id, nil, query: query, features: td_schema)
          training_dataset =  JSON.parse(json_result)
          training_dataset_id = training_dataset["id"]

          # endpoint to attach transformation functions
          endpoint = "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/trainingdatasets/#{training_dataset_id}/transformationfunctions"
          json_result =  get endpoint
          parsed_json = JSON.parse(json_result)
          feature_transformation = parsed_json["items"].first["transformationFunction"]
          expect(feature_transformation["name"]).to eql("plus_one")
          expect(feature_transformation["outputType"]).to eql("FloatType()")

          feature_transformation = parsed_json["items"].second["transformationFunction"]
          expect(feature_transformation["name"]).to eql("plus_one")
          expect(feature_transformation["outputType"]).to eql("FloatType()")
        end

        it "should be able to create training dataset with prefixed join" do
          # create first feature group
          featurestore_id = get_featurestore_id(@project.id)
          project_name = @project.projectname
          features = [
              {type: "INT", name: "a_testfeature", primary: true},
              {type: "INT", name: "a_testfeature1"},
          ]
          json_result, fg_name_a = create_cached_featuregroup(@project.id, featurestore_id, features: features, featuregroup_name: "test_fg_a_#{short_random_id}", online:true)
          parsed_json = JSON.parse(json_result)
          fg_id = parsed_json["id"]
          # create second feature group
          features = [
              {type: "INT", name: "a_testfeature", primary: true},
              {type: "INT", name: "b_testfeature1"},
          ]
          json_result_b, fg_name_b = create_cached_featuregroup(@project.id, featurestore_id, features: features, featuregroup_name: "test_fg_b_#{short_random_id}", online:true)
          parsed_json_b = JSON.parse(json_result_b)
          fg_id_b = parsed_json_b["id"]

          # create third feature group
          features = [
              {type: "INT", name: "a_testfeature", primary: true},
              {type: "INT", name: "c_testfeature1",label: true},
          ]
          json_result_c, fg_name_c = create_cached_featuregroup(@project.id, featurestore_id, features: features, featuregroup_name: "test_fg_c_#{short_random_id}", online:true)
          parsed_json_c = JSON.parse(json_result_c)
          fg_id_c = parsed_json_c["id"]
          # create queryDTO object
          query = {
              leftFeatureGroup: {
                  id: fg_id
              },
              leftFeatures: [{name: 'a_testfeature1'}],
              joins: [{
                          query: {
                              leftFeatureGroup: {
                                  id: fg_id_c
                              },
                              leftFeatures: [{name: 'c_testfeature1'}]
                          },
                          prefix: "prefix_c_"
                      },
                      {
                          query: {
                              leftFeatureGroup: {
                                  id: fg_id_b
                              },
                              leftFeatures: [{name: 'b_testfeature1'}]
                          },
                          prefix: "prefix_b_"
                      }
              ]
          }

          td_schema = [
              {type: "INT", name: "a_testfeature1", label: false},
              {type: "INT", name: "b_testfeature1", label: false},
              {type: "INT", name: "c_testfeature1", label: true}
          ]

          json_result, _ = create_hopsfs_training_dataset(@project.id, featurestore_id, nil, query: query, features: td_schema)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)
          training_dataset_id = parsed_json["id"]
          json_result = get "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/trainingdatasets/#{training_dataset_id}/query"
          expect_status_details(200)
          query = JSON.parse(json_result)

          expect(query['query']).to eql("SELECT `fg0`.`a_testfeature1` `a_testfeature1`, `fg1`.`c_testfeature1` `prefix_c_c_testfeature1`, `fg2`.`b_testfeature1` `prefix_b_b_testfeature1`\nFROM `#{project_name.downcase}_featurestore`.`#{fg_name_a}_1` `fg0`\nINNER JOIN `#{project_name.downcase}_featurestore`.`#{fg_name_c}_1` `fg1` ON `fg0`.`a_testfeature` = `fg1`.`a_testfeature`\nINNER JOIN `#{project_name.downcase}_featurestore`.`#{fg_name_b}_1` `fg2` ON `fg0`.`a_testfeature` = `fg2`.`a_testfeature`")
          expect(query['queryOnline']).to eql("SELECT `fg0`.`a_testfeature1` `a_testfeature1`, `fg1`.`c_testfeature1` `prefix_c_c_testfeature1`, `fg2`.`b_testfeature1` `prefix_b_b_testfeature1`\nFROM `#{project_name.downcase}`.`#{fg_name_a}_1` `fg0`\nINNER JOIN `#{project_name.downcase}`.`#{fg_name_c}_1` `fg1` ON `fg0`.`a_testfeature` = `fg1`.`a_testfeature`\nINNER JOIN `#{project_name.downcase}`.`#{fg_name_b}_1` `fg2` ON `fg0`.`a_testfeature` = `fg2`.`a_testfeature`")
        end

        it "should be able to create training dataset with PIT join and reproduce the PIT query" do
          # create first feature group
          featurestore_id = get_featurestore_id(@project.id)
          project_name = @project.projectname
          features = [
            {type: "INT", name: "a_testfeature", primary: true},
            {type: "INT", name: "a_testfeature1"},
            {type: "BIGINT", name: "event_time"}
          ]
          json_result, fg_name_a = create_cached_featuregroup(@project.id, featurestore_id, features: features,
                                                              featuregroup_name: "test_fg_a_#{short_random_id}",
                                                              online:true, event_time: "event_time")
          parsed_json = JSON.parse(json_result)
          fg_id = parsed_json["id"]
          # create second feature group
          features = [
            {type: "INT", name: "a_testfeature", primary: true},
            {type: "INT", name: "b_testfeature1"},
            {type: "BIGINT", name: "event_time"}
          ]
          json_result_b, fg_name_b = create_cached_featuregroup(@project.id, featurestore_id, features: features,
                                                                featuregroup_name: "test_fg_b_#{short_random_id}",
                                                                online:true, event_time: "event_time")
          parsed_json_b = JSON.parse(json_result_b)
          fg_id_b = parsed_json_b["id"]

          # create third feature group
          features = [
            {type: "INT", name: "a_testfeature", primary: true},
            {type: "INT", name: "c_testfeature1",label: true},
            {type: "BIGINT", name: "event_time"}
          ]
          json_result_c, fg_name_c = create_cached_featuregroup(@project.id, featurestore_id, features: features,
                                                                featuregroup_name: "test_fg_c_#{short_random_id}",
                                                                online:true, event_time: "event_time")
          parsed_json_c = JSON.parse(json_result_c)
          fg_id_c = parsed_json_c["id"]
          # create queryDTO object
          query = {
            leftFeatureGroup: {
              id: fg_id,
              eventTime: "event_time"
            },
            leftFeatures: [{name: 'a_testfeature1'}, {type: "INT", name: "event_time"}],
            joins: [{
                      query: {
                        leftFeatureGroup: {
                          id: fg_id_c,
                          eventTime: "event_time"
                        },
                        leftFeatures: [{name: 'c_testfeature1'}]
                      },
                      prefix: "prefix_c_"
                    },
                    {
                      query: {
                        leftFeatureGroup: {
                          id: fg_id_b,
                          eventTime: "event_time"
                        },
                        leftFeatures: [{name: 'b_testfeature1'}]
                      },
                      prefix: "prefix_b_"
                    }
            ]
          }

          td_schema = [
            {type: "INT", name: "a_testfeature1", label: false},
            {type: "INT", name: "b_testfeature1", label: false},
            {type: "INT", name: "c_testfeature1", label: true}
          ]

          json_result, _ = create_hopsfs_training_dataset(@project.id, featurestore_id, nil, query: query, features: td_schema)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)
          training_dataset_id = parsed_json["id"]
          json_result = get "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/trainingdatasets/#{training_dataset_id}/query"
          expect_status_details(200)
          query = JSON.parse(json_result)

          expect(query['query']).to eql("SELECT `fg0`.`a_testfeature1` `a_testfeature1`, `fg0`.`event_time` `event_time`, `fg1`.`c_testfeature1` `prefix_c_c_testfeature1`, `fg2`.`b_testfeature1` `prefix_b_b_testfeature1`\nFROM `#{project_name.downcase}_featurestore`.`#{fg_name_a}_1` `fg0`\nINNER JOIN `#{project_name.downcase}_featurestore`.`#{fg_name_c}_1` `fg1` ON `fg0`.`a_testfeature` = `fg1`.`a_testfeature`\nINNER JOIN `#{project_name.downcase}_featurestore`.`#{fg_name_b}_1` `fg2` ON `fg0`.`a_testfeature` = `fg2`.`a_testfeature`")
          expect(query['queryOnline']).to eql("SELECT `fg0`.`a_testfeature1` `a_testfeature1`, `fg0`.`event_time` `event_time`, `fg1`.`c_testfeature1` `prefix_c_c_testfeature1`, `fg2`.`b_testfeature1` `prefix_b_b_testfeature1`\nFROM `#{project_name.downcase}`.`#{fg_name_a}_1` `fg0`\nINNER JOIN `#{project_name.downcase}`.`#{fg_name_c}_1` `fg1` ON `fg0`.`a_testfeature` = `fg1`.`a_testfeature`\nINNER JOIN `#{project_name.downcase}`.`#{fg_name_b}_1` `fg2` ON `fg0`.`a_testfeature` = `fg2`.`a_testfeature`")
          expect(query['pitQuery']).to eql("WITH right_fg0 AS (SELECT *\nFROM (SELECT `fg0`.`a_testfeature1` `a_testfeature1`, `fg0`.`event_time` `event_time`, `fg1`.`c_testfeature1` `prefix_c_c_testfeature1`, `fg0`.`a_testfeature` `join_pk_a_testfeature`, `fg0`.`event_time` `join_evt_event_time`, RANK() OVER (PARTITION BY `fg0`.`a_testfeature`, `fg0`.`event_time` ORDER BY `fg1`.`event_time` DESC) pit_rank_hopsworks\nFROM `#{project_name.downcase}_featurestore`.`#{fg_name_a}_1` `fg0`\nINNER JOIN `#{project_name.downcase}_featurestore`.`#{fg_name_c}_1` `fg1` ON `fg0`.`a_testfeature` = `fg1`.`a_testfeature` AND `fg0`.`event_time` >= `fg1`.`event_time`) NA\nWHERE `pit_rank_hopsworks` = 1), right_fg1 AS (SELECT *\nFROM (SELECT `fg0`.`a_testfeature1` `a_testfeature1`, `fg0`.`event_time` `event_time`, `fg2`.`b_testfeature1` `prefix_b_b_testfeature1`, `fg0`.`a_testfeature` `join_pk_a_testfeature`, `fg0`.`event_time` `join_evt_event_time`, RANK() OVER (PARTITION BY `fg0`.`a_testfeature`, `fg0`.`event_time` ORDER BY `fg2`.`event_time` DESC) pit_rank_hopsworks\nFROM `#{project_name.downcase}_featurestore`.`#{fg_name_a}_1` `fg0`\nINNER JOIN `#{project_name.downcase}_featurestore`.`#{fg_name_b}_1` `fg2` ON `fg0`.`a_testfeature` = `fg2`.`a_testfeature` AND `fg0`.`event_time` >= `fg2`.`event_time`) NA\nWHERE `pit_rank_hopsworks` = 1) (SELECT `right_fg0`.`a_testfeature1` `a_testfeature1`, `right_fg0`.`event_time` `event_time`, `right_fg0`.`prefix_c_c_testfeature1` `prefix_c_c_testfeature1`, `right_fg1`.`prefix_b_b_testfeature1` `prefix_b_b_testfeature1`\nFROM right_fg0\nINNER JOIN right_fg1 ON `right_fg0`.`join_pk_a_testfeature` = `right_fg1`.`join_pk_a_testfeature` AND `right_fg0`.`join_evt_event_time` = `right_fg1`.`join_evt_event_time`)")
        end

        it "should be able to create and retrieve query with filter" do
                  # create first feature group
                  featurestore_id = get_featurestore_id(@project.id)
                  project_name = @project.projectname
                  features = [
                      {type: "INT", name: "k_testfeature", primary: true},
                      {type: "INT", name: "a_testfeature"},
                  ]
                  json_result, fg_name_a = create_cached_featuregroup(@project.id, featurestore_id, features:
                  features, featuregroup_name: "test_fg_a_#{short_random_id}", online:true)
                  parsed_json = JSON.parse(json_result)
                  fg_id = parsed_json["id"]
                  # create second feature group
                  features = [
                      {type: "INT", name: "k_testfeature", primary: true},
                      {type: "INT", name: "b_testfeature"},
                  ]
                  json_result_b, fg_name_b = create_cached_featuregroup(@project.id, featurestore_id, features:
                  features, featuregroup_name: "test_fg_b_#{short_random_id}", online:true)
                  parsed_json_b = JSON.parse(json_result_b)
                  fg_id_b = parsed_json_b["id"]

                  # create third feature group
                  features = [
                      {type: "INT", name: "k_testfeature", primary: true},
                      {type: "INT", name: "c_testfeature"},
                  ]
                  json_result_c, fg_name_c = create_cached_featuregroup(@project.id, featurestore_id, features:
                  features, featuregroup_name: "test_fg_c_#{short_random_id}", online:true)
                  parsed_json_c = JSON.parse(json_result_c)
                  fg_id_c = parsed_json_c["id"]
                  # create queryDTO object
                  query = {
                    leftFeatureGroup: {
                        id: fg_id
                    },
                    leftFeatures: [{name: 'a_testfeature'}],
                    joins: [{
                                query: {
                                    leftFeatureGroup: {
                                        id: fg_id_b
                                    },
                                    leftFeatures: [{name: 'b_testfeature'}]
                                }
                            },
                            {
                                query: {
                                    leftFeatureGroup: {
                                        id: fg_id_c
                                    },
                                    leftFeatures: [{name: 'c_testfeature'}]
                                }
                            }
                    ],
                    filter: {
                      type: 'AND',
                      leftFilter: {
                        feature: {name: 'a_testfeature', featureGroupId: fg_id},
                        condition: 'GREATER_THAN',
                        value: '2'
                      },
                      rightLogic: {
                        type: 'OR',
                        leftFilter: {
                          feature: {name: 'b_testfeature', featureGroupId: fg_id_b},
                          condition: 'LESS_THAN',
                          value: '3'
                        },
                        rightFilter: {
                          feature: {name: 'c_testfeature', featureGroupId: fg_id_c},
                          condition: 'GREATER_THAN_OR_EQUAL',
                          value: "{\"name\": \"b_testfeature\", \"type\": \"int\", \"description\": null, \"partition\": true,
\"hudiPrecombineKey\": false, \"primary\": false, \"onlineType\": null, \"defaultValue\": null, \"featureGroupId\":
#{fg_id_b}}"
                        }
                      }
                    }
                  }

                  td_schema = [
                      {type: "INT", name: "a_testfeature", label: false},
                      {type: "INT", name: "b_testfeature", label: false},
                      {type: "INT", name: "c_testfeature", label: true}
                  ]

                  json_result, _ = create_hopsfs_training_dataset(@project.id, featurestore_id, nil, query: query, features: td_schema)
                  parsed_json = JSON.parse(json_result)
                  expect_status_details(201)
                  training_dataset_id = parsed_json["id"]
                  json_result = get "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/trainingdatasets/#{training_dataset_id}/query"
                  expect_status_details(200)
                  query = JSON.parse(json_result)

                  expect(query['query']).to eql("SELECT `fg0`.`a_testfeature` `a_testfeature`, `fg1`.`b_testfeature` `b_testfeature`, `fg2`.`c_testfeature` `c_testfeature`\nFROM `#{project_name.downcase}_featurestore`.`#{fg_name_a}_1` `fg0`\nINNER JOIN `#{project_name.downcase}_featurestore`.`#{fg_name_b}_1` `fg1` ON `fg0`.`k_testfeature` = `fg1`.`k_testfeature`\nINNER JOIN `#{project_name.downcase}_featurestore`.`#{fg_name_c}_1` `fg2` ON `fg0`.`k_testfeature` = `fg2`.`k_testfeature`\nWHERE `fg0`.`a_testfeature` > 2 AND (`fg1`.`b_testfeature` < 3 OR `fg2`.`c_testfeature` >= `fg1`.`b_testfeature`)")
                  expect(query['queryOnline']).to eql("SELECT `fg0`.`a_testfeature` `a_testfeature`, `fg1`.`b_testfeature` `b_testfeature`, `fg2`.`c_testfeature` `c_testfeature`\nFROM `#{project_name.downcase}`.`#{fg_name_a}_1` `fg0`\nINNER JOIN `#{project_name.downcase}`.`#{fg_name_b}_1` `fg1` ON `fg0`.`k_testfeature` = `fg1`.`k_testfeature`\nINNER JOIN `#{project_name.downcase}`.`#{fg_name_c}_1` `fg2` ON `fg0`.`k_testfeature` = `fg2`.`k_testfeature`\nWHERE `fg0`.`a_testfeature` > 2 AND (`fg1`.`b_testfeature` < 3 OR `fg2`.`c_testfeature` >= `fg1`.`b_testfeature`)")
        end
      end
    end
  end
end
