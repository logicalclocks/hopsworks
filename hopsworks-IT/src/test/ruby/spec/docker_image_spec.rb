=begin
 This file is part of Hopsworks
 Copyright (C) 2021, Logical Clocks AB. All rights reserved

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
  let(:python_version) {'3.7'}
  before :all do
    @managed_docker_registry = getVar('managed_docker_registry').value
    @cloud = getVar('cloud').value
  end
  after :all do
    clean_all_test_projects(spec: "docker_image")
    setVar('managed_docker_registry', @managed_docker_registry)
    setVar('cloud', @cloud)
  end
  context 'docker image and tags' do
    context 'default' do
      before :all do
        setVar('managed_docker_registry', "false")
        setVar('cloud', "")
        with_valid_project
      end
      it 'default docker base image format should be recognized as preinstalled' do
        set_docker_image(@project, "base:2.1.0")
        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}"
        expect(json_body[:isPreinstalledDockerImage]).to be true
      end

      it 'default docker python37 image format should be recognized as preinstalled' do
        set_docker_image(@project, "python37:2.1.0")
        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}"
        expect(json_body[:isPreinstalledDockerImage]).to be true
      end

      it 'default project unique docker image and tag format should not be recognized as preinstalled' do
        set_docker_image(@project, @project[:projectname].downcase + ":1611136370296-2.1.0.0")
        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}"
        expect(json_body[:isPreinstalledDockerImage]).to be false
      end
    end
    context 'ecr' do
      before :all do
        setVar('managed_docker_registry', "true")
        setVar('cloud', "AWS")
        with_valid_project
      end
      it 'ecr docker base image format should be recognized as preinstalled' do
        set_docker_image(@project, "base:2.1.0")
        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}"
        expect(json_body[:isPreinstalledDockerImage]).to be true
      end

      it 'ecr docker python37 image format should be recognized as preinstalled' do
        set_docker_image(@project, "base:python37_2.1.0")
        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}"
        expect(json_body[:isPreinstalledDockerImage]).to be true
      end

      it 'ecr project unique docker image and tag format should not be recognized as preinstalled' do
        set_docker_image(@project, "base:" + @project[:projectname].downcase + "_" + "1611136370296-2.1.0.0")
        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}"
        expect(json_body[:isPreinstalledDockerImage]).to be false
      end
    end
  end
end