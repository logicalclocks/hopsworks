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
  after(:all) {clean_all_test_projects(spec: "conda")}
  describe '#Conda basic operations'  do

    let(:num_hosts) {Host.count}
    let(:conda_channel) {Variables.find_by(id: "conda_default_repo").value}
    let(:python_version) {'3.6'}
    let(:python_version_2) {'3.7'}

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
              delete_env(@project[:id], python_version)
            end
            list_envs(@project[:id])
            expect_status(404)
          end

          it 'should fail to get env commands' do
            @project = get_project_by_name(@project[:projectname])
            if !@project[:python_version].nil? and !@project[:python_version].empty?
              delete_env(@project[:id], python_version)
            end
            get_env_commands(@project[:id], python_version)
            expect_status(404)
          end

          it 'should fail to list libraries' do
            @project = get_project_by_name(@project[:projectname])
            if !@project[:python_version].nil? and !@project[:python_version].empty?
              delete_env(@project[:id], python_version)
            end
            list_libraries(@project[:id], python_version)
            expect_status(404)
          end

          it 'should fail to list library commands' do
            @project = get_project_by_name(@project[:projectname])
            if !@project[:python_version].nil? and !@project[:python_version].empty?
              delete_env(@project[:id], python_version)
            end
            get_library_commands(@project[:id], python_version, 'numpy')
            expect_status(404)
          end

          it 'should fail to install library' do
            @project = get_project_by_name(@project[:projectname])
            if !@project[:python_version].nil? and !@project[:python_version].empty?
              delete_env(@project[:id], python_version)
            end
            install_library(@project[:id], python_version, 'requests', 'conda', '2.20.0', conda_channel)
            expect_status(404)
          end

          it 'should fail to search for a library' do
            @project = get_project_by_name(@project[:projectname])
            if !@project[:python_version].nil? and !@project[:python_version].empty?
              delete_env(@project[:id], python_version)
            end
            search_library(@project[:id], python_version, 'conda', 'dropbox', conda_channel)
            expect_status(404)
          end
        end

        context 'conda enabled' do
          it 'enable anaconda' do
            @project = create_env_and_update_project(@project, python_version)

            if not conda_exists(python_version)
              skip "Anaconda is not installed in the machine or test is run locally"
            end

            # Enabling anaconda will not create an environment yet
            expect(check_if_img_exists_locally(@project[:projectname].downcase + ":" + getVar('hopsworks_version').value + ".0")).to be false

            # There should be no CondaCommands in the database
            expect(CondaCommands.find_by(project_id: @project[:id])).to be nil

            # Install a library to create the new environment
            install_library(@project[:id], python_version, 'beautifulsoup4', 'conda', '4.9.0', conda_channel)
            expect_status(201)

            get_env_commands(@project[:id], python_version)
            expect_status(200)
            expect(json_body[:count]).to be > 0
            expect(json_body[:count]).to be <= num_hosts

            wait_for do
              CondaCommands.find_by(project_id: @project[:id]).nil?
            end

            get_env_commands(@project[:id], python_version)
            expect_status(200)
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

          it 'search library (conda)' do
            @project = create_env_and_update_project(@project, python_version)
            search_library(@project[:id], python_version, 'conda', 'dropbox', conda_channel)
            expect_status(200)
            expect(json_body.count).to be >= 1
            dropbox = json_body[:items].detect { |library| library[:library] == "dropbox" }
            expect(dropbox[:versions].count).to be >= 1
          end
          
          it 'search library (pip)' do
            @project = create_env_and_update_project(@project, python_version)
            search_library(@project[:id], python_version, 'pip', 'dropbox')
            expect_status(200)
            expect(json_body.count).to be >= 1
            dropbox = json_body[:items].detect { |library| library[:library] == "dropbox" }
            expect(dropbox[:versions].count).to be >= 1
          end

          it 'should not fail if library is not found (conda)' do
            @project = create_env_and_update_project(@project, python_version)
            search_library(@project[:id], python_version, 'conda', 'pretty-sure-not-to-exist', conda_channel)
            expect_status(204)
          end

          it 'should not fail if library is not found (pip)' do
            @project = create_env_and_update_project(@project, python_version)
            search_library(@project[:id], python_version, 'pip', 'pretty-sure-not-to-exist')
            expect_status(204)
          end

          it 'should not fail if library starts with number' do
            @project = create_env_and_update_project(@project, python_version)
            search_library(@project[:id], python_version, 'conda', '4ti2', 'conda-forge')
            expect_status(200)
            expect(json_body.count).to be >= 1
            lib_name = json_body[:items].detect { |library| library[:library] == "4ti2" }
            expect(lib_name[:versions].count).to be >= 1
          end

          it 'should fail to search if library contains forbidden chars - conda' do
            @project = create_env_and_update_project(@project, python_version)
            search_library(@project[:id], python_version, 'conda', '`touch /tmp/hello`', 'defaults')
            expect_status(422)
          end

          it 'should fail to search if library contains forbidden chars - pip' do
            @project = create_env_and_update_project(@project, python_version)
            search_library(@project[:id], python_version, 'pip', '`touch /tmp/hello`')
            expect_status(422)
          end

          it 'should fail to search if package manager contains forbidden chars' do
            @project = create_env_and_update_project(@project, python_version)
            search_library(@project[:id], python_version, 'pip&', 'hello')
            expect_status(422)
          end

          it 'should fail to search if channel contains forbidden chars' do
            @project = create_env_and_update_project(@project, python_version)
            search_library(@project[:id], python_version, 'conda', 'hello', 'https%3A%2F%2Fhello.com%2F%20%26test')
            expect_status(422)
          end

          it 'should fail to install library if package manager not set' do
            @project = create_env_and_update_project(@project, python_version)
            install_library(@project[:id], python_version, 'dropbox', '', '9.0.0', conda_channel)
            expect_status(400)
          end

          it 'should fail to install library if version not set' do
            @project = create_env_and_update_project(@project, python_version)
            install_library(@project[:id], python_version, 'dropbox', 'conda', '', conda_channel)
            expect_status(400)
          end

          it 'should fail to install library if env version wrong' do
            @project = create_env_and_update_project(@project, python_version)
            install_library(@project[:id], python_version_2, 'dropbox', 'conda', '9.0.0', conda_channel)
            expect_status(404)
          end

          it 'should fail to install library if library contains forbidden chars url encoded' do
            @project = create_env_and_update_project(@project, python_version)
            install_library(@project[:id], python_version, '%26%20touch%20%2Ftmp%2Ftest', 'conda', '9.0.0', conda_channel)
            expect_status(422)
          end

          it 'should fail to install library if version number contains forbidden chars' do
            @project = create_env_and_update_project(@project, python_version)
            install_library(@project[:id], python_version, 'dropbox', 'conda', 'rm -rf *', conda_channel)
            expect_status(422)
          end

          it 'should fail to install library if conda channel contains forbidden chars' do
            @project = create_env_and_update_project(@project, python_version)
            install_library(@project[:id], python_version, 'dropbox', 'conda',
                            '9.0.0', 'https%3A%2F%2Fhello.com%2F%20%26test')
            expect_status(422)
          end

          it 'should fail if you try to use another package manager' do
            @project = create_env_and_update_project(@project, python_version)
            install_library(@project[:id], python_version, 'dropbox', 'cargo', '9.0.0', conda_channel)
            expect_status(404)
          end

          it 'should fail to install same library with upper and lower case variation' do
            @project = create_env_and_update_project(@project, python_version)
            install_library(@project[:id], python_version, 'scipy', 'pip', '1.2.2', conda_channel)
            expect_status(409) #scipy is in the base env
            install_library(@project[:id], python_version, 'SCIPY', 'pip', '1.2.2', conda_channel)
            expect_status(409)
          end

          it 'install libraries' do
            @project = create_env_and_update_project(@project, python_version)
            install_library(@project[:id], python_version, 'imageio', 'conda', '2.2.0', conda_channel)
            expect_status(201)

            get_library_commands(@project[:id], python_version, 'imageio')
            expect_status(200)
            expect(json_body[:count]).to be == 1

            wait_for do
              CondaCommands.find_by(project_id: @project[:id]).nil?
            end

            get_library_commands(@project[:id], python_version, 'imageio')
            expect_status(200)
            expect(json_body[:count]).to be == 0

            install_library(@project[:id], python_version, 'tflearn', 'pip', '0.3.2', conda_channel)
            expect_status(201)

            wait_for do
              CondaCommands.find_by(project_id: @project[:id]).nil?
            end
          end

          it 'list libraries' do
            @project = create_env_and_update_project(@project, python_version)
            list_libraries(@project[:id], python_version)

            tflearn_library = json_body[:items].detect { |library| library[:library] == "tflearn" }
            tensorflow_library = json_body[:items].detect { |library| library[:library] == "tensorflow" }
            hops_library = json_body[:items].detect { |library| library[:library] == "hops" }
            imageio_library = json_body[:items].detect { |library| library[:library] == "imageio" }

            expect(tflearn_library[:packageManager]).to eq ("PIP")
            expect(tflearn_library[:version]).to eq ("0.3.2")

            expect(hops_library[:packageManager]).to eq ("PIP")

            expect(imageio_library[:packageManager]).to eq("CONDA")
            expect(imageio_library[:version]).to eq ("2.2.0")

          end

          it 'uninstall libraries' do
            @project = create_env_and_update_project(@project, python_version)
            uninstall_library(@project[:id], python_version, 'imageio')
            expect_status(204)

            wait_for do
              CondaCommands.find_by(project_id: @project[:id]).nil?
            end
          end

          it 'export environment' do
            @project = create_env_and_update_project(@project, python_version)
            export_env(@project[:id], python_version)
            expect_status(200)

            wait_for do
              CondaCommands.find_by(project_id: @project[:id]).nil?
            end
          end

          it 'remove env' do
            if not conda_exists(python_version)
              skip "Anaconda is not installed in the machine or test is run locally"
            end
            @project = create_env_and_update_project(@project, python_version)
            # Install a library to create the new environment
            install_library(@project[:id], python_version, 'dropbox', 'conda', '10.2.0', conda_channel)
            expect_status(201)
            # Wait until library is installed
            wait_for do
              CondaCommands.find_by(project_id: @project[:id]).nil?
            end
            @project = get_project_by_name(@project[:projectname])
            non_versioned_project_image = @project.docker_image.rpartition('.').first
            delete_env(@project[:id], python_version)
            expect_status(204)

            wait_for do
              CondaCommands.find_by(project_id: @project[:id]).nil?
            end
            # Sleep so that kagent has enough time to process system command to cleanup the docker images
            sleep(15)
            # Check if project docker images were removed by kagent
            expect(check_if_img_exists_locally(non_versioned_project_image + ".0")).to be false
            expect(check_if_img_exists_locally(non_versioned_project_image + ".1")).to be false

            # Check that docker registry does not contain the image tags
            expect(image_in_registry(@project.projectname.downcase, non_versioned_project_image.split(":")[1])).to be false
          end

          it 'clean up env of deleted project' do
            if not conda_exists(python_version)
              skip "Anaconda is not installed in the machine or test is run locally"
            end
            projectname = "project_#{short_random_id}"
            project = create_project_by_name(projectname)
            project = create_env_and_update_project(project, python_version)
            # Install a library to create the new environment
            install_library(project[:id], python_version, 'dropbox', 'conda', '10.2.0', conda_channel)
            expect_status(201)
            project = get_project_by_name(project[:projectname])
            non_versioned_project_image = project.docker_image.rpartition('.').first
            wait_for do
              CondaCommands.find_by(project_id: project[:id]).nil?
            end
            delete_project(project)
            # Wait for garbage collection
            sleep(20)
            wait_for do
              CondaCommands.find_by(project_id: project[:id]).nil?
            end
            # Check if project docker images were removed by kagent
            expect(check_if_img_exists_locally(non_versioned_project_image + ".0")).to be false
            expect(check_if_img_exists_locally(non_versioned_project_image + ".1")).to be false

            # Check that docker registry does not contain the image tags
            expect(image_in_registry(project.projectname.downcase, non_versioned_project_image.split(":")[1])).to be false
          end

          it 'destroy anaconda should not delete base docker image' do
            create_env(@project, python_version)
            expect_status(201)
            if not conda_exists(python_version)
              skip "Anaconda is not installed in the machine or test is run locally"
            end

            # Enabling anaconda will not create an environment yet
            expect(check_if_img_exists_locally("python36:" + getVar('hopsworks_version').value)).to be true

            delete_env(@project[:id], python_version)
            expect_status(204)
            wait_for do
              CondaCommands.find_by(project_id: @project[:id]).nil?
            end
            expect(check_if_img_exists_locally("python36:" + getVar('hopsworks_version').value)).to be true
          end

          it 'create environment from yml with jupyter install true' do
            upload_yml
            delete_env(@project[:id], python_version)
            create_env_yml(@project[:id], "/Projects/#{@project[:projectname]}/Resources/environment_cpu.yml", true)
            expect_status(201)

            wait_for do
              CondaCommands.find_by(project_id: @project[:id]).nil?
            end

            @project = get_project_by_name(@project[:projectname])
            expect(python_version).to eq "3.6"
          end

          it 'create environment from yml with jupyter install false' do
            delete_env(@project[:id], python_version)
            create_env_yml(@project[:id], "/Projects/#{@project[:projectname]}/Resources/environment_cpu.yml", false)
            expect_status(201)

            wait_for do
              CondaCommands.find_by(project_id: @project[:id]).nil?
            end

            @project = get_project_by_name(@project[:projectname])
            expect(python_version).to eq "3.6"
          end
        end
      end
    end
  end
end
