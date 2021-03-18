=begin
 Copyright (C) 2021, Logical Clocks AB. All rights reserved
=end

class ValidationRule < ActiveRecord::Base
  def self.table_name
    "validation_rule"
  end
end