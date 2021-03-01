# This file is part of Hopsworks
# Copyright (C) 2020, Logical Clocks AB. All rights reserved
#
# Hopsworks is free software: you can redistribute it and/or modify it under the terms of
# the GNU Affero General Public License as published by the Free Software Foundation,
# either version 3 of the License, or (at your option) any later version.
#
# Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
# without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
# PURPOSE.  See the GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License along with this program.
# If not, see <https://www.gnu.org/licenses/>.
#

module FeatureStoreStatisticsHelper

  def create_statistics_commit(project_id, featurestore_id, entity_type, entity_id, commit_time: 1597903688000)
    post_statistics_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/#{entity_type}/#{entity_id}/statistics"
    json_data = {
        commitTime: commit_time,
        content: '{"columns": ["a", "b", "c"]}'
    }
    post post_statistics_endpoint, json_data.to_json
  end

  def get_statistics_commit(project_id, featurestore_id, entity_type, entity_id, commit_time: 1597903688000)
    get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/#{entity_type}/#{entity_id}/statistics?filter_by=commit_time_eq:#{commit_time}&fields=content"
  end

  def get_last_statistics_commit(project_id, featurestore_id, entity_type, entity_id)
    get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/#{entity_type}/#{entity_id}/statistics?sort_by=commit_time:desc&offset=0&limit=1&fields=content"
  end
end