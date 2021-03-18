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

require 'open3'

module HopsHelper

  @@hdfs_user = Variables.find_by(id: "hdfs_user").value
  @@rmyarn_user = Variables.find_by(id: "rmyarn_user").value
  @@yarn_user = Variables.find_by(id: "yarn_user").value
  @@hadoop_home = Variables.find_by(id: "hadoop_dir").value
  @@hopsworks_user = Variables.find_by(id: "hopsworks_user").value

  def chmod_hdfs(path, permission)
    system "sudo su #{@@hdfs_user} /bin/bash -c \"#{@@hadoop_home}/bin/hdfs dfs -chmod #{permission} #{path}\""
    if $?.exitstatus > 0
      raise "Failed to chmod: #{permission} directory: #{path}"
    end
  end

  def mkdir(path, owner, group, mode)
    system "sudo su #{@@hdfs_user} /bin/bash -c \"#{@@hadoop_home}/bin/hdfs dfs -mkdir -p #{path}\""
    if $?.exitstatus > 0
      raise "Failed to create directory: #{path}"
    end
    system "sudo su #{@@hdfs_user} /bin/bash -c \"#{@@hadoop_home}/bin/hdfs dfs -chown #{owner}:#{group} #{path}\""
    if $?.exitstatus > 0
      raise "Failed to chown directory: #{path} to #{user}:#{group}"
    end
    system "sudo su #{@@hdfs_user} /bin/bash -c \"#{@@hadoop_home}/bin/hdfs dfs -chmod #{mode} #{path}\""
    if $?.exitstatus > 0
      raise "Failed to chmod: #{mode} directory: #{path} "
    end
  end

  def chmod_local_dir(src, mode, recursive=true)
    if recursive
      system "sudo /bin/bash -c  \"chmod -R #{mode} #{src}\""
    else
      system "sudo /bin/bash -c  \"chmod #{mode} #{src}\""
    end
    if $?.exitstatus > 0
      raise "Failed chmod: #{mode} local dir: #{src}"
    end
  end

  def copy_from_local(src, dest, owner, group, mode, name)
    system "sudo su #{@@hdfs_user} /bin/bash -c \"#{@@hadoop_home}/bin/hdfs dfs -copyFromLocal -f #{src} #{dest}\""
    if $?.exitstatus > 0
      raise "Failed to copy: #{src} to #{dest} "
    end

    system "sudo su #{@@hdfs_user} /bin/bash -c \"#{@@hadoop_home}/bin/hdfs dfs -chown -R #{name}__#{owner}:#{group} #{dest}\""
    if $?.exitstatus > 0
      raise "Failed to chown: #{dest} to #{owner}:#{group}"
    end

    system "sudo su #{@@hdfs_user} /bin/bash -c \"#{@@hadoop_home}/bin/hdfs dfs -chmod -R #{mode} #{dest}\""
    if $?.exitstatus > 0
      raise "Failed to chmod: #{mode} directory: #{dest} "
    end
  end

  def test_dir(path)
    system "sudo su #{@@hdfs_user} /bin/bash -c \"#{@@hadoop_home}/bin/hdfs dfs -test -d #{path}\""
    return $?.exitstatus == 0
  end

  def test_file(path)
    system "sudo su #{@@hdfs_user} /bin/bash -c \"#{@@hadoop_home}/bin/hdfs dfs -test -e #{path}\""
    return $?.exitstatus == 0
  end

  def check_log_dir_has_files(hdfs_user, application_id)
    cmd = "sudo su #{@@hdfs_user} /bin/bash -c \"#{@@hadoop_home}/bin/hdfs dfs -count /user/#{@@yarn_user}/logs/#{hdfs_user}/logs/#{application_id}\""
    file_count = ""
    Open3.popen3(cmd) do |_, stdout, _, _|
      file_count = stdout.read
    end
    file_count.split(" ")[1].to_i > 0
  end

  def copy(src, dest, owner, group, mode, name)
    system "sudo su #{@@hdfs_user} /bin/bash -c \"#{@@hadoop_home}/bin/hdfs dfs -cp #{src} #{dest}\""
    if $?.exitstatus > 0
      raise "Failed to copy directory: #{src} to #{dest}"
    end

    system "sudo su #{@@hdfs_user} /bin/bash -c \"#{@@hadoop_home}/bin/hdfs dfs -chown -R #{name}__#{owner}:#{group} #{dest}\""
    if $?.exitstatus > 0
      raise "Failed to chown directory: #{dest} to #{owner}:#{group}"
    end

    system "sudo su #{@@hdfs_user} /bin/bash -c \"#{@@hadoop_home}/bin/hdfs dfs -chmod -R #{mode} #{dest}\""
    if $?.exitstatus > 0
      raise "Failed to chmod: #{mode} directory: #{dest} "
    end
  end

  def rm(path)
    system "sudo su #{@@hdfs_user} /bin/bash -c \"#{@@hadoop_home}/bin/hdfs dfs -rm #{path}\""
    if $?.exitstatus > 0
      raise "Failed to rm directory: #{path} "
    end
  end

  def get_storage_policy(path)
    cmd = "sudo su #{@@hdfs_user} /bin/bash -c \"#{@@hadoop_home}/bin/hdfs storagepolicies -getStoragePolicy -path #{path}\""
    policy = ""
    Open3.popen3(cmd) do |_, stdout, _, _|
      policy = stdout.read
    end
    policy
  end

  def getHopsworksUser
    @@hopsworks_user
  end

  def touchz(path, owner, group)
    system "sudo su #{@@hdfs_user} /bin/bash -c \"#{@@hadoop_home}/bin/hdfs dfs -touchz #{path}\""
    if $?.exitstatus > 0
      raise "Failed to create file: #{path}"
    end
    system "sudo su #{@@hdfs_user} /bin/bash -c \"#{@@hadoop_home}/bin/hdfs dfs -chown #{owner}:#{group} #{path}\""
    if $?.exitstatus > 0
      raise "Failed to change owner: #{path} to #{owner}:#{group}"
    end
  end

  ################################################### YARN helpers #####################################################

  def get_application_state(app_id, states)
    cmd = "sudo su #{@@rmyarn_user} /bin/bash -c \"#{@@hadoop_home}/bin/yarn app -list -appStates #{states} | grep #{app_id}\""
    state = ""
    Open3.popen3(cmd) do |_, stdout, _, _|
      state = stdout.read
    end
    # state looks like
    # application_1600882170037_0008	      demo_job_5_jar	      Hopsworks-Yarn	demo_spark_deb57f91__deb57f97	   default	            KILLED	            KILLED	           100%	http://resourcemanager.service.consul:8088/cluster/app/application_1600882170037_0008
    state.split[5]
  end

  ######################################################################################################################

end