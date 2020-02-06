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
  describe "xattrs" do
    before(:all) do
      with_valid_session
    end

    context "with authentication" do
      before :all do
        with_valid_project
        @project = get_project
        @attr1 = "attr1"
        @attr1v = "this is my first attribute"
        @attr1v2 = "this is my updated first attribute"
        @attr2 = "attr2"
        @attr2v = "this is my second attribute"
        @dsname = "dataset_#{short_random_id}"
        ds = create_dataset_by_name(@project, @dsname)
      end

      it "should attach an extended attribute to a dataset" do
        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/xattrs//Projects/#{@project[:projectname]}/#{@dsname}?name=#{@attr1}",
        {@attr1 => @attr1v}.to_json
        expect_status(201)
      end

      it "should fail to attach an extended attribute if no name is supplied" do
        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/xattrs//Projects/#{@project[:projectname]}/#{@dsname}",
        {@attr1 => @attr1v}.to_json
        expect_json(errorCode: 180006)
        expect_status(400)
      end

      it "should fail to attach an extended attribute to a relative path" do
        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/xattrs/Projects/#{@project[:projectname]}/#{@dsname}?name=#{@attr1}",
        {@attr1 => @attr1v}.to_json
        expect_json(errorCode: 180007)
        expect_status(500)
      end

      it "should fail to attach an extended attribute with long name to a dataset" do
        longattr="A"*256
        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/xattrs//Projects/#{@project[:projectname]}/#{@dsname}?name=#{longattr}",
        {longattr => @attr1v}.to_json
        expect_json(errorCode: 180005)
        expect_status(400)
      end

      it "should fail to attach an extended attribute with long value to a dataset" do
        bigvalue="A"*13501
        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/xattrs//Projects/#{@project[:projectname]}/#{@dsname}?name=#{@attr1}",
        {@attr1 => bigvalue}.to_json
        expect_json(errorCode: 180005)
        expect_status(400)
      end

      it "should get the extended attribute attached to a dataset" do
        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/xattrs//Projects/#{@project[:projectname]}/#{@dsname}?name=#{@attr1}"
        parsed_body = JSON.parse(response.body)
        expect(parsed_body["items"].count).to eq(1)
        expect(parsed_body["items"][0]["name"]).to eq(@attr1)
        expect(parsed_body["items"][0]["value"]).to eq(@attr1v)
        expect_status(202)
      end

      it "should fail to get a non existent extended attribute" do
        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/xattrs//Projects/#{@project[:projectname]}/#{@dsname}?name=attr3"
        expect_status(404)
      end

      it "should replace an extended attribute to a dataset" do
        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/xattrs//Projects/#{@project[:projectname]}/#{@dsname}?name=#{@attr1}",
        {@attr1 => @attr1v2}.to_json
        expect_status(200)
      end

      it "should get the newly replaced extended attribute attached to a dataset" do
        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/xattrs//Projects/#{@project[:projectname]}/#{@dsname}?name=#{@attr1}"
        parsed_body = JSON.parse(response.body)
        expect(parsed_body["items"].count).to eq(1)
        expect(parsed_body["items"][0]["name"]).to eq(@attr1)
        expect(parsed_body["items"][0]["value"]).to eq(@attr1v2)
        expect_status(202)
      end

      it "should attach another extended attribute to a dataset" do
        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/xattrs//Projects/#{@project[:projectname]}/#{@dsname}?name=#{@attr2}",
        {@attr2 => @attr2v}.to_json
        expect_status(201)
      end

      it "should get all extended attributes attached to a dataset" do
        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/xattrs//Projects/#{@project[:projectname]}/#{@dsname}"
        parsed_body = JSON.parse(response.body)
        expect(parsed_body["items"].count).to eq(2)

        for i in 1..2 do
          if parsed_body["items"][0]["name"] == @attr1
            expect(parsed_body["items"][0]["value"]).to eq(@attr1v2)
          else
            expect(parsed_body["items"][0]["name"]).to eq(@attr2)
            expect(parsed_body["items"][0]["value"]).to eq(@attr2v)
          end
        end

        expect_status(202)
      end

      it "should delete an extended attributes attached to a dataset" do
        delete "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/xattrs//Projects/#{@project[:projectname]}/#{@dsname}?name=#{@attr1}"
        expect_status(204)
      end

      it "should get the remaining extended attributes attached to a dataset" do
        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/xattrs//Projects/#{@project[:projectname]}/#{@dsname}"
        parsed_body = JSON.parse(response.body)
        expect(parsed_body["items"].count).to eq(1)
        expect(parsed_body["items"][0]["name"]).to eq(@attr2)
        expect(parsed_body["items"][0]["value"]).to eq(@attr2v)
        expect_status(202)
      end

      it "should fail to delete a non existent extended attribute" do
        delete "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/xattrs//Projects/#{@project[:projectname]}/#{@dsname}?name=attr3"
        expect_status(404)
      end

      it "should fail to delete an extended attribute if no name is supplied" do
        delete "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/xattrs//Projects/#{@project[:projectname]}/#{@dsname}"
        expect_json(errorCode: 180006)
        expect_status(400)
      end

    end

  end
end
