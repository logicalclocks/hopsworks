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

  #this function is used when stopping epipe, doing some operation and making sure we try to restart epipe even if an
  #error happens within the yield block
  def epipe_stop_restart
    begin
      epipe_stop
      yield
    ensure
      epipe_restart_checked
    end
  end

  #on slow vms it can take 1-3s for epipe to restart
  #epipe_active expects epipe to be active and will fail if epipe is down
  def epipe_restart_checked(msg: "epipe is down")
    epipe_restart
    sleep(1)
    epipe_active(msg: msg)
  end

  def epipe_restart
    execute_remotely ENV['EPIPE_HOST'], "sudo systemctl restart epipe"
  end

  def epipe_active(msg: "epipe is down")
    output = execute_remotely ENV['EPIPE_HOST'], "systemctl is-active epipe"
    expect(output.strip).to eq("active"), msg
  end

  def is_epipe_active
    output = execute_remotely ENV['EPIPE_HOST'], "systemctl is-active epipe"
    output.strip.eql? "active"
  end

  #search especially relies on the logs to be consumed.
  #we check the log every 1s for <repeat>  * wait_time (s).
  #we try restarting epipe if we think epipe is stuck.
  #if epipe is up and consumed all the logs, this method should not sleep at all and exit on first check
  def epipe_wait_on(wait_time: 10, repeat: 1, &log_size)
    result = (0..repeat).to_a.each_with_object([]) do | _, output |
      #wait returns on success or after timeout
      result = wait_for_me_time(wait_time) do
        pending = log_size.call
        if pending == 0
          { "success" => true, "pending" => 0 }
        else
          { "msg" => "logs are not being consumed by epipe - pending:#{pending}", "success" => false, "pending" => pending }
        end
      end
      pp "WARNING - #{result["msg"]}" if (result["pending"] > 0)
      if result["success"] == false
        pending = log_size.call
        if (result["pending"] - pending) < 10
          pp "WARNING - no progress - restarting epipe"
          epipe_restart
        end
      end
      output[0] = result
    end
    #short wait for epipe-elasticsearch propagation
    sleep(3)
    epipe_active(msg: "epipe is dead")
    result[0]
  end

  def epipe_wait_on_mutations(wait_time: 10, repeat: 1)
    result = epipe_wait_on(wait_time: wait_time, repeat: repeat) do
      count = HDFSMetadataLog.count
      count
    end
    result
  end

  def epipe_wait_on_provenance(wait_time:10, repeat: 1, with_restart: true)
    result = epipe_wait_on(wait_time: wait_time, repeat: repeat) do
      count = FileProv.count
      count
    end
    if result["success"] == true
      result = epipe_wait_on(wait_time: wait_time, repeat: repeat) do
        count = AppProv.count
        count
      end
    end
    result
  end
end