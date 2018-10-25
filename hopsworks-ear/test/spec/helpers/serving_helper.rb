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

INFERENCE_SCHEMA_NAME = "inferenceschema"
INFERENCE_SCHEMA_VERSION = 1

TOUR_FILE_LOCATION = "/user/hdfs/tensorflow_demo/data/mnist/model/*"

module ServingHelper

  def with_serving(project_id, project_name, user)
    # Copy model to the project directory
    mkdir("/Projects/#{project_name}/Models/mnist/", user, "#{project_name}__Models", 750)
    copy(TOUR_FILE_LOCATION, "/Projects/#{project_name}/Models/mnist/", user, "#{project_name}__Models", 750)

    @serving ||= create_serving(project_id, project_name)
    @topic = ProjectTopics.find(@serving[:kafka_topic_id])
  end

  def create_serving(project_id, project_name)
    serving_name = "testModel#{short_random_id}"
    put "#{ENV['HOPSWORKS_API']}/project/#{project_id}/serving/",
              {modelName: serving_name,
               modelPath: "/Projects/#{project_name}/Models/mnist/",
               modelVersion: 1,
               batchingEnabled: true,
               kafkaTopicDTO: {
                  name: "CREATE",
                  numOfPartitions: 1,
                  numOfReplicas: 1
               }}
    expect_status(201)

    TfServing.find_by(project_id: project_id, model_name: serving_name)
  end

  def purge_all_serving_instances()
    system "sudo /bin/bash -c \"pgrep -f tensorflow_model_server | xargs kill\""
  end
end