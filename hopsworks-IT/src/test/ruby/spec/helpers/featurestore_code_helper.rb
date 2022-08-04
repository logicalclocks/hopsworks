# This file is part of Hopsworks
# Copyright (C) 2021, Logical Clocks AB. All rights reserved
#
# Hopsworks is free software: you can redistribute it and/or modify it under the terms of
# the GNU Affero General Public License as published by the Free Software Foundation,
# either version 3 of the License, or (at your option) any later version.
#
# Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
# without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
# PURPOSE.  See the GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License along with this program.
# If not, see <https://www.gnu.org/licenses/>.
#

module FeatureStoreCodeHelper

  def save_code(featurestore_id, dataset_type, dataset_id, entity_id, type, application_id)
	post_statistics_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/#{dataset_type}/#{dataset_id}/code?entityId=#{entity_id}&type=#{type}"
	json_data = {
        commitTime: 1597903688000,
		applicationId: application_id
    }
    json_result = post post_statistics_endpoint, json_data.to_json
	expect_status_details(200)
	
	return JSON.parse(json_result)
  end

  def get_all_code(project_id, featurestore_id, dataset_type, dataset_id)
    json_result = get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/#{dataset_type}/#{dataset_id}/code?fields=content"
		expect_status_details(200)
	
	return JSON.parse(json_result)
  end

  def get_code(project_id, featurestore_id, dataset_type, dataset_id, code_id)
    json_result = get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/#{dataset_type}/#{dataset_id}/code/#{code_id}?fields=content"
		expect_status_details(200)
	
	return JSON.parse(json_result)
  end
  
  def get_code_content()
    return JSON.parse("{\"cells\":[],\"metadata\":{},\"nbformat\":4,\"nbformat_minor\":5}")
  end

  def save_notebook(application_id, dataset_type)
    settings = get_settings(@project)

		#start notebook
		post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jupyter/start", JSON(settings)
		expect_status_details(200)
		port = json_body[:port]
		token = json_body[:token]

		#save header
		bearer = ""
		Airborne.configure do |config|
		  bearer = config.headers["Authorization"]
		  config.headers["Authorization"] = "token #{token}"
		end

		#add code content
		temp_name = create_notebook(port)
		update_notebook(port, get_code_content, temp_name)

		#create a session for the notebook
		_, kernel_id = create_notebook_session(port, temp_name, temp_name)

		#reset header
		Airborne.configure do |config|
		  config.headers["Authorization"] = bearer
		end

		#create featuregroup/training dataset
		featurestore_id = get_featurestore_id(@project.id)
		parsed_json = create_dataset(featurestore_id, dataset_type)

		#save code
		dataset_id = parsed_json["id"]
		parsed_json = save_code(featurestore_id, dataset_type, dataset_id, kernel_id, "JUPYTER", application_id)

		stop_jupyter(@project)

		[parsed_json, featurestore_id, dataset_id]
  end
  
  def save_job(application_id, dataset_type)
		#update tour project by adding services
		new_project = {description:"", status: 0, services: ["JOBS","JUPYTER","HIVE","KAFKA","SERVING", "FEATURESTORE"],
			   projectTeam:[], retentionPeriod: ""}
		put "#{ENV['HOPSWORKS_API']}/project/#{@project.id}", new_project

		#create featuregroup/training dataset
		featurestore_id = get_featurestore_id(@project.id)
		parsed_json = create_dataset(featurestore_id, dataset_type)

		#create job
		job_spark_1 = "demo_job_1"
		create_sparktour_job(@project, job_spark_1, "jar")

		#save code
		dataset_id = parsed_json["id"]
		return save_code(featurestore_id, dataset_type, dataset_id, job_spark_1, "JOB", application_id), featurestore_id, dataset_id
  end
  
  def create_dataset(featurestore_id, dataset_type)
		if dataset_type == "featuregroups"
		  json_result, _ = create_cached_featuregroup(@project.id, featurestore_id)
			expect_status_details(201)
		else
		  connector = get_hopsfs_training_datasets_connector(@project[:projectname])

		  features = [
			{type: "int", name: "testfeature"},
			{type: "int", name: "testfeature1"}
		  ]
		  json_result, _ = create_hopsfs_training_dataset(@project[:id], featurestore_id, connector, features: features)
			expect_status_details(201)
		end

		return JSON.parse(json_result)
  end
end