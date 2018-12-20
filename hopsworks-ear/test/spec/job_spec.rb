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
  describe 'job' do
    context 'without authentication' do
      before :all do
        with_valid_project
        reset_session
      end
      it "should fail" do
        get_job(@project[:id], 1, nil)
        expect_json(errorCode: 200003)
        expect_status(401)
      end
    end
    context 'with authentication create, delete, get' do
      before :all do
        with_valid_tour_project("spark")
      end
      after :each do
        clean_jobs(@project[:id])
      end
      it "should create three spark jobs" do
        create_sparktour_job(@project, "demo_job_1")
        expect_status(201)
        create_sparktour_job(@project, "demo_job_2")
        expect_status(201)
        create_sparktour_job(@project, "demo_job_3")
        expect_status(201)
      end
      it "should fail to create two jobs with same name" do
        create_sparktour_job(@project, "demo_job_1")
        create_sparktour_job(@project, "demo_job_1")
        expect_status(409)
      end
      it "should get a single spark job" do
        create_sparktour_job(@project, "demo_job_1")
        get_job(@project[:id], "demo_job_1", nil)
        expect_status(200)
      end
      it "should get spark job dto with href" do
        create_sparktour_job(@project, "demo_job_1")
        get_job(@project[:id], "demo_job_1", nil)
        expect_status(200)
        #validate href
        expect(URI(json_body[:href]).path).to eq "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jobs/demo_job_1"
        expect(json_body[:config][:type]).to eq "sparkJobConfiguration"
        expect(json_body[:jobType]).to eq "SPARK"
      end
      it "should get three jobs" do
        create_sparktour_job(@project, "demo_job_1")
        create_sparktour_job(@project, "demo_job_2")
        create_sparktour_job(@project, "demo_job_3")
        get_jobs(@project[:id], nil)
        expect_status(200)
        expect(json_body[:items].count).to eq 3
        expect(json_body[:count]).to eq 3
      end
      it "should inspect spark app jar and get configuration" do
        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jobs/spark/inspection?path=/Projects/#{@project[:projectname]}/TestJob/spark-examples.jar"
        expect_status(200)
        expect(json_body[:type]).to eq ("sparkJobConfiguration")
      end
      it "should delete created job" do
        create_sparktour_job(@project, "demo_job_1")
        delete_job(@project[:id], "demo_job_1")
        expect_status(204)
      end
      it "should fail deleting non-existing job" do
        delete_job(@project[:id], "demo_job_1")
        expect_status(404)
      end
      it "should fail to delete job as Data Scientist" do
        create_sparktour_job(@project, "demo_job_1")
        member = create_user
        add_member(member[:email], "Data scientist")
        create_session(member[:email], "Pass123")
        delete_job(@project[:id], "demo_job_1")
      end
    end
  end
  describe 'job sort, filter, offset and limit' do
    $job_spark_1 = "demo_job_1"
    $job_spark_2 = "demo_job_2"
    context 'with authentication' do
      before :all do
        with_valid_tour_project("spark")
        create_sparktour_job(@project, $job_spark_1)
        create_sparktour_job(@project, "demo_job_2")
        create_sparkpy_job(@project, "demo_pyjob_1")
        create_sparktour_job(@project, "demo_job_3")
        create_sparktour_job(@project, "demo_job_4")
        create_sparktour_job(@project, "demo_job_5")
        create_sparkpy_job(@project, "demo_pyjob_2")
      end
      after :all do
        clean_jobs(@project[:id])
      end
      describe "Jobs sort" do
        it "should get all jobs sorted by name" do
          #sort in memory and compare with query
          get_jobs(@project[:id], nil)
          jobs = json_body[:items].map {|job| job[:name]}
          sorted = jobs.sort_by(&:downcase)
          get_jobs(@project[:id], "?sort_by=name:asc")
          sorted_res = json_body[:items].map {|job| job[:name]}
          expect(sorted_res).to eq(sorted)
        end
        it "should get all jobs sorted by name descending" do
          #sort in memory and compare with query
          get_jobs(@project[:id], nil)
          jobs = json_body[:items].map {|job| job[:name]}
          sorted = jobs.sort_by(&:downcase).reverse
          get_jobs(@project[:id], "?sort_by=name:desc")
          sorted_res = json_body[:items].map {|job| job[:name]}
          expect(sorted_res).to eq(sorted)
        end
        it "should get all jobs sorted by id" do
          #sort in memory and compare with query
          get_jobs(@project[:id], nil)
          jobs = json_body[:items].map {|job| job[:id]}
          sorted = jobs.sort
          get_jobs(@project[:id], "?sort_by=id:asc")
          sorted_res = json_body[:items].map {|job| job[:id]}
          expect(sorted_res).to eq(sorted)
        end
        it "should get all jobs sorted by id descending" do
          #sort in memory and compare with query
          get_jobs(@project[:id], nil)
          jobs = json_body[:items].map {|job| job[:id]}
          sorted = jobs.sort.reverse
          get_jobs(@project[:id], "?sort_by=id:desc")
          sorted_res = json_body[:items].map {|job| job[:id]}
          expect(sorted_res).to eq(sorted)
        end
        it "should get all jobs sorted by date created" do
          #sort in memory and compare with query
          get_jobs(@project[:id], nil)
          jobs = json_body[:items].map {|job| job[:creationTime]}
          sorted = jobs.sort
          get_jobs(@project[:id], "?sort_by=date_created:asc")
          sorted_res = json_body[:items].map {|job| job[:creationTime]}
          expect(sorted_res).to eq(sorted)
        end
        it "should get all jobs sorted by date created descending" do
          #sort in memory and compare with query
          get_jobs(@project[:id], nil)
          jobs = json_body[:items].map {|job| job[:creationTime]}
          sorted = jobs.sort.reverse
          get_jobs(@project[:id], "?sort_by=date_created:desc")
          sorted_res = json_body[:items].map {|job| job[:creationTime]}
          expect(sorted_res).to eq(sorted)
        end
        it "should get all jobs sorted by type ascending and name ascending" do
          #sort in memory and compare with query
          get_jobs(@project[:id], nil)
          jobs = json_body[:items].map {|job| "#{job[:jobType]} #{job[:name]}"}
          sorted = jobs.sort
          get_jobs(@project[:id], "?sort_by=jobtype:asc,name:asc")
          sorted_res = json_body[:items].map {|job| "#{job[:jobType]} #{job[:name]}"}
          expect(sorted_res).to eq(sorted)
        end
        it "should get all jobs sorted by type ascending and name descending" do
          #sort in memory and compare with query
          get_jobs(@project[:id], nil)
          s = json_body[:items].sort do |a, b|
            res = (a[:jobType] <=> b[:jobType])
            res = -(a[:name] <=> b[:name]) if res == 0
            res
          end
          sorted = s.map {|o| "#{o[:jobType]} #{o[:name]}"}
          get_jobs(@project[:id], "?sort_by=jobtype:asc,name:desc")
          sorted_res = json_body[:items].map {|job| "#{job[:jobType]} #{job[:name]}"}
          expect(sorted_res).to eq(sorted)
        end
        it "should get all jobs sorted by type descending and name ascending" do
          #sort in memory and compare with query
          get_jobs(@project[:id], nil)
          s = json_body[:items].sort do |a, b|
            res = -(a[:jobType] <=> b[:jobType])
            res = (a[:name] <=> b[:name]) if res == 0
            res
          end
          sorted = s.map {|o| "#{o[:jobType]} #{o[:name]}"}
          get_jobs(@project[:id], "?sort_by=jobtype:desc,name:asc")
          sorted_res = json_body[:items].map {|job| "#{job[:jobType]} #{job[:name]}"}
          expect(sorted_res).to eq(sorted)
        end
        it "should fail to sort by unsupported field" do
          get_jobs(@project[:id], "?sort_by=unsupported:desc,name:asc")
          expect_status(404)
          expect_json(errorCode: 120004)
        end
      end
      describe "Jobs filter" do
        it "should get three jobs with type spark" do
          get_jobs(@project[:id], "?filter_by=jobtype:spark")
          expect_status(200)
          expect(json_body[:items].count).to eq 5
        end
        it "should get three jobs with type pyspark" do
          get_jobs(@project[:id], "?filter_by=jobtype:pyspark")
          expect_status(200)
          expect(json_body[:items].count).to eq 2
        end
        it "should fail to find job with type flink" do
          get_jobs(@project[:id], "?filter_by=jobtype:flink")
          expect_status(200)
          expect(json_body[:items]).to be nil
        end
        it "should find jobs with name like 'demo'" do
          get_jobs(@project[:id], "?filter_by=name:demo")
          expect_status(200)
          expect(json_body[:items].count).to eq 7
        end
        it "should find jobs with name like 'demo_job'" do
          get_jobs(@project[:id], "?filter_by=name:demo")
          expect_status(200)
          expect(json_body[:items].count).to eq 7
        end
        it "should find job with name like" do
          get_jobs(@project[:id], "?filter_by=name:" + $job_spark_1)
          expect_status(200)
          expect(json_body[:items].count).to eq 1
        end

        describe "Jobs filter latest_execution" do
          it "should execute two jobs and search based on finalStatus" do
            start_execution(@project[:id], $job_spark_1)
            execution_id = json_body[:id]
            wait_for_execution do
              get_execution(@project[:id], $job_spark_1, execution_id)
              json_body[:state].eql? "FINISHED"
            end
            start_execution(@project[:id], $job_spark_2)
            execution_id = json_body[:id]
            wait_for_execution do
              get_execution(@project[:id], $job_spark_2, execution_id)
              json_body[:state].eql? "FINISHED"
              end
            get_jobs(@project[:id], "?filter_by=latest_execution:finished")
            expect_status(200)
            expect(json_body[:items].count).to eq 2
          end
          it "should execute two jobs and search based on finalStatus with query startingWith term" do
            get_jobs(@project[:id], "?filter_by=latest_execution:finis")
            expect_status(200)
            expect(json_body[:items].count).to eq 2
          end
          it "should execute two jobs and search based on state" do
            get_jobs(@project[:id], "?filter_by=latest_execution:succeeded")
            expect_status(200)
            expect(json_body[:items].count).to eq 2
          end
          it "should execute two jobs and search based on state with query startingWith term" do
            get_jobs(@project[:id], "?filter_by=latest_execution:succee")
            expect_status(200)
            expect(json_body[:items].count).to eq 2
          end
          it "should execute two jobs and search based on name" do
            get_jobs(@project[:id], "?filter_by=latest_execution:" + $job_spark_1)
            expect_status(200)
            expect(json_body[:items].count).to eq 1
          end
          it "should execute two jobs and search based on name with query startingWith term" do
            get_jobs(@project[:id], "?filter_by=latest_execution:demo_job")
            expect_status(200)
            expect(json_body[:items].count).to eq 5
          end
        end
      end
    end
  end
end
