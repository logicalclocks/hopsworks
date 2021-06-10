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
    json_result = get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/storageconnectors/"
    connectors = JSON.parse(json_result)
    connectors.select{|c| c['storageConnectorType'].eql?(type)}.map { |c| c.with_indifferent_access }
  end

  def get_storage_connector(project_id, featurestore_id, name)
    get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/storageconnectors/#{name}"
  end

  def create_hopsfs_connector(project_id, featurestore_id, datasetName: "Resources")
    type = "featurestoreHopsfsConnectorDTO"
    storageConnectorType = "HOPSFS"
    create_hopsfs_connector_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/storageconnectors"
    hopsfs_connector_name = "hopsfs_connector_#{random_id}"
    json_data = {
        name: hopsfs_connector_name,
        description: "testhopsfsconnectordescription",
        type: type,
        storageConnectorType: storageConnectorType,
        datasetName: datasetName
    }
    json_result = post create_hopsfs_connector_endpoint, json_data.to_json
    [json_result, hopsfs_connector_name]
  end

  def update_hopsfs_connector(project_id, featurestore_id, connector_name, datasetName: "Resources")
    type = "featurestoreHopsfsConnectorDTO"
    storageConnectorType = "HOPSFS"
    update_hopsfs_connector_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/storageconnectors/#{connector_name}"
    json_data = {
        name: connector_name,
        description: "testhopsfsconnectordescription",
        type: type,
        storageConnectorType: storageConnectorType,
        datasetName: datasetName
    }
    json_result = put update_hopsfs_connector_endpoint, json_data.to_json
    [json_result, connector_name]
  end

  def create_jdbc_connector(project_id, featurestore_id, connectionString: "jdbc://test")
    type = "featurestoreJdbcConnectorDTO"
    storageConnectorType = "JDBC"
    create_jdbc_connector_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/storageconnectors"
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
    update_jdbc_connector_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/storageconnectors/#{connector_name}"
    json_data = {
        name: connector_name,
        description: "testfeaturegroupdescription",
        type: type,
        storageConnectorType: storageConnectorType,
        connectionString: connectionString,
        arguments: "test1,test2"
    }
    put update_jdbc_connector_endpoint, json_data.to_json
  end

  def create_s3_connector(project_id, featurestore_id, encryption_algorithm: nil, encryption_key: nil,
                          access_key: nil, secret_key: nil, bucket: "test")
    type = "featurestoreS3ConnectorDTO"
    storageConnectorType = "S3"
    create_s3_connector_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/storageconnectors"
    s3_connector_name = "s3_connector_#{random_id}"
    json_data = {
        name: s3_connector_name,
        description: "tests3connectordescription",
        type: type,
        storageConnectorType: storageConnectorType,
        bucket: bucket,
    }

    json_data["serverEncryptionAlgorithm"] = encryption_algorithm
    unless encryption_key.nil?
      json_data["serverEncryptionKey"] = encryption_key
    end

    unless access_key.nil?
      json_data["secretKey"] = access_key
      json_data["accessKey"] = secret_key
    end

    json_result = post create_s3_connector_endpoint, json_data.to_json
    [json_result, s3_connector_name]
  end

  def update_s3_connector(project_id, featurestore_id, connector_name, access_key: nil, secret_key: nil, bucket: "test")
    type = "featurestoreS3ConnectorDTO"
    storageConnectorType = "S3"
    update_s3_connector_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/storageconnectors/#{connector_name}"
    json_data = {
        name: connector_name,
        description: "tests3connectordescription",
        type: type,
        storageConnectorType: storageConnectorType,
        bucket: bucket
    }
    unless secret_key.nil?
      json_data["secretKey"] = secret_key
    end
    unless access_key.nil?
      json_data["accessKey"] = access_key
    end
    json_result = put update_s3_connector_endpoint, json_data.to_json
    [json_result, connector_name]
  end

  def create_redshift_connector(project_id, featurestore_id, redshift_connector_name: nil, clusterIdentifier: "redshift-connector",
                                databaseUserName: "awsUser",  databasePassword: nil, iamRole: nil)
    type = "featurestoreRedshiftConnectorDTO"
    storageConnectorType = "REDSHIFT"
    create_redshift_connector_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/storageconnectors/"
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
    json_result = post create_redshift_connector_endpoint, json_data.to_json
    [json_result, redshift_connector_name]
  end

  def create_snowflake_connector(project_id, featurestore_id)
    endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/storageconnectors/"
    type = "featurestoreSnowflakeConnectorDTO"
    storageConnectorType = "SNOWFLAKE"
    connector = { name: "snowflake_connector_#{random_id}",
                   description: "test snowflake connector",
                   type: type,
                   storageConnectorType: storageConnectorType,
                   sfOptions: [{name:"sfTimezone", value:"spark"}, {name: "sfCompress", value: "true"}],
                   database: "test",
                   password: "123456PWD",
                   role: "role",
                   schema: "PUBLIC",
                   url: "http://123456.eu-central-1.snowflakecomputing.com",
                   user: "user0",
                   warehouse: "warehouse"
    }
    post endpoint, connector.to_json
  end

  def update_redshift_connector(project_id, featurestore_id, connector_name, redshift_connector_json)
    update_redshift_connector_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/storageconnectors/#{connector_name}"
    redshift_connector_json["type"] = "featurestoreRedshiftConnectorDTO"
    redshift_connector_json["storageConnectorType"] = "REDSHIFT"
    put update_redshift_connector_endpoint, redshift_connector_json.to_json
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
    return FeatureStoreConnector.find_by(name: connector_name)
  end

  def get_s3_connector_id
    @s3_connector_id
  end

  def with_s3_connector(project_id)
    featurestore_id = get_featurestore_id(project_id)
    json_result, _ = create_s3_connector(project_id, featurestore_id,
                                         encryption_algorithm: "AES256",
                                         access_key: "test", secret_key: "test",
                                         bucket: "testbucket")

    parsed_json = JSON.parse(json_result)
    expect_status_details(201)
    connector_id = parsed_json["id"]
    @s3_connector_id = connector_id
  end

  def delete_connector(project_id, featurestore_id, name)
    delete "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/storageconnectors/#{name}"
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

  def check_snowflake_connector_update(json_body, connector)
    expect(json_body[:name]).to eq connector[:name]
    expect(json_body[:description]).to eq connector[:description]
    expect(json_body[:url]).to eq connector[:url]
    expect(json_body[:warehouse]).to eq connector[:warehouse]
    expect(json_body[:schema]).to eq connector[:schema]
    expect(json_body[:database]).to eq connector[:database]
    expect(json_body[:user]).to eq connector[:user]
    expect(json_body[:password]).to eq connector[:password]
    expect(json_body[:token]).to eq connector[:token]
    expect(json_body[:table]).to eq connector[:table]
    expect(json_body[:role]).to eq connector[:role]
    expect(json_body[:sfOptions]).to eq connector[:sfOptions]
  end
end