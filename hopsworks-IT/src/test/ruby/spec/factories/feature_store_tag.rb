=begin
 Copyright (C) 2020, Logical Clocks AB. All rights reserved
=end

class FeatureStoreTag < ActiveRecord::Base
  def self.table_name
    "feature_store_tag"
  end
end