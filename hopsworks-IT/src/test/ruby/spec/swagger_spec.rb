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
describe "On #{ENV['OS']}" do
  describe 'swagger' do
    describe "get swagger.json and validate contents" do
      context 'with valid endpoint' do
        it "should get swagger.json with info fields present" do
          get "#{ENV['HOPSWORKS_API']}/swagger.json"
          expect_status(200)
          parsed_json = JSON.parse(response.body)
          expect(parsed_json.key?("swagger")).to be true
          expect(parsed_json["info"]["version"]).not_to be_empty
        end
      end
    end
  end
end