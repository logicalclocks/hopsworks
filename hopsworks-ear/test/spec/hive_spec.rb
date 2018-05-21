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

describe "Hive tests" do

  describe "Hive Scratchdir Cleaner" do

    before :each do
      with_valid_project
    end

    it 'should be able to delete a user directory' do
      # Simulate a query running by creating a directory
      project_user = "#{@project["projectname"]}__#{@user["username"]}"
      mkdir("/tmp/hive/#{project_user}", project_user, project_user, "700")
      sleep(60)
      if test_dir("/tmp/hive/#{project_user}")
        raise "Directory: /tmp/hive/#{project_user} hasn't been deleted"
      end
    end

    # TODO: in future we should also test that the directory is not cleaned up if
    # there is a running application
  end
end
