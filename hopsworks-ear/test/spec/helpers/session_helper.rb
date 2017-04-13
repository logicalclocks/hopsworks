module SessionHelper
  def with_valid_session
    unless @cookies
      user = create_user
      post "#{ENV['HOPSWORKS_API']}/auth/login", URI.encode_www_form({ email: user.email, password: "Pass123"}), { content_type: 'application/x-www-form-urlencoded'}
      @cookies = {"SESSIONID"=> json_body[:sessionID]}
      @user = user
    end
    Airborne.configure do |config|
      config.headers = {:cookies => @cookies, content_type: 'application/json' }
    end
  end
  
  def register_user(params={})
    user = {}
    user[:email]            = params[:email] ? params[:email] : "#{random_id}@email.com"
    user[:firstName]        = params[:first_name] ? params[:first_name] : "name"
    user[:lastName]         = params[:last_name] ? params[:last_name] : "last"
    user[:chosenPassword]   = params[:password] ? params[:password] : "Pass123"
    user[:repeatedPassword] = params[:password] ? params[:password] : "Pass123"
    user[:securityQuestion] = params[:security_question] ? params[:security_question] : "Name of your first pet?"
    user[:securityAnswer]   = params[:security_answer] ? params[:security_answer] : "example_answer"
    user[:ToS]              = params[:tos] ? params[:tos] :  true
    user[:authType]         = params[:auth_type] ? params[:auth_type] : "Mobile"
    user[:twoFactor]        = params[:twoFactor] ? params[:twoFactor] : false
    
    post "#{ENV['HOPSWORKS_API']}/auth/register", user
  end

  def create_validated_user(params={})
    params[:email] = "#{random_id}@email.com" unless params[:email]
    register_user(params)
    user = User.find_by(email: params[:email])
    key = user.username + user.validation_key
    get "/hopsworks/security/validate_account.xhtml", {params: {key: key}}
  end
  
  def set_two_factor(value)
    variables = Variables.find_by(id: "twofactor_auth")
    variables.value = value
    variables.save
    variables
  end
  
  def set_two_factor_exclud(value)
    variables = Variables.find_by(id: "twofactor-excluded-groups")
    variables.value = value
    variables.save
    variables
  end

  def reset_session
    Airborne.configure do |config|
      config.headers = {:cookies => {}, content_type: 'application/json' }
    end
  end

  def create_session(email, password)
    reset_session
    post "#{ENV['HOPSWORKS_API']}/auth/login", URI.encode_www_form({ email: email, password: password}), { content_type: 'application/x-www-form-urlencoded'}
    cookies = {"SESSIONID"=> json_body[:sessionID]}
    Airborne.configure do |config|
      config.headers = {:cookies => cookies, content_type: 'application/json' }
    end
    cookies
  end

  def create_role(user)
    group = BbcGroup.find_by(group_name: "HOPS_USER")
    PeopleGroup.create(uid: user.uid, gid: group.gid)
  end

  def create_agent_role(user)
    group = BbcGroup.find_by(group_name: "AGENT")
    PeopleGroup.create(uid: user.uid, gid: group.gid)
  end
  
  def create_user(params={})
    params[:email] = "#{random_id}@email.com" unless params[:email]
    create_validated_user(params)
    user = User.find_by(email: params[:email])
    create_role(user)
    user.status = 4
    user.save
    user
  end
  
  def create_2factor_user(params={})
    params[:email] = "#{random_id}@email.com" unless params[:email]
    params[:twoFactor] = 1
    create_validated_user(params)
    user = User.find_by(email: params[:email])
    create_role(user)
    user
  end
  
  def create_2factor_user_agent(params={})
    params[:email] = "#{random_id}@email.com" unless params[:email]
    params[:twoFactor] = 1
    create_validated_user(params)
    user = User.find_by(email: params[:email])
    create_agent_role(user)
    user
  end
  
  def create_blocked_user(params={})
    params[:email] = "#{random_id}@email.com" unless params[:email]
    create_validated_user(params)
    user = User.find_by(email: params[:email])
    create_role(user)
    user.status = 6
    user.save
    user
  end
  
  def create_deactivated_user(params={})
    params[:email] = "#{random_id}@email.com" unless params[:email]
     create_validated_user(params)
    user = User.find_by(email: params[:email])
    create_role(user)
    user.status = 5
    user.save
    user
  end
  
  def create_lostdevice_user(params={})
    params[:email] = "#{random_id}@email.com" unless params[:email]
     create_validated_user(params)
    user = User.find_by(email: params[:email])
    create_role(user)
    user.status = 7
    user.save
    user
  end
end
