require 'spec_helper'

describe 'expect_json_types options' do
  describe 'match_expected', match_expected: true, match_actual: false do
    it 'should require all expected properties' do
      mock_get 'simple_get'
      get '/simple_get'
      expect{ expect_json_types(name: :string, other: :string) }.to raise_error(ExpectationNotMetError)
    end

    it 'should not require the actual properties' do
      mock_get 'simple_get'
      get '/simple_get'
      expect_json_types(name: :string)
    end
  end

  describe 'match_actual', match_expected: false, match_actual: true do
    it 'should require all actual properties' do
      mock_get 'simple_get'
      get '/simple_get'
      expect{ expect_json_types(name: :string) }.to raise_error(ExpectationError)
    end

    it 'should not require the expected properties' do
      mock_get 'simple_get'
      get '/simple_get'
      expect_json_types(name: :string, age: :int, address: :null, other: :string)
    end
  end

  describe 'match_both', match_expected: true, match_actual: true do
    it 'should require all actual properties' do
      mock_get 'simple_get'
      get '/simple_get'
      expect{ expect_json_types(name: :string) }.to raise_error(ExpectationError)
    end

    it 'should require all expected properties' do
      mock_get 'simple_get'
      get '/simple_get'
      expect{ expect_json_types(name: :string, other: :string) }.to raise_error(ExpectationNotMetError)
    end
  end

  describe 'match_none', match_expected: false, match_actual: false do
    it 'should not require the actual properties' do
      mock_get 'simple_get'
      get '/simple_get'
      expect_json_types(name: :string)
    end

    it 'should not require the expected properties' do
      mock_get 'simple_get'
      get '/simple_get'
      expect_json_types(name: :string, age: :int, address: :null, other: :string)
    end
  end
end
