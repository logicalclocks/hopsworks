=begin
This file is part of HopsWorks

Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.

HopsWorks is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

HopsWorks is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
=end
module VariablesHelper
  
  def refresh_variables
    with_admin_session
    post "#{ENV['HOPSWORKS_API']}/admin/variables/refresh"
  end
  
  def set_two_factor(value)
    variables = Variables.find_by(id: "twofactor_auth")
    variables.value = value
    variables.save
    refresh_variables
    variables
  end
  
  def set_two_factor_exclud(value)
    variables = Variables.find_by(id: "twofactor-excluded-groups")
    variables.value = value
    variables.save
    refresh_variables
    variables
  end
  
  def set_ldap_enabled (value)
    variables = Variables.find_by(id: "ldap_auth")
    if variables.nil?
      variables.id = "ldap_auth"
    end
    variables.value = value
    variables.save
    refresh_variables
    variables
  end

end
