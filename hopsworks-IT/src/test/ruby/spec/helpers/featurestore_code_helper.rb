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

  def create_code_commit(project_id, featurestore_id, entity_type, entity_id, kernel_id, action_type, application_id)
    post_statistics_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/#{entity_type}/#{entity_id}/code?kernelId=#{kernel_id}&type=#{action_type}"
    json_data = {
        commitTime: 1597903688000,
		applicationId: application_id
    }
    post post_statistics_endpoint, json_data.to_json
  end

  def get_all_code_commit(project_id, featurestore_id, entity_type, entity_id)
    get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/#{entity_type}/#{entity_id}/code?fields=content"
  end

  def get_code_commit(project_id, featurestore_id, entity_type, entity_id, code_id)
    get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/#{entity_type}/#{entity_id}/code/#{code_id}?fields=content"
  end
  
  def get_code_content()
    return JSON.parse("{\"cells\":[],\"metadata\":{},\"nbformat\":4,\"nbformat_minor\":5}")
  end
  
  def save_code(application_id, entity)
    settings = get_settings(@project)
		  
	json_result = post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jupyter/start", JSON(settings)
	expect_status_details(200)
	jupyter_project = JSON.parse(json_result)
	port = jupyter_project["port"]
	token = jupyter_project["token"]
	  
	bearer = ""
	Airborne.configure do |config|
	  bearer = config.headers["Authorization"]
	  config.headers["Authorization"] = "token #{token}"
	end
	  
	temp_name = create_notebook(port)

	update_notebook(port, get_code_content, temp_name)
	  
	#create a session for the notebook
	session_id, kernel_id = create_notebook_session(port, temp_name, temp_name)
	  
	Airborne.configure do |config|
	  config.headers["Authorization"] = bearer
	end
	
	featurestore_id = get_featurestore_id(@project.id)
	
	if entity == "featuregroups"
	   json_result = create_featuregroup_code(featurestore_id)
	else
	   json_result = create_training_code(featurestore_id)
	end
	
	parsed_json = JSON.parse(json_result)
	dataset_id = parsed_json["id"]
	json_result = create_code_commit(@project.id, featurestore_id, entity, dataset_id, kernel_id, "JUPYTER", application_id)
	expect_status(200)
	  
	stop_jupyter(@project)
	
	return JSON.parse(json_result), featurestore_id, dataset_id
  end
  
  def create_featuregroup_code(featurestore_id)
	json_result, _ = create_cached_featuregroup(@project.id, featurestore_id)
	expect_status(201)
	
	return json_result
  end
  
  def create_training_code(featurestore_id)
	connector = get_hopsfs_training_datasets_connector(@project[:projectname])

	features = [
	  {type: "int", name: "testfeature"},
	  {type: "int", name: "testfeature1"}
	]
	json_result, _ = create_hopsfs_training_dataset(@project[:id], featurestore_id, connector, features: features)
	expect_status(201)
	
	return json_result
  end
end