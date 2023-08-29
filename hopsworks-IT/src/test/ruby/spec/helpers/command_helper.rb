=begin
 This file is part of Hopsworks
 Copyright (C) 2023, Hopsworks AB. All rights reserved

 Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 the GNU Affero General Public License as published by the Free Software Foundation,
 either version 3 of the License, or (at your option) any later version.

 Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 PURPOSE.  See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along with this program.
 If not, see <https://www.gnu.org/licenses/>.
=end

module CommandHelper
  def wait_on_command(wait_time: 10, repeat: 1, &log_size)
    result = (0..repeat).to_a.each_with_object([]) do | _, output |
      #wait returns on success or after timeout
      initial = -1
      idx = 0
      result = wait_for_me_time(repeat*wait_time, wait_time) do
        idx = idx + 1
        pending = log_size.call
        if initial == -1
          initial = pending
        end
        if pending == 0
          { "success" => true, "pending" => 0 }
        else
          msg = "command logs are not being consumed by executor -"
          msg = msg + "initial:#{initial} after:#{idx} x #{wait_time}s at pending:#{pending}"
          { "msg" => msg, "success" => false, "pending" => pending }
        end
      end
      pp "WARNING - #{result["msg"]}" if (result["success"] == false)
      if result["success"] == false
        pending = log_size.call
        if (result["pending"] - pending) < 10
          pp "WARNING - no progress - executor dead?"
        end
      end
      output[0] = result
    end
    #short wait for executor-opensearch propagation
    sleep(2)
    result[0]
  end

  def wait_on_command_search(wait_time: 10, repeat: 6)
    result = wait_on_command(wait_time: wait_time, repeat: repeat) do
      count = CommandSearch.count
      count
    end
    result
  end
end