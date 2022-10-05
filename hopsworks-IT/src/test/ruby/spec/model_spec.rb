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
    @cleanup = true
  end
  after(:all) {clean_all_test_projects(spec: "model") if @cleanup}
  experiment_1 = "experiment_1"
  experiment_2 = "experiment_2"
  describe 'model' do
    context 'without authentication' do
      before :all do
        with_valid_project
        reset_session
      end
      it "should fail" do
        get_model(@project[:id], "mnist_1")
        expect_json(errorCode: 200003)
        expect_status_details(401)
      end
    end
    context 'with authentication create, get' do
      before :all do
        with_valid_tour_project("ml")
      end
      it "should not find any models" do
        get_models(@project[:id], nil)
        expect_status_details(200)
        expect(json_body[:items]).to be nil
        expect(json_body[:count]).to eq 0
        expect(URI(json_body[:href]).path).to eq "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/modelregistries/#{@project[:id]}/models"
      end
      it "should not find specific model" do
        get_model(@project[:id], "app_id_4254316623_1")
        expect_status_details(404)
      end
      it "should produce 6 models and check items, count and href" do
        create_model_job(@project, experiment_1, "export_model.ipynb")
        run_experiment_blocking(@project, experiment_1)

        wait_result = epipe_wait_on_provenance(repeat: 5)
        expect(wait_result["success"]).to be(true), wait_result["msg"]

        get_models(@project[:id], nil)
        expect_status_details(200)
        expect(json_body[:items].count).to eq 6
        expect(json_body[:count]).to eq 6
        json_body[:items].each {|model| expect(URI(model[:href]).path).to eq "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/modelregistries/#{@project[:id]}/models/#{model[:id]}"}
      end
      it "run hsml integration tests" do
        create_model_job(@project, experiment_2, "test_hsml.ipynb")
        run_experiment_blocking(@project, experiment_2)
      end
      it "should get all existing models" do
        get_models(@project[:id], nil)
        json_body[:items].each {|model|
        get_model(@project[:id], model[:id])
        expect_status_details(200)}
      end
      it "should delete specific model" do
        get_models(@project[:id], nil)
        model = json_body[:items][0]

        delete_model(@project[:id], model[:id])
        expect_status_details(204)

        wait_result = epipe_wait_on_provenance(repeat: 5)
        expect(wait_result["success"]).to be(true), wait_result["msg"]

        get_model(@project[:id], model[:id])
        expect_status_details(404)
      end
      it "retrieving models from project with no Models dataset should fail" do
        delete "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/Projects/#{@project[:projectname]}/Models"
        expect_status_details(204)
        wait_result = epipe_wait_on_provenance(repeat: 5)
        expect(wait_result["success"]).to be(true), wait_result["msg"]

        get_models(@project[:id], nil)
        expect_status_details(500)
        expect(json_body[:errorCode]).to eq(360008)
      end
    end
  end
  describe 'models sort, filter, offset and limit' do
    context 'with authentication' do
      before :all do
        with_valid_tour_project("ml")
        create_model_job(@project, experiment_1, "export_model.ipynb")
        run_experiment_blocking(@project, experiment_1)
      end
      describe "Models sort" do
        it "should get all models sorted by name ascending" do
          #sort in memory and compare with query
          get_models(@project[:id], nil)
          models = json_body[:items].map {|model| model[:name]}
          sorted = models.sort_by(&:downcase)
          get_models(@project[:id], "?sort_by=name:asc")
          sorted_res = json_body[:items].map {|model| model[:name]}
          expect(sorted_res).to eq(sorted)
        end
        it "should get all models sorted by name descending" do
          #sort in memory and compare with query
          get_models(@project[:id], nil)
          models = json_body[:items].map {|model| model[:name]}
          sorted = models.sort_by(&:downcase).reverse
          get_models(@project[:id], "?sort_by=name:desc")
          sorted_res = json_body[:items].map {|model| model[:name]}
          expect(sorted_res).to eq(sorted)
        end
      end
      describe "Models limit, offset, sort" do
        it "should return limit=x models" do
          #sort in memory and compare with query
          get_models(@project[:id], nil)
          models = json_body[:items].map {|model| model[:name]}
          sorted = models.sort_by(&:downcase)
          get_models(@project[:id], "?limit=2&sort_by=name:asc")
          sorted_res = json_body[:items].map {|model| model[:name]}
          expect(sorted_res).to eq(sorted.take(2))
        end
        it "should return models starting from offset=y" do
          #sort in memory and compare with query
          get_models(@project[:id], nil)
          models = json_body[:items].map {|model| model[:name]}
          sorted = models.sort_by(&:downcase)
          get_models(@project[:id], "?offset=1&sort_by=name:asc")
          sorted_res = json_body[:items].map {|model| model[:name]}
          expect(sorted_res).to eq(sorted.drop(1))
        end
        it "should return limit=x models with offset=y" do
          #sort in memory and compare with query
          get_models(@project[:id], nil)
          models = json_body[:items].map {|model| model[:name]}
          sorted = models.sort_by(&:downcase)
          get_models(@project[:id], "?offset=1&limit=2&sort_by=name:asc")
          sorted_res = json_body[:items].map {|model| model[:name]}
          expect(sorted_res).to eq(sorted.drop(1).take(2))
        end
        it "should ignore if limit < 0" do
          #sort in memory and compare with query
          get_models(@project[:id], nil)
          models = json_body[:items].map {|model| model[:name]}
          sorted = models.sort_by(&:downcase)
          get_models(@project[:id], "?limit<0&sort_by=name:asc")
          sorted_res = json_body[:items].map {|model| model[:name]}
          expect(sorted_res).to eq(sorted)
        end
        it "should ignore if offset < 0" do
          #sort in memory and compare with query
          get_models(@project[:id], nil)
          models = json_body[:items].map {|model| model[:name]}
          sorted = models.sort_by(&:downcase)
          get_models(@project[:id], "?offset<0&sort_by=name:asc")
          sorted_res = json_body[:items].map {|model| model[:name]}
          expect(sorted_res).to eq(sorted)
        end
        it "should ignore if limit = 0" do
          #sort in memory and compare with query
          get_models(@project[:id], nil)
          models = json_body[:items].map {|model| model[:name]}
          sorted = models.sort_by(&:downcase)
          get_models(@project[:id], "?limit=0&sort_by=name:asc")
          sorted_res = json_body[:items].map {|model| model[:name]}
          expect(sorted_res).to eq(sorted)
        end
        it "should ignore if offset = 0" do
          #sort in memory and compare with query
          get_models(@project[:id], nil)
          models = json_body[:items].map {|model| model[:name]}
          sorted = models.sort_by(&:downcase)
          get_models(@project[:id], "?offset=0&sort_by=name:asc")
          sorted_res = json_body[:items].map {|model| model[:name]}
          expect(sorted_res).to eq(sorted)
        end
        it "should ignore if limit = 0 and offset = 0" do
          #sort in memory and compare with query
          get_models(@project[:id], nil)
          models = json_body[:items].map {|model| model[:name]}
          sorted = models.sort_by(&:downcase)
          get_models(@project[:id], "?limit=0&offset=0&sort_by=name:asc")
          sorted_res = json_body[:items].map {|model| model[:name]}
          expect(sorted_res).to eq(sorted)
        end
      end
      describe "Models filter" do
        it "should get 0 model with name_eq resnet" do
          get_models(@project[:id], "?filter_by=name_eq:resnet")
          expect_status_details(200)
          expect(json_body[:items]).to be nil
        end
        it "should get 0 model with name_like resnet" do
          get_models(@project[:id], "?filter_by=name_like:resnet")
          expect_status_details(200)
          expect(json_body[:items]).to be nil
        end
        it "should get 3 TensorFlow models with name_eq model_tf" do
          get_models(@project[:id], "?filter_by=name_eq:model_tf")
          expect_status_details(200)
          expect(json_body[:items].count).to eq 3
          json_body[:items].each {|model| expect(model[:framework]).to eq "TENSORFLOW"}
        end
        it "should get 1 Python model with name_eq model_python" do
          get_models(@project[:id], "?filter_by=name_eq:model_python")
          expect_status_details(200)
          expect(json_body[:items].count).to eq 1
          json_body[:items].each {|model| expect(model[:framework]).to eq "PYTHON"}
        end
        it "should get 1 Sklearn model with name_eq model_sklearn" do
          get_models(@project[:id], "?filter_by=name_eq:model_sklearn")
          expect_status_details(200)
          expect(json_body[:items].count).to eq 1
          json_body[:items].each {|model| expect(model[:framework]).to eq "SKLEARN"}
        end
        it "should get 1 Torch model with name_eq model_torch" do
          get_models(@project[:id], "?filter_by=name_eq:model_torch")
          expect_status_details(200)
          expect(json_body[:items].count).to eq 1
          json_body[:items].each {|model| expect(model[:framework]).to eq "TORCH"}
        end
        it "should get 6 model with name_like model" do
          get_models(@project[:id], "?filter_by=name_like:model")
          expect_status_details(200)
          expect(json_body[:items].count).to eq 6
        end
        it "should get 3 model with name_like model_t" do
          get_models(@project[:id], "?filter_by=name_like:model_t")
          expect_status_details(200)
          expect(json_body[:items].count).to eq 4
        end
      end
    end
  end

  describe 'shared' do
    before :all do
      setup
      setup_shared
      # setup_debug
      # setup_shared_debug
      epipe_wait_on_provenance

      @test_synth_model_job = "test_synth_model"
      @model_name_1 = "model_1"
    end

    def setup()
      @user1_params = {email: "user1_#{random_id}@email.com", first_name: "User", last_name: "1", password: "Pass123"}
      @user1 = create_user_with_role(@user1_params, "HOPS_ADMIN")
      pp "user email: #{@user1[:email]}" if defined?(@debugOpt) && @debugOpt

      create_session(@user1[:email], @user1_params[:password])
      @project1 = create_project
      pp @project1[:projectname] if defined?(@debugOpt) && @debugOpt
    end
    def setup_shared()
      @user2_params = {email: "user2_#{random_id}@email.com", first_name: "User", last_name: "2", password: "Pass123"}
      @user2 = create_user_with_role(@user2_params, "HOPS_ADMIN")
      pp "user email: #{@user2[:email]}" if defined?(@debugOpt) && @debugOpt

      create_session(@user2[:email], @user2_params[:password])
      @project2 = create_project
      pp @project2[:projectname] if defined?(@debugOpt) && @debugOpt

      create_session(@user1_params[:email], @user1_params[:password])
      share_dataset_checked(@project1, "Models", @project2[:projectname], datasetType: "DATASET")

      create_session(@user2_params[:email], @user2_params[:password])
      accept_dataset_checked(@project2, "#{@project1[:projectname]}::Models", datasetType: "DATASET")
      share_dataset_checked(@project2, "Models", @project1[:projectname], datasetType: "DATASET")

      create_session(@user1_params[:email], @user1_params[:password])
      accept_dataset_checked(@project1, "#{@project2[:projectname]}::Models", datasetType: "DATASET")
    end
    def setup_debug()
      @user1_params = {email: "user1_798d9d633133c3202679b0834bbccd41d44fcca1@email.com", first_name: "User", last_name: "1", password: "Pass123"}
      @user1 = get_user_by_mail(@user1_params[:email])

      create_session(@user1_params[:email], @user1_params[:password])
      @project1 = get_project_by_name("ProJect_8a439873")
    end
    def setup_shared_debug()
      @user2_params = {email: "user2_6a7d2e29a708f15a5febd74fc36edc6a091a25f4@email.com", first_name: "User", last_name: "2", password: "Pass123"}
      @user2 = get_user_by_mail(@user2_params[:email])

      create_session(@user2_params[:email], @user2_params[:password])
      @project2 = get_project_by_name("ProJect_28edf9c2")
    end

    it 'should setup model in shared models dataset' do
      create_session(@user2_params[:email], @user2_params[:password])
      if job_exists(@project2[:id], @test_synth_model_job)
        pp "job exists - skipping"
      else
        prepare_spark_job(@project2, @user2[:username], @test_synth_model_job, "py")
      end
      expect(job_exists(@project2[:id], @test_synth_model_job)).to be(true)

      create_session(@user1_params[:email], @user1_params[:password])
      if model_exists(@project1, @model_name_1)
        pp "model exists - skipping"
      else
        create_session(@user2_params[:email], @user2_params[:password])
        args = [@project1[:projectname], @model_name_1]
        run_job(@project2, @test_synth_model_job, args: args)
      end

      create_session(@user1_params[:email], @user1_params[:password])
      expect(model_exists(@project1, @model_name_1)).to be(true)
    end
  end
  describe 'model export' do
    def setup_user(email, password)
      if email.nil? || password.nil?
        email = "user_#{random_id}@email.com"
        password = "Pass123"
        user_params = { email: email, first_name: "User", last_name: "1", password: password }
        user = create_user_with_role(user_params, "HOPS_USER")
      else
        user = create_session(email, password)
      end
      return user, email, password
    end
    def setup_project(project_name, user_params, create: true)
      create_session(user_params[:email], user_params[:password])
      if create
        if project_name.nil?
          project = create_project
        else
          project = create_project_by_name(project_name)
        end

      else
        project = get_project_by_name(project_name)
      end
      return project
    end
    before :all do
      email = nil
      password = nil
      project_name = nil
      create = true

      @user1, @email1, @password1 = setup_user(email, password)
      @user1_params = {email: @email1, password: @password1, username: @user1[:username]}
      pp "user:#{@email1}" if defined?(@debugOpt) && @debugOpt
      @project1 = setup_project(project_name, @user1_params, create: create)
      pp "project:#{@project1[:projectname]}" if defined?(@debugOpt) && @debugOpt
    end
    after :all do
      clean_all_test_projects(spec: "ee_ml_pipeline") if @cleanup
    end
    def run_job_check_partials(user_params, project, job_name, job_type, file_type,
                               job_config, job_args: nil, &is_job_done)
      create_session(user_params[:email], user_params[:password])

      if job_exists(project[:id], job_name)
        pp "job exists - skipping" if defined?(@debugOpt) && @debugOpt
      else
        prepare_job(project, user_params[:username], job_name, job_type, file_type, job_config: job_config)
      end
      expect(job_exists(project[:id], job_name)).to be(true)
      if is_job_done.call()
        pp "all job outputs are created already - skipping" if defined?(@debugOpt) && @debugOpt
      else
        run_job(project, job_name, job_type: job_type, args: job_args)
        unless is_job_done.call()
          raise "job outputs are not all there - something went wrong"
        end
      end
    end

    it 'in pyspark' do
      job_name = "export_model_spark"
      file_type = "py"
      job_type = "SPARK"
      run_job_check_partials(@user1_params, @project1, job_name, job_type, file_type,
                             get_spark_default_py_config(@project1, job_name, file_type)) do
        model_exists(@project1, "mnist_spark", version: 1)
      end
    end
  end
end
