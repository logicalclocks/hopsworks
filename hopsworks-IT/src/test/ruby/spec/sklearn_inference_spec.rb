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
  after (:all) do
    clean_all_test_projects
    purge_all_sklearn_serving_instances
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

    describe "#infer" do
      context 'without authentication', vm: true do
        before :all do
          with_valid_project
          with_python_enabled(@project[:id], "3.6")
          sleep(10)
          with_sklearn_serving(@project[:id], @project[:projectname], @user[:username])
          sleep(10)
          reset_session
        end

        after :all do
          purge_all_sklearn_serving_instances()
          delete_all_sklearn_serving_instances(@project)
        end

        it "the inference should fail" do
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/inference/models/#{@serving[:name]}:predict"
          expect_json(errorCode: 200003)
          expect_status(401)
        end
      end

      context 'with authentication, python enabled and with sklearn serving', vm: true do
        before :all do
          with_valid_project
          with_python_enabled(@project[:id], "3.6")
          with_sklearn_serving(@project[:id], @project[:projectname], @user[:username])
        end

        after :all do
          purge_all_sklearn_serving_instances()
          delete_all_sklearn_serving_instances(@project)
        end

        it "should fail to send a request to a non existing model" do
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/inference/models/nonexistingmodel:predict"
          expect_json(errorCode: 250000)
          expect_status(404)
        end

        it "should fail to send a request to a non running model" do
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/inference/models/#{@serving[:name]}:predict"
          expect_json(errorCode: 250001)
          expect_status(400)
        end

        context 'with running model do' do

          before :all do
            post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}?action=start"
            expect_status(200)

            # Sleep a bit to avoid race condition
            sleep(40)

            # Sleep some time while the SkLearn Flask server starts
            wait_for do
              system "pgrep -f sklearn_flask_server.py -a"
              $?.exitstatus == 0
            end
          end

          after :all do
            purge_all_sklearn_serving_instances()
          end

          it "should succeeds to infer from a serving with kafka logging" do
            post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/inference/models/#{@serving[:name]}:predict", {inputs: test_data}
            expect_status(200)
            parsed_json = JSON.parse(response.body)
            expect(parsed_json.key?("predictions")).to be true
            expect(parsed_json["predictions"].length == test_data.length).to be true
          end

          it "should succeed to infer from a serving with no kafka logging" do
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
                {id: @serving[:id],
                 name: @serving[:name],
                 artifactPath: @serving[:artifact_path],
                 modelVersion: @serving[:version],
                 kafkaTopicDTO: {
                     name: "NONE"
                 },
                 servingType: "SKLEARN"
                }
            expect_status(201)

            post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/inference/models/#{@serving[:name]}:predict", {
                inputs: test_data
            }
            expect_status(200)

            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
                {id: @serving[:id],
                 name: @serving[:name],
                 artifactPath: @serving[:artifact_path],
                 modelVersion: @serving[:model_version],
                 kafkaTopicDTO: {
                     name: @topic[:topic_name]
                 },
                 servingType: "SKLEARN"
                }
          end

          it "should receive an error if the input payload is malformed" do
            post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/inference/models/#{@serving[:name]}:predict", {
                somethingwrong: test_data
            }
            expect_json(errorCode: 250008)
            expect_status(400)
          end

          it "should receive an error if the input payload is empty" do
            post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/inference/models/#{@serving[:name]}:predict"
            expect_json(errorCode: 250008)
            expect_status(400)
          end
        end
      end
    end
  end
end