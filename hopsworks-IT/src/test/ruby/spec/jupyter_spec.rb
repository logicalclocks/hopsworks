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

require 'json'

describe "On #{ENV['OS']}" do
  describe "Jupyter Dataset" do
    before :all do
      with_valid_project
    end

    it "should not have the sticky bit set - HOPSWORKS-750" do
      get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/getContent"
      ds = json_body.detect { |d| d[:name] == "Jupyter"}
      expect(ds[:permission]).not_to include("t", "T")
    end
  end

  python_versions = ['2.7', '3.6']
  python_versions.each do |version|
    describe "Jupyter basic operations - python " + version do
      before :each do
        with_valid_project
        delete_env(@project[:id], '3.6')
      end

      it "should start and stop a notebook server" do

        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/python/environments/#{version}?action=create&pythonKernelEnable=true"
        expect_status(201)

        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jupyter/settings"
        expect_status(200)

        settings = json_body
        settings[:distributionStrategy] = ""
        staging_dir = settings[:privateDir]

        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jupyter/start", JSON(settings)
        expect_status(200)
        secret_dir = json_body[:secret]
        
        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jupyter/running"
        expect_status(200)

        jwt_file = File.join(staging_dir, "token.jwt")
        expect(File.file? jwt_file).to be true
        
        jupyter_dir = Variables.find_by(id: "jupyter_dir").value
        project_username = "#{@project[:projectname]}__#{@user[:username]}"
        path2secret = File.join(jupyter_dir, "Projects", @project[:projectname], project_username, secret_dir, "certificates")
        
        kstore_file = File.join(path2secret, "#{project_username}__kstore.jks")
        expect(File.file? kstore_file).to be true
        tstore_file = File.join(path2secret, "#{project_username}__tstore.jks")
        expect(File.file? tstore_file).to be true
        password_file = File.join(path2secret, "#{project_username}__cert.key")
        expect(File.file? password_file).to be true
        
        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jupyter/stop"
        expect_status(200)

        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jupyter/running"
        expect_status(404)

      end

      it "should not allow starting multiple notebook servers" do

        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/python/environments/#{version}?action=create&pythonKernelEnable=true"
        expect_status(201)

        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jupyter/settings"
        expect_status(200)

        settings = json_body
        settings[:distributionStrategy] = ""

        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jupyter/start", JSON(settings)
        expect_status(200)

        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jupyter/start", JSON(settings)
        expect_status(400)

        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jupyter/running"
        expect_status(200)

        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jupyter/stop"
        expect_status(200)

        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jupyter/running"
        expect_status(404)

      end

      it "should allow multiple restarts" do

        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/python/environments/#{version}?action=create&pythonKernelEnable=true"
        expect_status(201)

        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jupyter/settings"
        expect_status(200)

        settings = json_body
        settings[:distributionStrategy] = ""

        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jupyter/start", JSON(settings)
        expect_status(200)

        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jupyter/running"
        expect_status(200)

        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jupyter/stop"
        expect_status(200)

        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jupyter/running"
        expect_status(404)

        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jupyter/start", JSON(settings)
        expect_status(200)

        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jupyter/stop"
        expect_status(200)

        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jupyter/running"
        expect_status(404)
      end

      it "should be killed by timer" do
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/python/environments/#{version}?action=create&pythonKernelEnable=true"
        expect_status(201)

        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jupyter/settings"
        expect_status(200)

        settings = json_body
        settings[:distributionStrategy] = ""
        settings[:shutdownLevel] = 0

        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jupyter/start", JSON(settings)
        expect_status(200)

        # There is a potential race condition here if the timer runs just before this call
        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jupyter/running"
        expect_status(200)

        json_body[:minutesUntilExpiration].should be < 2

        sleep(90)

        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jupyter/running"
        expect_status(404)
      end

      it "should not be killed by timer" do

        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/python/environments/#{version}?action=create&pythonKernelEnable=true"
        expect_status(201)

        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jupyter/settings"
        expect_status(200)

        settings = json_body
        settings[:distributionStrategy] = ""
        settings[:shutdownLevel] = 6

        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jupyter/start", JSON(settings)
        expect_status(200)

        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jupyter/running"
        expect_status(200)

        initial_minutes_left = ((((settings[:shutdownLevel]))*60)-1)
        json_body[:minutesUntilExpiration].should be > initial_minutes_left-3

        sleep(90)

        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jupyter/running"
        expect_status(200)

        json_body[:minutesUntilExpiration].should be < initial_minutes_left
      end

      it "should convert .ipynb file to .py file" do

        copy("/user/hdfs/tensorflow_demo/notebooks/Experiment/Keras/mnist.ipynb", "/Projects/#{@project[:projectname]}/Resources", @user[:username], "#{@project[:projectname]}__Resources", 750, "#{@project[:projectname]}")

        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/python/environments/#{version}?action=create&pythonKernelEnable=true"
        expect_status(201)

        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/getContent/Resources"
        expect_status(200)
        notebook_file = json_body.detect { |d| d[:name] == "mnist.ipynb" }
        expect(notebook_file).to be_present

        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jupyter/convertIPythonNotebook/Resources/mnist.ipynb"
        expect_status(200)

        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/getContent/Resources"
        expect_status(200)
        python_file = json_body.detect { |d| d[:name] == "mnist.py" }
        expect(python_file).to be_present
      end
    end
  end
end
