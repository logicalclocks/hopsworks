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

  def wait_for
    timeout = 1800
    start = Time.now
    x = yield
    until x
      if Time.now - start > timeout
        raise "Timed out waiting for Anaconda to finish. Timeout #{timeout} sec"
      end
      sleep(1)
      x = yield
    end
  end

  module_function :wait_for

  def conda_exists
    conda_var = Variables.find_by(id: "anaconda_dir")
    if not conda_var
      return false
    end
    @conda_bin = File.join(conda_var.value, 'bin', 'conda')
    File.exists?(@conda_bin)
  end

  def upload_yml
    chmod_local_dir("#{ENV['PROJECT_DIR']}".gsub("/hopsworks", ""), 701, false)
    chmod_local_dir("#{ENV['PROJECT_DIR']}/tools", 777)
    copy_from_local("#{ENV['PROJECT_DIR']}/tools/conda/python3.6.yml",
                    "/Projects/#{@project[:projectname]}/Resources/environment_cpu.yml", @user[:username],
                    "#{@project[:projectname]}__Resources", 750, "#{@project[:projectname]}")
  end

  def get_conda_envs_locally
    cmd = "#{@conda_bin} env list --json"
    Open3.popen3(cmd) do |_, stdout, _, _|
      JSON.parse(stdout.read)
    end
  end

  def check_if_env_exists_locally(env_name)
    local_envs = get_conda_envs_locally
    result = local_envs['envs'].select{|x| x.include? env_name}
    not result.empty?
  end

  def trigger_conda_gc
    tmp = Dir.tmpdir()
    trigger_file = File.join(tmp, 'trigger_conda_gc')
    File.open(trigger_file, "w") { |file| file.puts "1"}
    File.chmod(0777, trigger_file)
  end

  def delete_env(projectId, version)
    delete "#{ENV['HOPSWORKS_API']}/project/#{projectId}/python/environments/#{version}"
  end

  def create_env(project, version)
    project = get_project_by_name(project[:projectname])
    if not (project[:python_version].nil? or project[:python_version].empty?)
      delete_env(project[:id], version)
    end
    post "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/python/environments/#{version}?action=create"
  end

  def list_envs(projectId)
    get "#{ENV['HOPSWORKS_API']}/project/#{projectId}/python/environments"
  end

  def create_env_and_update_project(project, version)
    project = get_project_by_name(project[:projectname])
    if project[:python_version].nil? or project[:python_version].empty?
      create_env(project, version)
      expect_status(201)
      get_project_by_name(project[:projectname]) #get project from db with updated python version
    else
      project
    end
  end

  def create_env_yml(projectId, allYmlPath, cpuYmlPath, gpuYmlPath, installJupyter)
    post "#{ENV['HOPSWORKS_API']}/project/#{projectId}/python/environments",
         {allYmlPath: allYmlPath, cpuYmlPath: cpuYmlPath, gpuYmlPath: gpuYmlPath, installJupyter: installJupyter}
  end

  def export_env(projectId, version)
    post "#{ENV['HOPSWORKS_API']}/project/#{projectId}/python/environments/#{version}?action=export"
  end

  def list_libraries(projectId, version)
    get "#{ENV['HOPSWORKS_API']}/project/#{projectId}/python/environments/#{version}/libraries"
  end

  def install_library(projectId, version, lib, package_manager, lib_version, machine, channel)
    post "#{ENV['HOPSWORKS_API']}/project/#{projectId}/python/environments/#{version}/libraries/#{lib}?package_manager=#{package_manager}&version=#{lib_version}&machine=#{machine}&channel=#{channel}"
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

  def get_library_commands(projectId, version, lib)
    get "#{ENV['HOPSWORKS_API']}/project/#{projectId}/python/environments/#{version}/libraries/#{lib}/commands"
  end

end
