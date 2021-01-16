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

      after :each do
        setVar("aws_instance_role", "false")
        create_session(@project[:username], "Pass123")
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
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, connector_name = create_s3_connector(project.id, featurestore_id,
                                                          bucket: "testbucket",
                                                          access_key: "access_key",
                                                          secret_key: "secret_key")

        parsed_json = JSON.parse(json_result)
        expect_status(201)
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
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, _ = create_s3_connector(project.id, featurestore_id, bucket: "testbucket")
        parsed_json = JSON.parse(json_result)
        expect_status(400)
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
        json_result, connector_name = create_s3_connector(project.id, featurestore_id, bucket: "testbucket")
        parsed_json = JSON.parse(json_result)
        expect_status(201)
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
        setVar("aws_instance_role", "false")
        create_session(project[:username], "Pass123")
      end

      it "should be able to add s3 connector to the featurestore with encryption algorithm but no key" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, connector_name = create_s3_connector(project.id, featurestore_id,
                                                          encryption_algorithm: "AES256",
                                                          secret_key: "test", access_key: "test",
                                                          bucket: "testbucket")
        parsed_json = JSON.parse(json_result)
        expect_status(201)
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
        expect(parsed_json["serverEncryptionAlgorithm"] == "AES256").to be true
        expect(parsed_json["serverEncryptionKey"] == nil).to be true
      end

      it "should be able to add s3 connector to the featurestore with encryption algorithm and with encryption key" do
        project = get_project
        encryption_algorithm = "SSE-KMS"
        encryption_key = "Test"
        featurestore_id = get_featurestore_id(project.id)
        json_result, connector_name = create_s3_connector(project.id, featurestore_id,
                                                          encryption_algorithm: encryption_algorithm,
                                                          encryption_key: encryption_key,
                                                          access_key: "test", secret_key: "test",
                                                          bucket: "testbucket")
        parsed_json = JSON.parse(json_result)
        expect_status(201)
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
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, _ = create_s3_connector(project.id, featurestore_id, access_key: "test", secret_key: "test", bucket: "testbucket")

        parsed_json = JSON.parse(json_result)
        member = create_user
        connector_name = parsed_json["name"]
        add_member_to_project(@project, member[:email], "Data scientist")
        reset_session
        create_session(member[:email],"Pass123")

        json_result1 = get "#{ENV['HOPSWORKS_API']}/project/#{project.id}/featurestores/#{featurestore_id}/storageconnectors/#{connector_name}"
        parsed_json1 = JSON.parse(json_result1)
        expect_status_details(200)
        expect(parsed_json1["name"] == connector_name).to be true
        expect(parsed_json1["accessKey"]).to eql("test")
        expect(parsed_json1["secretKey"]).to eql("test")
      end

      it "should be able to share s3 connector between shared projects - without sharing the secret key" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, _ = create_s3_connector(project.id, featurestore_id, access_key: "test",
                                             secret_key: "test", bucket: "testbucket")

        parsed_json = JSON.parse(json_result)
        connector_name = parsed_json["name"]
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

        json_result1 = get "#{ENV['HOPSWORKS_API']}/project/#{project1.id}/featurestores/#{featurestore_id}/storageconnectors/#{connector_name}"
        expect_status_details(200)
        parsed_json1 = JSON.parse(json_result1)
        expect(parsed_json1["name"] == connector_name).to be true
        expect(parsed_json1["accessKey"]).to be nil
      end

      it "should not be able to add s3 connector to the featurestore with wrong encryption algorithm" do
        project = get_project
        with_encryption_key = true;
        featurestore_id = get_featurestore_id(project.id)
        json_result, _ = create_s3_connector(project.id, featurestore_id, encryption_algorithm: "WRONG-ALGORITHM",
                                             access_key: "test", secret_key: "test", bucket: "testbucket")
        parsed_json = JSON.parse(json_result)
        expect_status(400)
        expect(parsed_json.key?("errorCode")).to be true
        expect(parsed_json.key?("errorMsg")).to be true
        expect(parsed_json.key?("usrMsg")).to be true
        expect(parsed_json["errorCode"] == 270104).to be true
      end

      it "should not be able to add s3 connector to the featurestore with encryption key provided but no encryption algorithm" do
        project = get_project
        encryption_algorithm = ""
        encryption_key = "Test"
        featurestore_id = get_featurestore_id(project.id)
        json_result, _ = create_s3_connector(project.id, featurestore_id,
                                             encryption_algorithm: encryption_algorithm,
                                             encryption_key: encryption_key,
                                             access_key: "test", secret_key: "test",
                                             bucket: "testbucket")

        parsed_json = JSON.parse(json_result)
        expect_status(400)
        expect(parsed_json.key?("errorCode")).to be true
        expect(parsed_json.key?("errorMsg")).to be true
        expect(parsed_json.key?("usrMsg")).to be true
        expect(parsed_json["errorCode"] == 270104).to be true
      end

      it "should be not able to add s3 connector to the featurestore without access key" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, _ = create_s3_connector(project.id, featurestore_id, secret_key: "test", bucket: "testbucket")
        parsed_json = JSON.parse(json_result)
        expect_status(400)
        expect(parsed_json.key?("errorCode")).to be true
        expect(parsed_json.key?("errorMsg")).to be true
        expect(parsed_json.key?("usrMsg")).to be true
      end

      it "should not be able to add s3 connector to the featurestore without specifying a bucket" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, _ = create_s3_connector(project.id, featurestore_id,
                                             access_key: "test", secret_key: "test", bucket: nil)
        parsed_json = JSON.parse(json_result)
        expect_status(400)
        expect(parsed_json.key?("errorCode")).to be true
        expect(parsed_json.key?("errorMsg")).to be true
        expect(parsed_json.key?("usrMsg")).to be true
        expect(parsed_json["errorCode"] == 270034).to be true
      end

      it "should be able to add jdbc connector to the featurestore" do
        project = get_project
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
        featurestore_id = get_featurestore_id(project.id)
        json_result, _ = create_jdbc_connector(project.id, featurestore_id, connectionString: nil)
        parsed_json = JSON.parse(json_result)
        expect_status(400)
        expect(parsed_json.key?("errorCode")).to be true
        expect(parsed_json.key?("errorMsg")).to be true
        expect(parsed_json.key?("usrMsg")).to be true
        expect(parsed_json["errorCode"] == 270032).to be true
      end

      it "should be able to delete a hopsfs connector from the featurestore" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, _ = create_hopsfs_connector(project.id, featurestore_id, datasetName: "Resources")
        parsed_json = JSON.parse(json_result)
        expect_status(201)
        connector_name = parsed_json["name"]
        delete_connector_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project.id}/featurestores/#{featurestore_id}/storageconnectors/#{connector_name}"
        delete delete_connector_endpoint
        expect_status(200)
      end

      it "should be able to delete a s3 connector from the featurestore" do
        project = get_project
        create_session(project[:username], "Pass123")
        featurestore_id = get_featurestore_id(project.id)
        json_result, connector_name = create_s3_connector(project.id, featurestore_id, access_key: "test",
                                                       secret_key: "test", bucket: "testbucket")
        parsed_json = JSON.parse(json_result)
        expect_status(201)
        connector_name = parsed_json["name"]
        delete_connector_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project.id}/featurestores/#{featurestore_id}/storageconnectors/#{connector_name}"
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
        connector_name = parsed_json["name"]
        delete_connector_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project.id}/featurestores/#{featurestore_id}/storageconnectors/#{connector_name}"
        delete delete_connector_endpoint
        expect_status(200)
      end

      it "should be able to update hopsfs connector in the featurestore" do
        project = get_project
        create_session(project[:username], "Pass123")
        featurestore_id = get_featurestore_id(project.id)
        json_result1, connector_name = create_hopsfs_connector(project.id, featurestore_id, datasetName: "Resources")
        expect_status(201)

        json_result2, _ = update_hopsfs_connector(project.id, featurestore_id, connector_name, datasetName: "Experiments")
        parsed_json2 = JSON.parse(json_result2)
        expect(parsed_json2.key?("id")).to be true
        expect(parsed_json2.key?("name")).to be true
        expect(parsed_json2.key?("description")).to be true
        expect(parsed_json2.key?("storageConnectorType")).to be true
        expect(parsed_json2.key?("featurestoreId")).to be true
        expect(parsed_json2.key?("datasetName")).to be true
        expect(parsed_json2.key?("hopsfsPath")).to be true
        expect(parsed_json2["name"] == connector_name).to be true
        expect(parsed_json2["storageConnectorType"] == "HOPSFS").to be true
        expect(parsed_json2["datasetName"] == "Experiments").to be true
      end

      it "should fail to update connector name" do
        project = get_project
        s3_connector_name = "s3_connector_#{random_id}"
        featurestore_id = get_featurestore_id(project.id)
        _, connector_name = create_s3_connector(project.id, featurestore_id,
                                                           encryption_algorithm: "AES256",
                                                           access_key: "test", secret_key: "test",
                                                           bucket: "testbucket")
        expect_status(201)

        update_s3_connector(project.id, featurestore_id, s3_connector_name,
                            access_key: "testdifferent", secret_key: "test",
                            bucket: "testbucket1")
        expect_status_details(404)
        json_result = get_storage_connector(project.id, featurestore_id, connector_name)
        expect_status_details(200)
        json_body = JSON.parse(json_result)
        expect(json_body["name"]).to eql(connector_name)
      end

      it "should be able to update S3 connector in the featurestore: provide same connector name" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result1, connector_name = create_s3_connector(project.id, featurestore_id,
                                                            encryption_algorithm: "AES256",
                                                            access_key: "test", secret_key: "test",
                                                            bucket: "testbucket")

        parsed_json1 = JSON.parse(json_result1)
        expect_status(201)

        json_result2, _ = update_s3_connector(project.id, featurestore_id, connector_name,
                                                            access_key: "testdifferent", secret_key: "test",
                                                            bucket: "testbucket2")
        parsed_json2 = JSON.parse(json_result2)

        expect(parsed_json2.key?("id")).to be true
        expect(parsed_json2.key?("name")).to be true
        expect(parsed_json2.key?("description")).to be true
        expect(parsed_json2.key?("storageConnectorType")).to be true
        expect(parsed_json2.key?("featurestoreId")).to be true
        expect(parsed_json2.key?("bucket")).to be true
        expect(parsed_json2.key?("secretKey")).to be true
        expect(parsed_json2["name"] == connector_name).to be true
        expect(parsed_json2["storageConnectorType"] == "S3").to be true
        expect(parsed_json2["bucket"] == "testbucket2").to be true
        expect(parsed_json2["accessKey"]).to eql("testdifferent")
      end

      it "should be able to update JDBC connector in the featurestore" do
        project = get_project
        create_session(project[:username], "Pass123")
        featurestore_id = get_featurestore_id(project.id)
        json_result1, connector_name = create_jdbc_connector(project.id, featurestore_id, connectionString: "jdbc://test2")
        expect_status(201)
        json_result2 = update_jdbc_connector(project.id, featurestore_id, connector_name, connectionString: "jdbc://test3")
        parsed_json2 = JSON.parse(json_result2)
        expect(parsed_json2.key?("id")).to be true
        expect(parsed_json2.key?("name")).to be true
        expect(parsed_json2.key?("description")).to be true
        expect(parsed_json2.key?("storageConnectorType")).to be true
        expect(parsed_json2.key?("featurestoreId")).to be true
        expect(parsed_json2.key?("connectionString")).to be true
        expect(parsed_json2.key?("arguments")).to be true
        expect(parsed_json2["name"] == connector_name).to be true
        expect(parsed_json2["storageConnectorType"] == "JDBC").to be true
        expect(parsed_json2["connectionString"] == "jdbc://test3").to be true
      end

      it "online storage connector connection string should contain the IP of the mysql" do
        # Storage connector looks like this: [project name]_[username]_onlinefeaturestore
        connector_name = "#{@project['projectname']}_#{@user['username']}_onlinefeaturestore"
        featurestore_id = get_featurestore_id(@project['id'])
        connector_json = get_storage_connector(@project['id'], featurestore_id, connector_name)
        connector = JSON.parse(connector_json)

        expect(connector['connectionString']).to match(/jdbc:mysql:\/\/\d{1,3}\.\d{1,3}\.\d{1,3}.\d{1,3}/)
      end

      it "online storage connector connection string should be stored with consul name in the database" do
        # Storage connector looks like this: [project name]_[username]_onlinefeaturestore
        connector_name = "#{@project['projectname']}_#{@user['username']}_onlinefeaturestore"
        connector_db = FeatureStoreConnector.find_by(name: connector_name)
        jdbc_connector_db = FeatureStoreJDBCConnector.find_by(id: connector_db.jdbc_id)

        expect(jdbc_connector_db[:connection_string]).to start_with("jdbc:mysql://onlinefs.mysql.service.consul")
      end
      
      it "online storage connector should contain the driver class and isolationLevel" do
        connector_name = "#{@project['projectname']}_#{@user['username']}_onlinefeaturestore"
        featurestore_id = get_featurestore_id(@project['id'])
        connector_json = get_storage_connector(@project['id'], featurestore_id, connector_name)
        connector = JSON.parse(connector_json)

        arguments_hash = Hash[]
        connector['arguments'].split(",").each {|arg|
          arg_split = arg.split("=")
          arguments_hash[arg_split[0]] = arg_split[1]
        }
        expect(arguments_hash['driver']).to eql("com.mysql.cj.jdbc.Driver")
        expect(arguments_hash['isolationLevel']).to eql("NONE")
      end
    end
  end
end
