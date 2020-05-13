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
  def time_this
    start_time = Time.now
    yield
    end_time = Time.now
    pp "time = #{end_time - start_time}"
  end

  def wait_for_me_time(timeout=480)
    start = Time.now
    x = yield
    until x["success"] == true
      if Time.now - start > timeout
        break
      end
      sleep(1)
      x = yield
    end
    return x
  end

  def expect_status_details(expected_status, error_code: nil)
    if response.code == resolve_status(204, response.code)
      expect(response.code).to eq(resolve_status(expected_status, response.code)), "expected rest status:#{expected_status}, found:#{response.code}"
    else
      json_result = JSON.parse(response.body) rescue nil
      if json_result
        expect(response.code).to eq(resolve_status(expected_status, response.code)), "expected rest status:#{expected_status}, found:#{response.code} and body:#{json_result}"
        if error_code
          expect(json_result["errorCode"]).not_to be_nil, "expected error code:#{error_code}, found none"
          expect(json_result["errorCode"]).to eq(error_code), "expected error code:#{error_code}, found:#{json_result["errorCode"]},"
        end
      else
        expect(response.code).to eq(resolve_status(expected_status, response.code)), "found code:#{response.code} and no/malfromed body"
      end
    end
  end

  def array_contains_one_of(array, &predicate)
    selected = array.select { |s| predicate.call(s) }
    expect(selected.length).to eq(1)
    selected[0]
  end

  def check_array_contains_one_of(array, &predicate)
    selected = array.select { |s| predicate.call(s) }
    selected.length == 1
  end

  def get_path_dataset(project, dataset)
    "/Projects/#{project[:projectname]}/#{dataset[:inode_name]}"
  end

  def get_path_dir(project, dataset, dir_relative_path)
    "/Projects/#{project[:projectname]}/#{dataset[:inode_name]}/#{dir_relative_path}"
  end
end