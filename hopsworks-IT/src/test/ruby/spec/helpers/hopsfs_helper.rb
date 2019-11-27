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

module HopsFSHelper

  @@hdfs_user = Variables.find_by(id: "hdfs_user").value
  @@hadoop_home = Variables.find_by(id: "hadoop_dir").value
  @@hopsworks_user = Variables.find_by(id: "hopsworks_user").value

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
      raise "Failed to chmod directory: #{path} "
    end
  end

  def chmod_local_dir(src, mode, recursive=true)
    if recursive
      system "sudo /bin/bash -c  \"chmod -R #{mode} #{src}\""
    else
      system "sudo /bin/bash -c  \"chmod #{mode} #{src}\""
    end
    if $?.exitstatus > 0
      raise "Failed chmod local dir: #{src} to #{mode} "
    end
  end

  def copy_from_local(src, dest, owner, group, mode, name)
    system "sudo su #{@@hdfs_user} /bin/bash -c \"#{@@hadoop_home}/bin/hdfs dfs -copyFromLocal #{src} #{dest}\""
    if $?.exitstatus > 0
      raise "Failed to copy: #{src} to #{dest} "
    end

    system "sudo su #{@@hdfs_user} /bin/bash -c \"#{@@hadoop_home}/bin/hdfs dfs -chown -R #{name}__#{owner}:#{group} #{dest}\""
    if $?.exitstatus > 0
      raise "Failed to chown: #{dest} to #{owner}:#{group}"
    end

    system "sudo su #{@@hdfs_user} /bin/bash -c \"#{@@hadoop_home}/bin/hdfs dfs -chmod -R #{mode} #{dest}\""
    if $?.exitstatus > 0
      raise "Failed to chmod: #{dest} "
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

  def copy(src, dest, owner, group, mode, name)
    system "sudo su #{@@hdfs_user} /bin/bash -c \"#{@@hadoop_home}/bin/hdfs dfs -cp #{src} #{dest}\""
    if $?.exitstatus > 0
      raise "Failed to chmod directory: #{dest} "
    end

    system "sudo su #{@@hdfs_user} /bin/bash -c \"#{@@hadoop_home}/bin/hdfs dfs -chown -R #{name}__#{owner}:#{group} #{dest}\""
    if $?.exitstatus > 0
      raise "Failed to chown directory: #{dest} to #{owner}:#{group}"
    end

    system "sudo su #{@@hdfs_user} /bin/bash -c \"#{@@hadoop_home}/bin/hdfs dfs -chmod -R #{mode} #{dest}\""
    if $?.exitstatus > 0
      raise "Failed to chmod directory: #{dest} "
    end
  end

  def rm(path)
    system "sudo su #{@@hdfs_user} /bin/bash -c \"#{@@hadoop_home}/bin/hdfs dfs -rm #{path}\""
    if $?.exitstatus > 0
      raise "Failed to chmod directory: #{path} "
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
      raise "Failed to change owner: #{path}"
    end
  end
end