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
        describe 'create, get, delete executions' do
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
            get_executions(@project[:id], $job_name, "")
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
              json_body[:log] != "No log available"
            end

            #get out log
            get_execution_log(@project[:id], $job_name, execution_id, "out")
            expect(json_body[:type]).to eq "OUT"
            expect(json_body[:log]).to be_present

            #wait for log aggregation
            wait_for_execution do
              get_execution_log(@project[:id], $job_name, execution_id, "err")
              json_body[:log] != "No log available"
            end

            #get err log
            get_execution_log(@project[:id], $job_name, execution_id, "err")
            expect(json_body[:type]).to eq "ERR"
            expect(json_body[:log]).to be_present
          end
        end
      end
      describe 'execution sort, filter, offset and limit' do
        $job_spark_1 = "demo_job_1"
        $execution_ids = []
        context 'with authentication' do
          before :all do
            with_valid_tour_project("spark")
            create_sparktour_job(@project, $job_spark_1)
            #start 3 executions
            for i in 0..2 do
              start_execution(@project[:id], $job_spark_1)
              $execution_ids.push(json_body[:id])
              #wait till it's finished and start second execution
              wait_for_execution do
                get_execution(@project[:id], $job_spark_1, json_body[:id])
                json_body[:state].eql? "FINISHED"
              end
            end
          end
          after :all do
            clean_jobs(@project[:id])
          end
          describe "Executions sort" do
            it "should get all executions sorted by id asc" do
              #sort in memory and compare with query
              #get all executions of job
              get_executions(@project[:id], $job_spark_1, "")
              executions = json_body[:items].map {|execution| execution[:id]}
              sorted = executions.sort
              get_executions(@project[:id], $job_spark_1, "?sort_by=id:asc")
              sorted_res = json_body[:items].map {|execution| execution[:id]}
              expect(sorted_res).to eq(sorted)
            end
            it "should get all executions sorted by id desc" do
              #sort in memory and compare with query
              #get all executions of job
              get_executions(@project[:id], $job_spark_1, "")
              executions = json_body[:items].map {|execution| execution[:id]}
              sorted = executions.sort.reverse
              get_executions(@project[:id], $job_spark_1, "?sort_by=id:desc")
              sorted_res = json_body[:items].map {|execution| execution[:id]}
              expect(sorted_res).to eq(sorted)
            end
            it "should get all executions sorted by submissiontime asc" do
              #sort in memory and compare with query
              #get all executions of job
              get_executions(@project[:id], $job_spark_1, "")
              executions = json_body[:items].map {|execution| execution[:submissionTime]}
              sorted = executions.sort
              get_executions(@project[:id], $job_spark_1, "?sort_by=submissiontime:asc")
              sorted_res = json_body[:items].map {|execution| execution[:submissionTime]}
              expect(sorted_res).to eq(sorted)
            end
            it "should get all executions sorted by submissiontime desc" do
              #sort in memory and compare with query
              #get all executions of job
              get_executions(@project[:id], $job_spark_1, "")
              executions = json_body[:items].map {|execution| execution[:submissionTime]}
              sorted = executions.sort.reverse
              get_executions(@project[:id], $job_spark_1, "?sort_by=submissiontime:desc")
              sorted_res = json_body[:items].map {|execution| execution[:submissionTime]}
              expect(sorted_res).to eq(sorted)
            end
            it "should get all executions sorted by state asc" do
              #sort in memory and compare with query
              #get all executions of job
              get_executions(@project[:id], $job_spark_1, "")
              executions = json_body[:items].map {|execution| execution[:state]}
              sorted = executions.sort_by(&:downcase)
              get_executions(@project[:id], $job_spark_1, "?sort_by=state:asc")
              sorted_res = json_body[:items].map {|execution| execution[:state]}
              expect(sorted_res).to eq(sorted)
            end
            it "should get all executions sorted by state desc" do
              #sort in memory and compare with query
              #get all executions of job
              get_executions(@project[:id], $job_spark_1, "")
              executions = json_body[:items].map {|execution| execution[:state]}
              sorted = executions.sort_by(&:downcase).reverse
              get_executions(@project[:id], $job_spark_1, "?sort_by=state:desc")
              sorted_res = json_body[:items].map {|execution| execution[:state]}
              expect(sorted_res).to eq(sorted)
            end
            it "should get all executions sorted by finalStatus asc" do
              #sort in memory and compare with query
              #get all executions of job
              get_executions(@project[:id], $job_spark_1, "")
              executions = json_body[:items].map {|execution| execution[:finalStatus]}
              sorted = executions.sort_by(&:downcase)
              get_executions(@project[:id], $job_spark_1, "?sort_by=finalStatus:asc")
              sorted_res = json_body[:items].map {|execution| execution[:finalStatus]}
              expect(sorted_res).to eq(sorted)
            end
            it "should get all executions sorted by finalStatus desc" do
              #sort in memory and compare with query
              #get all executions of job
              get_executions(@project[:id], $job_spark_1, "")
              executions = json_body[:items].map {|execution| execution[:finalStatus]}
              sorted = executions.sort_by(&:downcase).reverse
              get_executions(@project[:id], $job_spark_1, "?sort_by=finalStatus:desc")
              sorted_res = json_body[:items].map {|execution| execution[:finalStatus]}
              expect(sorted_res).to eq(sorted)
            end
            it "should get all executions sorted by appId asc" do
              #sort in memory and compare with query
              #get all executions of job
              get_executions(@project[:id], $job_spark_1, "")
              executions = json_body[:items].map {|execution| execution[:appId]}
              sorted = executions.sort_by(&:downcase)
              get_executions(@project[:id], $job_spark_1, "?sort_by=appId:asc")
              sorted_res = json_body[:items].map {|execution| execution[:appId]}
              expect(sorted_res).to eq(sorted)
            end
            it "should get all executions sorted by appId desc" do
              #sort in memory and compare with query
              #get all executions of job
              get_executions(@project[:id], $job_spark_1, "")
              executions = json_body[:items].map {|execution| execution[:appId]}
              sorted = executions.sort_by(&:downcase).reverse
              get_executions(@project[:id], $job_spark_1, "?sort_by=appId:desc")
              sorted_res = json_body[:items].map {|execution| execution[:appId]}
              expect(sorted_res).to eq(sorted)
            end
            it "should get all executions sorted by progress asc" do
              #sort in memory and compare with query
              #get all executions of job
              get_executions(@project[:id], $job_spark_1, "")
              executions = json_body[:items].map {|execution| execution[:progress]}
              sorted = executions.sort
              get_executions(@project[:id], $job_spark_1, "?sort_by=progress:asc")
              sorted_res = json_body[:items].map {|execution| execution[:progress]}
              expect(sorted_res).to eq(sorted)
            end
            it "should get all executions sorted by progress desc" do
              #sort in memory and compare with query
              #get all executions of job
              get_executions(@project[:id], $job_spark_1, "")
              executions = json_body[:items].map {|execution| execution[:progress]}
              sorted = executions.sort.reverse
              get_executions(@project[:id], $job_spark_1, "?sort_by=progress:desc")
              sorted_res = json_body[:items].map {|execution| execution[:progress]}
              expect(sorted_res).to eq(sorted)
            end
            it "should get all executions sorted by duration asc" do
              #sort in memory and compare with query
              #get all executions of job
              get_executions(@project[:id], $job_spark_1, "")
              executions = json_body[:items].map {|execution| execution[:duration]}
              sorted = executions.sort
              get_executions(@project[:id], $job_spark_1, "?sort_by=duration:asc")
              sorted_res = json_body[:items].map {|execution| execution[:duration]}
              expect(sorted_res).to eq(sorted)
            end
            it "should get all executions sorted by duration desc" do
              #sort in memory and compare with query
              #get all executions of job
              get_executions(@project[:id], $job_spark_1, "")
              executions = json_body[:items].map {|execution| execution[:duration]}
              sorted = executions.sort.reverse
              get_executions(@project[:id], $job_spark_1, "?sort_by=duration:desc")
              sorted_res = json_body[:items].map {|execution| execution[:duration]}
              expect(sorted_res).to eq(sorted)
            end
          end
          describe "Executions offset, limit" do
            it 'should return limit=x executions.' do
              get_executions(@project[:id], $job_spark_1, "")
              executions = json_body[:items].map {|execution| execution[:id]}
              sorted = executions.sort
              get_executions(@project[:id], $job_spark_1, "?limit=2&sort_by=id:asc")
              sorted_res = json_body[:items].map {|execution| execution[:id]}
              expect(sorted_res).to eq(sorted.take(2))
            end
            it 'should return executions starting from offset=y.' do
              get_executions(@project[:id], $job_spark_1, "")
              executions = json_body[:items].map {|execution| execution[:id]}
              sorted = executions.sort
              get_executions(@project[:id], $job_spark_1, "?offset=1&sort_by=id:asc")
              sorted_res = json_body[:items].map {|execution| execution[:id]}
              expect(sorted_res).to eq(sorted.drop(1))
            end
            it 'should return limit=x executions with offset=y.' do
              get_executions(@project[:id], $job_spark_1, "")
              executions = json_body[:items].map {|execution| execution[:id]}
              sorted = executions.sort
              get_executions(@project[:id], $job_spark_1, "?offset=1&limit=2&sort_by=id:asc")
              sorted_res = json_body[:items].map {|execution| execution[:id]}
              expect(sorted_res).to eq(sorted.drop(1).take(2))
            end
            it 'should ignore if limit < 0.' do
              get_executions(@project[:id], $job_spark_1, "")
              executions = json_body[:items].map {|execution| execution[:id]}
              sorted = executions.sort
              get_executions(@project[:id], $job_spark_1, "?limit=-2&sort_by=id:asc")
              sorted_res = json_body[:items].map {|execution| execution[:id]}
              expect(sorted_res).to eq(sorted)
            end
            it 'should ignore if offset < 0.' do
              get_executions(@project[:id], $job_spark_1, "")
              executions = json_body[:items].map {|execution| execution[:id]}
              sorted = executions.sort
              get_executions(@project[:id], $job_spark_1, "?offset=-1&sort_by=id:asc")
              sorted_res = json_body[:items].map {|execution| execution[:id]}
              expect(sorted_res).to eq(sorted)
            end
            it 'should ignore if limit = 0.' do
              get_executions(@project[:id], $job_spark_1, "")
              executions = json_body[:items].map {|execution| execution[:id]}
              sorted = executions.sort
              get_executions(@project[:id], $job_spark_1, "?limit=0&sort_by=id:asc")
              sorted_res = json_body[:items].map {|execution| execution[:id]}
              expect(sorted_res).to eq(sorted)
            end
            it 'should ignore if offset = 0.' do
              get_executions(@project[:id], $job_spark_1, "")
              executions = json_body[:items].map {|execution| execution[:id]}
              sorted = executions.sort
              get_executions(@project[:id], $job_spark_1, "?offset=0&sort_by=id:asc")
              sorted_res = json_body[:items].map {|execution| execution[:id]}
              expect(sorted_res).to eq(sorted)
            end
            it 'should ignore if limit=0 and offset = 0.' do
              get_executions(@project[:id], $job_spark_1, "")
              executions = json_body[:items].map {|execution| execution[:id]}
              sorted = executions.sort
              get_executions(@project[:id], $job_spark_1, "?offset=0&limit=0&sort_by=id:asc")
              sorted_res = json_body[:items].map {|execution| execution[:id]}
              expect(sorted_res).to eq(sorted)
            end
          end
          describe "Executions filter" do
            it "should get all executions filtered by state" do
              get_executions(@project[:id], $job_spark_1, "?filter_by=state:finished")
              json_body[:items].map {|execution| execution[:id]}
              expect(json_body[:items].count).to eq 3
            end
            it "should get all executions filtered by state_neq" do
              get_executions(@project[:id], $job_spark_1, "?filter_by=state_neq:finished")
              expect(json_body[:items]).to be nil
            end
            it "should get all executions filtered by finalStatus" do
              get_executions(@project[:id], $job_spark_1, "?filter_by=finalstatus:succeeded")
              json_body[:items].map {|execution| execution[:id]}
              expect(json_body[:items].count).to eq 3
            end
            it "should get all executions filtered by finalStatus_neq" do
              get_executions(@project[:id], $job_spark_1, "?filter_by=finalstatus_neq:succeeded")
              expect(json_body[:items]).to be nil
            end
            it "should get all executions filtered by submissiontime" do
              #get submissiontime of last execution
              get_execution(@project[:id], $job_spark_1, $execution_ids[0])
              submissiontime = json_body[:submissionTime]
              get_executions(@project[:id], $job_spark_1, "?filter_by=submissiontime:#{submissiontime.gsub('Z','')}.000")
              expect_status(200)
              expect(json_body[:items].count).to eq 1
            end
            it "should get all executions filtered by submissiontime gt" do
              #get submissiontime of last execution
              get_execution(@project[:id], $job_spark_1, $execution_ids[0])
              submissiontime = json_body[:submissionTime]
              get_executions(@project[:id], $job_spark_1, "?filter_by=submissiontime_gt:#{submissiontime.gsub('Z','')}.000")
              expect_status(200)
              expect(json_body[:items].count).to eq 2
            end
            it "should get all executions filtered by submissiontime lt" do
              #get submissiontime of last execution
              get_execution(@project[:id], $job_spark_1, $execution_ids[1])
              submissiontime = json_body[:submissionTime]
              get_executions(@project[:id], $job_spark_1, "?filter_by=submissiontime_lt:#{submissiontime.gsub('Z','')}.000")
              expect_status(200)
              expect(json_body[:items].count).to eq 1
            end
          end
        end
      end
    end
  end
end