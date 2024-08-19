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
  after(:all) {clean_all_test_projects(spec: "storage_connector") if defined?(@cleanup) && @cleanup}

  describe "Create, delete and update operations on storage connectors in a specific featurestore" do

    describe "online feature store storage connector" do
      context "with valid project, featurestore service enabled, and with storage connector flags enabled" do
        before :all do
          with_valid_project
          # setup online feature store by creating a online feature group (see HWORKS-919)
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, online:true)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)
        end

        after :each do
          create_session(@project[:username], "Pass123")
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

          arguments_hash = connector['arguments']
          expect(arguments_hash.find{ |item| item['name'] == 'driver' }['value']).to eql("com.mysql.cj.jdbc.Driver")
          expect(arguments_hash.find{ |item| item['name'] == 'isolationLevel' }['value']).to eql("NONE")
        end

        it "should get online storage connector from base project when accessing a shared feature store" do
          project = get_project
          base_featurestore_id = get_featurestore_id(project.id)
          reset_session

          #create another project
          projectname = "project_#{short_random_id}"
          shared_fs_project = create_project_by_name(projectname)
          shared_fs_id = get_featurestore_id(shared_fs_project.id)
          # login with user for project and share dataset
          create_session(shared_fs_project[:username], "Pass123")
          featurestore = "#{shared_fs_project[:projectname].downcase}_featurestore.db"
          share_dataset(shared_fs_project, featurestore, project[:projectname], datasetType: "&type=FEATURESTORE")
          reset_session
          #login with user for shared_fs_project and accept dataset
          create_session(project[:username],"Pass123")
          accept_dataset(project, "#{shared_fs_project[:projectname]}::#{featurestore}", datasetType: "&type=FEATURESTORE")

          base_project_connector = "#{project['projectname']}_#{@user['username']}_onlinefeaturestore"
          connector_name = "onlinefeaturestore"

          #connector from shared fs should use base project connector
          connector_json = get_storage_connector(project['id'], shared_fs_id, connector_name)
          connector = JSON.parse(connector_json)
          expect(connector["name"]).to eql(base_project_connector)

          #connector from base should use base project connector
          connector_json = get_storage_connector(project['id'], base_featurestore_id, connector_name)
          connector = JSON.parse(connector_json)
          expect(connector["name"]).to eql(base_project_connector)
        end

        it "should not be able to delete default online connector from the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector_name = "#{@project['projectname']}_#{@user['username']}_onlinefeaturestore"
          result = get_storage_connector(project.id, featurestore_id, connector_name)
          expect_status_details(200)
          delete_connector_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project.id}/featurestores/#{featurestore_id}/storageconnectors/#{connector_name}"
          delete delete_connector_endpoint
          expect_status_details(400)
        end
      end
    end

    describe "other storage connectors" do
      context "with valid project, featurestore service enabled, and with storage connector flags enabled" do
        before :all do
          with_valid_project
        end

        after :each do
          create_session(@project[:username], "Pass123")
        end

        it "should be able to add hopsfs connector to the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, connector_name = create_hopsfs_connector(project.id, featurestore_id, datasetName: "Resources")
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)
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
          expect_status_details(400)
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
          expect_status_details(201)
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

        it "should be able to add s3 connector without providing the access and secret key or iamRole" do
          project = get_project
          create_session(project[:username], "Pass123")
          featurestore_id = get_featurestore_id(project.id)
          arguments= [
            {
              "name": "key1",
              "value": "option1"
            },
            {
              "name": "key2",
              "value": "option2"
            }
          ]
          json_result, connector_name = create_s3_connector(project.id, featurestore_id, bucket: "testbucket", arguments: arguments)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)
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
          expect(parsed_json['arguments'][0]['name']).to eql('key1')
          expect(parsed_json['arguments'][0]['value']).to eql('option1')
          expect(parsed_json['arguments'][1]['name']).to eql('key2')
          expect(parsed_json['arguments'][1]['value']).to eql('option2')
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
          expect_status_details(201)
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
          expect_status_details(201)
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

        it "should be able to share s3 connector between shared projects - with shared secret key" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, _ = create_s3_connector(project.id, featurestore_id, access_key: "test",
                                              secret_key: "test", bucket: "testbucket")

          parsed_json = JSON.parse(json_result)
          connector_name = parsed_json["name"]
          reset_session
          #create another project
          project1 = create_project
          reset_session
          # logi with user for project and share dataset
          create_session(project[:username], "Pass123")
          featurestore = "#{@project[:projectname].downcase}_featurestore.db"
          share_dataset(@project, featurestore, project1[:projectname], datasetType: "&type=FEATURESTORE")
          reset_session
          #login with user for project 1 and accept dataset
          create_session(project1[:username],"Pass123")
          accept_dataset(project1, "#{@project[:projectname]}::#{featurestore}", datasetType: "&type=FEATURESTORE")

          json_result1 = get "#{ENV['HOPSWORKS_API']}/project/#{project1.id}/featurestores/#{featurestore_id}/storageconnectors/#{connector_name}"
          expect_status_details(200)
          parsed_json1 = JSON.parse(json_result1)
          expect(parsed_json1["name"]).to eql(connector_name)
          expect(parsed_json1["accessKey"]).to eql("test")
        end

        it "should not be able to add s3 connector to the featurestore with wrong encryption algorithm" do
          project = get_project
          with_encryption_key = true;
          featurestore_id = get_featurestore_id(project.id)
          json_result, _ = create_s3_connector(project.id, featurestore_id, encryption_algorithm: "WRONG-ALGORITHM",
                                              access_key: "test", secret_key: "test", bucket: "testbucket")
          parsed_json = JSON.parse(json_result)
          expect_status_details(400)
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
          expect_status_details(400)
          expect(parsed_json.key?("errorCode")).to be true
          expect(parsed_json.key?("errorMsg")).to be true
          expect(parsed_json.key?("usrMsg")).to be true
          expect(parsed_json["errorCode"] == 270104).to be true
        end

        it "should not be able to add s3 connector to the featurestore with secret key and without access key" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, _ = create_s3_connector(project.id, featurestore_id, secret_key: "test", bucket: "testbucket")
          parsed_json = JSON.parse(json_result)
          expect_status_details(400)
          expect(parsed_json.key?("errorCode")).to be true
          expect(parsed_json.key?("errorMsg")).to be true
          expect(parsed_json.key?("usrMsg")).to be true
        end

        it "should not be able to add s3 connector to the featurestore with access key and without secret key" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, _ = create_s3_connector(project.id, featurestore_id, access_key: "test", bucket: "testbucket")
          parsed_json = JSON.parse(json_result)
          expect_status_details(400)
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
          expect_status_details(400)
          expect(parsed_json.key?("errorCode")).to be true
          expect(parsed_json.key?("errorMsg")).to be true
          expect(parsed_json.key?("usrMsg")).to be true
          expect(parsed_json["errorCode"] == 270034).to be true
        end

        it "should be able to add jdbc connector to the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, connector_name = create_jdbc_connector(project.id, featurestore_id, connectionString:
            "jdbc:mysql://localhost:3306/test?user=root&password=123pass")
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)
          expect(parsed_json.key?("id")).to be true
          expect(parsed_json.key?("name")).to be true
          expect(parsed_json.key?("description")).to be true
          expect(parsed_json.key?("storageConnectorType")).to be true
          expect(parsed_json.key?("featurestoreId")).to be true
          expect(parsed_json.key?("connectionString")).to be true
          expect(parsed_json.key?("arguments")).to be true
          expect(parsed_json["name"] == connector_name).to be true
          expect(parsed_json["storageConnectorType"] == "JDBC").to be true
          expect(parsed_json["connectionString"] == "jdbc:mysql://localhost:3306/test?user=root&password=123pass").to be true
          # expect secrets to be created
          secret_name = "jdbc_"+connector_name+"_"+featurestore_id.to_s
          result = get_private_secret(secret_name)
          expect_status_details(200)
          secrets_json = JSON.parse(result)
          expect(secrets_json["items"][0]["secret"]).to eql("123pass")
        end

        it "should not be able to add jdbc connector without a connection string to the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, _ = create_jdbc_connector(project.id, featurestore_id, connectionString: nil)
          parsed_json = JSON.parse(json_result)
          expect_status_details(400)
          expect(parsed_json.key?("errorCode")).to be true
          expect(parsed_json.key?("errorMsg")).to be true
          expect(parsed_json.key?("usrMsg")).to be true
          expect(parsed_json["errorCode"] == 270032).to be true
        end

        it "should create secrets for password in arguments for jdbc connector" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, connector_name = create_jdbc_connector(project.id, featurestore_id,
                                                              connectionString: "jdbc:mysql://localhost:3306/test",
                                                              arguments: [{name: "password", value: "123pass"},
                                                                          {name: "user", value: "username"}])
          # expect secrets created for password
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)
          expect(parsed_json.key?("arguments")).to be true
          # expect secrets to be created
          secret_name = "jdbc_"+connector_name+"_"+featurestore_id.to_s
          result = get_private_secret(secret_name)
          expect_status_details(200)
          secrets_json = JSON.parse(result)
          expect(secrets_json["items"][0]["secret"]).to eql("123pass")
        end

        it "should be able to delete a hopsfs connector from the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, _ = create_hopsfs_connector(project.id, featurestore_id, datasetName: "Resources")
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)
          connector_name = parsed_json["name"]
          delete_connector_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project.id}/featurestores/#{featurestore_id}/storageconnectors/#{connector_name}"
          delete delete_connector_endpoint
          expect_status_details(200)
        end

        it "should not be able to delete default hopsfs connector from the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector_name = project.projectname + "_Training_Datasets"
          delete_connector_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project.id}/featurestores/#{featurestore_id}/storageconnectors/#{connector_name}"
          delete delete_connector_endpoint
          expect_status_details(400)
        end

        it "should be able to delete a s3 connector from the featurestore" do
          project = get_project
          create_session(project[:username], "Pass123")
          featurestore_id = get_featurestore_id(project.id)
          json_result, connector_name = create_s3_connector(project.id, featurestore_id, access_key: "test",
                                                        secret_key: "test", bucket: "testbucket")
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)
          connector_name = parsed_json["name"]
          delete_connector_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project.id}/featurestores/#{featurestore_id}/storageconnectors/#{connector_name}"
          delete delete_connector_endpoint
          expect_status_details(200)
        end

        it "should be able to delete a JDBC connector from the featurestore" do
          project = get_project
          create_session(project[:username], "Pass123")
          featurestore_id = get_featurestore_id(project.id)
          json_result, connector_name = create_jdbc_connector(project.id, featurestore_id, connectionString: "jdbc://test2")
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)
          connector_name = parsed_json["name"]
          delete_connector_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project.id}/featurestores/#{featurestore_id}/storageconnectors/#{connector_name}"
          delete delete_connector_endpoint
          expect_status_details(200)
          get_private_secret("jdbc_"+connector_name+"_"+featurestore_id.to_s)
          # secret should get deleted after update
          expect_status_details(404)
        end

        it "should be able to update hopsfs connector in the featurestore" do
          project = get_project
          create_session(project[:username], "Pass123")
          featurestore_id = get_featurestore_id(project.id)
          json_result1, connector_name = create_hopsfs_connector(project.id, featurestore_id, datasetName: "Resources")
          expect_status_details(201)

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
          expect_status_details(201)

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
                                                              access_key: "test", secret_key: "test",
                                                              bucket: "testbucket")

          parsed_json1 = JSON.parse(json_result1)
          expect_status_details(201)
          arguments = [
            {
              "name": "key1",
              "value": "option1"
            },
            {
              "name": "key2",
              "value": "option2"
            }
          ]
          json_result2, _ = update_s3_connector(project.id, featurestore_id, connector_name,
                                                              access_key: "testdifferent", secret_key: "test",
                                                              bucket: "testbucket2", arguments: arguments)
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
          expect(parsed_json2['arguments'][0]['name']).to eql('key1')
          expect(parsed_json2['arguments'][0]['value']).to eql('option1')
          expect(parsed_json2['arguments'][1]['name']).to eql('key2')
          expect(parsed_json2['arguments'][1]['value']).to eql('option2')
        end

        it "should be able to update JDBC connector in the featurestore" do
          project = get_project
          create_session(project[:username], "Pass123")
          featurestore_id = get_featurestore_id(project.id)
          json_result1, connector_name = create_jdbc_connector(project.id, featurestore_id, connectionString:
            "jdbc://test2",arguments: [{name: "password", value: "123pass"},
                                       {name: "user", value: "username"}])
          expect_status_details(201)
          json_result2 = update_jdbc_connector(project.id, featurestore_id, connector_name,
                                               connectionString: "jdbc://test3",
                                               arguments: [{name: "password", value: "123pass_new"}])
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
          # expect secrets to be updated
          secret_name = "jdbc_"+connector_name+"_"+featurestore_id.to_s
          result = get_private_secret(secret_name)
          expect_status_details(200)
          secrets_json = JSON.parse(result)
          expect(secrets_json["items"][0]["secret"]).to eql("123pass_new")
        end
      end

      context "list storage connectors" do
        before :all do
          with_valid_project

          project = get_project
          featurestore_id = get_featurestore_id(project.id)

          # create sc
          json_result, connector_name = create_hopsfs_connector(project.id, featurestore_id, datasetName: "Resources")
          expect_status_details(201)

          json_result, connector_name = create_hopsfs_connector(project.id, featurestore_id, datasetName: "Resources")
          expect_status_details(201)

          json_result, @connector_name = create_hopsfs_connector(project.id, featurestore_id, datasetName: "Resources")
          expect_status_details(201)
        end

        it "should be able to get a list of connectors in the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)

          # Get the list
          get "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/storageconnectors"
          expect_status_details(200)

          expect(json_body[:items].size).to eq(4)
          expect(json_body[:count]).to eq(4)
        end

        it "should be able to get a list of connectors in the featurestore sorted by id" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)

          # Get the list
          get "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/storageconnectors?sort_by=ID:asc"
          expect_status_details(200)
          ids = json_body[:items].map { |o| o[:id] }
          sorted_ids = ids.sort

          expect(json_body[:items].size).to eq(4)
          expect(json_body[:count]).to eq(4)
          expect(ids).to eq(sorted_ids)
        end

        it "should be able to get a list of connectors in the featurestore sorted by name" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)

          # Get the list
          get "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/storageconnectors?sort_by=NAME:asc"
          expect_status_details(200)
          names = json_body[:items].map { |o| "#{o[:name]}" }
          sorted_names = names.sort_by(&:downcase)

          expect(json_body[:items].size).to eq(4)
          expect(json_body[:count]).to eq(4)
          expect(names).to eq(sorted_names)
        end

        it "should be able to get a list of connectors in the featurestore filtered by type" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)

          # Get the list
          get "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/storageconnectors?filter_by=TYPE:HOPSFS"
          expect_status_details(200)

          expect(json_body[:items].size).to eq(4)
          expect(json_body[:count]).to eq(4)
          expect(json_body[:items].all? { |sc| sc[:storageConnectorType] == "HOPSFS" }).to be true
        end

        it "should be able to get a list of connectors in the featurestore filtered by name" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)

          # Get the list
          get "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/storageconnectors?filter_by=NAME:" + @connector_name
          expect_status_details(200)

          expect(json_body[:items].size).to eq(1)
          expect(json_body[:count]).to eq(1)
          expect(json_body[:items].all? { |sc| sc[:storageConnectorType] == "HOPSFS" }).to be true
        end

        it "should be able to get a list of connectors in the featurestore filtered by name like" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)

          # Get the list
          get "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/storageconnectors?filter_by=NAME_LIKE:hopsfs_connector_"
          expect_status_details(200)

          expect(json_body[:items].size).to eq(3)
          expect(json_body[:count]).to eq(3)
          expect(json_body[:items].all? { |sc| sc[:storageConnectorType] == "HOPSFS" }).to be true
        end

        it "should be able to get a list of connectors in the featurestore limit" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)

          # Get the list
          get "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/storageconnectors?sort_by=ID:asc&limit=2"
          expect_status_details(200)

          expect(json_body[:items].size == 2).to be true
          expect(json_body[:count]).to eq(4)
        end

        it "should be able to get a list of connectors in the featurestore offset" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)

          # Get the list
          get "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/storageconnectors?sort_by=ID:asc"
          expect_status_details(200)
          ids = json_body[:items].map { |o| o[:id] }

          get "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/storageconnectors?sort_by=ID:asc&offset=1"
          expect_status_details(200)
          ids_with_offset = json_body[:items].map { |o| o[:id] }

          expect(json_body[:items].size).to eq(3)
          expect(json_body[:count]).to eq(4)
          for i in 0..json_body[:items].size-1 do
            expect(ids[i+1]).to eq(ids_with_offset[i])
          end
        end
      end

      context "with storage connector flags disabled" do

        # Since there are no flags for S3, JDBC or HOPSFS, we use Kafka connectors for testing the storage connector flags,
        # but it should behave similarly with other types of storage connectors since the same validation mechanism is used for all types.
        # StorageConnectorUtils.isStorageConnectorTypeEnabled() decides which connector type is enabled, and Unit tests can be found in the corresponding folder.

        before :all do
          # enable kafka storage connectors if not enabled
          @enable_kafka_storage_connectors = getVar('enable_kafka_storage_connectors')
          setVar('enable_kafka_storage_connectors', "true")

          with_valid_project

          # create dummy truststore/keystore files in hopsfs
          create_test_files

          # create new kafka connector for testing
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          additional_data = {
            bootstrapServers: "localhost:9091",
            securityProtocol: "SASL_SSL",
            sslTruststoreLocation: "/Projects/#{@project['projectname']}/Resources/sampleTrustStore.jks",
            sslTruststorePassword: "ts_pw",
            sslEndpointIdentificationAlgorithm: "",
            options: [
              {name: "kafka.sasl.jaas.config", value: "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"FEATURE_User\" password=\"\";"},
              {name: "kafka.sasl.mechanism", value: "SCRAM-SHA-256"}
            ]
          }
          connector_name = "kafka_connector_#{random_id}"
          json_result = create_kafka_connector(project.id, featurestore_id, additional_data, connector_name)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          @connector_name = parsed_json["name"]

          # disable kafka storage connectors
          setVar('enable_kafka_storage_connectors', "false")

          create_session(@project[:username], "Pass123")
        end

        after :all do
          # enable kafka storage connectors temporarily
          setVar('enable_kafka_storage_connectors', "true")

          create_session(@project[:username], "Pass123")

          # remove kafka connector for testing
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          delete_connector(project.id, featurestore_id, @connector_name)
          expect_status_details(200)

          # set back default value
          setVar('enable_kafka_storage_connectors', @enable_kafka_storage_connectors[:value])

          create_session(@project[:username], "Pass123")
        end

        it "should not be able to add a connector to the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          additional_data = {
            bootstrapServers: "localhost:9091",
            securityProtocol: "SASL_SSL",
            sslTruststoreLocation: "/Projects/#{@project['projectname']}/Resources/sampleTrustStore.jks",
            sslTruststorePassword: "ts_pw",
            sslEndpointIdentificationAlgorithm: "",
            options: [
              {name: "kafka.sasl.jaas.config", value: "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"FEATURE_User\" password=\"\";"},
              {name: "kafka.sasl.mechanism", value: "SCRAM-SHA-256"}
            ]
          }
          connector_name = "kafka_connector_#{random_id}"
          create_kafka_connector(project.id, featurestore_id, additional_data, connector_name)
          expect_status_details(400, error_code: 270214)
        end

        it "should not be able to get a connector from the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result = get "#{ENV['HOPSWORKS_API']}/project/#{project.id}/featurestores/#{featurestore_id}/storageconnectors/" + @connector_name
          expect_status_details(400, error_code: 270214)
        end

        it "should not be able to update a connector in the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          updated_data = {
            description: "new desc",
            bootstrapServers: "localhost:9091;localhost:9092",
            securityProtocol: "SSL",
            sslTruststoreLocation: "/Projects/#{@project['projectname']}/Resources/newSampleTrustStore.jks",
            sslTruststorePassword: "ts_pw",
            sslKeystoreLocation: "/Path/does/not/exists",
            sslKeystorePassword: "new_ks_pw",
            sslKeyPassword: "new_pw",
            sslEndpointIdentificationAlgorithm: "HTTPS",
            options: [{name: "option2", value: "value2"}, {name: "option3", value: "value3"}]
          }
          json_result, _ = update_kafka_connector(project.id, featurestore_id, @connector_name, updated_data)
          expect_status_details(400, error_code: 270214)
        end

        it "should not be able to delete a connector in the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          delete_connector(project.id, featurestore_id, @connector_name)
          expect_status_details(400, error_code: 270214)
        end
      end
    end
  end

  describe "Create, delete and update operations for kafka connectors" do
    context 'with valid project, featurestore service enabled' do
      before :all do
        # ensure kafka storage connectors are enabled
        @enable_kafka_storage_connectors = getVar('enable_kafka_storage_connectors')
        setVar('enable_kafka_storage_connectors', "true")

        @cleanup = true
        @debugOpt = false
        with_valid_project

        # create dummy truststore/keystore files in hopsfs
        create_test_files
      end

      after :each do
        create_session(@project[:username], "Pass123")
      end

      after :all do
        setVar('enable_kafka_storage_connectors', @enable_kafka_storage_connectors[:value])
      end

      it "should create ssl authenticated kafka connector" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)

        additional_data = {
          bootstrapServers: "localhost:9091",
          securityProtocol: "SSL",
          sslTruststoreLocation: "/Projects/#{@project['projectname']}/Resources/sampleTrustStore.jks",
          sslTruststorePassword: "ts_pw",
          sslKeystoreLocation: "/Projects/#{@project['projectname']}/Resources/sampleKeyStore.jks",
          sslKeystorePassword: "ks_pw",
          sslKeyPassword: "pw",
          sslEndpointIdentificationAlgorithm: "",
          options: [{name: "option1", value: "value1"}]
        }

        connector_name = "kafka_connector_#{random_id}"
        json_result = create_kafka_connector(project.id, featurestore_id, additional_data, connector_name)

        parsed_json = JSON.parse(json_result)
        expect_status_details(201)
        expect(parsed_json.key?("id")).to be true
        expect(parsed_json["featurestoreId"]).to eql(featurestore_id)
        expect(parsed_json["name"]).to eql(connector_name)
        expect(parsed_json["storageConnectorType"]).to eql("KAFKA")
        expect(parsed_json["bootstrapServers"]).to eql("localhost:9091")
        expect(parsed_json["securityProtocol"]).to eql("SSL")
        expect(parsed_json["sslTruststoreLocation"]).to eql("/Projects/#{@project['projectname']}/Resources/sampleTrustStore.jks")
        expect(parsed_json["sslTruststorePassword"]).to eql("ts_pw")
        expect(parsed_json["sslKeystoreLocation"]).to eql("/Projects/#{@project['projectname']}/Resources/sampleKeyStore.jks")
        expect(parsed_json["sslKeystorePassword"]).to eql("ks_pw")
        expect(parsed_json["sslKeyPassword"]).to eql("pw")
        expect(parsed_json["sslEndpointIdentificationAlgorithm"]).to eql("")
        expect(parsed_json["options"].length).to eql(1)
        expect(parsed_json["options"][0]["name"]).to eql("option1")
        expect(parsed_json["options"][0]["value"]).to eql("value1")
      end

      it "should create kafka connector with other security protocol than ssl" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)

        additional_data = {
          bootstrapServers: "localhost:9091",
          securityProtocol: "SASL_SSL",
          sslTruststoreLocation: "/Projects/#{@project['projectname']}/Resources/sampleTrustStore.jks",
          sslTruststorePassword: "ts_pw",
          sslEndpointIdentificationAlgorithm: "",
          options: [
            {name: "kafka.sasl.jaas.config", value: "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"FEATURE_User\" password=\"\";"},
            {name: "kafka.sasl.mechanism", value: "SCRAM-SHA-256"}
          ]
        }

        connector_name = "kafka_connector_#{random_id}"
        json_result = create_kafka_connector(project.id, featurestore_id, additional_data, connector_name)

        parsed_json = JSON.parse(json_result)
        expect_status_details(201)
        expect(parsed_json.key?("id")).to be true
        expect(parsed_json["featurestoreId"]).to eql(featurestore_id)
        expect(parsed_json["name"]).to eql(connector_name)
        expect(parsed_json["storageConnectorType"]).to eql("KAFKA")
        expect(parsed_json["bootstrapServers"]).to eql("localhost:9091")
        expect(parsed_json["securityProtocol"]).to eql("SASL_SSL")
        expect(parsed_json["sslTruststoreLocation"]).to eql("/Projects/#{@project['projectname']}/Resources/sampleTrustStore.jks")
        expect(parsed_json["sslTruststorePassword"]).to eql("ts_pw")
        expect(parsed_json["sslKeystoreLocation"]).to be nil
        expect(parsed_json["sslKeystorePassword"]).to be nil
        expect(parsed_json["sslKeyPassword"]).to be nil
        expect(parsed_json["sslEndpointIdentificationAlgorithm"]).to eql("")
        expect(parsed_json["options"].select{ |o| o["name"] == "kafka.sasl.jaas.config"}.first["value"]).to eql("org.apache.kafka.common.security.scram.ScramLoginModule required username=\"FEATURE_User\" password=\"\";")
        expect(parsed_json["options"].select{ |o| o["name"] == "kafka.sasl.mechanism"}.first["value"]).to eql("SCRAM-SHA-256")
      end

      it "should update all fields of a kafka storage connector" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)

        additional_data = {
          bootstrapServers: "localhost:9091",
          securityProtocol: "SSL",
          sslTruststoreLocation: "/Projects/#{@project['projectname']}/Resources/sampleTrustStore.jks",
          sslTruststorePassword: "ts_pw",
          sslKeystoreLocation: "/Projects/#{@project['projectname']}/Resources/sampleKeyStore.jks",
          sslKeystorePassword: "ks_pw",
          sslKeyPassword: "pw",
          sslEndpointIdentificationAlgorithm: "",
          options: [{name: "option1", value: "value1"}]
        }

        connector_name = "kafka_connector_#{random_id}"
        create_kafka_connector(project.id, featurestore_id, additional_data, connector_name)
        expect_status_details(201)

        updated_data = {
          description: "new desc",
          bootstrapServers: "localhost:9091;localhost:9092",
          securityProtocol: "SSL",
          sslTruststoreLocation: "/Projects/#{@project['projectname']}/Resources/newSampleTrustStore.jks",
          sslTruststorePassword: "ts_pw",
          sslKeystoreLocation: "/Projects/#{@project['projectname']}/Resources/newSampleKeyStore.jks",
          sslKeystorePassword: "new_ks_pw",
          sslKeyPassword: "new_pw",
          sslEndpointIdentificationAlgorithm: "HTTPS",
          options: [{name: "option2", value: "value2"}, {name: "option3", value: "value3"}]
        }

        json_result, _ = update_kafka_connector(project.id, featurestore_id, connector_name, updated_data)

        parsed_json = JSON.parse(json_result)
        expect_status_details(200)
        expect(parsed_json.key?("id")).to be true
        expect(parsed_json["featurestoreId"]).to eql(featurestore_id)
        expect(parsed_json["name"]).to eql(connector_name)
        expect(parsed_json["storageConnectorType"]).to eql("KAFKA")
        expect(parsed_json["bootstrapServers"]).to eql("localhost:9091;localhost:9092")
        expect(parsed_json["securityProtocol"]).to eql("SSL")
        expect(parsed_json["sslTruststoreLocation"]).to eql("/Projects/#{@project['projectname']}/Resources/newSampleTrustStore.jks")
        expect(parsed_json["sslTruststorePassword"]).to eql("ts_pw")
        expect(parsed_json["sslKeystoreLocation"]).to eql("/Projects/#{@project['projectname']}/Resources/newSampleKeyStore.jks")
        expect(parsed_json["sslKeystorePassword"]).to eql("new_ks_pw")
        expect(parsed_json["sslKeyPassword"]).to eql("new_pw")
        expect(parsed_json["sslEndpointIdentificationAlgorithm"]).to eql("HTTPS")
        expect(parsed_json["options"].length).to eql(2)
        expect(parsed_json["options"].select{ |o| o["name"] == "option2"}.first["value"]).to eql("value2")
        expect(parsed_json["options"].select{ |o| o["name"] == "option3"}.first["value"]).to eql("value3")
      end

      it "should get a kafka storage connector" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)

        additional_data = {
          bootstrapServers: "localhost:9091",
          securityProtocol: "SSL",
          sslTruststoreLocation: "/Projects/#{@project['projectname']}/Resources/sampleTrustStore.jks",
          sslTruststorePassword: "ts_pw",
          sslKeystoreLocation: "/Projects/#{@project['projectname']}/Resources/sampleKeyStore.jks",
          sslKeystorePassword: "ks_pw",
          sslKeyPassword: "pw",
          sslEndpointIdentificationAlgorithm: "",
          options: [{name: "option1", value: "value1"}]
        }

        connector_name = "kafka_connector_#{random_id}"
        create_kafka_connector(project.id, featurestore_id, additional_data, connector_name)
        expect_status_details(201)

        json_result = get_storage_connector(project.id, featurestore_id, connector_name)
        parsed_json = JSON.parse(json_result)
        expect_status_details(200)
        expect(parsed_json.key?("id")).to be true
        expect(parsed_json["featurestoreId"]).to eql(featurestore_id)
        expect(parsed_json["name"]).to eql(connector_name)
        expect(parsed_json["storageConnectorType"]).to eql("KAFKA")
        expect(parsed_json["bootstrapServers"]).to eql("localhost:9091")
        expect(parsed_json["securityProtocol"]).to eql("SSL")
        expect(parsed_json["sslTruststoreLocation"]).to eql("/Projects/#{@project['projectname']}/Resources/sampleTrustStore.jks")
        expect(parsed_json["sslTruststorePassword"]).to eql("ts_pw")
        expect(parsed_json["sslKeystoreLocation"]).to eql("/Projects/#{@project['projectname']}/Resources/sampleKeyStore.jks")
        expect(parsed_json["sslKeystorePassword"]).to eql("ks_pw")
        expect(parsed_json["sslKeyPassword"]).to eql("pw")
        expect(parsed_json["sslEndpointIdentificationAlgorithm"]).to eql("")
        expect(parsed_json["options"].length).to eql(1)
        expect(parsed_json["options"][0]["name"]).to eql("option1")
        expect(parsed_json["options"][0]["value"]).to eql("value1")
      end

      it "should access a kafka connector with a different user than the creator both belonging to same project" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)

        additional_data = {
          bootstrapServers: "localhost:9091",
          securityProtocol: "SSL",
          sslTruststoreLocation: "/Projects/#{@project['projectname']}/Resources/sampleTrustStore.jks",
          sslTruststorePassword: "ts_pw",
          sslKeystoreLocation: "/Projects/#{@project['projectname']}/Resources/sampleKeyStore.jks",
          sslKeystorePassword: "ks_pw",
          sslKeyPassword: "pw",
          sslEndpointIdentificationAlgorithm: "",
          options: [{name: "option1", value: "value1"}]
        }

        connector_name = "kafka_connector_#{random_id}"
        create_kafka_connector(project.id, featurestore_id, additional_data, connector_name)
        expect_status_details(201)

        member = create_user
        add_member_to_project(@project, member[:email], "Data scientist")
        reset_session
        create_session(member[:email],"Pass123")

        json_result = get_storage_connector(project.id, featurestore_id, connector_name)
        parsed_json = JSON.parse(json_result)
        expect_status_details(200)
        # make sure secrets are shared
        expect(parsed_json["sslTruststorePassword"]).to eql("ts_pw")
        expect(parsed_json["sslKeystorePassword"]).to eql("ks_pw")
        expect(parsed_json["sslKeyPassword"]).to eql("pw")
      end

      it "should access a kafka connector from a shared feature store with a different user than the creator" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)

        additional_data = {
          bootstrapServers: "localhost:9091",
          securityProtocol: "SSL",
          sslTruststoreLocation: "/Projects/#{@project['projectname']}/Resources/sampleTrustStore.jks",
          sslTruststorePassword: "ts_pw",
          sslKeystoreLocation: "/Projects/#{@project['projectname']}/Resources/sampleKeyStore.jks",
          sslKeystorePassword: "ks_pw",
          sslKeyPassword: "pw",
          sslEndpointIdentificationAlgorithm: "",
          options: [{name: "option1", value: "value1"}]
        }

        connector_name = "kafka_connector_#{random_id}"
        create_kafka_connector(project.id, featurestore_id, additional_data, connector_name)
        expect_status_details(201)
        reset_session

        #create another project
        projectname = "project_#{short_random_id}"
        project1 = create_project_by_name(projectname)
        reset_session
        # login with user for project and share dataset
        create_session(project[:username], "Pass123")
        featurestore = "#{@project[:projectname].downcase}_featurestore.db"
        share_dataset(@project, featurestore, project1[:projectname], datasetType: "&type=FEATURESTORE")
        reset_session
        #login with user for project 1 and accept dataset
        create_session(project1[:username],"Pass123")
        accept_dataset(project1, "#{@project[:projectname]}::#{featurestore}", datasetType: "&type=FEATURESTORE")

        # and try to get storage connector created by other user
        json_result = get_storage_connector(project1.id, featurestore_id, connector_name)
        parsed_json = JSON.parse(json_result)
        expect_status_details(200)
        # make sure secrets are shared
        expect(parsed_json["sslTruststorePassword"]).to eql("ts_pw")
        expect(parsed_json["sslKeystorePassword"]).to eql("ks_pw")
        expect(parsed_json["sslKeyPassword"]).to eql("pw")
      end

      it "should delete kafka connector" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)

        additional_data = {
          bootstrapServers: "localhost:9091",
          securityProtocol: "SSL",
          sslTruststoreLocation: "/Projects/#{@project['projectname']}/Resources/sampleTrustStore.jks",
          sslTruststorePassword: "ts_pw",
          sslKeystoreLocation: "/Projects/#{@project['projectname']}/Resources/sampleKeyStore.jks",
          sslKeystorePassword: "ks_pw",
          sslKeyPassword: "pw",
          sslEndpointIdentificationAlgorithm: "",
          options: [{name: "option1", value: "value1"}]
        }

        connector_name = "kafka_connector_#{random_id}"
        json_result = create_kafka_connector(project.id, featurestore_id, additional_data, connector_name)
        parsed_json = JSON.parse(json_result)
        expect_status_details(201)
        delete_connector(project.id,featurestore_id,connector_name)
        expect_status_details(200)
        expect(test_file(parsed_json["sslTruststoreLocation"])).to be false
        expect(test_file(parsed_json["sslKeystoreLocation"])).to be false
      end
    end
  end

  describe "Operations for online kafka connector" do
    context 'with valid project, featurestore service enabled' do
      before :all do
        @enable_bring_your_own_kafka = getVar('enable_bring_your_own_kafka')
        @enable_kafka_storage_connectors = getVar('enable_kafka_storage_connectors')
        setVar('enable_kafka_storage_connectors', "true")

        @connector_name = "kafka_connector"

        @cleanup = true
        @debugOpt = false
        with_valid_project
      end

      after :all do
        setVar('enable_bring_your_own_kafka', @enable_bring_your_own_kafka[:value])
        setVar('enable_kafka_storage_connectors', @enable_kafka_storage_connectors[:value])
      end

      it "should get a online kafka storage connector with default values if it didn't exist" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)

        setVar('enable_bring_your_own_kafka', "true")
        create_session(project[:username], "Pass123")

        json_result = get_online_kafka_storage_connector(project.id, featurestore_id)
        parsed_json = JSON.parse(json_result)
        expect_status_details(200)
        expect(parsed_json.key?("id")).to be true
        expect(parsed_json["featurestoreId"]).to eql(featurestore_id)
        expect(parsed_json["name"]).to eql(@connector_name)
        expect(parsed_json["storageConnectorType"]).to eql("KAFKA")
        if ENV['OS'] == "ubuntu"
          expect(parsed_json["bootstrapServers"]).to eql("10.0.2.15:9091")
        end
        expect(parsed_json["securityProtocol"]).to eql("SSL")
        expect(parsed_json.key?("sslTruststoreLocation")).to be false
        expect(parsed_json.key?("sslTruststorePassword")).to be false
        expect(parsed_json.key?("sslKeystoreLocation")).to be false
        expect(parsed_json.key?("sslKeystorePassword")).to be false
        expect(parsed_json.key?("sslKeyPassword")).to be false
        expect(parsed_json["sslEndpointIdentificationAlgorithm"]).to eql("")
        expect(parsed_json["externalKafka"]).to be false
      end

      it "should get already created online kafka storage connector" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)

        setVar('enable_bring_your_own_kafka', "true")
        create_session(project[:username], "Pass123")

        additional_data = {
          bootstrapServers: "localhost:9091",
          securityProtocol: "SASL_SSL",
          sslEndpointIdentificationAlgorithm: "",
          options: [{name: "option1", value: "value1"}]
        }

        create_kafka_connector(project.id, featurestore_id, additional_data, @connector_name)
        expect_status_details(201)

        json_result = get_online_kafka_storage_connector(project.id, featurestore_id)
        parsed_json = JSON.parse(json_result)
        expect_status_details(200)
        expect(parsed_json.key?("id")).to be true
        expect(parsed_json["featurestoreId"]).to eql(featurestore_id)
        expect(parsed_json["name"]).to eql(@connector_name)
        expect(parsed_json["storageConnectorType"]).to eql("KAFKA")
        expect(parsed_json["bootstrapServers"]).to eql("localhost:9091")
        expect(parsed_json["securityProtocol"]).to eql("SASL_SSL")
        expect(parsed_json["sslEndpointIdentificationAlgorithm"]).to eql("")
        expect(parsed_json["options"].length).to eql(1)
        expect(parsed_json["options"][0]["name"]).to eql("option1")
        expect(parsed_json["options"][0]["value"]).to eql("value1")
        expect(parsed_json["externalKafka"]).to be true

        delete "#{ENV['HOPSWORKS_API']}/project/#{project.id}/featurestores/#{featurestore_id}/storageconnectors/#{@connector_name}"
        expect_status_details(200)
      end

      it "should get a online kafka storage connector with default values if it didn't exist and byok is not enabled" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)

        setVar('enable_bring_your_own_kafka', "false")
        create_session(project[:username], "Pass123")

        json_result = get_online_kafka_storage_connector(project.id, featurestore_id)
        parsed_json = JSON.parse(json_result)
        expect_status_details(200)
        expect(parsed_json.key?("id")).to be true
        expect(parsed_json["featurestoreId"]).to eql(featurestore_id)
        expect(parsed_json["name"]).to eql(@connector_name)
        expect(parsed_json["storageConnectorType"]).to eql("KAFKA")
        if ENV['OS'] == "ubuntu"
          expect(parsed_json["bootstrapServers"]).to eql("10.0.2.15:9091")
        end
        expect(parsed_json["securityProtocol"]).to eql("SSL")
        expect(parsed_json.key?("sslTruststoreLocation")).to be false
        expect(parsed_json.key?("sslTruststorePassword")).to be false
        expect(parsed_json.key?("sslKeystoreLocation")).to be false
        expect(parsed_json.key?("sslKeystorePassword")).to be false
        expect(parsed_json.key?("sslKeyPassword")).to be false
        expect(parsed_json["sslEndpointIdentificationAlgorithm"]).to eql("")
        expect(parsed_json["externalKafka"]).to be false
      end

      it "should get a online kafka storage connector with default values if it exists and byok is not enabled" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)

        setVar('enable_bring_your_own_kafka', "false")
        create_session(project[:username], "Pass123")

        additional_data = {
          bootstrapServers: "localhost:9091",
          securityProtocol: "SASL_SSL",
          sslEndpointIdentificationAlgorithm: "",
          options: [{name: "option1", value: "value1"}]
        }

        create_kafka_connector(project.id, featurestore_id, additional_data, @connector_name)
        expect_status_details(201)

        json_result = get_online_kafka_storage_connector(project.id, featurestore_id)
        parsed_json = JSON.parse(json_result)
        expect_status_details(200)
        expect(parsed_json.key?("id")).to be true
        expect(parsed_json["featurestoreId"]).to eql(featurestore_id)
        expect(parsed_json["name"]).to eql(@connector_name)
        expect(parsed_json["storageConnectorType"]).to eql("KAFKA")
        if ENV['OS'] == "ubuntu"
          expect(parsed_json["bootstrapServers"]).to eql("10.0.2.15:9091")
        end
        expect(parsed_json["securityProtocol"]).to eql("SSL")
        expect(parsed_json.key?("sslTruststoreLocation")).to be false
        expect(parsed_json.key?("sslTruststorePassword")).to be false
        expect(parsed_json.key?("sslKeystoreLocation")).to be false
        expect(parsed_json.key?("sslKeystorePassword")).to be false
        expect(parsed_json.key?("sslKeyPassword")).to be false
        expect(parsed_json["sslEndpointIdentificationAlgorithm"]).to eql("")
        expect(parsed_json["externalKafka"]).to be false

        delete "#{ENV['HOPSWORKS_API']}/project/#{project.id}/featurestores/#{featurestore_id}/storageconnectors/#{@connector_name}"
        expect_status_details(200)
      end
    end
  end
end
