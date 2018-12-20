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
module ExecutionHelper

  def get_executions(project_id, job_name, query)
    get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/jobs/#{job_name}/executions#{query}"
  end

  def get_execution(project_id, job_name, execution_id)
    get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/jobs/#{job_name}/executions/#{execution_id}"
  end

  def start_execution(project_id, job_name)
    post "#{ENV['HOPSWORKS_API']}/project/#{project_id}/jobs/#{job_name}/executions?action=start"
  end

  def stop_execution(project_id, job_name)
    post "#{ENV['HOPSWORKS_API']}/project/#{project_id}/jobs/#{job_name}/executions?action=stop"
  end

  def get_execution_log(project_id, job_name, execution_id, type)
    get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/jobs/#{job_name}/executions/#{execution_id}/log/#{type}"
  end

  def wait_for_execution
    timeout = 120
    start = Time.now
    x = yield
    until x
      if Time.now - start > timeout
        raise "Timed out waiting for Job to finish. Timeout #{timeout} sec"
      end
      sleep(5)
      x = yield
    end
  end

  def find_executions(job_id)
    Execution.where(["job_id = ?", job_id]).select("id, name, creation_time, project_id, creator, json_config").first
  end

  def count_executions(job_id)
    Execution.where(["job_id = ?", job_id]).count
  end
end
