=begin
 This file is part of Hopsworks
 Copyright (C) 2022, Logical Clocks AB. All rights reserved

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
  after(:all) {clean_all_test_projects(spec: "gcs_storage_connector") if defined?(@cleanup) && @cleanup}

  before :all do
    @cleanup = true
    @debugOpt = false
    with_valid_project

    # create dummy truststore/keystore files in hopsfs
    create_test_files
  end

  after :each do
    create_session(@project[:username], "Pass123")
  end

  it "should create gcs connector without encryption" do
    project = get_project
    featurestore_id = get_featurestore_id(project.id)

    key_path = "/Projects/#{@project['projectname']}/Resources/sampleKey.json"
    bucket = 'testbucket'
    json_result, connector_name = create_gcs_connector(project.id, featurestore_id, key_path,bucket)

    parsed_json = JSON.parse(json_result)
    expect_status_details(201)
    expect(parsed_json.key?("id")).to be true
    expect(parsed_json["name"]).to eql(connector_name)
    expect(parsed_json["storageConnectorType"]).to eql("GCS")
    expect(parsed_json["description"]).to eql("test gcs connector description")
    expect(parsed_json["featurestoreId"]).to eql(featurestore_id)
    expect(parsed_json["keyPath"]).to eql(key_path)
    expect(parsed_json['bucket']).to eql(bucket)
  end

  it "should create gcs connector with encryption" do
    project = get_project
    featurestore_id = get_featurestore_id(project.id)

    key_path = "/Projects/#{@project['projectname']}/Resources/sampleKey.json"
    bucket = 'testbucket'
    encryption_key = "yFXZv3V+KT++v8/L6HcRqjVPAotNO3beIzcetgTBoSM="
    encryption_hash = "WWHIkhgaK+eDWvkRlLBLqJLF3aaQ0JoT1gOt2WGuiGg="
    algorithm = "AES256"
    json_result, connector_name = create_gcs_connector(project.id, featurestore_id, key_path, bucket, algorithm: algorithm,
                                                       encryption_key: encryption_key,
                                                       encryption_key_hash: encryption_hash)

    parsed_json = JSON.parse(json_result)
    expect_status_details(201)
    expect(parsed_json.key?("id")).to be true
    expect(parsed_json["name"]).to eql(connector_name)
    expect(parsed_json["storageConnectorType"]).to eql("GCS")
    expect(parsed_json["description"]).to eql("test gcs connector description")
    expect(parsed_json["featurestoreId"]).to eql(featurestore_id)
    expect(parsed_json["keyPath"]).to eql(key_path)
    expect(parsed_json["algorithm"]).to eql(algorithm)
    expect(parsed_json["encryptionKey"]).to eql(encryption_key)
    expect(parsed_json["encryptionKeyHash"]).to eql(encryption_hash)
    expect(parsed_json['bucket']).to eql('testbucket')
  end

  it "should update all fields of gcs connector with same name" do
    project = get_project
    featurestore_id = get_featurestore_id(project.id)

    key_path = "/Projects/#{@project['projectname']}/Resources/sampleKey.json"
    bucket = 'testbucket'
    _, connector_name = create_gcs_connector(project.id, featurestore_id, key_path, bucket)
    expect_status_details(201)

    description = "test gcs updated"
    encryption_key = "new key"
    encryption_hash = "new hash"
    algorithm = "AES256"
    key_path = "/Projects/#{@project['projectname']}/Resources/newSampleKey.json"
    bucket = 'updated_bucket'
    additional_data = {
      name: connector_name,
      description: description,
      keyPath: key_path,
      algorithm: algorithm,
      encryptionKey: encryption_key,
      encryptionKeyHash: encryption_hash,
      bucket: bucket
    }

    update_result = update_gcs_connector_json(project.id, featurestore_id, connector_name, additional_data )
    parsed_result_update = JSON.parse(update_result)
    expect_status_details(200)
    expect(parsed_result_update.key?("id")).to be true
    expect(parsed_result_update["name"]).to eql(connector_name)
    expect(parsed_result_update["storageConnectorType"]).to eql("GCS")
    expect(parsed_result_update["description"]).to eql(description)
    expect(parsed_result_update["featurestoreId"]).to eql(featurestore_id)
    expect(parsed_result_update["keyPath"]).to eql(key_path)
    expect(parsed_result_update["encryptionKey"]).to eql(encryption_key)
    expect(parsed_result_update["encryptionKeyHash"]).to eql(encryption_hash)
    expect(parsed_result_update['bucket']).to eql(bucket)
  end

  it "should update only encryption secret of gcs connector with same name" do
    project = get_project
    featurestore_id = get_featurestore_id(project.id)

    key_path = "/Projects/#{@project['projectname']}/Resources/sampleKey.json"
    bucket = 'testbucket'
    encryption_key = "yFXZv3V+KT++v8/L6HcRqjVPAotNO3beIzcetgTBoSM="
    encryption_hash = "WWHIkhgaK+eDWvkRlLBLqJLF3aaQ0JoT1gOt2WGuiGg="
    algorithm = "AES256"
    _, connector_name = create_gcs_connector(project.id, featurestore_id, key_path, bucket, algorithm: algorithm,
                                                       encryption_key: encryption_key,
                                                       encryption_key_hash: encryption_hash)
    expect_status_details(201)

    description = "test gcs updated"
    encryption_key = "new key"
    encryption_hash = "new hash"
    algorithm = "AES256"
    key_path = "/Projects/#{@project['projectname']}/Resources/newSampleKey.json"
    additional_data = {
      name: connector_name,
      description: description,
      keyPath: key_path,
      algorithm: algorithm,
      encryptionKey: encryption_key,
      encryptionKeyHash: encryption_hash,
      bucket: bucket
    }

    update_result = update_gcs_connector_json(project.id, featurestore_id, connector_name, additional_data )
    parsed_result_update = JSON.parse(update_result)
    expect_status_details(200)
    expect(parsed_result_update.key?("id")).to be true
    expect(parsed_result_update["name"]).to eql(connector_name)
    expect(parsed_result_update["storageConnectorType"]).to eql("GCS")
    expect(parsed_result_update["description"]).to eql(description)
    expect(parsed_result_update["featurestoreId"]).to eql(featurestore_id)
    expect(parsed_result_update["keyPath"]).to eql(key_path)
    expect(parsed_result_update["encryptionKey"]).to eql(encryption_key)
    expect(parsed_result_update["encryptionKeyHash"]).to eql(encryption_hash)
    expect(parsed_result_update['bucket']).to eql(bucket)
  end

  it "should fail to update non existing gcs connector" do
    project = get_project
    featurestore_id = get_featurestore_id(project.id)

    key_path = "/Projects/#{@project['projectname']}/Resources/newSampleKey.json"
    bucket = 'testbucket'
    connector_name = "gcs_connector_#{random_id}"
    description = "test gcs updated"
    additional_data = {
      name: connector_name,
      description: description,
      keyPath: key_path,
      bucket: bucket
    }
    update_result = update_gcs_connector_json(project.id, featurestore_id, connector_name, additional_data)

    parsed_result_update = JSON.parse(update_result)
    expect_status_details(404)
    expect(parsed_result_update["errorCode"]).to eql(270042)
  end

  it "should fail to update gcs connector with a different name" do
    project = get_project
    featurestore_id = get_featurestore_id(project.id)
    key_path = "/Projects/#{@project['projectname']}/Resources/sampleKey.json"
    bucket = 'testbucket'
    json_result, connector_name = create_gcs_connector(project.id, featurestore_id, key_path, bucket)
    expect_status_details(201)

    description = "test gcs wrong name"
    additional_data = {
      name: "gcs_connector_#{random_id}",
      description: description,
      keyPath: key_path,
      bucket: bucket
    }
    update_result = update_gcs_connector_json(project.id, featurestore_id, connector_name, additional_data)

    parsed_result_update = JSON.parse(update_result)
    expect_status_details(400)
    expect(parsed_result_update["errorCode"]).to eql(270136)
  end

  it "should get gcs connector" do
    project = get_project
    featurestore_id = get_featurestore_id(project.id)
    key_path = "/Projects/#{@project['projectname']}/Resources/sampleKey.json"
    bucket = 'testbucket'
    _, connector_name = create_gcs_connector(project.id, featurestore_id, key_path, bucket)
    expect_status_details(201)

    result = get_storage_connector(project.id, featurestore_id, connector_name)
    parsed_result = JSON.parse(result)
    expect_status_details(200)
    expect(parsed_result.key?("id")).to be true
    expect(parsed_result["name"]).to eql(connector_name)
  end

  it "should update connector with encryption to without encryption" do
    project = get_project
    featurestore_id = get_featurestore_id(project.id)
    key_path = "/Projects/#{@project['projectname']}/Resources/sampleKey.json"
    bucket = 'testbucket'
    encryption_key = "new key"
    encryption_hash = "new hash"
    algorithm = "AES256"
    _, connector_name = create_gcs_connector(project.id, featurestore_id, key_path, bucket, algorithm:algorithm,
                                             encryption_key:encryption_key, encryption_key_hash:encryption_hash )
    expect_status_details(201)
    description = "test gcs updated"
    key_path = "/Projects/#{@project['projectname']}/Resources/newSampleKey.json"
    additional_data = {
      name: connector_name,
      description: description,
      keyPath: key_path,
      bucket: bucket,
      algorithm: nil
    }

    update_result = update_gcs_connector_json(project.id, featurestore_id, connector_name, additional_data )
    parsed_result_update = JSON.parse(update_result)
    expect_status_details(200)
    expect(parsed_result_update.key?("id")).to be true
    expect(parsed_result_update["name"]).to eql(connector_name)
    expect(parsed_result_update["storageConnectorType"]).to eql("GCS")
    expect(parsed_result_update["description"]).to eql(description)
    expect(parsed_result_update["featurestoreId"]).to eql(featurestore_id)
    expect(parsed_result_update["keyPath"]).to eql(key_path)
    expect(parsed_result_update['bucket']).to eql(bucket)
    expect(parsed_result_update.key?("encryptionKey")).to be false
    expect(parsed_result_update.key?("encryptionKeyHash")).to be false
    expect(parsed_result_update.key?("algorithm")).to be false
  end

  it "should delete gcs connector" do
    project = get_project
    featurestore_id = get_featurestore_id(project.id)
    key_path = "/Projects/#{@project['projectname']}/Resources/sampleKey.json"
    bucket = 'testbucket'
    _, connector_name = create_gcs_connector(project.id, featurestore_id, key_path, bucket)
    expect_status_details(201)

    delete_connector(project.id, featurestore_id, connector_name)
    expect_status_details(200)
    expect(test_file(key_path)).to be false
  end

end
