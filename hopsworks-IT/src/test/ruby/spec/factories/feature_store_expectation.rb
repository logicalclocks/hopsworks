=begin
 Copyright (C) 2021, Logical Clocks AB. All rights reserved
=end

class FeatureStoreExpectation < ActiveRecord::Base
  def self.table_name
    "feature_store_expectation"
  end
end