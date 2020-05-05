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
  def epipe_restart
    execute_remotely ENV['EPIPE_HOST'], "sudo systemctl restart epipe"
  end

  def epipe_active
    output = execute_remotely ENV['EPIPE_HOST'], "systemctl is-active epipe"
    expect(output.strip).to eq("active"), "epipe is down"
  end

  def epipe_wait_on_mutations(repeat=3)
    epipe_active
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
        epipe_restart
        sleep(1)
        epipe_active
      end
    end
    pending_mutations = HDFSMetadataLog.count
    expect(pending_mutations).to eq(0), "hdfs_metadata_logs is not being consumed by epipe - pending:#{pending_mutations}"
    #wait for epipe-elasticsearch propagation
    sleep(3)
  end
end