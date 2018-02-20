=begin
This file is part of HopsWorks

Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.

HopsWorks is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

HopsWorks is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
=end

describe 'conda' do
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

        # wait until anaconda is enabled, don't have a clue how long it will take...
        # 4 minutes is sufficient to get a cup of coffee
        sleep 300

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
        hops_library = json_body.detect { |library| library[:lib] == "hops" }
        imageio_library = json_body.detect { |library| library[:lib] == "imageio" }

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

        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps/remove", {lib: "tflearn", version: "0.3.2", channelUrl: "PyPi", installType: "PIP", machineType: "ALL"}
        expect_status(200)

        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps/remove", {lib: "imageio", version: "2.2.0", channelUrl: "defaults", installType: "CONDA", machineType: "CPU"}
        expect_status(200)

        sleep 60

        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps"

        tflearn_library = json_body.detect { |library| library[:lib] == "tflearn" }
        imageio_library = json_body.detect { |library| library[:lib] == "imageio" }
        hops_library = json_body.detect { |library| library[:lib] == "hops" }

        expect(tflearn_library).not_to be_present
        expect(imageio_library).not_to be_present
        expect(hops_library).to be_present

      end

      it 'remove env' do

        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps/destroyAnaconda"
        expect_status(200)

      end
    end
  end
end
