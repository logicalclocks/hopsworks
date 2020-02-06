=begin
 Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 are released under the following license:

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
  after(:all) {clean_all_test_projects}
  describe '#Conda basic operations'  do
    after (:all){clean_projects}

    let(:num_hosts) {Host.count}
    let(:conda_channel) {Variables.find_by(id: "conda_default_repo").value}
    let(:python_version) {'2.7'}
    let(:python_version_2) {'3.6'}

    describe "#create" do
      context 'without authentication' do
        before :all do
          with_valid_project
          reset_session
        end
        it "not authenticated" do
          create_env(@project, python_version)
          expect_json(errorCode: 200003)
          expect_status(401)
        end
      end

      context 'with authentication' do
        before :all do
          with_valid_project
        end

        context 'conda not enabled' do
          it 'should fail to list envs' do
            @project = get_project_by_name(@project[:projectname])
            if !@project[:python_version].nil? and !@project[:python_version].empty?
              delete_env(@project[:id], @project[:python_version])
            end
            list_envs(@project[:id])
            expect_status(404)
          end

          it 'should fail to get env commands' do
            @project = get_project_by_name(@project[:projectname])
            if !@project[:python_version].nil? and !@project[:python_version].empty?
              delete_env(@project[:id], @project[:python_version])
            end
            get_env_commands(@project[:id], python_version)
            expect_status(404)
          end

          it 'should fail to list libraries' do
            @project = get_project_by_name(@project[:projectname])
            if !@project[:python_version].nil? and !@project[:python_version].empty?
              delete_env(@project[:id], @project[:python_version])
            end
            list_libraries(@project[:id], python_version)
            expect_status(404)
          end

          it 'should fail to list library commands' do
            @project = get_project_by_name(@project[:projectname])
            if !@project[:python_version].nil? and !@project[:python_version].empty?
              delete_env(@project[:id], @project[:python_version])
            end
            get_library_commands(@project[:id], python_version, 'numpy')
            expect_status(404)
          end

          it 'should fail to install library' do
            @project = get_project_by_name(@project[:projectname])
            if !@project[:python_version].nil? and !@project[:python_version].empty?
              delete_env(@project[:id], @project[:python_version])
            end
            install_library(@project[:id], python_version, 'requests', 'conda', '2.20.0', 'CPU', conda_channel)
            expect_status(404)
          end

          it 'should fail to search for a library' do
            @project = get_project_by_name(@project[:projectname])
            if !@project[:python_version].nil? and !@project[:python_version].empty?
              delete_env(@project[:id], @project[:python_version])
            end
            search_library(@project[:id], python_version, 'conda', 'dropbox', conda_channel)
            expect_status(404)
          end
        end

        context 'conda enabled' do
          it 'enable anaconda' do
            @project = create_env_and_update_project(@project, python_version)

            if not conda_exists
              skip "Anaconda is not installed in the machine or test is run locally"
            end

            # Enabling anaconda will not create an environment yet
            expect(check_if_env_exists_locally(@project[:projectname])).to be false
            # There should be no CondaCommands in the database
            expect(CondaCommands.find_by(proj: @project[:projectname])).to be nil

            # Install a library to create the new environment
            install_library(@project[:id], @project[:python_version], 'requests', 'conda', '2.20.0', 'CPU', conda_channel)
            expect_status(201)
            es_index_date_suffix = Time.now.strftime("%Y.%m.%d")

            get_env_commands(@project[:id], @project[:python_version])
            expect_status(200)
            expect(json_body[:count]).to be > 0
            expect(json_body[:count]).to be <= num_hosts

            wait_for do
              CondaCommands.find_by(proj: @project[:projectname]).nil?
            end

            get_env_commands(@project[:id], @project[:python_version])
            expect_status(200)
            expect(json_body[:count]).to be == 0

            expect(check_if_env_exists_locally(@project[:projectname])).to be true

            Airborne.configure do |config|
              config.base_url = ''
            end

            # Elasticsearch index should have been created for this project
            index_name = "#{@project[:projectname].downcase}_kagent-#{es_index_date_suffix}"
            elastic_head "#{index_name}"

            Airborne.configure do |config|
              config.base_url = "https://#{ENV['WEB_HOST']}:#{ENV['WEB_PORT']}"
            end

            expect_status(200)
          end

          it 'search library (conda)' do
            @project = create_env_and_update_project(@project, python_version)
            search_library(@project[:id], @project[:python_version], 'conda', 'dropbox', conda_channel)
            expect_status(200)
            expect(json_body.count).to be >= 1
            dropbox = json_body[:items].detect { |library| library[:library] == "dropbox" }
            expect(dropbox[:versions].count).to be >= 1
          end
          
          it 'search library (pip)' do
            @project = create_env_and_update_project(@project, python_version)
            search_library(@project[:id], @project[:python_version], 'pip', 'dropbox')
            expect_status(200)
            expect(json_body.count).to be >= 1
            dropbox = json_body[:items].detect { |library| library[:library] == "dropbox" }
            expect(dropbox[:versions].count).to be >= 1
          end

          it 'should not fail if library is not found (conda)' do
            @project = create_env_and_update_project(@project, python_version)
            search_library(@project[:id], @project[:python_version], 'conda', 'pretty-sure-not-to-exist', conda_channel)
            expect_status(204)
          end

          it 'should not fail if library is not found (pip)' do
            @project = create_env_and_update_project(@project, python_version)
            search_library(@project[:id], @project[:python_version], 'pip', 'pretty-sure-not-to-exist')
            expect_status(204)
          end

          it 'should not fail if library starts with number' do
            @project = create_env_and_update_project(@project, python_version)
            search_library(@project[:id], @project[:python_version], 'conda', '4ti2', 'conda-forge')
            expect_status(200)
            expect(json_body.count).to be >= 1
            lib_name = json_body[:items].detect { |library| library[:library] == "4ti2" }
            expect(lib_name[:versions].count).to be >= 1
          end

          it 'should fail to search if library contains forbidden chars - conda' do
            @project = create_env_and_update_project(@project, python_version)
            search_library(@project[:id], @project[:python_version], 'conda', '`touch /tmp/hello`', 'defaults')
            expect_status(422)
          end

          it 'should fail to search if library contains forbidden chars - pip' do
            @project = create_env_and_update_project(@project, python_version)
            search_library(@project[:id], @project[:python_version], 'pip', '`touch /tmp/hello`')
            expect_status(422)
          end

          it 'should fail to search if package manager contains forbidden chars' do
            @project = create_env_and_update_project(@project, python_version)
            search_library(@project[:id], @project[:python_version], 'pip&', 'hello')
            expect_status(422)
          end

          it 'should fail to search if channel contains forbidden chars' do
            @project = create_env_and_update_project(@project, python_version)
            search_library(@project[:id], @project[:python_version], 'conda', 'hello', 'https%3A%2F%2Fhello.com%2F%20%26test')
            expect_status(422)
          end

          it 'should fail to install library if package manager not set' do
            @project = create_env_and_update_project(@project, python_version)
            install_library(@project[:id], @project[:python_version], 'dropbox', '', '9.0.0', 'CPU', conda_channel)
            expect_status(400)
          end

          it 'should fail to install library if version not set' do
            @project = create_env_and_update_project(@project, python_version)
            install_library(@project[:id], @project[:python_version], 'dropbox', 'conda', '', 'CPU', conda_channel)
            expect_status(400)
          end

          it 'should fail to install library if machine type not set' do
            @project = create_env_and_update_project(@project, python_version)
            install_library(@project[:id], @project[:python_version], 'dropbox', 'conda', '9.0.0', '', conda_channel)
            expect_status(400)
          end

          it 'should fail to install library if env version wrong' do
            @project = create_env_and_update_project(@project, python_version)
            install_library(@project[:id], python_version_2, 'dropbox', 'conda', '9.0.0', 'CPU', conda_channel)
            expect_status(404)
          end

          it 'should fail to install library if library contains forbidden chars url encoded' do
            @project = create_env_and_update_project(@project, python_version)
            install_library(@project[:id], @project[:python_version], '%26%20touch%20%2Ftmp%2Ftest', 'conda', '9.0.0', 'CPU', conda_channel)
            expect_status(422)
          end

          it 'should fail to install library if version number contains forbidden chars' do
            @project = create_env_and_update_project(@project, python_version)
            install_library(@project[:id], @project[:python_version], 'dropbox', 'conda', 'rm -rf *', 'CPU', conda_channel)
            expect_status(422)
          end

          it 'should fail to install library if conda channel contains forbidden chars' do
            @project = create_env_and_update_project(@project, python_version)
            install_library(@project[:id], @project[:python_version], 'dropbox', 'conda',
                            '9.0.0', 'CPU', 'https%3A%2F%2Fhello.com%2F%20%26test')
            expect_status(422)
          end

          it 'should fail if you try to use another package manager' do
            @project = create_env_and_update_project(@project, python_version)
            install_library(@project[:id], python_version_2, 'dropbox', 'cargo', '9.0.0', 'CPU', conda_channel)
            expect_status(404)
          end

          it 'should fail to install same library with upper and lower case variation' do
            @project = create_env_and_update_project(@project, python_version)
            install_library(@project[:id], python_version, 'scipy', 'pip', '1.2.2', 'ALL', conda_channel)
            expect_status(201)
            install_library(@project[:id], python_version, 'scipy', 'pip', '1.2.2', 'ALL', conda_channel)
            expect_status(409)
            install_library(@project[:id], python_version, 'SCIPY', 'pip', '1.2.2', 'ALL', conda_channel)
            expect_status(409)
          end

          it 'install libraries' do
            @project = create_env_and_update_project(@project, python_version)
            install_library(@project[:id], @project[:python_version], 'imageio', 'conda', '2.2.0', 'CPU', conda_channel)
            expect_status(201)

            get_library_commands(@project[:id], @project[:python_version], 'imageio')
            expect_status(200)
            expect(json_body[:count]).to be > 0
            expect(json_body[:count]).to be <= num_hosts

            wait_for do
              CondaCommands.find_by(proj: @project[:projectname]).nil?
            end

            get_library_commands(@project[:id], @project[:python_version], 'imageio')
            expect_status(200)
            expect(json_body[:count]).to be == 0

            install_library(@project[:id], @project[:python_version], 'tflearn', 'pip', '0.3.2', 'ALL', conda_channel)
            expect_status(201)

            wait_for do
              CondaCommands.find_by(proj: @project[:projectname]).nil?
            end
          end

          it 'list libraries' do
            @project = create_env_and_update_project(@project, python_version)
            list_libraries(@project[:id], @project[:python_version])

            tflearn_library = json_body[:items].detect { |library| library[:library] == "tflearn" }
            tensorflow_library = json_body[:items].detect { |library| library[:library] == "tensorflow" }
            hops_library = json_body[:items].detect { |library| library[:library] == "hops" }
            imageio_library = json_body[:items].detect { |library| library[:library] == "imageio" }

            expect(tensorflow_library[:machine]).to eq ("CPU")

            expect(tflearn_library[:machine]).to eq ("ALL")
            expect(tflearn_library[:packageManager]).to eq ("PIP")
            expect(tflearn_library[:version]).to eq ("0.3.2")

            expect(hops_library[:machine]).to eq ("ALL")
            expect(hops_library[:packageManager]).to eq ("PIP")

            expect(imageio_library[:machine]).to eq("CPU")
            expect(imageio_library[:packageManager]).to eq("CONDA")
            expect(imageio_library[:version]).to eq ("2.2.0")

          end

          it 'uninstall libraries' do
            @project = create_env_and_update_project(@project, python_version)
            uninstall_library(@project[:id], @project[:python_version], 'imageio')
            expect_status(204)

            wait_for do
              CondaCommands.find_by(proj: @project[:projectname]).nil?
            end
          end

          it 'export environment' do
            @project = create_env_and_update_project(@project, python_version)
            export_env(@project[:id], @project[:python_version])
            expect_status(200)

            wait_for do
              CondaCommands.find_by(proj: @project[:projectname]).nil?
            end
          end

          it 'remove env' do
            @project = create_env_and_update_project(@project, python_version)
            delete_env(@project[:id], @project[:python_version])
            expect_status(204)

            wait_for do
              CondaCommands.find_by(proj: @project[:projectname]).nil?
            end

            Airborne.configure do |config|
              config.base_url = ''
            end

            # Elasticsearch index should have been deleted
            index_name = "#{@project[:projectname]}_kagent-*"
            response = elastic_get "_cat/indices/#{index_name}"

            Airborne.configure do |config|
              config.base_url = "https://#{ENV['WEB_HOST']}:#{ENV['WEB_PORT']}"
            end

            expect(response.body).to eq("")

            if not conda_exists
              skip "Anaconda is not installed in the machine or test is run locally"
            end
            expect(check_if_env_exists_locally(@project[:projectname])).to be false
          end

          it 'destroy anaconda should not delete base environments' do
            create_env(@project, python_version)
            expect_status(201)
            if not conda_exists
              skip "Anaconda is not installed in the machine or test is run locally"
            end

            # Enabling anaconda will not create an environment yet
            expect(check_if_env_exists_locally(@project[:projectname])).to be false

            delete_env(@project[:id], python_version)
            expect_status(204)
            wait_for do
              CondaCommands.find_by(proj: @project[:projectname]).nil?
            end
            expect(check_if_env_exists_locally("python27")).to be true
            expect(check_if_env_exists_locally("python36")).to be true
          end

          it 'create environment from yml' do
            delete_env(@project[:id], python_version)
            upload_yml
            create_env_yml(@project[:id], "/Projects/#{@project[:projectname]}/Resources/environment_cpu.yml", nil, nil, true)
            expect_status(201)

            wait_for do
              CondaCommands.find_by(proj: @project[:projectname]).nil?
            end

            @project = get_project_by_name(@project[:projectname])
            expect(@project[:python_version]).to eq "3.6"
          end

          it 'GC stale Conda env' do
            if not conda_exists
              skip "Anaconda is not installed in the machine or test is run locally"
            end

            @project = get_project_by_name(@project[:projectname])
            delete_env(@project[:id], @project[:python_version])

            wait_for do
              CondaCommands.find_by(proj: @project[:projectname]).nil?
            end

            # Create a second project with Anaconda enabled
            project2 = create_project
            project2 = create_env_and_update_project(project2, python_version)
            expect_status(201)

            # Install a library to create the new environment
            install_library(project2[:id], project2[:python_version], 'paramiko', 'conda', '2.4.2', 'CPU', conda_channel)
            expect_status(201)
            wait_for do
              CondaCommands.find_by(proj: project2[:projectname]).nil?
            end
            expect(check_if_env_exists_locally(project2[:projectname])).to be true

            # Disable Anaconda for project2 directly in the database
            # so it does not send a command to kagent
            tmp_proj = Project.find_by(id: project2[:id])
            tmp_proj.conda = 0
            tmp_proj.save

            wait_for do
              CondaCommands.find_by(proj: project2[:projectname]).nil?
            end

            trigger_conda_gc

            wait_for do
              check_if_env_exists_locally(project2[:projectname]) == false
            end

            expect(check_if_env_exists_locally(project2[:projectname])).to be false
          end
        end
      end
    end

    describe "#Creation not executed on non-conda hosts" do
      context 'with admin rights' do
        before :each do
          with_valid_project
        end

        it 'should be able to disable conda on a host' do
          with_admin_session
          if num_hosts > 1
            # In case we have multi vms disable the one on which Hopsworks is not running
            admin_create_update_cluster_node("hopsworks1.logicalclocks.com", {condaEnabled: "false"})
          else
            admin_create_update_cluster_node("hopsworks0.logicalclocks.com", {condaEnabled: "false"})
          end
          expect_status(204)
        end

        it 'should fail to create an environment on the local machine - single vm' do
          if num_hosts > 1
            skip "Multi vm setup."
          end

          create_session(@user[:email], "Pass123")
          create_env(@project, python_version)
          expect_status(503)
        end

        it 'should not have created any conda_commands in the db - single vm' do
          if num_hosts > 1
            skip "Multi vm setup."
          end

          expect(CondaCommands.find_by(proj: @project[:projectname])).to be nil
        end

        it 'should create an environment on the other machines - multi vm' do
          if num_hosts == 1
            skip "Single vm setup"
          end

          create_session(@user[:email], "Pass123")
          create_env(@project, python_version)
          expect_status(201)
        end

        it 'should delete the environment from the other machines - multi vm' do
          if num_hosts == 1
            skip "Singe vm setup"
          end

          delete_env(@project[:id], python_version)
          expect_status(204)
        end

        it 'should be able to re-enable conda on a host' do
          with_admin_session
          if num_hosts > 1
            # In case we have multi vms disable the one on which Hopsworks is not running
            admin_create_update_cluster_node("hopsworks1.logicalclocks.com", {condaEnabled: "true"})
          else
            admin_create_update_cluster_node("hopsworks0.logicalclocks.com", {condaEnabled: "true"})
          end
          expect_status(204)
        end

        it 'should be able to create an environment' do
          create_session(@user[:email], "Pass123")
          create_env(@project, python_version)
          expect_status(201)
        end
      end
    end

    describe "#Library installation not executed on non-conda hosts" do
      context 'with admin rights' do
        before :all do
          with_valid_project
        end

        it 'should create an environment ' do
          @project = create_env_and_update_project(@project, python_version)
        end

        it 'should be able to disable conda on a host' do
          with_admin_session
          if num_hosts > 1
            # In case we have multi vms disable the one on which Hopsworks is not running
            admin_create_update_cluster_node("hopsworks1.logicalclocks.com", {condaEnabled: "false"})
          else
            admin_create_update_cluster_node("hopsworks0.logicalclocks.com", {condaEnabled: "false"})
          end
          expect_status(204)
        end

        it 'should fail to install a library' do
          create_session(@user[:email], "Pass123")
          @project = create_env_and_update_project(@project, python_version)
          install_library(@project[:id], @project[:python_version], 'imageio', 'conda', '2.2.0', 'CPU', conda_channel)
          if num_hosts == 1
            #  If single VM there are no hosts on which to install the library. Hopsworks returns 412
            expect_status(503)
          else
            # If it is a multi vm there are hosts to install the library.
            expect_status(201)
          end
        end

        it 'should not have created any conda_commands in the db' do
          if num_hosts == 1
            # For single vm, there should not be any command in the db
            expect(CondaCommands.find_by(proj: @project[:projectname])).to be nil
          else
            # For multi vm setup there should be (num_hosts - 1) * 2 commands.
            # For each library installation there will be one command for the
            # environment creation and one for the installation.
            expect(CondaCommands.where(proj: @project[:projectname]).count).to eq((num_hosts - 1) * 2)
          end
        end

        it 'should be able to re-enable conda on a host' do
          with_admin_session
          if num_hosts > 1
            # In case we have multi vms disable the one on which Hopsworks is not running
            admin_create_update_cluster_node("hopsworks1.logicalclocks.com", {condaEnabled: "true"})
          else
            admin_create_update_cluster_node("hopsworks0.logicalclocks.com", {condaEnabled: "true"})
          end
          expect_status(204)
        end

        it 'should be able to install a library' do
          create_session(@user[:email], "Pass123")
          @project = create_env_and_update_project(@project, python_version)
          install_library(@project[:id], @project[:python_version], 'dropbox', 'conda', '9.0.0', 'CPU', conda_channel)
          expect_status(201)

          # Check that the command has been register into the table and it will be eventually sent to the agent
          expect(CondaCommands.find_by(proj: @project[:projectname])).not_to be nil
        end
      end
    end
  end
end
