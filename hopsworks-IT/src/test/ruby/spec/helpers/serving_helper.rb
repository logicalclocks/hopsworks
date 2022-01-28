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

TF_TOURS_LOCATION = "/user/hdfs/tensorflow_demo/data"
MNIST_TOUR_DATA_LOCATION = "#{TF_TOURS_LOCATION}/mnist"
TF_MODEL_TOUR_FILE_LOCATION = "#{MNIST_TOUR_DATA_LOCATION}/model/*"
SKLEARN_MODEL_TOUR_FILE_LOCATION = "#{TF_TOURS_LOCATION}/iris/iris_knn.pkl"
SKLEARN_SCRIPT_FILE_NAME="iris_flower_classifier.py"
SKLEARN_SCRIPT_TOUR_FILE_LOCATION = "/user/hdfs/tensorflow_demo/notebooks/end_to_end_pipeline/sklearn/#{SKLEARN_SCRIPT_FILE_NAME}"

module ServingHelper

  def with_tf_serving(project_id, project_name, user)
    copy_mnist_files(project_id, project_name, user)
    @serving ||= create_tf_serving(project_id, project_name)
    @topic = ProjectTopics.find(@serving[:kafka_topic_id])
  end

  def with_tensorflow_serving(project_id, project_name, user)
    copy_mnist_files(project_name, user)
    @serving ||= create_tensorflow_serving(project_id, project_name)
    @topic = ProjectTopics.find(@serving[:kafka_topic_id])
  end

  def with_sklearn_serving(project_id, project_name, user)
    copy_iris_files(project_name, user)
    @serving ||= create_sklearn_serving(project_id, project_name)
    @topic = ProjectTopics.find(@serving[:kafka_topic_id])
  end

  def create_tf_serving(project_id, project_name)
    serving_name = "testModel#{short_random_id}"
    put "#{ENV['HOPSWORKS_API']}/project/#{project_id}/serving/",
              {name: serving_name,
               modelPath: "/Projects/#{project_name}/Models/mnist",
               modelVersion: 1,
               batchingEnabled: true,
               kafkaTopicDTO: {
                  name: "CREATE",
                  numOfPartitions: 1,
                  numOfReplicas: 1
               },
               modelServer: "TENSORFLOW_SERVING",
               servingTool: "DEFAULT",
               requestedInstances: 1
              }
    expect_status_details(201)
    Serving.find_by(project_id: project_id, name: serving_name)
  end
  
  def create_sklearn_serving(project_id, project_name)
    serving_name = "testModel#{short_random_id}"
    put "#{ENV['HOPSWORKS_API']}/project/#{project_id}/serving/",
        {name: serving_name,
         modelPath: "/Projects/#{project_name}/Models/irisflowerclassifier",
         modelVersion: 1,
         predictor: "/Projects/#{project_name}/Models/irisflowerclassifier/1/#{SKLEARN_SCRIPT_FILE_NAME}",
         kafkaTopicDTO: {
             name: "CREATE",
             numOfPartitions: 1,
             numOfReplicas: 1
         },
         inferenceLogging: "ALL",
         modelServer: "PYTHON",
         servingTool: "DEFAULT",
         requestedInstances: 1
        }
    expect_status_details(201)
    Serving.find_by(project_id: project_id, name: serving_name)
  end

  def copy_mnist_files(project_name, user)
    mkdir("/Projects/#{project_name}/Models/mnist/", "#{project_name}__#{user}", "#{project_name}__Models", 750)
    copy(TF_MODEL_TOUR_FILE_LOCATION, "/Projects/#{project_name}/Models/mnist/", "#{user}", "#{project_name}__Models", 750, "#{project_name}")
  end

  def copy_iris_files(project_name, user)
    mkdir("/Projects/#{project_name}/Models/irisflowerclassifier/", "#{project_name}__#{user}", "#{project_name}__Models", 750)
    mkdir("/Projects/#{project_name}/Models/irisflowerclassifier/1", "#{project_name}__#{user}", "#{project_name}__Models", 750)
    copy(SKLEARN_MODEL_TOUR_FILE_LOCATION, "/Projects/#{project_name}/Models/irisflowerclassifier/1/", "#{user}", "#{project_name}__Models", 750, "#{project_name}")
    copy(SKLEARN_SCRIPT_TOUR_FILE_LOCATION, "/Projects/#{project_name}/Models/irisflowerclassifier/1/", "#{user}", "#{project_name}__Models", 750, "#{project_name}")
  end

  def purge_all_kfserving_instances(project_name="", should_exist=true)

    if !project_name.empty?
      # Purge all inferenceservices in a namespace
      namespace = project_name.gsub("_","-").downcase
      output = nil
      inf_services = nil

      if kubernetes_installed
        kube_user = Variables.find_by(id: "kube_user").value

        #Get all inference services
        cmd = "sudo su #{kube_user} /bin/bash -c \"kubectl get inferenceservices --namespace=#{namespace} -o=jsonpath='{.items[*].metadata.name}'\""
        Open3.popen3(cmd) do |_, stdout, _, wait_thr|
          inf_services = stdout.read.split("\n")
        end
        if should_exist
          expect(inf_services).not_to be_empty
        end

        #Remove all inference services
        cmd = "sudo su #{kube_user} /bin/bash -c \"kubectl delete --all inferenceservices --namespace=#{namespace}\""
        Open3.popen3(cmd) do |_, stdout, _, wait_thr|
          output = stdout.read
        end

        if should_exist
          output != "No resources in #{namespace} namespace." and output != ""
        else
          output == "No resources in #{namespace} namespace." or output == ""
        end
        if should_exist
          inf_services.each do |name|
            expect(output).to include(name)
          end
        else
          inf_services.each do |name|
            expect(output).not_to include(name)
          end
        end
      end
    else
      # Purge all inferenceservices
      cmd = "sudo su #{kube_user} /bin/bash -c \"kubectl delete --all inferenceservices --all-namespaces\""
      Open3.popen3(cmd) do |_, stdout, _, wait_thr|
        output = stdout.read
      end
    end
  end

  def purge_all_tf_serving_instances()
    if !kubernetes_installed
      system "sudo /bin/bash -c \"pgrep -f tensorflow_model_server | xargs kill\""
    end
  end

  def purge_all_sklearn_serving_instances()
    if !kubernetes_installed
      system "sudo /bin/bash -c \"pgrep -f sklearn_flask_server | xargs kill\""
    end
  end

  def delete_all_servings(project_id)
    serving_list = JSON.parse(get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/serving/")
    expect_status_details(200)
    serving_list.each do |serving|
      delete "#{ENV['HOPSWORKS_API']}/project/#{project_id}/serving/#{serving["id"]}"
      expect_status_details(200)
    end
  end

  def start_serving(project, serving)
    post "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/serving/#{serving[:id]}?action=start"
    expect_status(200)
  end

  def get_servings(project, query)
    get "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/serving#{query}"
  end

  def stop_serving(project, serving)
    post "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/serving/#{serving[:id]}?action=stop"
    expect_status(200)
  end

  def wait_for_type(serving_name)
    if !kubernetes_installed
      wait_for(60) do
        system "pgrep -f #{serving_name} -a"
        $?.exitstatus == 0
      end
      #Wait a bit more for the actual server to start in the container
      sleep(20)
    else
      sleep(30)
    end
  end

  def check_process_running(name)
    if !kubernetes_installed
      # check if the process is running on the host
      system "pgrep -f #{name}"
      if $?.exitstatus != 1
        raise "the process is still running"
      end
    else
      sleep(30)
    end
  end

  def get_kube_service_host(project, service)
    project.gsub! '_', '-'
    "#{service}.#{project}.logicalclocks.com"
  end

  def parse_model_server(value)
    return case
      when value == 0 ; "TENSORFLOW_SERVING"
      when value == 1 ; "PYTHON"
      else puts "Model server value cannot be parsed"
      end
  end

  def parse_serving_tool(value)
    return case
      when value == 0 ; "DEFAULT"
      else puts "Serving tool value cannot be parsed"
      end
  end

  def get_serving(serving_name)
    serving_list = get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/"
    expect_status(200)
    servings = JSON.parse(serving_list)
    servings.select { |serving| serving['name'] == serving_name}[0]
  end

end