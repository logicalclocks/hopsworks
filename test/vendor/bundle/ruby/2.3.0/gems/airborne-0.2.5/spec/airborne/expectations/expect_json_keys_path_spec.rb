require 'spec_helper'

describe 'expect_json_keys with path' do
  it 'should ensure json keys with path' do
    mock_get('simple_nested_path')
    get '/simple_nested_path', {}
    expect_json_keys('address', [:street, :city])
  end

  it 'should fail when keys are missing with path' do
    mock_get('simple_nested_path')
    get '/simple_nested_path', {}
    expect { expect_json_keys('address', [:bad]) }.to raise_error(ExpectationNotMetError)
  end
end
