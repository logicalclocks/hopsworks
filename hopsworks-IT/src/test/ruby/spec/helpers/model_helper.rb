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
module ModelHelper

  def create_model_job(project, job_name)

    # need to enable python for conversion .ipynb to .py works
    get "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/python/environments"
    if response.code == 404
       post "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/python/environments/3.7?action=create"
       expect_status(201)
    end

    if !test_file("/Projects/#{project[:projectname]}/Resources/" + job_name + ".ipynb")
        copy_from_local("#{ENV['PROJECT_DIR']}/hopsworks-IT/src/test/ruby/spec/auxiliary/export_model.ipynb",
                        "/Projects/#{project[:projectname]}/Resources/" + job_name + ".ipynb", @user[:username],
                        "#{project[:projectname]}__Resources", 750, "#{project[:projectname]}")
    end

    job_config = get_spark_default_py_config(project, job_name, "ipynb")
    job_config["amMemory"] = 2500
    job_config["spark.executor.memory"] = 2000

    put "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/jobs/#{job_name}", job_config
    expect_status(201)
  end

  def get_models(project_id, query)
    get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/models#{query}"
  end

  def get_model(project_id, ml_id, query: "")
    get_model_request = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/models/#{ml_id}/#{query}"
    pp get_model_request if defined?(@debugOpt) && @debugOpt
    get get_model_request
  end

  def model_exists(project, name, version: 1)
    result = wait_for_me_time(10) do
      artifact_id = "#{name}_#{version}"
      get_model(project[:id], artifact_id)
      terminate = response.code == resolve_status(200, response.code) || (response.code == resolve_status(404, response.code) && json_body[:errorCode] == 360000)
      { 'success' => terminate, 'msg' => "wait for model in elastic", 'resp_code' => response.code, 'error_code' => json_body[:errorCode]}
    end
    if result['resp_code'] == resolve_status(200, result['resp_code'])
      true
    elsif result['resp_code'] == resolve_status(404, result['resp_code'])  && result['error_code'] == 360000
      false
    else
      expect_status_details(200)
    end
  end
end
