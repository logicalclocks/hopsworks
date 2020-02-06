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

  def create_topics(project_id, amount = 10, schema_name = nil , schema_version = nil)
    for i in 1..amount 
      create_topic(project_id, schema_name, schema_version)
    end
    get_project_topics(project_id)
    json_body
  end

  def create_topic(project_id, schema_name=nil, schema_version = nil)
    if (schema_name.nil?)
      _, schema_name = add_schema(project_id)
      schema_version = 1
    end
    _, topic_name = add_topic(project_id, schema_name, schema_version)
    ProjectTopics.find_by(project_id:project_id, topic_name:topic_name)
    return topic_name
  end


  def create_schema(project_id, kafka_schema_name = "kafka_schema_#{random_id}")
    schema_name = add_schema(project_id, kafka_schema_name)
    Subjects.find_by(subject: schema_name, version: 1, project_id: project_id)
  end

  def add_schema(project_id, kafka_schema_name = "kafka_schema_#{random_id}")
    add_schema_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/kafka/subjects/" + kafka_schema_name + "/versions"
    json_data = {
       schema: "[]"
    }
    json_data = json_data.to_json
    json_result = post add_schema_endpoint, json_data
    return json_result, kafka_schema_name
  end

  def add_topic(project_id, schema_name, schema_version)
    add_topic_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/kafka/topics"
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
	    "/kafka/subjects/#{kafka_schema_name}/versions/#{kafka_schema_version}"
    delete delete_schema_endpoint
  end

  def delete_topic(project_id, kafka_topic_name)
    delete_kafka_topic = "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s +
        "/kafka/topics/#{kafka_topic_name}"
    delete delete_kafka_topic
  end

  def get_project_topics(project_id, more = "")
    get "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/kafka/topics?filter_by=shared:false" + more
  end

  def get_all_topics(project_id, more = "")
    get "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/kafka/topics" + more 
  end

  def share_topic(owner_project, topic_name, dest_project)
	  put "#{ENV['HOPSWORKS_API']}/project/#{owner_project[:id]}/kafka/topics/#{topic_name}/shared/#{dest_project[:inode_name]}"
  end

  def unshare_topic_within_owner_project(owner_project, topic_name, dest_project)
    delete "#{ENV['HOPSWORKS_API']}/project/#{owner_project[:id]}/kafka/topics/#{topic_name}/shared/#{dest_project[:inode_name]}"
  end

  def unshare_topic_within_dest_project(dest_project, topic_name)
    delete "#{ENV['HOPSWORKS_API']}/project/#{dest_project[:id]}/kafka/topics/#{topic_name}/shared"
  end

  def get_shared_topics(project_id)
    get "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/kafka/topics?filter_by=shared:true"
  end

  def clean_topics(project_id)
    get_project_topics(project_id)
    json_body[:items].map{|topic| topic[:name]}.each{ |t| delete "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/kafka/topics" + t}
  end
end
