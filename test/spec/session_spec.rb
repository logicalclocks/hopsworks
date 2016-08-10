describe "session" do
  before(:each) do
    reset_session
  end
  describe 'login' do
    it 'should work with valid params' do
      user = create_user
      post "/hopsworks/api/auth/login", URI.encode_www_form({ email: user.email, password: "Pass123"}), { content_type: 'application/x-www-form-urlencoded'}
      expect_json_types(sessionID: :string, status: :string)
      expect_status(200)
    end

    it 'should fail with invalid params' do
      user = create_user
      post "/hopsworks/api/auth/login", URI.encode_www_form({ email: user.email, password: "not_pass"}), { content_type: 'application/x-www-form-urlencoded'}
      expect_json_types(errorMsg: :string)
      expect_status(401)
    end
  end

  describe "register" do
    it "should create a new unvalidated user" do
      email = "#{random_id}@email.com"
      first_name = "name"
      last_name = "last"
      password = "Pass123"
      post "/hopsworks/api/auth/register", {email: email, chosenPassword: password, repeatedPassword: password, firstName: first_name, lastName: last_name, securityQuestion: "Name of your first pet?", securityAnswer: "example_answer", ToS: true, authType: "Mobile"}
      expect_json(errorMsg: ->(value){ expect(value).to be_empty})
      expect_json(successMessage: ->(value){ expect(value).to include("We registered your account request")})
      expect_status(200)
    end

    it "should fail if email exists" do
      email = "#{random_id}@email.com"
      first_name = "name"
      last_name = "last"
      password = "Pass123"
      register_user(email: email)
      post "/hopsworks/api/auth/register", {email: email, chosenPassword: password, repeatedPassword: password, firstName: first_name, lastName: last_name, securityQuestion: "Name of your first pet?", securityAnswer: "example_answer", ToS: true, authType: "Mobile"}
      expect_json(successMessage: ->(value){ expect(value).to be_nil})
      expect_json(errorMsg: ->(value){ expect(value).to include("There is an existing account")})
      expect_status(400)
    end

    it "should validate an exisiting unvalidated user" do
      email = "#{random_id}@email.com"
      register_user(email: email)
      user = User.find_by(email: email)
      key = user.username + user.validation_key
      get "/hopsworks/security/validate_account.xhtml", {params: {key: key}}
      expect_status(200)
    end

    it "should fail to signup if not confirmed and no role" do
      email = "#{random_id}@email.com"
      create_validated_user(email: email)
      create_session(email, "Pass123")
<<<<<<< HEAD
      expect_json(successMessage: ->(value){ expect(value).to be_nil})
      expect_json(errorMsg: ->(value){ expect(value).to include("Authentication failed")})
=======
      expect_json(successMessage: -> (value){ expect(value).to be_nil})
      expect_json(errorMsg: -> (value){ expect(value).to include("Authentication failed")})
>>>>>>> dfd0864b44525fc362551d64b23b23c81601167b
      expect_status(401)
    end

    it "should fail to signup with role and status 2" do
      email = "#{random_id}@email.com"
      register_user(email: email)
      create_role(User.find_by(email: email))
      create_session(email, "Pass123")
<<<<<<< HEAD
      expect_json(successMessage: ->(value){ expect(value).to be_nil})
      expect_json(errorMsg: ->(value){ expect(value).to include("Authentication failed")})
=======
      expect_json(successMessage: -> (value){ expect(value).to be_nil})
      expect_json(errorMsg: -> (value){ expect(value).to include("Authentication failed")})
>>>>>>> dfd0864b44525fc362551d64b23b23c81601167b
      expect_status(401)
    end

    it "should fail to signup with role and status 3" do
      email = "#{random_id}@email.com"
      create_validated_user(email: email)
      create_role(User.find_by(email: email))
      create_session(email, "Pass123")
<<<<<<< HEAD
      expect_json(successMessage: ->(value){ expect(value).to be_nil})
      expect_json(errorMsg: ->(value){ expect(value).to include("Authentication failed")})
=======
      expect_json(successMessage: -> (value){ expect(value).to be_nil})
      expect_json(errorMsg: -> (value){ expect(value).to include("Authentication failed")})
>>>>>>> dfd0864b44525fc362551d64b23b23c81601167b
      expect_status(401)
    end

    it "should fail with status 4 and no role" do
      email = "#{random_id}@email.com"
      create_validated_user(email: email)
      user = User.find_by(email: email)
      user.status = 4
      user.save
      create_session(email, "Pass123")
<<<<<<< HEAD
      expect_json(successMessage: ->(value){ expect(value).to be_nil})
      expect_json(errorMsg: ->(value){ expect(value).to include("No valid role found for this user")})
=======
      expect_json(successMessage: -> (value){ expect(value).to be_nil})
      expect_json(errorMsg: -> (value){ expect(value).to include("No valid role found for this user")})
>>>>>>> dfd0864b44525fc362551d64b23b23c81601167b
      expect_status(401)
    end
  end
end
