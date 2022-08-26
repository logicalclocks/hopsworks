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

  def create_flink_job(project, job_name, properties)
    job_conf = get_flink_conf(job_name)
    if properties.nil?
      job_conf[:properties] = properties
    end

    job_conf[:appName] = job_name
    put "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/jobs/#{job_name}", job_conf
    expect_status_details(201)
  end

  def get_spark_default_py_config(project, job_name, type)
    job_config = {
      "type": "sparkJobConfiguration",
      "appName": "#{job_name}",
      "amQueue": "default",
      "amMemory": 1024,
      "amVCores": 1,
      "jobType": "PYSPARK",
      "appPath": "hdfs:///Projects/#{project[:projectname]}/Resources/" + job_name + ".#{type}",
      "mainClass": "org.apache.spark.deploy.PythonRunner",
      "defaultArgs": "10",
      "spark.executor.instances": 1,
      "spark.executor.cores": 1,
      "spark.executor.memory": 1500,
      "spark.executor.gpus": 0,
      "spark.dynamicAllocation.enabled": true,
      "spark.dynamicAllocation.minExecutors": 1,
      "spark.dynamicAllocation.maxExecutors": 1,
      "spark.dynamicAllocation.initialExecutors": 1
    }
    job_config
  end

  def get_python_default_config(project, job_name, file_type)
    job_conf = {
        "type" => "pythonJobConfiguration",
        "appName" => "#{job_name}",
        "resourceConfig" => {
            "memory" => 1024,
            "cores" => 1,
            "gpus" => 0
        },
        "jobType" => "PYTHON",
        "appPath" => "hdfs:///Projects/#{project[:projectname]}/Resources/#{job_name}.#{file_type}",
    }
    job_conf
  end

  def get_flink_conf(job_name)
    job_conf = {
        "type":"flinkJobConfiguration",
        "jobType": "FLINK",
        "amQueue":"default",
        "jobmanager.heap.size":1024,
        "amVCores":1,
        "jobManagerMemory":1024,
        "taskmanager.heap.size":1024,
        "taskmanager.numberOfTaskSlots":1,
        "appName":"#{job_name}",
        "localResources":[]}

  end

  def get_existing_active_executions()
    active_executions = 0
    logged_user = @user
    projects = get_testing_projects
    projects.each do |project|
      response, _ = login_user(project[:username], "Pass123")
      if response.code == 200
        create_session(project[:username], "Pass123")
        pp "#{project[:projectname]} id:#{project[:id]}" if defined?(@debugOpt) && @debugOpt
        get_jobs(project[:id])
        if json_body.key?(:count) && json_body[:count] > 0
          for job in json_body[:items]
            get_executions(project[:id], job[:name])
            if json_body.key?(:count) && json_body[:count] > 0
              for execution in json_body[:items]
                if is_execution_active(execution)
                  active_executions = active_executions + 1
                end
              end
            end
          end
        end
      end
    end
    create_session(logged_user[:email], "Pass123")
    active_executions
  end

  def create_job(project, job_name, job_conf, expected_status: 201, error_code: nil)
    put "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/jobs/#{job_name}", job_conf
    expect_status_details(expected_status, error_code: error_code)
  end

  def create_sparktour_job(project, job_name, type, job_conf: nil, expected_status: 201, error_code: nil)
    # need to enable python for conversion .ipynb to .py works
    get "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/python/environments"
    if response.code == 404
       post "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/python/environments/#{ENV['PYTHON_VERSION']}?action=create"
       expect_status_details(201)
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
            "defaultArgs": "10",
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
    elsif type.eql? "py"
      if !test_file("/Projects/#{project[:projectname]}/Resources/#{job_name}.py")
        if !test_file("/Projects/#{project[:projectname]}/Resources/" + job_name + ".ipynb")
          copy_from_local("#{ENV['PROJECT_DIR']}/hopsworks-IT/src/test/ruby/spec/auxiliary/run_single_experiment.ipynb",
                "/Projects/#{project[:projectname]}/Resources/" + job_name + ".ipynb", @user[:username], "#{project[:projectname]}__Resources", 750, "#{project[:projectname]}")
          get "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/jupyter/convertIPythonNotebook/Resources/" + job_name + ".ipynb"
          expect_status_details(200)
        end
      end
      if job_conf.nil?
        job_conf = get_spark_default_py_config(project, job_name, "py")
      end
    else
      if !test_file("/Projects/#{project[:projectname]}/Resources/" + job_name + ".ipynb")
        copy_from_local("#{ENV['PROJECT_DIR']}/hopsworks-IT/src/test/ruby/spec/auxiliary/run_single_experiment.ipynb",
        "/Projects/#{project[:projectname]}/Resources/" + job_name + ".ipynb", @user[:username], "#{project[:projectname]}__Resources", 750, "#{project[:projectname]}")
      end
      if job_conf.nil?
        job_conf = get_spark_default_py_config(project, job_name, "ipynb")
      end
    end
    create_job(project, job_name, job_conf, expected_status: expected_status, error_code: error_code)
  end

  def create_python_job(project, job_name, type, job_conf=nil)
    # need to enable python for conversion .ipynb to .py works
    get "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/python/environments"
    if response.code == 404
      post "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/python/environments/#{ENV['PYTHON_VERSION']}?action=create"
      expect_status_details(201)
    end

    if job_conf.nil?
      job_conf = get_python_default_config(project, job_name, type)
      job_conf["defaultArgs"] = "10"
      job_conf["files"] = "hdfs:///Projects/#{project[:projectname]}/Resources/README.md,hdfs:///Projects/#{project[:projectname]}/TestJob/spark-examples.jar"
    end

    if type.eql? "py"
      if !test_file("/Projects/#{project[:projectname]}/Resources/" + job_name + ".py")
        copy_from_local("#{ENV['PROJECT_DIR']}/hopsworks-IT/src/test/ruby/spec/auxiliary/test_job.py",
                        "/Projects/#{project[:projectname]}/Resources/" + job_name + ".py", @user[:username],
                        "#{@project[:projectname]}__Resources", 750, "#{@project[:projectname]}")
      end
      put "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/jobs/#{job_name}", job_conf

    elsif type.eql? "ipynb"
      if !test_file("/Projects/#{project[:projectname]}/Resources/" + job_name + ".ipynb")
        copy_from_local("#{ENV['PROJECT_DIR']}/hopsworks-IT/src/test/ruby/spec/auxiliary/test_job.ipynb",
                        "/Projects/#{project[:projectname]}/Resources/" + job_name + ".ipynb", @user[:username],
                        "#{@project[:projectname]}__Resources", 750, "#{@project[:projectname]}")
      end

      get "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/jupyter/convertIPythonNotebook/Resources/" + job_name + ".ipynb"
      expect_status_details(200)
      job_conf[:appPath] = "/Projects/#{project[:projectname]}/Resources/" + job_name + ".ipynb"
      put "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/jobs/#{job_name}", job_conf

    end
  end

  def create_docker_job(project, job_name, job_conf=nil)

    if job_conf.nil?
      job_conf = {
        "type" => "dockerJobConfiguration",
        "appName" => "#{job_name}",
        "resourceConfig" => {
          "memory" => 1024,
          "cores" => 1,
          "gpus" => 0
        },
        "jobType" => "DOCKER",
        "imagePath" => "alpine",
        "volumes" => ["/tmp:/tmp2","/var:/var2"],
        "envVars" => ["ENV1=val1","ENV2=val2"],
        "command" => ["/bin/sh", "-c", 'sleep 30 && cp /Projects/' + project[:projectname] + '/DataValidation/README.md /Projects/' + project[:projectname] + '/Resources/README_DV.md ' +
                                       '&& cp /Projects/' + project[:projectname] + '/Jupyter/README.md /Projects/' + project[:projectname] + '/Resources/README_Jupyter.md && echo hello'],
        "inputPaths" => ["/Projects/#{project[:projectname]}/DataValidation", "/Projects/#{project[:projectname]}/Jupyter"],
        "outputPath" => "/Projects/#{project[:projectname]}/Resources",
        "uid" => "1",
        "gid" => "1"
      }
      put "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/jobs/#{job_name}", job_conf
    end
  end

  def get_jobs(project_id, query: nil, expected_status: 200, error_code: nil)
    get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/jobs#{query}"
    expect_status_details(expected_status, error_code: error_code)
  end

  def get_job(project_id, job_name, query: "", expected_status: nil, error_code: nil)
    get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/jobs/#{job_name}/#{query}"
    expect_status_details(expected_status, error_code: error_code) unless expected_status.nil?
  end

  def delete_job(project_id, job_name, expected_status: 204)
    delete "#{ENV['HOPSWORKS_API']}/project/#{project_id}/jobs/#{job_name}"
    expect_status_details(expected_status)
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

  def job_does_not_exist()
    response.code == resolve_status(404, response.code) && json_body[:errorCode] == 130009
  end

  def job_exists(project_id, job_name, query: "")
    get_job(project_id, job_name, query: query)
    if response.code == resolve_status(200, response.code)
      true
    elsif response.code == resolve_status(404, response.code) && json_body[:errorCode] == 130009
      false
    else
      expect_status_details(200)
    end
  end

  def prepare_spark_job(project, username, job_name, job_type, job_config: nil, src_dir: "#{ENV['PROJECT_DIR']}/hopsworks-IT/src/test/ruby/spec/auxiliary")
    src = "#{src_dir}/#{job_name}.#{job_type}"
    dst = "/Projects/#{project[:projectname]}/Resources/#{job_name}.#{job_type}"
    group = "#{project[:projectname]}__Jupyter"
    copy_from_local(src, dst, username, group, 750, "#{project[:projectname]}")
    if job_config.nil?
      job_config = get_spark_default_py_config(project, job_name, job_type)
      job_config["amMemory"] = 2048
      job_config["spark.executor.memory"] = 4096
      job_config["defaultArgs"] = nil
    end
    create_sparktour_job(project, job_name, job_type, job_conf: job_config)
    expect_status_details(201)
  end

  def prepare_job(project, username, job_name, job_type, file_type, job_config: nil, src_dir: "#{ENV['PROJECT_DIR']}/hopsworks-IT/src/test/ruby/spec/auxiliary")
    chmod_local_dir("#{ENV['PROJECT_DIR']}", 777, true)
    src = "#{src_dir}/#{job_name}.#{file_type}"
    dst = "/Projects/#{project[:projectname]}/Resources/#{job_name}.#{file_type}"
    group = "#{project[:projectname]}__Jupyter"
    copy_from_local(src, dst, username, group, 750, "#{project[:projectname]}")
    if job_type == "PYTHON"
      create_python_job(project, job_name, file_type, job_conf = job_config)
    else
      create_sparktour_job(project, job_name, file_type, job_conf: job_config)
    end
    expect_status_details(201)
  end

  def get_default_job_configuration(project_id, job_type)
    get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/jobs/#{job_type}/configuration"
  end

  def create_dummy_job(project, job_name, job_conf)
    put "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/jobs/#{job_name}", job_conf
    expect_status_details(201)
    json_body
  end
end
