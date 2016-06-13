require 'spec_helper'

describe 'expect path' do
  describe 'errors' do
    before :each do
      mock_get('array_with_index')
      get '/array_with_index'
    end

    it 'should raise PathError when incorrect path containing .. is used' do
      expect do
        expect_json('cars..make', 'Tesla')
      end.to raise_error(PathError, "Invalid Path, contains '..'")
    end

    it 'should raise PathError when trying to call property on an array' do
      expect do
        expect_json('cars.make', 'Tesla')
      end.to raise_error(PathError, "Expected Array\nto be an object with property make")
    end
  end

  it 'should work with numberic properties' do
    mock_get('numeric_property')
    get '/numeric_property'
    expect_json('cars.0.make', 'Tesla')
  end

  it 'should work with numberic properties' do
    mock_get('numeric_property')
    get '/numeric_property'
    expect_json_keys('cars.0', [:make, :model])
  end
end
