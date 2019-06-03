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

module KafkaHelper

  def with_kafka_topic(project_id)
    with_kafka_schema(project_id)
    @topic ||= create_topic(project_id, @schema[:name], 1)
  end

  def with_kafka_schema(project_id)
    @schema ||= create_schema(project_id)
  end

  def get_topic
    @topic
  end

  def get_schema
    @schema
  end

  def create_topic(project_id, schema_name=nil, schema_version = nil)
    if (schema_name.nil?)
      _, schema_name = add_schema(project_id)
      schema_version = 1
    end
    _, topic_name = add_topic(project_id, schema_name, schema_version)
    ProjectTopics.find_by(project_id:project_id, topic_name:topic_name)
  end


  def create_schema(project_id, kafka_schema_name = "kafka_schema_#{random_id}")
    schema_name = add_schema(project_id, kafka_schema_name)
    SchemaTopics.find_by(name:schema_name, version: 1)
  end

  def add_schema(project_id, kafka_schema_name = "kafka_schema_#{random_id}")
    add_schema_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/kafka/schema/add"
    json_data = {
        name: kafka_schema_name,
        contents: "[]",
        versions: []
    }
    json_data = json_data.to_json
    json_result = post add_schema_endpoint, json_data
    return json_result, kafka_schema_name
  end

  def add_topic(project_id, schema_name, schema_version)
    add_topic_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/kafka/topic/add"
    kafka_topic_name = "kafka_topic_#{random_id}"
    json_data = {
        name: kafka_topic_name,
        numOfPartitions: 2,
        numOfReplicas: 1,
        schemaName: schema_name,
        schemaVersion: schema_version
    }
    json_data = json_data.to_json
    json_result = post add_topic_endpoint, json_data
    return json_result, kafka_topic_name
  end

  def delete_schema(project_id, kafka_schema_name, kafka_schema_version)
    delete_schema_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s +
        "/kafka/removeSchema/#{kafka_schema_name}/#{kafka_schema_version}"
    delete delete_schema_endpoint
  end

  def delete_topic(project_id, kafka_topic_name)
    delete_kafka_topic = "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s +
        "/kafka/topic/#{kafka_topic_name}/remove"
    delete delete_kafka_topic
  end

end
