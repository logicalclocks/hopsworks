require 'spec_helper'

describe 'expect_json options' do
  describe 'match_expected', match_expected: true, match_actual: false do
    it 'should require all expected properties' do
      mock_get 'simple_get'
      get '/simple_get'
      expect{ expect_json(name: 'Alex', other: 'other') }.to raise_error(ExpectationNotMetError)
    end

    it 'should not require the actual properties' do
      mock_get 'simple_get'
      get '/simple_get'
      expect_json(name: 'Alex')
    end
  end

  describe 'match_actual', match_expected: false, match_actual: true do
    it 'should require all actual properties' do
      mock_get 'simple_get'
      get '/simple_get'
      expect{ expect_json(name: 'Alex') }.to raise_error(ExpectationError)
    end

    it 'should not require the expected properties' do
      mock_get 'simple_get'
      get '/simple_get'
      expect_json(name: 'Alex', age: 32, address: nil, other: 'other')
    end
  end

  describe 'match_both', match_expected: true, match_actual: true do
    it 'should require all actual properties' do
      mock_get 'simple_get'
      get '/simple_get'
      expect{ expect_json(name: 'Alex') }.to raise_error(ExpectationError)
    end

    it 'should require all expected properties' do
      mock_get 'simple_get'
      get '/simple_get'
      expect{ expect_json(name: 'Alex', other: 'other') }.to raise_error(ExpectationNotMetError)
    end
  end

  describe 'match_none', match_expected: false, match_actual: false do
    it 'should not require the actual properties' do
      mock_get 'simple_get'
      get '/simple_get'
      expect_json(name: 'Alex')
    end

    it 'should not require the expected properties' do
      mock_get 'simple_get'
      get '/simple_get'
      expect_json(name: 'Alex', age: 32, address: nil, other: 'other')
    end
  end
end
