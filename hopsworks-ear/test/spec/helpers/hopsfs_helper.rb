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
module HopsFSHelper

  @@hdfs_user = Variables.find_by(id: "hdfs_user").value
  @@hadoop_home = Variables.find_by(id: "hadoop_dir").value

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

  def test_dir(path)
    system "sudo su #{@@hdfs_user} /bin/bash -c \"#{@@hadoop_home}/bin/hdfs dfs test -d #{path}\""
    return $?.exitstatus == 0
  end

  def copy(src, dest, owner, group, mode)
    system "sudo su #{@@hdfs_user} /bin/bash -c \"#{@@hadoop_home}/bin/hdfs dfs -cp #{src} #{dest}\""
    if $?.exitstatus > 0
      raise "Failed to chmod directory: #{dest} "
    end

    system "sudo su #{@@hdfs_user} /bin/bash -c \"#{@@hadoop_home}/bin/hdfs dfs -chown -R #{owner}:#{group} #{dest}\""
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
end