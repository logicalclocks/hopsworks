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

    it "should convert .ipynb file to .py file" do

      copy("/user/hdfs/tensorflow_demo/notebooks/Experiment/Keras/mnist.ipynb", "/Projects/#{@project[:projectname]}/Resources", @user[:username], "#{@project[:projectname]}__Resources", 750)

      get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps/enable/2.7/true"
      expect_status(200)

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