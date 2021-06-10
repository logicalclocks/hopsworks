=begin
 This file is part of Hopsworks
 Copyright (C) 2021, Logical Clocks AB. All rights reserved

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
  after(:all) {clean_all_test_projects(spec: "snowflake_storage_connector")}

  before :all do
    with_valid_project
    featurestore_id = get_featurestore_id(@project[:id])
    @connector_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/#{featurestore_id}/storageconnectors"
    type = "featurestoreSnowflakeConnectorDTO"
    storageConnectorType = "SNOWFLAKE"
    @connector = { name: "snowflake_connector_#{random_id}",
                   description: "test snowflake connector",
                   type: type,
                   storageConnectorType: storageConnectorType,
                   sfOptions: [{name:"sfTimezone", value:"spark"}, {name: "sfCompress", value: "true"}],
                   database: "test",
                   password: "123456PWD",
                   role: "role",
                   schema: "PUBLIC",
                   table: "table",
                   url: "http://123456.eu-central-1.snowflakecomputing.com",
                   user: "user0",
                   warehouse: "warehouse"
                 }
  end

  it "should create a connector" do
      connector = @connector.clone
      post @connector_endpoint, connector.to_json
      expect_status_details(201)
      check_snowflake_connector_update(json_body, connector)
  end

  it "should create a connector without warehouse, table and role" do
      connector = @connector.clone
      connector[:name] = "snowflake_connector_#{random_id}"
      connector[:warehouse] = nil
      connector[:table] = nil
      connector[:role] = nil
      post @connector_endpoint, connector.to_json
      expect_status_details(201)
      check_snowflake_connector_update(json_body, connector)
  end

  it "should fail to create a connector if schema is not set" do
      connector = @connector.clone
      connector[:name] = "snowflake_connector_#{random_id}"
      connector[:schema] = nil
      post @connector_endpoint, connector.to_json
      expect_status_details(400)
  end

  it "should fail to create a connector if database is not set" do
      connector = @connector.clone
      connector[:name] = "snowflake_connector_#{random_id}"
      connector[:database] = nil
      post @connector_endpoint, connector.to_json
      expect_status_details(400)
  end

  it "should fail to create a connector if user is not set" do
      connector = @connector.clone
      connector[:name] = "snowflake_connector_#{random_id}"
      connector[:user] = nil
      post @connector_endpoint, connector.to_json
      expect_status_details(400)
  end

  it "should fail to create a connector if url is not set" do
      connector = @connector.clone
      connector[:name] = "snowflake_connector_#{random_id}"
      connector[:url] = nil
      post @connector_endpoint, connector.to_json
      expect_status_details(400)
  end

  it "should fail to create a connector if both password and token are set" do
      connector = @connector.clone
      connector[:name] = "snowflake_connector_#{random_id}"
      connector[:token] = "123456token"
      post @connector_endpoint, connector.to_json
      expect_status_details(400)
  end

  it "should fail to create a connector if both password and token are not set" do
      connector = @connector.clone
      connector[:name] = "snowflake_connector_#{random_id}"
      connector[:password] = nil
      connector[:token] = nil
      post @connector_endpoint, connector.to_json
      expect_status_details(400)
  end

  it "should set empty table name to null" do
    connector = @connector.clone
    connector[:name] = "snowflake_connector_#{random_id}"
    connector[:table] = " "
    post @connector_endpoint, connector.to_json
    expect_status_details(201)
    connector[:table] = nil
    check_snowflake_connector_update(json_body, connector)
  end

  it "should update a connector" do
      connector = @connector.clone
      connector[:url] = "https://123457.eu-central-1.snowflakecomputing.com"
      connector[:schema] = "PRIVATE"
      connector[:warehouse] = "warehouse1"
      connector[:database] = "test1"
      connector[:user] = "user1"
      connector[:password] = "123456 Pwd"
      connector[:role] = "role1"
      connector[:sfOptions] = [{name:"sfTimezone", value:"spark"},
                               {name: "sfCompress", value: "true"},
                               {name: "s3MaxFileSize", value: "10MB"}]
      put "#{@connector_endpoint}/#{@connector[:name]}", connector.to_json
      expect_status_details(200)
      check_snowflake_connector_update(json_body, connector)
  end

  it "should update a connector from password to token auth" do
      connector = @connector.clone
      connector[:url] = "https://123458.eu-central-2.snowflakecomputing.com"
      connector[:schema] = "PRIVATE"
      connector[:warehouse] = "warehouse2"
      connector[:database] = "test2"
      connector[:user] = "user2"
      connector[:password] = nil
      connector[:token] = "123456token"
      connector[:role] = "role2"
      connector[:sfOptions] = [{name:"sfTimezone", value:"spark"},
                               {name: "sfCompress", value: "true"},
                               {name: "s3MaxFileSize", value: "100MB"}]
      put "#{@connector_endpoint}/#{@connector[:name]}", connector.to_json
      expect_status_details(200)
      check_snowflake_connector_update(json_body, connector)
  end

  it "should update a connector from token to password auth" do
      connector = @connector.clone
      connector[:url] = "https://123459.eu-central-3.snowflakecomputing.com"
      connector[:schema] = "PRIVATE"
      connector[:warehouse] = "warehouse3"
      connector[:database] = "test3"
      connector[:user] = "user3"
      connector[:password] = "123456Pwd"
      connector[:token] = nil
      connector[:role] = "role3"
      connector[:sfOptions] = [{name:"sfTimezone", value:"spark"},
                               {name: "sfCompress", value: "true"},
                               {name: "s3MaxFileSize", value: "1000MB"}]
      put "#{@connector_endpoint}/#{@connector[:name]}", connector.to_json
      expect_status_details(200)
      check_snowflake_connector_update(json_body, connector)
  end
end