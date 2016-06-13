require 'spec_helper'
require 'date'

describe 'expect_json_types with date' do
  it 'should verify correct date types' do
    mock_get('date_response')
    get '/date_response'
    expect_json_types(createdAt: :date)
  end

  it 'should verify correct date types with path' do
    mock_get('date_response')
    get '/date_response'
    expect_json_types('createdAt', :date)
  end
end

describe 'expect_json with date' do
  it 'should verify correct date value' do
    mock_get('date_response')
    get '/date_response'
    prev_day = DateTime.new(2014, 10, 19)
    next_day = DateTime.new(2014, 10, 21)
    expect_json(createdAt: date { |value| expect(value).to be_between(prev_day, next_day) })
  end
end
