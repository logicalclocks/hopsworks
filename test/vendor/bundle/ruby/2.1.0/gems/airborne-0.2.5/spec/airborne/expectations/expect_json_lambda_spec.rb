require 'spec_helper'

describe 'expect_json lambda' do
  it 'should invoke proc passed in' do
    mock_get('simple_get')
    get '/simple_get'
    expect_json(name: ->(name) { expect(name.length).to eq(4) })
  end
end
