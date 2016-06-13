require 'airborne/optional_hash_type_expectations'
require 'airborne/path_matcher'
require 'airborne/request_expectations'
require 'airborne/rest_client_requester'
require 'airborne/rack_test_requester'
require 'airborne/base'

RSpec.configure do |config|
  config.include Airborne
  config.add_setting :base_url
  config.add_setting :match_expected
  config.add_setting :match_actual
  config.add_setting :match_expected_default, default: true
  config.add_setting :match_actual_default, default: false
  config.add_setting :headers
  config.add_setting :rack_app
  config.add_setting :requester_type
  config.add_setting :requester_module
  config.before do |example|
    config.match_expected = example.metadata[:match_expected].nil? ?
      Airborne.configuration.match_expected_default? : example.metadata[:match_expected]
    config.match_actual = example.metadata[:match_actual].nil? ?
      Airborne.configuration.match_actual_default? : example.metadata[:match_actual]
  end
end
