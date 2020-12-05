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

  def get_storage_connectors(project_id, featurestore_id, type)
    get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/storageconnectors/#{type}"
  end

  def get_jdbc_storate_connector(project_id, featurestore_id, name)
    get_storage_connector(project_id, featurestore_id, "JDBC", name)
  end

  def get_storage_connector(project_id, featurestore_id, type, name)
    connectors_json = get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/storageconnectors/#{type}"
    expect_status(200)
    connectors = JSON.parse(connectors_json)

    connectors.select{|connector| connector['name'] == name}[0]
  end

  def get_storage_connector_by_name(project_id, featurestore_id, type, name)
    get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/storageconnectors/#{type}/#{name}"
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

  def update_hopsfs_connector(project_id, featurestore_id, connector_name, datasetName: "Resources")
    type = "featurestoreHopsfsConnectorDTO"
    storageConnectorType = "HopsFS"
    update_hopsfs_connector_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/featurestores/" + featurestore_id.to_s + "/storageconnectors/HOPSFS/" + connector_name
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

  def update_jdbc_connector(project_id, featurestore_id, connector_name, connectionString: "jdbc://test")
    type = "featurestoreJdbcConnectorDTO"
    storageConnectorType = "JDBC"
    update_jdbc_connector_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/featurestores/" + featurestore_id.to_s + "/storageconnectors/JDBC/" + connector_name
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


  def update_s3_connector(project_id, featurestore_id, connector_name, s3_connector_name, with_access_keys, bucket:
          "test")
    type = "featurestoreS3ConnectorDTO"
    storageConnectorType = "S3"
    update_s3_connector_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/featurestores/" + featurestore_id.to_s + "/storageconnectors/S3/" + connector_name
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

  def create_redshift_connector(project_id, featurestore_id, redshift_connector_name: nil, clusterIdentifier: "redshift-connector",
                                databaseUserName: "awsUser",  databasePassword: nil, iamRole: nil)
    type = "featurestoreRedshiftConnectorDTO"
    storageConnectorType = "REDSHIFT"
    create_redshift_connector_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/storageconnectors/#{storageConnectorType}"
    redshift_connector_name ||= "redshift_connector_#{random_id}"
    json_data = {
        name: redshift_connector_name,
        description: "test feature store connector description",
        type: type,
        storageConnectorType: storageConnectorType,
        clusterIdentifier: clusterIdentifier,
        databaseDriver: "com.amazon.redshift.jdbc42.Driver",
        databaseEndpoint: "abc123xyz789.us-west-1.redshift.amazonaws.com",
        databaseName: "dev",
        databasePort: 5439,
        tableName: "test",
        databaseUserName: databaseUserName,
        databasePassword: databasePassword,
        iamRole: iamRole,
        arguments: "test1,test2"
    }
    json_data = json_data.to_json
    json_result = post create_redshift_connector_endpoint, json_data
    return json_result, redshift_connector_name
  end

  def update_redshift_connector(project_id, featurestore_id, connector_name, redshift_connector_json)
    type = "featurestoreRedshiftConnectorDTO"
    storageConnectorType = "REDSHIFT"
    update_redshift_connector_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/storageconnectors/#{storageConnectorType}/#{connector_name}"
    redshift_connector_json["type"] = type
    redshift_connector_json["storageConnectorType"] = storageConnectorType
    json_data = redshift_connector_json.to_json
    json_result = put update_redshift_connector_endpoint, json_data
    return json_result
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
    @jdbc_connector_id
  end

  def get_hopsfs_training_datasets_connector(project_name)
    connector_name = project_name + "_Training_Datasets"
    return FeatureStoreHopsfsConnector.find_by(name: connector_name)
  end

  def get_s3_connector_id
    @s3_connector_id
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

  def delete_connector(project_id, featurestore_id, type, name)
    delete "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/storageconnectors/#{type}/#{name}"
  end

  def with_redshift_connectors
    with_valid_project
    project1 = create_project
    featurestore = get_featurestores_checked(@project[:id])[0]
    create_redshift_connector(@project[:id], featurestore["featurestoreId"], redshift_connector_name: "redshift_connector_iam",
                              iamRole: "arn:aws:iam::123456789012:role/test-role-p1")
    create_redshift_connector(@project[:id], featurestore["featurestoreId"], redshift_connector_name: "redshift_connector_pwd",
                              databasePassword: "awsUserPwd")
    project1_featurestore = get_featurestores_checked(project1[:id])[0]
    create_redshift_connector(project1[:id], project1_featurestore["featurestoreId"], redshift_connector_name: "redshift_connector_iam",
                              iamRole: "arn:aws:iam::123456789012:role/test-role-p2")
    create_redshift_connector(project1[:id], project1_featurestore["featurestoreId"], redshift_connector_name: "redshift_connector_pwd",
                              databasePassword: "awsUserPwd")
    return @project, project1
  end

  def check_redshift_connector_update(json_body, connector)
    expect(json_body[:name]).to eq connector[:name]
    expect(json_body[:description]).to eq connector[:description]
    expect(json_body[:clusterIdentifier]).to eq connector[:clusterIdentifier]
    expect(json_body[:databaseDriver]).to eq connector[:databaseDriver]
    expect(json_body[:databaseEndpoint]).to eq connector[:databaseEndpoint]
    expect(json_body[:databaseName]).to eq connector[:databaseName]
    expect(json_body[:databaseUserName]).to eq connector[:databaseUserName]
    expect(json_body[:autoCreate]).to eq connector[:autoCreate]
    expect(json_body[:databaseGroup]).to eq connector[:databaseGroup]
    expect(json_body[:databasePort]).to eq connector[:databasePort]
    expect(json_body[:tableName]).to eq connector[:tableName]
    expect(json_body[:arguments]).to eq connector[:arguments]
    expect(json_body[:iamRole]).to eq connector[:iamRole]
    expect(json_body[:databasePassword]).to eq connector[:databasePassword]
  end
end