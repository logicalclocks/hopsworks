# This file is part of Hopsworks
# Copyright (C) 2024, Hopsworks AB. All rights reserved
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
  after(:all) {clean_all_test_projects(spec: "similarity_search")}

  describe "similarity search" do
    describe "online feature groups" do
      context 'with valid project, featurestore service enabled' do
        before :all do
          with_valid_project
        end

        it "should be able to add a cached featuregroup with online feature serving, and embedding with project index to the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, online:true, embedding_index_name: "")
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)
          expect(parsed_json.key?("id")).to be true
          expect(parsed_json.key?("featurestoreName")).to be true
          expect(parsed_json.key?("onlineEnabled")).to be true
          expect(parsed_json.key?("name")).to be true
          expect(parsed_json["featurestoreName"] == project.projectname.downcase + "_featurestore").to be true
          expect(parsed_json["name"] == featuregroup_name).to be true
          expect(parsed_json["type"] == "cachedFeaturegroupDTO").to be true
          expect(parsed_json["onlineTopicName"]).to end_with("_onlinefs")
          expect(parsed_json.key?("embeddingIndex")).to be true
          expect(parsed_json["embeddingIndex"]["indexName"]).to eq("#{project.id}__embedding_default_project_embedding_0")
          expect(parsed_json["embeddingIndex"]["features"].length).to be 1
        end

        it "should be able to delete a feature group with embedding and project index" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, online:true, embedding_index_name: "")
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)
          featuregroup_id = parsed_json["id"]
          delete_featuregroup_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project.id}/featurestores/#{featurestore_id}/featuregroups/#{featuregroup_id}"
          delete delete_featuregroup_endpoint
          expect_status_details(200)

          # index project.id + "__embedding_default_project_embedding_0" should NOT be deleted
          result = opensearch_get "#{project.id.to_s}__embedding_default_project_embedding_0"
          opensearch_status_details(result, 200)
        end

        it "should be able to add a cached featuregroup with online feature serving, and embedding with custom index to the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, online:true, embedding_index_name: "test_index")
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)
          expect(parsed_json.key?("id")).to be true
          expect(parsed_json.key?("featurestoreName")).to be true
          expect(parsed_json.key?("onlineEnabled")).to be true
          expect(parsed_json.key?("name")).to be true
          expect(parsed_json["featurestoreName"] == project.projectname.downcase + "_featurestore").to be true
          expect(parsed_json["name"] == featuregroup_name).to be true
          expect(parsed_json["type"] == "cachedFeaturegroupDTO").to be true
          expect(parsed_json["onlineTopicName"]).to end_with("_onlinefs")
          expect(parsed_json.key?("embeddingIndex")).to be true
          expect(parsed_json["embeddingIndex"]["indexName"]).to eq("#{project.id}__embedding_test_index")
          expect(parsed_json["embeddingIndex"]["features"].length).to be 1
        end

        it "should be able to delete a feature group with embedding and custom index" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, online:true, embedding_index_name: "test_index")
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)
          featuregroup_id = parsed_json["id"]
          delete_featuregroup_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project.id}/featurestores/#{featurestore_id}/featuregroups/#{featuregroup_id}"
          delete delete_featuregroup_endpoint
          expect_status_details(200)

          # index project.id + "__embedding_test_index" should be deleted
          result = opensearch_get "#{project.id}__embedding_test_index"
          opensearch_status_details(result, 404)
        end

        it "should remove all indices in vector db when the project is deleted" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, online:true, embedding_index_name: "test_index")
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)
          post "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/delete"
          expect_status_details(200)
          expect_json(successMessage: "The project and all related files were removed successfully.")

          result = opensearch_get "_cat/indices"
          opensearch_status_details(result, 200)
          # all indices started with #{project.id}__embedding_ should be removed when the project is deleted
          expect(result.body.include?("#{project.id}__embedding_")).to be false
        end
      end
    end
  end
end
