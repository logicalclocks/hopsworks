=begin
 This file is part of Hopsworks
 Copyright (C) 2020, Logical Clocks AB. All rights reserved

 Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 the GNU Affero General Public License as published by the Free Software Foundation,
 either version 3 of the License, or (at your option) any later version.

 Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 PURPOSE.  See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along with this program.
 If not, see <https://www.gnu.org/licenses/>.
=end
module EpipeHelper
  def epipe_stop
    execute_remotely ENV['EPIPE_HOST'], "sudo systemctl stop epipe"
  end
  def epipe_stop_restart
    begin
      epipe_stop
      yield
    ensure
      epipe_restart_checked
    end
  end

  def epipe_restart_checked
    epipe_restart
    #if necessary wait (1+2)s - on vms it can sometimes be a slow process - in general 1s will be enough
    sleep(1) unless is_epipe_active
    sleep(2) unless is_epipe_active
    epipe_active
  end

  def epipe_restart
    execute_remotely ENV['EPIPE_HOST'], "sudo systemctl restart epipe"
  end

  def epipe_active
    output = execute_remotely ENV['EPIPE_HOST'], "systemctl is-active epipe"
    expect(output.strip).to eq("active"), "epipe is down"
  end

  def is_epipe_active
    output = execute_remotely ENV['EPIPE_HOST'], "systemctl is-active epipe"
    output.strip.eql? "active"
  end

  def epipe_wait_on_mutations(repeat=3)
    repeat.times do
      result = wait_for_me_time(30) do
        pending_mutations = HDFSMetadataLog.count
        if pending_mutations == 0
          { 'success' => true }
        else
          { 'success' => false, 'msg' => "hdfs_metadata_logs is not being consumed by epipe - pending:#{pending_mutations}" }
        end
      end
      if result["success"] == true
        break
      else
        pp "WARNING - #{result["msg"]}"
        epipe_restart_checked
      end
    end
    pending_mutations = HDFSMetadataLog.count
    expect(pending_mutations).to eq(0), "hdfs_metadata_logs is not being consumed by epipe - pending:#{pending_mutations}"
    #wait for epipe-elasticsearch propagation
    sleep(3)
  end

  def epipe_wait_on_provenance(repeat=3, with_restart: true)
    repeat.times do
      #check FileProv log table
      result = wait_for_me_time(30) do
        pending_prov = FileProv.count
        if pending_prov == 0
          { 'success' => true }
        else
          { 'success' => false, 'msg' => "hdfs_file_prov_logs is not being consumed by epipe - pending:#{pending_prov}" }
        end
      end
      if result["success"] == true
        #check AppProv log table
        result = wait_for_me_time(30) do
          pending_prov = AppProv.count
          if pending_prov == 0
            { 'success' => true }
          else
            { 'success' => false, 'msg' => "hdfs_app_prov_logs is not being consumed by epipe - pending:#{pending_prov}" }
          end
        end
      end
      if result["success"] == true
        break
      else
        if with_restart
          #restart epipe and try waiting again
          pp "WARNING - #{result["msg"]}"
          epipe_restart_checked
        end
      end
    end
    pending_prov = FileProv.count
    expect(pending_prov).to eq(0), "hdfs_file_prov_logs is not being consumed by epipe - pending:#{pending_prov}"
    pending_prov = AppProv.count
    expect(pending_prov).to eq(0), "hdfs_app_prov_logs is not being consumed by epipe - pending:#{pending_prov}"
    #wait for epipe-elasticsearch propagation
    sleep(3)
  end
end