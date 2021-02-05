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

require 'open3'
require 'json'
require 'tmpdir'

module CondaHelper

  #the registry url and port should not change, if they do fix the tests
  @@registry = "registry.service.consul:4443"

  def wait_for(timeout=600, error_msg="Timed out waiting for Anaconda to finish.")
    start = Time.now
    x = yield
    until x
      if Time.now - start > timeout
        raise "#{error_msg} Timeout #{timeout} sec"
      end
      sleep(1)
      x = yield
    end
  end

  module_function :wait_for

  def conda_exists(python_version)
    image_name = @@registry + "/python" + python_version.gsub(".","") + ":" + getVar('hopsworks_version').value
    system("docker pull " + image_name + "> /dev/null 2>&1")
    system("docker inspect --type=image " + image_name + "> /dev/null 2>&1")
  end

  def image_in_registry(docker_image_name, tag)
    # Handle only local docker registry for now
    if getVar('managed_docker_registry').value.eql? "false"
      system("docker exec registry ls -l /var/lib/registry/docker/registry/v2/repositories/#{docker_image_name}/_manifests/tags/#{tag}.0")
    end
  end

  def upload_wheel
      chmod_local_dir("#{ENV['PROJECT_DIR']}".gsub("/hopsworks", ""), 701, false)
      chmod_local_dir("#{ENV['PROJECT_DIR']}", 777, true)
      copy_from_local("#{ENV['PROJECT_DIR']}/hopsworks-IT/src/test/ruby/spec/auxiliary/lark_parser-0.10.1-py2.py3-none-any.whl",
                      "/Projects/#{@project[:projectname]}/Resources/lark_parser-0.10.1-py2.py3-none-any.whl", @user[:username],
                      "#{@project[:projectname]}__Resources", 750, "#{@project[:projectname]}")
  end

  def upload_yml
      copy_from_local("#{ENV['PROJECT_DIR']}/hopsworks-IT/src/test/ruby/spec/auxiliary/python3.7.yml",
                    "/Projects/#{@project[:projectname]}/Resources/environment_cpu.yml", @user[:username],
                    "#{@project[:projectname]}__Resources", 750, "#{@project[:projectname]}")
  end

  def upload_requirements
      copy_from_local("#{ENV['PROJECT_DIR']}/hopsworks-IT/src/test/ruby/spec/auxiliary/requirements.txt",
                    "/Projects/#{@project[:projectname]}/Resources/requirements.txt", @user[:username],
                    "#{@project[:projectname]}__Resources", 750, "#{@project[:projectname]}")
  end

  def upload_environment
      copy_from_local("#{ENV['PROJECT_DIR']}/hopsworks-IT/src/test/ruby/spec/auxiliary/environment.yml",
                    "/Projects/#{@project[:projectname]}/Resources/environment.yml", @user[:username],
                    "#{@project[:projectname]}__Resources", 750, "#{@project[:projectname]}")
  end

  def get_conda_envs_locally
    cmd = "#{@conda_bin} env list --json"
    Open3.popen3(cmd) do |_, stdout, _, _|
      JSON.parse(stdout.read)
    end
  end

  def get_project_env_by_id(id)
    PythonEnvironment.find_by(project_id: "#{id}")
  end

  def check_if_img_exists_locally(docker_image_name)
    image_name = @@registry + "/" + docker_image_name
    return system("docker inspect --type=image " + image_name + "> /dev/null 2>&1")
  end

  def delete_env(projectId, version)
    delete "#{ENV['HOPSWORKS_API']}/project/#{projectId}/python/environments/#{version}"
  end

  def wait_for_sync
    wait_for do
      CondaCommands.where(["project_id = ? and op = ?", @project[:id], "SYNC_BASE_ENV"]).empty?
    end
  end

  def create_env(project, version, wait_for_sync_complete=true)
    env = get_project_env_by_id(project[:id])
    project = get_project_by_name(project[:projectname])
    if not env.nil?
      delete_env(project[:id], version)
    end
    project = post "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/python/environments/#{version}?action=create"
    if wait_for_sync_complete
      wait_for_sync
    end
    project
  end

  def list_envs(projectId)
    get "#{ENV['HOPSWORKS_API']}/project/#{projectId}/python/environments"
  end

  def create_env_and_update_project(project, version)
    env = get_project_env_by_id(project[:id])
    project = get_project_by_name(project[:projectname])
    if env.nil?
      create_env(project, version)
      expect_status(201)
      wait_for_sync
      get_project_by_name(project[:projectname]) #get project from db with updated python version
    else
      wait_for_sync
      project
    end
  end

  def create_env_from_file(projectId, path, installJupyter)
    post "#{ENV['HOPSWORKS_API']}/project/#{projectId}/python/environments",
         {path: path, installJupyter: installJupyter}
  end

  def export_env(projectId, version)
    post "#{ENV['HOPSWORKS_API']}/project/#{projectId}/python/environments/#{version}?action=export"
  end

  def list_libraries(projectId, version)
    get "#{ENV['HOPSWORKS_API']}/project/#{projectId}/python/environments/#{version}/libraries"
  end

  def install_library(projectId, version, lib, package_source, lib_version, channel, dependency_url=nil)

    lib_spec = {
      "version": lib_version,
      "packageSource": package_source,
      "channelUrl": channel,
      "dependencyUrl": dependency_url
    }

    post "#{ENV['HOPSWORKS_API']}/project/#{projectId}/python/environments/#{version}/libraries/#{lib}", lib_spec
  end

  def search_library(projectId, version, package_manager, lib, conda_channel="")
    request_url = "#{ENV['HOPSWORKS_API']}/project/#{projectId}/python/environments/#{version}/libraries/#{package_manager}?query=#{lib}"
    if not conda_channel.empty?
      request_url += "&channel=#{conda_channel}"
    end
    get request_url
  end

  def uninstall_library(projectId, version, lib)
    delete "#{ENV['HOPSWORKS_API']}/project/#{projectId}/python/environments/#{version}/libraries/#{lib}"
  end

  def get_env_commands(projectId, version)
    get "#{ENV['HOPSWORKS_API']}/project/#{projectId}/python/environments/#{version}/commands"
  end

    def get_env_conflicts(projectId, version, query="")
      get "#{ENV['HOPSWORKS_API']}/project/#{projectId}/python/environments/#{version}/conflicts" + query
    end

  def get_library_commands(projectId, version, lib)
    get "#{ENV['HOPSWORKS_API']}/project/#{projectId}/python/environments/#{version}/libraries/#{lib}/commands"
  end

end
