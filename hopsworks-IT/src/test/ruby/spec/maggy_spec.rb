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
describe "On #{ENV['OS']}" do
  after(:all) {clean_all_test_projects}
  before(:all) do
    with_valid_session
  end

  describe "maggyresource" do

    context "#logged in" do

      it "should be able to register" do
        post "#{ENV['HOPSWORKS_API']}/maggy/drivers", {"appId": "42", "hostIp": "127.0.3.5", "port": 12345,
        "secret": "magster"}
        expect_status(200)
      end

      it "should be able to get a registered driver" do
        get "#{ENV['HOPSWORKS_API']}/maggy/drivers/42"
        expect_status(200)
        json_body[:appId].eql? '42'
        json_body[:hostIp].eql? '127.0.3.5'
        json_body[:secret].eql? 'magster'
        json_body[:port].eql? '12345'
      end
    end
  end
end
