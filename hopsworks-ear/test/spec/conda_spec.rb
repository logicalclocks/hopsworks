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

describe '#Conda basic operations'  do
  after (:all){clean_projects}
  describe "#create" do
    context 'without authentication' do
      before :all do
        with_valid_project
        reset_session
      end
      it "not authenticated" do
        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps/enable/2.7/true"
        expect_json(errorMsg: "Client not authorized for this invocation")
        expect_status(401)
      end
    end

    context 'with authentication' do
      before :all do
        with_valid_project
      end
      it 'enable anaconda' do
        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps/enable/2.7/true"
        expect_status(200)

        wait_for do
          response = get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps/"
          hops_library = json_body.detect { |library| library[:lib] == "hops" }
          !hops_library.nil?
        end
      end

      it 'install libraries' do
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps/install", {lib: "imageio", version: "2.2.0", channelUrl: "defaults", installType: "CONDA", machineType: "CPU"}
        expect_status(200)

        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps/install", {lib: "tflearn", version: "0.3.2", channelUrl: "PyPi", installType: "PIP", machineType: "ALL"}
        expect_status(200)
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

        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps/remove", {lib: "imageio", version: "2.2.0", channelUrl: "defaults", installType: "CONDA", machineType: "CPU"}
        expect_status(200)

        wait_for do
            get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps"
            imageio_library = json_body.detect { |library| library[:lib] == "imageio" }
            imageio_library.nil?
        end

      end

      it 'export environment' do

        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps/export"
        expect_status(200)

      end

      it 'remove env' do

        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps/destroyAnaconda"
        expect_status(200)

        # Sleep so Kagent gets time to pickup command
        sleep(20)

      end

      it 'enable environment from yml' do

        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps/enableYml", {pythonKernelEnable: "true", allYmlPath: "/Projects/#{@project[:projectname]}/Resources/environment_cpu.yml", cpuYmlPath: "", gpuYmlPath: ""}
        expect_status(200)

        wait_for do
          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps"
          hops_library = json_body.detect { |library| library[:lib] == "hops" }
          hops_library.nil?
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
        put "#{ENV['HOPSWORKS_API']}/admin/hosts", {hostname: "hopsworks0", condaEnabled: "false"}
        expect_status(204)
      end

      it 'should fail to create an environment (single machine test)' do
        create_session(@user[:email], "Pass123")
        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps/enable/2.7/true"
        expect_status(500)
      end

      it 'should not have created any conda_commands in the db' do
        expect(CondaCommands.find_by(proj: @project[:projectname])).to be nil
      end

      it 'should be able to re-enable conda on a host' do
        with_admin_session
        put "#{ENV['HOPSWORKS_API']}/admin/hosts", {hostname: "hopsworks0", condaEnabled: "true"}
        expect_status(204)
      end

      it 'should be able to create an environment' do
        create_session(@user[:email], "Pass123")
        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps/enable/2.7/true"
        expect_status(200)

        wait_for do
          response = get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps/"
          hops_library = json_body.detect { |library| library[:lib] == "hops" }
          !hops_library.nil?
        end
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

        wait_for do
          response = get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps/"
          hops_library = json_body.detect { |library| library[:lib] == "hops" }
          !hops_library.nil?
        end
      end

      it 'should be able to disable conda on a host' do
        with_admin_session
        put "#{ENV['HOPSWORKS_API']}/admin/hosts", {hostname: "hopsworks0", condaEnabled: "false"}
        expect_status(204)
      end

      it 'should fail to install a library' do
        create_session(@user[:email], "Pass123")
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps/install", {lib: "imageio", version: "2.2.0", channelUrl: "defaults", installType: "CONDA", machineType: "CPU"}
        expect_status(404)
      end

      it 'should not have created any conda_commands in the db' do
        expect(CondaCommands.find_by(proj: @project[:projectname])).to be nil
      end

      it 'should be able to re-enable conda on a host' do
        with_admin_session
        put "#{ENV['HOPSWORKS_API']}/admin/hosts", {hostname: "hopsworks0", condaEnabled: "true"}
        expect_status(204)
      end

      it 'should be able to install a library' do
        create_session(@user[:email], "Pass123")
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps/install", {lib: "imageio", version: "2.2.0", channelUrl: "defaults", installType: "CONDA", machineType: "CPU"}
        expect_status(200)

        # Check that the command has been register into the table and it will be eventually sent to the agent
        expect(CondaCommands.find_by(proj: @project[:projectname])).not_to be nil
      end
    end
  end
end
