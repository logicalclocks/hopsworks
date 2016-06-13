require 'spec_helper'

describe 'expect_json_keys' do
  it 'should fail when json keys are missing' do
    mock_get('simple_json')
    get '/simple_json', {}
    expect { expect_json_keys([:foo, :bar, :baz, :bax]) }.to raise_error(ExpectationNotMetError)
  end

  it 'should ensure correct json keys' do
    mock_get('simple_json')
    get '/simple_json', {}
    expect_json_keys([:foo, :bar, :baz])
  end

  it 'should ensure correct partial json keys' do
    mock_get('simple_json')
    get '/simple_json', {}
    expect_json_keys([:foo, :bar])
  end
end
