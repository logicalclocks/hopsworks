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
