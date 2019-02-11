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
module JobHelper

  def create_sparktour_job(project, job_name, type, job_conf)

    # need to enable python for conversion .ipynb to .py works
    get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps/enabled"
    if response.code == 503
        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps/enable/2.7/true"
        expect_status(200)
    end

    if type.eql? "jar"
      if job_conf.nil?
        job_conf = {
            "type": "sparkJobConfiguration",
            "appName": "#{job_name}",
            "amQueue": "default",
            "amMemory": 1024,
            "amVCores": 1,
            "jobType": "SPARK",
            "appPath": "hdfs:///Projects/#{project[:projectname]}/TestJob/spark-examples.jar",
            "mainClass": "org.apache.spark.examples.SparkPi",
            "args": "10",
            "spark.executor.instances": 1,
            "spark.executor.cores": 1,
            "spark.executor.memory": 1024,
            "spark.executor.gpus": 0,
            "spark.dynamicAllocation.enabled": false,
            "spark.dynamicAllocation.minExecutors": 1,
            "spark.dynamicAllocation.maxExecutors": 10,
            "spark.dynamicAllocation.initialExecutors": 1
        }
      end

      put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jobs/#{job_name}", job_conf

    elsif type.eql? "py"

      if !test_file("/Projects/#{@project[:projectname]}/Resources/" + job_name + ".ipynb")
        copy("/user/hdfs/tensorflow_demo/notebooks/Experiment/TensorFlow/minimal_mnist_classifier_on_hops.ipynb",
              "/Projects/#{@project[:projectname]}/Resources/" + job_name + ".ipynb", @user[:username], "#{@project[:projectname]}__Resources", 750, "#{@project[:projectname]}")
      end

      get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jupyter/convertIPythonNotebook/Resources/" + job_name + ".ipynb"
      expect_status(200)
      if job_conf.nil?
        job_conf = {
            "type": "sparkJobConfiguration",
            "appName": "#{job_name}",
            "amQueue": "default",
            "amMemory": 1024,
            "amVCores": 1,
            "jobType": "PYSPARK",
            "appPath": "hdfs:///Projects/#{project[:projectname]}/Resources/" + job_name + ".py",
            "mainClass": "org.apache.spark.deploy.PythonRunner",
            "args": "10",
            "spark.executor.instances": 1,
            "spark.executor.cores": 1,
            "spark.executor.memory": 1500,
            "spark.executor.gpus": 0,
            "spark.dynamicAllocation.enabled": false,
            "spark.dynamicAllocation.minExecutors": 1,
            "spark.dynamicAllocation.maxExecutors": 10,
            "spark.dynamicAllocation.initialExecutors": 1
        }
      end

      put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jobs/#{job_name}", job_conf
      expect_status(201)

    else
        if !test_file("/Projects/#{@project[:projectname]}/Resources/" + job_name + ".ipynb")
          copy("/user/hdfs/tensorflow_demo/notebooks/Experiment/TensorFlow/minimal_mnist_classifier_on_hops.ipynb",
          "/Projects/#{@project[:projectname]}/Resources/" + job_name + ".ipynb", @user[:username], "#{@project[:projectname]}__Resources", 750, "#{@project[:projectname]}")
        end

        job_conf = {
          "type":"sparkJobConfiguration",
          "appName":"#{job_name}",
          "amQueue":"default",
          "amMemory":1024,
          "amVCores":1,
          "jobType":"PYSPARK",
          "appPath":"hdfs:///Projects/#{project[:projectname]}/Resources/" + job_name + ".ipynb",
          "mainClass":"org.apache.spark.deploy.PythonRunner",
          "args":"10",
          "spark.executor.instances":1,
          "spark.executor.cores":1,
          "spark.executor.memory":1500,
          "spark.executor.gpus":0,
          "spark.dynamicAllocation.enabled":false,
          "spark.dynamicAllocation.minExecutors":1,
          "spark.dynamicAllocation.maxExecutors":10,
          "spark.dynamicAllocation.initialExecutors":1
        }

        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jobs/#{job_name}", job_conf
        expect_status(201)
    end
  end


  def get_jobs(project_id, query)
    get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/jobs#{query}"
  end

  def get_job(project_id, job_name, query)
    get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/jobs/#{job_name}/#{query}"
  end

  def delete_job(project_id, job_name)
    delete "#{ENV['HOPSWORKS_API']}/project/#{project_id}/jobs/#{job_name}"
  end

  def get_job_from_db(job_id)
    # SELECT all columns expect for 'type', this is needed since type is a reserved variable name in ruby used in inheritance.
    # ActiveRecord::SubclassNotFound Exception: The single-table inheritance mechanism failed to locate the subclass: 'SPARK'. This error is raised because the column 'type' is reserved for storing the class in case of inheritance. Please rename t$
    Job.where(["id = ?", job_id]).select("id, name, creation_time, project_id, creator, json_config").first
  end

  def count_jobs(proj_id)
    Job.where(["project_id = ?", proj_id]).count
  end

  def clean_jobs(project_id)
    with_valid_session
    get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/jobs"
    if !json_body.empty? && json_body[:items].present?
      json_body[:items].map{|job| job[:name]}.each{|i| delete "#{ENV['HOPSWORKS_API']}/project/#{project_id}/jobs/#{i}"}
    end
  end
end
