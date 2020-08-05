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

require 'uri'

describe "On #{ENV['OS']}" do
  after(:all) {clean_all_test_projects}
  describe 'featurestore' do
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
          expect(parsed_json[0]["featurestoreName"] == project.projectname.downcase + "_featurestore").to be true
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
          expect(parsed_json["featurestoreName"] == project.projectname.downcase + "_featurestore").to be true
        end

        it "should be able to get a featurestore with a particular name" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          featurestore_name = project.projectname.downcase + "_featurestore"
          get_project_featurestore_with_name = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s +
              "/featurestores/" + featurestore_name.to_s
          get get_project_featurestore_with_name
          parsed_json = JSON.parse(response.body)
          expect_status(200)
          expect(parsed_json.key?("projectName")).to be true
          expect(parsed_json.key?("featurestoreId")).to be true
          expect(parsed_json["projectName"] == project.projectname).to be true
          expect(parsed_json["featurestoreId"] == featurestore_id).to be true
        end

        it "should be able to get shared feature stores" do
          project = get_project
          projectname = "project_#{short_random_id}"
          second_project = create_project_by_name(projectname)
          share_dataset(second_project, "#{projectname}_featurestore.db", @project['projectname'], "&type=FEATURESTORE")

          list_project_featurestores_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{@project['id']}/featurestores"
          get list_project_featurestores_endpoint
          json_body = JSON.parse(response.body)
          expect_status(200)
          # The dataset has not been accepted yet, so it should not be returned in the feature store list
          expect(json_body.length == 1)
          project_featurestore = json_body.select {
             |d| d["featurestoreName"] == "#{project.projectname.downcase}_featurestore"  }
          expect(project_featurestore).to be_present
          second_featurestore = json_body.select {
            |d| d["featurestoreName"] == "#{projectname}_featurestore"  }
          expect(second_featurestore.length).to be 0

          accept_dataset(@project, "#{projectname}_featurestore.db", "&type=FEATURESTORE")

          list_project_featurestores_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{@project['id']}/featurestores"
          get list_project_featurestores_endpoint
          json_body = JSON.parse(response.body)
          expect_status(200)
          # The dataset has been accepted, so it should return the second feature store as well
          expect(json_body.length == 2)
          project_featurestore = json_body.select {
             |d| d["featurestoreName"] == "#{project.projectname.downcase}_featurestore"  }
          expect(project_featurestore).to be_present
          second_featurestore = json_body.select {
            |d| d["featurestoreName"] == "#{projectname}_featurestore"  }
          expect(second_featurestore).to be_present

          get_shared_featurestore_with_name = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s +
              "/featurestores/" + "#{projectname}_featurestore"
          get get_shared_featurestore_with_name
          parsed_json = JSON.parse(response.body)
          expect_status(200)
          expect(parsed_json.key?("projectName")).to be true
          expect(parsed_json.key?("featurestoreId")).to be true
          expect(parsed_json["projectName"] == projectname).to be true
          expect(parsed_json["featurestoreName"] == "#{projectname}_featurestore").to be true
        end
      end
    end

    describe "Create, delete and update operations on storage connectors in a specific featurestore" do
      context 'with valid project, featurestore service enabled' do
        before :all do
          with_valid_project
        end

        it "should be able to add hopsfs connector to the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, connector_name = create_hopsfs_connector(project.id, featurestore_id, datasetName: "Resources")
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          expect(parsed_json.key?("id")).to be true
          expect(parsed_json.key?("name")).to be true
          expect(parsed_json.key?("description")).to be true
          expect(parsed_json.key?("storageConnectorType")).to be true
          expect(parsed_json.key?("featurestoreId")).to be true
          expect(parsed_json.key?("datasetName")).to be true
          expect(parsed_json.key?("hopsfsPath")).to be true
          expect(parsed_json["name"] == connector_name).to be true
          expect(parsed_json["storageConnectorType"] == "HOPSFS").to be true
          expect(parsed_json["datasetName"] == "Resources").to be true
        end

        it "should not be able to add hopsfs connector without a valid dataset" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, connector_name = create_hopsfs_connector(project.id, featurestore_id, datasetName: "-")
          parsed_json = JSON.parse(json_result)
          expect_status(400)
          expect(parsed_json.key?("errorCode")).to be true
          expect(parsed_json.key?("errorMsg")).to be true
          expect(parsed_json.key?("usrMsg")).to be true
          expect(parsed_json["errorCode"] == 270037).to be true
        end

        it "should be able to add s3 connector to the featurestore without encryption information" do
          setVar("aws_instance_role", "false")
          project = get_project
          create_session(project[:username], "Pass123")
          featurestore_id = get_featurestore_id(project.id)
          json_result, connector_name = create_s3_connector_without_encryption(project.id, featurestore_id, bucket:
              "testbucket")

          parsed_json = JSON.parse(json_result)
          expect_status(201)
          setVar("aws_instance_role", "false")
          expect(parsed_json.key?("id")).to be true
          expect(parsed_json.key?("name")).to be true
          expect(parsed_json.key?("description")).to be true
          expect(parsed_json.key?("storageConnectorType")).to be true
          expect(parsed_json.key?("featurestoreId")).to be true
          expect(parsed_json.key?("bucket")).to be true
          expect(parsed_json.key?("secretKey")).to be true
          expect(parsed_json.key?("accessKey")).to be true
          expect(parsed_json["name"] == connector_name).to be true
          expect(parsed_json["storageConnectorType"] == "S3").to be true
          expect(parsed_json["bucket"] == "testbucket").to be true
        end

        it "should not be able to add s3 connector without providing the access and secret key if the IAM Role is set to false" do
          setVar("aws_instance_role", "false")
          project = get_project
          create_session(project[:username], "Pass123")
          featurestore_id = get_featurestore_id(project.id)
          access_key = nil
          secret_key = nil
          with_access_and_secret_key = false
          json_result, connector_name = create_s3_connector_with_or_without_access_key_and_secret_key(project.id, featurestore_id,
                                                                               with_access_and_secret_key,
                                                                               access_key, secret_key,
                                                                               bucket: "testbucket")
          parsed_json = JSON.parse(json_result)
          expect_status(400)
          setVar("aws_instance_role", "false")
          expect(parsed_json.key?("errorCode")).to be true
          expect(parsed_json.key?("errorMsg")).to be true
          expect(parsed_json.key?("usrMsg")).to be true
          expect(parsed_json["errorCode"] == 270035 || parsed_json["errorCode"] == 270036).to be true
        end

        it "should be able to add s3 connector without providing the access and secret key if the IAM Role is set to true" do
          setVar("aws_instance_role", "true")
          project = get_project
          create_session(project[:username], "Pass123")
          featurestore_id = get_featurestore_id(project.id)
          access_key = nil
          secret_key = nil
          with_access_and_secret_key = false
          json_result, connector_name = create_s3_connector_with_or_without_access_key_and_secret_key(project.id, featurestore_id,
                                                                                                      with_access_and_secret_key,
                                                                                                      access_key, secret_key,
                                                                                                      bucket: "testbucket")
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          setVar("aws_instance_role", "false")
          expect(parsed_json.key?("id")).to be true
          expect(parsed_json.key?("name")).to be true
          expect(parsed_json.key?("description")).to be true
          expect(parsed_json.key?("storageConnectorType")).to be true
          expect(parsed_json.key?("featurestoreId")).to be true
          expect(parsed_json.key?("bucket")).to be true
          expect(parsed_json["secretKey"] == nil).to be true
          expect(parsed_json["accessKey"] == nil).to be true
          expect(parsed_json["name"] == connector_name).to be true
          expect(parsed_json["storageConnectorType"] == "S3").to be true
          expect(parsed_json["bucket"] == "testbucket").to be true
        end

        it "should be able to add s3 connector to the featurestore with encryption algorithm but no key" do
          setVar("aws_instance_role", "false")
          project = get_project
          create_session(project[:username], "Pass123")
          encryption_algorithm = "AES256"
          encryption_key = nil
          access_key = "test"
          secret_key = "test"
          with_encryption_key = false;
          featurestore_id = get_featurestore_id(project.id)
          json_result, connector_name = create_s3_connector_with_encryption(project.id, featurestore_id, with_encryption_key,
                                                            encryption_algorithm, encryption_key, secret_key,
                                                                            access_key, bucket: "testbucket")
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          setVar("aws_instance_role", "false")
          expect(parsed_json.key?("id")).to be true
          expect(parsed_json.key?("name")).to be true
          expect(parsed_json.key?("description")).to be true
          expect(parsed_json.key?("storageConnectorType")).to be true
          expect(parsed_json.key?("featurestoreId")).to be true
          expect(parsed_json.key?("bucket")).to be true
          expect(parsed_json.key?("secretKey")).to be true
          expect(parsed_json.key?("accessKey")).to be true
          expect(parsed_json["name"] == connector_name).to be true
          expect(parsed_json["storageConnectorType"] == "S3").to be true
          expect(parsed_json["bucket"] == "testbucket").to be true
          expect(parsed_json["serverEncryptionAlgorithm"] == encryption_algorithm).to be true
          expect(parsed_json["serverEncryptionKey"] == nil).to be true
        end

        it "should be able to add s3 connector to the featurestore with encryption algorithm and with encryption key" do
          setVar("aws_instance_role", "false")
          project = get_project
          create_session(project[:username], "Pass123")
          encryption_algorithm = "SSE-KMS"
          encryption_key = "Test"
          access_key = "test"
          secret_key = "test"
          with_encryption_key = true;
          featurestore_id = get_featurestore_id(project.id)
          json_result, connector_name = create_s3_connector_with_encryption(project.id, featurestore_id, with_encryption_key,
                                                            encryption_algorithm, encryption_key,access_key,
                                                                            secret_key, bucket: "testbucket")
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          setVar("aws_instance_role", "false")
          expect(parsed_json.key?("id")).to be true
          expect(parsed_json.key?("name")).to be true
          expect(parsed_json.key?("description")).to be true
          expect(parsed_json.key?("storageConnectorType")).to be true
          expect(parsed_json.key?("featurestoreId")).to be true
          expect(parsed_json.key?("bucket")).to be true
          expect(parsed_json.key?("secretKey")).to be true
          expect(parsed_json.key?("accessKey")).to be true
          expect(parsed_json["name"] == connector_name).to be true
          expect(parsed_json["storageConnectorType"] == "S3").to be true
          expect(parsed_json["bucket"] == "testbucket").to be true
          expect(parsed_json["serverEncryptionAlgorithm"] == encryption_algorithm).to be true
          expect(parsed_json["serverEncryptionKey"] == encryption_key).to be true
        end

        it "should not be able to add s3 connector to the featurestore with wrong encryption algorithm" do
          setVar("aws_instance_role", "false")
          project = get_project
          create_session(project[:username], "Pass123")
          encryption_algorithm = "WRONG-ALGORITHM"
          encryption_key = "Test"
          access_key = "test"
          secret_key = "test"
          with_encryption_key = true;
          featurestore_id = get_featurestore_id(project.id)
          json_result, connector_name = create_s3_connector_with_encryption(project.id, featurestore_id, with_encryption_key,
                                                                            encryption_algorithm, encryption_key,
                                                                            access_key, secret_key,
                                                                            bucket: "testbucket")
          parsed_json = JSON.parse(json_result)
          expect_status(400)
          setVar("aws_instance_role", "false")
          setVar("aws_instance_role", "false")
          expect(parsed_json.key?("errorCode")).to be true
          expect(parsed_json.key?("errorMsg")).to be true
          expect(parsed_json.key?("usrMsg")).to be true
          expect(parsed_json["errorCode"] == 270104).to be true
        end

        it "should not be able to add s3 connector to the featurestore with encryption key provided but no
        encryption algorithm" do
          setVar("aws_instance_role", "false")
          project = get_project
          create_session(project[:username], "Pass123")
          encryption_algorithm = ""
          encryption_key = "Test"
          access_key = "test"
          secret_key = "test"
          with_encryption_key = true;
          featurestore_id = get_featurestore_id(project.id)
          json_result, connector_name = create_s3_connector_with_encryption(project.id, featurestore_id, with_encryption_key,
                                                                            encryption_algorithm, encryption_key,
                                                                            access_key, secret_key,
                                                                            bucket: "testbucket")
          parsed_json = JSON.parse(json_result)
          expect_status(400)
          setVar("aws_instance_role", "false")
          setVar("aws_instance_role", "false")
          expect(parsed_json.key?("errorCode")).to be true
          expect(parsed_json.key?("errorMsg")).to be true
          expect(parsed_json.key?("usrMsg")).to be true
          expect(parsed_json["errorCode"] == 270104).to be true
        end

        it "should be not able to add s3 connector to the featurestore with wrong server key and access key pair" do
          setVar("aws_instance_role", "false")
          project = get_project
          create_session(project[:username], "Pass123")
          encryption_algorithm = "SSE-KMS"
          encryption_key = "Test"
          secret_key = "test"
          access_key = nil
          with_encryption_key = true;
          featurestore_id = get_featurestore_id(project.id)
          json_result, connector_name = create_s3_connector_with_encryption(project.id, featurestore_id, with_encryption_key,
                                                                            encryption_algorithm, encryption_key,
                                                                            access_key, secret_key,
                                                                            bucket: "testbucket")
          parsed_json = JSON.parse(json_result)
          expect_status(400)
          setVar("aws_instance_role", "false")
          expect(parsed_json.key?("errorCode")).to be true
          expect(parsed_json.key?("errorMsg")).to be true
          expect(parsed_json.key?("usrMsg")).to be true
        end

        it "should not be able to add s3 connector to the featurestore without specifying a bucket" do
          setVar("aws_instance_role", "false")
          project = get_project
          create_session(project[:username], "Pass123")
          featurestore_id = get_featurestore_id(project.id)
          json_result, connector_name = create_s3_connector_without_encryption(project.id, featurestore_id, bucket: nil)
          parsed_json = JSON.parse(json_result)
          expect_status(400)
          setVar("aws_instance_role", "false")
          expect(parsed_json.key?("errorCode")).to be true
          expect(parsed_json.key?("errorMsg")).to be true
          expect(parsed_json.key?("usrMsg")).to be true
          expect(parsed_json["errorCode"] == 270034).to be true
        end

        it "should be able to add jdbc connector to the featurestore" do
          project = get_project
          create_session(project[:username], "Pass123")
          featurestore_id = get_featurestore_id(project.id)
          json_result, connector_name = create_jdbc_connector(project.id, featurestore_id, connectionString: "jdbc://test2")
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          expect(parsed_json.key?("id")).to be true
          expect(parsed_json.key?("name")).to be true
          expect(parsed_json.key?("description")).to be true
          expect(parsed_json.key?("storageConnectorType")).to be true
          expect(parsed_json.key?("featurestoreId")).to be true
          expect(parsed_json.key?("connectionString")).to be true
          expect(parsed_json.key?("arguments")).to be true
          expect(parsed_json["name"] == connector_name).to be true
          expect(parsed_json["storageConnectorType"] == "JDBC").to be true
          expect(parsed_json["connectionString"] == "jdbc://test2").to be true
        end

        it "should not be able to add jdbc connector without a connection string to the featurestore" do
          project = get_project
          create_session(project[:username], "Pass123")
          featurestore_id = get_featurestore_id(project.id)
          json_result, connector_name = create_jdbc_connector(project.id, featurestore_id, connectionString: nil)
          parsed_json = JSON.parse(json_result)
          expect_status(400)
          expect(parsed_json.key?("errorCode")).to be true
          expect(parsed_json.key?("errorMsg")).to be true
          expect(parsed_json.key?("usrMsg")).to be true
          expect(parsed_json["errorCode"] == 270032).to be true
        end

        it "should be able to delete a hopsfs connector from the featurestore" do
          project = get_project
          create_session(project[:username], "Pass123")
          featurestore_id = get_featurestore_id(project.id)
          json_result, connector_name = create_hopsfs_connector(project.id, featurestore_id, datasetName: "Resources")
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          connector_id = parsed_json["id"]
          delete_connector_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/storageconnectors/HOPSFS/" + connector_id.to_s
          delete delete_connector_endpoint
          parsed_json = JSON.parse(response.body)
          expect_status(200)
          expect(parsed_json.key?("id")).to be true
          expect(parsed_json["id"] == connector_id).to be true
        end

        it "should be able to delete a s3 connector from the featurestore" do
          project = get_project
          create_session(project[:username], "Pass123")
          featurestore_id = get_featurestore_id(project.id)
          json_result, connector_name = create_s3_connector_without_encryption(project.id, featurestore_id, bucket:
              "testbucket")
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          connector_id = parsed_json["id"]
          delete_connector_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/storageconnectors/S3/" + connector_id.to_s
          delete delete_connector_endpoint
          parsed_json = JSON.parse(response.body)
          expect_status(200)
          expect(parsed_json.key?("id")).to be true
          expect(parsed_json["id"] == connector_id).to be true
        end

        it "should be able to delete a JDBC connector from the featurestore" do
          project = get_project
          create_session(project[:username], "Pass123")
          featurestore_id = get_featurestore_id(project.id)
          json_result, connector_name = create_jdbc_connector(project.id, featurestore_id, connectionString: "jdbc://test2")
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          connector_id = parsed_json["id"]
          delete_connector_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/storageconnectors/JDBC/" + connector_id.to_s
          delete delete_connector_endpoint
          parsed_json = JSON.parse(response.body)
          expect_status(200)
          expect(parsed_json.key?("id")).to be true
          expect(parsed_json["id"] == connector_id).to be true
        end

        it "should be able to update hopsfs connector in the featurestore" do
          project = get_project
          create_session(project[:username], "Pass123")
          featurestore_id = get_featurestore_id(project.id)
          json_result1, connector_name1 = create_hopsfs_connector(project.id, featurestore_id, datasetName: "Resources")
          parsed_json1 = JSON.parse(json_result1)
          expect_status(201)
          connector_id = parsed_json1["id"]
          json_result2, connector_name2 = update_hopsfs_connector(project.id, featurestore_id, connector_id, datasetName: "Experiments")
          parsed_json2 = JSON.parse(json_result2)
          expect(parsed_json2.key?("id")).to be true
          expect(parsed_json2.key?("name")).to be true
          expect(parsed_json2.key?("description")).to be true
          expect(parsed_json2.key?("storageConnectorType")).to be true
          expect(parsed_json2.key?("featurestoreId")).to be true
          expect(parsed_json2.key?("datasetName")).to be true
          expect(parsed_json2.key?("hopsfsPath")).to be true
          expect(parsed_json2["name"] == connector_name2).to be true
          expect(parsed_json2["storageConnectorType"] == "HOPSFS").to be true
          expect(parsed_json2["datasetName"] == "Experiments").to be true
        end

        it "should be able to update S3 connector in the featurestore" do
          setVar("aws_instance_role", "false")
          project = get_project
          create_session(project[:username], "Pass123")
          featurestore_id = get_featurestore_id(project.id)
          encryption_algorithm = "AES256"
          encryption_key = ""
          access_key = "test"
          secret_key = "test"
          with_encryption_key = true;
          featurestore_id = get_featurestore_id(project.id)
          json_result1, connector_name1 = create_s3_connector_with_encryption(project.id, featurestore_id, with_encryption_key,
                                                                            encryption_algorithm, encryption_key,
                                                                              access_key, secret_key,
                                                                              bucket: "testbucket")

          parsed_json1 = JSON.parse(json_result1)
          expect_status(201)
          connector_id = parsed_json1["id"]

          json_result2, connector_name2 = update_s3_connector(project.id, featurestore_id, connector_id, bucket:
              "testbucket2")
          parsed_json2 = JSON.parse(json_result2)

          expect(parsed_json2.key?("id")).to be true
          expect(parsed_json2.key?("name")).to be true
          expect(parsed_json2.key?("description")).to be true
          expect(parsed_json2.key?("storageConnectorType")).to be true
          expect(parsed_json2.key?("featurestoreId")).to be true
          expect(parsed_json2.key?("bucket")).to be true
          expect(parsed_json2.key?("secretKey")).to be true
          expect(parsed_json2.key?("accessKey")).to be true
          expect(parsed_json2["name"] == connector_name2).to be true
          expect(parsed_json2["storageConnectorType"] == "S3").to be true
          expect(parsed_json2["bucket"] == "testbucket2").to be true
          setVar("aws_instance_role", "false")
        end

        it "should be able to update JDBC connector in the featurestore" do
          project = get_project
          create_session(project[:username], "Pass123")
          featurestore_id = get_featurestore_id(project.id)
          json_result1, connector_name1 = create_jdbc_connector(project.id, featurestore_id, connectionString: "jdbc://test2")
          parsed_json1 = JSON.parse(json_result1)
          expect_status(201)
          connector_id = parsed_json1["id"]
          json_result2, connector_name2 = update_jdbc_connector(project.id, featurestore_id, connector_id, connectionString: "jdbc://test3")
          parsed_json2 = JSON.parse(json_result2)
          expect(parsed_json2.key?("id")).to be true
          expect(parsed_json2.key?("name")).to be true
          expect(parsed_json2.key?("description")).to be true
          expect(parsed_json2.key?("storageConnectorType")).to be true
          expect(parsed_json2.key?("featurestoreId")).to be true
          expect(parsed_json2.key?("connectionString")).to be true
          expect(parsed_json2.key?("arguments")).to be true
          expect(parsed_json2["name"] == connector_name2).to be true
          expect(parsed_json2["storageConnectorType"] == "JDBC").to be true
          expect(parsed_json2["connectionString"] == "jdbc://test3").to be true
        end

      end
    end

    describe "Create, delete and update operations on offline cached featuregroups in a specific featurestore" do

      context 'with valid project, featurestore service enabled' do
        before :all do
          with_valid_project
        end

        it "should be able to add a offline cached featuregroup to the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          expect(parsed_json.key?("id")).to be true
          expect(parsed_json.key?("featurestoreName")).to be true
          expect(parsed_json.key?("name")).to be true
          expect(parsed_json["featurestoreName"] == project.projectname.downcase + "_featurestore").to be true
          expect(parsed_json["name"] == featuregroup_name).to be true
          expect(parsed_json["type"] == "cachedFeaturegroupDTO").to be true
        end

        it "should be able to add a cached featuregroup with non default statistics settings to the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id,
                                                                      default_stats_settings: false)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          expect(parsed_json.key?("id")).to be true
          expect(parsed_json.key?("featurestoreName")).to be true
          expect(parsed_json.key?("name")).to be true
          expect(parsed_json.key?("numBins")).to be true
          expect(parsed_json.key?("numClusters")).to be true
          expect(parsed_json.key?("corrMethod")).to be true
          expect(parsed_json.key?("statisticColumns")).to be true
          expect(parsed_json.key?("featHistEnabled")).to be true
          expect(parsed_json.key?("featCorrEnabled")).to be true
          expect(parsed_json.key?("clusterAnalysisEnabled")).to be true
          expect(parsed_json.key?("descStatsEnabled")).to be true
          expect(parsed_json["featurestoreName"] == project.projectname.downcase + "_featurestore").to be true
          expect(parsed_json["name"] == featuregroup_name).to be true
          expect(parsed_json["type"] == "cachedFeaturegroupDTO").to be true
          expect(parsed_json["numBins"] == 10).to be true
          expect(parsed_json["numClusters"] == 10).to be true
          expect(parsed_json["corrMethod"] == "spearman").to be true
          expect(parsed_json["statisticColumns"].length == 1).to be true
          expect(parsed_json["statisticColumns"][0] == "testfeature").to be true
          expect(parsed_json["featHistEnabled"]).to be false
          expect(parsed_json["featCorrEnabled"]).to be false
          expect(parsed_json["clusterAnalysisEnabled"]).to be false
          expect(parsed_json["descStatsEnabled"]).to be false
        end

        it "should fail when creating the same feature group and version twice" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id,
            featuregroup_name: "duplicatedname")
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id,
            featuregroup_name: "duplicatedname")
          parsed_json = JSON.parse(json_result)
          expect_status(400)
        end

        it "should be able to add a offline cached featuregroup with hive partitioning to the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup_with_partition(project.id, featurestore_id)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          expect(parsed_json.key?("id")).to be true
          expect(parsed_json.key?("featurestoreName")).to be true
          expect(parsed_json.key?("name")).to be true
          expect(parsed_json.key?("featurestoreName")).to be true
          expect(parsed_json["featurestoreName"] == project.projectname.downcase + "_featurestore").to be true
          expect(parsed_json["name"] == featuregroup_name).to be true
          expect(parsed_json["type"] == "cachedFeaturegroupDTO").to be true
        end

        it "should set the feature group permissions to be the same as for the feature store db" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup_with_partition(project.id, featurestore_id)
          path = "/apps/hive/warehouse/#{project['projectname'].downcase}_featurestore.db/#{featuregroup_name}_1"
          ds = get_dataset_stat_checked(@project, path, "&type=FEATURESTORE")
          expect(ds[:attributes][:permission]).to eql("rwxrwx--T")
        end

        it "should not be able to add a cached offline featuregroup to the featurestore with a invalid hive table name" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, features: nil, featuregroup_name: "TEST_!%$1--")
          parsed_json = JSON.parse(json_result)
          expect_status(400)
          expect(parsed_json.key?("errorCode")).to be true
          expect(parsed_json.key?("errorMsg")).to be true
          expect(parsed_json.key?("usrMsg")).to be true
          expect(parsed_json["errorCode"] == 270091).to be true
        end

        it "should not be able to add a cached offline featuregroup to the featurestore with a number only hive table name" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, features: nil,
                                                                      featuregroup_name: "1111")
          parsed_json = JSON.parse(json_result)
          expect_status(400)
          expect(parsed_json.key?("errorCode")).to be true
          expect(parsed_json.key?("errorMsg")).to be true
          expect(parsed_json.key?("usrMsg")).to be true
          expect(parsed_json["errorCode"] == 270091).to be true
        end

        it "should not be able to add a cached offline featuregroup to the featurestore with an empty hive table name" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, features: nil, featuregroup_name: "")
          parsed_json = JSON.parse(json_result)
          expect_status(400)
          expect(parsed_json.key?("errorCode")).to be true
          expect(parsed_json.key?("errorMsg")).to be true
          expect(parsed_json.key?("usrMsg")).to be true
          expect(parsed_json["errorCode"] == 270091).to be true
        end

        it "should not be able to add a cached offline featuregroup to the featurestore with a hive table name containing upper case" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, features: nil, featuregroup_name: "TEST_featuregroup")
          parsed_json = JSON.parse(json_result)
          expect_status(400)
          expect(parsed_json.key?("errorCode")).to be true
          expect(parsed_json.key?("errorMsg")).to be true
          expect(parsed_json.key?("usrMsg")).to be true
          expect(parsed_json["errorCode"] == 270091).to be true
        end

        it "should not be able to add a cached offline featuregroup to the featurestore with a too long hive table name" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, features: nil,
                                                                      featuregroup_name: "a"*65)
          parsed_json = JSON.parse(json_result)
          expect_status(400)
          expect(parsed_json.key?("errorCode")).to be true
          expect(parsed_json.key?("errorMsg")).to be true
          expect(parsed_json.key?("usrMsg")).to be true
          expect(parsed_json["errorCode"] == 270091).to be true
        end

        it "should not be able to add a cached offline featuregroup to the featurestore with an invalid version" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, features: nil,
                                                                      version: -1)
          parsed_json = JSON.parse(json_result)
          expect_status(400)
          expect(parsed_json.key?("errorCode")).to be true
          expect(parsed_json.key?("errorMsg")).to be true
          expect(parsed_json.key?("usrMsg")).to be true
          expect(parsed_json["errorCode"] == 270059).to be true
        end

        it "should be able to add a new cached offline featuregroup without version to the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, features: nil,
                                                                      featuregroup_name: "no_version_fg", version: nil)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          expect(parsed_json["version"] == 1).to be true
        end

        it "should be able to add a new version of an existing cached offline featuregroup without version to the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, features: nil,
                                                                      featuregroup_name: "no_version_fg_add")
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          # add second version
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, features: nil,
                                                                      featuregroup_name: "no_version_fg_add", version: nil)
          parsed_json = JSON.parse(json_result)
          # version should be incremented to 2
          expect(parsed_json["version"] == 2).to be true
        end

        it "should not be able to add a offline cached featuregroup to the featurestore with invalid feature name" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          features = [
              {
                  type: "INT",
                  name: "--",
                  description: "--",
                  primary: true,
                  onlineType: nil,
                  partition: false
              }
          ]
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, features:features)
          parsed_json = JSON.parse(json_result)
          expect(parsed_json.key?("errorCode")).to be true
          expect(parsed_json.key?("errorMsg")).to be true
          expect(parsed_json.key?("usrMsg")).to be true
          expect(parsed_json["errorCode"] == 270040).to be true
        end

        it "should be able to add a feature group without primary key" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          features = [
              {
                  type: "INT",
                  name: "test",
                  description: "--",
                  primary: false
              }
          ]
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, features:features)
          expect_status(201)
        end


        it "should be able to add a offline cached featuregroup to the featurestore with empty feature description" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          features = [
              {
                  type: "INT",
                  name: "test_feat_no_description",
                  description: "",
                  primary: true,
                  onlineType: nil,
                  partition: false
              }
          ]
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, features:features)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          expect(parsed_json["features"].length).to be 1
          expect(parsed_json["features"].first["description"] == "").to be true
        end

        it "should be able to add an offline cached featuregroup with ' and ; in the description'" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          features = [
              {
                  type: "INT",
                  name: "test_feature",
                  description: "this description contains ' and ;'",
                  primary: true,
                  onlineType: nil,
                  partition: false
              }
          ]
          json_result, featuregroup_name =
              create_cached_featuregroup(project.id, featurestore_id,
                                         features:features,
                                         featuregroup_description:"this description contains ' and ;'%*")

              parsed_json = JSON.parse(json_result)
          expect_status(201)
          expect(parsed_json["description"]).to eql("this description contains ' and ;'%*")
          expect(parsed_json["features"].length).to be 1
          expect(parsed_json["features"].first["description"]).to eql("this description contains ' and ;'")
        end

        it "should be able to preview a offline cached featuregroup in the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          featuregroup_id = parsed_json["id"]
          preview_featuregroup_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s + "/preview"
          get preview_featuregroup_endpoint
          parsed_json = JSON.parse(response.body)
          expect_status(200)
        end

        it "should be able to get a feature group based on name and version" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)

          # Create first version
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
          parsed_json = JSON.parse(json_result)
          expect_status(201)

          # Create second version
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, featuregroup_name: featuregroup_name, version: 2)
          parsed_json = JSON.parse(json_result)
          expect_status(201)

          # Get the first version
          get_featuregroup_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project.id}/featurestores/#{featurestore_id}/featuregroups/#{featuregroup_name}?version=1"
          get get_featuregroup_endpoint
          parsed_json = JSON.parse(response.body)
          expect_status(200)
          expect(parsed_json[0]["name"]).to eq featuregroup_name
          expect(parsed_json[0]["version"]).to eq 1

          # Get the second version
          get_featuregroup_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project.id}/featurestores/#{featurestore_id}/featuregroups/#{featuregroup_name}?version=2"
          get get_featuregroup_endpoint
          parsed_json = JSON.parse(response.body)
          expect_status(200)
          expect(parsed_json[0]["name"]).to eq featuregroup_name
          expect(parsed_json[0]["version"]).to eq 2
        end

        it "should be able to get a list of feature group versions based on name" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)

          # Create first version
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
          parsed_json = JSON.parse(json_result)
          expect_status(201)

          # Create second version
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, featuregroup_name: featuregroup_name, version: 2)
          parsed_json = JSON.parse(json_result)
          expect_status(201)

          # Get the list
          get_featuregroup_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project.id}/featurestores/#{featurestore_id}/featuregroups/#{featuregroup_name}"
          get get_featuregroup_endpoint
          parsed_json = JSON.parse(response.body)
          expect_status(200)
          expect(parsed_json.size).to eq 2
        end

        it "should fail to get a feature store by name that does not exists" do
          # Get the first version
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          get_featuregroup_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project.id}/featurestores/#{featurestore_id}/featuregroups/doesnotexists?version=1"
          get get_featuregroup_endpoint
          parsed_json = JSON.parse(response.body)
          expect_status(400)
        end

        it "should be able to get the hive schema of a cached offline featuregroup in the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          featuregroup_id = parsed_json["id"]
          get_featuregroup_schema_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s + "/details"
          get get_featuregroup_schema_endpoint
          parsed_json = JSON.parse(response.body)
          expect_status(200)
          expect(parsed_json.key?("schema")).to be true
        end

        it "should be able to delete a cached featuregroup from the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          featuregroup_id = parsed_json["id"]
          delete_featuregroup_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s
          delete delete_featuregroup_endpoint
          expect_status(200)
        end

        it "should be able to clear the contents of a cached featuregroup in the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          featuregroup_id = parsed_json["id"]
          clear_featuregroup_contents_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s + "/clear"
          post clear_featuregroup_contents_endpoint
          expect_status(200)
        end

        it "should not be able to update the metadata of a cached featuregroup from the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          featuregroup_id = parsed_json["id"]
          featuregroup_version = parsed_json["version"]
          json_result = update_cached_featuregroup_metadata(project.id, featurestore_id, featuregroup_id,
                                                          featuregroup_version)
          parsed_json = JSON.parse(json_result)
          expect_status(400)
          expect(parsed_json.key?("errorCode")).to be true
          expect(parsed_json.key?("errorMsg")).to be true
          expect(parsed_json.key?("usrMsg")).to be true
          expect(parsed_json["errorCode"] == 270093).to be true
        end

        it "should be able to update the statistics settings of a cached featuregroup" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          featuregroup_id = parsed_json["id"]
          featuregroup_version = parsed_json["version"]
          json_result = update_cached_featuregroup_stats_settings(project.id, featurestore_id, featuregroup_id,
                                                                  featuregroup_version)
          parsed_json = JSON.parse(json_result)
          expect_status(200)
          expect(parsed_json["numBins"] == 10).to be true
          expect(parsed_json["numClusters"] == 10).to be true
          expect(parsed_json["corrMethod"] == "spearman").to be true
          expect(parsed_json["statisticColumns"].length == 1).to be true
          expect(parsed_json["statisticColumns"][0] == "testfeature").to be true
          expect(parsed_json["featHistEnabled"]).to be false
          expect(parsed_json["featCorrEnabled"]).to be false
          expect(parsed_json["clusterAnalysisEnabled"]).to be false
          expect(parsed_json["descStatsEnabled"]).to be false
        end

        it "should be able to get schema of shared feature group" do
          project = get_project
          create_session(project[:username], "Pass123")
          projectname = "project_#{short_random_id}"
          # Create a feature group in second project and share it with the first project
          second_project = create_project_by_name(projectname)
          featurestore_id = get_featurestore_id(second_project.id)
          share_dataset(second_project, "#{projectname}_featurestore.db", @project['projectname'], "&type=FEATURESTORE")
          json_result, featuregroup_name = create_cached_featuregroup(second_project.id, featurestore_id)
          featuregroup_id = JSON.parse(json_result)['id']
          accept_dataset(project, "#{projectname}::#{projectname}_featurestore.db", "&type=FEATURESTORE")

          # Create a new user and add it only to the first project
          member = create_user
          add_member_to_project(project, member[:email], "Data scientist")
          create_session(member[:email], "Pass123")

          # The new member should be able to fetch the schema from Hive
          result =
              get "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/featurestores/#{featurestore_id}/featuregroups/#{featuregroup_id}/details"
          expect_status(200)
          parsed_json = JSON.parse(result)
          expect(parsed_json['schema']).to start_with "CREATE TABLE"
        end

        it "should be able to get a data preview of a shared feature group" do
          project = get_project
          create_session(project[:username], "Pass123")
          projectname = "project_#{short_random_id}"
          # Create a feature group in second project and share it with the first project
          second_project = create_project_by_name(projectname)
          featurestore_id = get_featurestore_id(second_project.id)
          share_dataset(second_project, "#{projectname}_featurestore.db", @project['projectname'], "&type=FEATURESTORE")
          json_result, featuregroup_name = create_cached_featuregroup(second_project.id, featurestore_id)
          featuregroup_id = JSON.parse(json_result)['id']
          accept_dataset(project, "#{projectname}::#{projectname}_featurestore.db", "&type=FEATURESTORE")

          # Create a new user and add it only to the first project
          member = create_user
          add_member_to_project(project, member[:email], "Data scientist")
          create_session(member[:email], "Pass123")

          # The new member should be able to fetch the schema from Hive
          get "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/featurestores/#{featurestore_id}/featuregroups/#{featuregroup_id}/preview"
          expect_status(200)
        end
      end
    end


    describe "Create, delete and update operations on on-demand featuregroups in a specific featurestore" do

      context 'with valid project, featurestore service enabled, and a jdbc connector' do
        before :all do
          with_valid_project
          with_jdbc_connector(@project[:id])
        end

        it "should be able to add an on-demand featuregroup to the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector_id = get_jdbc_connector_id
          json_result, featuregroup_name = create_on_demand_featuregroup(project.id, featurestore_id, connector_id)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          expect(parsed_json.key?("id")).to be true
          expect(parsed_json.key?("query")).to be true
          expect(parsed_json.key?("jdbcConnectorId")).to be true
          expect(parsed_json.key?("jdbcConnectorName")).to be true
          expect(parsed_json.key?("features")).to be true
          expect(parsed_json.key?("featurestoreName")).to be true
          expect(parsed_json.key?("name")).to be true
          expect(parsed_json["featurestoreName"] == project.projectname.downcase + "_featurestore").to be true
          expect(parsed_json["name"] == featuregroup_name).to be true
          expect(parsed_json["type"] == "onDemandFeaturegroupDTO").to be true
          expect(parsed_json["jdbcConnectorId"] == connector_id).to be true
        end

        it "should not be able to add an on-demand featuregroup to the featurestore with a name containing upper case letters" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector_id = get_jdbc_connector_id
          json_result, featuregroup_name = create_on_demand_featuregroup(project.id, featurestore_id, connector_id,
                                                                         name: "TEST_ondemand_fg")
          parsed_json = JSON.parse(json_result)
          expect_status(400)
          expect(parsed_json.key?("errorCode")).to be true
          expect(parsed_json.key?("errorMsg")).to be true
          expect(parsed_json.key?("usrMsg")).to be true
          expect(parsed_json["errorCode"] == 270091).to be true
        end

        it "should not be able to add an on-demand featuregroup to the featurestore without a SQL query" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector_id = get_jdbc_connector_id
          json_result, featuregroup_name = create_on_demand_featuregroup(project.id, featurestore_id, connector_id, query: "")
          parsed_json = JSON.parse(json_result)
          expect_status(400)
          expect(parsed_json.key?("errorCode")).to be true
          expect(parsed_json.key?("errorMsg")).to be true
          expect(parsed_json.key?("usrMsg")).to be true
          expect(parsed_json["errorCode"] == 270044).to be true
        end

        it "should be able to delete an on-demand featuregroup from the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector_id = get_jdbc_connector_id
          json_result, featuregroup_name = create_on_demand_featuregroup(project.id, featurestore_id, connector_id)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          featuregroup_id = parsed_json["id"]
          delete_featuregroup_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s
          delete delete_featuregroup_endpoint
          expect_status(200)
        end

        it "should be able to update the metadata (description) of an on-demand featuregroup from the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector_id = get_jdbc_connector_id
          json_result, featuregroup_name = create_on_demand_featuregroup(project.id, featurestore_id, connector_id)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          featuregroup_id = parsed_json["id"]
          featuregroup_version = parsed_json["version"]
          json_result2, featuregroup_name2  = update_on_demand_featuregroup(project.id, featurestore_id,
                                                                            connector_id, featuregroup_id,
                                                                            featuregroup_version, query: nil,
                                                                            featuregroup_name: featuregroup_name,
                                                                            featuregroup_desc: "new description")
          parsed_json2 = JSON.parse(json_result2)
          expect_status(200)
          expect(parsed_json2["version"] == featuregroup_version).to be true
          expect(parsed_json2["description"] == "new description").to be true
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
          get_featuregroups_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups"
          get get_featuregroups_endpoint
          parsed_json = JSON.parse(response.body)
          expect_status(200)
          expect(parsed_json.length == 0).to be true
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
          expect_status(201)
          get get_featuregroups_endpoint
          parsed_json = JSON.parse(response.body)
          expect_status(200)
          expect(parsed_json.length == 1).to be true
          expect(parsed_json[0].key?("id")).to be true
          expect(parsed_json[0].key?("featurestoreName")).to be true
          expect(parsed_json[0].key?("name")).to be true
          expect(parsed_json[0]["featurestoreName"] == project.projectname.downcase + "_featurestore").to be true
          expect(parsed_json[0]["name"] == featuregroup_name).to be true
        end

        it "should be able to get a featuregroup with a particular id" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
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
          expect(parsed_json["featurestoreName"] == project.projectname.downcase + "_featurestore").to be true
          expect(parsed_json["name"] == featuregroup_name).to be true
          expect(parsed_json["id"] == featuregroup_id).to be true
        end
      end
    end

    describe "Create, delete and update operations on online cached featuregroups in a specific featurestore" do

      context 'with valid project, featurestore service enabled, and online feature store enabled' do
        before :all do
          if getVar("featurestore_online_enabled") == false
            skip "Online Feature Store not enabled, skip online featurestore tests"
          end
          with_valid_project
          with_jdbc_connector(@project[:id])
        end

        it "should be able to add a cached featuregroup with online feature serving to the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, online:true)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          expect(parsed_json.key?("id")).to be true
          expect(parsed_json.key?("featurestoreName")).to be true
          expect(parsed_json.key?("onlineEnabled")).to be true
          expect(parsed_json.key?("name")).to be true
          expect(parsed_json["featurestoreName"] == project.projectname.downcase + "_featurestore").to be true
          expect(parsed_json["name"] == featuregroup_name).to be true
          expect(parsed_json["type"] == "cachedFeaturegroupDTO").to be true
        end

        it "should be able to preview a offline featuregroup in the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, _ = create_cached_featuregroup(project.id, featurestore_id, online:true)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          featuregroup_id = parsed_json["id"]
          get "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s + "/preview?storage=offline"
          expect_status(200)
        end

        it "should be able to get a specific partition" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, _ = create_cached_featuregroup_with_partition(project.id, featurestore_id)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          featuregroup_id = parsed_json["id"]
          get "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s + "/preview?storage=offline&partition=testfeature2=1"
          expect_status(200)
        end

        it "should be able to preview a online featuregroup in the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, _ = create_cached_featuregroup(project.id, featurestore_id, online:true)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          featuregroup_id = parsed_json["id"]
          get "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s + "/preview?storage=online"
          expect_status(200)
        end

        it "should be able to limit the number of rows in a preview" do
          project = create_project_by_name_existing_user("online_fs")
          featurestore_id = get_featurestore_id(project.id)
          json_result, _ = create_cached_featuregroup(project.id, featurestore_id, featuregroup_name: 'online_fg', online:true)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          featuregroup_id = parsed_json["id"]

          # add sample ros
          OnlineFg.create(testfeature: 1).save
          OnlineFg.create(testfeature: 2).save

          get "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s + "/preview?storage=online&limit=1"
          expect_status(200)
          parsed_json = JSON.parse(response.body)
          expect(parsed_json['items'].length).to eql 1
        end

        it "should be able to get the MySQL schema of a cached online featuregroup in the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, _ = create_cached_featuregroup(project.id, featurestore_id, online:true)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          featuregroup_id = parsed_json["id"]
          get "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s + "/details"
          expect_status(200)
          parsed_json = JSON.parse(response.body)
          expect(parsed_json.key?("schema")).to be true
        end

        it "should be able to delete a cached online featuregroup from the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, online:true)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          featuregroup_id = parsed_json["id"]
          delete_featuregroup_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s
          delete delete_featuregroup_endpoint
          expect_status(200)
        end

        it "should not be able to update the metadata of a cached online featuregroup from the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, online:true)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          featuregroup_id = parsed_json["id"]
          featuregroup_version = parsed_json["version"]
          json_result = update_cached_featuregroup_metadata(project.id, featurestore_id, featuregroup_id,
                                                      featuregroup_version)
          parsed_json = JSON.parse(json_result)

          expect_status(400)
          expect(parsed_json.key?("errorCode")).to be true
          expect(parsed_json.key?("errorMsg")).to be true
          expect(parsed_json.key?("usrMsg")).to be true
          expect(parsed_json["errorCode"] == 270093).to be true
        end

        it "should be able to enable online serving for a offline cached feature group" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, online:false)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          featuregroup_id = parsed_json["id"]
          featuregroup_version = parsed_json["version"]
          enable_cached_featuregroup_online(project.id, featurestore_id, featuregroup_id, featuregroup_version)
          expect_status(200)
        end

        it "should be able to disable online serving for a online cached feature group" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, online:true)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          featuregroup_id = parsed_json["id"]
          featuregroup_version = parsed_json["version"]
          disable_cached_featuregroup_online(project.id, featurestore_id, featuregroup_id, featuregroup_version)
          expect_status(200)
        end

        it "should be able to get online featurestore JDBC connector" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          get_online_featurestore_connector_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/storageconnectors/onlinefeaturestore"
          get get_online_featurestore_connector_endpoint
          parsed_json = JSON.parse(response.body)
          expect(parsed_json.key?("type")).to be true
          expect(parsed_json.key?("description")).to be true
          expect(parsed_json.key?("featurestoreId")).to be true
          expect(parsed_json.key?("id")).to be true
          expect(parsed_json.key?("name")).to be true
          expect(parsed_json.key?("storageConnectorType")).to be true
          expect(parsed_json.key?("arguments")).to be true
          expect(parsed_json.key?("connectionString")).to be true
          expect(parsed_json["featurestoreId"] == featurestore_id).to be true
          expect(parsed_json["storageConnectorType"] == "JDBC").to be true
          expect(parsed_json["name"]).to include("_onlinefeaturestore")
          expect(parsed_json["connectionString"]).to include("jdbc:mysql:")
          expect(parsed_json["arguments"]).to include("password=")
          expect(parsed_json["arguments"]).to include("user=")
          expect_status(200)
        end
      end
    end

    describe "Create, delete and update operations on hopsfs training datasets in a specific featurestore" do

      context 'with valid project, featurestore service enabled' do
        before :all do
          with_valid_project
        end

        it "should be able to add a hopsfs training dataset to the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
          json_result, training_dataset_name = create_hopsfs_training_dataset(project.id, featurestore_id, connector)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          expect(parsed_json.key?("id")).to be true
          expect(parsed_json.key?("featurestoreName")).to be true
          expect(parsed_json.key?("name")).to be true
          expect(parsed_json.key?("creator")).to be true
          expect(parsed_json.key?("location")).to be true
          expect(parsed_json.key?("version")).to be true
          expect(parsed_json.key?("dataFormat")).to be true
          expect(parsed_json.key?("trainingDatasetType")).to be true
          expect(parsed_json.key?("location")).to be true
          expect(parsed_json.key?("storageConnectorId")).to be true
          expect(parsed_json.key?("storageConnectorName")).to be true
          expect(parsed_json.key?("inodeId")).to be true
          expect(parsed_json.key?("features")).to be true
          expect(parsed_json.key?("seed")).to be true
          expect(parsed_json["featurestoreName"] == project.projectname.downcase + "_featurestore").to be true
          expect(parsed_json["name"] == training_dataset_name).to be true
          expect(parsed_json["trainingDatasetType"] == "HOPSFS_TRAINING_DATASET").to be true
          expect(parsed_json["storageConnectorId"] == connector.id).to be true
          expect(parsed_json["features"].length).to be 2
          expect(parsed_json["seed"] == 1234).to be true


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
                  percentage: 0.8
              },
              {
                  name: "train_split",
                  percentage: 0.2
              }
          ]
          json_result, training_dataset_name = create_hopsfs_training_dataset(project.id, featurestore_id, connector,
                                                                              splits: splits)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          expect(parsed_json.key?("splits")).to be true
          expect(parsed_json["splits"].length).to be 2
        end

        it "should not be able to add a hopsfs training dataset to the featurestore with a non numeric split percentage" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
          split = [{name: "train_split", percentage: "wrong"}]
          json_result, training_dataset_name = create_hopsfs_training_dataset(project.id, featurestore_id, connector, splits: split)
          parsed_json = JSON.parse(json_result)
          expect_status(400)
          expect(parsed_json.key?("errorCode")).to be true
          expect(parsed_json.key?("errorMsg")).to be true
          expect(parsed_json.key?("usrMsg")).to be true
          expect(parsed_json["errorCode"] == 270099).to be true
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
                                                                              splits: splits)
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
          expect(parsed_json["storageConnectorName"] == "#{project['projectname']}_Training_Datasets")
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
                               "&type=DATASET")
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
          expect(parsed_json2.key?("creator")).to be true
          expect(parsed_json2.key?("location")).to be true
          expect(parsed_json2.key?("version")).to be true
          expect(parsed_json2.key?("dataFormat")).to be true
          expect(parsed_json2.key?("trainingDatasetType")).to be true
          expect(parsed_json2.key?("storageConnectorId")).to be true
          expect(parsed_json2.key?("storageConnectorName")).to be true
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

        it "should be able to update the jobs of a training dataset" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
          json_result1, training_dataset_name = create_hopsfs_training_dataset(project.id, featurestore_id, connector)
          parsed_json1 = JSON.parse(json_result1)
          expect_status(201)

          # Create a fake job
          create_sparktour_job(project, "ingestion_job", "jar", nil)

          jobs = [{"jobName" => "ingestion_job"}]

          training_dataset_id = parsed_json1["id"]
          json_result2 = update_hopsfs_training_dataset_metadata(project.id, featurestore_id,
                                                                 training_dataset_id, "tfrecords", connector, jobs: jobs)
          parsed_json2 = JSON.parse(json_result2)
          expect_status(200)

          # make sure the name didn't change
          expect(parsed_json2["jobs"].count).to eql(1)
          expect(parsed_json2["jobs"][0]["jobName"]).to eql("ingestion_job")
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
          expect_status(400)
        end

      end
    end

    describe "Create, delete and update operations on external training datasets in a specific featurestore" do

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
          expect(parsed_json.key?("creator")).to be true
          expect(parsed_json.key?("location")).to be true
          expect(parsed_json.key?("version")).to be true
          expect(parsed_json.key?("dataFormat")).to be true
          expect(parsed_json.key?("trainingDatasetType")).to be true
          expect(parsed_json.key?("description")).to be true
          expect(parsed_json.key?("storageConnectorId")).to be true
          expect(parsed_json.key?("storageConnectorName")).to be true
          expect(parsed_json.key?("features")).to be true
          expect(parsed_json.key?("seed")).to be true
          expect(parsed_json["featurestoreName"] == project.projectname.downcase + "_featurestore").to be true
          expect(parsed_json["name"] == training_dataset_name).to be true
          expect(parsed_json["trainingDatasetType"] == "EXTERNAL_TRAINING_DATASET").to be true
          expect(parsed_json["storageConnectorId"] == connector_id).to be true
          expect(parsed_json["features"].length).to be 2
          expect(parsed_json["seed"] == 1234).to be true
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
          parsed_json = JSON.parse(json_result)
          expect_status(400)
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
                                                                                connector_id, splits: splits)

          parsed_json = JSON.parse(json_result)
          expect_status(201)
          expect(parsed_json.key?("splits")).to be true
          expect(parsed_json["splits"].length).to be 2
        end

        it "should not be able to add an external training dataset to the featurestore with a non numeric split percentage" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector_id = get_s3_connector_id
          splits = [{name: "train_split", percentage: "wrong"}]
          json_result, training_dataset_name = create_external_training_dataset(project.id, featurestore_id,
                                                                                connector_id, splits: splits)
          parsed_json = JSON.parse(json_result)
          expect_status(400)
          expect(parsed_json.key?("errorCode")).to be true
          expect(parsed_json.key?("errorMsg")).to be true
          expect(parsed_json.key?("usrMsg")).to be true
          expect(parsed_json["errorCode"] == 270099).to be true
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
                                                                                connector_id, splits: splits)

          parsed_json = JSON.parse(json_result)
          expect_status(201)
          expect(parsed_json.key?("splits")).to be true
          expect(parsed_json["splits"].length).to be 2
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
          expect(parsed_json2.key?("creator")).to be true
          expect(parsed_json2.key?("location")).to be true
          expect(parsed_json2.key?("version")).to be true
          expect(parsed_json2.key?("dataFormat")).to be true
          expect(parsed_json2.key?("trainingDatasetType")).to be true
          expect(parsed_json2.key?("description")).to be true
          expect(parsed_json2.key?("storageConnectorId")).to be true
          expect(parsed_json2.key?("storageConnectorName")).to be true
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
          json_new_connector, _ = create_s3_connector_without_encryption(project.id, featurestore_id)
          new_connector = JSON.parse(json_new_connector)

          json_result2 = update_external_training_dataset_metadata(project.id, featurestore_id,
                                                                   training_dataset_id, training_dataset_name, "desc",
                                                                   new_connector['id'])
          parsed_json2 = JSON.parse(json_result2)
          expect_status(200)

          # make sure the name didn't change
          expect(parsed_json2["storageConnectorId"]).to be connector_id
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
      end
    end
  end
end
