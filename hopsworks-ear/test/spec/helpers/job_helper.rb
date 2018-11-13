=begin
Copyright (C) 2013 - 2018, Logical Clocks AB. All rights reserved

Permission is hereby granted, free of charge, to any person obtaining a copy of this
software and associated documentation files (the "Software"), to deal in the Software
without restriction, including without limitation the rights to use, copy, modify, merge,
publish, distribute, sublicense, and/or sell copies of the Software, and to permit
persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or
substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
=end
module JobHelper

  def create_sparktour_job(project, job_name)
    job_conf = {
        "type":"sparkJobConfiguration",
        "appName":"#{job_name}",
        "amQueue":"default",
        "amMemory":1024,
        "amVCores":1,
        "jobType":"SPARK",
        "appPath":"hdfs:///Projects/#{project[:projectname]}/TestJob/spark-examples.jar",
        "mainClass":"org.apache.spark.examples.SparkPi",
        "args":"10",
        "spark.executor.instances":1,
        "spark.executor.cores":1,
        "spark.executor.memory":1024,
        "spark.executor.gpus":0,
        "spark.dynamicAllocation.enabled":false,
        "spark.dynamicAllocation.minExecutors":1,
        "spark.dynamicAllocation.maxExecutors":10,
        "spark.dynamicAllocation.initialExecutors":1
    }

    post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jobs", job_conf
    #job_id = json_body[:id]
    #job = get_job_from_db(job_id)
    #expect(job[:id]).to eq job_id
    #expect(job[:name]).to eq job_conf[:appName]
    #job
  end

  def get_jobs(project_id)
    get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/jobs"
  end

  def get_jobs_with_type(project_id, type)
    get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/jobs?type=#{type}"
  end

  def get_job(project_id, job_name)
    get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/jobs/#{job_name}"
  end

  def delete_job(project_id, job_name)
    delete "#{ENV['HOPSWORKS_API']}/project/#{project_id}/jobs/#{job_name}"
  end

  def get_project_jobs(project_id)
    get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/jobs"
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

    if !json_body.empty?
      json_body[:items].map{|job| job[:name]}.each{|i| delete "#{ENV['HOPSWORKS_API']}/project/#{project_id}/jobs/#{i}"}
    end
  end
end
