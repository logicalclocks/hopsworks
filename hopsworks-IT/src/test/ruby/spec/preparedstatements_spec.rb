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
  after(:all) {clean_all_test_projects(spec: "preparedstatements")}

  describe "prepared statements" do

    describe "list" do
      context 'with valid project, featurestore service enabled' do
        before :all do
          with_valid_project
        end
		
		it "should be able to get a feature view serving vector" do
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

          json_result, _ = create_feature_view(@project.id, featurestore_id, query)
          parsed_json = JSON.parse(json_result)
          expect_status(201)

          feature_view_name = parsed_json["name"]
          feature_view_version = parsed_json["version"]
          json_result = get "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/featureview/#{feature_view_name}/version/#{feature_view_version}/preparedstatement"
          expect_status_details(200)

          parsed_json = JSON.parse(json_result)
          expect(parsed_json["items"].first["preparedStatementParameters"].first["index"]).to eql(1)
          expect(parsed_json["items"].first["preparedStatementParameters"].first["name"]).to eql("a")
          expect(parsed_json["items"].first["queryOnline"]).to eql("SELECT `fg0`.`d` AS `d`, `fg0`.`c` AS `c`, `fg0`.`a` AS `a`, `fg0`.`b` AS `b`\n"+
                                                                   "FROM `#{project_name.downcase}`.`#{fg_name}_1` AS `fg0`\n" +
                                                                   "WHERE `fg0`.`a` = ?")
        end

        it "should be able to get a feature view serving vector in correct order" do
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

          json_result, _ = create_feature_view(@project.id, featurestore_id, query)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          feature_view_name = parsed_json["name"]
          feature_view_version = parsed_json["version"]
          json_result = get "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/featureview/#{feature_view_name}/version/#{feature_view_version}/preparedstatement"
          expect_status_details(200)

          parsed_json = JSON.parse(json_result)
          expect(parsed_json["items"].first["preparedStatementParameters"].first["index"]).to eql(1)
          expect(parsed_json["items"].first["preparedStatementParameters"].first["name"]).to eql("a_testfeature")
          expect(parsed_json["items"].first["queryOnline"]).to eql("SELECT `fg0`.`a_testfeature1` AS `a_testfeature1`\nFROM `#{project_name.downcase}`.`#{fg_name}_1` AS `fg0`\nWHERE `fg0`.`a_testfeature` = ?")
          expect(parsed_json["items"].second["preparedStatementParameters"].first["index"]).to eql(1)
          expect(parsed_json["items"].second["preparedStatementParameters"].first["name"]).to eql("a_testfeature")
          expect(parsed_json["items"].second["queryOnline"]).to eql("SELECT `fg0`.`b_testfeature1` AS `b_testfeature1`\nFROM `#{project_name.downcase}`.`#{fg_name_b}_1` AS `fg0`\nWHERE `fg0`.`a_testfeature` = ?")
        end

        it "should be able to get a feature view serving vector in correct order and remove feature group with only primary key and label" do
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

          feature_schema = [
              {type: "INT", name: "a_testfeature1", label: false},
              {type: "INT", name: "b_testfeature1", label: false},
              {type: "INT", name: "c_testfeature1", label: true}
          ]

          json_result, _ = create_feature_view(@project.id, featurestore_id, query, features: feature_schema)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          feature_view_name = parsed_json["name"]
          feature_view_version = parsed_json["version"]
          json_result = get "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/featureview/#{feature_view_name}/version/#{feature_view_version}/preparedstatement?batch=false"
          expect_status_details(200)

          parsed_json = JSON.parse(json_result)
          expect(parsed_json["items"].length).to eql(2)
          expect(parsed_json["items"].first["preparedStatementParameters"].first["index"]).to eql(1)
          expect(parsed_json["items"].first["preparedStatementParameters"].first["name"]).to eql("a_testfeature")
          expect(parsed_json["items"].first["queryOnline"]).to eql("SELECT `fg0`.`a_testfeature1` AS `a_testfeature1`\nFROM `#{project_name.downcase}`.`#{fg_name}_1` AS `fg0`\nWHERE `fg0`.`a_testfeature` = ?")
          expect(parsed_json["items"].second["preparedStatementParameters"].first["index"]).to eql(1)
          expect(parsed_json["items"].second["preparedStatementParameters"].first["name"]).to eql("a_testfeature")
          expect(parsed_json["items"].second["queryOnline"]).to eql("SELECT `fg0`.`b_testfeature1` AS `b_testfeature1`\nFROM `#{project_name.downcase}`.`#{fg_name_b}_1` AS `fg0`\nWHERE `fg0`.`a_testfeature` = ?")
        end

        it "it should add primary keys to the prepared statement in batch mode" do
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
                        leftFeatures: [{name: 'b_testfeature1'}],
                      },
                      leftOn: [{name: 'a_testfeature'}],
                      rightOn: [{name: 'b_testfeature'}],
                      prefix: 'yolo_'
                   }]
          }

          feature_schema = [
            {type: "INT", name: "a_testfeature1", label: false},
            {type: "INT", name: "b_testfeature1", label: false},
          ]

          json_result, _ = create_feature_view(@project.id, featurestore_id, query, features: feature_schema)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          feature_view_name = parsed_json["name"]
          feature_view_version = parsed_json["version"]
          json_result = get "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/featureview/#{feature_view_name}/version/#{feature_view_version}/preparedstatement?batch=true"
          expect_status_details(200)

          parsed_json = JSON.parse(json_result)
          expect(parsed_json["items"].length).to eql(2)
          expect(parsed_json["items"].first["preparedStatementParameters"].first["index"]).to eql(1)
          expect(parsed_json["items"].first["preparedStatementParameters"].first["name"]).to eql("a_testfeature")
          expect(parsed_json["items"].first["queryOnline"]).to eql("SELECT `fg0`.`a_testfeature1` AS `a_testfeature1`, `fg0`.`a_testfeature` AS `a_testfeature`\nFROM `#{project_name.downcase}`.`#{fg_name}_1` AS `fg0`\nWHERE `fg0`.`a_testfeature` IN ?")
          expect(parsed_json["items"].second["preparedStatementParameters"].first["index"]).to eql(1)
          expect(parsed_json["items"].second["preparedStatementParameters"].first["name"]).to eql("b_testfeature")
          expect(parsed_json["items"].second["queryOnline"]).to eql("SELECT `fg0`.`b_testfeature1` AS `yolo_b_testfeature1`, `fg0`.`b_testfeature` AS `yolo_b_testfeature`\nFROM `#{project_name.downcase}`.`#{fg_name_b}_1` AS `fg0`\nWHERE `fg0`.`b_testfeature` IN ?")
          expect(parsed_json["items"].second["prefix"]).to eql("yolo_")
        end

        it "should fail when calling get serving vector from feature view created from offline fg" do
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

          json_result, _ = create_feature_view(@project.id, featurestore_id, query)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          feature_view_name = parsed_json["name"]
          feature_view_version = parsed_json["version"]
          get "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/featureview/#{feature_view_name}/version/#{feature_view_version}/preparedstatement"
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

          json_result, _ = create_feature_view(@project.id, featurestore_id, query)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          feature_view_name = parsed_json["name"]
          feature_view_version = parsed_json["version"]

          # delete the second feature group
          delete_featuregroup_checked(@project.id, featurestore_id, fg_id_b)

          get "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/featureview/#{feature_view_name}/version/#{feature_view_version}/preparedstatement"
          expect_status_details(400)
        end

        it "should fail when calling get serving vector from feature view created from fg without primary key" do
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

          json_result, _ = create_feature_view(@project.id, featurestore_id, query)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          feature_view_name = parsed_json["name"]
          feature_view_version = parsed_json["version"]
          get "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/featureview/#{feature_view_name}/version/#{feature_view_version}/preparedstatement"
          expect_status_details(400)
        end
      end
    end
  end
end
