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
  describe '#Conda basic operations'  do
    after (:all){clean_projects}

    let(:num_hosts) {Host.count}
    let(:conda_channel) {Variables.find_by(id: "conda_default_repo").value}

    describe "#create" do
      context 'without authentication' do
        before :all do
          with_valid_project
          reset_session
        end
        it "not authenticated" do
          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps/enable/2.7/true"
          expect_json(errorCode: 200003)
          expect_status(401)
        end
      end

      context 'with authentication' do
        before :all do
          with_valid_project
        end

        it 'destroy anaconda should not delete base environments' do
          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps/enable/2.7/true"
          expect_status(200)
          if not conda_exists
            skip "Anaconda is not installed in the machine or test is run locally"
          end

          # Enabling anaconda will not create an environment yet
          expect(check_if_env_exists_locally(@project[:projectname])).to be false

          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps/destroyAnaconda"
          expect_status(200)
          wait_for do
            CondaCommands.find_by(proj: @project[:projectname]).nil?
          end
          expect(check_if_env_exists_locally("python27")).to be true
          expect(check_if_env_exists_locally("python36")).to be true
        end
                
        it 'enable anaconda' do
          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps/enable/2.7/true"
          expect_status(200)
          if not conda_exists
            skip "Anaconda is not installed in the machine or test is run locally"
          end

          # Enabling anaconda will not create an environment yet
          expect(check_if_env_exists_locally(@project[:projectname])).to be false
          # There should be no CondaCommands in the database
          expect(CondaCommands.find_by(proj: @project[:projectname])).to be nil

          # Install a library to create the new environment
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps/install",
               {lib: "requests", version: "2.20.0", channelUrl: "#{conda_channel}", installType: "CONDA", machineType: "CPU"}
          expect_status(200)
          wait_for do
            CondaCommands.find_by(proj: @project[:projectname]).nil?
          end
          expect(check_if_env_exists_locally(@project[:projectname])).to be true
        end

        it 'search libraries' do
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps/search",
               {lib: "dropbox", channelUrl: "#{conda_channel}", installType: "CONDA"}
          expect_status(200)
          expect(json_body.count).to be >= 1
          dropbox = json_body.detect { |library| library[:lib] == "dropbox" }
          expect(dropbox[:versions].count).to be >= 1
        end
        
        it 'GC stale Conda env' do
          if not conda_exists
            skip "Anaconda is not installed in the machine or test is run locally"
          end
        
          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps/enable/2.7/true"
          expect_status(200)
          
          # Install a library to create the new environment
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps/install",
               {lib: "paramiko", version: "2.4.2", channelUrl: "#{conda_channel}", installType: "CONDA", machineType: "CPU"}
          expect_status(200)
          wait_for do
            CondaCommands.find_by(proj: @project[:projectname]).nil?
          end
          expect(check_if_env_exists_locally(@project[:projectname])).to be true

          # Create a second project with Anaconda enabled
          project2 = create_project
          get "#{ENV['HOPSWORKS_API']}/project/#{project2[:id]}/pythonDeps/enable/2.7/true"
          expect_status(200)
          
          # Install a library to create the new environment                                                                  
          post "#{ENV['HOPSWORKS_API']}/project/#{project2[:id]}/pythonDeps/install",
               {lib: "paramiko", version: "2.4.2", channelUrl: "#{conda_channel}", installType: "CONDA", machineType: "CPU"}
          expect_status(200)
          wait_for do
            CondaCommands.find_by(proj: project2[:projectname]).nil?
          end
          expect(check_if_env_exists_locally(project2[:projectname])).to be true

          # Disable Anaconda for project2 directly in the database
          # so it does not send a command to kagent
          tmp_proj = Project.find_by(id: project2[:id])
          tmp_proj.conda = 0
          tmp_proj.save

          trigger_conda_gc
          sleep(15)
          expect(check_if_env_exists_locally(project2[:projectname])).to be false
        end

        it 'install libraries' do
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps/install",
               {lib: "imageio", version: "2.2.0", channelUrl: "#{conda_channel}", installType: "CONDA", machineType: "CPU"}
          expect_status(200)

          wait_for do
            CondaCommands.find_by(proj: @project[:projectname]).nil?
          end

          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps/install",
               {lib: "tflearn", version: "0.3.2", channelUrl: "PyPi", installType: "PIP", machineType: "ALL"}
          expect_status(200)

          wait_for do
            CondaCommands.find_by(proj: @project[:projectname]).nil?
          end
        end

        it 'list libraries' do
          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps/"

          tflearn_library = json_body.detect { |library| library[:lib] == "tflearn" }
          serving_library = json_body.detect { |library| library[:lib] == "tensorflow-serving-api" }
          hops_library = json_body.detect { |library| library[:lib] == "hops" }
          imageio_library = json_body.detect { |library| library[:lib] == "imageio" }

          expect(serving_library[:machineType]).to eq ("ALL")

          expect(tflearn_library[:machineType]).to eq ("ALL")
          expect(tflearn_library[:installType]).to eq ("PIP")
          expect(tflearn_library[:version]).to eq ("0.3.2")

          expect(hops_library[:machineType]).to eq ("ALL")
          expect(hops_library[:installType]).to eq ("PIP")

          expect(imageio_library[:machineType]).to eq("CPU")
          expect(imageio_library[:installType]).to eq("CONDA")
          expect(imageio_library[:version]).to eq ("2.2.0")

        end

        it 'uninstall libraries' do
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps/remove",
               {lib: "imageio", version: "2.2.0", channelUrl: "#{conda_channel}", installType: "CONDA", machineType: "CPU"}
          expect_status(200)

          wait_for do
            CondaCommands.find_by(proj: @project[:projectname]).nil?
          end
        end

        it 'export environment' do
          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps/export"
          expect_status(200)
        end

        it 'remove env' do
          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps/destroyAnaconda"
          expect_status(200)

          wait_for do
            CondaCommands.find_by(proj: @project[:projectname]).nil?
          end
          if not conda_exists
            skip "Anaconda is not installed in the machine or test is run locally"
          end
          expect(check_if_env_exists_locally(@project[:projectname])).to be false
        end

        it 'enable environment from yml' do
          skip "MMLSpark breaks this code"
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps/enableYml",
               {pythonKernelEnable: "true", allYmlPath: "/Projects/#{@project[:projectname]}/Resources/environment_cpu.yml", cpuYmlPath: "", gpuYmlPath: ""}
          expect_status(200)

          wait_for do
            CondaCommands.find_by(proj: @project[:projectname]).nil?
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
            put "#{ENV['HOPSWORKS_API']}/admin/hosts", {hostname: "hopsworks1.logicalclocks.com", condaEnabled: "false"}
          else
            put "#{ENV['HOPSWORKS_API']}/admin/hosts", {hostname: "hopsworks0.logicalclocks.com", condaEnabled: "false"}
          end
          expect_status(204)
        end

        it 'should fail to create an environment on the local machine - single vm' do
          if num_hosts > 1
            skip "Multi vm setup."
          end

          create_session(@user[:email], "Pass123")
          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps/enable/2.7/true"
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
          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps/enable/2.7/true"
          expect_status(200)
        end

        it 'should delete the environment from the other machines - multi vm' do
          if num_hosts == 1
            skip "Singe vm setup"
          end

          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps/destroyAnaconda"
          expect_status(200)
        end

        it 'should be able to re-enable conda on a host' do
          with_admin_session
          if num_hosts > 1
            # In case we have multi vms disable the one on which Hopsworks is not running
            put "#{ENV['HOPSWORKS_API']}/admin/hosts", {hostname: "hopsworks1.logicalclocks.com", condaEnabled: "true"}
          else
            put "#{ENV['HOPSWORKS_API']}/admin/hosts", {hostname: "hopsworks0.logicalclocks.com", condaEnabled: "true"}
          end
          expect_status(204)
        end

        it 'should be able to create an environment' do
          create_session(@user[:email], "Pass123")
          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps/enable/2.7/true"
          expect_status(200)
        end
      end
    end

    describe "#Library installation not executed on non-conda hosts" do
      context 'with admin rights' do
        before :all do
          with_valid_project
        end

        it 'should create an environment ' do
          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps/enable/2.7/true"
          expect_status(200)
        end

        it 'should be able to disable conda on a host' do
          with_admin_session
          if num_hosts > 1
            # In case we have multi vms disable the one on which Hopsworks is not running
            put "#{ENV['HOPSWORKS_API']}/admin/hosts", {hostname: "hopsworks1.logicalclocks.com", condaEnabled: "false"}
          else
            put "#{ENV['HOPSWORKS_API']}/admin/hosts", {hostname: "hopsworks0.logicalclocks.com", condaEnabled: "false"}
          end
          expect_status(204)
        end

        it 'should fail to install a library' do
          create_session(@user[:email], "Pass123")
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps/install",
               {lib: "imageio", version: "2.2.0", channelUrl: "#{conda_channel}", installType: "CONDA", machineType: "CPU"}

          if num_hosts == 1
            #  If single VM there are no hosts on which to install the library. Hopsworks returns 412
            expect_status(503)
          else
            # If it is a multi vm there are hosts to install the library.
            expect_status(200)
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
            put "#{ENV['HOPSWORKS_API']}/admin/hosts", {hostname: "hopsworks1.logicalclocks.com", condaEnabled: "true"}
          else
            put "#{ENV['HOPSWORKS_API']}/admin/hosts", {hostname: "hopsworks0.logicalclocks.com", condaEnabled: "true"}
          end
          expect_status(204)
        end

        it 'should be able to install a library' do
          create_session(@user[:email], "Pass123")
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps/install",
               {lib: "dropbox", version: "9.0.0", channelUrl: "#{conda_channel}", installType: "CONDA", machineType: "CPU"}
          expect_status(200)

          # Check that the command has been register into the table and it will be eventually sent to the agent
          expect(CondaCommands.find_by(proj: @project[:projectname])).not_to be nil
        end
      end
    end
  end
end
