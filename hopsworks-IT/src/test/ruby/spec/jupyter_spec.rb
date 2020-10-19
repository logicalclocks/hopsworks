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
  after(:all) {clean_all_test_projects(spec: "jupyter")}
  describe "Jupyter Dataset" do
    before :all do
      with_valid_project
    end

    it "should not have the sticky bit set - HOPSWORKS-750" do
      get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/?expand=inodes&action=listing"
      ds = json_body[:items].detect { |d| d[:name] == "Jupyter"}
      expect(ds[:attributes][:permission]).not_to include("t", "T")
    end
  end

  python_versions = ['3.7']
  python_versions.each do |version|
    describe "Jupyter basic operations - python " + version do
      before :each do
        with_valid_project
        delete_env(@project[:id], '3.7')
      end

      it "should start, get logs and stop a notebook server" do

        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/python/environments/#{version}?action=create"
        expect_status(201)

        secret_dir, staging_dir, settings = start_jupyter(@project)

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

        # Get logs from elasticsearch
        # Sleep a bit to make sure that logs are propagated correctly to the index
        sleep(30)

        # Check that the logs are written in the elastic index.
        begin
          Airborne.configure do |config|
            config.base_url = ''
          end
          response = elastic_get "#{@project[:projectname].downcase}_logs*/_search?q=jobname=#{@user[:username]}"
          index = response.body
        rescue
          p "jupyter spec: Error calling elastic_get #{$!}"
        else
          parsed_index = JSON.parse(index)
          expect(parsed_index['hits']['total']['value']).to be > 0
        ensure
          Airborne.configure do |config|
            config.base_url = "https://#{ENV['WEB_HOST']}:#{ENV['WEB_PORT']}"
          end
        end


        stop_jupyter(@project)

        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jupyter/running"
        expect_status(404)

      end

      it "should not allow starting multiple notebook servers" do

        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/python/environments/#{version}?action=create"
        expect_status(201)

        secret_dir, staging_dir, settings = start_jupyter(@project)

        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jupyter/start", JSON(settings)
        expect_status(400)

        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jupyter/running"
        expect_status(200)

        stop_jupyter(@project)

        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jupyter/running"
        expect_status(404)

      end

      it "should allow multiple restarts" do

        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/python/environments/#{version}?action=create"
        expect_status(201)

        secret_dir, staging_dir, settings = start_jupyter(@project)

        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jupyter/running"
        expect_status(200)

        stop_jupyter(@project)

        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jupyter/running"
        expect_status(404)

        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jupyter/start", JSON(settings)
        expect_status(200)

        stop_jupyter(@project)

        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jupyter/running"
        expect_status(404)
      end

      it "should be killed by timer" do
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/python/environments/#{version}?action=create"
        expect_status(201)

        secret_dir, staging_dir, settings = start_jupyter(@project, expected_status=200, shutdownLevel=0)

        # There is a potential race condition here if the timer runs just before this call
        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jupyter/running"
        expect_status(200)

        json_body[:minutesUntilExpiration].should be < 2

        sleep(90)

        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jupyter/running"
        expect_status(404)
      end

      it "should not be killed by timer" do

        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/python/environments/#{version}?action=create"
        expect_status(201)

        secret_dir, staging_dir, settings = start_jupyter(@project, expected_status=200, shutdownLevel=6)

        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jupyter/running"
        expect_status(200)

        initial_minutes_left = ((((settings[:shutdownLevel]))*60)-1)
        json_body[:minutesUntilExpiration].should be > initial_minutes_left-3

        sleep(90)

        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jupyter/running"
        expect_status(200)

        json_body[:minutesUntilExpiration].should be < initial_minutes_left
      end

      it "should be able to start from a shared dataset" do

        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/python/environments/#{version}?action=create"
        expect_status(201)

        projectname = "project_#{short_random_id}"
        project = create_project_by_name(projectname)

        copy_from_local("#{ENV['PROJECT_DIR']}/hopsworks-IT/src/test/ruby/spec/auxiliary/run_single_experiment.ipynb",
                                "/Projects/#{projectname}/Jupyter/shared_notebook.ipynb", @user[:username],
                                "#{projectname}__Resources", 750, "#{projectname}")

        dsname = "Jupyter"
        share_dataset(project, dsname, @project[:projectname], permission: "EDITABLE")
        expect_status(204)

        accept_dataset(@project, "/Projects/#{projectname}/#{dsname}", datasetType: "&type=DATASET")
        expect_status(204)

        secret_dir, staging_dir, settings = start_jupyter(@project, expected_status=200, shutdownLevel=6, baseDir="/Projects/#{project[:projectname]}/#{dsname}")

        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jupyter/running"
        expect_status(200)

        # List all notebooks and files in Jupyter
        list_content(json_body[:port], json_body[:token])
        expect_status(200)

        notebook = json_body[:content].detect { |content| content[:path] == "shared_notebook.ipynb" }
        expect(notebook).not_to be_nil

      end

      it "should convert .ipynb file to .py file" do

        copy_from_local("#{ENV['PROJECT_DIR']}/hopsworks-IT/src/test/ruby/spec/auxiliary/export_model.ipynb",
                        "/Projects/#{@project[:projectname]}/Resources", @user[:username], "#{@project[:projectname]}__Resources", 750, "#{@project[:projectname]}")

        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/python/environments/#{version}?action=create"
        expect_status(201)

        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/Resources/?action=listing&expand=inodes"
        expect_status(200)
        notebook_file = json_body[:items].detect { |d| d[:attributes][:name] == "export_model.ipynb" }
        expect(notebook_file).to be_present

        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jupyter/convertIPythonNotebook/Resources/export_model.ipynb"
        expect_status(200)

        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/Resources/?action=listing&expand=inodes"
        expect_status(200)
        python_file = json_body[:items].detect { |d| d[:attributes][:name] == "export_model.py" }
        expect(python_file).to be_present
      end

      it "should convert .ipynb file to .py file if the filename has special symbols or space" do
        copy_from_local("#{ENV['PROJECT_DIR']}/hopsworks-IT/src/test/ruby/spec/auxiliary/export_model.ipynb",
                        "/Projects/#{@project[:projectname]}/Resources", @user[:username], "#{@project[:projectname]}__Resources", 750, "#{@project[:projectname]}")

        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/python/environments/#{version}?action=create"
        expect_status(201)

        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/Resources/?action=listing&expand=inodes"
        expect_status(200)
        notebook_file = json_body[:items].detect { |d| d[:attributes][:name] == "export_model.ipynb" }
        expect(notebook_file).to be_present

        create_dir(@project, "Resources/test_dir", query: "&type=DATASET")
        expect_status(201)

        copy_dataset(@project, "Resources/export_model.ipynb", "/Projects/#{@project[:projectname]}/Resources/test_dir/[export model].ipynb", datasetType: "&type=DATASET")
        expect_status_details(204)

        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jupyter/convertIPythonNotebook/Resources/test_dir/%5Bexport%20model%5D.ipynb"
        expect_status(200)

        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/Resources/test_dir/?action=listing&expand=inodes"
        expect_status(200)
        python_file = json_body[:items].detect { |d| d[:attributes][:name] == "[export model].py" }
        expect(python_file).to be_present
      end
    end
  end

  describe "Jupyter quota" do
    before :all do
      @cookies = with_admin_session
      with_valid_project
    end

    after :all do
      @cookies = nil
    end

    it 'it should not be able to start Jupyter with 0 quota and payment type PREPAID' do
      set_yarn_quota(@project, 0)
      set_payment_type(@project, "PREPAID")
      start_jupyter(@project, expected_status=412)
    end

    it 'should not be able to start Jupyter with negative quota and payment type PREPAID' do
      set_yarn_quota(@project, -10)
      set_payment_type(@project, "PREPAID")
      start_jupyter(@project, expected_status=412)
    end

    it 'should be able to start Jupyter with 0 quota and payment type NOLIMIT' do
      set_yarn_quota(@project, 0)
      set_payment_type(@project, "NOLIMIT")
      start_jupyter(@project, expected_status=200)
      stop_jupyter(@project)
    end

    it 'should be able to start Jupyter with negative quota and payment type NOLIMIT' do
      set_yarn_quota(@project, -10)
      set_payment_type(@project, "NOLIMIT")
      start_jupyter(@project, expected_status=200)
      stop_jupyter(@project)
    end
  end
end
