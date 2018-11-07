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

  def start_execution(project_id, job_id)
    put "/hopsworks-api-v2/projects/#{project_id}/jobs/#{job_id}/executions?action=start"    
  end

  def stop_execution(project_id, job_id)
    put "/hopsworks-api-v2/projects/#{project_id}/jobs/#{job_id}/executions?action=stop"
  end

  def get_execution(project_id, job_id, execution_id)
    get "/hopsworks-api-v2/projects/#{project_id}/jobs/#{job_id}/executions/#{execution_id}"
  end

  def get_executions(project_id, job_id)
    get "/hopsworks-api-v2/projects/#{project_id}/jobs/#{job_id}/executions"
  end

  def wait_for_execution
    timeout = 600
    start = Time.now
    x = yield
    until x
      if Time.now - start > timeout
        raise "Timed out waiting for Job to finish. Timeout #{timeout} sec"
      end
      sleep(1)
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
