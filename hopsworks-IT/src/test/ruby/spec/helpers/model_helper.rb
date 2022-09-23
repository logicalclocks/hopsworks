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

  def create_model_job(project, job_name, notebook_file)

    # need to enable python for conversion .ipynb to .py works
    get "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/python/environments"
    if response.code == 404
       post "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/python/environments/#{ENV['PYTHON_VERSION']}?action=create"
       expect_status_details(201)
    end

    if !test_file("/Projects/#{project[:projectname]}/Resources/" + job_name + ".ipynb")
        copy_from_local("#{ENV['PROJECT_DIR']}/hopsworks-IT/src/test/ruby/spec/auxiliary/#{notebook_file}",
                        "/Projects/#{project[:projectname]}/Resources/" + job_name + ".ipynb", @user[:username],
                        "#{project[:projectname]}__Resources", 750, "#{project[:projectname]}")
    end

    job_config = get_spark_default_py_config(project, job_name, "ipynb")
    job_config["amMemory"] = 2500
    job_config["spark.executor.memory"] = 2000

    put "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/jobs/#{job_name}", job_config
    expect_status_details(201)
  end

  def create_model(project, model_project_name, registry_id, name, version)

    get_dataset_stat(project, "Models/" + name)
    if response.code == 400
      create_dir_checked(project, "Models/" + name)
    end
    create_dir_checked(project, "Models/" + name + "/" + version.to_s)

    json_data = {
        id: name + "_" + version.to_s,
        name: name,
        version: version,
        projectName: model_project_name
    }

    json_data = json_data.to_json

    create_model_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/modelregistries/" + registry_id.to_s + "/models/" + name + "_" + version.to_s
    json_result = put create_model_endpoint, json_data
    expect_status_details(201)

    wait_result = epipe_wait_on_provenance(repeat: 5)
    expect(wait_result["success"]).to be(true), wait_result["msg"]

    return json_result
  end

  def get_models(project_id, query)
    get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/modelregistries/#{project_id}/models#{query}"
  end

  def get_model(project_id, ml_id, query: "")
    get_model_request = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/modelregistries/#{project_id}/models/#{ml_id}/#{query}"
    pp get_model_request if defined?(@debugOpt) && @debugOpt
    get get_model_request
  end

  def delete_model(project_id, ml_id)
    delete_model_request = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/modelregistries/#{project_id}/models/#{ml_id}"
    pp delete_model_request if defined?(@debugOpt) && @debugOpt
    delete delete_model_request
  end

  def model_exists(project, name, version: 1)
    result = wait_for_me_time(10) do
      artifact_id = "#{name}_#{version}"
      get_model(project[:id], artifact_id)
      terminate = response.code == resolve_status(200, response.code) || (response.code == resolve_status(404, response.code) && json_body[:errorCode] == 360000)
      { 'success' => terminate, 'msg' => "wait for model in opensearch", 'resp_code' => response.code, 'error_code' => json_body[:errorCode]}
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
