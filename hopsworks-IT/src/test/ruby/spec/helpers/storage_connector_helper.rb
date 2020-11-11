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

module StorageConnectorHelper

  def get_jdbc_storate_connector(project_id, featurestore_id, name)
    get_storage_connector(project_id, featurestore_id, "JDBC", name)
  end

  def get_storage_connector(project_id, featurestore_id, type, name)
    connectors_json = get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/storageconnectors/#{type}"
    expect_status(200)
    connectors = JSON.parse(connectors_json)

    connectors.select{|connector| connector['name'] == name}[0]
  end

  def create_hopsfs_connector(project_id, featurestore_id, datasetName: "Resources")
    type = "featurestoreHopsfsConnectorDTO"
    storageConnectorType = "HopsFS"
    create_hopsfs_connector_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/featurestores/" + featurestore_id.to_s + "/storageconnectors/HOPSFS"
    hopsfs_connector_name = "hopsfs_connector_#{random_id}"
    json_data = {
        name: hopsfs_connector_name,
        description: "testhopsfsconnectordescription",
        type: type,
        storageConnectorType: storageConnectorType,
        datasetName: datasetName
    }
    json_data = json_data.to_json
    json_result = post create_hopsfs_connector_endpoint, json_data
    return json_result, hopsfs_connector_name
  end

  def update_hopsfs_connector(project_id, featurestore_id, connector_id, datasetName: "Resources")
    type = "featurestoreHopsfsConnectorDTO"
    storageConnectorType = "HopsFS"
    update_hopsfs_connector_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/featurestores/" + featurestore_id.to_s + "/storageconnectors/HOPSFS/" + connector_id.to_s
    hopsfs_connector_name = "hopsfs_connector_#{random_id}"
    json_data = {
        name: hopsfs_connector_name,
        description: "testhopsfsconnectordescription",
        type: type,
        storageConnectorType: storageConnectorType,
        datasetName: datasetName
    }
    json_data = json_data.to_json
    json_result = put update_hopsfs_connector_endpoint, json_data
    return json_result, hopsfs_connector_name
  end

  def create_jdbc_connector(project_id, featurestore_id, connectionString: "jdbc://test")
    type = "featurestoreJdbcConnectorDTO"
    storageConnectorType = "JDBC"
    create_jdbc_connector_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/featurestores/" + featurestore_id.to_s + "/storageconnectors/JDBC"
    jdbc_connector_name = "jdbc_connector_#{random_id}"
    json_data = {
        name: jdbc_connector_name,
        description: "testfeaturegroupdescription",
        type: type,
        storageConnectorType: storageConnectorType,
        connectionString: connectionString,
        arguments: "test1,test2"
    }
    json_data = json_data.to_json
    json_result = post create_jdbc_connector_endpoint, json_data
    return json_result, jdbc_connector_name
  end

  def update_jdbc_connector(project_id, featurestore_id, connector_id, connectionString: "jdbc://test")
    type = "featurestoreJdbcConnectorDTO"
    storageConnectorType = "JDBC"
    update_jdbc_connector_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/featurestores/" + featurestore_id.to_s + "/storageconnectors/JDBC/" + connector_id.to_s
    jdbc_connector_name = "jdbc_connector_#{random_id}"
    json_data = {
        name: jdbc_connector_name,
        description: "testfeaturegroupdescription",
        type: type,
        storageConnectorType: storageConnectorType,
        connectionString: connectionString,
        arguments: "test1,test2"
    }
    json_data = json_data.to_json
    json_result = put update_jdbc_connector_endpoint, json_data
    return json_result, jdbc_connector_name
  end

  def create_s3_connector_with_or_without_access_key_and_secret_key(project_id, featurestore_id, with_access_and_secret_key,
                                                                    access_key, secret_key, bucket: "test")
    type = "featurestoreS3ConnectorDTO"
    storageConnectorType = "S3"
    create_s3_connector_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/featurestores/" + featurestore_id.to_s + "/storageconnectors/S3"
    s3_connector_name = "s3_connector_#{random_id}"
    json_data = {
        name: s3_connector_name,
        description: "tests3connectordescription",
        type: type,
        storageConnectorType: storageConnectorType,
        bucket: bucket
    }
    if with_access_and_secret_key
      json_data["secretKey"] = access_key
      json_data["accessKey"] = secret_key
    else

    end
    json_data = json_data.to_json
    json_result = post create_s3_connector_endpoint, json_data
    return json_result, s3_connector_name
  end

  def create_s3_connector_without_encryption(project_id, featurestore_id, bucket: "test")
    type = "featurestoreS3ConnectorDTO"
    storageConnectorType = "S3"
    create_s3_connector_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/featurestores/" + featurestore_id.to_s + "/storageconnectors/S3"
    s3_connector_name = "s3_connector_#{random_id}"
    json_data = {
        name: s3_connector_name,
        description: "tests3connectordescription",
        type: type,
        storageConnectorType: storageConnectorType,
        bucket: bucket,
        secretKey: "test",
        accessKey: "test"
    }
    json_data = json_data.to_json
    json_result = post create_s3_connector_endpoint, json_data
    return json_result, s3_connector_name
  end

  def create_s3_connector_with_encryption(project_id, featurestore_id, with_encryption_key, encryption_algorithm,
                                          encryption_key, access_key, secret_key,
                                          bucket: "test")
    type = "featurestoreS3ConnectorDTO"
    storageConnectorType = "S3"
    create_s3_connector_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/featurestores/" + featurestore_id.to_s + "/storageconnectors/S3"
    s3_connector_name = "s3_connector_#{random_id}"
    json_data = {
        name: s3_connector_name,
        description: "tests3connectordescription",
        type: type,
        storageConnectorType: storageConnectorType,
        bucket: bucket,
        secretKey: access_key,
        accessKey: secret_key
    }

    if with_encryption_key
      json_data["serverEncryptionAlgorithm"] = encryption_algorithm
      json_data["serverEncryptionKey"] = encryption_key
    else
      json_data["serverEncryptionAlgorithm"] = encryption_algorithm
    end

    json_data = json_data.to_json
    json_result = post create_s3_connector_endpoint, json_data
    return json_result, s3_connector_name
  end


  def update_s3_connector(project_id, featurestore_id, connector_id, s3_connector_name, with_access_keys, bucket:
          "test")
    type = "featurestoreS3ConnectorDTO"
    storageConnectorType = "S3"
    update_s3_connector_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/featurestores/" + featurestore_id.to_s + "/storageconnectors/S3/" + connector_id.to_s
    json_data = {
        name: s3_connector_name,
        description: "tests3connectordescription",
        type: type,
        storageConnectorType: storageConnectorType,
        bucket: bucket
    }
    if with_access_keys
      json_data["secretKey"] = "test2"
      json_data["accessKey"] = "test2"
    end
    json_data = json_data.to_json
    json_result = put update_s3_connector_endpoint, json_data
    return json_result, s3_connector_name
  end

  def with_jdbc_connector(project_id)
    featurestore_id = get_featurestore_id(project_id)
    json_result, connector_name = create_jdbc_connector(project_id, featurestore_id, connectionString: "jdbc://test2")
    parsed_json = JSON.parse(json_result)
    expect_status(201)
    connector_id = parsed_json["id"]
    @jdbc_connector_id = connector_id
  end

  def get_jdbc_connector_id
    return @jdbc_connector_id
  end

  def get_hopsfs_training_datasets_connector(project_name)
    connector_name = project_name + "_Training_Datasets"
    return FeatureStoreHopsfsConnector.find_by(name: connector_name)
  end

  def get_s3_connector_id
    return @s3_connector_id
  end

  def with_s3_connector(project_id)
    encryption_algorithm = "AES256"
    encryption_key = "Test"
    secret_key = "test"
    access_key = "test"
    with_encryption_key = true
    featurestore_id = get_featurestore_id(project_id)
    json_result, connector_name = create_s3_connector_with_encryption(project_id, featurestore_id,
                                                                      with_encryption_key, encryption_algorithm,
                                                                      encryption_key, access_key, secret_key, bucket:
                                                                          "testbucket")
    parsed_json = JSON.parse(json_result)
    expect_status_details(201)
    connector_id = parsed_json["id"]
    @s3_connector_id = connector_id
  end
end