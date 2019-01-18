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
        create_sparktour_job(@project, "demo_job_1", "jar", nil)
        expect_status(201)
        create_sparktour_job(@project, "demo_job_2", "jar", nil)
        expect_status(201)
        create_sparktour_job(@project, "demo_job_3", "jar", nil)
        expect_status(201)
      end
      it "should update a job with different job_config" do
        create_sparktour_job(@project, "demo_job_1", "jar", nil)
        #get job, change args and config params and put it
        get_job(@project[:id], "demo_job_1", nil)
        config = json_body[:config]
        config[:args] = '100'
        config[:'spark.executor.memory'] = '2048'
        create_sparktour_job(@project, "demo_job_1", "jar", config)
        expect_status(200)
        get_job(@project[:id], "demo_job_1", nil)
        expect(json_body[:config][:args]).to eq "100"
        expect(json_body[:config][:'spark.executor.memory']).to eq 2048
      end
      it "should get a single spark job" do
        create_sparktour_job(@project, "demo_job_1", "jar", nil)
        get_job(@project[:id], "demo_job_1", nil)
        expect_status(200)
      end
      it "should get spark job dto with href" do
        create_sparktour_job(@project, "demo_job_1", "jar", nil)
        get_job(@project[:id], "demo_job_1", nil)
        expect_status(200)
        #validate href
        expect(URI(json_body[:href]).path).to eq "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jobs/demo_job_1"
        expect(json_body[:config][:type]).to eq "sparkJobConfiguration"
        expect(json_body[:jobType]).to eq "SPARK"
      end
      it "should get three jobs" do
        create_sparktour_job(@project, "demo_job_1", "jar", nil)
        create_sparktour_job(@project, "demo_job_2", "jar", nil)
        create_sparktour_job(@project, "demo_job_3", "jar", nil)
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
        create_sparktour_job(@project, "demo_job_1", "jar", nil)
        delete_job(@project[:id], "demo_job_1")
        expect_status(204)
      end
      it "should fail deleting non-existing job" do
        delete_job(@project[:id], "demo_job_1")
        expect_status(404)
      end
      it "should fail to delete job as Data Scientist" do
        create_sparktour_job(@project, "demo_job_1", "jar", nil)
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
        create_sparktour_job(@project, $job_spark_1, "jar", nil)
        create_sparktour_job(@project, $job_spark_2, "jar", nil)
        create_sparktour_job(@project, "demo_py_job_1", "py", nil)
        create_sparktour_job(@project, "demo_job_3", "jar", nil)
        create_sparktour_job(@project, "demo_ipynb_job_1", "ipynb", nil)
        create_sparktour_job(@project, "demo_job_4", "jar", nil)
        create_sparktour_job(@project, "demo_job_5", "jar", nil)
        create_sparktour_job(@project, "demo_py_job_2", "py", nil)
        create_sparktour_job(@project, "demo_ipynb_job_2", "ipynb", nil)
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
      describe "Jobs filter, limit" do
        it "should return limit=x jobs" do
          #sort in memory and compare with query
          get_jobs(@project[:id], nil)
          jobs = json_body[:items].map {|job| job[:name]}
          sorted = jobs.sort_by(&:downcase)
          get_jobs(@project[:id], "?limit=2&sort_by=name:asc")
          sorted_res = json_body[:items].map {|job| job[:name]}
          expect(sorted_res).to eq(sorted.take(2))
        end
        it "should return jobs starting from offset=y" do
          #sort in memory and compare with query
          get_jobs(@project[:id], nil)
          jobs = json_body[:items].map {|job| job[:name]}
          sorted = jobs.sort_by(&:downcase)
          get_jobs(@project[:id], "?offset=1&sort_by=name:asc")
          sorted_res = json_body[:items].map {|job| job[:name]}
          expect(sorted_res).to eq(sorted.drop(1))
        end
        it "should return limit=x jobs with offset=y" do
          #sort in memory and compare with query
          get_jobs(@project[:id], nil)
          jobs = json_body[:items].map {|job| job[:name]}
          sorted = jobs.sort_by(&:downcase)
          get_jobs(@project[:id], "?offset=1&limit=2&sort_by=name:asc")
          sorted_res = json_body[:items].map {|job| job[:name]}
          expect(sorted_res).to eq(sorted.drop(1).take(2))
        end
        it "should ignore if limit < 0" do
          #sort in memory and compare with query
          get_jobs(@project[:id], nil)
          jobs = json_body[:items].map {|job| job[:name]}
          sorted = jobs.sort_by(&:downcase)
          get_jobs(@project[:id], "?limit<0&sort_by=name:asc")
          sorted_res = json_body[:items].map {|job| job[:name]}
          expect(sorted_res).to eq(sorted)
        end
        it "should ignore if offset < 0" do
          #sort in memory and compare with query
          get_jobs(@project[:id], nil)
          jobs = json_body[:items].map {|job| job[:name]}
          sorted = jobs.sort_by(&:downcase)
          get_jobs(@project[:id], "?offset<0&sort_by=name:asc")
          sorted_res = json_body[:items].map {|job| job[:name]}
          expect(sorted_res).to eq(sorted)
        end
        it "should ignore if limit = 0" do
          #sort in memory and compare with query
          get_jobs(@project[:id], nil)
          jobs = json_body[:items].map {|job| job[:name]}
          sorted = jobs.sort_by(&:downcase)
          get_jobs(@project[:id], "?limit=0&sort_by=name:asc")
          sorted_res = json_body[:items].map {|job| job[:name]}
          expect(sorted_res).to eq(sorted)
        end
        it "should ignore if offset = 0" do
          #sort in memory and compare with query
          get_jobs(@project[:id], nil)
          jobs = json_body[:items].map {|job| job[:name]}
          sorted = jobs.sort_by(&:downcase)
          get_jobs(@project[:id], "?offset=0&sort_by=name:asc")
          sorted_res = json_body[:items].map {|job| job[:name]}
          expect(sorted_res).to eq(sorted)
        end
        it "should ignore if limit = 0 and offset = 0" do
          #sort in memory and compare with query
          get_jobs(@project[:id], nil)
          jobs = json_body[:items].map {|job| job[:name]}
          sorted = jobs.sort_by(&:downcase)
          get_jobs(@project[:id], "?limit=0&offset=0&sort_by=name:asc")
          sorted_res = json_body[:items].map {|job| job[:name]}
          expect(sorted_res).to eq(sorted)
        end
      end
      describe "Jobs filter" do
        it "should get five jobs with type spark" do
          get_jobs(@project[:id], "?filter_by=jobtype:spark")
          expect_status(200)
          expect(json_body[:items].count).to eq 5
        end
        it "should get four jobs with type pyspark" do
          get_jobs(@project[:id], "?filter_by=jobtype:pyspark")
          expect_status(200)
          expect(json_body[:items].count).to eq 4
        end
        it "should fail to find job with type flink" do
          get_jobs(@project[:id], "?filter_by=jobtype:flink")
          expect_status(200)
          expect(json_body[:items]).to be nil
        end
        it "should find jobs with name like 'demo'" do
          get_jobs(@project[:id], "?filter_by=name:demo")
          expect_status(200)
          expect(json_body[:items].count).to eq 9
        end
        it "should find jobs with name like 'demo_job'" do
          get_jobs(@project[:id], "?filter_by=name:demo")
          expect_status(200)
          expect(json_body[:items].count).to eq 9
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
