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
  after(:all) {clean_all_test_projects(spec: "adls_storage_connector")}

  before :all do
    with_valid_project
    featurestore_id = get_featurestore_id(@project[:id])
    @connector_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/#{featurestore_id}/storageconnectors"
  end

  type = "featurestoreADLSConnectorDTO"
  storageConnectorType = "ADLS"

  it "should be able to create a generation 1 adls connector" do
    json_data = {
      name: "adls_connector_#{random_id}",
      description: "testadlsconnectordescription",
      type: type,
      storageConnectorType: storageConnectorType,
      generation: 1,
      directoryId: "directoryId",
      applicationId: "applicationId",
      serviceCredential: "serviceCredential",
      accountName: "accountName"
    }
    json_data = post @connector_endpoint, json_data.to_json
    expect_status_details(201)
    connector = JSON.parse(json_data)
    expect(connector["generation"]).to eql(1)
    expect(connector["directoryId"]).to eql("directoryId")
    expect(connector["applicationId"]).to eql("applicationId")
    expect(connector["serviceCredential"]).to eql("serviceCredential")
    expect(connector["accountName"]).to eql("accountName")
  end

  it "should be able to create a generation 2 adls connector" do
    json_data = {
      name: "adls_connector_#{random_id}",
      description: "testadlsconnectordescription",
      type: type,
      storageConnectorType: storageConnectorType,
      generation: 2,
      directoryId: "directoryId",
      applicationId: "applicationId",
      serviceCredential: "serviceCredential",
      accountName: "accountName",
      containerName: "containerName"
    }
    json_data = post @connector_endpoint, json_data.to_json
    expect_status_details(201)
    connector = JSON.parse(json_data)
    expect(connector["generation"]).to eql(2)
    expect(connector["directoryId"]).to eql("directoryId")
    expect(connector["applicationId"]).to eql("applicationId")
    expect(connector["serviceCredential"]).to eql("serviceCredential")
    expect(connector["accountName"]).to eql("accountName")
    expect(connector["containerName"]).to eql("containerName")
  end

  it "should not be able to create a generation 3 adls connector" do
    json_data = {
      name: "adls_connector_#{random_id}",
      description: "testadlsconnectordescription",
      type: type,
      storageConnectorType: storageConnectorType,
      generation: 3,
      directoryId: "directoryId",
      applicationId: "applicationId",
      serviceCredential: "serviceCredential",
      accountName: "accountName",
      containerName: "containerName"
    }
    post @connector_endpoint, json_data.to_json
    expect_status_details(400)
  end

  it "should not be able to create a connector without the directory id" do
    json_data = {
      name: "adls_connector_#{random_id}",
      description: "testadlsconnectordescription",
      type: type,
      storageConnectorType: storageConnectorType,
      generation: 2,
      applicationId: "applicationId",
      serviceCredential: "serviceCredential",
      accountName: "accountName",
      containerName: "containerName"
    }
    post @connector_endpoint, json_data.to_json
    expect_status_details(400)
  end

  it "should not be able to create a connector without the application id" do
    json_data = {
      name: "adls_connector_#{random_id}",
      description: "testadlsconnectordescription",
      type: type,
      storageConnectorType: storageConnectorType,
      generation: 2,
      directoryId: "directoryId",
      serviceCredential: "serviceCredential",
      accountName: "accountName",
      containerName: "containerName"
    }
    post @connector_endpoint, json_data.to_json
    expect_status_details(400)
  end

  it "should not be able to create a connector without the service credential" do
    json_data = {
      name: "adls_connector_#{random_id}",
      description: "testadlsconnectordescription",
      type: type,
      storageConnectorType: storageConnectorType,
      generation: 2,
      directoryId: "directoryId",
      applicationId: "applicationId",
      accountName: "accountName",
      containerName: "containerName"
    }
    post @connector_endpoint, json_data.to_json
    expect_status_details(400)
  end

  it "should not be able to create a connector without the storage account name" do
    json_data = {
      name: "adls_connector_#{random_id}",
      description: "testadlsconnectordescription",
      type: type,
      storageConnectorType: storageConnectorType,
      generation: 2,
      directoryId: "directoryId",
      applicationId: "applicationId",
      serviceCredential: "serviceCredential",
      containerName: "containerName"
    }
    post @connector_endpoint, json_data.to_json
    expect_status_details(400)
  end

  it "should not be able to create a gen 2 connector without container name" do
    json_data = {
      name: "adls_connector_#{random_id}",
      description: "testadlsconnectordescription",
      type: type,
      storageConnectorType: storageConnectorType,
      generation: 2,
      directoryId: "directoryId",
      applicationId: "applicationId",
      serviceCredential: "serviceCredential",
      accountName: "accountName",
    }
    post @connector_endpoint, json_data.to_json
    expect_status_details(400)
  end

  it "should be able to update the service credential" do
    json_data = {
      name: "adls_connector_#{random_id}",
      description: "testadlsconnectordescription",
      type: type,
      storageConnectorType: storageConnectorType,
      generation: 2,
      directoryId: "directoryId",
      applicationId: "applicationId",
      serviceCredential: "serviceCredential",
      accountName: "accountName",
      containerName: "containerName"
    }
    json_data = post @connector_endpoint, json_data.to_json
    expect_status_details(201)
    connector = JSON.parse(json_data)
    connector["serviceCredential"] = "anothercredential"

    json_data = put "#{@connector_endpoint}/#{connector["name"]}", connector.to_json
    expect_status_details(200)
    connector = JSON.parse(json_data)
    expect(connector["serviceCredential"]).to eql("anothercredential")
  end

  it "should be able to update the application id" do
    json_data = {
      name: "adls_connector_#{random_id}",
      description: "testadlsconnectordescription",
      type: type,
      storageConnectorType: storageConnectorType,
      generation: 2,
      directoryId: "directoryId",
      applicationId: "applicationId",
      serviceCredential: "serviceCredential",
      accountName: "accountName",
      containerName: "containerName"
    }
    json_data = post @connector_endpoint, json_data.to_json
    expect_status_details(201)
    connector = JSON.parse(json_data)
    connector["applicationId"] = "anotherAppId"

    json_data = put "#{@connector_endpoint}/#{connector["name"]}", connector.to_json
    expect_status_details(200)
    connector = JSON.parse(json_data)
    expect(connector["applicationId"]).to eql("anotherAppId")
  end
end