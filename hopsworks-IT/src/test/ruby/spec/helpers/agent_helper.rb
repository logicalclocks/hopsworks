=begin
 This file is part of Hopsworks
 Copyright (C) 2019, Logical Clocks AB. All rights reserved
 
 Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 the GNU Affero General Public License as published by the Free Software Foundation,
 either version 3 of the License, or (at your option) any later version.
 
 Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 PURPOSE.  See the GNU Affero General Public License for more details.
 
 You should have received a copy of the GNU Affero General Public License along with this program.
 If not, see <https://www.gnu.org/licenses/>.
=end

require 'net/ssh'

module AgentHelper
    @@KAGENT_STATUS = "systemctl is-active kagent"
    @@KAGENT_START = "sudo systemctl start kagent"
    @@KAGENT_STOP = "sudo systemctl stop kagent"
    @@SPARKHISTORYSERVER_STATUS = "systemctl is-active sparkhistoryserver"
    @@SPARKHISTORYSERVER_START = "systemctl start sparkhistoryserver"
    @@SPARKHISTORYSERVER_STOP = "systemctl stop sparkhistoryserver"

    @@username = ENV.fetch('REMOTE_SSH_USER', 'vagrant')
    @@port = ENV.fetch('REMOTE_SSH_PORT', '22')
    @@password = ENV.fetch('REMOTE_SSH_PASSWORD', 'vagrant')

    def is_kagent_running(hostname)
        output = execute_remotely hostname, @@KAGENT_STATUS
        output.strip.eql? "active"
    end

    def kagent_start(hostname)
        execute_remotely hostname, @@KAGENT_START
    end

    def kagent_stop(hostname)
        execute_remotely hostname, @@KAGENT_STOP
    end

    def sparkhistoryserver_start(hostname)
        execute_remotely hostname, @@SPARKHISTORYSERVER_START
    end

    def sparkhistoryserver_stop(hostname)
        execute_remotely hostname, @@SPARKHISTORYSERVER_STOP
    end

    def is_sparkhistoryserver_running(hostname)
      output = execute_remotely hostname, @@SPARKHISTORYSERVER_STATUS
      output.strip.eql? "active"
    end

    def execute_remotely(hostname, command)
        Net::SSH.start(hostname, @@username, :port => @@port,
            :password => @@password, :verify_host_key => :never) do |ssh|
            output = ssh.exec!(command)
        end
    end
end
