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
  describe 'hops-util-py' do
    before :all do
      with_valid_project
      project = get_project
      user = get_user
      with_it_test_artifacts(project, user)
    end
    after (:all) {clean_projects}

    describe "run notebook that tests distributed training, hyperparameter search, feature store API, kafka utils
              and TLS utils in hops-util-py using Python 3.6 environment" do

      context 'with valid project, featurestore service, python 3.6 enabled, and sample data in /Resources' do
        before :all do
          project = get_project
          with_python_enabled(project.id, "3.6")
          with_it_test_job(project, get_python_it_tests_job_name("3.6"),
                           get_python_it_tests_project_dir(project.projectname) + get_python_it_tests_notebook_name, nil)
        end

        it "should be able to run the python tests" do
          project = get_project
          # Start job
          start_execution(project.id, get_python_it_tests_job_name("3.6"), nil)
          expect_status(201)
          execution_id = json_body[:id]

          # Wait for execution to complete
          wait_for_execution(4000) do
            get_execution(project.id, get_python_it_tests_job_name("3.6"), execution_id)
            execution_dto = JSON.parse(response.body)
            not is_execution_active(execution_dto)
          end

          # Check that the execution completed successfully
          get_execution(project.id, get_python_it_tests_job_name("3.6"), execution_id)
          execution_dto = JSON.parse(response.body)
          expect(execution_dto["state"] == "FINISHED").to be true
          expect(execution_dto["finalStatus"] == "SUCCEEDED").to be true

        end
      end
    end

      describe "run notebook that tests distributed training, hyperparameter search, feature store API, kafka utils
              and TLS utils in hops-util-py using Python 2.7 environment" do

        context 'with valid project, featurestore service, python 2.7 enabled, and sample data in /Resources' do
          before :all do
            project = get_project
            with_python_enabled(project.id, "2.7")
            with_it_test_job(project, get_python_it_tests_job_name("2.7"),
                             get_python_it_tests_project_dir(project.projectname) + get_python_it_tests_notebook_name, nil)
          end

          it "should be able to run the python tests" do
            project = get_project

            # Start job
            start_execution(project.id, get_python_it_tests_job_name("2.7"), nil)
            expect_status(201)
            execution_id = json_body[:id]

            # Wait for execution to complete
            wait_for_execution(4000) do
              get_execution(project.id, get_python_it_tests_job_name("2.7"), execution_id)
              execution_dto = JSON.parse(response.body)
              not is_execution_active(execution_dto)
            end

            # Check that the execution completed successfully
            get_execution(project.id, get_python_it_tests_job_name("2.7"), execution_id)
            execution_dto = JSON.parse(response.body)
            expect(execution_dto["state"] == "FINISHED").to be true
            expect(execution_dto["finalStatus"] == "SUCCEEDED").to be true

          end
        end
      end
  end
end
