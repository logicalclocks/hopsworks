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
  before :all do
    # ensure bigquery storage connectors are enabled
    @enable_bigquery_storage_connector = getVar('enable_bigquery_storage_connectors')
    setVar('enable_bigquery_storage_connectors', "true")

    @cleanup = true
    with_valid_project
    # create dummy truststore/keystore files in hopsfs
    create_test_files
  end

  after :all do
    clean_all_test_projects(spec: "bigquery_storage_connector") if defined?(@cleanup) && @cleanup
    setVar('enable_bigquery_storage_connectors', @enable_bigquery_storage_connector[:value])
  end

  after :each do
    create_session(@project[:username], "Pass123")
  end

  def create_connector_materializationDataset
    project = get_project
    key_path =  "/Projects/#{@project['projectname']}/Resources/sampleKey.json"
    parent_project = 'feature-store-testing'
    material_dataset = 'views'
    create_data = {
      keyPath: key_path,
      parentProject: parent_project,
      materializationDataset: material_dataset
    }
    json_result, connector_name = create_bigquery_connector(project.id,  get_featurestore_id(project.id), create_data)
    return [JSON.parse(json_result),connector_name]
  end

  it 'should create a bigquery connector with queryProject,dataset,table' do
    project = get_project
    featurestore_id = get_featurestore_id(project.id)
    key_path =  "/Projects/#{@project['projectname']}/Resources/sampleKey.json"
    parent_project = 'feature-store-testing'
    json_data = {
      keyPath: key_path,
      parentProject: parent_project,
      queryProject: 'feature-store-testing',
      dataset: 'temp',
      queryTable: 'shakespeare',
      arguments: [
        {
          "name": "key1",
          "value": "option1"
        },
        {
          "name": "key2",
          "value": "option2"
        }
      ]
    }
    json_result, connector_name = create_bigquery_connector(project.id, featurestore_id, json_data)
    parsed_json = JSON.parse(json_result)
    expect_status_details(201)
    expect(parsed_json.key?('id')).to be true
    expect(parsed_json['name']).to eql(connector_name)
    expect(parsed_json['description']).to eql('test bigq connector')
    expect(parsed_json['keyPath']).to eql(key_path)
    expect(parsed_json['parentProject']).to eql(parent_project)
    expect(parsed_json['queryProject']).to eql('feature-store-testing')
    expect(parsed_json['dataset']).to eql('temp')
    expect(parsed_json['queryTable']).to eql('shakespeare')
    expect(parsed_json['arguments'][0]['name']).to eql('key1')
    expect(parsed_json['arguments'][0]['value']).to eql('option1')
    expect(parsed_json['arguments'][1]['name']).to eql('key2')
    expect(parsed_json['arguments'][1]['value']).to eql('option2')
  end

  it 'should create a bigquery connector with materializationDataset' do
    parsed_json, connector_name = create_connector_materializationDataset
    key_path =  "/Projects/#{@project['projectname']}/Resources/sampleKey.json"
    parent_project = 'feature-store-testing'
    material_dataset = 'views'

    expect_status_details(201)
    expect(parsed_json.key?('id')).to be true
    expect(parsed_json['name']).to eql(connector_name)
    expect(parsed_json['description']).to eql('test bigq connector')
    expect(parsed_json['keyPath']).to eql(key_path)
    expect(parsed_json['parentProject']).to eql(parent_project)
    expect(parsed_json['materializationDataset']).to eql(material_dataset)
  end

  it 'should update bigquery connector with same name' do
    parsed_json, connector_name = create_connector_materializationDataset
    expect_status_details(201)
    type_dto = 'featurestoreBigqueryConnectorDTO'
    storage_connector_type = 'BIGQUERY'
    description='test bigquery updated'
    dataset= 'temp'
    table='shakespeare'
    query_project = 'query-project-dummy'
    key_path_updated =  "/Projects/#{@project['projectname']}/Resources/newSampleKey.json"
    parent_project = 'feature-store-testing'

    update_data={
      type: type_dto,
      storageConnectorType: storage_connector_type,
      name: connector_name,
      description: description,
      keyPath: key_path_updated,
      parentProject: parent_project,
      queryProject: query_project,
      dataset: dataset,
      queryTable: table
    }
    project = get_project
    update_result=update_connector_json(project.id, get_featurestore_id(project.id), connector_name, update_data )
    parsed_result_update=JSON.parse(update_result)
    expect_status_details(200)
    expect(parsed_result_update.key?('id')).to be true
    expect(parsed_result_update.key?('featurestoreId')).to be true
    expect(parsed_result_update['name']).to eql(connector_name)
    expect(parsed_result_update['description']).to eql(description)
    expect(parsed_result_update['keyPath']).to eql(key_path_updated)
    expect(parsed_result_update['parentProject']).to eql(parent_project)
    expect(parsed_result_update['queryProject']).to eql(query_project)
    expect(parsed_result_update['dataset']).to eql(dataset)
    expect(parsed_result_update['queryTable']).to eql(table)
  end

  it 'should fail to update non existing connector' do
    type_dto = 'featurestoreBigqueryConnectorDTO'
    storage_connector_type = 'BIGQUERY'
    connector_name="bigq_connector_#{random_id}"
    description='test gcs updated'
    update_data={
      type: type_dto,
      storageConnectorType: storage_connector_type,
      name: connector_name,
      description: description,
      keyPath: '/path/to/file',
      parentProject: 'test-project'
    }
    project = get_project
    update_result=update_connector_json(project.id, get_featurestore_id(project.id), connector_name, update_data )
    parsed_result_update=JSON.parse(update_result)
    expect_status_details(404)
    error_code=270042
    expect(parsed_result_update['errorCode']).to eql(error_code)
  end

  it 'should fail to update connector with a different name' do
    parsed_json,connector_name = create_connector_materializationDataset
    expect_status_details(201)
    description='test update connector wrong name'
    type_dto = 'featurestoreBigqueryConnectorDTO'
    storage_connector_type = 'BIGQUERY'
    additional_data={
      type: type_dto,
      storageConnectorType: storage_connector_type,
      name: "connector_#{random_id}",
      description: description,
      keyPath: '/path/to/file',
      parentProject: 'test-project'
    }
    project = get_project
    update_result=update_connector_json(project.id, get_featurestore_id(project.id), connector_name, additional_data)

    parsed_result_update=JSON.parse(update_result)
    expect_status_details(400)
    error_code=270136
    expect(parsed_result_update['errorCode']).to eql(error_code)
  end

  it 'should get bigquery connector' do
    parsed_json,connector_name = create_connector_materializationDataset
    expect_status_details(201)
    project=get_project
    result=get_storage_connector(project.id, get_featurestore_id(project.id), connector_name)
    parsed_result=JSON.parse(result)
    expect_status_details(200)
    expect(parsed_result.key?('id')).to be true
    expect(parsed_result['name']).to eql(connector_name)
  end

  it 'should delete bigquery connector' do
    parsed_json,connector_name = create_connector_materializationDataset
    expect_status_details(201)
    project=get_project
    delete_connector(project.id, get_featurestore_id(project.id), connector_name)
    expect_status_details(200)
    expect(test_file(parsed_json["keyPath"])).to be false
  end
end
