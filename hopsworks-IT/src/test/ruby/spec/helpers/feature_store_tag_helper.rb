=begin
 This file is part of Hopsworks
 Copyright (C) 2020, Logical Clocks AB. All rights reserved

 Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 the GNU Affero General Public License as published by the Free Software Foundation,
 either version 3 of the License, or (at your option) any later version.

 Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 PURPOSE.  See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along with this program.
 If not, see <https://www.gnu.org/licenses/>.
=end

module FeatureStoreTagHelper
  def getAllFeatureStoreTags
    get "#{ENV['HOPSWORKS_API']}/tags"
  end

  def getFeatureStoreTagByName(name)
    get "#{ENV['HOPSWORKS_API']}/tags/#{name}"
  end

  def createFeatureStoreTag(name, type)
    post "#{ENV['HOPSWORKS_API']}/tags?name=#{name}&type=#{type}"
  end

  def updateFeatureStoreTag(name, newName, type)
    put "#{ENV['HOPSWORKS_API']}/tags/#{name}?name=#{newName}&type=#{type}"
  end

  def deleteFeatureStoreTag(name)
    delete "#{ENV['HOPSWORKS_API']}/tags/#{name}"
  end

end
