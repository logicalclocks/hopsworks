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
  describe 'featurestore integration tests' do
    after (:all) {clean_projects}

    describe "create feature store tour project and run example feature engineering job" do
      before :all do
        with_valid_tour_project("featurestore")
      end

      it "should have copied a sample .jar job, notebooks and data to the project's datasets" do
        project = get_project

        # Check TestJob dataset
        get_datasets_in_path(project, "TestJob", "&type=DATASET")
        inode_list = JSON.parse(response.body)
        expect_status(200)
        expect(find_inode_in_dataset(inode_list, "hops-examples-featurestore")).to be true
        expect(find_inode_in_dataset(inode_list, "data")).to be true

        # Check Jupyter dataset
        get_datasets_in_path(project, "Jupyter", "&type=DATASET")
        inode_list = JSON.parse(response.body)
        expect_status(200)
        expect(inode_list.empty?).to be false

      end

      it "should have created an example feature engineering job when the tour was started" do
        project = get_project
        get_job(project.id, get_featurestore_tour_job_name, nil)
        parsed_json = JSON.parse(response.body)
        expect_status(200)
        expect(parsed_json["name"] == get_featurestore_tour_job_name).to be true
      end

      it "should have started the example feature engineering job when the tour was created" do
        project = get_project
        get_executions(project.id, get_featurestore_tour_job_name, "")
        parsed_json = JSON.parse(response.body)
        expect_status(200)
        expect(parsed_json["items"].count == 1).to be true
      end

      it "the example feature engineering job should complete successfully, and after completion there should be 4 feature groups created in the project's feature store" do
        project = get_project

        # Check that the job was created and started and extracts its id
        get_executions(project.id, get_featurestore_tour_job_name, "")
        parsed_json = JSON.parse(response.body)
        expect_status(200)
        expect(parsed_json["items"].count == 1).to be true
        execution = parsed_json["items"][0]

        # Wait for execution to complete
        wait_for_execution(2000) do
          get_execution(project.id, get_featurestore_tour_job_name, execution["id"])
          execution_dto = JSON.parse(response.body)
          not is_execution_active(execution_dto)
        end

        # Check that the execution completed successfully
        get_execution(project.id, get_featurestore_tour_job_name, execution["id"])
        execution_dto = JSON.parse(response.body)
        expect(execution_dto["state"] == "FINISHED").to be true
        expect(execution_dto["finalStatus"] == "SUCCEEDED").to be true

        # Check that the job created 4 feature groups
        featurestore_id = get_featurestore_id(project.id)
        get_featuregroups_endpoint= "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups"
        get get_featuregroups_endpoint
        parsed_json = JSON.parse(response.body)
        expect_status(200)
        expect(parsed_json.length >= 4).to be true
      end
    end
  end
end
