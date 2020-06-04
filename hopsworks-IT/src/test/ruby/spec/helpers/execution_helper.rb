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
module ExecutionHelper

  def get_executions(project_id, job_name, query)
    get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/jobs/#{job_name}/executions#{query}"
  end

  def get_execution(project_id, job_name, execution_id)
    get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/jobs/#{job_name}/executions/#{execution_id}"
  end

  def start_execution(project_id, job_name, args=nil)
      headers = { 'Content-Type' => 'text/plain' }
      post "#{ENV['HOPSWORKS_API']}/project/#{project_id}/jobs/#{job_name}/executions", args, headers
    end

  def stop_execution(project_id, job_name, execution_id)
    stateDTO = {}
    stateDTO["state"] = "stopped"
    put "#{ENV['HOPSWORKS_API']}/project/#{project_id}/jobs/#{job_name}/executions/#{execution_id}/status", stateDTO
  end

  def get_execution_log(project_id, job_name, execution_id, type)
    get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/jobs/#{job_name}/executions/#{execution_id}/log/#{type}"
  end

  def wait_for_execution_active(project_id, job_name, execution_id, expected_active_state, appOrExecId)
    id = ''
    wait_result = wait_for_me_time do
      get_execution(project_id, job_name, execution_id)
      if appOrExecId.eql? 'id'
        id = json_body[:id]
      else
        id = json_body[:appId]
      end
      found_state = (json_body[:state].eql? expected_active_state) || !is_execution_active(json_body)
      { 'success' => found_state, 'msg' => "expected:#{expected_active_state} found:#{json_body[:state]}" }
    end
    expect(wait_result["success"]).to be(true), wait_result["msg"]
    expect(id).not_to be_nil
    id
  end

  def wait_for_execution_completed(project_id, job_name, execution_id, expected_end_state)
    wait_result = wait_for_me_time do
      get_execution(project_id, job_name, execution_id)
      unless is_execution_active(json_body)
        expect(json_body[:state]).to eq(expected_end_state), "job completed with state:#{json_body[:state]}"
      end
      found_state = json_body[:state].eql? expected_end_state
      { 'success' => found_state, 'msg' => "expected:#{expected_end_state} found:#{json_body[:state]}" }
    end
    expect(wait_result["success"]).to be(true), wait_result["msg"]
  end

  def find_executions(job_id)
    Execution.where(["job_id = ?", job_id]).select("id, name, creation_time, project_id, creator, json_config").first
  end

  def count_executions(job_id)
    Execution.where(["job_id = ?", job_id]).count
  end

  def is_execution_active(execution_dto)
    state = execution_dto["state"]
    !(state == "FINISHED" || state == "FAILED" || state == "KILLED" || state == "INITIALIZATION_FAILED")
  end
end

