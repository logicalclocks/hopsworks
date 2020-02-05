=begin
 This file is part of Hopsworks
 Copyright (C) 2019, Logical Clocks AB. All rights reserved

 Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 the GNU Affero General Public License as published by the Free Software Foundation,
 either version 3 of the License, or (at your option) any later version.

 Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 PURPOSE.  See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along with this program.
 If not, see <https://www.gnu.org/licenses/>.
=end
module ExperimentHelper

  def create_experiment_job(project, job_name)

    # need to enable python for conversion .ipynb to .py works
    get "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/python/environments"
    if response.code == 404
       post "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/python/environments/3.6?action=create"
       expect_status(201)
    end

    if !test_file("/Projects/#{project[:projectname]}/Resources/" + job_name + ".ipynb")
        copy_from_local("#{ENV['PROJECT_DIR']}/hopsworks-IT/src/test/ruby/spec/aux/run_experiment.ipynb",
                        "/Projects/#{project[:projectname]}/Resources/" + job_name + ".ipynb", @user[:username],
                        "#{@project[:projectname]}__Resources", 750, "#{@project[:projectname]}")
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
      "spark.executor.instances":1,
      "spark.executor.cores":1,
      "spark.executor.memory":1500,
      "spark.executor.gpus":0,
      "spark.dynamicAllocation.enabled": true,
      "spark.dynamicAllocation.minExecutors":1,
      "spark.dynamicAllocation.maxExecutors":1,
      "spark.dynamicAllocation.initialExecutors":1
    }

    put "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/jobs/#{job_name}", job_conf
    expect_status(201)
  end

  def create_experiment_opt_job(project, job_name)

    # need to enable python for conversion .ipynb to .py works
    get "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/python/environments"
    if response.code == 404
       post "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/python/environments/3.6?action=create"
       expect_status(201)
    end

    if !test_file("/Projects/#{project[:projectname]}/Resources/" + job_name + ".ipynb")
        copy_from_local("#{ENV['PROJECT_DIR']}/hopsworks-IT/src/test/ruby/spec/aux/opt_experiment.ipynb",
                        "/Projects/#{project[:projectname]}/Resources/" + job_name + ".ipynb", @user[:username],
                        "#{@project[:projectname]}__Resources", 750, "#{@project[:projectname]}")
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
      "spark.executor.instances":1,
      "spark.executor.cores":1,
      "spark.executor.memory":1500,
      "spark.executor.gpus":0,
      "spark.dynamicAllocation.enabled": true,
      "spark.dynamicAllocation.minExecutors":1,
      "spark.dynamicAllocation.maxExecutors":1,
      "spark.dynamicAllocation.initialExecutors":1
    }

    put "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/jobs/#{job_name}", job_conf
    expect_status(201)
  end

  def get_results(project_id, ml_id, query)
    get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/experiments/#{ml_id}/#{query}"
  end

  def run_experiment_blocking(project, job_name)
    start_execution(project[:id], job_name, nil)
    execution_id = json_body[:id]
    expect_status(201)
    wait_for_execution_completed(project[:id], job_name, execution_id, "FINISHED")
    wait_for_project_prov(project)
  end

  def get_experiments(project_id, query)
    get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/experiments#{query}"
  end

  def get_experiment(project_id, ml_id, query)
    get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/experiments/#{ml_id}/#{query}"
  end

  def delete_experiment(project_id, ml_id)
    delete "#{ENV['HOPSWORKS_API']}/project/#{project_id}/experiments/#{ml_id}"
  end

  def create_tensorboard(project_id, ml_id)
    post "#{ENV['HOPSWORKS_API']}/project/#{project_id}/experiments/#{ml_id}/tensorboard"
  end

  def get_tensorboard(project_id, ml_id)
    get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/experiments/#{ml_id}/tensorboard"
  end

  def delete_tensorboard(project_id, ml_id)
    delete "#{ENV['HOPSWORKS_API']}/project/#{project_id}/experiments/#{ml_id}/tensorboard"
  end
end
