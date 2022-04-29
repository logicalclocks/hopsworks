=begin
 Copyright (C) 2022, Logical Clocks AB. All rights reserved
=end

# serving_kserve_sklearn_inference_spec.rb: Tests for making inference with sklearn models on KServe

describe "On #{ENV['OS']}" do

  before :all do
    if !kserve_installed
      skip "This test only runs with KServe installed"
    end
    with_admin_session()
    with_valid_project
    admin_update_user(@user[:uid], {status: "ACTIVATED_ACCOUNT"})
    @admin_user = @user

    with_kserve_sklearn(@project[:id], @project[:projectname], @user[:username])
  end

  after (:all) do
    clean_all_test_projects(spec: "serving_kserve_sklearn_inference")
    purge_all_kserve_instances
  end

  describe 'inference' do

    let(:test_data) {
      [
          [2.496980740040751, 4.7732122731342805, 3.451636599101792, 2.4535334273371285],
          [2.8691773509649345, 1.5165563288683765, 4.687886086087035, 5.957422837863767],
          [2.4598251584768143, 3.010853794745395, 2.8037439502023704, 4.918140241551333],
          [2.6556101758261903, 7.268154644213865, 4.7387524652868445, 4.770835967099467],
          [3.9682516036046436, 5.571227258700232, 4.226145217398939, 5.221222221285237]
      ]
    }

    context 'without authentication', vm: true do

      before :all do
        reset_session
      end

      it "the inference should fail" do
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/inference/models/#{@serving[:name]}:predict", {
            instances: test_data
        }
        expect_status_details(401, error_code: 200003)
      end
    end

    context 'with JWT authentication', vm: true do

      before :all do
        create_session(@admin_user[:email], "Pass123")
      end

      it "should fail to send a request to a non existing model"  do
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/inference/models/nonexistingmodel:predict", {
            instances: test_data
        }
        expect_status_details(404)
      end

      context 'with running model do' do

        before :all do
          start_serving(@project, @serving)
          wait_for_serving_status(@serving[:name], "Running")
        end

        after :all do
          stop_serving(@project, @serving)
          wait_for_serving_status(@serving[:name], "Stopped")
        end

        it "should fail to infer from a KServe serving with JWT authentication" do
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/inference/models/#{@serving[:name]}:predict", {
              instances: test_data
          }
          expect_status_details(400, error_code: 250009)
        end
      end
    end

    context 'with API Key authentication' do

      before(:all) do
        create_session(@admin_user[:email], "Pass123")

        @key = create_api_key("serving_test_#{random_id_len(4)}", %w(SERVING))
        @invalid_scope_key = create_api_key("serving_test_#{random_id_len(4)}", %w(DATASET_VIEW DATASET_CREATE
        DATASET_DELETE))

        reset_session
        set_api_key_to_header(@key)
      end

      it "should fail to send a request to a non existing model"  do
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/inference/models/nonexistingmodel:predict", {
            instances: test_data
        }
        expect_status_details(404)
      end

      it "should fail to send a request to a non running model" do
        serving = get_serving(@serving[:name])
        expect(serving[:status]).not_to eq("RUNNING")

        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/inference/models/#{@serving[:name]}:predict", {
            instances: test_data
        }
        expect_status_details(400, error_code: 250001)
      end

      context 'with running model do' do
        before :all do
          start_serving(@project, @serving)
          wait_for_serving_status(@serving[:name], "Running")
        end

        after :all do
          stop_serving(@project, @serving)
          wait_for_serving_status(@serving[:name], "Stopped")
        end

        context "through Hopsworks REST API" do

          context 'with invalid API key' do

            before :each do
              set_api_key_to_header(@invalid_scope_key)
            end

            it 'should fail to access inference end-point with invalid scope' do
              post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/inference/models/#{@serving[:name]}:predict", {
                  instances: test_data
              }
              expect_status_details(403, error_code: 320004)
            end
          end

          context 'with valid API Key' do

            before :all do
              set_api_key_to_header(@key)

              # backup
              copy("/Projects/#{@project[:projectname]}/Models/irisflowerclassifier/1/iris_knn.pkl",
                   "/Projects/#{@project[:projectname]}/Models/irisflowerclassifier/iris_knn_copy.pkl",
                   @admin_user[:username], "#{@project[:projectname]}__Models", 750, "#{@project[:projectname]}")

              mkdir("/Projects/#{@project[:projectname]}/Models/irisflowerclassifiercopy/", @admin_user[:username], "#{@project[:projectname]}__Models", 750)

              copy("/Projects/#{@project[:projectname]}/Models/irisflowerclassifier/*",
                   "/Projects/#{@project[:projectname]}/Models/irisflowerclassifiercopy/",
                   @admin_user[:username], "#{@project[:projectname]}__Models", 750, "#{@project[:projectname]}")

              rm("/Projects/#{@project[:projectname]}/Models/irisflowerclassifiercopy/1/iris_knn.pkl")
            end

            it "should succeed to infer from a serving with kafka logging" do
              post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/inference/models/#{@serving[:name]}:predict", {
                  instances: test_data
              }
              expect_status_details(200)
            end

            it "should receive an error if the input payload is malformed" do
              post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/inference/models/#{@serving[:name]}:predict", {
                  somethingwrong: test_data
              }
              expect_status_details(500)
            end

            it "should receive an error if the input payload is empty" do
              post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/inference/models/#{@serving[:name]}:predict"
              expect_status_details(400, error_code: 250008)
            end

            it "should succeed to infer from a serving with no kafka logging" do
              stop_serving(@project, @serving)
              wait_for_serving_status(@serving[:name], "Stopped")

              put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
               {id: @serving[:id],
                name: @serving[:name],
                modelPath: @serving[:model_path],
                modelVersion: @serving[:model_version],
                batchingEnabled: @serving[:enable_batching],
                modelServer: parse_model_server(@serving[:model_server]),
                servingTool: parse_serving_tool(@serving[:serving_tool]),
                requestedInstances: @serving[:instances]
               }
              expect_status_details(201)

              start_serving(@project, @serving)
              wait_for_serving_status(@serving[:name], "Running")

              post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/inference/models/#{@serving[:name]}:predict", {
                  instances: test_data
              }
              expect_status_details(200)
              expect(json_body).to include :predictions
            end

            it "should succeed to infer from a serving with model file and without predictor and transformer" do
              stop_serving(@project, @serving)
              wait_for_serving_status(@serving[:name], "Stopped")

              put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
               {id: @serving[:id],
                name: @serving[:name],
                modelPath: @serving[:model_path],
                modelVersion: @serving[:model_version],
                kafkaTopicDTO: {
                   name: "NONE"
                },
                modelServer: parse_model_server(@serving[:model_server]),
                servingTool: parse_serving_tool(@serving[:serving_tool]),
                requestedInstances: @serving[:instances]
               }
              expect_status_details(201)
              
              start_serving(@project, @serving)
              wait_for_serving_status(@serving[:name], "Running")

              post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/inference/models/#{@serving[:name]}:predict", {
                  instances: test_data
              }
              expect_status_details(200)
              expect(json_body).to include :predictions
            end            

            it "should succeed to infer from a serving with model file and transformer and without predictor" do
              stop_serving(@project, @serving)
              wait_for_serving_status(@serving[:name], "Stopped")

              put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
               {id: @serving[:id],
                name: @serving[:name],
                modelPath: @serving[:model_path],
                modelVersion: @serving[:model_version],
                batchingEnabled: false,
                kafkaTopicDTO: {
                   name: "NONE"
                },
                modelServer: parse_model_server(@serving[:model_server]),
                servingTool: parse_serving_tool(@serving[:serving_tool]),
                transformer: "/Projects/#{@project[:projectname]}/Models/irisflowerclassifier/1/transformer.py",
                requestedInstances: @serving[:instances],
                requestedTransformerInstances: 1
               }
              expect_status_details(201)

              start_serving(@project, @serving)
              wait_for_serving_status(@serving[:name], "Running")

              post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/inference/models/#{@serving[:name]}:predict", {
                  instances: test_data
              }
              expect_status_details(200)
              expect(json_body).to include :predictions
            end

            it "should succeed to infer from a serving without model file and transformer but with predictor" do
              stop_serving(@project, @serving)
              wait_for_serving_status(@serving[:name], "Stopped")

              put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
               {id: @serving[:id],
                name: @serving[:name],
                modelPath: "/Projects/#{@project[:projectname]}/Models/irisflowerclassifiercopy",
                modelVersion: @serving[:model_version],
                batchingEnabled: false,
                kafkaTopicDTO: {
                   name: "NONE"
                },
                modelServer: parse_model_server(@serving[:model_server]),
                servingTool: parse_serving_tool(@serving[:serving_tool]),
                predictor: "/Projects/#{@project[:projectname]}/Models/irisflowerclassifiercopy/1/#{SKLEARN_SCRIPT_FILE_NAME}",
                requestedInstances: @serving[:instances]
               }
              expect_status_details(201)

              start_serving(@project, @serving)
              wait_for_serving_status(@serving[:name], "Running")

              post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/inference/models/#{@serving[:name]}:predict", {
                  inputs: test_data
              }
              expect_status_details(200)
              expect(json_body).to include :predictions
            end            

            it "should succeed to infer from a serving without model file but with predictor and transformer" do
              stop_serving(@project, @serving)
              wait_for_serving_status(@serving[:name], "Stopped")

              put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
               {id: @serving[:id],
                name: @serving[:name],
                modelPath: "/Projects/#{@project[:projectname]}/Models/irisflowerclassifiercopy",
                modelVersion: @serving[:model_version],
                batchingEnabled: false,
                kafkaTopicDTO: {
                   name: "NONE"
                },
                modelServer: parse_model_server(@serving[:model_server]),
                servingTool: parse_serving_tool(@serving[:serving_tool]),
                predictor: "/Projects/#{@project[:projectname]}/Models/irisflowerclassifiercopy/1/#{SKLEARN_SCRIPT_FILE_NAME}",
                transformer: "/Projects/#{@project[:projectname]}/Models/irisflowerclassifiercopy/1/transformer.py",
                requestedInstances: @serving[:instances],
                requestedTransformerInstances: 1
               }
              expect_status_details(201)

              start_serving(@project, @serving)
              wait_for_serving_status(@serving[:name], "Running")

              post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/inference/models/#{@serving[:name]}:predict", {
                  inputs: test_data
              }
              expect_status_details(200)
              expect(json_body).to include :predictions
            end
          end
        end

        context 'through Istio ingress gateway' do

          before :all do
            # refresh serving once it's running to get internal IPs and port
            @serving = Serving.find(@serving[:id])
            wait_for_serving_status(@serving[:name], "Running")

            @serving_endpoints = get_serving(@serving[:name])
          end

          context 'with valid API Key' do

            before :all do
              set_api_key_to_header(@key)
            end

            it 'should succeed to infer' do
              make_prediction_request_istio(@project[:projectname], @serving[:name], @serving_endpoints, { inputs: test_data })
              expect_status_details(200)
              expect(json_body).to include :predictions
            end
          end

          context 'with invalid API Key' do

            it 'should fail to send inference request with invalid raw secret' do
              invalid_key = @key + "typo"
              set_api_key_to_header(invalid_key)
              make_prediction_request_istio(@project[:projectname], @serving[:name], @serving_endpoints, { inputs: test_data })
              expect_status(401)
              expect_json(userMsg: "could not authenticate API key")
            end

            it 'should fail to send inference request with non-existent prefix' do
              invalid_key = "typo" + @key
              set_api_key_to_header(invalid_key)
              make_prediction_request_istio(@project[:projectname], @serving[:name], @serving_endpoints, { inputs: test_data })
              expect_status(401)
              expect_json(userMsg: "invalid or non-existent api key")
            end

            it 'should fail to send inference request with invalid scope' do
              set_api_key_to_header(@invalid_scope_key)
              make_prediction_request_istio(@project[:projectname], @serving[:name], @serving_endpoints, { inputs: test_data })
              expect_status(401)
              expect_json(userMsg: "invalid or non-existent api key")
            end

            it 'should fail to send inference request with invalid user role' do
              project_name =  @project[:projectname]

              cm = get_serving_kube_config_map
              expect(cm).not_to be_empty
              expect(cm).to include("data")
              expect(cm["data"]).to include("authenticator")
              authenticator = JSON.parse(cm["data"]["authenticator"])
              expect(authenticator).to include("allowedUserRoles")
              expect(authenticator).to include("allowedProjectUserRoles")

              result = update_authenticator_kube_config_map(["HOPS_ADMIN"], authenticator["allowedProjectUserRoles"])
              expect(result).not_to be_nil
              expect(result).to eql("configmap/hops-system--serving patched\n")
              restart_authenticator

              create_session(@admin_user[:email], "Pass123")
              newUser = create_user_with_role("HOPS_USER")
              admin_update_user(newUser.uid, {status: "ACTIVATED_ACCOUNT"})
              expect_status(200)
              add_member_to_project(@project, newUser[:email], "Data owner")
              create_session(newUser[:email], "Pass123")
              key = create_api_key("serving_test_#{random_id_len(4)}", %w(SERVING))

              reset_session
              set_api_key_to_header(key)

              make_prediction_request_istio(project_name, @serving[:name], @serving_endpoints, { inputs: test_data })
              expect_status(403)
              expect_json(userMsg: "unauthorized user, user role not allowed")

              result = update_authenticator_kube_config_map(authenticator["allowedUserRoles"], authenticator["allowedProjectUserRoles"])
              expect(result).not_to be_nil
              expect(result).to eql("configmap/hops-system--serving patched\n")
              restart_authenticator
            end

            it 'should fail to send inference request with invalid project user (member) role' do
              project_name =  @project[:projectname]

              cm = get_serving_kube_config_map
              expect(cm).not_to be_empty
              expect(cm).to include("data")
              expect(cm["data"]).to include("authenticator")
              authenticator = JSON.parse(cm["data"]["authenticator"])
              expect(authenticator).to include("allowedProjectUserRoles")

              result = update_authenticator_kube_config_map(authenticator["allowedUserRoles"], ["Data owner"])
              expect(result).not_to be_nil
              expect(result).to eql("configmap/hops-system--serving patched\n")
              restart_authenticator

              create_session(@admin_user[:email], "Pass123")
              newUser = create_user_with_role("HOPS_ADMIN")
              admin_update_user(newUser.uid, {status: "ACTIVATED_ACCOUNT"})
              expect_status(200)
              add_member_to_project(@project, newUser[:email], "Data scientist")

              create_session(newUser[:email], "Pass123")
              key = create_api_key("serving_test_#{random_id_len(4)}", %w(SERVING))

              reset_session
              set_api_key_to_header(key)

              make_prediction_request_istio(project_name, @serving[:name], @serving_endpoints, { inputs: test_data })
              expect_status(403)
              expect_json(userMsg: "unauthorized user, project member roles not allowed")

              result = update_authenticator_kube_config_map(authenticator["allowedUserRoles"], authenticator["allowedProjectUserRoles"])
              expect(result).not_to be_nil
              expect(result).to eql("configmap/hops-system--serving patched\n")
              restart_authenticator
            end

            it 'should fail to send inference request with a non-member' do
              project_name =  @project[:projectname]

              create_session(@admin_user[:email], "Pass123")
              newUser = create_user_with_role("HOPS_ADMIN")
              admin_update_user(newUser.uid, {status: "ACTIVATED_ACCOUNT"})
              expect_status(200)
              create_session(newUser[:email], "Pass123")
              key = create_api_key("serving_test_#{random_id_len(4)}", %w(SERVING))

              reset_session
              set_api_key_to_header(key)

              make_prediction_request_istio(project_name, @serving[:name], @serving_endpoints, { inputs: test_data })
              expect_status(403)
              expect_json(userMsg: "unauthorized user, not a member of the project")
            end

            it 'should fail to send inference request with a deactivated user' do
              project_name =  @project[:projectname]

              create_session(@admin_user[:email], "Pass123")
              newUser = create_user_with_role("HOPS_ADMIN")
              expect_status(200)
              add_member(newUser[:email], "Data scientist")
              create_session(newUser[:email], "Pass123")
              key = create_api_key("serving_test_#{random_id_len(4)}", %w(SERVING))

              reset_session
              set_api_key_to_header(key)

              make_prediction_request_istio(project_name, @serving[:name], @serving_endpoints, { inputs: test_data })
              expect_status(403)
              expect_json(userMsg: "unauthorized user, account status not activated")
            end
          end
        end
      end
    end
  end
end
