=begin
 This file is part of Hopsworks
 Copyright (C) 2020, Logical Clocks AB. All rights reserved

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
  describe 'admin variables' do
    before :all do
      with_admin_session
    end

    it 'should succeed to fetch the value of a variable' do
      result = get "#{ENV['HOPSWORKS_API']}/variables/hopsworks_user"
      expect_status_details(200)
      expect(JSON.parse(result)['successMessage']).to eql("glassfish")
    end

    it 'should succeed to fetch a variable with admin visibility' do
      get "#{ENV['HOPSWORKS_API']}/variables/hopsworks_master_password"
      expect_status_details(200)
    end

    it 'should receive a 404 if the variable does not exists' do
      get "#{ENV['HOPSWORKS_API']}/variables/doesnotexists"
      expect_status_details(404)
    end
  end

  describe 'with user session' do
    before :all do
      with_valid_session
    end

    it 'should be able to fetch a variable with notauthenticated visibility' do
      get "#{ENV['HOPSWORKS_API']}/variables/first_time_login"
      expect_status_details(200)
    end

    it 'should be able to fetch a variable with user visibility' do
      get "#{ENV['HOPSWORKS_API']}/variables/hopsworks_enterprise"
      expect_status_details(200)
    end

    it 'should fail to fetch a variable with admin visibility' do
      get "#{ENV['HOPSWORKS_API']}/variables/hopsworks_master_password"
      expect_status_details(403)
    end
  end

  describe 'without authentication' do
    before :all do
      reset_session
    end

    it 'should fail to fetch a variable' do
      get "#{ENV['HOPSWORKS_API']}/variables/hopsworks_master_password"
      expect_status_details(401)
    end
  end

  describe 'with variable' do
    describe 'quotas_max_parallel_executions' do
      context "enabled" do
        before :all do
          setVar("quotas_max_parallel_executions", "1")
          @cookies_pe = with_admin_session
          with_valid_tour_project("spark")
        end
        after :all do
          setVar("quotas_max_parallel_executions", "-1")
          @cookies_pe = nil
        end
        it "should not launch more than configured max parallel executions" do
          create_sparktour_job(@project, "max_parallel_exec", "jar")
          start_execution(@project[:id], "max_parallel_exec")

          # reached limit
          resp = start_execution(@project[:id], "max_parallel_exec", expected_status: 400)
          parsed = JSON.parse(resp)
          expect(parsed['usrMsg']).to include("quota")
        end
      end
    end

    describe 'enable_data_science_profile' do
      context 'disabled' do
        before :all do
          # disable data science profile
          setVar('enable_data_science_profile', "false")
          with_valid_project
        end

        after :all do
          setVar('enable_data_science_profile', "true")
        end

        it "should fail to get models" do
          get_models(@project[:id], nil)
          expect_status_details(400, error_code: 120012)
          get_model(@project[:id], "mnist_1")
          expect_status_details(400, error_code: 120012)
        end

        it "should fail to get model registries" do
          get_model_registries(@project[:id], nil)
          expect_status_details(400, error_code: 120012)
        end

        it "should fail to get experiments" do
          get_experiment(@project[:id], "app_id_4252123_1", nil)
          expect_status_details(400, error_code: 120012)
        end

        it "should fail to get servings" do
          get_servings(@project, nil)
          expect_status_details(400, error_code: 120012)
        end
      end
    end

    describe 'enable_conda_install' do
      context 'disabled' do
        before do
          @enable_conda_install = getVar('enable_conda_install')
          setVar('enable_conda_install', false)
          with_valid_project
        end
        after do
          setVar('enable_conda_install', @enable_conda_install.value)
        end
        it 'should fail to install library via conda' do
          project = create_env_and_update_project(@project, ENV['PYTHON_VERSION'])
          data = {
            "packageSource": 'CONDA',
            "channelUrl": 'conda-forge'
          }
          post "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/python/environments/#{ENV['PYTHON_VERSION']}/libraries/#{'r'}",
               data
          expect_status_details(403)
        end
      end
    end

    describe 'quotas_model_deployments_total' do
      context "enabled" do
        before :all do
          setVar("quotas_model_deployments_total", "1")
          @local_project = create_project
        end
        after :all do
          setVar("quotas_model_deployments_total", "-1")
          purge_all_tf_serving_instances
          delete_all_servings(@local_project.id)
        end
        it "should fail to create serving if quota has been reached" do
          ## This deployument should go through
          with_tensorflow_serving(@local_project.id, @local_project.projectname, @user.username)

          ## Second deployment should fail because quota has been reached
          create_tensorflow_serving(@local_project.id, @local_project.projectname, expected_status: 400)
        end
      end
    end
    describe "allowed maximum number of executions per job" do
      before :all do
        @exec_per_job = getVar('executions_per_job_limit')
        setVar('executions_per_job_limit', 2)
        with_valid_tour_project("spark")
      end
      after :all do
        setVar('executions_per_job_limit', @exec_per_job [:value])
        clean_jobs(@project[:id])
      end
      it "should not start more than the allowed maximum number of executions per job" do
        job_name = "demo_job_3"
        create_sparktour_job(@project, job_name, 'jar')
        begin
          start_execution(@project[:id], job_name)
          execution_id1 = json_body[:id]
          start_execution(@project[:id], job_name)
          execution_id2 = json_body[:id]
          start_execution(@project[:id], job_name, expected_status: 400, error_code: 130040)
        ensure
          wait_for_execution_completed(@project[:id], job_name, execution_id1, "FINISHED") unless execution_id1.nil?
          wait_for_execution_completed(@project[:id], job_name, execution_id2, "FINISHED") unless execution_id2.nil?
        end
      end
    end
    describe "with quota enabled" do
      before :all do
        setVar("quotas_model_deployments_running", "1")
        @local_project = create_project
        with_tensorflow_serving(@local_project.id, @local_project.projectname, @user.username)
      end
      after :all do
        setVar("quotas_model_deployments_running", "-1")
        purge_all_tf_serving_instances
        delete_all_servings(@local_project.id)
      end
      it "should fail to start serving if quota has been reached" do
        ## This deployment should start
        start_serving(@local_project, @serving)

        second_serving = create_tensorflow_serving(@local_project.id, @local_project.projectname)
        ## Starting this one should fail because quota has beed reached
        post "#{ENV['HOPSWORKS_API']}/project/#{@local_project.id}/serving/#{second_serving.id}?action=start"
        expect_status_details(400)
        parsed = JSON.parse(response)
        expect(parsed['devMsg']).to include("quota")
      end
    end
  end
end
