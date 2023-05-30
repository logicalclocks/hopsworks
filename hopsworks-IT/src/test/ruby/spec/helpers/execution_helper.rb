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

TERMINAL_EXECUTION_FINAL_STATUS = ["SUCCEEDED", "KILLED", "FAILED"]
TERMINAL_EXECUTION_STATE = ["FINISHED", "FAILED", "KILLED", "INITIALIZATION_FAILED", "APP_MASTER_START_FAILED", "FRAMEWORK_FAILURE"]

module ExecutionHelper

  def get_executions(project_id, job_name, query: "", expected_status: 200)
    get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/jobs/#{job_name}/executions#{query}"
    expect_status_details(expected_status)
  end

  def get_execution(project_id, job_name, execution_id, expected_status: 200)
    get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/jobs/#{job_name}/executions/#{execution_id}"
    expect_status_details(expected_status)
  end

  def start_execution(project_id, job_name, args: nil, expected_status: 201, error_code: nil)
    pp "active executions:#{get_existing_active_executions()}" if defined?(@debugOpt) && @debugOpt
    headers = { 'Content-Type' => 'text/plain' }
    path = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/jobs/#{job_name}/executions"
    pp "#{path}, #{args}, #{headers}" if defined?(@debugOpt) && @debugOpt
    resp = post path, args, headers
    expect_status_details(expected_status, error_code: error_code)
    resp
  end

  def stop_execution(project_id, job_name, execution_id, expected_status: 202)
    put "#{ENV['HOPSWORKS_API']}/project/#{project_id}/jobs/#{job_name}/executions/#{execution_id}/status"
    expect_status_details(expected_status)
  end

  def delete_execution(project_id, job_name, execution_id, expected_status: 204)
    delete "#{ENV['HOPSWORKS_API']}/project/#{project_id}/jobs/#{job_name}/executions/#{execution_id}"
    expect_status_details(expected_status)
  end

  def get_execution_log(project_id, job_name, execution_id, type)
    get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/jobs/#{job_name}/executions/#{execution_id}/log/#{type}"
  end

  def wait_for_execution_active(project_id, job_name, execution_id, expected_active_state, appOrExecId)
    begin
      id = ''
      wait_result = wait_for_me_time do
        get_execution(project_id, job_name, execution_id)
        if appOrExecId.eql? 'id'
          id = json_body[:id]
        else
          id = json_body[:appId]
        end
        found_state = (json_body[:state].eql? expected_active_state) || !is_execution_active(json_body)
        pp "waiting execution:<#{execution_id}, #{id}> active - state:#{json_body[:state]}" if defined?(@debugOpt) && @debugOpt
        { 'success' => found_state, 'msg' => "expected:#{expected_active_state} found:#{json_body[:state]}" }
      end
      expect(wait_result["success"]).to be(true), wait_result["msg"]
      expect(id).not_to be_nil
      id
    rescue StandardError => error
      wait_for_me_time(timeout=5, delay=5) do
        begin
          stop_execution(project_id, job_name, execution_id)
        rescue RSpec::Expectations::ExpectationNotMetError => e
          { 'success' => false }
        else
          { 'success' => true }
        end
      end
      raise error
    end
  end

  def wait_for_execution_completed(project_id, job_name, execution_id, expected_end_state, expected_final_status: nil)
    begin
      wait_result = wait_for_me_time do
        get_execution(project_id, job_name, execution_id)
        execution_finished = !is_execution_active(json_body)
        pp "waiting execution completed - state:#{json_body[:state]}" if defined?(@debugOpt) && @debugOpt
        { 'success' => execution_finished, 'msg' => "expected:#{expected_end_state} found:#{json_body[:state]}", "result" => json_body}
      end
      expect(wait_result["success"]).to be(true), wait_result["msg"]
      expect(wait_result["result"][:state]).to eq(expected_end_state), "job completed with state:#{json_body[:state]}"
      expect(wait_result["result"][:finalStatus]).to eq(expected_final_status) unless expected_final_status.nil?
    rescue StandardError => error
      wait_for_me_time(timeout=5, delay=5) do
        begin
          stop_execution(project_id, job_name, execution_id)
        rescue RSpec::Expectations::ExpectationNotMetError => e
          { 'success' => false }
        else
          { 'success' => true }
        end
      end
      raise error
    end
  end

  def run_execution(project_id, job_name)
    start_execution(project_id, job_name)
    execution_id = json_body[:id]
    wait_for_execution_completed(project_id, job_name, execution_id, "FINISHED")
    execution_id
  end

  def find_executions(job_id)
    Execution.where(["job_id = ?", job_id]).select("id, name, creation_time, project_id, creator, json_config").first
  end

  def count_executions(job_id)
    Execution.where(["job_id = ?", job_id]).count
  end

  def is_execution_active(execution_dto)
    state = execution_dto[:state]
    finalStatus = execution_dto[:finalStatus]
    not (TERMINAL_EXECUTION_STATE.include?(state) && TERMINAL_EXECUTION_FINAL_STATUS.include?(finalStatus))
  end

  #job_type: SPARK/PYTHON maybe later DOCKER
  def run_job(project, job_name, job_type: "SPARK", args: nil)
    arguments = nil
    arguments = args.join(' ') unless args.nil?
    start_execution(project[:id], job_name, args: arguments)
    execution_id = json_body[:id]
    if job_type == "SPARK"
      app_id = wait_for_execution_active(project[:id], job_name, execution_id, "RUNNING", "appId")
      wait_for_execution_completed(project[:id], job_name, execution_id, "FINISHED", expected_final_status: "SUCCEEDED")
    elsif job_type == "PYTHON"
      app_id = wait_for_execution_active(project[:id], job_name, execution_id, "RUNNING", "id")
      wait_for_execution_completed(project[:id], job_name, execution_id, "FINISHED", expected_final_status: "SUCCEEDED")
    else
      app_id = wait_for_execution_active(project[:id], job_name, execution_id, "RUNNING", "id")
    end

    { app_id: app_id, execution_id: execution_id }
  end

  def wait_for_yarn_app_state(app_id, state)
    app_state = ""
    wait_for(60, "YARN app did not transition to #{state}") do
      app_state = get_application_state(app_id, state)
      app_state == state
    end
    expect(app_state).to eq state
  end

  def wait_for_kube_job(job_name, should_exist=true, timeout=10)
    kube_user = Variables.find_by(id: "kube_user").value
    output = nil
    wait_for(timeout, "Kubernetes job was not removed") do
      #Check Kubernetes that the job has been removed
      cmd = "sudo su #{kube_user} /bin/bash -c \"kubectl get jobs -A\""
      Open3.popen3(cmd) do |_, stdout, _, _|
        output = stdout.read
      end
      puts "should_exist: #{should_exist}, output: #{output}"
      if should_exist
        output != "No resources found" and output != ""
      else
        output == "No resources found" or output == ""
      end
    end
    if should_exist
      expect(output).to include(job_name.gsub("_","-"))
    else
      expect(output).not_to include(job_name.gsub("_","-"))
    end
  end

  def wait_for_docker_job_output(path)
    file_found = false
    wait_for(60, "Docker job output not found") do
      file_found = test_file(path)
    end
    expect(file_found).to be true
  end
end

