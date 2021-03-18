=begin
 This file is part of Hopsworks
 Copyright (C) 2020, Logical Clocks AB. All rights reserved

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
  after(:all) {clean_all_test_projects(spec: "redshift_storage_connector")}
  before :all do
    @p1, @p2 = with_redshift_connectors
    @featurestore1 = get_featurestores_checked(@p1[:id])[0]
    @featurestore2 = get_featurestores_checked(@p2[:id])[0]
    connectors = get_storage_connectors(@p1[:id], @featurestore1["featurestoreId"], "REDSHIFT")
    @connector = connectors[0]
  end
  describe "get" do
    context 'without authentication' do
      before :all do
        reset_session
      end
      it "should fail to get" do
        get_storage_connectors(@p1[:id], @featurestore1["featurestoreId"], "REDSHIFT")
        expect_status_details(401)
      end
      it "should fail to access connectors in a project with no role" do
        with_valid_session
        get_storage_connectors(@p1[:id], @featurestore1["featurestoreId"], "REDSHIFT")
        expect_status_details(403)
      end
    end
    context 'with authentication' do
      before :all do
        create_session(@p1[:username], "Pass123")
      end
      it "should get" do
        connectors = get_storage_connectors(@p1[:id], @featurestore1["featurestoreId"], "REDSHIFT")
        expect_status_details(200)
        expect(connectors.length).to eq 2
      end
      it "should get by name" do
        connectors = get_storage_connectors(@p1[:id], @featurestore1["featurestoreId"], "REDSHIFT")
        expect_status_details(200)
        connector_name = connectors[0][:name]
        get_storage_connector(@p1[:id], @featurestore1["featurestoreId"], connector_name)
        expect_status_details(200)
        expect(json_body[:name]).to eq connector_name
      end
    end
  end
  describe "create" do
    context 'without authentication' do
      before :all do
        reset_session
      end
      it "should fail to create" do
        create_redshift_connector(@p1[:id], @featurestore1["featurestoreId"], redshift_connector_name: "redshift_connector_iam",
                                  iamRole: "arn:aws:iam::123456789012:role/test-role-p1")
        expect_status_details(401)
      end
      it "should fail to create a connector in a project with no role" do
        with_valid_session
        create_redshift_connector(@p1[:id], @featurestore1["featurestoreId"], redshift_connector_name: "redshift_connector_iam",
                                  iamRole: "arn:aws:iam::123456789012:role/test-role-p1")
        expect_status_details(403)
      end
    end
    context 'with authentication' do
      before :all do
        create_session(@p1[:username], "Pass123")
      end
      it "should create a connector with iamRole" do
        create_redshift_connector(@p1[:id], @featurestore1["featurestoreId"], redshift_connector_name: "connector_iam",
                                  iamRole: "arn:aws:iam::123456789012:role/test-role-p1")
        expect_status_details(201)
        get_private_secret("redshift_connector_iam_#{@featurestore1["featurestoreId"]}")
        expect_status_details(404)
      end
      it "should create a connector with password" do
        create_redshift_connector(@p1[:id], @featurestore1["featurestoreId"], redshift_connector_name: "connector_pwd",
                                  databasePassword: "awsUserPwd")
        expect_status_details(201)
        get_private_secret("redshift_connector_pwd_#{@featurestore1["featurestoreId"]}")
        expect_status_details(200)
        delete_secret("redshift_connector_pwd_#{@featurestore1["featurestoreId"]}")
        expect_status_details(400)
      end
      it "should fail to create with iam and password" do
        create_redshift_connector(@p1[:id], @featurestore1["featurestoreId"], databasePassword: "awsUserPwd",
                                  iamRole: "arn:aws:iam::123456789012:role/test-role-p1")
        expect_status_details(400)
      end
      it "should fail to create connector with an existing name" do
        create_redshift_connector(@p1[:id], @featurestore1["featurestoreId"], redshift_connector_name: "connector_iam",
                                  iamRole: "arn:aws:iam::123456789012:role/test-role-p1")
        expect_status_details(400)
      end
      it "should fail to create with no iam and password" do
        create_redshift_connector(@p1[:id], @featurestore1["featurestoreId"], databasePassword: nil, iamRole: nil)
        expect_status_details(400)
      end
    end
  end
  describe "update" do
    context 'without authentication' do
      before :all do
        reset_session
      end
      it "should fail to update" do
        connector = @connector.clone
        connector[:description] = "updated description"
        update_redshift_connector(@p1[:id], @featurestore1["featurestoreId"], @connector[:name], connector)
        expect_status_details(401)
      end
      it "should fail to update a connector in a project with no role" do
        with_valid_session
        connector = @connector.clone
        connector[:description] = "updated description"
        update_redshift_connector(@p1[:id], @featurestore1["featurestoreId"], @connector[:name], connector)
        expect_status_details(403)
      end
    end
    context 'with authentication' do
      before :all do
        create_session(@p1[:username], "Pass123")
      end
      it "should update connector clusterIdentifier" do
        copyConnector = @connector.clone
        copyConnector[:description] = "updated description"
        update_redshift_connector(@p1[:id], @featurestore1["featurestoreId"], copyConnector[:name], copyConnector)
        expect_status_details(200)
        check_redshift_connector_update(json_body, copyConnector)
        get_storage_connector(@p1[:id], @featurestore1["featurestoreId"], @connector[:name])
        expect_status_details(200)
        check_redshift_connector_update(json_body, copyConnector)
      end
      it "should update connector cluster identifier" do
        copyConnector = @connector.clone
        copyConnector[:clusterIdentifier] = "redshift-connector-updated"
        update_redshift_connector(@p1[:id], @featurestore1["featurestoreId"], copyConnector[:name], copyConnector)
        expect_status_details(200)
        check_redshift_connector_update(json_body, copyConnector)
        get_storage_connector(@p1[:id], @featurestore1["featurestoreId"], @connector[:name])
        expect_status_details(200)
        check_redshift_connector_update(json_body, copyConnector)
      end
      it "should update connector database driver" do
        copyConnector = @connector.clone
        copyConnector[:databaseDriver] = "com.amazon.redshift.jdbc43.Driver"
        update_redshift_connector(@p1[:id], @featurestore1["featurestoreId"], copyConnector[:name], copyConnector)
        expect_status_details(200)
        check_redshift_connector_update(json_body, copyConnector)
        get_storage_connector(@p1[:id], @featurestore1["featurestoreId"], @connector[:name])
        expect_status_details(200)
        check_redshift_connector_update(json_body, copyConnector)
      end
      it "should update connector database endpoint" do
        copyConnector = @connector.clone
        copyConnector[:databaseEndpoint] = "abc123xyz789.us-east-1.redshift.amazonaws.com"
        update_redshift_connector(@p1[:id], @featurestore1["featurestoreId"], copyConnector[:name], copyConnector)
        expect_status_details(200)
        check_redshift_connector_update(json_body, copyConnector)
        get_storage_connector(@p1[:id], @featurestore1["featurestoreId"], @connector[:name])
        expect_status_details(200)
        check_redshift_connector_update(json_body, copyConnector)
      end
      it "should update connector database name" do
        copyConnector = @connector.clone
        copyConnector[:databaseName] = "database"
        update_redshift_connector(@p1[:id], @featurestore1["featurestoreId"], copyConnector[:name], copyConnector)
        expect_status_details(200)
        check_redshift_connector_update(json_body, copyConnector)
        get_storage_connector(@p1[:id], @featurestore1["featurestoreId"], @connector[:name])
        expect_status_details(200)
        check_redshift_connector_update(json_body, copyConnector)
      end
      it "should update connector table name" do
        copyConnector = @connector.clone
        copyConnector[:tableName] = "test1"
        update_redshift_connector(@p1[:id], @featurestore1["featurestoreId"], copyConnector[:name], copyConnector)
        expect_status_details(200)
        check_redshift_connector_update(json_body, copyConnector)
        get_storage_connector(@p1[:id], @featurestore1["featurestoreId"], @connector[:name])
        expect_status_details(200)
        check_redshift_connector_update(json_body, copyConnector)
      end
      it "should update connector database user" do
        copyConnector = @connector.clone
        copyConnector[:databaseUserName] = "databaseUser"
        update_redshift_connector(@p1[:id], @featurestore1["featurestoreId"], copyConnector[:name], copyConnector)
        expect_status_details(200)
        check_redshift_connector_update(json_body, copyConnector)
        get_storage_connector(@p1[:id], @featurestore1["featurestoreId"], @connector[:name])
        expect_status_details(200)
        check_redshift_connector_update(json_body, copyConnector)
      end
      it "should update connector database user auto create" do
        copyConnector = @connector.clone
        copyConnector[:autoCreate] = true
        update_redshift_connector(@p1[:id], @featurestore1["featurestoreId"], copyConnector[:name], copyConnector)
        expect_status_details(200)
        check_redshift_connector_update(json_body, copyConnector)
        get_storage_connector(@p1[:id], @featurestore1["featurestoreId"], @connector[:name])
        expect_status_details(200)
        check_redshift_connector_update(json_body, copyConnector)
      end
      it "should update connector database group" do
        copyConnector = @connector.clone
        copyConnector[:databaseGroup] = "auto_login"
        update_redshift_connector(@p1[:id], @featurestore1["featurestoreId"], copyConnector[:name], copyConnector)
        expect_status_details(200)
        check_redshift_connector_update(json_body, copyConnector)
        get_storage_connector(@p1[:id], @featurestore1["featurestoreId"], @connector[:name])
        expect_status_details(200)
        check_redshift_connector_update(json_body, copyConnector)
      end
      it "should update connector arguments" do
        copyConnector = @connector.clone
        copyConnector[:arguments] = "test1,test2,test3"
        update_redshift_connector(@p1[:id], @featurestore1["featurestoreId"], copyConnector[:name], copyConnector)
        expect_status_details(200)
        check_redshift_connector_update(json_body, copyConnector)
        get_storage_connector(@p1[:id], @featurestore1["featurestoreId"], @connector[:name])
        expect_status_details(200)
        check_redshift_connector_update(json_body, copyConnector)
      end
      it "should update connector database databasePort" do
        copyConnector = @connector.clone
        copyConnector[:databasePort] = 5454
        update_redshift_connector(@p1[:id], @featurestore1["featurestoreId"], copyConnector[:name], copyConnector)
        expect_status_details(200)
        check_redshift_connector_update(json_body, copyConnector)
        get_storage_connector(@p1[:id], @featurestore1["featurestoreId"], @connector[:name])
        expect_status_details(200)
        check_redshift_connector_update(json_body, copyConnector)
      end
      it "should update connector database password" do
        get_storage_connector(@p1[:id], @featurestore1["featurestoreId"], "redshift_connector_pwd")
        connector = json_body
        copyConnector = connector.clone
        copyConnector[:databasePassword] = "databasepwd"
        update_redshift_connector(@p1[:id], @featurestore1["featurestoreId"], copyConnector[:name], copyConnector)
        expect_status_details(200)
        check_redshift_connector_update(json_body, copyConnector)
        get_storage_connector(@p1[:id], @featurestore1["featurestoreId"], connector[:name])
        expect_status_details(200)
        check_redshift_connector_update(json_body, copyConnector)
      end
      it "should update connector database iam role" do
        get_storage_connector(@p1[:id], @featurestore1["featurestoreId"], "redshift_connector_iam")
        connector = json_body
        copyConnector = connector.clone
        copyConnector[:iamRole] = "arn:aws:iam::123456789012:role/test-role-p1-owners"
        update_redshift_connector(@p1[:id], @featurestore1["featurestoreId"], copyConnector[:name], copyConnector)
        expect_status_details(200)
        check_redshift_connector_update(json_body, copyConnector)
        get_storage_connector(@p1[:id], @featurestore1["featurestoreId"], connector[:name])
        expect_status_details(200)
        check_redshift_connector_update(json_body, copyConnector)
      end
      it "should update connector from iam role to password" do
        get_storage_connector(@p1[:id], @featurestore1["featurestoreId"], "redshift_connector_iam")
        connector = json_body
        copyConnector = connector.clone
        copyConnector[:iamRole] = nil
        copyConnector[:databasePassword] = "databasepwd"
        update_redshift_connector(@p1[:id], @featurestore1["featurestoreId"], copyConnector[:name], copyConnector)
        expect_status_details(200)
        check_redshift_connector_update(json_body, copyConnector)
        get_storage_connector(@p1[:id], @featurestore1["featurestoreId"], connector[:name])
        expect_status_details(200)
        check_redshift_connector_update(json_body, copyConnector)
        get_private_secret("redshift_redshift_connector_iam_#{@featurestore1["featurestoreId"]}")
        expect_status_details(200)
      end
      it "should update connector from password to iam role" do
        get_storage_connector(@p1[:id], @featurestore1["featurestoreId"], "redshift_connector_pwd")
        connector = json_body
        copyConnector = connector.clone
        copyConnector[:iamRole] = "arn:aws:iam::123456789012:role/test-role-p1-owners"
        copyConnector[:databasePassword] = nil
        update_redshift_connector(@p1[:id], @featurestore1["featurestoreId"], copyConnector[:name], copyConnector)
        expect_status_details(200)
        check_redshift_connector_update(json_body, copyConnector)
        get_storage_connector(@p1[:id], @featurestore1["featurestoreId"], connector[:name])
        expect_status_details(200)
        check_redshift_connector_update(json_body, copyConnector)
        get_private_secret("redshift_redshift_connector_pwd_#{@featurestore1["featurestoreId"]}")
        expect_status_details(404)
      end
      it "should update connector " do
        copyConnector = @connector.clone
        copyConnector[:description] = "updated description1"
        copyConnector[:clusterIdentifier] = "redshift-connector-updated1"
        copyConnector[:databaseDriver] = "com.amazon.redshift.jdbc52.Driver"
        copyConnector[:databaseEndpoint] = "abc123xyz789.us-west-2.redshift.amazonaws.com"
        copyConnector[:databaseName] = "database1"
        copyConnector[:databaseUserName] = "databaseUser1"
        copyConnector[:databasePort] = 5455
        copyConnector[:tableName] = "table1"
        copyConnector[:arguments] = "test1,test2,test3,test4"
        update_redshift_connector(@p1[:id], @featurestore1["featurestoreId"], copyConnector[:name], copyConnector)
        expect_status_details(200)
        check_redshift_connector_update(json_body, copyConnector)
        get_storage_connector(@p1[:id], @featurestore1["featurestoreId"], @connector[:name])
        expect_status_details(200)
        check_redshift_connector_update(json_body, copyConnector)
      end
      it "should fail to update connector name" do
        copyConnector = @connector.clone
        copyConnector[:name] = "updated_connector_name"
        update_redshift_connector(@p1[:id], @featurestore1["featurestoreId"], copyConnector[:name], copyConnector)
        expect_status_details(404)
        get_storage_connector(@p1[:id], @featurestore1["featurestoreId"], @connector[:name])
        expect_status_details(200)
        expect(json_body[:name]).to eq @connector[:name]
      end
      it "should fail to update with iam role when password is set" do
        get_storage_connector(@p2[:id], @featurestore2["featurestoreId"], "redshift_connector_pwd")
        connector = json_body
        copyConnector = connector.clone
        copyConnector[:iamRole] = "arn:aws:iam::123456789012:role/test-role-p1"
        update_redshift_connector(@p2[:id], @featurestore2["featurestoreId"], copyConnector[:name], copyConnector)
        expect_status_details(400)

        get_storage_connector(@p2[:id], @featurestore2["featurestoreId"], connector[:name])
        expect_status_details(200)
        check_redshift_connector_update(json_body, connector)
      end
      it "should fail to update with password when iam role set" do
        get_storage_connector(@p2[:id], @featurestore2["featurestoreId"], "redshift_connector_iam")
        connector = json_body
        copyConnector = connector.clone
        copyConnector[:databasePassword] = "newPassword"
        update_redshift_connector(@p2[:id], @featurestore2["featurestoreId"], copyConnector[:name], copyConnector)
        expect_status_details(400)

        get_storage_connector(@p2[:id], @featurestore2["featurestoreId"], connector[:name])
        expect_status_details(200)
        check_redshift_connector_update(json_body, connector)
      end
      it "should fail to update with no password and iam role" do
        get_storage_connector(@p2[:id], @featurestore2["featurestoreId"], "redshift_connector_iam")
        connector = json_body
        copyConnector = connector.clone
        copyConnector[:databasePassword] = nil
        copyConnector[:iamRole] = nil
        update_redshift_connector(@p2[:id], @featurestore2["featurestoreId"], copyConnector[:name], copyConnector)
        expect_status_details(400)

        get_storage_connector(@p2[:id], @featurestore2["featurestoreId"], connector[:name])
        expect_status_details(200)
        check_redshift_connector_update(json_body, connector)
      end
    end
  end
  describe "shared" do
    context 'with authentication' do
      before :all do
        reset_session
        with_valid_session
        @p3 = create_project_by_name("project_#{short_random_id}")
        reset_session
        create_session(@p1[:username], "Pass123")
        share_dataset_checked(@p1 , "#{@featurestore1["featurestoreName"]}.db", @p3[:projectname], datasetType: "FEATURESTORE")
        create_redshift_connector(@p1[:id], @featurestore1["featurestoreId"], redshift_connector_name: "p3-connector-pwd",
                                  databasePassword: "awsUserPwd")
        create_session(@p3[:username], "Pass123")
        accept_dataset_checked(@p3, "#{@p1[:projectname]}::#{@featurestore1["featurestoreName"]}.db", datasetType: "FEATURESTORE")
      end
      it "should get connectors in a shared fs" do
        get_storage_connectors(@p3[:id], @featurestore1["featurestoreId"], "REDSHIFT")
        expect_status_details(200)
        expect(json_body.length > 2).to eq true
      end
      it "should get connector in a shared fs with no password" do
        get_storage_connector(@p3[:id], @featurestore1["featurestoreId"], "p3-connector-pwd")
        expect_status_details(200)
        expect(json_body.key?("databasePassword")).to eq false
      end
      it "should fail to create connector in a shared fs" do
        create_redshift_connector(@p3[:id], @featurestore1["featurestoreId"], redshift_connector_name: "connector_iam_p3",
                                  iamRole: "arn:aws:iam::123456789012:role/test-role-p3")
        expect_status_details(403)
      end
      it "should fail to create connector with password in a shared fs" do
        create_redshift_connector(@p3[:id], @featurestore1["featurestoreId"], redshift_connector_name: "connector_iam_p3",
                                  databasePassword: "awsUserPwd")
        expect_status_details(403)
      end
      it "should fail to edit connectors in a shared fs" do
        get_storage_connector(@p3[:id], @featurestore1["featurestoreId"], "p3-connector-pwd")
        connector = json_body
        copyConnector = connector.clone
        copyConnector[:description] = "updated description p3"
        copyConnector[:clusterIdentifier] = "redshift-connector-updated-p3"
        copyConnector[:databaseName] = "database-p3"
        copyConnector[:databaseUserName] = "databaseUser-p3"
        copyConnector[:databasePort] = 5456
        copyConnector[:arguments] = "test1,test2,test3,test4,p3"
        update_redshift_connector(@p3[:id], @featurestore1["featurestoreId"], copyConnector[:name], copyConnector)
        expect_status_details(403)
        get_storage_connector(@p3[:id], @featurestore1["featurestoreId"], connector[:name])
        expect_status_details(200)
        check_redshift_connector_update(json_body, connector)
      end
      it "should fail to edit connector database password in a shared fs" do
        get_storage_connector(@p3[:id], @featurestore1["featurestoreId"], "p3-connector-pwd")
        connector = json_body
        copyConnector = connector.clone
        copyConnector[:databasePassword] = "password-p3"
        update_redshift_connector(@p3[:id], @featurestore1["featurestoreId"], copyConnector[:name], copyConnector)
        expect_status_details(403)
        get_storage_connector(@p3[:id], @featurestore1["featurestoreId"], connector[:name])
        expect_status_details(200)
        check_redshift_connector_update(json_body, connector)
      end
      it "should fail to delete connector with password in a shared fs" do
        delete_connector(@p3[:id], @featurestore1["featurestoreId"], "p3-connector-pwd")
        expect_status_details(403)
      end
    end
  end
  describe "delete" do
    context 'without authentication' do
      before :all do
        reset_session
      end
      it "should fail to delete connector" do
        delete_connector(@p1[:id], @featurestore1["featurestoreId"], @connector[:name])
        expect_status_details(401)
      end
      it "should fail to update a connector in a project with no role" do
        with_valid_session
        delete_connector(@p1[:id], @featurestore1["featurestoreId"], @connector[:name])
        expect_status_details(403)
      end
    end
    context 'with authentication' do
      before :all do
        create_session(@p1[:username], "Pass123")
      end
      it "should delete connector by name" do
        delete_connector(@p1[:id], @featurestore1["featurestoreId"], @connector[:name])
        expect_status_details(200)
        get_storage_connector(@p1[:id], @featurestore1["featurestoreId"], @connector[:name])
        expect_status_details(404)
      end
      it "should delete secrete when deleting connector" do
        get_storage_connector(@p2[:id], @featurestore2["featurestoreId"], "redshift_connector_pwd")
        connector = json_body
        get_private_secret("redshift_redshift_connector_pwd_#{@featurestore2["featurestoreId"]}")
        expect_status_details(200)
        delete_connector(@p2[:id], @featurestore2["featurestoreId"], connector[:name])
        expect_status_details(200)
        get_private_secret("redshift_redshift_connector_pwd_#{@featurestore2["featurestoreId"]}")
        expect_status_details(404)
      end
    end
  end
end