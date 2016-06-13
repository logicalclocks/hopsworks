require 'spec_helper'

describe 'expect_json_sizes' do
  it 'should detect sizes' do
    mock_get('array_of_values')
    get '/array_of_values'
    expect_json_sizes(grades: 4, bad: 3, emptyArray: 0)
  end

  it 'should allow full object graph' do
    mock_get('array_with_nested')
    get '/array_with_nested'
    expect_json_sizes(cars: { 0 => { owners: 1 }, 1 => { owners: 1 } })
  end

  it 'should allow properties to be tested against a path' do
    mock_get('array_with_nested')
    get '/array_with_nested'
    expect_json_sizes('cars.0.owners', 1)
  end

  it 'should test against all elements in the array when path contains * AND expectation is an Integer' do
    mock_get('array_with_nested')
    get '/array_with_nested'
    expect_json_sizes('cars.*.owners', 1)
  end

  it 'should test against all elements in the array when path contains * AND expectation is a Hash' do
    mock_get('array_with_nested')
    get '/array_with_nested'
    expect_json_sizes('cars.*', owners: 1)
  end
end
