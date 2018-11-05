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
describe "On #{ENV['OS']}" do
  describe 'execution' do
    after (:all) {clean_projects}
    describe "#create" do
      $job_name = "demo_job_1"
      context 'without authentication' do
        before :all do
          with_valid_project
          reset_session
        end
        it "should fail" do
          create_sparktour_job(@project, $job_name)
          expect_json(errorCode: 200003)
          expect_status(401)
        end
      end
      context 'with authentication' do
        before :all do
          with_valid_tour_project("spark")

        end
        after :each do
          clean_jobs(@project[:id])
        end
        it "should start a job and get its executions" do
          #create job
          create_sparktour_job(@project, $job_name)
          job_id = json_body[:id]
          #start execution
          start_execution(@project[:id], $job_name)
          execution_id = json_body[:id]
          expect_status(201)
          expect(json_body[:state]).to eq "INITIALIZING"
          #get execution
          get_execution(@project[:id], $job_name, json_body[:id])
          expect_status(200)
          expect(json_body[:id]).to eq(execution_id)
          #wait till it's finished and start second execution
          wait_for_execution do
            get_execution(@project[:id], $job_name, json_body[:id])
            json_body[:state].eql? "FINISHED"
          end
          #start execution
          start_execution(@project[:id], $job_name)
          execution_id = json_body[:id]
          expect_status(201)

          #get all executions of job
          get_executions(@project[:id], $job_name)
          expect(json_body[:items].count).to eq 2

          #check database
          num_executions = count_executions(job_id)
          expect(num_executions).to eq 2

          wait_for_execution do
            get_execution(@project[:id], $job_name, execution_id)
            json_body[:state].eql? "FINISHED"
          end
        end

        it "should start and stop job" do
          create_sparktour_job(@project, $job_name)
          expect_status(201)

          #start execution
          start_execution(@project[:id], $job_name)
          execution_id = json_body[:id]
          expect_status(201)
          wait_for_execution do
            get_execution(@project[:id], $job_name, execution_id)
            json_body[:state].eql? "ACCEPTED"
          end

          stop_execution(@project[:id], $job_name)
          expect_status(200)
          wait_for_execution do
            get_execution(@project[:id], $job_name, execution_id)
            json_body[:state].eql? "KILLED"
          end
        end

        it "should fail to start two executions in parallel" do
          create_sparktour_job(@project, $job_name)
          start_execution(@project[:id], $job_name)
          start_execution(@project[:id], $job_name)
          expect_status(400)
          expect_json(errorCode: 130010)
        end
        it "should run job and get out and err logs" do
          create_sparktour_job(@project, $job_name)
          start_execution(@project[:id], $job_name)
          execution_id = json_body[:id]
          expect_status(201)

          wait_for_execution do
            get_execution(@project[:id], $job_name, execution_id)
            json_body[:state].eql? "FINISHED"
          end

          #wait for log aggregation
          wait_for_execution do
            get_execution_log(@project[:id], $job_name, execution_id, "out")
            json_body[:message] != "No log available"
          end

          #get out log
          get_execution_log(@project[:id], $job_name, execution_id, "out")
          expect(json_body[:type]).to eq "out"
          expect(json_body[:message]).to be_present

          #wait for log aggregation
          wait_for_execution do
            get_execution_log(@project[:id], $job_name, execution_id, "err")
            json_body[:message] != "No log available"
          end

          #get err log
          get_execution_log(@project[:id], $job_name, execution_id, "err")
          expect(json_body[:type]).to eq "err"
          expect(json_body[:message]).to be_present
        end
      end
    end
  end
end