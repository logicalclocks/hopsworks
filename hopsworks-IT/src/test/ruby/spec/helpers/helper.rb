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
module Helper
  def wait_for(timeout=480)
    start = Time.now
    x = yield
    until x
      if Time.now - start > timeout
        raise "Timed out waiting for action to finish. Timeout #{timeout} sec"
      end
      sleep(1)
      x = yield
    end
  end

  def expect_status_details(expected_status)
    if response.code == resolve_status(204, response.code)
      expect(response.code).to eq(resolve_status(expected_status, response.code)), "found code:#{response.code}"
    else
      expect(response.code).to eq(resolve_status(expected_status, response.code)), "found code:#{response.code} and body:#{json_body}"
    end
  end
end