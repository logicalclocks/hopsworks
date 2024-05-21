# This file is part of Hopsworks
# Copyright (C) 2022, Logical Clocks AB. All rights reserved
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
  after(:all) {clean_all_test_projects(spec: "featureview")}

  describe "feature view" do
    describe "internal" do
      context 'with valid project, featurestore service enabled' do
        before :all do
          with_valid_project
        end

        it "should be able to add a feature view to the featurestore" do
          featurestore_id = get_featurestore_id(@project.id)

          json_result, fg_name = create_cached_featuregroup(@project.id, featurestore_id, online:true)
          parsed_json = JSON.parse(json_result)

          json_result = create_feature_view_from_feature_group(@project.id, featurestore_id, parsed_json)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          expect(parsed_json.key?("id")).to be true
		      expect(parsed_json.key?("featurestoreId")).to be true
          expect(parsed_json.key?("featurestoreName")).to be true
          expect(parsed_json.key?("name")).to be true
		      expect(parsed_json.key?("description")).to be true
		      expect(parsed_json.key?("created")).to be true
          expect(parsed_json["creator"].key?("email")).to be true
          expect(parsed_json.key?("version")).to be true
          expect(parsed_json.key?("query")).to be true
          expect(parsed_json.key?("features")).to be true

          expect(parsed_json["featurestoreId"] == featurestore_id).to be true
          expect(parsed_json["description"] == "testfeatureviewdescription").to be true
		      expect(parsed_json["version"] == 1).to be true

          fv_features = parsed_json['features']
          expect(fv_features.length).to be 1

          expect(fv_features.select{|feature| feature['index'] == 0}[0]['name']).to eql("testfeature")
          expect(fv_features.select{|feature| feature['index'] == 0}[0]['type']).to eql("int")
          expect(fv_features.select{|feature| feature['index'] == 0}[0]['label']).to be false
        end

        it "should not be able to add a feature view to the featurestore with upper case characters" do
          featurestore_id = get_featurestore_id(@project.id)

          json_result, fg_name = create_cached_featuregroup(@project.id, featurestore_id, online:true)
          parsed_json = JSON.parse(json_result)

          json_result = create_feature_view_from_feature_group(@project.id, featurestore_id, parsed_json,
                                                                name:"TEST_feature_view")
          parsed_json = JSON.parse(json_result)
          expect_status_details(400)

          expect(parsed_json.key?("errorCode")).to be true
          expect(parsed_json.key?("errorMsg")).to be true
          expect(parsed_json.key?("usrMsg")).to be true
          expect(parsed_json["errorCode"] == 270091).to be true
        end

        it "should not be able to add a feature view to the featurestore with an invalid version" do
          featurestore_id = get_featurestore_id(@project.id)

          json_result, fg_name = create_cached_featuregroup(@project.id, featurestore_id, online:true)
          parsed_json = JSON.parse(json_result)

          json_result = create_feature_view_from_feature_group(@project.id, featurestore_id, parsed_json, version:-1)
          parsed_json = JSON.parse(json_result)
          expect_status_details(400)

          expect(parsed_json.key?("errorCode")).to be true
          expect(parsed_json.key?("errorMsg")).to be true
          expect(parsed_json.key?("usrMsg")).to be true
          expect(parsed_json["errorCode"] == 270058).to be true
        end

        it "should be able to add a feature view without version to the featurestore" do
          featurestore_id = get_featurestore_id(@project.id)

          json_result, fg_name = create_cached_featuregroup(@project.id, featurestore_id, online:true)
          parsed_json = JSON.parse(json_result)

          json_result = create_feature_view_from_feature_group(@project.id, featurestore_id, parsed_json,
                                                                  version:nil)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)
          expect(parsed_json["version"] == 1).to be true
        end

        it "should be able to add a new version of an existing feature view without version to the featurestore" do
          featurestore_id = get_featurestore_id(@project.id)

          json_result, fg_name = create_cached_featuregroup(@project.id, featurestore_id, online:true)
          parsed_fg_json = JSON.parse(json_result)

          json_result = create_feature_view_from_feature_group(@project.id, featurestore_id, parsed_fg_json)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          # add second version
          json_result = create_feature_view_from_feature_group(@project.id, featurestore_id, parsed_fg_json, name:parsed_json["name"], version:nil)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)
          # version should be incremented to 2
          expect(parsed_json["version"] == 2).to be true
        end

        it "should not be able to create a feature view with the same name and version" do
          featurestore_id = get_featurestore_id(@project.id)

          json_result, fg_name = create_cached_featuregroup(@project.id, featurestore_id, online:true)
          parsed_fg_json = JSON.parse(json_result)

          json_result = create_feature_view_from_feature_group(@project.id, featurestore_id, parsed_fg_json)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          json_result = create_feature_view_from_feature_group(@project.id, featurestore_id, parsed_fg_json, name:parsed_json["name"], version:parsed_json["version"])
          expect_status_details(400)
        end

        it "should be able to delete a feature view from the featurestore" do
          featurestore_id = get_featurestore_id(@project.id)

          json_result, fg_name = create_cached_featuregroup(@project.id, featurestore_id, online:true)
          parsed_json = JSON.parse(json_result)

          json_result = create_feature_view_from_feature_group(@project.id, featurestore_id, parsed_json)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          # Make sure that the directory exists
          get_datasets_in_path(@project,
                               "#{@project[:projectname]}_Training_Datasets/.featureviews/#{parsed_json['name']}_#{parsed_json['version']}",
                               query: "&type=DATASET")
          expect_status_details(200)

          delete_feature_view(@project.id, parsed_json["name"], feature_store_id: featurestore_id)
          expect_status_details(200)
        end

        it "should be able to delete a feature view and all training data from the featurestore" do
          all_metadata = create_featureview_training_dataset_from_project(@project)
          parsed_json = all_metadata["response"]
          featureview = all_metadata["featureView"]
          connector = all_metadata["connector"]
          s3_connector = make_connector_dto(get_s3_connector_id)
          create_featureview_training_dataset(@project.id, featureview, connector, version: nil,
                                              td_type: "IN_MEMORY_TRAINING_DATASET")
          create_featureview_training_dataset(@project.id, featureview, s3_connector, version: nil,
                                              is_internal: false)
          # Make sure that the directory exists
          get_datasets_in_path(@project,
                               "#{@project[:projectname]}_Training_Datasets/.featureviews/#{featureview['name']}_#{featureview['version']}",
                               query: "&type=DATASET")
          expect_status_details(200)
          # Make sure that training data directory exists
          get_datasets_in_path(@project,
                               "#{@project[:projectname]}_Training_Datasets/#{featureview['name']}_#{featureview['version']}_#{parsed_json['version']}",
                               query: "&type=DATASET")
          expect_status_details(200)

          delete_feature_view(@project.id, featureview["name"])
          expect_status_details(200)

          # Make sure that the directory get deleted
          get_datasets_in_path(@project,
                               "#{@project[:projectname]}_Training_Datasets/.featureviews/#{featureview['name']}_#{featureview['version']}",
                               query: "&type=DATASET")
          expect_status_details(400)
          # Make sure that training data directory get deleted
          get_datasets_in_path(@project,
                               "#{@project[:projectname]}_Training_Datasets/#{featureview['name']}_#{featureview['version']}_#{parsed_json['version']}",
                               query: "&type=DATASET")
          expect_status_details(400)

        end

        it "should not be able to update feature view that doesnt exist" do
          featurestore_id = get_featurestore_id(@project.id)

          json_data = {
            name: "feature_view_name",
            version: 1,
            description: "testfeatureviewdescription",
            type: "featureViewDTO"
          }

          update_feature_view(@project.id, featurestore_id, json_data, "feature_view_name", 1)
          expect_status_details(404)
        end

        it "should be able to update the description of a feature view" do
          featurestore_id = get_featurestore_id(@project.id)

          json_result, fg_name = create_cached_featuregroup(@project.id, featurestore_id, online:true)
          parsed_json = JSON.parse(json_result)

          json_result = create_feature_view_from_feature_group(@project.id, featurestore_id, parsed_json)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)
          featureview_name = parsed_json["name"]
          featureview_version = parsed_json["version"]

          new_description = "new_testfeatureviewdescription"
          json_data = {
            name: featureview_name,
            version: featureview_version,
            description: new_description,
            type: "featureViewDTO"
          }

          json_result = update_feature_view(@project.id, featurestore_id, json_data, featureview_name, featureview_version)
          parsed_json = JSON.parse(json_result)
          expect_status_details(200)

          expect(parsed_json["description"]).to eql(new_description)
        end

        it "should be able to update the name of a feature view" do
          featurestore_id = get_featurestore_id(@project.id)

          json_result, fg_name = create_cached_featuregroup(@project.id, featurestore_id, online:true)
          parsed_json = JSON.parse(json_result)

          json_result = create_feature_view_from_feature_group(@project.id, featurestore_id, parsed_json)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)
          featureview_name = parsed_json["name"]
          featureview_version = parsed_json["version"]

          json_data = {
            name: "new_testfeatureviewname",
			      version: featureview_version,
            type: "featureViewDTO"
          }

          json_result = update_feature_view(@project.id, featurestore_id, json_data, featureview_name, featureview_version)
          parsed_json = JSON.parse(json_result)
          expect_status_details(200)

          expect(parsed_json["name"]).to eql(featureview_name)
        end

        it "should be able to get a feature view based on name and version" do
          featurestore_id = get_featurestore_id(@project.id)

          json_result, fg_name = create_cached_featuregroup(@project.id, featurestore_id, online:true)
          parsed_fg_json = JSON.parse(json_result)

          json_result = create_feature_view_from_feature_group(@project.id, featurestore_id, parsed_fg_json)
          parsed_json_1 = JSON.parse(json_result)
          expect_status_details(201)
		      featureview_name = parsed_json_1["name"]

          # add second version
          json_result = create_feature_view_from_feature_group(@project.id, featurestore_id, parsed_fg_json, name:featureview_name, version:nil)
          parsed_json_2 = JSON.parse(json_result)
          expect_status_details(201)

          # Get the first version
          json_result = get_feature_view_by_name_and_version(@project.id, featurestore_id, parsed_json_1["name"], parsed_json_1["version"])
          parsed_json = JSON.parse(json_result)
          expect_status_details(200)
          expect(parsed_json['version']).to be 1
          expect(parsed_json['name']).to eq featureview_name

          json_result = get_feature_view_by_name_and_version(@project.id, featurestore_id, parsed_json_2["name"], parsed_json_2["version"])
          parsed_json = JSON.parse(json_result)
          expect_status_details(200)
          expect(parsed_json['version']).to be 2
          expect(parsed_json['name']).to eq featureview_name
        end

        it "should fail to get a feature view with a name that does not exists" do
          featurestore_id = get_featurestore_id(@project.id)
		      json_result = get_feature_view_by_name(@project.id, featurestore_id, "doesnotexists")
          expect_status_details(404)
        end

        it "should fail to create a feature view with no features and no query" do
          featurestore_id = get_featurestore_id(@project.id)
          json_data = {
              name: "no_features_no_query",
              version: 1,
              type: "featureViewDTO"
          }
          create_feature_view_with_json(@project.id, featurestore_id, json_data)
          expect_status_details(400)
        end

        it "should not be able to create a feature view from a query object without features" do
          # create feature group
          featurestore_id = get_featurestore_id(@project.id)
          features = [
              {type: "INT", name: "testfeature", primary: true},
              {type: "INT", name: "testfeature1"},
          ]
          fg = create_cached_featuregroup_checked_return_fg(@project.id, featurestore_id, "test_fg_#{short_random_id}",
                                                            features: features)
          # create queryDTO object
          query = {
              leftFeatureGroup: {
                id: fg[:id],
                type: fg[:type],
              },
              leftFeatures: []
          }
          json_result = create_feature_view(@project.id, featurestore_id, query)
          expect_status_details(400)
        end

        it "should not be able to create a feature view from a query object that joins query without features" do
          # create feature group
          featurestore_id = get_featurestore_id(@project.id)
          features = [
              {type: "INT", name: "testfeature", primary: true},
              {type: "INT", name: "testfeature1"},
          ]
          fg = create_cached_featuregroup_checked_return_fg(@project.id, featurestore_id, "test_fg_#{short_random_id}",
                                                            features: features)
          # create queryDTO object
          query = {
              leftFeatureGroup: {
                id: fg[:id],
                type: fg[:type],
              },
              leftFeatures: [{name: 'a_testfeature'}, {name: 'a_testfeature1'}],
              joins: [
                {
                  leftFeatureGroup: {
                    id: fg[:id],
                    type: fg[:type],
                  },
                  leftFeatures: []
                }
              ]
          }
          json_result = create_feature_view(@project.id, featurestore_id, query)
          expect_status_details(400)
        end
		
        it "should be able to create a feature view from a query object - 1" do
          # create feature group
          featurestore_id = get_featurestore_id(@project.id)
          features = [
              {type: "INT", name: "testfeature", primary: true},
              {type: "INT", name: "testfeature1"},
          ]
          fg = create_cached_featuregroup_checked_return_fg(@project.id, featurestore_id, "test_fg_#{short_random_id}",
                                                   features: features)
          # create queryDTO object
          query = {
              leftFeatureGroup: {
                id: fg[:id],
                type: fg[:type],
              },
              leftFeatures: [{name: 'testfeature'}, {name: 'testfeature1'}]
          }
          json_result = create_feature_view(@project.id, featurestore_id, query)
          expect_status_details(201)
          feature_view = JSON.parse(json_result)

          expect(feature_view.key?('query')).to be true
          td_features = feature_view['features']
          expect(td_features.count).to eql(2)
          expect(td_features.select{|feature| feature['index'] == 0}[0]['name']).to eql("testfeature")
          expect(td_features.select{|feature| feature['index'] == 0}[0]['featuregroup']['id']).to eql(fg[:id])
          expect(td_features.select{|feature| feature['index'] == 1}[0]['name']).to eql("testfeature1")
          expect(td_features.select{|feature| feature['index'] == 1}[0]['featuregroup']['id']).to eql(fg[:id])
        end

        it "should be able to create a feature view from a query object with label" do
          # create feature group
          featurestore_id = get_featurestore_id(@project.id)
          features = [
              {type: "INT", name: "testfeature", primary: true},
              {type: "INT", name: "testfeature1"},
          ]
          fg = create_cached_featuregroup_checked_return_fg(@project.id, featurestore_id, "test_fg_#{short_random_id}",
                                                   features: features)
          # create queryDTO object
          query = {
              leftFeatureGroup: {
                id: fg[:id],
                type: fg[:type],
              },
              leftFeatures: [{name: 'testfeature'}, {name: 'testfeature1'}]
          }
          features = [
              {type: "INT", name: "testfeature", label: true},
              {type: "INT", name: "testfeature1", label: false},
          ]
          json_result = create_feature_view(@project.id, featurestore_id, query, features: features)
          expect_status_details(201)
          feature_view = JSON.parse(json_result)

          expect(feature_view.key?('query')).to be true
          td_features = feature_view['features']
          expect(td_features.count).to eql(2)
          expect(td_features.select{|feature| feature['index'] == 0}[0]['label']).to be true
          expect(td_features.select{|feature| feature['index'] == 1}[0]['label']).to be false
        end

        it "should be able to create a feature view from a query object with label containing feature group" do
          # create feature group
          featurestore_id = get_featurestore_id(@project.id)
          features = [
            {type: "INT", name: "testfeature", primary: true},
            {type: "INT", name: "testfeature1"},
          ]
          fg = create_cached_featuregroup_checked_return_fg(@project.id, featurestore_id, "test_fg_#{short_random_id}",
                                                            features: features)
          fg1 = create_cached_featuregroup_checked_return_fg(@project.id, featurestore_id, "test_fg_#{short_random_id}",
                                                            features: features)
          # create queryDTO object
          query = {
            leftFeatureGroup: {
              id: fg[:id],
              type: fg[:type],
            },
            leftFeatures: [{name: 'testfeature', featureGroupId: fg[:id]}],
            joins: [{
                      query: {
                        leftFeatureGroup: {
                          id: fg1[:id],
                          type: fg1[:type],
                        },
                        leftFeatures: [{ name: 'testfeature', featureGroupId: fg1[:id]}]
                      },
                      prefix: "fg1_"
                    }
            ]
          }
          features = [
            {type: "INT", name: "testfeature", featuregroup: fg1, label: true}
          ]
          json_result = create_feature_view(@project.id, featurestore_id, query, features: features)
          expect_status_details(201)
          feature_view = JSON.parse(json_result)

          expect(feature_view.key?('query')).to be true
          td_features = feature_view['features']
          expect(td_features.count).to eql(2)
          expect(td_features.select{|feature| feature['index'] == 0}[0]['featuregroup']['id']).to eql(fg[:id])
          expect(td_features.select{|feature| feature['index'] == 0}[0]['name']).to eql("testfeature")
          expect(td_features.select{|feature| feature['index'] == 0}[0]['label']).to be false
          expect(td_features.select{|feature| feature['index'] == 1}[0]['featuregroup']['id']).to eql(fg1[:id])
          expect(td_features.select{|feature| feature['index'] == 1}[0]['name']).to eql("fg1_testfeature")
          expect(td_features.select{|feature| feature['index'] == 1}[0]['label']).to be true
        end
		
        it "should be able to create a feature view from a query object with missing label" do
          # create feature group
          featurestore_id = get_featurestore_id(@project.id)
          features = [
            {type: "INT", name: "testfeature", primary: true},
            {type: "INT", name: "testfeature1"},
          ]
          fg = create_cached_featuregroup_checked_return_fg(@project.id, featurestore_id, "test_fg_#{short_random_id}",
                                                   features: features)
          # create queryDTO object
          query = {
            leftFeatureGroup: {
              id: fg[:id],
              type: fg[:type],
            },
            leftFeatures: [{name: 'testfeature'}, {name: 'testfeature1'}]
          }
          features = [
            {type: "INT", name: "does_not_exists", label: true},
          ]
          create_feature_view(@project.id, featurestore_id, query, features: features)
          expect_status_details(404)
        end

        it "should fail to create a feature view with invalid query" do
          # create feature group
          featurestore_id = get_featurestore_id(@project.id)
          features = [
              {type: "INT", name: "testfeature", primary: true},
              {type: "INT", name: "testfeature1"},
          ]
          fg = create_cached_featuregroup_checked_return_fg(@project.id, featurestore_id, "test_fg_#{short_random_id}",
                                                   features: features)
          # create queryDTO object
          query = {
              leftFeatureGroup: {
                id: fg[:id],
                type: fg[:type],
              },
              leftFeatures: [{name: 'does_not_exists'}]
          }
          create_feature_view(@project.id, featurestore_id, query)
          expect_status_details(400)
        end

        it "should be able to create a feature view from a query object - 2" do
          # create first feature group
          featurestore_id = get_featurestore_id(@project.id)
          features = [
              {type: "INT", name: "a_testfeature", primary: true},
              {type: "INT", name: "a_testfeature1"},
          ]
          fg = create_cached_featuregroup_checked_return_fg(@project.id, featurestore_id, "test_fg_#{short_random_id}", features: features)
          # create second feature group
          features = [
              {type: "INT", name: "a_testfeature", primary: true},
              {type: "INT", name: "b_testfeature1"},
          ]
          fg_b = create_cached_featuregroup_checked_return_fg(@project.id, featurestore_id, "test_fg_b_#{short_random_id}", features: features)
          # create queryDTO object
          query = {
              leftFeatureGroup: {
                  id: fg[:id],
                  type: fg[:type],
              },
              leftFeatures: [{name: 'a_testfeature'}, {name: 'a_testfeature1'}],
              joins: [{
                       query: {
                           leftFeatureGroup: {
                             id: fg_b[:id],
                             type: fg_b[:type],
                           },
                           leftFeatures: [{name: 'a_testfeature'}, {name: 'b_testfeature1'}]
                       }
                  }
              ]
          }

          json_result = create_feature_view(@project.id, featurestore_id, query)
          expect_status_details(201)
          feature_view = JSON.parse(json_result)

          expect(feature_view.key?('query')).to be true
          td_features = feature_view['features']
          expect(td_features.length).to eql(3)
          # check that all the features are indexed correctly and that they are picked from the correct feature group
          expect(td_features.select{|feature| feature['index'] == 0}[0]['name']).to eql("a_testfeature")
          expect(td_features.select{|feature| feature['index'] == 0}[0]['featuregroup']['id']).to eql(fg[:id])
          expect(td_features.select{|feature| feature['index'] == 1}[0]['name']).to eql("a_testfeature1")
          expect(td_features.select{|feature| feature['index'] == 1}[0]['featuregroup']['id']).to eql(fg[:id])
          expect(td_features.select{|feature| feature['index'] == 2}[0]['name']).to eql("b_testfeature1")
          expect(td_features.select{|feature| feature['index'] == 2}[0]['featuregroup']['id']).to eql(fg_b[:id])
        end
      end

      context 'list feature views' do
        before :all do
          with_valid_project

          project = get_project
          featurestore_id = get_featurestore_id(project.id)

          json_result, _ = create_cached_featuregroup(project.id, featurestore_id, online:true)
          parsed_fg_json = JSON.parse(json_result)

          create_feature_view_from_feature_group(project.id, featurestore_id, parsed_fg_json, name:"featureview1")
          expect_status_details(201)

          create_feature_view_from_feature_group(project.id, featurestore_id, parsed_fg_json, name:"featureview2", version:1)
          expect_status_details(201)

          create_feature_view_from_feature_group(project.id, featurestore_id, parsed_fg_json, name:"featureview2", version:2)
          expect_status_details(201)
        end

        it "should be able to get a list of feature view" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)

          # Get the list
          json_result = get_feature_views(project.id, featurestore_id)
          parsed_json = JSON.parse(json_result)
          expect_status_details(200)

          expect(parsed_json["items"].size).to eq(3)
        end

        it "should be able to get a list of feature view sorted by id" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)

          # Get the list
          get "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featureview?sort_by=ID:asc"
          expect_status_details(200)
          ids = json_body[:items].map { |o| o[:id] }
          sorted_ids = ids.sort

          expect(json_body[:items].length).to eq(3)
          expect(ids).to eq(ids.sort)
          expect(ids).not_to eq(ids.sort {|x, y| y <=> x})
        end

        it "should be able to get a list of feature view sorted by id descending" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)

          # Get the list
          get "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featureview?sort_by=ID:desc"
          expect_status_details(200)
          ids = json_body[:items].map { |o| o[:id] }

          expect(json_body[:items].length).to eq(3)
          expect(ids).to eq(ids.sort {|x, y| y <=> x})
          expect(ids).not_to eq(ids.sort)
        end

        it "should be able to get a list of feature view sorted by name and version" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)

          # Get the list
          get "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featureview?sort_by=NAME:asc,VERSION:asc"
          expect_status_details(200)
          names_versions = json_body[:items].map { |o| "#{o[:name]}_#{o[:version]}" }
          sorted_names_versions = names_versions.sort_by(&:downcase)
  
          expect(json_body[:items].length).to eq(3)
          expect(names_versions).to eq(sorted_names_versions)
        end

        it "should be able to get a list of feature view filtered by name" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)

          # Get the list
          get "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featureview?filter_by=NAME:featureview2"
          expect_status_details(200)
  
          expect(json_body[:items].length).to eq(2)
          expect(json_body[:items].all? { |fv| fv[:name] == "featureview2" }).to be true
        end

        it "should be able to get a list of feature view filtered by latest_version" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)

          # Get the list
          get "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featureview?filter_by=latest_version"
          expect_status_details(200)
          names = json_body[:items].map { |o| "#{o[:name]}" }
  
          expect(json_body[:items].length).to eq(2)
          expect(json_body[:items].length).to eq(names.uniq.size)
          expect(json_body[:items].any? { |fv| fv[:name] == "featureview2" && fv[:version] == 2}).to be true
        end

        it "should be able to get a list of feature view filtered by name and version" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)

          # Get the list
          get "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featureview?filter_by=NAME:featureview2&filter_by=VERSION:2"
          expect_status_details(200)

          expect(json_body[:items].length).to eq(1)
          expect(json_body[:items].all? { |fv| fv[:name] == "featureview2" }).to be true
          expect(json_body[:items].all? { |fv| fv[:version] == 2 }).to be true
        end

        it "should be able to get a list of feature view limit" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)

          # Get the list
          get "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featureview?sort_by=ID:asc&limit=2"
          expect_status_details(200)

          expect(json_body[:items].length).to eq(2)
        end

        it "should be able to get a list of feature view offset" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)

          # Get the list
          get "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featureview?sort_by=ID:asc"
          expect_status_details(200)
          ids = json_body[:items].map { |o| o[:id] }

          get "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featureview?sort_by=ID:asc&offset=1"
          expect_status_details(200)
          ids_with_offset = json_body[:items].map { |o| o[:id] }

          expect(json_body[:items].length).to eq(2)
          for i in 0..json_body[:items].length-1 do
            expect(ids[i+1]).to eq(ids_with_offset[i])
          end
        end

        it "should be able to get a list of feature view versions based on the name" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)

          # Get the list fv1
          json_result = get_feature_view_by_name(project.id, featurestore_id, "featureview1")
          parsed_json = JSON.parse(json_result)
          expect_status_details(200)

          expect(parsed_json["items"].size).to eq 1

          # Get the list fv2
          json_result = get_feature_view_by_name(project.id, featurestore_id, "featureview2")
          parsed_json = JSON.parse(json_result)
          expect_status_details(200)

          expect(parsed_json["items"].size).to eq 2
        end
      end
    end

    describe "permissions" do
      before :all do
        # Create users
        @user1_params = {email: "user1_#{random_id}@email.com", first_name: "User", last_name: "1", password: "Pass123"}
        @user1 = create_user(@user1_params)
        pp "user email: #{@user1[:email]}" if defined?(@debugOpt) && @debugOpt
        @user_data_scientist_params = {email: "data_scientist_#{random_id}@email.com", first_name: "User", last_name: "data_scientist", password: "Pass123"}
        @user_data_scientist = create_user(@user_data_scientist_params)
        pp "user email: #{@user_data_scientist[:email]}" if defined?(@debugOpt) && @debugOpt
        @user_data_owner_params = {email: "data_owner_#{random_id}@email.com", first_name: "User", last_name: "data_owner", password: "Pass123"}
        @user_data_owner = create_user(@user_data_owner_params)
        pp "user email: #{@user_data_owner[:email]}" if defined?(@debugOpt) && @debugOpt

        # Create base project
        create_session(@user1[:email], @user1_params[:password])
        @project1 = create_project
        pp @project1[:projectname] if defined?(@debugOpt) && @debugOpt

        # Add members to projects
        add_member_to_project(@project1, @user_data_owner_params[:email], "Data owner")
        add_member_to_project(@project1, @user_data_scientist_params[:email], "Data scientist")
      end

      context "feature view permissions" do

        # create

        it 'data owner should be able to create fv' do
          create_session(@user1[:email], @user1_params[:password])
          fs = get_featurestore(@project1[:id])
          json_result, fg_name = create_cached_featuregroup(@project1[:id], fs["featurestoreId"], online:true)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          create_session(@user_data_owner[:email], @user_data_owner_params[:password])
          json_result = create_feature_view_from_feature_group(@project1[:id], fs["featurestoreId"], parsed_json)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)
        end

        it 'data scientist should be able to create fv' do
          create_session(@user1[:email], @user1_params[:password])
          fs = get_featurestore(@project1[:id])
          json_result, fg_name = create_cached_featuregroup(@project1[:id], fs["featurestoreId"], online:true)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          create_session(@user_data_scientist[:email], @user_data_scientist_params[:password])
          json_result = create_feature_view_from_feature_group(@project1[:id], fs["featurestoreId"], parsed_json)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)
        end

        # get

        it 'data owner should be able to get fv' do
          create_session(@user1[:email], @user1_params[:password])
          fs = get_featurestore(@project1[:id])
          json_result, fg_name = create_cached_featuregroup(@project1[:id], fs["featurestoreId"], online:true)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          json_result = create_feature_view_from_feature_group(@project1[:id], fs["featurestoreId"], parsed_json)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          create_session(@user_data_owner[:email], @user_data_owner_params[:password])
          json_result = get_feature_view_by_name_and_version(@project1[:id], fs["featurestoreId"], parsed_json["name"], parsed_json["version"])
          parsed_json = JSON.parse(json_result)
          expect_status_details(200)
        end

        it 'data scientist should be able to get fv' do
          create_session(@user1[:email], @user1_params[:password])
          fs = get_featurestore(@project1[:id])
          json_result, fg_name = create_cached_featuregroup(@project1[:id], fs["featurestoreId"], online:true)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          json_result = create_feature_view_from_feature_group(@project1[:id], fs["featurestoreId"], parsed_json)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          create_session(@user_data_scientist[:email], @user_data_scientist_params[:password])
          json_result = get_feature_view_by_name_and_version(@project1[:id], fs["featurestoreId"], parsed_json["name"], parsed_json["version"])
          parsed_json = JSON.parse(json_result)
          expect_status_details(200)
        end

        # update

        it 'data owner should be able to update fv' do
          create_session(@user1[:email], @user1_params[:password])
          fs = get_featurestore(@project1[:id])
          json_result, fg_name = create_cached_featuregroup(@project1[:id], fs["featurestoreId"], online:true)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          json_result = create_feature_view_from_feature_group(@project1[:id], fs["featurestoreId"], parsed_json)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          create_session(@user_data_owner[:email], @user_data_owner_params[:password])

          json_data = {
            name: "new_testfeatureviewname",
			      version: parsed_json["version"],
            type: "featureViewDTO",
            description: "temp desc"
          }
          json_result = update_feature_view(@project1[:id], fs["featurestoreId"], json_data, parsed_json["name"], parsed_json["version"])
          parsed_json = JSON.parse(json_result)
          expect_status_details(200)
          
          json_result = get_feature_view_by_name_and_version(@project1[:id], fs["featurestoreId"], parsed_json["name"], parsed_json["version"])
          parsed_json = JSON.parse(json_result)
          expect_status_details(200)
          expect(parsed_json["description"]).to eql(json_data[:description])
        end

        it 'data scientist should not be able to update fv' do
          create_session(@user1[:email], @user1_params[:password])
          fs = get_featurestore(@project1[:id])
          json_result, fg_name = create_cached_featuregroup(@project1[:id], fs["featurestoreId"], online:true)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          json_result = create_feature_view_from_feature_group(@project1[:id], fs["featurestoreId"], parsed_json)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          create_session(@user_data_scientist[:email], @user_data_scientist_params[:password])
          description = parsed_json["description"]

          json_data = {
            name: "new_testfeatureviewname",
			      version: parsed_json["version"],
            type: "featureViewDTO",
            description: "temp desc"
          }
          json_result = update_feature_view(@project1[:id], fs["featurestoreId"], json_data, parsed_json["name"], parsed_json["version"])
          expect_status_details(403)
          
          json_result = get_feature_view_by_name_and_version(@project1[:id], fs["featurestoreId"], parsed_json["name"], parsed_json["version"])
          parsed_json = JSON.parse(json_result)
          expect_status_details(200)
          expect(parsed_json["description"]).to eql(description)
        end
        
        it 'data scientist should be able to update self made fv' do
          create_session(@user1[:email], @user1_params[:password])
          fs = get_featurestore(@project1[:id])
          json_result, fg_name = create_cached_featuregroup(@project1[:id], fs["featurestoreId"], online:true)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          create_session(@user_data_scientist[:email], @user_data_scientist_params[:password])
          json_result = create_feature_view_from_feature_group(@project1[:id], fs["featurestoreId"], parsed_json)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          json_data = {
            name: "new_testfeatureviewname",
			      version: parsed_json["version"],
            type: "featureViewDTO",
            description: "temp desc"
          }
          json_result = update_feature_view(@project1[:id], fs["featurestoreId"], json_data, parsed_json["name"], parsed_json["version"])
          parsed_json = JSON.parse(json_result)
          expect_status_details(200)
          
          json_result = get_feature_view_by_name_and_version(@project1[:id], fs["featurestoreId"], parsed_json["name"], parsed_json["version"])
          parsed_json = JSON.parse(json_result)
          expect_status_details(200)
          expect(parsed_json["description"]).to eql(json_data[:description])
        end

        # delete

        it 'data owner should be able to delete fv' do
          create_session(@user1[:email], @user1_params[:password])
          fs = get_featurestore(@project1[:id])
          json_result, fg_name = create_cached_featuregroup(@project1[:id], fs["featurestoreId"], online:true)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          json_result = create_feature_view_from_feature_group(@project1[:id], fs["featurestoreId"], parsed_json)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          create_session(@user_data_owner[:email], @user_data_owner_params[:password])
          delete_feature_view(@project1["id"], parsed_json["name"], feature_store_id: fs["featurestoreId"])
          
          json_result = get_feature_view_by_name_and_version(@project1[:id], fs["featurestoreId"], parsed_json["name"], parsed_json["version"])
          parsed_json = JSON.parse(json_result)
          expect_status_details(404)
        end

        it 'data scientist should not be able to delete fv' do
          create_session(@user1[:email], @user1_params[:password])
          fs = get_featurestore(@project1[:id])
          json_result, fg_name = create_cached_featuregroup(@project1[:id], fs["featurestoreId"], online:true)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          json_result = create_feature_view_from_feature_group(@project1[:id], fs["featurestoreId"], parsed_json)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          create_session(@user_data_scientist[:email], @user_data_scientist_params[:password])
          delete_feature_view(@project1["id"], parsed_json["name"], feature_store_id: fs["featurestoreId"], expected_status: 403)
          
          json_result = get_feature_view_by_name_and_version(@project1[:id], fs["featurestoreId"], parsed_json["name"], parsed_json["version"])
          parsed_json = JSON.parse(json_result)
          expect_status_details(200)
        end
        
        it 'data scientist should be able to delete self made fv' do
          create_session(@user1[:email], @user1_params[:password])
          fs = get_featurestore(@project1[:id])
          json_result, fg_name = create_cached_featuregroup(@project1[:id], fs["featurestoreId"], online:true)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          create_session(@user_data_scientist[:email], @user_data_scientist_params[:password])
          json_result = create_feature_view_from_feature_group(@project1[:id], fs["featurestoreId"], parsed_json)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          delete_feature_view(@project1["id"], parsed_json["name"], feature_store_id: fs["featurestoreId"])
          
          json_result = get_feature_view_by_name_and_version(@project1[:id], fs["featurestoreId"], parsed_json["name"], parsed_json["version"])
          parsed_json = JSON.parse(json_result)
          expect_status_details(404)
        end
      end
    end
  end
end
