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
require 'websocket-client-simple'

describe "On #{ENV['OS']}" do
  before :all do
    @debugOpt=false
    # Enable the reporting
    RSpec.configure do |c|
      c.example_status_persistence_file_path  = 'jupyter_debug.txt' if @debugOpt
    end
    with_valid_project
  end

  before(:each) {get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jupyter/stop"}

  after(:all) {clean_all_test_projects(spec: "jupyter")}
  describe "Jupyter core" do

    before :all do
      with_valid_project
    end

    describe "Jupyter Dataset" do
      it "should not have the sticky bit set - HOPSWORKS-750" do
        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/?expand=inodes&action=listing"
        ds = json_body[:items].detect { |d| d[:name] == "Jupyter"}
        expect(ds[:attributes][:permission]).not_to include("t", "T")
      end
    end

    remote_fs_drivers = ["hdfscontentsmanager", "hopsfsmount"]
    remote_fs_drivers.each do |driver|
      describe "with jupyter remote filesystem driver: #{driver}" do
        before(:all) do
          setVar("jupyter_remote_fs_driver", driver)
          create_session(@project[:username], "Pass123")
        end
        after(:all) do
          setVar("jupyter_remote_fs_driver", "hdfscontentsmanager")
          create_session(@project[:username], "Pass123")
        end

        python_versions = [ENV['PYTHON_VERSION']]
        jupyter_home = ""
        if driver == "hopsfsmount"
          jupyter_home = "Jupyter"
        end
        python_versions.each do |version|
          it 'should get recent jupyter notebooks' do
            start_jupyter(@project)
            port = json_body[:port]
            token = json_body[:token]
            hdfsUsername = "#{@project[:projectname]}__#{@user[:username]}"

            kernel_id = ""
            auth_token(token) do
              temp_name = create_notebook(port, path: jupyter_home)
              update_notebook(port, get_code_content, temp_name)
              _, kernel_id = create_notebook_session(port, temp_name, temp_name)
            end

            attachConfiguration(@project, hdfsUsername, kernel_id)

            recentnotebooks_search(@project, 1)

            stop_jupyter(@project)
          end
          it "should start, get logs and stop a notebook server" do
            secret_dir, staging_dir, settings = start_jupyter(@project)

            jupyter_running(@project, expected_status: 200)
            if ENV['OS'] == "ubuntu"
              # Token is in staging_dir if not kube
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
            end

            # Check that the logs are written in the opensearch index.
            begin
              Airborne.configure do |config|
                config.base_url = ''
              end
              wait_for_me_time(30) do
                response = opensearch_get "#{@project[:projectname].downcase}_logs*/_search?q=jobname=#{@user[:username]}"
                index = response.body
                { 'success' => JSON.parse(index)['hits']['total']['value'] > 0 }
              end
            rescue
              p "jupyter spec: Error calling opensearch_get #{$!}"
            else
              response = opensearch_get "#{@project[:projectname].downcase}_logs*/_search?q=jobname=#{@user[:username]}"
              index = response.body
              parsed_index = JSON.parse(index)
              expect(parsed_index['hits']['total']['value']).to be > 0
            ensure
              Airborne.configure do |config|
                config.base_url = "https://#{ENV['WEB_HOST']}:#{ENV['WEB_PORT']}"
              end
            end

            stop_jupyter(@project)
            jupyter_running(@project, expected_status: 404)
          end

          kernels = ["ipython", "pyspark", "spark"]
          kernels.each do |kernel|
            it "should attach jupyter configuration as xattr to a notebook with #{kernel} kernel" do
              create_env_and_update_project(@project, version)

              get_settings(@project)
              shutdownLevel=6
              settings = json_body
              settings[:shutdownLevel] = shutdownLevel
              start_jupyter(@project, settings: settings)
              jupyter_project = json_body
              port = jupyter_project[:port]
              token = jupyter_project[:token]
              notebook_name = "test_attach_xattr_#{kernel}_kernel.ipynb"

              Airborne.configure do |config|
                config.headers["Authorization"] = "token #{token}"
              end

              temp_name = create_notebook(port, path: jupyter_home)
              notebook_json = read_notebook("#{ENV['PROJECT_DIR']}/hopsworks-IT/src/test/ruby/spec/auxiliary/#{notebook_name}")

              update_notebook(port, notebook_json, temp_name)

              #create a session for the notebook
              session_id, kernel_id = create_notebook_session(port, temp_name)

              #create a websocket session
              ws = create_websocket_connection_to_jupyter_server(port, kernel_id, session_id)
              notebook_code = get_notebook_code(notebook_json)
              ws.on :open do
                #run the notebook cells
                for code in notebook_code do
                  msg_type = 'execute_request'
                  content = { code: code.join(""), 'silent':false}
                  hdr = { msg_id: SecureRandom.uuid, username: '', session: session_id, msg_type: msg_type, version: '5.2' }
                  msg = { 'header': hdr, parent_header: hdr, metadata: {}, content: content }
                  ws.send(msg)
                end
              end

              ws.on :close do |e|
                puts e
              end

              ws.on :error do |e|
                fail "Failed to create a websocket connection. #{e}"
              end

              #reset session and relogin
              reset_session
              create_session(@project[:username],"Pass123")
              #get the attached xatrr
              get_configuration(@project, "#{settings[:baseDir]}/#{temp_name}")
              stop_jupyter(@project)
            end
          end

          it "should convert .ipynb file to .py file" do

            copy_from_local("#{ENV['PROJECT_DIR']}/hopsworks-IT/src/test/ruby/spec/auxiliary/export_model.ipynb",
                            "/Projects/#{@project[:projectname]}/Resources", @user[:username], "#{@project[:projectname]}__Resources", 750, "#{@project[:projectname]}")

            get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/Resources/?action=listing&expand=inodes"
            expect_status_details(200)
            notebook_file = json_body[:items].detect { |d| d[:attributes][:name] == "export_model.ipynb" }
            expect(notebook_file).to be_present

            get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jupyter/convertIPythonNotebook/Resources/export_model.ipynb"
            expect_status_details(200)

            get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/Resources/?action=listing&expand=inodes"
            expect_status_details(200)
            python_file = json_body[:items].detect { |d| d[:attributes][:name] == "export_model.py" }
            expect(python_file).to be_present
          end

          it "should convert .ipynb file to .py file if the filename has special symbols or space" do
            copy_from_local("#{ENV['PROJECT_DIR']}/hopsworks-IT/src/test/ruby/spec/auxiliary/export_model.ipynb",
                            "/Projects/#{@project[:projectname]}/Resources", @user[:username], "#{@project[:projectname]}__Resources", 750, "#{@project[:projectname]}")

            get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/Resources/?action=listing&expand=inodes"
            expect_status_details(200)
            notebook_file = json_body[:items].detect { |d| d[:attributes][:name] == "export_model.ipynb" }
            expect(notebook_file).to be_present
            # test_dir will be created for each kernel
            test_dir = "test_dir#{short_random_id}"
            create_dir(@project, "Resources/#{test_dir}", query: "&type=DATASET")
            expect_status_details(201)

            copy_dataset(@project, "Resources/export_model.ipynb", "/Projects/#{@project[:projectname]}/Resources/#{test_dir}/[export model].ipynb", datasetType: "&type=DATASET")
            expect_status_details(204)

            get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jupyter/convertIPythonNotebook/Resources/#{test_dir}/%5Bexport%20model%5D.ipynb"
            expect_status_details(200)

            get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/Resources/#{test_dir}/?action=listing&expand=inodes"
            expect_status_details(200)
            python_file = json_body[:items].detect { |d| d[:attributes][:name] == "[export model].py" }
            expect(python_file).to be_present
          end
        end
      end
    end
  end

  describe "Jupyter extended" do
    remote_fs_drivers = ["hdfscontentsmanager", "hopsfsmount"]
    remote_fs_drivers.each do |driver|
      describe "with jupyter remote filesystem driver:  #{driver}" do
        before(:all) do
          setVar("jupyter_remote_fs_driver", driver)
          create_session(@project[:username], "Pass123")
        end
        after(:all) do
          setVar("jupyter_remote_fs_driver", "hdfscontentsmanager")
          create_session(@project[:username], "Pass123")
        end

        python_versions = [ENV['PYTHON_VERSION']]
        python_versions.each do |version|

          it "should be able to start from a shared dataset" do
            if driver == "hopsfsmount"
              skip "This test run only without hopsfs mount"
            end
            projectname = "project_#{short_random_id}"
            project = create_project_by_name(projectname)

            copy_from_local("#{ENV['PROJECT_DIR']}/hopsworks-IT/src/test/ruby/spec/auxiliary/run_single_experiment.ipynb",
                            "/Projects/#{projectname}/Jupyter/shared_notebook.ipynb", @user[:username],
                            "#{projectname}__Resources", 750, "#{projectname}")

            dsname = "Jupyter"
            share_dataset(project, dsname, @project[:projectname], permission: "EDITABLE", expected_status: 204)

            accept_dataset(@project, "/Projects/#{projectname}/#{dsname}", datasetType: "&type=DATASET", expected_status: 204)

            secret_dir, staging_dir, settings = start_jupyter(@project, shutdownLevel=6, baseDir="/Projects/#{project[:projectname]}/#{dsname}")

            jupyter_running(@project, expected_status: 200)

            # List all notebooks and files in Jupyter
            list_content(json_body[:port], json_body[:token])
            expect_status_details(200)

            notebook = json_body[:content].detect { |content| content[:path] == "shared_notebook.ipynb" }
            expect(notebook).not_to be_nil

          end

          it "should fail to start if insufficient executor memory is provided" do

            get_settings(@project)
            shutdownLevel=6
            settings = json_body
            settings[:shutdownLevel] = shutdownLevel
            settings[:pythonKernel] = false
            settings[:jobConfig][:"spark.executor.memory"] = 1023
            start_jupyter(@project, settings: settings, expected_status: 400, error_code: 130029)
            # set back spark.executor.memory=1024
            settings[:jobConfig][:"spark.executor.memory"] = 1024
            update_jupyter(@project, settings)
          end

          it "should work to start jupyter server with spark files attached" do
            get_settings(@project)
            settings = json_body
            settings[:jobConfig][:"spark.yarn.dist.files"]="hdfs:///Projects/#{@project[:projectname]}/Resources/README.md"
            start_jupyter(@project, settings: settings)
            jupyter_running(@project, expected_status: 200)
            stop_jupyter(@project)
          end

          it "should not allow starting multiple notebook servers" do
            start_jupyter(@project)
            start_jupyter(@project, expected_status: 400)
            jupyter_running(@project, expected_status: 200)
            stop_jupyter(@project)
            jupyter_running(@project, expected_status: 404)
          end

          it "should allow for creation of a non terminating jupyter session" do
            secret_dir, staging_dir, settings = start_jupyter(@project, shutdownLevel=6, baseDir=nil, noLimit=true)
            jupyter_running(@project, expected_status: 200)

            expect(json_body[:noLimit]).to eq(true)

            stop_jupyter(@project)
            jupyter_running(@project, expected_status: 404)
          end

          it "should allow multiple restarts" do
            secret_dir, staging_dir, settings = start_jupyter(@project)
            jupyter_running(@project, expected_status: 200)
            stop_jupyter(@project)
            jupyter_running(@project, expected_status: 404)
            start_jupyter(@project, settings: settings)
            stop_jupyter(@project)
            jupyter_running(@project, expected_status: 404)
          end

          it "should be killed by timer" do

            secret_dir, staging_dir, settings = start_jupyter(@project, shutdownLevel=0)

            # There is a potential race condition here if the timer runs just before this call
            jupyter_running(@project, expected_status: 200)
            json_body[:minutesUntilExpiration].should be < 2

            wait_for_me_time(180, 5) do
              jupyter_running(@project, expected_status: 200)
              is_running = response.code == resolve_status(200, response.code)
              { 'success' => is_running }
            end
            # JupyterNotebookCleaner is running every 30min it does not make sense to wait 30min in a test
            get "#{ENV['HOPSWORKS_TESTING']}/test/jupyter/cleanup"
            expect_status(200) # No detail returned

            jupyter_running(@project, expected_status: 404)
          end

          it "should not be killed by timer" do
            secret_dir, staging_dir, settings = start_jupyter(@project, shutdownLevel=6)

            jupyter_running(@project, expected_status: 200)

            initial_minutes_left = ((((settings[:shutdownLevel]))*60)-1)
            json_body[:minutesUntilExpiration].should be > initial_minutes_left-3

            sleep(90)
            get "#{ENV['HOPSWORKS_TESTING']}/test/jupyter/cleanup"
            expect_status(200) # No detail returned

            jupyter_running(@project, expected_status: 200)

            json_body[:minutesUntilExpiration].should be < initial_minutes_left
          end

          it "should update the jupyter settings without the need of starting a notebook server" do
            get_settings(@project)
            settings = json_body
            settings[:jobConfig][:amVCores] = 10
            update_jupyter(@project, settings)
            get_settings(@project)
            expect(json_body[:jobConfig][:amVCores]).to be 10
            settings[:jobConfig][:amVCores] = 1
            update_jupyter(@project, settings)
          end

          context "Failure scenarios - python " + version do

            before :each do
              with_admin_session
              with_valid_project
            end

            after :each do
              update_host_service_on_all_hosts('kafka', 'SERVICE_START')
              update_host_service_on_all_hosts('zookeeper', 'SERVICE_START')
            end

            after(:each) do |example|
              if example.exception
                update_host_service_on_all_hosts('kafka', 'SERVICE_START')
                update_host_service_on_all_hosts('zookeeper', 'SERVICE_START')
              end
            end

            it "Should start Jupyter if Kafka is down" do
              update_host_service_on_all_hosts('kafka', 'SERVICE_STOP')

              start_jupyter(@project)
              jupyter_running(@project, expected_status: 200)
              stop_jupyter(@project)
              jupyter_running(@project, expected_status: 404)
            end

            it "Should start Jupyter if Zookeeper is down" do
              update_host_service_on_all_hosts('zookeeper', 'SERVICE_STOP')

              start_jupyter(@project)
              jupyter_running(@project, expected_status: 200)
              stop_jupyter(@project)
              jupyter_running(@project, expected_status: 404)
            end

            it "Should start Jupyter if Zookeeper and Kafka is down" do
              update_host_service_on_all_hosts('zookeeper', 'SERVICE_STOP')
              update_host_service_on_all_hosts('kafka', 'SERVICE_STOP')

              start_jupyter(@project)
              jupyter_running(@project, expected_status: 200)
              stop_jupyter(@project)
              jupyter_running(@project, expected_status: 404)
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
            set_yarn_quota(@project, 0, "PREPAID")
            start_jupyter(@project, expected_status: 412)
          end

          it 'should not be able to start Jupyter with negative quota and payment type PREPAID' do
            set_yarn_quota(@project, -10, "PREPAID")
            start_jupyter(@project, expected_status: 412)
          end

          it 'should be able to start Jupyter with 0 quota and payment type NOLIMIT' do
            set_yarn_quota(@project, 0, "NOLIMIT")
            start_jupyter(@project)
            stop_jupyter(@project)
          end

          it 'should be able to start Jupyter with negative quota and payment type NOLIMIT' do
            set_yarn_quota(@project, -10, "NOLIMIT")
            start_jupyter(@project)
            stop_jupyter(@project)
          end
        end
      end
    end
  end
end
