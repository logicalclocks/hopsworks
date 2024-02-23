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
  after :all do
    clean_all_test_projects(spec: "featureviewkeywords")
  end

  describe "feature view keywords" do
    describe "internal" do
      context 'with valid project, featurestore service enabled' do
        before :all do
		  with_valid_project
        end

        it "should be able to attach keywords" do
          featurestore_id = get_featurestore_id(@project.id)
          features = ['a', 'b', 'c', 'd'].map do |feat_name|
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
              id: fg_id,
              type: parsed_json["type"],
            },
            leftFeatures: ['d', 'c', 'a', 'b'].map do |feat_name|
              {name: feat_name}
            end,
            joins: []
          }
		  
          json_result = create_feature_view(@project.id, featurestore_id, query)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)
          feature_view_name = parsed_json["name"]
          feature_view_version = parsed_json["version"]
		  
          post "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/featureview/#{feature_view_name}/version/#{feature_view_version}/keywords",
              {keywords: ['hello', 'this', 'keyword123', 'CAPITAL_LETTERS']}.to_json
          expect_status_details(200)

          json_result = get "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/featureview/#{feature_view_name}/version/#{feature_view_version}/keywords"
          expect_status_details(200)
          parsed_json = JSON.parse(json_result)
          expect(parsed_json['keywords']).to include('hello')
          expect(parsed_json['keywords']).to include('this')
          expect(parsed_json['keywords']).to include('keyword123')
          expect(parsed_json['keywords']).to include('CAPITAL_LETTERS')
        end
		
        it "should be able to find the attached keywords in the list of used keywords" do
          featurestore_id = get_featurestore_id(@project.id)
          features = ['a', 'b', 'c', 'd'].map do |feat_name|
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
              id: fg_id,
              type: parsed_json["type"],
            },
            leftFeatures: ['d', 'c', 'a', 'b'].map do |feat_name|
              {name: feat_name}
            end,
            joins: []
          }
		  
          json_result = create_feature_view(@project.id, featurestore_id, query)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)
          feature_view_name = parsed_json["name"]
          feature_view_version = parsed_json["version"]
		  
          post "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/featureview/#{feature_view_name}/version/#{feature_view_version}/keywords",
              {keywords: ['test', 'lololo']}.to_json
          expect_status_details(200)

          # wait for epipe has time for processing
          epipe_wait_on_mutations(wait_time:5, repeat: 2)

          json_result = get "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/keywords"
          expect_status_details(200)
          parsed_json = JSON.parse(json_result)
          expect(parsed_json['keywords']).to include('test')
          expect(parsed_json['keywords']).to include('lololo')
        end

        it "should fail to attach invalid keywords" do
          featurestore_id = get_featurestore_id(@project.id)
		  features = ['a', 'b', 'c', 'd'].map do |feat_name|
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
              id: fg_id,
              type: parsed_json["type"],
            },
            leftFeatures: ['d', 'c', 'a', 'b'].map do |feat_name|
              {name: feat_name}
            end,
            joins: []
          }

          json_result = create_feature_view(@project.id, featurestore_id, query)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)
          feature_view_name = parsed_json["name"]
          feature_view_version = parsed_json["version"]
		  
          post "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/featureview/#{feature_view_name}/version/#{feature_view_version}/keywords",
              {keywords: ['hello', 'this', '@#!@#^(&$']}
          expect_status_details(400)
        end

        it "should be able to remove keyword" do
          featurestore_id = get_featurestore_id(@project.id)
		  features = ['a', 'b', 'c', 'd'].map do |feat_name|
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
              id: fg_id,
              type: parsed_json["type"],
            },
            leftFeatures: ['d', 'c', 'a', 'b'].map do |feat_name|
              {name: feat_name}
            end,
            joins: []
          }

          json_result = create_feature_view(@project.id, featurestore_id, query)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)
          feature_view_name = parsed_json["name"]
          feature_view_version = parsed_json["version"]
		  
          post "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/featureview/#{feature_view_name}/version/#{feature_view_version}/keywords",
              {keywords: ['hello', 'this', 'keyword123']}.to_json
          expect_status_details(200)

          json_result = get "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/featureview/#{feature_view_name}/version/#{feature_view_version}/keywords"
          expect_status_details(200)
          parsed_json = JSON.parse(json_result)
          expect(parsed_json['keywords']).to include('hello')
          expect(parsed_json['keywords']).to include('this')
          expect(parsed_json['keywords']).to include('keyword123')

          delete "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/featureview/#{feature_view_name}/version/#{feature_view_version}/keywords?keyword=hello"
          expect_status_details(200)

          json_result = get "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/featureview/#{feature_view_name}/version/#{feature_view_version}/keywords"
          expect_status_details(200)
          parsed_json = JSON.parse(json_result)
          expect(parsed_json['keywords']).not_to include('hello')
          expect(parsed_json['keywords']).to include('this')
          expect(parsed_json['keywords']).to include('keyword123')
        end
      end
    end
  end
end