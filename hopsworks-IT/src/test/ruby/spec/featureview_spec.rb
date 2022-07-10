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
          expect_status(201)

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

          json_result = create_feature_view_from_feature_group(@project.id, featurestore_id, parsed_json, name:"TEST_feature_view")
          parsed_json = JSON.parse(json_result)
          expect_status(400)

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
          expect_status(400)

          expect(parsed_json.key?("errorCode")).to be true
          expect(parsed_json.key?("errorMsg")).to be true
          expect(parsed_json.key?("usrMsg")).to be true
          expect(parsed_json["errorCode"] == 270058).to be true
        end

        it "should be able to add a feature view without version to the featurestore" do
          featurestore_id = get_featurestore_id(@project.id)

          json_result, fg_name = create_cached_featuregroup(@project.id, featurestore_id, online:true)
          parsed_json = JSON.parse(json_result)

          json_result = create_feature_view_from_feature_group(@project.id, featurestore_id, parsed_json, version:nil)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          expect(parsed_json["version"] == 1).to be true
        end

        it "should be able to add a new version of an existing feature view without version to the featurestore" do
          featurestore_id = get_featurestore_id(@project.id)

          json_result, fg_name = create_cached_featuregroup(@project.id, featurestore_id, online:true)
          parsed_fg_json = JSON.parse(json_result)

          json_result = create_feature_view_from_feature_group(@project.id, featurestore_id, parsed_fg_json)
          parsed_json = JSON.parse(json_result)
          expect_status(201)

          # add second version
          json_result = create_feature_view_from_feature_group(@project.id, featurestore_id, parsed_fg_json, name:parsed_json["name"], version:nil)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          # version should be incremented to 2
          expect(parsed_json["version"] == 2).to be true
        end

        it "should not be able to create a feature view with the same name and version" do
          featurestore_id = get_featurestore_id(@project.id)

          json_result, fg_name = create_cached_featuregroup(@project.id, featurestore_id, online:true)
          parsed_fg_json = JSON.parse(json_result)

          json_result = create_feature_view_from_feature_group(@project.id, featurestore_id, parsed_fg_json)
          parsed_json = JSON.parse(json_result)
          expect_status(201)

          json_result = create_feature_view_from_feature_group(@project.id, featurestore_id, parsed_fg_json, name:parsed_json["name"], version:parsed_json["version"])
          expect_status(400)
        end

        it "should be able to delete a feature view from the featurestore" do
          featurestore_id = get_featurestore_id(@project.id)

          json_result, fg_name = create_cached_featuregroup(@project.id, featurestore_id, online:true)
          parsed_json = JSON.parse(json_result)

          json_result = create_feature_view_from_feature_group(@project.id, featurestore_id, parsed_json)
          parsed_json = JSON.parse(json_result)
          expect_status(201)

		  # Make sure that the directory exists
          get_datasets_in_path(@project,
                               "#{@project[:projectname]}_Training_Datasets/.featureviews/#{parsed_json['name']}_#{parsed_json['version']}",
                               query: "&type=DATASET")
          expect_status(200)

          delete_feature_view(@project.id, parsed_json["name"], feature_store_id: featurestore_id)
          expect_status(200)
        end

        it "should not be able to update feature view that doesnt exist" do
          featurestore_id = get_featurestore_id(@project.id)

          json_data = {
            name: "feature_view_name",
            version: 1,
            description: "testfeatureviewdescription"
          }

          update_feature_view(@project.id, featurestore_id, json_data, "feature_view_name", 1)
          expect_status(404)
        end

        it "should be able to update the description of a feature view" do
          featurestore_id = get_featurestore_id(@project.id)

          json_result, fg_name = create_cached_featuregroup(@project.id, featurestore_id, online:true)
          parsed_json = JSON.parse(json_result)

          json_result = create_feature_view_from_feature_group(@project.id, featurestore_id, parsed_json)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          featureview_name = parsed_json["name"]
          featureview_version = parsed_json["version"]

          new_description = "new_testfeatureviewdescription"
          json_data = {
            name: featureview_name,
            version: featureview_version,
            description: new_description
          }

          json_result = update_feature_view(@project.id, featurestore_id, json_data, featureview_name, featureview_version)
          parsed_json = JSON.parse(json_result)
          expect_status(200)

          expect(parsed_json["description"]).to eql(new_description)
        end

        it "should be able to update the name of a feature view" do
          featurestore_id = get_featurestore_id(@project.id)

          json_result, fg_name = create_cached_featuregroup(@project.id, featurestore_id, online:true)
          parsed_json = JSON.parse(json_result)

          json_result = create_feature_view_from_feature_group(@project.id, featurestore_id, parsed_json)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          featureview_name = parsed_json["name"]
          featureview_version = parsed_json["version"]

          json_data = {
            name: "new_testfeatureviewname",
			version: featureview_version
          }

          json_result = update_feature_view(@project.id, featurestore_id, json_data, featureview_name, featureview_version)
          parsed_json = JSON.parse(json_result)
          expect_status(200)

          expect(parsed_json["name"]).to eql(featureview_name)
        end

        it "should be able to get a list of feature view" do
          project = create_project()
          featurestore_id = get_featurestore_id(project.id)

          json_result, fg_name = create_cached_featuregroup(project.id, featurestore_id, online:true)
          parsed_fg_json = JSON.parse(json_result)

          json_result = create_feature_view_from_feature_group(project.id, featurestore_id, parsed_fg_json, name:"featureview1")
          parsed_json = JSON.parse(json_result)
          expect_status(201)

          # add second version
          json_result = create_feature_view_from_feature_group(project.id, featurestore_id, parsed_fg_json, name:"featureview2")
          parsed_json = JSON.parse(json_result)
          expect_status(201)

          # Get the list
		  json_result = get_feature_views(project.id, featurestore_id)
          parsed_json = JSON.parse(json_result)
          expect_status(200)

          expect(parsed_json["items"].size).to eq 2
        end

        it "should be able to get a list of feature view versions based on the name" do
          featurestore_id = get_featurestore_id(@project.id)

          json_result, fg_name = create_cached_featuregroup(@project.id, featurestore_id, online:true)
          parsed_fg_json = JSON.parse(json_result)

          json_result = create_feature_view_from_feature_group(@project.id, featurestore_id, parsed_fg_json)
          parsed_json = JSON.parse(json_result)
          expect_status(201)

          # add second version
          json_result = create_feature_view_from_feature_group(@project.id, featurestore_id, parsed_fg_json, name:parsed_json["name"], version:nil)
          parsed_json = JSON.parse(json_result)
          expect_status(201)

          # Get the list
		  json_result = get_feature_view_by_name(@project.id, featurestore_id, parsed_json["name"])
          parsed_json = JSON.parse(json_result)
          expect_status(200)

          expect(parsed_json["items"].size).to eq 2
        end

        it "should be able to get a feature view based on name and version" do
          featurestore_id = get_featurestore_id(@project.id)

          json_result, fg_name = create_cached_featuregroup(@project.id, featurestore_id, online:true)
          parsed_fg_json = JSON.parse(json_result)

          json_result = create_feature_view_from_feature_group(@project.id, featurestore_id, parsed_fg_json)
          parsed_json_1 = JSON.parse(json_result)
          expect_status(201)
		  featureview_name = parsed_json_1["name"]

          # add second version
          json_result = create_feature_view_from_feature_group(@project.id, featurestore_id, parsed_fg_json, name:featureview_name, version:nil)
          parsed_json_2 = JSON.parse(json_result)
          expect_status(201)

          # Get the first version
          json_result = get_feature_view_by_name_and_version(@project.id, featurestore_id, parsed_json_1["name"], parsed_json_1["version"])
          parsed_json = JSON.parse(json_result)
          expect_status(200)
          expect(parsed_json['version']).to be 1
          expect(parsed_json['name']).to eq featureview_name

          json_result = get_feature_view_by_name_and_version(@project.id, featurestore_id, parsed_json_2["name"], parsed_json_2["version"])
          parsed_json = JSON.parse(json_result)
          expect_status(200)
          expect(parsed_json['version']).to be 2
          expect(parsed_json['name']).to eq featureview_name
        end

        it "should fail to get a feature view with a name that does not exists" do
          featurestore_id = get_featurestore_id(@project.id)
		  json_result = get_feature_view_by_name(@project.id, featurestore_id, "doesnotexists")
          expect_status(404)
        end

        it "should fail to create a feature view with no features and no query" do
          featurestore_id = get_featurestore_id(@project.id)
          json_data = {
              name: "no_features_no_query",
              version: 1
          }
		  create_feature_view_with_json(@project.id, featurestore_id, json_data)
          expect_status_details(400)
        end
		
		it "should be able to create a feature view from a query object - 1" do
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
          json_result = create_feature_view(@project.id, featurestore_id, query)
          expect_status_details(201)
          feature_view = JSON.parse(json_result)

          expect(feature_view.key?('query')).to be true
          td_features = feature_view['features']
          expect(td_features.count).to eql(2)
          expect(td_features.select{|feature| feature['index'] == 0}[0]['name']).to eql("testfeature")
          expect(td_features.select{|feature| feature['index'] == 0}[0]['featuregroup']['id']).to eql(fg_id)
          expect(td_features.select{|feature| feature['index'] == 1}[0]['name']).to eql("testfeature1")
          expect(td_features.select{|feature| feature['index'] == 1}[0]['featuregroup']['id']).to eql(fg_id)
        end

        it "should be able to create a feature view from a query object with label" do
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
          json_result = create_feature_view(@project.id, featurestore_id, query, features: features)
          expect_status_details(201)
          feature_view = JSON.parse(json_result)

          expect(feature_view.key?('query')).to be true
          td_features = feature_view['features']
          expect(td_features.count).to eql(2)
          expect(td_features.select{|feature| feature['index'] == 0}[0]['label']).to be true
          expect(td_features.select{|feature| feature['index'] == 1}[0]['label']).to be false
        end
		
        it "should be able to create a feature view from a query object with missing label" do
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
          fg_id = create_cached_featuregroup_checked(@project.id, featurestore_id, "test_fg_#{short_random_id}", features: features)
          # create queryDTO object
          query = {
              leftFeatureGroup: {
                  id: fg_id
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

          json_result = create_feature_view(@project.id, featurestore_id, query)
          expect_status_details(201)
          feature_view = JSON.parse(json_result)

          expect(feature_view.key?('query')).to be true
          td_features = feature_view['features']
          expect(td_features.length).to eql(3)
          # check that all the features are indexed correctly and that they are picked from the correct feature group
          expect(td_features.select{|feature| feature['index'] == 0}[0]['name']).to eql("a_testfeature")
          expect(td_features.select{|feature| feature['index'] == 0}[0]['featuregroup']['id']).to eql(fg_id)
          expect(td_features.select{|feature| feature['index'] == 1}[0]['name']).to eql("a_testfeature1")
          expect(td_features.select{|feature| feature['index'] == 1}[0]['featuregroup']['id']).to eql(fg_id)
          expect(td_features.select{|feature| feature['index'] == 2}[0]['name']).to eql("b_testfeature1")
          expect(td_features.select{|feature| feature['index'] == 2}[0]['featuregroup']['id']).to eql(fg_id_b)
        end
      end
    end
  end
end
