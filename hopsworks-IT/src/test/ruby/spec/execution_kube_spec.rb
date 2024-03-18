=begin
 Copyright (C) 2020, Logical Clocks AB. All rights reserved
=end

describe "On #{ENV['OS']}" do
  after(:all) {clean_all_test_projects(spec: "execution_kube")}
  before(:all) do
    if ENV['OS'] == "ubuntu"
      skip "These tests do not run on ubuntu"
    end
  end
  describe 'execution' do
    describe "#create" do
      context 'without authentication' do
        before :all do
          with_valid_project
          reset_session
        end
        it "should fail" do
          create_python_job(@project, "demo_job", 'jar')
          expect_status_details(401, error_code: 200003)
        end
      end
      context 'with authentication and docker job' do
        before :all do
          setVar("docker_job_mounts_list", "/tmp,/var")
          setVar("docker_job_mounts_allowed", "false")
          setVar("docker_job_uid_strict", "false")
          with_valid_project
          $job_name = "j_#{short_random_id}"
        end
        after :all do
          setVar("docker_job_mounts_allowed", "true")
        end
        describe 'create, get, delete executions' do
          it "should fail to start a docker job with mounted volumes if not allowed" do
            create_docker_job(@project,  $job_name)
            #start execution
            start_execution(@project[:id],  $job_name, expected_status: 400)
          end
        end
      end
      context 'with authentication and docker job' do
        before :all do
          setVar("docker_job_mounts_list", "var")
          setVar("docker_job_mounts_allowed", "true")
          setVar("docker_job_uid_strict", "false")
          with_valid_project
          $job_name = "j_#{short_random_id}"
        end
        after :all do
          setVar("docker_job_mounts_list", "/tmp,/var")
        end
        describe 'create, get, delete executions' do
          it "should fail to start a docker job with a forbidden volume" do
            create_docker_job(@project, $job_name)
            #start execution
            start_execution(@project[:id], $job_name, expected_status: 400)
          end
        end
      end
      context 'with authentication and docker job' do
        before :all do
          setVar("docker_job_mounts_list", "/tmp,/var")
          setVar("docker_job_mounts_allowed", "true")
          setVar("docker_job_uid_strict", "true")
          with_valid_project
          $job_name = "j_#{short_random_id}"
        end
        after :all do
          setVar("docker_job_uid_strict", "false")
        end
        describe 'create, get, delete executions' do
          it "should fail to start a docker job with provided uid and gid if not allowed" do
            create_docker_job(@project,  $job_name)
            #start execution
            start_execution(@project[:id], $job_name, expected_status: 400)
          end
        end
      end
      context 'with authentication and docker job' do
        before :all do
          setVar("docker_job_mounts_list", "/tmp,/var")
          setVar("docker_job_mounts_allowed", "true")
          setVar("docker_job_uid_strict", "false")
          with_valid_project
        end
        before :each do
          $job_name = "j_#{short_random_id}"
        end
        describe 'create, get, delete executions' do
          it "should start/stop a job and get its executions" do
            #create job
            create_docker_job(@project, $job_name)
            job_id = json_body[:id]
            #start execution
            start_execution(@project[:id], $job_name)
            execution_id = json_body[:id]
            expect(json_body[:state]).to eq "INITIALIZING"
            expect(json_body[:finalStatus]).to eq "UNDEFINED"
            #get execution
            get_execution(@project[:id], $job_name, json_body[:id])
            expect(json_body[:id]).to eq(execution_id)
            #wait till it's finished and start second execution
            wait_for_execution_completed(@project[:id], $job_name, json_body[:id], "FINISHED", expected_final_status: "SUCCEEDED")
            #start execution
            start_execution(@project[:id], $job_name)
            execution_id = json_body[:id]

            #get all executions of job
            get_executions(@project[:id], $job_name)
            expect(json_body[:items].count).to eq 2

            #check database
            num_executions = count_executions(job_id)
            expect(num_executions).to eq 2

            wait_for_execution_completed(@project[:id], $job_name, execution_id, "FINISHED", expected_final_status: "SUCCEEDED")
          end
          it "should start a job that outputs a file and asserts the file is in datasets." do
            #create job
            create_docker_job(@project, $job_name)
            job_id = json_body[:id]
            #start execution
            start_execution(@project[:id], $job_name)
            execution_id = json_body[:id]
            expect(json_body[:state]).to eq "INITIALIZING"
            #get execution
            get_execution(@project[:id], $job_name, json_body[:id])
            expect(json_body[:id]).to eq(execution_id)
            #wait till it's finished and start second execution
            wait_for_execution_completed(@project[:id], $job_name, json_body[:id], "FINISHED", expected_final_status: "SUCCEEDED")

            # wait for Kubernetes handler to copy to hdfs
            wait_for_docker_job_output("/Projects/#{@project[:projectname]}/Resources/README_DV.md")
            wait_for_docker_job_output("/Projects/#{@project[:projectname]}/Resources/README_Jupyter.md")

            # wait for stdout and stderr and assert content
            wait_for_docker_job_output("/Projects/#{@project[:projectname]}/Logs/Docker/#{$job_name}/#{execution_id}/stdout.log")
            wait_for_docker_job_output("/Projects/#{@project[:projectname]}/Logs/Docker/#{$job_name}/#{execution_id}/stderr.log")
          end
        end
      end

      job_types = ['py', 'ipynb']
      job_types.each do |type|
        context 'with authentication and executable ' + type do
          before :all do
            with_valid_tour_project("spark")
          end
          before :each do
            $job_name = "j_#{short_random_id}"
          end
          describe 'create, get, delete executions' do
            before(:all) do
              setVar("mount_hopsfs_in_python_job", "false")
              create_session(@project[:username], "Pass123")
            end
            after(:all) do
              setVar("mount_hopsfs_in_python_job", "true")
              create_session(@project[:username], "Pass123")
            end

            it "should start/stop a job and get its executions" do
              #create job
              create_python_job(@project, $job_name, type)
              job_id = json_body[:id]
              #start execution
              start_execution(@project[:id], $job_name)
              execution_id = json_body[:id]
              expect(json_body[:state]).to eq "INITIALIZING"
              expect(json_body[:finalStatus]).to eq "UNDEFINED"
              #get execution
              get_execution(@project[:id], $job_name, json_body[:id])
              expect(json_body[:id]).to eq(execution_id)
              #wait till it's finished and start second execution
              wait_for_execution_completed(@project[:id], $job_name, json_body[:id], "FINISHED", expected_final_status: "SUCCEEDED")
              #start execution
              start_execution(@project[:id], $job_name)
              execution_id = json_body[:id]

              #get all executions of job
              get_executions(@project[:id], $job_name)
              expect(json_body[:items].count).to eq 2

              #check database
              num_executions = count_executions(job_id)
              expect(num_executions).to eq 2

              wait_for_execution_completed(@project[:id], $job_name, execution_id, "FINISHED", expected_final_status: "SUCCEEDED")
            end
            it "should start and stop job" do
              create_python_job(@project, $job_name, type)
              expect_status_details(201)

              #start execution
              start_execution(@project[:id], $job_name)
              execution_id = json_body[:id]
              stop_execution(@project[:id], $job_name, execution_id)
              wait_for_execution_completed(@project[:id], $job_name, execution_id, "KILLED", expected_final_status: "KILLED")
            end
            it "should fail to start a python job with missing appPath" do
              create_python_job(@project, $job_name, type)
              config = json_body[:config]
              create_python_job(@project, $job_name, type, config)
              delete_dataset(@project, config[:appPath], datasetType: "?type=DATASET")
              #start execution
              start_execution(@project[:id], $job_name, expected_status: 400)
            end
            it "should fail to start a python job with missing files param" do
              create_python_job(@project, $job_name, type)
              config = json_body[:config]
              config[:'files'] = "hdfs:///Projects/#{@project[:projectname]}/Resources/iamnothere.txt"
              create_python_job(@project, $job_name, type, config)
              #start execution
              start_execution(@project[:id], $job_name, expected_status: 400)
            end
            it "should fail with OOMKilled" do
              # Check that we set the Reason why a container failed if something other than the application program caused it to fail
              # In this case we set the memory to 10MB so the docker container runs out of memory
              create_python_job(@project, $job_name, type)
              config = json_body[:config]
              config[:'resourceConfig'][:'memory'] = 10
              create_python_job(@project, $job_name, type, config)
              start_execution(@project[:id], $job_name, expected_status: 201)
              execution_id = json_body[:id]
              wait_for_execution_completed(@project[:id], $job_name, execution_id, "FAILED", expected_final_status: "FAILED")
              get_execution_log(@project[:id], $job_name, execution_id, "err")
              expect(json_body[:log]).to be_present
              expect(json_body[:log]).to include("Reason: OOMKilled")
            end
            it "should start two executions in parallel" do
              create_python_job(@project, $job_name, type)
              begin
                start_execution(@project[:id], $job_name)
                execution_id_1 = json_body[:id]
                start_execution(@project[:id], $job_name)
                execution_id_2 = json_body[:id]
              ensure
                wait_for_execution_completed(@project[:id], $job_name, execution_id_1, "FINISHED", expected_final_status: "SUCCEEDED") unless execution_id_1.nil?
                wait_for_execution_completed(@project[:id], $job_name, execution_id_2, "FINISHED", expected_final_status: "SUCCEEDED") unless execution_id_2.nil?
              end
            end
            it "should start a job with default args" do
              create_python_job(@project, $job_name, type)
              default_args = json_body[:config][:defaultArgs]
              start_execution(@project[:id], $job_name)
              execution_id = json_body[:id]
              begin
                get_execution(@project[:id], $job_name, execution_id)
                expect(json_body[:args]).not_to be_nil
                expect(default_args).not_to be_nil
                expect(json_body[:args]).to eq default_args
              ensure
                wait_for_execution_completed(@project[:id], $job_name, execution_id, "FINISHED", expected_final_status: "SUCCEEDED")
              end
              if type=='ipynb' # Papermill
                expect(json_body[:notebookOutPath]).not_to be_nil
                notebookOutputPath = json_body[:notebookOutPath]
                get_dataset_stat(@project, notebookOutputPath, datasetType: "&type=DATASET")
                expect_status_details(200)
              end
            end
            it "should start a job with args -a string_test -b 2.5 (notebook) or 123 (Python)" do
              create_python_job(@project, $job_name, type)

              if type=='ipynb'
                args = "-a string_test -b 2.5"
              elsif type=='py'
                args = "123"
              end

              start_execution(@project[:id], $job_name, args: args)
              execution_id = json_body[:id]
              begin
                get_execution(@project[:id], $job_name, execution_id)
                expect(json_body[:args]).to eq args
              ensure
                wait_for_execution_completed(@project[:id], $job_name, execution_id, "FINISHED", expected_final_status: "SUCCEEDED")
              end
              if type=='ipynb' # Papermill
                expect(json_body[:notebookOutPath]).not_to be_nil
                notebookOutputPath = json_body[:notebookOutPath]
                get_dataset_stat(@project, notebookOutputPath, datasetType: "&type=DATASET")
                expect_status_details(200)
              end
            end
            it "should start an execution and delete it while running" do
              create_python_job(@project, $job_name, type)
              expect_status_details(201)
              #start execution
              start_execution(@project[:id], $job_name)
              execution_id = json_body[:id]
              #Wait a few seconds for kubernetes to start the job
              wait_for_kube_job($job_name)
              delete_execution(@project[:id], $job_name, execution_id)

              #check database
              num_executions = count_executions($job_name)
              expect(num_executions).to eq 0

              wait_for_kube_job($job_name, should_exist=false)
            end
            def get_logs(project, job_name, execution_id, log_type)
              #wait for log aggregation
              wait_result = wait_for_me_time(120) do
                get_execution_log(project[:id], $job_name, execution_id, log_type)
                { 'success' => (json_body[:log] != "No log available. If job failed instantaneously, please check again later or try running the job again. Log aggregation can take a few minutes to complete."), 'msg' => "wait for out log aggregation" }
              end
              expect(wait_result["success"]).to be(true), wait_result["msg"]

              #get err log
              get_execution_log(project[:id], job_name, execution_id, log_type)
              expect(json_body[:type]).to eq log_type.upcase
              expect(json_body[:log]).to be_present
            end
            it "should run job and get out and err logs" do
              create_python_job(@project, $job_name, type)
              execution_id = run_execution(@project[:id], $job_name)
              get_logs(@project, $job_name, execution_id, "out")
              get_logs(@project, $job_name, execution_id, "err")
            end
          end
          describe 'with mounting hopsfs enabled: ' + type do
            before(:all) do
              setVar("mount_hopsfs_in_python_job", "true")
              create_session(@project[:username], "Pass123")
            end
            it "should start/stop a job and get its executions" do
              #create job
              create_python_job(@project, $job_name, type, job_conf=nil, with_modules=true)
              #start execution
              start_execution(@project[:id], $job_name)
              execution_id = json_body[:id]
              expect(json_body[:state]).to eq "INITIALIZING"
              expect(json_body[:finalStatus]).to eq "UNDEFINED"
              #get execution
              get_execution(@project[:id], $job_name, json_body[:id])
              expect(json_body[:id]).to eq(execution_id)
              #wait till it's finished and start second execution
              wait_for_execution_completed(@project[:id], $job_name, json_body[:id], "FINISHED", expected_final_status: "SUCCEEDED")
            end
          end
        end
      end
    end
  end
end