=begin
 This file is part of Hopsworks
 Copyright (C) 2022, Logical Clocks AB. All rights reserved

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
  describe "Proxy jwt auth test" do
    after :all do
      reset_session
    end
    describe "without authentication" do
      before :all do
        reset_session
      end
      it "should not allow access to grafana" do
        proxy_helper_get_grafana
        expect_status_details(401)
      end
      it "should not allow access to yarn ui" do
        proxy_helper_get_yarnui
        expect_status_details(401)
      end
      it "should not allow access to hdfs ui" do
        proxy_helper_get_hdfsui
        expect_status_details(401)
      end
      it "should not allow access to tensorboard" do
        proxy_helper_get_tensorboard
        expect_status_details(401)
      end
    end
    describe "with admin session" do
      before :all do
        with_admin_session_for_proxy
      end
      it "should allow access to grafana" do
        proxy_helper_get_grafana
        expect_status_details(200)
      end
      it "should allow access to yarn ui" do
        proxy_helper_get_yarnui
        expect_status_details(200)
      end
      it "should allow access to hdfs ui" do
        proxy_helper_get_hdfsui
        expect_status_details(200)
      end
      it "should allow access to tensorboard" do
        proxy_helper_get_tensorboard
        expect_status_details(403)
        expect(response.body).to include("Access to the specified resource has been forbidden.")
      end
    end
    describe "with user session" do
      before :all do
        reset_session
        with_user_session_for_proxy
      end
      it "should allow access to grafana" do
        proxy_helper_get_grafana
        expect_status(200)
      end
      it "should allow access to yarn ui" do
        proxy_helper_get_yarnui
        expect_status(400)
      end
      it "should allow access to hdfs ui" do
        proxy_helper_get_hdfsui
        expect_status(403)
      end
      it "should allow access to tensorboard" do
        proxy_helper_get_tensorboard
        expect_status(403)
        expect(response.body).to include("Access to the specified resource has been forbidden.")
      end
    end
  end
end