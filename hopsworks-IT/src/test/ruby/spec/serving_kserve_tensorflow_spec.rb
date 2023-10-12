=begin
 Copyright (C) 2022, Logical Clocks AB. All rights reserved
=end

# serving_kserve_tensorflow_spec.rb: Tests for serving tensorflow models on KServe

require 'json'

describe "On #{ENV['OS']}" do
  
  before :all do
    # ensure data science profile is enabled
    setVar('enable_data_science_profile', "true")
  end

  after :all do
    clean_all_test_projects(spec: "serving_kserve_tensorflow")
  end

  describe "kubernetes not installed" do
    before :all do
      if kubernetes_installed
        skip "These tests only runs without Kubernetes installed"
      end
      with_valid_project
      copy_mnist_files(@project[:projectname], @user[:username])
    end

    it "should fail to create a serving when Kubernetes is not installed" do
      put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/", parse_serving_json(
          {name: "mnist",
           modelPath: "/Projects/#{@project[:projectname]}/Models/mnist",
           modelVersion: 1,
           batchingConfiguration: {
               batchingEnabled: false
           },
           kafkaTopicDTO: {
               name: "NONE"
           },
           modelServer: "TENSORFLOW_SERVING",
           modelFramework: "TENSORFLOW",
           servingTool: "KSERVE",
           requestedInstances: 1
          })
      expect_status_details(400, error_code: 240014)
      expect_json(errorMsg: "Kubernetes is not installed", usrMsg: "Serving tool not supported. KServe requires Kubernetes to be installed")
    end
  end

  describe "kubernetes installed" do
    before :all do
      if !kubernetes_installed
        skip "These tests only runs with Kubernetes installed"
      end
    end

    describe "kserve not installed" do
      before :all do
        if kserve_installed
          skip "These tests only runs without KServe installed"
        end
      end

      it "should fail to create a serving when KServe is not installed" do
        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/", parse_serving_json(
          {name: "mnist",
           modelPath: "/Projects/#{@project[:projectname]}/Models/mnist",
           modelVersion: 1,
           batchingConfiguration: {
               batchingEnabled: false
           },
           kafkaTopicDTO: {
               name: "NONE"
           },
           modelServer: "TENSORFLOW_SERVING",
           modelFramework: "TENSORFLOW",
           servingTool: "KSERVE",
           requestedInstances: 1
          })
        expect_status_details(400, error_code: 240015)
        expect_json(errorMsg: "KServe is not installed or disabled", usrMsg: "Serving tool not supported")
      end
    end

    describe "kserve installed" do
      before :all do
        if !kserve_installed
          skip "These tests only runs with KServe installed"
        end
      end

      describe "#create" do
        context 'without authentication' do
          before :all do
            with_valid_project
            copy_mnist_files(@project[:projectname], @user[:username])
            reset_session
          end

          it "should fail to create the serving" do
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/", parse_serving_json(
                {name: "testmodel",
                 modelPath: "/Projects/#{@project[:projectname]}/Models/mnist",
                 modelVersion: 1,
                 batchingConfiguration: {
                  batchingEnabled: false
                 },
                 modelServer: "TENSORFLOW_SERVING",
                 modelFramework: "TENSORFLOW",
                 servingTool: "KSERVE",
                 requestedInstances: 1
                })
            expect_status_details(401, error_code: 200003)
          end
        end

        context 'with authentication', vm: true do
          before :all do
            with_valid_project
            copy_mnist_files(@project[:projectname], @user[:username])
          end

          after :all do
            delete_all_servings(@project[:id])
          end

          # serving name

          it "should fail to create a serving with an invalid name" do
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/", parse_serving_json(
                {name: "invalidName",
                 modelPath: "/Projects/#{@project[:projectname]}/Models/mnist",
                 modelVersion: 1,
                 batchingConfiguration: {
                   batchingEnabled: false
                 },
                 kafkaTopicDTO: {
                     name: "NONE"
                 },
                 modelServer: "TENSORFLOW_SERVING",
                 modelFramework: "TENSORFLOW",
                 servingTool: "KSERVE",
                 requestedInstances: 1
                })
            expect_status_details(422)
            expect_json(usrMsg: "Serving name must consist of lower case alphanumeric characters, '-' or '.', and start and end with an alphanumeric character")
          end

          # artifact version

          it "should create a serving with a MODEL-ONLY artifact without transformer" do
            name = "testmodel1"
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/", parse_serving_json(
                {name: name,
                 modelPath: "/Projects/#{@project[:projectname]}/Models/mnist",
                 batchingConfiguration: {
                   batchingEnabled: false
                 },
                 modelVersion: 1,
                 modelServer: "TENSORFLOW_SERVING",
                 modelFramework: "TENSORFLOW",
                 servingTool: "KSERVE",
                 requestedInstances: 1
                })
            expect_status_details(201)

            serving = Serving.find_by(project_id: @project[:id], name: name)
            expect(serving[:artifact_version]).to eql 0 # MODEL-ONLY
          end

          it "should create a serving with a new artifact version when artifact version is CREATE and a transformer is specified" do
            name = "testmodel2"
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/", parse_serving_json(
                {name: name,
                 modelPath: "/Projects/#{@project[:projectname]}/Models/mnist",
                 batchingConfiguration: {
                   batchingEnabled: false
                 },
                 modelVersion: 1,
                 modelServer: "TENSORFLOW_SERVING",
                 modelFramework: "TENSORFLOW",
                 servingTool: "KSERVE",
                 transformer: "/Projects/#{@project[:projectname]}/Models/mnist/1/transformer.py",
                 requestedInstances: 1,
                 requestedTransformerInstances: 1
                })
            expect_status_details(201)

            serving = Serving.find_by(project_id: @project[:id], name: name)
            expect(serving[:artifact_version]).to eql 1 # New version
          end

          it "should fail to create a serving with an artifact version and without transformer" do
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/", parse_serving_json(
                {name: "testmodel3",
                 modelPath: "/Projects/#{@project[:projectname]}/Models/mnist",
                 batchingConfiguration: {
                   batchingEnabled: false
                 },
                 modelVersion: 1,
                 artifactVersion: "1",
                 modelServer: "TENSORFLOW_SERVING",
                 modelFramework: "TENSORFLOW",
                 servingTool: "KSERVE",
                 requestedInstances: 1
                })
            expect_status_details(400, error_code: 240019)
            expect_json(usrMsg: "Other than MODEL-ONLY artifacts require a predictor or transformer")
          end

          it "should fail to create a serving with MODEL-ONLY artifact and transformer" do
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/", parse_serving_json(
                {name: "testmodel4",
                 modelPath: "/Projects/#{@project[:projectname]}/Models/mnist",
                 batchingConfiguration: {
                   batchingEnabled: false
                 },
                 modelVersion: 1,
                 artifactVersion: "0", # MODEL-ONLY
                 modelServer: "TENSORFLOW_SERVING",
                 modelFramework: "TENSORFLOW",
                 servingTool: "KSERVE",
                 transformer: "/Projects/#{@project[:projectname]}/Models/mnist/1/transformer.py",
                 requestedInstances: 1,
                 requestedTransformerInstances: 1
                })
            expect_status_details(400, error_code: 240019)
            expect_json(usrMsg: "Predictors and transformers cannot be used in MODEL-ONLY artifacts")
          end

          it "should fail to create a serving with a non-existing artifact version" do
            name = "testmodelnonexistingartifactversion"
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/", parse_serving_json(
                {name: name,
                modelPath: "/Projects/#{@project[:projectname]}/Models/mnist",
                batchingConfiguration: {
                  batchingEnabled: false
                },
                modelVersion: 1,
                artifactVersion: 99,
                modelServer: "TENSORFLOW_SERVING",
                modelFramework: "TENSORFLOW",
                servingTool: "KSERVE",
                transformer: "transformer.py",
                requestedInstances: 1,
                requestedTransformerInstances: 1
                })
            expect_status_details(400, error_code: 240016)
            expect_json(usrMsg: "Transformer script does not exist")
          end

          # predictor

          it "should fail to create a serving with a predictor" do
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/", parse_serving_json(
               {name: "testmodelwithpredictor",
                modelPath: "/Projects/#{@project[:projectname]}/Models/mnist",
                batchingConfiguration: {
                  batchingEnabled: false
                },
                modelVersion: 1,
                modelServer: "TENSORFLOW_SERVING",
                modelFramework: "TENSORFLOW",
                servingTool: "KSERVE",
                predictor: "/Projects/#{@project[:projectname]}/Models/mnist/1/transformer.py",
                requestedInstances: 1,
                requestedTransformerInstances: 1
                })
            expect_status_details(400, error_code: 240020)
          end

          # transformer

          it "should fail to create a serving with invalid transformer script path" do
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/", parse_serving_json(
                {name: "testmodel14",
                 modelPath: "/Projects/#{@project[:projectname]}/Models/mnist",
                 batchingConfiguration: {
                   batchingEnabled: false
                 },
                 modelVersion: 1,
                 modelServer: "TENSORFLOW_SERVING",
                 modelFramework: "TENSORFLOW",
                 servingTool: "KSERVE",
                 transformer: "/Projects/#{@project[:projectname]}/Models/mnist/invalid.ext",
                 requestedInstances: 1,
                 requestedTransformerInstances: 1
                })
            expect_status_details(422)
            expect_json(usrMsg: "Transformer script should have a valid extension: .py, .ipynb")
          end

          it "should fail to create a serving with non-existent transformer script" do
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/", parse_serving_json(
                {name: "testmodel15",
                 modelPath: "/Projects/#{@project[:projectname]}/Models/mnist",
                 batchingConfiguration: {
                   batchingEnabled: false
                 },
                 modelVersion: 1,
                 modelServer: "TENSORFLOW_SERVING",
                 modelFramework: "TENSORFLOW",
                 servingTool: "KSERVE",
                 transformer: "/Projects/#{@project[:projectname]}/Models/mnist/non-existent.py",
                 requestedInstances: 1,
                 requestedTransformerInstances: 1
                })
            expect_status_details(400, error_code: 240016)
            expect_json(usrMsg: "Transformer script does not exist")
          end

          it "should create a serving with a python script as transformer" do
            name = "testmodel16"
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/", parse_serving_json(
                {name: name,
                 modelPath: "/Projects/#{@project[:projectname]}/Models/mnist",
                 batchingConfiguration: {
                   batchingEnabled: false
                 },
                 modelVersion: 1,
                 modelServer: "TENSORFLOW_SERVING",
                 modelFramework: "TENSORFLOW",
                 servingTool: "KSERVE",
                 transformer: "/Projects/#{@project[:projectname]}/Models/mnist/1/transformer.py",
                 requestedInstances: 1,
                 requestedTransformerInstances: 1
                })
            expect_status_details(201)

            serving = Serving.find_by(project_id: @project[:id], name: name)
            expect(serving[:artifact_version]).to eql 2 # New version
          end

          it "should create a serving with a jupyter notebook as transformer" do
            name = "testmodel17"
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/", parse_serving_json(
                {name: name,
                 modelPath: "/Projects/#{@project[:projectname]}/Models/mnist",
                 batchingConfiguration: {
                   batchingEnabled: false
                 },
                 modelVersion: 1,
                 modelServer: "TENSORFLOW_SERVING",
                 modelFramework: "TENSORFLOW",
                 servingTool: "KSERVE",
                 transformer: "/Projects/#{@project[:projectname]}/Models/mnist/1/transformer.ipynb",
                 requestedInstances: 1,
                 requestedTransformerInstances: 1
                })
            expect_status_details(201)

            serving = Serving.find_by(project_id: @project[:id], name: name)
            expect(serving[:artifact_version]).to eql 3 # New version
          end

          it "should fail to create a serving with an existent artifact but different transformer script" do
            copy("/Projects/#{@project[:projectname]}/Models/mnist/1/transformer.py",
                 "/Projects/#{@project[:projectname]}/Models/mnist/1/transformer-copy.py",
                 @user[:username], "#{@project[:projectname]}__Models", 750, "#{@project[:projectname]}")

            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/", parse_serving_json(
                {name: "testmodel18",
                 modelPath: "/Projects/#{@project[:projectname]}/Models/mnist",
                 batchingConfiguration: {
                   batchingEnabled: false
                 },
                 modelVersion: 1,
                 artifactVersion: 1,
                 modelServer: "TENSORFLOW_SERVING",
                 modelFramework: "TENSORFLOW",
                 servingTool: "KSERVE",
                 transformer: "/Projects/#{@project[:projectname]}/Models/mnist/1/transformer-copy.py",
                 requestedInstances: 1,
                 requestedTransformerInstances: 1
                })
            expect_status_details(400, error_code: 240019)
            expect_json(usrMsg: "Existing artifacts cannot be modified. To change predictors or transformers, create a new artifact")
          end

          # kafka topic

          it "should fail to create a serving with an existing kafka topic with inferenceschema version 1" do
            # Create kafka topic
            json, topic_name = add_topic(@project[:id], INFERENCE_SCHEMA_NAME, 1)

            # Create serving
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/", parse_serving_json(
                {name: "testmodeltopicschema1",
                 modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
                 modelVersion: 1,
                 batchingConfiguration: {
                   batchingEnabled: false
                 },
                 kafkaTopicDTO: {
                     name: topic_name
                 },
                 inferenceLogging: "ALL",
                 modelServer: "TENSORFLOW_SERVING",
                 modelFramework: "TENSORFLOW",
                 servingTool: "KSERVE",
                 requestedInstances: 1
                })
            expect_status_details(400, error_code: 240023)
            expect_json(usrMsg: "Inference logging in KServe deployments requires schema version 4 or greater")
          end

          it "should fail to create a serving with an existing kafka topic with inferenceschema version 2" do
            # Create kafka topic
            json, topic_name = add_topic(@project[:id], INFERENCE_SCHEMA_NAME, 2)

            # Create serving
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/", parse_serving_json(
                {name: "testmodeltopicschema2",
                 modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
                 modelVersion: 1,
                 batchingConfiguration: {
                   batchingEnabled: false
                 },
                 kafkaTopicDTO: {
                     name: topic_name
                 },
                 inferenceLogging: "ALL",
                 modelServer: "TENSORFLOW_SERVING",
                 modelFramework: "TENSORFLOW",
                 servingTool: "KSERVE",
                 requestedInstances: 1
                })
            expect_status_details(400, error_code: 240023)
            expect_json(usrMsg: "Inference logging in KServe deployments requires schema version 4 or greater")
          end

          it "should fail to create a serving with an existing kafka topic with inferenceschema version 3" do
            # Create kafka topic
            json, topic_name = add_topic(@project[:id], INFERENCE_SCHEMA_NAME, 3)

            # Create serving
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/", parse_serving_json(
                {name: "testmodeltopicschema3",
                 modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
                 modelVersion: 1,
                 batchingConfiguration: {
                   batchingEnabled: false
                 },
                 kafkaTopicDTO: {
                     name: topic_name
                 },
                 inferenceLogging: "ALL",
                 modelServer: "TENSORFLOW_SERVING",
                 modelFramework: "TENSORFLOW",
                 servingTool: "KSERVE",
                 requestedInstances: 1
                })
            expect_status_details(400, error_code: 240023)
            expect_json(usrMsg: "Inference logging in KServe deployments requires schema version 4 or greater")
          end

          it "should create a serving with an existing kafka topic with inferenceschema version 4" do
            # Create kafka topic
            json, topic_name = add_topic(@project[:id], INFERENCE_SCHEMA_NAME, 4)

            # Create serving
            name = "testmodeltopicschema4"
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/", parse_serving_json(
                {name: name,
                 modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
                 modelVersion: 1,
                 batchingConfiguration: {
                   batchingEnabled: false
                 },
                 kafkaTopicDTO: {
                     name: topic_name
                 },
                 inferenceLogging: "ALL",
                 modelServer: "TENSORFLOW_SERVING",
                 modelFramework: "TENSORFLOW",
                 servingTool: "KSERVE",
                 requestedInstances: 1
                })
            expect_status_details(201)

            # Kafka authorizer needs some time to take up the new permissions.
            sleep(5)

            # Check that the serving is actually using that topic
            serving_list = get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/"
            kafka_topic_name = JSON.parse(serving_list).select { |serving| serving['name'] == name}[0]['kafkaTopicDTO']['name']
            expect(kafka_topic_name).to eql topic_name
          end

          # resources config

          it "should create the serving with default predictorResources if not set" do
            name = "res1model"
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/", parse_serving_json(
                {name: name,
                 modelPath: "/Projects/#{@project[:projectname]}/Models/mnist",
                 modelVersion: 1,
                 batchingConfiguration: {
                   batchingEnabled: false
                 },
                 modelServer: "TENSORFLOW_SERVING",
                 modelFramework: "TENSORFLOW",
                 servingTool: "KSERVE",
                 requestedInstances: 1
                })
            expect_status_details(201)

            serving_list = get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/"
            resource_config = JSON.parse(serving_list).select { |serving| serving['name'] == name}[0]['predictorResources']
            expect(resource_config['requests']['memory']).to be 32
            expect(resource_config['requests']['cores']).to be 0.2
            expect(resource_config['requests']['gpus']).to be 0
          end

          it "should create the serving with overridden predictorResources" do
            name = "res2model"
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/", parse_serving_json(
                {name: name,
                 modelPath: "/Projects/#{@project[:projectname]}/Models/mnist",
                 modelVersion: 1,
                 batchingConfiguration: {
                   batchingEnabled: false
                 },
                 modelServer: "TENSORFLOW_SERVING",
                 modelFramework: "TENSORFLOW",
                 servingTool: "KSERVE",
                 requestedInstances: 1,
                 predictorResources: {
                   requests: {
                    memory: 1000,
                    cores: 2.0,
                    gpus: 1
                   },
                   limits: {
                    memory: 2000,
                    cores: 3.0,
                    gpus: 2
                   }
                }
                })
            expect_status_details(201)

            serving_list = get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/"
            resource_config = JSON.parse(serving_list).select { |serving| serving['name'] == name}[0]['predictorResources']
            expect(resource_config['requests']['memory']).to be 1000
            expect(resource_config['requests']['cores']).to be 2.0
            expect(resource_config['requests']['gpus']).to be 1
            expect(resource_config['limits']['memory']).to be 2000
            expect(resource_config['limits']['cores']).to be 3.0
            expect(resource_config['limits']['gpus']).to be 2
          end

          # request batching

          it "should create a serving with request batching enabled and no extra batching configuration" do
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/", parse_serving_json(
                {name: "testrequestbatchingtensorflowkserve1",
                 modelPath: "/Projects/#{@project[:projectname]}/Models/mnist",
                 modelVersion: 1,
                 batchingConfiguration: {
                   batchingEnabled: true
                 },
                 kafkaTopicDTO: {
                     name: "NONE"
                 },
                 modelServer: "TENSORFLOW_SERVING",
                 modelFramework: "TENSORFLOW",
                 servingTool: "KSERVE",
                 requestedInstances: 1
                })
            expect_status_details(201)
          end

          it "should create a serving with request batching enabled and extra batching configuration provided" do
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/", parse_serving_json(
                {name: "testrequestbatchingtensorflowkserve2",
                 modelPath: "/Projects/#{@project[:projectname]}/Models/mnist",
                 modelVersion: 1,
                 batchingConfiguration: {
                   batchingEnabled: true,
                   maxBatchSize: 32,
                   maxLatency: 5000,
                   timeout: 60
                 },
                 kafkaTopicDTO: {
                   name: "NONE"
                 },
                 modelServer: "TENSORFLOW_SERVING",
                 modelFramework: "TENSORFLOW",
                 servingTool: "KSERVE",
                 requestedInstances: 1
                })
            expect_status_details(201)
          end

          # transformer instances

          it "should fail to create a serving with transformer and without requested instances" do
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/", parse_serving_json(
                 {name: "testmodel25",
                  modelPath: "/Projects/#{@project[:projectname]}/Models/mnist",
                  batchingConfiguration: {
               batchingEnabled: false
             },
                  modelVersion: 1,
                  modelServer: "TENSORFLOW_SERVING",
                  modelFramework: "TENSORFLOW",
                  servingTool: "KSERVE",
                  transformer: "/Projects/#{@project[:projectname]}/Models/mnist/1/transformer.ipynb",
                  requestedInstances: 1
                 })
            expect_status_details(422)
            expect_json(usrMsg: "Number of transformer instances must be provided with a transformer")
          end

          it "should fail to create a serving with transformer instances but without transformer" do
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/", parse_serving_json(
                 {name: "testmodel26",
                  modelPath: "/Projects/#{@project[:projectname]}/Models/mnist",
                  batchingConfiguration: {
                    batchingEnabled: false
                  },
                  modelVersion: 1,
                  modelServer: "TENSORFLOW_SERVING",
                  modelFramework: "TENSORFLOW",
                  servingTool: "KSERVE",
                  requestedInstances: 1,
                  requestedTransformerInstances: 1
                 })
            expect_status_details(422)
            expect_json(usrMsg: "Number of transformer instances cannot be provided without a transformer")
          end

          it "should create a serving with transformer and requested transformer instances" do
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/", parse_serving_json(
                 {name: "testmodel27",
                  modelPath: "/Projects/#{@project[:projectname]}/Models/mnist",
                  batchingConfiguration: {
                    batchingEnabled: false
                  },
                  modelVersion: 1,
                  modelServer: "TENSORFLOW_SERVING",
                  modelFramework: "TENSORFLOW",
                  servingTool: "KSERVE",
                  transformer: "/Projects/#{@project[:projectname]}/Models/mnist/1/transformer.ipynb",
                  requestedInstances: 1,
                  requestedTransformerInstances: 1
                 })
            expect_status_details(201)
          end
        end
      end

      describe "#update", vm: true do
        before :all do
          with_valid_project
          with_kserve_tensorflow(@project[:id], @project[:projectname], @user[:username])
        end

        after :all do
          delete_all_servings(@project[:id])
          purge_all_kserve_instances(@project[:projectname])
        end

        after :each do
          sleep(10)
        end

        # transformer

        it "should be able to update a serving with a transformer script and new artifact version" do
          copy("/Projects/#{@project[:projectname]}/Models/mnist/1/transformer.py",
               "/Projects/#{@project[:projectname]}/Models/mnist/1/transformer-copy-2.py",
               @user[:username], "#{@project[:projectname]}__Models", 750, "#{@project[:projectname]}")

          serving = Serving.find(@serving[:id])

          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/", parse_serving_json(
              {id: serving[:id],
               name: serving[:name],
               modelPath: serving[:model_path],
               modelVersion: serving[:model_version],
               artifactVersion: -1, # New version
               batchingConfiguration: serving[:batching_configuration],
               modelServer: parse_model_server(serving[:model_server]),
               modelFramework: parse_model_framework(serving[:model_framework]),
               servingTool: parse_serving_tool(serving[:serving_tool]),
               transformer: "/Projects/#{@project[:projectname]}/Models/mnist/1/transformer-copy-2.py",
               requestedInstances: serving[:instances],
               requestedTransformerInstances: 1,
              })
          expect_status_details(201)

          serving = Serving.find(@serving[:id])
          expect(serving[:artifact_version]).to be > 0

          wait_result = wait_for_me_time(60) do
            get_datasets_in_path(@project, "#{serving[:model_path]}/#{serving[:model_version]}/Artifacts/#{serving[:artifact_version]}", query: "&type=DATASET")
            ds = json_body[:items].detect { |d| d[:attributes][:name] == "mnist_#{serving[:model_version]}_#{serving[:artifact_version]}.zip" }
            { "success" => ds.present?, "ds" => ds }
          end
          expect(wait_result['ds']).to be_present
        end

        it "should not be able to update a serving with existing artifact version and new transformer script" do
          copy("/Projects/#{@project[:projectname]}/Models/mnist/1/transformer.py",
               "/Projects/#{@project[:projectname]}/Models/mnist/1/transformer-copy-3.py",
               @user[:username], "#{@project[:projectname]}__Models", 750, "#{@project[:projectname]}")

          serving = Serving.find(@serving[:id])

          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/", parse_serving_json(
              {id: serving[:id],
               name: serving[:name],
               modelPath: serving[:model_path],
               modelVersion: serving[:model_version],
               artifactVersion: serving[:artifact_version],
               batchingConfiguration: serving[:batching_configuration],
               modelServer: parse_model_server(serving[:model_server]),
               modelFramework: parse_model_framework(serving[:model_framework]),
               servingTool: parse_serving_tool(serving[:serving_tool]),
               transformer: "/Projects/#{@project[:projectname]}/Models/mnist/1/transformer-copy-3.py",
               requestedInstances: serving[:instances],
               requestedTransformerInstances: 1,
              })
          expect_status_details(400, error_code: 240019)
          expect_json(usrMsg: "Existing artifacts cannot be modified. To change predictors or transformers, create a new artifact")
        end

        # kafka topic

        it "should be able to update the inference logging mode" do
          serving = Serving.find(@serving[:id])
          topic = ProjectTopics.find(@serving[:kafka_topic_id])
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/", parse_serving_json(
              {id: serving[:id],
               name: serving[:name],
               modelPath: serving[:model_path],
               modelVersion: serving[:model_version],
               batchingConfiguration: serving[:batching_configuration],
               kafkaTopicDTO: {
                   name: topic[:topic_name]
               },
               inferenceLogging: "PREDICTIONS",
               modelServer: parse_model_server(serving[:model_server]),
               modelFramework: parse_model_framework(serving[:model_framework]),
               servingTool: parse_serving_tool(serving[:serving_tool]),
               requestedInstances: serving[:instances]
              })
          expect_status_details(201)

          serving = Serving.find(@serving[:id])
          expect(parse_inference_logging(serving[:inference_logging])).to eql "PREDICTIONS"
        end

        # number of instances

        it "should be able to update the number of instances of the predictor and transformer" do
          serving = Serving.find(@serving[:id])
          name = "testmodelonlytrans"
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/", parse_serving_json(
          {id: serving[:id],
           name: name,
           modelPath: serving[:model_path],
           modelVersion: serving[:model_version],
           batchingConfiguration: serving[:batching_configuration],
           modelServer: parse_model_server(serving[:model_server]),
           modelFramework: parse_model_framework(serving[:model_framework]),
           servingTool: parse_serving_tool(serving[:serving_tool]),
           transformer: "/Projects/#{@project[:projectname]}/Models/mnist/1/transformer.py",
           requestedInstances: 1,
           requestedTransformerInstances: 1,
          })
          expect_status_details(201)

          serving = Serving.find_by(project_id: @project[:id], name: name)
          expect(serving[:instances]).to eql 1
          expect(serving[:transformer_instances]).to eql 1

          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/", parse_serving_json(
              {id: serving[:id],
               name: serving[:name],
               modelPath: serving[:model_path],
               modelVersion: serving[:model_version],
               artifactVersion: serving[:artifact_version],
               batchingConfiguration: serving[:batching_configuration],
               modelServer: parse_model_server(serving[:model_server]),
               modelFramework: parse_model_framework(serving[:model_framework]),
               servingTool: parse_serving_tool(serving[:serving_tool]),
               transformer: "transformer.py",
               requestedInstances: 2,
               requestedTransformerInstances: 2,
              })
          expect_status_details(201)

          serving = Serving.find(serving[:id])
          expect(serving[:instances]).to eql 2
          expect(serving[:transformer_instances]).to eql 2
        end

        # request batching

        it "should should update request batching " do
          serving = Serving.find(@serving[:id])
          name = "testupdaterequestbatching"
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/", parse_serving_json(
              {id: serving[:id],
               name: name,
               modelPath: serving[:model_path],
               modelVersion: serving[:model_version],
               batchingConfiguration: serving[:batching_configuration],
               modelServer: parse_model_server(serving[:model_server]),
               modelFramework: parse_model_framework(serving[:model_framework]),
               servingTool: parse_serving_tool(serving[:serving_tool]),
               transformer: "/Projects/#{@project[:projectname]}/Models/mnist/1/transformer.py",
               requestedInstances: 1,
               requestedTransformerInstances: 1,
              })
          expect_status_details(201)

          serving = Serving.find_by(project_id: @project[:id], name: name)
          expect(JSON.parse(serving[:batching_configuration])['batchingEnabled']).to eql false

          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/", parse_serving_json(
              {id: serving[:id],
               name: serving[:name],
               modelPath: serving[:model_path],
               modelVersion: serving[:model_version],
               artifactVersion: serving[:artifact_version],
               batchingConfiguration: {
                 batchingEnabled: true,
                 maxBatchSize: 32,
                 maxLatency: 5000,
                 timeout: 60
               },
               modelServer: parse_model_server(serving[:model_server]),
               modelFramework: parse_model_framework(serving[:model_framework]),
               servingTool: parse_serving_tool(serving[:serving_tool]),
               transformer: "transformer.py",
               requestedInstances: 2,
               requestedTransformerInstances: 2,
              })
          expect_status_details(201)
          expect(JSON.parse(serving[:batching_configuration])['batchingEnabled']).to eql false

          serving = Serving.find(serving[:id])
          expect(JSON.parse(serving[:batching_configuration])['batchingEnabled']).to eql true
          expect(JSON.parse(serving[:batching_configuration])['maxBatchSize']).to eql 32
          expect(JSON.parse(serving[:batching_configuration])['maxLatency']).to eql 5000
          expect(JSON.parse(serving[:batching_configuration])['timeout']).to eql 60
        end
      end

      describe "#start", vm: true do
        before :all do
          with_valid_project
          with_kserve_tensorflow(@project[:id], @project[:projectname], @user[:username])
        end

        after :all do
          delete_all_servings(@project[:id])
          purge_all_kserve_instances(@project[:projectname])
        end

        it "should be able to start a serving instance" do
          start_serving(@project, @serving)
          wait_for_serving_status(@project, @serving[:name], ["Running"])

          # Check that the logs are written in the opensearch index.
          wait_result = wait_for_me_time(30, 4) do
            result = opensearch_rest do
              response = opensearch_get "#{@project[:projectname].downcase}_serving*/_search?q=serving_name:#{@serving[:name]}"
              index = response.body
              parsed_index = JSON.parse(index)
              hits = parsed_index['hits']['total']['value']
              { 'success' => hits > 0, 'hits' => hits}
            end
            result
          end
          expect(wait_result["hits"]).to be > 0
        end

        it "should fail to start a running instance" do
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}?action=start"
          expect_status_details(400, error_code: 240003)
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}?action=stop"
        end

        it "should be able to start a serving instance with transformer" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/", parse_serving_json(
               {id: @serving[:id],
                name: @serving[:name],
                modelPath: @serving[:model_path],
                batchingConfiguration: @serving[:batching_configuration],
                modelVersion: @serving[:model_version],
                modelServer: parse_model_server(@serving[:model_server]),
                modelFramework: parse_model_framework(@serving[:model_framework]),
                servingTool: parse_serving_tool(@serving[:serving_tool]),
                transformer: "/Projects/#{@project[:projectname]}/Models/mnist/1/transformer.ipynb",
                requestedInstances: @serving[:instances],
                requestedTransformerInstances: 1
               })
          expect_status_details(201)

          start_serving(@project, @serving)
          wait_for_serving_status(@project, @serving[:name], ["Running"])

          # Check that the logs are written in the opensearch index.
          wait_result = wait_for_me_time(30, 4) do
            opensearch_rest do
              response = opensearch_get "#{@project[:projectname].downcase}_serving*/_search?q=serving_name:#{@serving[:name]}"
              index = response.body
              parsed_index = JSON.parse(index)
              hits = parsed_index['hits']['total']['value']
              { 'success' => hits > 0, 'hits' => hits}
            end
          end
          expect(wait_result["hits"]).to be > 0

          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}?action=stop"
        end
      end

      describe "#kill", vm: true do
        before :all do
          with_valid_project
          with_kserve_tensorflow(@project[:id], @project[:projectname], @user[:username])
        end

        after :all do
          delete_all_servings(@project[:id])
        end

        it "should fail to kill a non running instance" do
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}?action=stop"
          expect_status_details(400, error_code: 240003)
        end

        it "should be able to kill a running serving instance" do
          start_serving(@project, @serving)
          wait_for_serving_status(@project, @serving[:name], ["Running"])

          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}?action=stop"
          expect_status_details(200)

          check_process_running("tensorflow_model_server")
        end
      end

      describe "#delete", vm: true do
        before :all do
          with_valid_project
          copy_mnist_files(@project[:projectname], @user[:username])
        end

        before :each do
          @serving = create_kserve_tensorflow(@project[:id], @project[:projectname])
        end

        it "should be able to delete a serving instance" do
          delete "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}"
          expect_status_details(200)
        end

        it "should be able to delete a running instance" do
          start_serving(@project, @serving)
          wait_for_serving_status(@project, @serving[:name], ["Running"])

          delete "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}"
          expect_status_details(200)

          sleep(5)

          check_process_running("tensorflow_model_server")
        end
      end
    end
  end
end
