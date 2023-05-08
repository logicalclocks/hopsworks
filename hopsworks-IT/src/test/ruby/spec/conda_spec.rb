=begin
 Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 are released under the following license:
 This file is part of Hopsworks
 Copyright (C) 2022, Logical Clocks AB. All rights reserved
 Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 the GNU Affero General Public License as published by the Free Software Foundation,
 either version 3 of the License, or (at your option) any later version.
 Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 PURPOSE.  See the GNU Affero General Public License for more details.
 You should have received a copy of the GNU Affero General Public License along with this program.
 If not, see <https://www.gnu.org/licenses/>.
 Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 are released under the following license:
 Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
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
  before :all do
    @debugOpt = false

    # Enable the reporting
    RSpec.configure do |c|
      c.example_status_persistence_file_path  = 'conda_debug.txt' if @debugOpt
    end
  end

  let(:num_hosts) {Host.count}
  let(:conda_channel) {Variables.find_by(id: "conda_default_repo").value}

  after(:all) {clean_all_test_projects(spec: "conda")}
  describe '#Conda core' do
    context 'with authentication' do
      before :all do
        with_valid_project
        wait_for_running_command(@project[:id])
      end
      context 'conda enabled' do
        before do
          if not conda_exists(ENV['PYTHON_VERSION'])
            skip "Anaconda is not installed in the machine or test is run locally"
          end
        end
        after :each do
          begin
            wait_for_running_command(@project[:id], timeout=10)
          rescue RuntimeError => e
            delete_env(@project[:id], ENV['PYTHON_VERSION'])
            @project = create_env_and_update_project(@project, ENV['PYTHON_VERSION'])
            wait_for_running_command(@project[:id])
          end
        end
        context 'environment' do
          it 'enable anaconda' do
            expect(CondaCommands.find_by(project_id: @project[:id])).to be_nil
            @project = create_env_and_update_project(@project, ENV['PYTHON_VERSION'])

            # Enabling anaconda will not create an environment yet
            expect(check_if_img_exists_locally(@project[:projectname].downcase + ":" + getVar('hopsworks_version').value + ".0")).to be false

            # There should be no CondaCommands in the database
            expect(CondaCommands.find_by(project_id: @project[:id])).to be nil

            # Install a library to create the new environment
            install_library(@project[:id], ENV['PYTHON_VERSION'], 'beautifulsoup4', 'CONDA', conda_channel, lib_version: '4.12.2')

            get_env_commands(@project[:id], ENV['PYTHON_VERSION'])
            expect(json_body[:count]).to be > 0
            expect(json_body[:count]).to be <= num_hosts

            wait_for_running_command(@project[:id])

            get_env_commands(@project[:id], ENV['PYTHON_VERSION'])
            expect(json_body[:count]).to be == 0

            # Need to get the latest image of the project from DB
            base_python_project_image=@project.docker_image
            @project = get_project_by_name(@project[:projectname])
            non_versioned_project_image = @project.docker_image.rpartition('.').first
            expect(check_if_img_exists_locally(non_versioned_project_image + ".0")).to be true
            expect(check_if_img_exists_locally(non_versioned_project_image + ".1")).to be true
            expect(check_if_img_exists_locally(non_versioned_project_image + ".2")).to be false
            expect(check_if_img_exists_locally(base_python_project_image)).to be true
          end
          it 'export environment' do
            delete_env(@project[:id], ENV['PYTHON_VERSION'])
            @project = create_env_and_update_project(@project, ENV['PYTHON_VERSION'])
            export_env(@project[:id], ENV['PYTHON_VERSION'])
            expect_status_details(200)

            wait_for_running_command(@project[:id])
          end
        end
        context 'search' do
          it 'search library (conda)' do
            @project = create_env_and_update_project(@project, ENV['PYTHON_VERSION'])
            search_library(@project[:id], ENV['PYTHON_VERSION'], 'conda', 'dropbox', conda_channel)
            expect(json_body.count).to be >= 1
            dropbox = json_body[:items].detect { |library| library[:library] == "dropbox" }
            expect(dropbox[:versions].count).to be >= 1
          end
          it 'search library (pip)' do
            @project = create_env_and_update_project(@project, ENV['PYTHON_VERSION'])
            search_library(@project[:id], ENV['PYTHON_VERSION'], 'pip', 'dropbox')
            expect(json_body.count).to be >= 1
            dropbox = json_body[:items].detect { |library| library[:library] == "dropbox" }
            expect(dropbox[:versions].count).to be >= 1
          end
        end
        context 'install libraries' do
          it 'install versioned libraries' do
            expect(CondaCommands.find_by(project_id: @project[:id])).to be_nil
            @project = create_env_and_update_project(@project, ENV['PYTHON_VERSION'])

            install_library(@project[:id], ENV['PYTHON_VERSION'], 'modin%5Bdask%5D', 'PIP', conda_channel, lib_version: '0.11.2')
            install_library(@project[:id], ENV['PYTHON_VERSION'], 'imageio', 'CONDA', conda_channel, lib_version: '2.9.0')

            get_library_commands(@project[:id], ENV['PYTHON_VERSION'], 'modin%5Bdask%5D', expected_commands: 1)
            get_library_commands(@project[:id], ENV['PYTHON_VERSION'], 'imageio', expected_commands: 1)

            wait_for_running_command(@project[:id])

            get_library_commands(@project[:id], ENV['PYTHON_VERSION'], 'modin%5Bdask%5D', expected_commands: 0)

            get_library_commands(@project[:id], ENV['PYTHON_VERSION'], 'imageio', expected_commands: 0)

            list_libraries(@project[:id], ENV['PYTHON_VERSION'])

            modin_library = json_body[:items].detect { |library| library[:library] == "modin" }
            imageio_library = json_body[:items].detect { |library| library[:library] == "imageio" }

            expect(modin_library[:packageSource]).to eq ("PIP")
            expect(modin_library[:version]).to eq ("0.11.2")

            expect(imageio_library[:packageSource]).to eq("CONDA")
            expect(imageio_library[:version]).to eq ("2.9.0")

            delete_env(@project[:id], ENV['PYTHON_VERSION'])

            wait_for_running_command(@project[:id])
          end
          it 'install latest library version' do
            expect(CondaCommands.find_by(project_id: @project[:id])).to be_nil
            @project = create_env_and_update_project(@project, ENV['PYTHON_VERSION'])

            install_library(@project[:id], ENV['PYTHON_VERSION'], 'folium', 'PIP', conda_channel)
            install_library(@project[:id], ENV['PYTHON_VERSION'], 'rapidjson', 'CONDA', conda_channel)

            get_library_commands(@project[:id], ENV['PYTHON_VERSION'], 'folium', expected_commands: 1)
            get_library_commands(@project[:id], ENV['PYTHON_VERSION'], 'rapidjson', expected_commands: 1)

            wait_for_running_command(@project[:id])

            get_library_commands(@project[:id], ENV['PYTHON_VERSION'], 'folium', expected_commands: 0)
            get_library_commands(@project[:id], ENV['PYTHON_VERSION'], 'rapidjson', expected_commands: 0)

            list_libraries(@project[:id], ENV['PYTHON_VERSION'])

            folium_library = json_body[:items].detect { |library| library[:library] == "folium" }
            rapidjson_library = json_body[:items].detect { |library| library[:library] == "rapidjson" }

            expect(folium_library[:packageSource]).to eq ("PIP")
            expect(rapidjson_library[:packageSource]).to eq("CONDA")

            delete_env(@project[:id], ENV['PYTHON_VERSION'])
            wait_for_running_command(@project[:id])
          end
          it 'install from git' do
            expect(CondaCommands.find_by(project_id: @project[:id])).to be_nil
            @project = create_env_and_update_project(@project, ENV['PYTHON_VERSION'])
            uninstall_library(@project[:id], ENV['PYTHON_VERSION'], 'hops')

            wait_for_running_command(@project[:id])

            install_library(@project[:id], ENV['PYTHON_VERSION'], 'hops-util-py.git@branch-2.0', 'GIT', 'git', 'https://github.com/logicalclocks/hops-util-py.git@branch-2.0')

            wait_for_running_command(@project[:id])

            list_libraries(@project[:id], ENV['PYTHON_VERSION'])

            hops_library = json_body[:items].detect { |library| library[:library] == "hops" }
            expect(hops_library[:version]).to eq "2.0.0.2"
          end
          it 'install from wheel' do
            expect(CondaCommands.find_by(project_id: @project[:id])).to be_nil
            @project = create_env_and_update_project(@project, ENV['PYTHON_VERSION'])

            upload_wheel
            install_library(@project[:id], ENV['PYTHON_VERSION'], 'lark_parser-0.10.1-py2.py3-none-any.whl', 'WHEEL', 'wheel', "/Projects/#{@project[:projectname]}/Resources/lark_parser-0.10.1-py2.py3-none-any.whl")

            wait_for_running_command(@project[:id])

            list_libraries(@project[:id], ENV['PYTHON_VERSION'])

            lark_library = json_body[:items].detect { |library| library[:library] == "lark-parser" }
            expect(lark_library[:version]).to eq "0.10.1"
          end
          it 'install from environment.yml' do
            expect(CondaCommands.find_by(project_id: @project[:id])).to be_nil
            @project = create_env_and_update_project(@project, ENV['PYTHON_VERSION'])

            upload_environment
            install_library(@project[:id], ENV['PYTHON_VERSION'], 'environment.yml', 'ENVIRONMENT_YAML', 'environment', "/Projects/#{@project[:projectname]}/Resources/environment.yml")

            wait_for_running_command(@project[:id])

            list_libraries(@project[:id], ENV['PYTHON_VERSION'])
            dropbox_library = json_body[:items].detect { |library| library[:library] == "dropbox" }
            expect(dropbox_library[:packageSource]).to eq("CONDA")
            expect(dropbox_library[:version]).to eq ("11.14.0")
          end
          it 'install from requirements.txt' do
            expect(CondaCommands.find_by(project_id: @project[:id])).to be_nil
            @project = create_env_and_update_project(@project, ENV['PYTHON_VERSION'])

            upload_requirements
            install_library(@project[:id], ENV['PYTHON_VERSION'], 'requirements.txt', 'REQUIREMENTS_TXT', 'requirements', "/Projects/#{@project[:projectname]}/Resources/requirements.txt")

            wait_for_running_command(@project[:id])

            list_libraries(@project[:id], ENV['PYTHON_VERSION'])
            imageio_library = json_body[:items].detect { |library| library[:library] == "imageio" }
            expect(imageio_library[:packageSource]).to eq("PIP")
            expect(imageio_library[:version]).to eq ("2.28.1")
          end
          it 'uninstall libraries' do
            @project = create_env_and_update_project(@project, ENV['PYTHON_VERSION'])
            uninstall_library(@project[:id], ENV['PYTHON_VERSION'], 'imageio')

            wait_for_running_command(@project[:id])

            list_libraries(@project[:id], ENV['PYTHON_VERSION'])
            imageio_library = json_body[:items].detect { |library| library[:library] == "imageio" }
            expect(imageio_library).to eq nil
          end
          it 'should be possible to install same library if uninstall operation is ongoing for the same library' do
          expect(CondaCommands.find_by(project_id: @project[:id])).to be_nil
          @project = create_env_and_update_project(@project, ENV['PYTHON_VERSION'])

          uninstall_library(@project[:id], ENV['PYTHON_VERSION'], 'hops')

          install_library(@project[:id], ENV['PYTHON_VERSION'], 'hops', 'PIP', conda_channel, lib_version: '2.1.0')

          wait_for_running_command(@project[:id])
        end
        end
      end
    end
  end

  describe '#Conda extended' do
    context 'without authentication' do
      before :all do
        with_valid_project
        reset_session
      end
      it "not authenticated" do
        env = get_project_env_by_id(@project[:id])
        expect(env).not_to be_nil
        delete_env(@project[:id], ENV['PYTHON_VERSION'], expected_status: 401, error_code: 200003)
      end
    end
    context 'with authentication' do
      before :all do
        with_valid_project
        wait_for_running_command(@project[:id])
      end
      context 'conda not enabled' do
        it 'should fail to list envs' do
          @env = get_project_env_by_id(@project[:id])
          @project = get_project_by_name(@project[:projectname])
          if !@env.nil?
            delete_env(@project[:id], ENV['PYTHON_VERSION'])
          end
          list_envs(@project[:id])
          expect_status_details(404)
        end

        it 'should fail to get an environment' do
          @env = get_project_env_by_id(@project[:id])
          @project = get_project_by_name(@project[:projectname])
          if !@env.nil?
            delete_env(@project[:id], ENV['PYTHON_VERSION'])
          end
          get_env(@project[:id], ENV['PYTHON_VERSION'])
          expect_status_details(404)
        end

        it 'should fail to get env commands' do
          @env = get_project_env_by_id(@project[:id])
          @project = get_project_by_name(@project[:projectname])
          if !@env.nil?
            delete_env(@project[:id], ENV['PYTHON_VERSION'])
          end
          get_env_commands(@project[:id], ENV['PYTHON_VERSION'], expected_status: 404)
        end

        it 'should fail to list libraries' do
          @env = get_project_env_by_id(@project[:id])
          @project = get_project_by_name(@project[:projectname])
          if !@env.nil?
            delete_env(@project[:id], ENV['PYTHON_VERSION'])
          end
          list_libraries(@project[:id], ENV['PYTHON_VERSION'])
          expect_status_details(404)
        end

        it 'should fail to list library commands' do
          @env = get_project_env_by_id(@project[:id])
          @project = get_project_by_name(@project[:projectname])
          if !@env.nil?
            delete_env(@project[:id], ENV['PYTHON_VERSION'])
          end
          get_library_commands(@project[:id], ENV['PYTHON_VERSION'], 'numpy', expected_status: 404)
        end

        it 'should fail to install library' do
          @env = get_project_env_by_id(@project[:id])
          @project = get_project_by_name(@project[:projectname])
          if !@env.nil?
            delete_env(@project[:id], ENV['PYTHON_VERSION'])
          end
          install_library(@project[:id], ENV['PYTHON_VERSION'], 'requests', 'CONDA', conda_channel, lib_version: '2.20.0', expected_status: 404)
        end

        it 'should fail to search for a library' do
          @env = get_project_env_by_id(@project[:id])
          @project = get_project_by_name(@project[:projectname])
          if !@env.nil?
            delete_env(@project[:id], ENV['PYTHON_VERSION'])
          end
          search_library(@project[:id], ENV['PYTHON_VERSION'], 'conda', 'dropbox', conda_channel, expected_status: 404)
        end
      end
      context 'conda enabled' do
        before do
          if not conda_exists(ENV['PYTHON_VERSION'])
            skip "Anaconda is not installed in the machine or test is run locally"
          end
        end
        after :each do
          begin
            wait_for(10) do
              CondaCommands.find_by(project_id: @project[:id]).nil?
            end
          rescue RuntimeError => e
            delete_env(@project[:id], ENV['PYTHON_VERSION'])
            @project = create_env_and_update_project(@project, ENV['PYTHON_VERSION'])
            wait_for_running_command(@project[:id])
          end
        end
        context 'search' do
          it 'should not fail if library is not found (conda)' do
            @project = create_env_and_update_project(@project, ENV['PYTHON_VERSION'])
            search_library(@project[:id], ENV['PYTHON_VERSION'], 'conda', 'pretty-sure-not-to-exist', conda_channel, expected_status: 204)
          end
          it 'should not fail if library is not found (pip)' do
            @project = create_env_and_update_project(@project, ENV['PYTHON_VERSION'])
            search_library(@project[:id], ENV['PYTHON_VERSION'], 'pip', 'pretty-sure-not-to-exist', expected_status: 204)
          end
          it 'should not fail if library starts with number' do
            @project = create_env_and_update_project(@project, ENV['PYTHON_VERSION'])
            search_library(@project[:id], ENV['PYTHON_VERSION'], 'conda', '4ti2', 'conda-forge')
            expect(json_body.count).to be >= 1
            lib_name = json_body[:items].detect { |library| library[:library] == "4ti2" }
            expect(lib_name[:versions].count).to be >= 1
          end
          it 'should fail to search if library contains forbidden chars - conda' do
            @project = create_env_and_update_project(@project, ENV['PYTHON_VERSION'])
            search_library(@project[:id], ENV['PYTHON_VERSION'], 'conda', '`touch/tmp/hello`', 'defaults', expected_status: 422)
          end
          it 'should fail to search if library contains forbidden chars - pip' do
            @project = create_env_and_update_project(@project, ENV['PYTHON_VERSION'])
            search_library(@project[:id], ENV['PYTHON_VERSION'], 'pip', '`touch /tmp/hello`', expected_status: 422)
          end
          it 'should fail to search if package manager contains forbidden chars' do
            @project = create_env_and_update_project(@project, ENV['PYTHON_VERSION'])
            search_library(@project[:id], ENV['PYTHON_VERSION'], 'pip&', 'hello', expected_status: 422)
          end
          it 'should fail to search if channel contains forbidden chars' do
            @project = create_env_and_update_project(@project, ENV['PYTHON_VERSION'])
            search_library(@project[:id], ENV['PYTHON_VERSION'], 'conda', 'hello', 'https%3A%2F%2Fhello.com%2F%20%26test', expected_status: 422)
          end
        end
        context 'install fails' do
          it 'should fail to install library if package manager not set' do
            expect(CondaCommands.find_by(project_id: @project[:id])).to be_nil
            @project = create_env_and_update_project(@project, ENV['PYTHON_VERSION'])
            install_library(@project[:id], ENV['PYTHON_VERSION'], 'dropbox', '', conda_channel, lib_version: '9.0.0', expected_status: 400)
          end
          it 'should fail to install library if env version wrong' do
            expect(CondaCommands.find_by(project_id: @project[:id])).to be_nil
            @project = create_env_and_update_project(@project, ENV['PYTHON_VERSION'])
            install_library(@project[:id], '2.7', 'dropbox', 'CONDA', conda_channel, lib_version: '9.0.0', expected_status: 404)
          end
          it 'should fail to install library if library contains forbidden chars url encoded' do
            expect(CondaCommands.find_by(project_id: @project[:id])).to be_nil
            @project = create_env_and_update_project(@project, ENV['PYTHON_VERSION'])
            install_library(@project[:id], ENV['PYTHON_VERSION'], '%26%20touch%20%2Ftmp%2Ftest', 'CONDA', conda_channel, lib_version: '9.0.0', expected_status: 422)
          end
          it 'should fail to install library if version number contains forbidden chars' do
            expect(CondaCommands.find_by(project_id: @project[:id])).to be_nil
            @project = create_env_and_update_project(@project, ENV['PYTHON_VERSION'])
            install_library(@project[:id], ENV['PYTHON_VERSION'], 'dropbox', 'CONDA', conda_channel, lib_version: 'rm -rf *', expected_status: 422)
          end
          it 'should fail to install library if conda channel contains forbidden chars' do
            expect(CondaCommands.find_by(project_id: @project[:id])).to be_nil
            @project = create_env_and_update_project(@project, ENV['PYTHON_VERSION'])
            install_library(@project[:id], ENV['PYTHON_VERSION'], 'dropbox', 'CONDA', 'https%3A%2F%2Fhello.com%2F%20%26test', lib_version: '9.0.0', expected_status: 422)
          end
          it 'should fail if you try to use another package source' do
            expect(CondaCommands.find_by(project_id: @project[:id])).to be_nil
            @project = create_env_and_update_project(@project, ENV['PYTHON_VERSION'])
            install_library(@project[:id], ENV['PYTHON_VERSION'], 'dropbox', 'CARGO', conda_channel, lib_version: '9.0.0', expected_status: 400)
          end
          it 'should fail to install same library with upper and lower case variation' do
            expect(CondaCommands.find_by(project_id: @project[:id])).to be_nil
            @project = create_env_and_update_project(@project, ENV['PYTHON_VERSION'])
            install_library(@project[:id], ENV['PYTHON_VERSION'], 'scipy', 'PIP', conda_channel, lib_version: '1.2.2', expected_status: 409)
            install_library(@project[:id], ENV['PYTHON_VERSION'], 'SCIPY', 'PIP', conda_channel, lib_version: '1.2.2', expected_status: 409)
          end
        end
        context 'environment' do
          it 'remove env' do
            expect(CondaCommands.find_by(project_id: @project[:id])).to be_nil
            @project = create_env_and_update_project(@project, ENV['PYTHON_VERSION'])
            # Install a library to create the new environment
            install_library(@project[:id], ENV['PYTHON_VERSION'], 'tenacity', 'CONDA', conda_channel, lib_version: '8.2.2')
            # Wait until library is installed
            wait_for_running_command(@project[:id])
            @project = get_project_by_name(@project[:projectname])
            non_versioned_project_image = @project.docker_image.rpartition('.').first
            delete_env(@project[:id], ENV['PYTHON_VERSION'])

            wait_for_running_command(@project[:id])
            wait_result = wait_for_me_time(15, 1) do
              begin
                # Check if project docker images were removed by kagent
                expect(check_if_img_exists_locally(non_versioned_project_image + ".0")).to be false
                expect(check_if_img_exists_locally(non_versioned_project_image + ".1")).to be false
                # Check that docker registry does not contain the image tags
                expect(image_in_registry(@project.projectname.downcase, non_versioned_project_image.split(":")[1])).to be false
              rescue RSpec::Expectations::ExpectationNotMetError => e
                { 'success' => false, 'error' => e }
              else
                { 'success' => true }
              end
            end
            raise wait_result['error'] unless wait_result['success']
          end
          it 'clean up env of deleted project' do
            expect(CondaCommands.find_by(project_id: @project[:id])).to be_nil
            project = create_project
            project = create_env_and_update_project(project, ENV['PYTHON_VERSION'])

            wait_for do
              CondaCommands.where(["project_id = ? and op = ?", project[:id], "SYNC_BASE_ENV"]).empty?
            end
            install_library(project[:id], ENV['PYTHON_VERSION'], 'dropbox', 'CONDA', conda_channel, lib_version: '11.14.0')
            project = get_project_by_name(project[:projectname])
            non_versioned_project_image = project.docker_image.rpartition('.').first
            wait_for_running_command(@project[:id])
            delete_project(project)
            # Wait for garbage collection
            wait_result = wait_for_me_time(15, 1) do
              begin
                # Check if project docker images were removed by kagent
                expect(check_if_img_exists_locally(non_versioned_project_image + ".0")).to be false
                expect(check_if_img_exists_locally(non_versioned_project_image + ".1")).to be false
                # Check that docker registry does not contain the image tags
                expect(image_in_registry(@project.projectname.downcase, non_versioned_project_image.split(":")[1])).to be false
              rescue RSpec::Expectations::ExpectationNotMetError => e
                { 'success' => false, 'error' => e }
              else
                { 'success' => true }
              end
            end
            raise wait_result['error'] unless wait_result['success']
          end
          it 'destroy anaconda should not delete base docker image' do
            expect(CondaCommands.find_by(project_id: @project[:id])).to be_nil
            create_env(@project, ENV['PYTHON_VERSION'])

            # Enabling anaconda will not create an environment yet
            expect(check_if_img_exists_locally("python310:" + getVar('hopsworks_version').value)).to be true

            delete_env(@project[:id], ENV['PYTHON_VERSION'])
            wait_for_running_command(@project[:id])
            expect(check_if_img_exists_locally("python310:" + getVar('hopsworks_version').value)).to be true
          end
          it 'create environment from yml with jupyter install true' do
            expect(CondaCommands.find_by(project_id: @project[:id])).to be_nil
            upload_yml
            delete_env(@project[:id], ENV['PYTHON_VERSION'])
            create_env_from_file(@project[:id], "/Projects/#{@project[:projectname]}/Resources/environment_cpu.yml", true)

            wait_for_running_command(@project[:id])

            @project = get_project_by_name(@project[:projectname])
            expect(ENV['PYTHON_VERSION']).to eq ENV['PYTHON_VERSION']
          end
          it 'create environment from yml with jupyter install false' do
            expect(CondaCommands.find_by(project_id: @project[:id])).to be_nil
            upload_yml
            delete_env(@project[:id], ENV['PYTHON_VERSION'])
            create_env_from_file(@project[:id], "/Projects/#{@project[:projectname]}/Resources/environment_cpu.yml", false)
            wait_for_running_command(@project[:id])

            @project = get_project_by_name(@project[:projectname])
            expect(ENV['PYTHON_VERSION']).to eq ENV['PYTHON_VERSION']
          end
          it 'create environment from requirements.txt with jupyter install true' do
            expect(CondaCommands.find_by(project_id: @project[:id])).to be_nil
            delete_env(@project[:id], ENV['PYTHON_VERSION'])
            upload_requirements
            create_env_from_file(@project[:id], "/Projects/#{@project[:projectname]}/Resources/requirements.txt", true)

            wait_for_running_command(@project[:id])

            @project = get_project_by_name(@project[:projectname])
            expect(ENV['PYTHON_VERSION']).to eq ENV['PYTHON_VERSION']

            list_libraries(@project[:id], ENV['PYTHON_VERSION'])
            imageio_library = json_body[:items].detect { |library| library[:library] == "imageio" }
            expect(imageio_library[:packageSource]).to eq("PIP")
            expect(imageio_library[:version]).to eq ("2.28.1")
          end
          it 'create environment from requirements.txt with jupyter install false' do
            expect(CondaCommands.find_by(project_id: @project[:id])).to be_nil
            delete_env(@project[:id], ENV['PYTHON_VERSION'])
            upload_requirements
            create_env_from_file(@project[:id], "/Projects/#{@project[:projectname]}/Resources/requirements.txt", false)
            wait_for_running_command(@project[:id])

            @project = get_project_by_name(@project[:projectname])
            expect(ENV['PYTHON_VERSION']).to eq ENV['PYTHON_VERSION']

            list_libraries(@project[:id], ENV['PYTHON_VERSION'])
            imageio_library = json_body[:items].detect { |library| library[:library] == "imageio" }
            expect(imageio_library[:packageSource]).to eq("PIP")
            expect(imageio_library[:version]).to eq ("2.28.1")
          end
          it 'check conflicts are empty' do
            expect(CondaCommands.find_by(project_id: @project[:id])).to be_nil
            delete_env(@project[:id], ENV['PYTHON_VERSION'])
            @project = create_env_and_update_project(@project, ENV['PYTHON_VERSION'])

            get_env_conflicts(@project[:id], ENV['PYTHON_VERSION'])
            expect(json_body[:items]).to be_nil
            expect(json_body[:count]).to be_nil
          end
          it 'check conflicts are not empty' do
            expect(CondaCommands.find_by(project_id: @project[:id])).to be_nil
            delete_env(@project[:id], ENV['PYTHON_VERSION'])
            @project = create_env_and_update_project(@project, ENV['PYTHON_VERSION'])

            uninstall_library(@project[:id], ENV['PYTHON_VERSION'], 'tensorboard')

            wait_for_running_command(@project[:id])

            get_env_conflicts(@project[:id], ENV['PYTHON_VERSION'])
            expect(json_body[:count]).to be > 0
          end
          it 'check jupyter conflicts' do
            expect(CondaCommands.find_by(project_id: @project[:id])).to be_nil
            delete_env(@project[:id], ENV['PYTHON_VERSION'])
            @project = create_env_and_update_project(@project, ENV['PYTHON_VERSION'])

            get_env_conflicts(@project[:id], ENV['PYTHON_VERSION'], "?filter_by=service:JUPYTER")
            expect(json_body[:items]).to be_nil
            expect(json_body[:count]).to be_nil

            uninstall_library(@project[:id], ENV['PYTHON_VERSION'], 'notebook')

            wait_for_running_command(@project[:id])

            get_env_conflicts(@project[:id], ENV['PYTHON_VERSION'], "?filter_by=service:JUPYTER")
            expect(json_body[:count]).to be > 0
          end
        end
      end
    end
  end
end