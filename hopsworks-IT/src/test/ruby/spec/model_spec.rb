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
  after(:all) {clean_all_test_projects}
  experiment_1 = "experiment_1"
  describe 'model' do
    after(:all) {clean_projects}
    context 'without authentication' do
      before :all do
        with_valid_project
        reset_session
      end
      it "should fail" do
        get_model(@project[:id], "mnist_1", nil)
        expect_json(errorCode: 200003)
        expect_status(401)
      end
    end
    context 'with authentication create, get' do
      before :all do
        with_valid_tour_project("deep_learning")
      end
      it "should not find any models" do
        get_models(@project[:id], nil)
        expect_status(200)
        expect(json_body[:items]).to be nil
        expect(json_body[:count]).to eq 0
        expect(URI(json_body[:href]).path).to eq "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/models"
      end
      it "should not find specific model" do
        get_model(@project[:id], "app_id_4254316623_1", nil)
        expect_status(404)
      end
      it "should produce 4 models and check items, count and href" do
        create_model_job(@project, experiment_1)
        run_experiment_blocking(@project, experiment_1)
        get_models(@project[:id], nil)
        expect_status(200)
        expect(json_body[:items].count).to eq 4
        expect(json_body[:count]).to eq 4
        json_body[:items].each {|model| expect(URI(model[:href]).path).to eq "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/models/#{model[:id]}"}
      end
      it "should get all existing models" do
        get_models(@project[:id], nil)
        json_body[:items].each {|model|
        get_model(@project[:id], model[:id], "")
        expect_status(200)}
      end
      it "should delete models by deleting Models dataset" do
        delete "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/Projects/#{@project[:projectname]}/Models"
        expect_status(204)
        wait_for_project_prov(@project)
        get_models(@project[:id], nil)
        expect(json_body[:count]).to eq(0)
      end
    end
  end
  describe 'models sort, filter, offset and limit' do
    context 'with authentication' do
      before :all do
        with_valid_tour_project("deep_learning")
        create_model_job(@project, experiment_1)
        run_experiment_blocking(@project, experiment_1)
      end
      describe "Models sort" do
        it "should get all models sorted by name ascending" do
          #sort in memory and compare with query
          get_models(@project[:id], nil)
          models = json_body[:items].map {|model| model[:name]}
          sorted = models.sort_by(&:downcase)
          get_models(@project[:id], "?sort_by=name:asc")
          sorted_res = json_body[:items].map {|model| model[:name]}
          expect(sorted_res).to eq(sorted)
        end
        it "should get all models sorted by name descending" do
          #sort in memory and compare with query
          get_models(@project[:id], nil)
          models = json_body[:items].map {|model| model[:name]}
          sorted = models.sort_by(&:downcase).reverse
          get_models(@project[:id], "?sort_by=name:desc")
          sorted_res = json_body[:items].map {|model| model[:name]}
          expect(sorted_res).to eq(sorted)
        end
      end
      describe "Models limit, offset, sort" do
        it "should return limit=x models" do
          #sort in memory and compare with query
          get_models(@project[:id], nil)
          models = json_body[:items].map {|model| model[:name]}
          sorted = models.sort_by(&:downcase)
          get_models(@project[:id], "?limit=2&sort_by=name:asc")
          sorted_res = json_body[:items].map {|model| model[:name]}
          expect(sorted_res).to eq(sorted.take(2))
        end
        it "should return models starting from offset=y" do
          #sort in memory and compare with query
          get_models(@project[:id], nil)
          models = json_body[:items].map {|model| model[:name]}
          sorted = models.sort_by(&:downcase)
          get_models(@project[:id], "?offset=1&sort_by=name:asc")
          sorted_res = json_body[:items].map {|model| model[:name]}
          expect(sorted_res).to eq(sorted.drop(1))
        end
        it "should return limit=x models with offset=y" do
          #sort in memory and compare with query
          get_models(@project[:id], nil)
          models = json_body[:items].map {|model| model[:name]}
          sorted = models.sort_by(&:downcase)
          get_models(@project[:id], "?offset=1&limit=2&sort_by=name:asc")
          sorted_res = json_body[:items].map {|model| model[:name]}
          expect(sorted_res).to eq(sorted.drop(1).take(2))
        end
        it "should ignore if limit < 0" do
          #sort in memory and compare with query
          get_models(@project[:id], nil)
          models = json_body[:items].map {|model| model[:name]}
          sorted = models.sort_by(&:downcase)
          get_models(@project[:id], "?limit<0&sort_by=name:asc")
          sorted_res = json_body[:items].map {|model| model[:name]}
          expect(sorted_res).to eq(sorted)
        end
        it "should ignore if offset < 0" do
          #sort in memory and compare with query
          get_models(@project[:id], nil)
          models = json_body[:items].map {|model| model[:name]}
          sorted = models.sort_by(&:downcase)
          get_models(@project[:id], "?offset<0&sort_by=name:asc")
          sorted_res = json_body[:items].map {|model| model[:name]}
          expect(sorted_res).to eq(sorted)
        end
        it "should ignore if limit = 0" do
          #sort in memory and compare with query
          get_models(@project[:id], nil)
          models = json_body[:items].map {|model| model[:name]}
          sorted = models.sort_by(&:downcase)
          get_models(@project[:id], "?limit=0&sort_by=name:asc")
          sorted_res = json_body[:items].map {|model| model[:name]}
          expect(sorted_res).to eq(sorted)
        end
        it "should ignore if offset = 0" do
          #sort in memory and compare with query
          get_models(@project[:id], nil)
          models = json_body[:items].map {|model| model[:name]}
          sorted = models.sort_by(&:downcase)
          get_models(@project[:id], "?offset=0&sort_by=name:asc")
          sorted_res = json_body[:items].map {|model| model[:name]}
          expect(sorted_res).to eq(sorted)
        end
        it "should ignore if limit = 0 and offset = 0" do
          #sort in memory and compare with query
          get_models(@project[:id], nil)
          models = json_body[:items].map {|model| model[:name]}
          sorted = models.sort_by(&:downcase)
          get_models(@project[:id], "?limit=0&offset=0&sort_by=name:asc")
          sorted_res = json_body[:items].map {|model| model[:name]}
          expect(sorted_res).to eq(sorted)
        end
      end
      describe "Models filter" do
        it "should get 0 model with name_eq resnet" do
          get_models(@project[:id], "?filter_by=name_eq:resnet")
          expect_status(200)
          expect(json_body[:items]).to be nil
        end
        it "should get 0 model with name_like resnet" do
          get_models(@project[:id], "?filter_by=name_like:resnet")
          expect_status(200)
          expect(json_body[:items]).to be nil
        end
        it "should get 3 models with name_eq mnist" do
          get_models(@project[:id], "?filter_by=name_eq:mnist")
          expect_status(200)
          expect(json_body[:items].count).to eq 3
        end
        it "should get 1 model with name_eq cif" do
          get_models(@project[:id], "?filter_by=name_eq:cifar")
          expect_status(200)
          expect(json_body[:items].count).to eq 1
        end
        it "should get 1 model with name_like cifar" do
          get_models(@project[:id], "?filter_by=name_like:cifar")
          expect_status(200)
          expect(json_body[:items].count).to eq 1
        end
        it "should get 3 model with name_like mni" do
          get_models(@project[:id], "?filter_by=name_like:mni")
          expect_status(200)
          expect(json_body[:items].count).to eq 3
        end
      end
    end
  end
end
