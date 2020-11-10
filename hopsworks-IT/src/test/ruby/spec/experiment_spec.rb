=begin
 This file is part of Hopsworks
 Copyright (C) 2019, Logical Clocks AB. All rights reserved

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
  before :all do
    @debugOpt = false
  end
  after(:all) {clean_all_test_projects(spec: "experiment")}
  experiment_1 = "experiment_1"
  experiment_2 = "experiment_2"
  describe 'experiment' do
    context 'without authentication' do
      before :all do
        with_valid_project
        reset_session
      end
      it "should fail" do
        get_experiment(@project[:id], "app_id_4252123_1", nil)
        expect_json(errorCode: 200003)
        expect_status(401)
      end
    end
    context 'with authentication create, delete, get' do
      before :all do
        with_valid_project

      end
      it "should not find any experiments" do
        get_experiments(@project[:id], nil)
        expect_status(200)
        expect(json_body[:items]).to be nil
        expect(json_body[:count]).to eq 0
        expect(URI(json_body[:href]).path).to eq "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/experiments"
      end
      it "should not find specific experiment" do
        get_experiment(@project[:id], "app_id_4254316623_1", nil)
        expect_status(404)
      end
      it "should run three experiments and check items, count and href" do
        create_experiment_job(@project, experiment_1)
        run_experiment_blocking(@project, experiment_1)
        get_experiments(@project[:id], nil)
        expect_status(200)
        expect(json_body[:items].count).to eq 3
        expect(json_body[:count]).to eq 3
        json_body[:items].each {|experiment| expect(URI(experiment[:state]).path).to eq "FINISHED"}
        json_body[:items].each {|experiment| expect(URI(experiment[:href]).path).to eq "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/experiments/#{experiment[:id]}"}
      end
      it "should get all existing experiments" do
        get_experiments(@project[:id], nil)
        json_body[:items].each {|experiment|
        get_experiment(@project[:id], experiment[:id], nil)
        expect_status(200)
        }
      end
      it "should delete a tensorboard for experiment" do
        get_experiments(@project[:id], nil)
        ml_id = json_body[:items][0][:id]

        create_tensorboard(@project[:id], ml_id)
        expect_status(201)

        get_tensorboard(@project[:id], ml_id)
        expect_status(200)

        delete_tensorboard(@project[:id], ml_id)
        expect_status(204)

        get_tensorboard(@project[:id], ml_id)
        expect_status(404)
      end
      it "should delete single experiment using experiment API" do
        get_experiments(@project[:id], nil)
        ml_id = json_body[:items][0][:id]
        delete_experiment(@project[:id], ml_id)
        expect_status(204)
        wait_result = epipe_wait_on_provenance(repeat: 5)
        expect(wait_result["success"]).to be(true), wait_result["msg"]

        get_experiments(@project[:id], nil)
        expect(json_body[:count]).to eq(2)
      end
      it "should delete single experiment by deleting ml_id folder" do
        get_experiments(@project[:id], nil)
        ml_id = json_body[:items][0][:id]
        delete "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/Projects/#{@project[:projectname]}/Experiments/#{ml_id}"
        expect_status(204)
        wait_result = epipe_wait_on_provenance(repeat: 5)
        expect(wait_result["success"]).to be(true), wait_result["msg"]

        get_experiments(@project[:id], nil)
        expect(json_body[:count]).to eq(1)
      end
      it "should delete experiment by deleting Experiments dataset" do
        delete "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/Projects/#{@project[:projectname]}/Experiments"
        expect_status(204)
        wait_result = epipe_wait_on_provenance(repeat: 5)
        expect(wait_result["success"]).to be(true), wait_result["msg"]

        get_experiments(@project[:id], nil)
        expect(json_body[:count]).to eq(0)
      end
    end
  end
  describe 'experiments sort, filter, offset and limit' do
    context 'with authentication' do
      before :all do
        with_valid_project

        create_experiment_job(@project, experiment_1)
        run_experiment_blocking(@project, experiment_1)

        create_experiment_opt_job(@project, experiment_2)
        run_experiment_blocking(@project, experiment_2)
      end
      describe "Experiment sort" do
        it "should get all experiments sorted by name ascending" do
          #sort in memory and compare with query
          get_experiments(@project[:id], nil)
          experiments = json_body[:items].map {|experiment| experiment[:name]}
          sorted = experiments.sort_by(&:downcase)
          get_experiments(@project[:id], "?sort_by=name:asc")
          sorted_res = json_body[:items].map {|experiment| experiment[:name]}
          expect(sorted_res).to eq(sorted)
        end
        it "should get all experiments sorted by name descending" do
          #sort in memory and compare with query
          get_experiments(@project[:id], nil)
          experiments = json_body[:items].map {|experiment| experiment[:name]}
          sorted = experiments.sort_by(&:downcase).reverse
          get_experiments(@project[:id], "?sort_by=name:desc")
          sorted_res = json_body[:items].map {|experiment| experiment[:name]}
          expect(sorted_res).to eq(sorted)
        end
        it "should get all experiments sorted by date created ascending" do
          #sort in memory and compare with query
          get_experiments(@project[:id], nil)
          experiments = json_body[:items].map {|experiment| experiment[:started]}
          sorted = experiments.sort
          get_experiments(@project[:id], "?sort_by=start:asc")
          sorted_res = json_body[:items].map {|experiment| experiment[:started]}
          expect(sorted_res).to eq(sorted)
        end
        it "should get all experiments sorted by date created descending" do
          #sort in memory and compare with query
          get_experiments(@project[:id], nil)
          experiments = json_body[:items].map {|experiment| experiment[:started]}
          sorted = experiments.sort.reverse
          get_experiments(@project[:id], "?sort_by=start:desc")
          sorted_res = json_body[:items].map {|experiment| experiment[:started]}
          expect(sorted_res).to eq(sorted)
        end
        it "should get all experiments sorted by date created ascending" do
          #sort in memory and compare with query
          get_experiments(@project[:id], nil)
          experiments = json_body[:items].map {|experiment| experiment[:finished]}
          sorted = experiments.sort
          get_experiments(@project[:id], "?sort_by=end:asc")
          sorted_res = json_body[:items].map {|experiment| experiment[:finished]}
          expect(sorted_res).to eq(sorted)
        end
        it "should get all experiments sorted by date created descending" do
          #sort in memory and compare with query
          get_experiments(@project[:id], nil)
          experiments = json_body[:items].map {|experiment| experiment[:finished]}
          sorted = experiments.sort.reverse
          get_experiments(@project[:id], "?sort_by=end:desc")
          sorted_res = json_body[:items].map {|experiment| experiment[:finished]}
          expect(sorted_res).to eq(sorted)
        end
      end
      describe "Experiments limit, offset, sort" do
        it "should return limit=x experiments" do
          #sort in memory and compare with query
          get_experiments(@project[:id], nil)
          experiments = json_body[:items].map {|experiment| experiment[:name]}
          sorted = experiments.sort_by(&:downcase)
          get_experiments(@project[:id], "?limit=2&sort_by=name:asc")
          sorted_res = json_body[:items].map {|experiment| experiment[:name]}
          expect(sorted_res).to eq(sorted.take(2))
        end
        it "should return experiments starting from offset=y" do
          #sort in memory and compare with query
          get_experiments(@project[:id], nil)
          experiments = json_body[:items].map {|experiment| experiment[:name]}
          sorted = experiments.sort_by(&:downcase)
          get_experiments(@project[:id], "?offset=1&sort_by=name:asc")
          sorted_res = json_body[:items].map {|experiment| experiment[:name]}
          expect(sorted_res).to eq(sorted.drop(1))
        end
        it "should return limit=x experiments with offset=y" do
          #sort in memory and compare with query
          get_experiments(@project[:id], nil)
          experiments = json_body[:items].map {|experiment| experiment[:name]}
          sorted = experiments.sort_by(&:downcase)
          get_experiments(@project[:id], "?offset=1&limit=2&sort_by=name:asc")
          sorted_res = json_body[:items].map {|experiment| experiment[:name]}
          expect(sorted_res).to eq(sorted.drop(1).take(2))
        end
        it "should ignore if limit < 0" do
          #sort in memory and compare with query
          get_experiments(@project[:id], nil)
          experiments = json_body[:items].map {|experiment| experiment[:name]}
          sorted = experiments.sort_by(&:downcase)
          get_experiments(@project[:id], "?limit<0&sort_by=name:asc")
          sorted_res = json_body[:items].map {|experiment| experiment[:name]}
          expect(sorted_res).to eq(sorted)
        end
        it "should ignore if offset < 0" do
          #sort in memory and compare with query
          get_experiments(@project[:id], nil)
          experiments = json_body[:items].map {|experiment| experiment[:name]}
          sorted = experiments.sort_by(&:downcase)
          get_experiments(@project[:id], "?offset<0&sort_by=name:asc")
          sorted_res = json_body[:items].map {|experiment| experiment[:name]}
          expect(sorted_res).to eq(sorted)
        end
        it "should ignore if limit = 0" do
          #sort in memory and compare with query
          get_experiments(@project[:id], nil)
          experiments = json_body[:items].map {|experiment| experiment[:name]}
          sorted = experiments.sort_by(&:downcase)
          get_experiments(@project[:id], "?limit=0&sort_by=name:asc")
          sorted_res = json_body[:items].map {|experiment| experiment[:name]}
          expect(sorted_res).to eq(sorted)
        end
        it "should ignore if offset = 0" do
          #sort in memory and compare with query
          get_experiments(@project[:id], nil)
          experiments = json_body[:items].map {|experiment| experiment[:name]}
          sorted = experiments.sort_by(&:downcase)
          get_experiments(@project[:id], "?offset=0&sort_by=name:asc")
          sorted_res = json_body[:items].map {|experiment| experiment[:name]}
          expect(sorted_res).to eq(sorted)
        end
        it "should ignore if limit = 0 and offset = 0" do
          #sort in memory and compare with query
          get_experiments(@project[:id], nil)
          experiments = json_body[:items].map {|experiment| experiment[:name]}
          sorted = experiments.sort_by(&:downcase)
          get_experiments(@project[:id], "?limit=0&offset=0&sort_by=name:asc")
          sorted_res = json_body[:items].map {|experiment| experiment[:name]}
          expect(sorted_res).to eq(sorted)
        end
      end
      describe "Experiments filter" do
        it "should get 3 experiments with name like experiment" do
          get_experiments(@project[:id], "?filter_by=name_like:mnist")
          expect_status(200)
          expect(json_body[:count]).to eq 3
          expect(json_body[:items].count).to eq 3
        end
        it "should get 0 experiment with name like not_existing" do
          get_experiments(@project[:id], "?filter_by=name_like:not_existing")
          expect_status(200)
          expect(json_body[:count]).to eq 0
          expect(json_body[:items]).to eq nil
        end
        it "should get 3 experiments with state FINISHED experiment" do
          get_experiments(@project[:id], "?filter_by=state:FINISHED")
          expect_status(200)
          expect(json_body[:count]).to eq 4
          expect(json_body[:items].count).to eq 4
        end
        it "should get 0 experiments with state FAILED experiment" do
          get_experiments(@project[:id], "?filter_by=state:FAILED")
          expect_status(200)
          expect(json_body[:count]).to eq 0
          expect(json_body[:items]).to eq nil
        end
        it "should get 0 experiments with state RUNNING experiment" do
          get_experiments(@project[:id], "?filter_by=state:RUNNING")
          expect_status(200)
          expect(json_body[:count]).to eq 0
          expect(json_body[:items]).to eq nil
        end
        it "should get 3 experiments with state FINISHED and name like experiment_ experiment" do
          get_experiments(@project[:id], "?filter_by=state:FINISHED&filter_by=name_like:mnist")
          expect_status(200)
          expect(json_body[:count]).to eq 3
          expect(json_body[:items].count).to eq 3
        end
      end
      describe "Hyperparameters" do
        it "should expand hyperparameters" do
          get_experiments(@project[:id], "?filter_by=name_eq:opt")
          expect_status(200)
          expect(json_body[:count]).to eq 1
          expect(json_body[:items].count).to eq 1
          ml_id = json_body[:items][0][:id]
          get_results(@project[:id], ml_id, "?expand=results")
        end
      end
    end
  end
  describe 'fake experiment' do
    def experiment_internal(project_id, app_id, run_id, type, body)
      exp_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/experiments/#{app_id}_#{run_id}?type=#{type}"
      pp "put #{exp_endpoint}, #{body}" if defined?(@debugOpt) && @debugOpt
      put exp_endpoint, body
    end
    def wait_for_experiment(project, app_id, run_id, &check_experiment_func)
      epipe_wait_on_provenance
      result = wait_for_me_time(10) do
        ex_id = "#{app_id}_#{run_id}"
        experiment = get_experiment(project[:id], ex_id, nil)
        terminate = response.code == resolve_status(200, response.code) || (response.code == resolve_status(404, response.code) && json_body[:errorCode] == 360000)
        begin
          check_experiment_func.call(experiment)
          return { 'success' => terminate, 'msg' => "wait for experiment in elastic", 'response_code' => response.code,
            'error_code' => json_body[:errorCode], 'experiment' => experiment}
        rescue => e
          { 'success' => false, 'msg' => "wait for experiment in elastic", 'response_code' => response.code,
            'error_code' => json_body[:errorCode], 'exception' => e}
        end
      end
      raise e if result["exception"].nil
      expect(result["response_code"]).to eq(200)
      result["experiment"]
    end
    def check_experiment_init(experiment)
      pp JSON.parse(experiment) if defined?(@debugOpt) && @debugOpt
      expect(experiment["id"]).not_to be_nil
      expect(experiment["appId"]).not_to be_nil
      expect(experiment["environment"]).not_to be_nil
      expect(experiment["experimentType"]).not_to be_nil
      expect(experiment["function"]).not_to be_nil
      expect(experiment["jobName"]).not_to be_nil
      expect(experiment["name"]).not_to be_nil
      expect(experiment["started"]).not_to be_nil
      expect(experiment["state"]).not_to be_nil
      expect(experiment["tensorboard"]).not_to be_nil
      expect(experiment["userFullName"]).not_to be_nil
    end
    def check_experiment_model(experiment)
      check_experiment_init(experiment)
      expect(experiment["model"]).not_to be_nil
      expect(experiment["modelProjectName"]).not_to be_nil
    end
    def check_experiment_final(experiment)
      check_experiment_model(experiment)
      expect(experiment["finished"]).not_to be_nil
      expect(experiment["metric"]).not_to be_nil
      expect(experiment["program"]).not_to be_nil
    end
    it 'create' do
      project = create_project
      app_id = "application_1603965705215_0032"
      model = "fake_model"
      version = 1
      run_id = 1

      dirname = "Experiments/#{app_id}_#{run_id}"
      create_dir(project, dirname, query: "&type=DATASET")
      expect_status_details(201)

      experiment_dto_1 = {
          "id": "#{app_id}_#{run_id}",
          "state": "RUNNING",
          "name": "#{model}_experiment",
          "projectName": project[:projectname],
          "function": "launch",
          "experimentType": "EXPERIMENT",
          "jobName": "create_#{model}",
          "appId": app_id
      }
      experiment_internal(project[:id], app_id, run_id, "INIT", experiment_dto_1)
      expect_status_details(201)
      wait_for_experiment(project, app_id, run_id, &method(:check_experiment_init))

      experiment_dto_2 = {
          "id": "#{app_id}_#{run_id}",
          "model": "#{model}_#{version}",
          "modelProjectName": project[:projectname]
      }
      experiment_internal(project[:id], app_id, run_id, "MODEL_UPDATE", experiment_dto_2)
      expect_status_details(200)
      wait_for_experiment(project, app_id, run_id, &method(:check_experiment_model))

      experiment_dto_3 = {
          "id": "#{app_id}_#{run_id}",
          "state": "FINISHED",
          "name": "#{model}_experiment",
          "projectName": project[:projectname],
          "metric": 0.06,
          "userFullName": "User 1",
          "function": "launch",
          "experimentType": "EXPERIMENT",
          "jobName": "create_#{model}",
          "duration": 376663,
          "appId": app_id,
          "environment": "Experiments/#{app_id}/environment_16002310123.yml",
          "program": "Experiments/#{app_id}_#{run_id}/program.py"
      }
      experiment_internal(project[:id], app_id, run_id, "FULL_UPDATE", experiment_dto_3)
      expect_status_details(200)
      wait_for_experiment(project, app_id, run_id, &method(:check_experiment_final))
    end
  end
end
