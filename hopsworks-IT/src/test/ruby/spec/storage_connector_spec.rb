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


describe "On #{ENV['OS']}" do
  after(:all) {clean_all_test_projects(spec: "storage_connector")}

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

      it "should be able to share access keys for s3 connector : for members belonging to same project" do
        setVar("aws_instance_role", "false")
        project = get_project
        project_id = project.id
        create_session(project[:username], "Pass123")
        featurestore_id = get_featurestore_id(project.id)
        json_result, connector_name = create_s3_connector_without_encryption(project.id, featurestore_id, bucket:
            "testbucket")

        parsed_json = JSON.parse(json_result)
        member = create_user
        connector_id = parsed_json["id"]
        add_member_to_project(@project, member[:email], "Data scientist")
        reset_session
        create_session(member[:email],"Pass123")

        json_result1 = get "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/featurestores/" + featurestore_id
                                                                                                          .to_s +
                               "/storageconnectors/S3/" + connector_id.to_s
        parsed_json1 = JSON.parse(json_result1)
        expect_status_details(200)
        expect(parsed_json1["id"] == connector_id).to be true
        setVar("aws_instance_role", "false")
      end

      it "should be able to share s3 connector between shared projects" do
        setVar("aws_instance_role", "false")
        project = get_project
        project_id = project.id
        create_session(project[:username], "Pass123")
        featurestore_id = get_featurestore_id(project.id)
        json_result, connector_name = create_s3_connector_without_encryption(project.id, featurestore_id, bucket:
            "testbucket")

        parsed_json = JSON.parse(json_result)
        connector_id = parsed_json["id"]
        reset_session
        #create another project
        projectname = "project_#{short_random_id}"
        project1 = create_project_by_name(projectname)
        reset_session
        # logi with user for project and share dataset
        create_session(project[:username], "Pass123")
        featurestore = "#{@project[:projectname].downcase}_featurestore.db"
        share_dataset(@project, featurestore, project1[:projectname], permission: "EDITABLE", datasetType: "&type=FEATURESTORE")
        reset_session
        #login with user for project 1 and accept dataset
        create_session(project1[:username],"Pass123")
        accept_dataset(project1, "#{@project[:projectname]}::#{featurestore}", datasetType: "&type=FEATURESTORE")
        json_result1 = get "#{ENV['HOPSWORKS_API']}/project/" + project1.id.to_s + "/featurestores/" +
                               featurestore_id.to_s + "/storageconnectors/S3/" + connector_id.to_s
        parsed_json1 = JSON.parse(json_result1)
        expect_status_details(200)
        expect(parsed_json1["id"] == connector_id).to be true
        setVar("aws_instance_role", "false")
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
        expect_status(200)
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
        expect_status(200)
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
        expect_status(200)
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

      it "should be able to update S3 connector in the featurestore: provide different connector name" do
        setVar("aws_instance_role", "false")
        project = get_project
        create_session(project[:username], "Pass123")
        featurestore_id = get_featurestore_id(project.id)
        s3_connector_name = "s3_connector_#{random_id}"
        with_access_keys = true
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

        json_result2, connector_name2 = update_s3_connector(project.id, featurestore_id, connector_id,
                                                            s3_connector_name, with_access_keys, bucket:
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

      it "should be able to update S3 connector in the featurestore: provide same connector name" do
        setVar("aws_instance_role", "false")
        project = get_project
        create_session(project[:username], "Pass123")
        featurestore_id = get_featurestore_id(project.id)
        with_access_keys = true
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
        s3_connector_name = connector_name1
        json_result2, connector_name2 = update_s3_connector(project.id, featurestore_id, connector_id,
                                                            s3_connector_name, with_access_keys, bucket:
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

      it "should be able to update S3 connector in the featurestore when IAM Role is set to true" do
        setVar("aws_instance_role", "false")
        project = get_project
        create_session(project[:username], "Pass123")
        featurestore_id = get_featurestore_id(project.id)
        s3_connector_name = "s3_connector_#{random_id}"
        with_access_keys = false
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
        # reset the session and now login with aws instance role
        reset_session
        setVar("aws_instance_role", "true")
        create_session(project[:username], "Pass123")
        connector_id = parsed_json1["id"]
        # provide a different connector name when updating
        json_result2, connector_name2 = update_s3_connector(project.id, featurestore_id, connector_id,
                                                            s3_connector_name, with_access_keys, bucket:
                                                                "testbucket2")
        parsed_json2 = JSON.parse(json_result2)

        json_result3 = get "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id
                                                                                                          .to_s +
                               "/storageconnectors/S3/" + connector_id.to_s
        expect_status(200)
        parsed_json3 = JSON.parse(json_result3)
        expect(connector_name2 != connector_name1).to be true
        expect(parsed_json3["accessKey"] == parsed_json1["accessKey"]).to be true
        expect(parsed_json3["secretKey"] == parsed_json1["secretKey"]).to be true
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

      it "online storage connector connection string should contain the IP of the mysql" do
        # Storage connector looks like this: [project name]_[username]_onlinefeaturestore
        connector_name = "#{@project['projectname']}_#{@user['username']}_onlinefeaturestore"
        featurestore_id = get_featurestore_id(@project['id'])
        connector = get_jdbc_storate_connector(@project['id'], featurestore_id, connector_name)

        expect(connector['connectionString']).to match(/jdbc:mysql:\/\/\d{1,3}\.\d{1,3}\.\d{1,3}.\d{1,3}/)
      end

      it "online storage connector connection string should be stored with consul name in the database" do
        # Storage connector looks like this: [project name]_[username]_onlinefeaturestore
        connector_name = "#{@project['projectname']}_#{@user['username']}_onlinefeaturestore"
        connector_db = FeatureStoreJDBCConnector.find_by(name: connector_name)

        expect(connector_db.connection_string).to start_with("jdbc:mysql://onlinefs.mysql.service.consul")
      end
    end
  end
end
