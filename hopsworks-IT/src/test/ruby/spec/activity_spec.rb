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
  after(:all) {clean_all_test_projects}
  describe "Activities" do
    describe "Activities sort, filter, offset and limit." do
      context 'with authentication' do
        before :all do
          with_valid_project
          @project = get_project
          create_sparktour_job(@project, "#{@project[:projectname]}_job", "jar", nil)
          member = create_user
          add_member(member[:email], "Data scientist")
          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/activities"
          @activities = json_body[:items]
          get "#{ENV['HOPSWORKS_API']}/users/activities"
          @user_activities = json_body[:items]
        end
        describe "Activities by project" do
          describe "Activities sort" do
            it 'should return activities sorted by flag.' do
              flags = @activities.map { |o| "#{o[:flag]}" }
              sorted = flags.sort_by(&:downcase)
              get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/activities?sort_by=flag"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]}" }
              expect(sortedRes).to eq(sorted)
            end
            it 'should return activities sorted by flag descending.' do
              flags = @activities.map { |o| "#{o[:flag]}" }
              sorted = flags.sort.reverse(&:downcase)
              get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/activities?sort_by=flag:desc"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]}" }
              expect(sortedRes).to eq(sorted)
            end
            it 'should return activities sorted by date created.' do
              dates = @activities.map { |o| "#{o[:timestamp]}" }
              sorted = dates.sort
              get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/activities?sort_by=date_created"
              sortedRes = json_body[:items].map { |o| "#{o[:timestamp]}" }
              expect(sortedRes).to eq(sorted)
            end
            it 'should return activities sorted by date created descending.' do
              flags = @activities.map { |o| "#{o[:timestamp]}" }
              sorted = flags.sort.reverse
              get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/activities?sort_by=date_created:desc"
              sortedRes = json_body[:items].map { |o| "#{o[:timestamp]}" }
              expect(sortedRes).to eq(sorted)
            end
            it 'should return activities sorted by flag descending and date created descending.' do
              s = @activities.sort do |a, b|
                res = -(a[:flag] <=> b[:flag])
                res = -(a[:timestamp] <=> b[:timestamp]) if res == 0
                res
              end
              sorted = s.map { |o| "#{o[:flag]} #{o[:timestamp]}" }
              get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/activities?sort_by=flag:desc,date_created:desc"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]} #{o[:timestamp]}" }
              expect(sortedRes).to eq(sorted)
            end
            it 'should return activities sorted by flag ascending and date created ascending.' do
              s = @activities.sort do |a, b|
                res = (a[:flag] <=> b[:flag])
                res = (a[:timestamp] <=> b[:timestamp]) if res == 0
                res
              end
              sorted = s.map { |o| "#{o[:flag]} #{o[:timestamp]}" }
              get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/activities?sort_by=flag:asc,date_created:asc"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]} #{o[:timestamp]}" }
              expect(sortedRes).to eq(sorted)
            end
            it 'should return activities sorted by flag descending and date created ascending.' do
              s = @activities.sort do |a, b|
                res = -(a[:flag] <=> b[:flag])
                res = (a[:timestamp] <=> b[:timestamp]) if res == 0
                res
              end
              sorted = s.map { |o| "#{o[:flag]} #{o[:timestamp]}" }
              get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/activities?sort_by=flag:desc,date_created:asc"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]} #{o[:timestamp]}" }
              expect(sortedRes).to eq(sorted)
            end
            it 'should return activities sorted by flag ascending and date created descending.' do
              s = @activities.sort do |a, b|
                res = (a[:flag] <=> b[:flag])
                res = -(a[:timestamp] <=> b[:timestamp]) if res == 0
                res
              end
              sorted = s.map { |o| "#{o[:flag]} #{o[:timestamp]}" }
              get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/activities?sort_by=flag:asc,date_created:desc"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]} #{o[:timestamp]}" }
              expect(sortedRes).to eq(sorted)
            end
            it 'should return activities sorted by date created descending and flag descending.' do
              s = @activities.sort do |a, b|
                res = -(a[:timestamp] <=> b[:timestamp])
                res = -(a[:flag] <=> b[:flag]) if res == 0
                res
              end
              sorted = s.map { |o| "#{o[:timestamp]} #{o[:flag]}" }
              get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/activities?sort_by=date_created:desc,flag:desc"
              sortedRes = json_body[:items].map { |o| "#{o[:timestamp]} #{o[:flag]}" }
              expect(sortedRes).to eq(sorted)
            end
            it 'should return activities sorted by date created ascending and flag ascending.' do
              s = @activities.sort do |a, b|
                res = (a[:timestamp] <=> b[:timestamp])
                res = (a[:flag] <=> b[:flag]) if res == 0
                res
              end
              sorted = s.map { |o| "#{o[:timestamp]} #{o[:flag]}" }
              get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/activities?sort_by=date_created:asc,flag:asc"
              sortedRes = json_body[:items].map { |o| "#{o[:timestamp]} #{o[:flag]}" }
              expect(sortedRes).to eq(sorted)
            end
            it 'should return activities sorted by date created descending and flag ascending.' do
              s = @activities.sort do |a, b|
                res = -(a[:timestamp] <=> b[:timestamp])
                res = (a[:flag] <=> b[:flag]) if res == 0
                res
              end
              sorted = s.map { |o| "#{o[:timestamp]} #{o[:flag]}" }
              get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/activities?sort_by=date_created:desc,flag:asc"
              sortedRes = json_body[:items].map { |o| "#{o[:timestamp]} #{o[:flag]}" }
              expect(sortedRes).to eq(sorted)
            end
            it 'should return activities sorted by date created ascending and flag descending.' do
              s = @activities.sort do |a, b|
                res = (a[:timestamp] <=> b[:timestamp])
                res = -(a[:flag] <=> b[:flag]) if res == 0
                res
              end
              sorted = s.map { |o| "#{o[:timestamp]} #{o[:flag]}" }
              get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/activities?sort_by=date_created:asc,flag:desc"
              sortedRes = json_body[:items].map { |o| "#{o[:timestamp]} #{o[:flag]}" }
              expect(sortedRes).to eq(sorted)
            end
          end
          describe "Activities offset and limit" do
            it 'should return limit=x activities.' do
              flags = @activities.map { |o| "#{o[:flag]}" }
              sorted = flags.sort_by(&:downcase)
              get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/activities?limit=10&sort_by=flag"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]}" }
              expect(sortedRes).to eq(sorted.take(10))
            end
            it 'should return limit=x activities with offset=y.' do
              flags = @activities.map { |o| "#{o[:flag]}" }
              sorted = flags.sort_by(&:downcase)
              get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/activities?offset=5&limit=6&sort_by=flag"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]}" }
              expect(sortedRes).to eq(sorted.drop(5).take(6))
            end
            it 'should ignore if limit < 0.' do
              flags = @activities.map { |o| "#{o[:flag]}" }
              sorted = flags.sort_by(&:downcase)
              get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/activities?limit=-6&sort_by=flag"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]}" }
              expect(sortedRes).to eq(sorted)
            end
            it 'should ignore if offset < 0.' do
              flags = @activities.map { |o| "#{o[:flag]}" }
              sorted = flags.sort_by(&:downcase)
              get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/activities?offset=-6&sort_by=flag"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]}" }
              expect(sortedRes).to eq(sorted)
            end
            it 'should ignore if limit = 0.' do
              flags = @activities.map { |o| "#{o[:flag]}" }
              sorted = flags.sort_by(&:downcase)
              get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/activities?limit=0&sort_by=flag"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]}" }
              expect(sortedRes).to eq(sorted)
            end
            it 'should ignore if offset = 0.' do
              flags = @activities.map { |o| "#{o[:flag]}" }
              sorted = flags.sort_by(&:downcase)
              get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/activities?offset=0&sort_by=flag"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]}" }
              expect(sortedRes).to eq(sorted)
            end
            it 'should ignore if offset = 0 and offset = 0.' do
              flags = @activities.map { |o| "#{o[:flag]}" }
              sorted = flags.sort_by(&:downcase)
              get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/activities?limit=0&offset=0&sort_by=flag"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]}" }
              expect(sortedRes).to eq(sorted)
            end
          end
          describe "Activities filter" do
            it 'should return activities with flag=project.' do
              get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/activities?filter_by=flag:project"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]}" }
              expect(sortedRes & ["MEMBER", "SERVICE", "DATASET", "JOB"]).to be_empty
            end
            it 'should return activities with flag=dataset.' do
              get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/activities?filter_by=flag:dataset"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]}" }
              expect(sortedRes & ["MEMBER", "PROJECT", "SERVICE", "JOB"]).to be_empty
            end
            it 'should return activities with flag=service.' do
              get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/activities?filter_by=flag:service"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]}" }
              expect(sortedRes & ["MEMBER", "PROJECT", "DATASET", "JOB"]).to be_empty
            end
            it 'should return activities with flag=member.' do
              get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/activities?filter_by=flag:member"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]}" }
              expect(sortedRes & ["PROJECT", "SERVICE", "DATASET", "JOB"]).to be_empty
            end
            it 'should return activities with flag=job.' do
              get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/activities?filter_by=flag:job"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]}" }
              expect(sortedRes & ["PROJECT", "SERVICE", "DATASET", "MEMBER"]).to be_empty
            end
            it 'should return activities with flag!=project.' do
              get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/activities?filter_by=flag_neq:project"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]}" }
              expect(sortedRes & ["PROJECT"]).to be_empty
            end
            it 'should return activities with flag!=dataset.' do
              get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/activities?filter_by=flag_neq:dataset"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]}" }
              expect(sortedRes & ["DATASET"]).to be_empty
            end
            it 'should return activities with flag!=service.' do
              get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/activities?filter_by=flag_neq:service"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]}" }
              expect(sortedRes & ["SERVICE"]).to be_empty
            end
            it 'should return activities with flag!=member.' do
              get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/activities?filter_by=flag_neq:member"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]}" }
              expect(sortedRes & ["MEMBER"]).to be_empty
            end
            it 'should return activities with flag!=job.' do
              get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/activities?filter_by=flag_neq:job"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]}" }
              expect(sortedRes & ["JOB"]).to be_empty
            end
            it 'should return activities with flag IN dataset,job.' do
              get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/activities?filter_by=flag:dataset,job"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]}" }
              expect(sortedRes & ["PROJECT", "SERVICE", "MEMBER"]).to be_empty
            end
            it 'should return activities with flag IN project,dataset,job.' do
              get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/activities?filter_by=flag:project,dataset,job"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]}" }
              expect(sortedRes & ["SERVICE", "MEMBER"]).to be_empty
            end
            it 'should return activities with flag NOT IN dataset,job.' do
              get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/activities?filter_by=flag_neq:dataset,job"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]}" }
              expect(sortedRes & ["DATASET", "JOB"]).to be_empty
            end
            it 'should return activities with flag NOT IN project,dataset,job.' do
              get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/activities?filter_by=flag_neq:project,dataset,job"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]}" }
              expect(sortedRes & ["PROJECT", "DATASET", "JOB"]).to be_empty
            end
          end
          describe "Activities expand" do
            it 'should expand user for all activities.' do
              get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/activities?expand=user"
              expandedRes = json_body[:items].map { |o| o[:user] }
              expandedRes.each do | u |
                expect(u[:email]).not_to be_nil
                expect(u[:firstname]).not_to be_nil
                expect(u[:lastname]).not_to be_nil
                expect(u[:username]).not_to be_nil
              end              
            end
            it 'should not expand user for all activities.' do
              get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/activities"
              expandedRes = json_body[:items].map { |o| o[:user] }
              expandedRes.each do | u |
                expect(u[:email]).to be_nil
                expect(u[:firstname]).to be_nil
                expect(u[:lastname]).to be_nil
                expect(u[:username]).to be_nil
              end              
            end
            it 'should not expand user.' do
              uri = URI(@activities.first[:href])
              get uri.path
              u = json_body[:user]
              expect(u[:email]).to be_nil
              expect(u[:firstname]).to be_nil
              expect(u[:lastname]).to be_nil
              expect(u[:username]).to be_nil
            end
            it 'should expand user.' do
              uri = URI(@activities.first[:href])
              get "#{uri.path}?expand=user"
              u = json_body[:user]
              expect(u[:email]).not_to be_nil
              expect(u[:firstname]).not_to be_nil
              expect(u[:lastname]).not_to be_nil
              expect(u[:username]).not_to be_nil
            end
          end
          describe "Activities invalid query" do
            it 'should return invalid query error code filter by param is invalid.' do
              get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/activities?filter_by=flag_neq:bla"
              expect(json_body[:errorCode]).to eq(310000)
            end
            it 'should return invalid query error code filter by key is invalid.' do
              get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/activities?filter_by=flag_nq:bla"
              expect(json_body[:errorCode]).to eq(120004)
            end
            it 'should return invalid query error code sort by param is invalid.' do
              get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/activities?sort_by=flag:bla"
              expect(json_body[:errorCode]).to eq(120004) 
            end
            it 'should return invalid query error code sort by key is invalid.' do
              get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/activities?sort_by=false:asc"
              expect(json_body[:errorCode]).to eq(120004)
            end
            it 'should return invalid query error code if expand by key is invalid.' do
              get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/activities?expand=invalid"
              expect(json_body[:errorCode]).to eq(120004)
            end
            it 'should return invalid query error code if expand by key is invalid and using uri.' do
              uri = URI(@activities.first[:href])
              get "#{uri.path}?expand=invalid"
              expect(json_body[:errorCode]).to eq(120004)
            end
          end
          describe "Activities count" do
            it 'should return the correct count when limit is set' do
              get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/activities?limit=5"
              expect(@activities.size).to eq(json_body[:count])
            end
            it 'should return the correct count when offset is set' do
              get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/activities?offset=5"
              expect(@activities.size).to eq(json_body[:count])
            end
          end
        end
        describe "Activities by user" do
          describe "Activities sort" do
            it 'should return activities sorted by flag.' do
              flags = @user_activities.map { |o| "#{o[:flag]}" }
              sorted = flags.sort_by(&:downcase)
              get "#{ENV['HOPSWORKS_API']}/users/activities?sort_by=flag"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]}" }
              expect(sortedRes).to eq(sorted)
            end
            it 'should return activities sorted by flag descending.' do
              flags = @user_activities.map { |o| "#{o[:flag]}" }
              sorted = flags.sort.reverse(&:downcase)
              get "#{ENV['HOPSWORKS_API']}/users/activities?sort_by=flag:desc"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]}" }
              expect(sortedRes).to eq(sorted)
            end
            it 'should return activities sorted by date created.' do
              dates = @user_activities.map { |o| "#{o[:timestamp]}" }
              sorted = dates.sort
              get "#{ENV['HOPSWORKS_API']}/users/activities?sort_by=date_created"
              sortedRes = json_body[:items].map { |o| "#{o[:timestamp]}" }
              expect(sortedRes).to eq(sorted)
            end
            it 'should return activities sorted by date created descending.' do
              flags = @user_activities.map { |o| "#{o[:timestamp]}" }
              sorted = flags.sort.reverse
              get "#{ENV['HOPSWORKS_API']}/users/activities?sort_by=date_created:desc"
              sortedRes = json_body[:items].map { |o| "#{o[:timestamp]}" }
              expect(sortedRes).to eq(sorted)
            end
            it 'should return activities sorted by flag descending and date created descending.' do
              s = @activities.sort do |a, b|
                res = -(a[:flag] <=> b[:flag])
                res = -(a[:timestamp] <=> b[:timestamp]) if res == 0
                res
              end
              sorted = s.map { |o| "#{o[:flag]} #{o[:timestamp]}" }
              get "#{ENV['HOPSWORKS_API']}/users/activities?sort_by=flag:desc,date_created:desc"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]} #{o[:timestamp]}" }
              expect(sortedRes).to eq(sorted)
            end
            it 'should return activities sorted by flag ascending and date created ascending.' do
              s = @activities.sort do |a, b|
                res = (a[:flag] <=> b[:flag])
                res = (a[:timestamp] <=> b[:timestamp]) if res == 0
                res
              end
              sorted = s.map { |o| "#{o[:flag]} #{o[:timestamp]}" }
              get "#{ENV['HOPSWORKS_API']}/users/activities?sort_by=flag:asc,date_created:asc"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]} #{o[:timestamp]}" }
              expect(sortedRes).to eq(sorted)
            end
            it 'should return activities sorted by flag descending and date created ascending.' do
              s = @activities.sort do |a, b|
                res = -(a[:flag] <=> b[:flag])
                res = (a[:timestamp] <=> b[:timestamp]) if res == 0
                res
              end
              sorted = s.map { |o| "#{o[:flag]} #{o[:timestamp]}" }
              get "#{ENV['HOPSWORKS_API']}/users/activities?sort_by=flag:desc,date_created:asc"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]} #{o[:timestamp]}" }
              expect(sortedRes).to eq(sorted)
            end
            it 'should return activities sorted by flag ascending and date created descending.' do
              s = @activities.sort do |a, b|
                res = (a[:flag] <=> b[:flag])
                res = -(a[:timestamp] <=> b[:timestamp]) if res == 0
                res
              end
              sorted = s.map { |o| "#{o[:flag]} #{o[:timestamp]}" }
              get "#{ENV['HOPSWORKS_API']}/users/activities?sort_by=flag:asc,date_created:desc"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]} #{o[:timestamp]}" }
              expect(sortedRes).to eq(sorted)
            end
            it 'should return activities sorted by date created descending and flag descending.' do
              s = @activities.sort do |a, b|
                res = -(a[:timestamp] <=> b[:timestamp])
                res = -(a[:flag] <=> b[:flag]) if res == 0
                res
              end
              sorted = s.map { |o| "#{o[:timestamp]} #{o[:flag]}" }
              get "#{ENV['HOPSWORKS_API']}/users/activities?sort_by=date_created:desc,flag:desc"
              sortedRes = json_body[:items].map { |o| "#{o[:timestamp]} #{o[:flag]}" }
              expect(sortedRes).to eq(sorted)
            end
            it 'should return activities sorted by date created ascending and flag ascending.' do
              s = @activities.sort do |a, b|
                res = (a[:timestamp] <=> b[:timestamp])
                res = (a[:flag] <=> b[:flag]) if res == 0
                res
              end
              sorted = s.map { |o| "#{o[:timestamp]} #{o[:flag]}" }
              get "#{ENV['HOPSWORKS_API']}/users/activities?sort_by=date_created:asc,flag:asc"
              sortedRes = json_body[:items].map { |o| "#{o[:timestamp]} #{o[:flag]}" }
              expect(sortedRes).to eq(sorted)
            end
            it 'should return activities sorted by date created descending and flag ascending.' do
              s = @activities.sort do |a, b|
                res = -(a[:timestamp] <=> b[:timestamp])
                res = (a[:flag] <=> b[:flag]) if res == 0
                res
              end
              sorted = s.map { |o| "#{o[:timestamp]} #{o[:flag]}" }
              get "#{ENV['HOPSWORKS_API']}/users/activities?sort_by=date_created:desc,flag:asc"
              sortedRes = json_body[:items].map { |o| "#{o[:timestamp]} #{o[:flag]}" }
              expect(sortedRes).to eq(sorted)
            end
            it 'should return activities sorted by date created ascending and flag descending.' do
              s = @activities.sort do |a, b|
                res = (a[:timestamp] <=> b[:timestamp])
                res = -(a[:flag] <=> b[:flag]) if res == 0
                res
              end
              sorted = s.map { |o| "#{o[:timestamp]} #{o[:flag]}" }
              get "#{ENV['HOPSWORKS_API']}/users/activities?sort_by=date_created:asc,flag:desc"
              sortedRes = json_body[:items].map { |o| "#{o[:timestamp]} #{o[:flag]}" }
              expect(sortedRes).to eq(sorted)
            end
          end
          describe "Activities offset and limit" do
            it 'should return limit=x activities.' do
              flags = @user_activities.map { |o| "#{o[:flag]}" }
              sorted = flags.sort_by(&:downcase)
              get "#{ENV['HOPSWORKS_API']}/users/activities?limit=10&sort_by=flag"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]}" }
              expect(sortedRes).to eq(sorted.take(10))
            end
            it 'should return limit=x activities with offset=y.' do
              flags = @user_activities.map { |o| "#{o[:flag]}" }
              sorted = flags.sort_by(&:downcase)
              get "#{ENV['HOPSWORKS_API']}/users/activities?offset=5&limit=6&sort_by=flag"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]}" }
              expect(sortedRes).to eq(sorted.drop(5).take(6))
            end
            it 'should ignore if limit < 0.' do
              flags = @user_activities.map { |o| "#{o[:flag]}" }
              sorted = flags.sort_by(&:downcase)
              get "#{ENV['HOPSWORKS_API']}/users/activities?limit=-6&sort_by=flag"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]}" }
              expect(sortedRes).to eq(sorted)
            end
            it 'should ignore if offset < 0.' do
              flags = @user_activities.map { |o| "#{o[:flag]}" }
              sorted = flags.sort_by(&:downcase)
              get "#{ENV['HOPSWORKS_API']}/users/activities?offset=-6&sort_by=flag"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]}" }
              expect(sortedRes).to eq(sorted)
            end
            it 'should ignore if limit = 0.' do
              flags = @user_activities.map { |o| "#{o[:flag]}" }
              sorted = flags.sort_by(&:downcase)
              get "#{ENV['HOPSWORKS_API']}/users/activities?limit=0&sort_by=flag"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]}" }
              expect(sortedRes).to eq(sorted)
            end
            it 'should ignore if offset = 0.' do
              flags = @user_activities.map { |o| "#{o[:flag]}" }
              sorted = flags.sort_by(&:downcase)
              get "#{ENV['HOPSWORKS_API']}/users/activities?offset=0&sort_by=flag"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]}" }
              expect(sortedRes).to eq(sorted)
            end
            it 'should ignore if offset = 0 and offset = 0.' do
              flags = @user_activities.map { |o| "#{o[:flag]}" }
              sorted = flags.sort_by(&:downcase)
              get "#{ENV['HOPSWORKS_API']}/users/activities?limit=0&offset=0&sort_by=flag"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]}" }
              expect(sortedRes).to eq(sorted)
            end
          end
          describe "Activities filter" do
            it 'should return activities with flag=project.' do
              get "#{ENV['HOPSWORKS_API']}/users/activities?filter_by=flag:project"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]}" }
              expect(sortedRes & ["MEMBER", "SERVICE", "DATASET", "JOB"]).to be_empty
            end
            it 'should return activities with flag=dataset.' do
              get "#{ENV['HOPSWORKS_API']}/users/activities?filter_by=flag:dataset"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]}" }
              expect(sortedRes & ["MEMBER", "PROJECT", "SERVICE", "JOB"]).to be_empty
            end
            it 'should return activities with flag=service.' do
              get "#{ENV['HOPSWORKS_API']}/users/activities?filter_by=flag:service"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]}" }
              expect(sortedRes & ["MEMBER", "PROJECT", "DATASET", "JOB"]).to be_empty
            end
            it 'should return activities with flag=member.' do
              get "#{ENV['HOPSWORKS_API']}/users/activities?filter_by=flag:member"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]}" }
              expect(sortedRes & ["PROJECT", "SERVICE", "DATASET", "JOB"]).to be_empty
            end
            it 'should return activities with flag=job.' do
              get "#{ENV['HOPSWORKS_API']}/users/activities?filter_by=flag:job"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]}" }
              expect(sortedRes & ["PROJECT", "SERVICE", "DATASET", "MEMBER"]).to be_empty
            end
            it 'should return activities with flag!=project.' do
              get "#{ENV['HOPSWORKS_API']}/users/activities?filter_by=flag_neq:project"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]}" }
              expect(sortedRes & ["PROJECT"]).to be_empty
            end
            it 'should return activities with flag!=dataset.' do
              get "#{ENV['HOPSWORKS_API']}/users/activities?filter_by=flag_neq:dataset"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]}" }
              expect(sortedRes & ["DATASET"]).to be_empty
            end
            it 'should return activities with flag!=service.' do
              get "#{ENV['HOPSWORKS_API']}/users/activities?filter_by=flag_neq:service"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]}" }
              expect(sortedRes & ["SERVICE"]).to be_empty
            end
            it 'should return activities with flag!=member.' do
              get "#{ENV['HOPSWORKS_API']}/users/activities?filter_by=flag_neq:member"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]}" }
              expect(sortedRes & ["MEMBER"]).to be_empty
            end
            it 'should return activities with flag!=job.' do
              get "#{ENV['HOPSWORKS_API']}/users/activities?filter_by=flag_neq:job"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]}" }
              expect(sortedRes & ["JOB"]).to be_empty
            end
            it 'should return activities with flag IN dataset,job.' do
              get "#{ENV['HOPSWORKS_API']}/users/activities?filter_by=flag:dataset,job"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]}" }
              expect(sortedRes & ["PROJECT", "SERVICE", "MEMBER"]).to be_empty
            end
            it 'should return activities with flag IN project,dataset,job.' do
              get "#{ENV['HOPSWORKS_API']}/users/activities?filter_by=flag:project,dataset,job"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]}" }
              expect(sortedRes & ["SERVICE", "MEMBER"]).to be_empty
            end
            it 'should return activities with flag NOT IN dataset,job.' do
              get "#{ENV['HOPSWORKS_API']}/users/activities?filter_by=flag_neq:dataset,job"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]}" }
              expect(sortedRes & ["DATASET", "JOB"]).to be_empty
            end
            it 'should return activities with flag NOT IN project,dataset,job.' do
              get "#{ENV['HOPSWORKS_API']}/users/activities?filter_by=flag_neq:project,dataset,job"
              sortedRes = json_body[:items].map { |o| "#{o[:flag]}" }
              expect(sortedRes & ["PROJECT", "DATASET", "JOB"]).to be_empty
            end
            describe "Activities expand" do
              it 'should expand user for all activities.' do
                get "#{ENV['HOPSWORKS_API']}/users/activities?expand=user"
                expandedRes = json_body[:items].map { |o| o[:user] }
                expandedRes.each do | u |
                  expect(u[:email]).not_to be_nil
                  expect(u[:firstname]).not_to be_nil
                  expect(u[:lastname]).not_to be_nil
                end              
              end
              it 'should not expand user for all activities.' do
                get "#{ENV['HOPSWORKS_API']}/users/activities"
                expandedRes = json_body[:items].map { |o| o[:user] }
                expandedRes.each do | u |
                  expect(u[:email]).to be_nil
                  expect(u[:firstname]).to be_nil
                  expect(u[:lastname]).to be_nil
                end              
              end
              it 'should not expand user.' do
                uri = URI(@user_activities.first[:href])
                get uri.path
                u = json_body[:user]
                expect(u[:email]).to be_nil
                expect(u[:firstname]).to be_nil
                expect(u[:lastname]).to be_nil             
              end
              it 'should expand user.' do
                uri = URI(@user_activities.first[:href])
                get "#{uri.path}?expand=user"
                u = json_body[:user]
                expect(u[:email]).not_to be_nil
                expect(u[:firstname]).not_to be_nil
                expect(u[:lastname]).not_to be_nil             
              end
            end
          end
          describe "Activities invalid query" do
            it 'should return invalid query error code if filter by parm is invalid.' do
              get "#{ENV['HOPSWORKS_API']}/users/activities?filter_by=flag_neq:bla"
              expect(json_body[:errorCode]).to eq(310000)
            end
            it 'should return invalid query error code if filter by key is invalid.' do
              get "#{ENV['HOPSWORKS_API']}/users/activities?filter_by=flag_nq:bla"
              expect(json_body[:errorCode]).to eq(120004)
            end
            it 'should return invalid query error code if sort by param is invalid.' do
              get "#{ENV['HOPSWORKS_API']}/users/activities?sort_by=flag:bla"
              expect(json_body[:errorCode]).to eq(120004) 
            end
            it 'should return invalid query error code if sort by key is invalid.' do
              get "#{ENV['HOPSWORKS_API']}/users/activities?sort_by=false:asc"
              expect(json_body[:errorCode]).to eq(120004)
            end
            it 'should return invalid query error code if expand by key is invalid.' do
              get "#{ENV['HOPSWORKS_API']}/users/activities?expand=invalid"
              expect(json_body[:errorCode]).to eq(120004)
            end
            it 'should return invalid query error code if expand by key is invalid and using uri.' do
              uri = URI(@user_activities.first[:href])
              get "#{uri.path}?expand=invalid"
              expect(json_body[:errorCode]).to eq(120004)
            end
          end
          describe "Activities count" do
            it 'should return the correct count when limit is set' do
              get "#{ENV['HOPSWORKS_API']}/users/activities?limit=5"
              expect(@user_activities.size).to eq(json_body[:count])
            end
            it 'should return the correct count when offset is set' do
              get "#{ENV['HOPSWORKS_API']}/users/activities?offset=5"
              expect(@user_activities.size).to eq(json_body[:count])
            end
          end
        end
      end
    end
  end
end
