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

describe "On #{ENV['OS']}" do
  describe 'featurestore' do
    after (:all) {clean_projects}

    describe "list featurestores for project, get featurestore by id" do

      context 'with valid project and featurestore service enabled' do
        before :all do
          with_valid_project
        end

        it "should be able to list all featurestores of the project and find one" do
          project = get_project
          list_project_featurestores_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores"
          get list_project_featurestores_endpoint
          parsed_json = JSON.parse(response.body)
          expect_status(200)
          expect(parsed_json.length == 1)
          expect(parsed_json[0].key?("projectName")).to be true
          expect(parsed_json[0].key?("featurestoreName")).to be true
          expect(parsed_json[0]["projectName"] == project.projectname).to be true
          expect(parsed_json[0]["featurestoreName"] == project.projectname + "_featurestore").to be true
        end

        it "should be able to get a featurestore with a particular id" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          list_project_featurestore_with_id = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s
          get list_project_featurestore_with_id
          parsed_json = JSON.parse(response.body)
          expect_status(200)
          expect(parsed_json.key?("projectName")).to be true
          expect(parsed_json.key?("featurestoreName")).to be true
          expect(parsed_json["projectName"] == project.projectname).to be true
          expect(parsed_json["featurestoreName"] == project.projectname + "_featurestore").to be true
        end
      end
    end

    describe "Create, delete and update operations on featuregroups in a specific featurestore" do

      context 'with valid project, featurestore service enabled' do
        before :all do
          with_valid_project
        end

        it "should be able to add a featuregroup to the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_featuregroup(project.id, featurestore_id)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          expect(parsed_json.key?("id")).to be true
          expect(parsed_json.key?("featurestoreName")).to be true
          expect(parsed_json.key?("name")).to be true
          expect(parsed_json["featurestoreName"] == project.projectname + "_featurestore").to be true
          expect(parsed_json["name"] == featuregroup_name).to be true
        end

        it "should be able to preview a featuregroup in the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_featuregroup(project.id, featurestore_id)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          featuregroup_id = parsed_json["id"]
          preview_featuregroup_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s + "/preview"
          get preview_featuregroup_endpoint
          parsed_json = JSON.parse(response.body)
          expect_status(200)
        end

        it "should be able to get the hive schema of a featuregroup in the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_featuregroup(project.id, featurestore_id)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          featuregroup_id = parsed_json["id"]
          get_featuregroup_schema_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s + "/schema"
          get get_featuregroup_schema_endpoint
          parsed_json = JSON.parse(response.body)
          expect_status(200)
        end

        it "should be able to delete a featuregroup from the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_featuregroup(project.id, featurestore_id)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          featuregroup_id = parsed_json["id"]
          delete_featuregroup_endpoint= "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s
          delete delete_featuregroup_endpoint
          parsed_json = JSON.parse(response.body)
          expect_status(200)
          expect(parsed_json.key?("id")).to be true
          expect(parsed_json["id"] == featuregroup_id).to be true
        end

        it "should be able to create a new version of a featuregroup from the featurestore with new metadata" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_featuregroup(project.id, featurestore_id)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          featuregroup_id = parsed_json["id"]
          featuregroup_version = parsed_json["version"]
          update_version_featuregroup(project.id, featurestore_id, featuregroup_name, featuregroup_version + 1)
          parsed_json = JSON.parse(response.body)
          expect_status(201)
          expect(parsed_json.key?("version")).to be true
          expect(parsed_json["version"] == featuregroup_version + 1).to be true
        end

        it "should be able to clear the contents of a featuregroup in the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_featuregroup(project.id, featurestore_id)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          featuregroup_id = parsed_json["id"]
          clear_featuregroup_contents_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s + "/clear"
          post clear_featuregroup_contents_endpoint
          expect_status(200)
        end

        it "should be able to update the metadata of a featuregroup from the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_featuregroup(project.id, featurestore_id)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          featuregroup_id = parsed_json["id"]
          featuregroup_version = parsed_json["version"]
          update_featuregroup_metadata(project.id, featurestore_id, featuregroup_id, featuregroup_version)
          expect_status(200)
        end

      end
    end

    describe "list featuregroups for project, get featuregroup by id" do

      context 'with valid project, featurestore service enabled' do
        before :all do
          with_valid_project
        end

        it "should be able to list all featuregroups of the project's featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          get_featuregroups_endpoint= "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups"
          get get_featuregroups_endpoint
          parsed_json = JSON.parse(response.body)
          expect_status(200)
          expect(parsed_json.length == 0).to be true
          json_result, featuregroup_name = create_featuregroup(project.id, featurestore_id)
          expect_status(201)
          get get_featuregroups_endpoint
          parsed_json = JSON.parse(response.body)
          expect_status(200)
          expect(parsed_json.length == 1).to be true
          expect(parsed_json[0].key?("id")).to be true
          expect(parsed_json[0].key?("featurestoreName")).to be true
          expect(parsed_json[0].key?("name")).to be true
          expect(parsed_json[0]["featurestoreName"] == project.projectname + "_featurestore").to be true
          expect(parsed_json[0]["name"] == featuregroup_name).to be true
        end

        it "should be able to get a featuregroup with a particular id" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_featuregroup(project.id, featurestore_id)
          expect_status(201)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          featuregroup_id = parsed_json["id"]
          get_featuregroup_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s
          get get_featuregroup_endpoint
          parsed_json = JSON.parse(response.body)
          expect_status(200)
          expect(parsed_json.key?("id")).to be true
          expect(parsed_json.key?("featurestoreName")).to be true
          expect(parsed_json.key?("featurestoreId")).to be true
          expect(parsed_json.key?("name")).to be true
          expect(parsed_json["featurestoreId"] == featurestore_id).to be true
          expect(parsed_json["featurestoreName"] == project.projectname + "_featurestore").to be true
          expect(parsed_json["name"] == featuregroup_name).to be true
          expect(parsed_json["id"] == featuregroup_id).to be true
        end
      end
    end

    describe "Create, delete and update operations on training datasets in a specific featurestore" do

      context 'with valid project, featurestore service enabled' do
        before :all do
          with_valid_project
        end

        it "should be able to add a training dataset to the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, training_dataset_name = create_training_dataset(project.id, featurestore_id)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          expect(parsed_json.key?("id")).to be true
          expect(parsed_json.key?("featurestoreName")).to be true
          expect(parsed_json.key?("name")).to be true
          expect(parsed_json["featurestoreName"] == project.projectname + "_featurestore").to be true
          expect(parsed_json["name"] == training_dataset_name).to be true
        end

        it "should be able to delete a training dataset from the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, training_dataset_name = create_training_dataset(project.id, featurestore_id)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          training_dataset_id = parsed_json["id"]
          delete_training_dataset_endpoint= "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/trainingdatasets/" + training_dataset_id.to_s
          delete delete_training_dataset_endpoint
          parsed_json = JSON.parse(response.body)
          expect_status(200)
          expect(parsed_json.key?("id")).to be true
          expect(parsed_json["id"] == training_dataset_id).to be true
        end

        it "should be able to create a new version of a training dataset" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, training_dataset_name = create_training_dataset(project.id, featurestore_id)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          training_dataset_version = parsed_json["version"]
          update_version_training_dataset(project.id, featurestore_id, training_dataset_name, training_dataset_version + 1)
          parsed_json = JSON.parse(response.body)
          expect_status(201)
          expect(parsed_json.key?("version")).to be true
          expect(parsed_json["version"] == training_dataset_version + 1).to be true
        end

        it "should be able to update the metadata of a training dataset from the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, training_dataset_name = create_training_dataset(project.id, featurestore_id)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          training_dataset_version = parsed_json["version"]
          training_dataset_id = parsed_json["id"]
          update_training_dataset_metadata(project.id, featurestore_id, training_dataset_id, training_dataset_version)
          expect_status(200)
        end

      end
    end

    describe "list training datasets for project, get training dataset by id" do

      context 'with valid project, featurestore service enabled' do
        before :all do
          with_valid_project
        end

        it "should be able to list all training datasets of the project's featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          get_training_datasets_endpoint= "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/trainingdatasets"
          get get_training_datasets_endpoint
          parsed_json = JSON.parse(response.body)
          expect_status(200)
          expect(parsed_json.length == 0).to be true
          json_result, training_dataset_name = create_training_dataset(project.id, featurestore_id)
          expect_status(201)
          get get_training_datasets_endpoint
          parsed_json = JSON.parse(response.body)
          expect_status(200)
          expect(parsed_json.length == 1).to be true
          expect(parsed_json[0].key?("id")).to be true
          expect(parsed_json[0].key?("featurestoreName")).to be true
          expect(parsed_json[0].key?("name")).to be true
          expect(parsed_json[0]["featurestoreName"] == project.projectname + "_featurestore").to be true
          expect(parsed_json[0]["name"] == training_dataset_name).to be true
        end

        it "should be able to get a training dataset with a particular id" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, training_dataset_name = create_training_dataset(project.id, featurestore_id)
          expect_status(201)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          training_dataset_id = parsed_json["id"]
          get_training_dataset_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/trainingdatasets/" + training_dataset_id.to_s
          get get_training_dataset_endpoint
          parsed_json = JSON.parse(response.body)
          expect_status(200)
          expect(parsed_json.key?("id")).to be true
          expect(parsed_json.key?("featurestoreName")).to be true
          expect(parsed_json.key?("featurestoreId")).to be true
          expect(parsed_json.key?("name")).to be true
          expect(parsed_json["featurestoreId"] == featurestore_id).to be true
          expect(parsed_json["featurestoreName"] == project.projectname + "_featurestore").to be true
          expect(parsed_json["name"] == training_dataset_name).to be true
          expect(parsed_json["id"] == training_dataset_id).to be true
        end
      end
    end
  end
end
