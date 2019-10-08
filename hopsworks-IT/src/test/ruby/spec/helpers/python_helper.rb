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

module PythonHelper

  def with_python_enabled(project_id, python_version)
    get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/python/environments"
    if response.code == 200
      version = json_body[:items][0][:pythonVersion]
      if version != python_version
        delete_conda_env(project_id)
        post "#{ENV['HOPSWORKS_API']}/project/#{project_id}/python/environments/#{python_version}?action=create"
        expect_status(201)
      end
    else
      post "#{ENV['HOPSWORKS_API']}/project/#{project_id}/python/environments/#{python_version}?action=create"
      expect_status(201)
    end
  end

  def delete_conda_env(project_id)
    get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/python/environments"
    if response.code == 200
      version = json_body[:items][0][:pythonVersion]
      delete "#{ENV['HOPSWORKS_API']}/project/#{project_id}/python/environments/#{version}"
      expect_status(204)
    end
  end

  def get_python_it_tests_job_name(version)
    return "python_it_tests_job_" + version
  end

  def get_python_it_tests_hdfs_dir
    return "/user/it_tests/"
  end

  def get_python_it_tests_notebook_name
    return "integration_tests.ipynb"
  end

  def get_python_it_tests_artifacts
    return [
        get_python_it_tests_notebook_name,
        "attendances_features.csv",
        "games_features.csv",
        "players_features.csv",
        "season_scores_features.csv",
        "teams_features.csv"
    ]
  end

  def get_python_it_tests_project_dir(project_name)
    return "hdfs:///Projects/#{project_name}/Resources/"
  end

  def copy_python_it_tests_artifacts_to_project(project, user)
    test_artifacts = get_python_it_tests_artifacts
    test_artifacts.each do |file_name|
      if !test_file(get_python_it_tests_project_dir(project.projectname) + file_name)
        copy(get_python_it_tests_hdfs_dir + file_name,
             get_python_it_tests_project_dir(project.projectname) + file_name, user.username,
             "#{project.projectname}__Resources",
             750,
             "#{project.projectname}")
      end
    end
  end

  def with_it_test_artifacts(project, user)
    copy_python_it_tests_artifacts_to_project(project, user)
  end

  def with_it_test_job(project, job_name, hdfs_path, job_conf)
    create_python_notebook_job(project, job_name, hdfs_path, job_conf)
  end

  def create_python_notebook_job(project, job_name, hdfs_path, job_conf)
    if job_conf.nil?
      job_conf = {
          "type": "sparkJobConfiguration",
          "appName": "#{job_name}",
          "amQueue": "default",
          "amMemory": 3000,
          "amVCores": 1,
          "jobType": "PYSPARK",
          "appPath": "#{hdfs_path}",
          "mainClass": "org.apache.spark.deploy.PythonRunner",
          "args": "",
          "spark.executor.instances": 1,
          "spark.executor.cores": 1,
          "spark.executor.memory": 3000,
          "spark.executor.gpus": 0,
          "spark.dynamicAllocation.enabled": true,
          "spark.dynamicAllocation.minExecutors": 1,
          "spark.dynamicAllocation.maxExecutors": 2,
          "spark.dynamicAllocation.initialExecutors": 1
      }
    end
    put "#{ENV['HOPSWORKS_API']}/project/#{project.id}/jobs/#{job_name}", job_conf
    expect_status(201)
  end

end
