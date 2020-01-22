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
INFERENCE_SCHEMA_VERSION = 2

TF_MODEL_TOUR_FILE_LOCATION = "/user/hdfs/tensorflow_demo/data/mnist/model/*"
SKLEARN_MODEL_TOUR_FILE_LOCATION = "/user/hdfs/tensorflow_demo/data/iris/iris_knn.pkl"
SKLEARN_SCRIPT_FILE_NAME="iris_flower_classifier.py"
SKLEARN_SCRIPT_TOUR_FILE_LOCATION = "/user/hdfs/tensorflow_demo/notebooks/End_To_End_Pipeline/sklearn/#{SKLEARN_SCRIPT_FILE_NAME}"

module ServingHelper

  def with_tf_serving(project_id, project_name, user)
    # Copy model to the project directory
    mkdir("/Projects/#{project_name}/Models/mnist/", "#{project_name}__#{user}", "#{project_name}__Models", 750)
    copy(TF_MODEL_TOUR_FILE_LOCATION, "/Projects/#{project_name}/Models/mnist/", "#{project_name}__#{user}", "#{project_name}__Models", 750, "#{project_name}")

    @serving ||= create_tf_serving(project_id, project_name)
    @topic = ProjectTopics.find(@serving[:kafka_topic_id])
  end

  def with_sklearn_serving(project_id, project_name, user)
    # Make Serving Dir
    mkdir("/Projects/#{project_name}/Models/IrisFlowerClassifier/", "#{project_name}__#{user}",
          "#{project_name}__Models", 750)
    # Make Version Dir
    mkdir("/Projects/#{project_name}/Models/IrisFlowerClassifier/1", "#{project_name}__#{user}",
          "#{project_name}__Models", 750)
    # Copy model to the servingversion dir
    copy(SKLEARN_MODEL_TOUR_FILE_LOCATION, "/Projects/#{project_name}/Models/IrisFlowerClassifier/1/",
         "#{project_name}__#{user}",
         "#{project_name}__Models", 750, "#{project_name}")
    # Copy script to the servingversion dir
    copy(SKLEARN_SCRIPT_TOUR_FILE_LOCATION, "/Projects/#{project_name}/Models/IrisFlowerClassifier/1/",
         "#{project_name}__#{user}",
         "#{project_name}__Models", 750, "#{project_name}")

    @serving ||= create_sklearn_serving(project_id, project_name)
    @topic = ProjectTopics.find(@serving[:kafka_topic_id])
  end

  def create_tf_serving(project_id, project_name)
    serving_name = "testModel#{short_random_id}"
    put "#{ENV['HOPSWORKS_API']}/project/#{project_id}/serving/",
              {name: serving_name,
               artifactPath: "/Projects/#{project_name}/Models/mnist/",
               modelVersion: 1,
               batchingEnabled: true,
               kafkaTopicDTO: {
                  name: "CREATE",
                  numOfPartitions: 1,
                  numOfReplicas: 1
               },
               servingType: "TENSORFLOW"
              }
    expect_status(201)

    Serving.find_by(project_id: project_id, name: serving_name)
  end

  def purge_all_tf_serving_instances()
    system "sudo /bin/bash -c \"pgrep -f tensorflow_model_server | xargs kill\""
  end

  def create_sklearn_serving(project_id, project_name)
    serving_name = "testModel#{short_random_id}"
    put "#{ENV['HOPSWORKS_API']}/project/#{project_id}/serving/",
        {name: serving_name,
         artifactPath: "/Projects/#{project_name}/Models/IrisFlowerClassifier/1/#{SKLEARN_SCRIPT_FILE_NAME}",
         modelVersion: 1,
         kafkaTopicDTO: {
             name: "CREATE",
             numOfPartitions: 1,
             numOfReplicas: 1
         },
         servingType: "SKLEARN"
        }
    expect_status(201)

    Serving.find_by(project_id: project_id, name: serving_name)
  end

  def purge_all_sklearn_serving_instances()
    system "sudo /bin/bash -c \"pgrep -f sklearn_flask_server | xargs kill\""
  end

  def delete_all_sklearn_serving_instances(project)
    serving_list = JSON.parse(get "#{ENV['HOPSWORKS_API']}/project/#{project.id}/serving/")
    expect_status(200)
    serving_list.each do |serving|
      delete "#{ENV['HOPSWORKS_API']}/project/#{project.id}/serving/#{serving["id"]}"
      expect_status(200)
    end
  end

  def start_serving(project, serving)
    post "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/serving/#{serving[:id]}?action=start"
    expect_status(200)

    # Sleep some time while the TfServing server starts
    wait_for do
      system "pgrep -f #{serving[:name]} -a"
      $?.exitstatus == 0
    end
  end
end