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

describe "On #{ENV['OS']}" do
  after(:all) {clean_all_test_projects}
  describe 'execution' do
    describe "#create" do
      context 'without authentication' do
        before :all do
          with_valid_project
          reset_session
        end
        it "should fail" do
          create_sparktour_job(@project, "demo_job", 'jar', nil)
          expect_status(401)
          expect_json(errorCode: 200003)
        end
      end
      job_types = ['py', 'jar', 'ipynb']
      job_types.each do |type|
        context 'with authentication and executable ' + type do
          before :all do
            with_valid_tour_project("spark")
          end
          after :each do
            clean_jobs(@project[:id])
          end
          describe 'create, get, delete executions' do
            it "should start a job and get its executions" do
              #create job
              $job_name_1 = "demo_job_1_" + type
              create_sparktour_job(@project, $job_name_1, type, nil)
              job_id = json_body[:id]
              #start execution
              start_execution(@project[:id], $job_name_1, nil)
              execution_id = json_body[:id]
              expect_status(201)
              expect(json_body[:state]).to eq "INITIALIZING"
              #get execution
              get_execution(@project[:id], $job_name_1, json_body[:id])
              expect_status(200)
              expect(json_body[:id]).to eq(execution_id)
              #wait till it's finished and start second execution
              wait_for_execution_completed(@project[:id], $job_name_1, json_body[:id], "FINISHED")
              #start execution
              start_execution(@project[:id], $job_name_1, nil)
              execution_id = json_body[:id]
              expect_status(201)

              #get all executions of job
              get_executions(@project[:id], $job_name_1, "")
              expect(json_body[:items].count).to eq 2

              #check database
              num_executions = count_executions(job_id)
              expect(num_executions).to eq 2

              wait_for_execution_completed(@project[:id], $job_name_1, execution_id, "FINISHED")
            end
            it "should start and stop job" do
              $job_name_2 = "demo_job_2_" + type
              create_sparktour_job(@project, $job_name_2, type, nil)
              expect_status(201)

              #start execution
              start_execution(@project[:id], $job_name_2, nil)
              execution_id = json_body[:id]
              expect_status(201)
              wait_for_execution_active(@project[:id], $job_name_2, execution_id, "ACCEPTED")
              stop_execution(@project[:id], $job_name_2, execution_id)
              expect_status(202)
              wait_for_execution_completed(@project[:id], $job_name_2, execution_id, "KILLED")
            end
            it "should fail to start a spark job with missing spark.yarn.dist.files" do
              $job_name_2 = "demo_job_2_" + type
              create_sparktour_job(@project, $job_name_2, type, nil)
              config = json_body[:config]
              config[:'spark.yarn.dist.files'] = "hdfs:///Projects/#{@project[:projectname]}/Resources/iamnothere.txt"
              create_sparktour_job(@project, $job_name_2, type, config)
              #start execution
              start_execution(@project[:id], $job_name_2, nil)
              expect_status(400)
            end
            it "should fail to start a spark job with missing spark.yarn.dist.pyFiles" do
              $job_name_2 = "demo_job_2_" + type
              create_sparktour_job(@project, $job_name_2, type, nil)
              config = json_body[:config]
              config[:'spark.yarn.dist.pyFiles'] = "hdfs:///Projects/#{@project[:projectname]}/Resources/iamnothere.py"
              create_sparktour_job(@project, $job_name_2, type, config)
              #start execution
              start_execution(@project[:id], $job_name_2, nil)
              expect_status(400)
            end
            it "should fail to start a spark job with missing spark.yarn.dist.jars" do
              $job_name_2 = "demo_job_2_" + type
              create_sparktour_job(@project, $job_name_2, type, nil)
              config = json_body[:config]
              config[:'spark.yarn.dist.jars'] = "hdfs:///Projects/#{@project[:projectname]}/Resources/iamnothere.jar"
              create_sparktour_job(@project, $job_name_2, type, config)
              #start execution
              start_execution(@project[:id], $job_name_2, nil)
              expect_status(400)
            end
            it "should fail to start a spark job with missing spark.yarn.dist.archives" do
              $job_name_2 = "demo_job_2_" + type
              create_sparktour_job(@project, $job_name_2, type, nil)
              config = json_body[:config]
              config[:'spark.yarn.dist.archives'] = "hdfs:///Projects/#{@project[:projectname]}/Resources/iamnothere.zip"
              create_sparktour_job(@project, $job_name_2, type, config)

              #start execution
              start_execution(@project[:id], $job_name_2, nil)
              expect_status(400)
            end
            it "should start two executions in parallel" do
              $job_name_3 = "demo_job_3_" + type
              create_sparktour_job(@project, $job_name_3, type, nil)
              start_execution(@project[:id], $job_name_3, nil)
              expect_status(201)
              start_execution(@project[:id], $job_name_3, nil)
              expect_status(201)
            end
            it "should start a job with args 123" do
              $job_name_3 = "demo_job_3_" + type
              create_sparktour_job(@project, $job_name_3, type, nil)
              args = "123"
              start_execution(@project[:id], $job_name_3, args)
              execution_id = json_body[:id]
              expect_status(201)
              get_execution(@project[:id], $job_name_3, execution_id)
              expect_status(200)
              expect(json_body[:args]).to eq args
            end
            it "should run job and get out and err logs" do
              $job_name_4 = "demo_job_4_" + type
              create_sparktour_job(@project, $job_name_4, type, nil)
              start_execution(@project[:id], $job_name_4, nil)
              execution_id = json_body[:id]
              expect_status(201)

              wait_for_execution_completed(@project[:id], $job_name_4, execution_id, "FINISHED")

              #wait for log aggregation
              wait_for_execution do
                get_execution_log(@project[:id], $job_name_4, execution_id, "out")
                json_body[:log] != "No log available"
              end

              #get out log
              get_execution_log(@project[:id], $job_name_4, execution_id, "out")
              expect(json_body[:type]).to eq "OUT"
              expect(json_body[:log]).to be_present

              #wait for log aggregation
              wait_for_execution do
                get_execution_log(@project[:id], $job_name_4, execution_id, "err")
                json_body[:log] != "No log available"
              end

              #get err log
              get_execution_log(@project[:id], $job_name_4, execution_id, "err")
              expect(json_body[:type]).to eq "ERR"
              expect(json_body[:log]).to be_present
            end
          end
        end
      end
      describe 'execution sort, filter, offset and limit' do
        $job_spark_1 = "demo_job_1"
        $execution_ids = []
        context 'with authentication' do
          before :all do
            with_valid_tour_project("spark")
            create_sparktour_job(@project, $job_spark_1, 'jar', nil)
            #start 3 executions
            for i in 0..2 do
              start_execution(@project[:id], $job_spark_1, nil)
              $execution_ids.push(json_body[:id])
              #wait till it's finished and start second execution
              wait_for_execution_completed(@project[:id], $job_spark_1, json_body[:id], "FINISHED")
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
              get_executions(@project[:id], $job_spark_1, "?filter_by=submissiontime:#{submissiontime.gsub('Z','')}.000Z")
              expect_status(200)
              expect(json_body[:items].count).to eq 1
            end
            it "should get all executions filtered by submissiontime gt" do
              #get submissiontime of last execution
              get_execution(@project[:id], $job_spark_1, $execution_ids[0])
              submissiontime = json_body[:submissionTime]
              get_executions(@project[:id], $job_spark_1, "?filter_by=submissiontime_gt:#{submissiontime.gsub('Z','')}.000Z")
              expect_status(200)
              expect(json_body[:items].count).to eq 2
            end
            it "should get all executions filtered by submissiontime lt" do
              #get submissiontime of last execution
              get_execution(@project[:id], $job_spark_1, $execution_ids[1])
              submissiontime = json_body[:submissionTime]
              get_executions(@project[:id], $job_spark_1, "?filter_by=submissiontime_lt:#{submissiontime.gsub('Z','')}.000Z")
              expect_status(200)
              expect(json_body[:items].count).to eq 1
            end
          end
        end
      end
    end

    describe "#quota" do
      before :all do
        @cookies = with_admin_session
        with_valid_tour_project("spark")
      end

      after :all do
        @cookies = nil
      end

      it 'should not be able to run jobs with 0 quota and payment type PREPAID' do
        set_yarn_quota(@project, 0)
        set_payment_type(@project, "PREPAID")
        create_sparktour_job(@project, "quota1", 'jar', nil)
        start_execution(@project[:id], "quota1", nil)
        expect_status(412)
      end

      it 'should not be able to run jobs with negative quota and payment type PREPAID' do
        set_yarn_quota(@project, -10)
        set_payment_type(@project, "PREPAID")
        create_sparktour_job(@project, "quota2", 'jar', nil)
        start_execution(@project[:id], "quota2", nil)
        expect_status(412)
      end

      it 'should not kill the running job if the quota goes negative' do
        set_yarn_quota(@project, 1)
        set_payment_type(@project, "PREPAID")
        create_sparktour_job(@project, "quota3", 'jar', nil)
        start_execution(@project[:id], "quota3", nil)
        expect_status(201)
        execution_id = json_body[:id]

        wait_for_execution_completed(@project[:id], "quota3", execution_id, "FINISHED")
      end

      it 'should be able to run jobs with 0 quota and payment type NOLIMIT' do
        set_yarn_quota(@project, 0)
        set_payment_type(@project, "NOLIMIT")
        create_sparktour_job(@project, "quota4", 'jar', nil)
        start_execution(@project[:id], "quota4", nil)
        expect_status(201)
      end

      it 'should be able to run jobs with negative quota and payment type NOLIMIT' do
        set_yarn_quota(@project, -10)
        set_payment_type(@project, "NOLIMIT")
        create_sparktour_job(@project, "quota5", 'jar', nil)
        start_execution(@project[:id], "quota5", nil)
        expect_status(201)
      end
    end
  end
end