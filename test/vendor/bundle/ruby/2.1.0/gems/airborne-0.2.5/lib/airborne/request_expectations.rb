require 'rspec'
require 'date'
require 'rack/utils'

module Airborne
  class ExpectationError < StandardError; end
  module RequestExpectations
    include RSpec
    include PathMatcher

    def expect_json_types(*args)
      call_with_path(args) do |param, body|
        expect_json_types_impl(param, body)
      end
    end

    def expect_json(*args)
      call_with_path(args) do |param, body|
        expect_json_impl(param, body)
      end
    end

    def expect_json_keys(*args)
      call_with_path(args) do |param, body|
        expect(body.keys).to include(*param)
      end
    end

    def expect_json_sizes(*args)
      args.push(convert_expectations_for_json_sizes(args.pop))

      expect_json_types(*args)
    end

    def expect_status(code)
      expect(response.code).to eq(resolve_status(code, response.code))
    end

    def expect_header(key, content)
      expect_header_impl(key, content)
    end

    def expect_header_contains(key, content)
      expect_header_impl(key, content, true)
    end

    def optional(hash)
      OptionalHashTypeExpectations.new(hash)
    end

    def regex(reg)
      Regexp.new(reg)
    end

    def date
      ->(value) { yield DateTime.parse(value) }
    end

    private

    def expect_header_impl(key, content, contains = nil)
      header = headers[key]
      if header
        if contains
          expect(header.downcase).to include(content.downcase)
        else
          expect(header.downcase).to eq(content.downcase)
        end
      else
        fail RSpec::Expectations::ExpectationNotMetError, "Header #{key} not present in the HTTP response"
      end
    end

    def expect_json_impl(expected, actual)
      return if nil_optional_hash?(expected, actual)

      actual = actual.to_s if expected.is_a?(Regexp)

      return expect(actual).to match(expected) if property?(expected)

      keys = []

      keys << expected.keys if match_expected?
      keys << actual.keys if match_actual?
      keys = expected.keys & actual.keys if match_none?

      keys.flatten.uniq.each do |prop|
        expected_value  = extract_expected_value(expected, prop)
        actual_value    = extract_actual(actual, prop)

        next expect_json_impl(expected_value, actual_value) if hash?(expected_value)
        next expected_value.call(actual_value) if expected_value.is_a?(Proc)
        next expect(actual_value.to_s).to match(expected_value) if expected_value.is_a?(Regexp)

        expect(actual_value).to eq(expected_value)
      end
    end

    def expect_json_types_impl(expected, actual)
      return if nil_optional_hash?(expected, actual)

      @mapper ||= get_mapper

      actual = convert_to_date(actual) if expected == :date

      return expect_type(expected, actual.class) if expected.is_a?(Symbol)
      return expected.call(actual) if expected.is_a?(Proc)

      keys = []

      keys << expected.keys if match_expected?
      keys << actual.keys if match_actual?
      keys = expected.keys & actual.keys if match_none?

      keys.flatten.uniq.each do |prop|
        type  = extract_expected_type(expected, prop)
        value = extract_actual(actual, prop)
        value = convert_to_date(value) if type == :date

        next expect_json_types_impl(type, value) if hash?(type)
        next type.call(value) if type.is_a?(Proc)

        val_class = value.class

        type_string = type.to_s

        if type_string.include?('array_of') && !(type_string.include?('or_null') && value.nil?)
          check_array_types(value, val_class, prop, type)
        else
          expect_type(type, val_class, prop)
        end
      end
    end

    def call_with_path(args)
      if args.length == 2
        get_by_path(args[0], json_body) do |json_chunk|
          yield(args[1], json_chunk)
        end
      else
        yield(args[0], json_body)
      end
    end

    def extract_expected_value(expected, prop)
      begin
        raise unless expected.keys.include?(prop)
        expected[prop]
      rescue
        raise ExpectationError, "Expectation is expected to contain property: #{prop}"
      end
    end

    def extract_expected_type(expected, prop)
      begin
        type = expected[prop]
        type.nil? ? raise : type
      rescue
        raise ExpectationError, "Expectation is expected to contain property: #{prop}"
      end
    end

    def extract_actual(actual, prop)
      begin
        value = actual[prop]
      rescue
        raise ExpectationError, "Expected #{actual.class} #{actual}\nto be an object with property #{prop}"
      end
    end

    def expect_type(expected_type, value_class, prop_name = nil)
      fail ExpectationError, "Expected type #{expected_type}\nis an invalid type" if @mapper[expected_type].nil?

      insert = prop_name.nil? ? '' : "#{prop_name} to be of type"
      message = "Expected #{insert} #{expected_type}\n got #{value_class} instead"

      expect(@mapper[expected_type].include?(value_class)).to eq(true), message
    end

    def convert_to_date(value)
      begin
        DateTime.parse(value)
      rescue
      end
    end

    def check_array_types(value, value_class, prop_name, expected_type)
      expect_array(value_class, prop_name, expected_type)
      value.each do |val|
        expect_type(expected_type, val.class, prop_name)
      end
    end

    def nil_optional_hash?(expected, hash)
      expected.is_a?(Airborne::OptionalHashTypeExpectations) && hash.nil?
    end

    def hash?(hash)
      hash.is_a?(Hash) || hash.is_a?(Airborne::OptionalHashTypeExpectations)
    end

    def expect_array(value_class, prop_name, expected_type)
      expect(value_class).to eq(Array), "Expected #{prop_name}\n to be of type #{expected_type}\n got #{value_class} instead"
    end

    def convert_expectations_for_json_sizes(old_expectations)
      unless old_expectations.is_a?(Hash)
        return convert_expectation_for_json_sizes(old_expectations)
      end

      old_expectations.each_with_object({}) do |(prop_name, expected_size), memo|
        new_value = if expected_size.is_a?(Hash)
                      convert_expectations_for_json_sizes(expected_size)
                    else
                      convert_expectation_for_json_sizes(expected_size)
                    end
        memo[prop_name] = new_value
      end
    end

    def convert_expectation_for_json_sizes(expected_size)
      ->(data) { expect(data.size).to eq(expected_size) }
    end

    def ensure_hash_contains_prop(prop_name, hash)
      begin
        yield
      rescue
        raise ExpectationError, "Expected #{hash.class} #{hash}\nto be an object with property #{prop_name}"
      end
    end

    def property?(expectations)
      [String, Regexp, Float, Fixnum, Bignum, TrueClass, FalseClass, NilClass, Array].include?(expectations.class)
    end

    def get_mapper
      base_mapper = {
        integer: [Fixnum, Bignum],
        array_of_integers: [Fixnum, Bignum],
        int: [Fixnum, Bignum],
        array_of_ints: [Fixnum, Bignum],
        float: [Float, Fixnum, Bignum],
        array_of_floats: [Float, Fixnum, Bignum],
        string: [String],
        array_of_strings: [String],
        boolean: [TrueClass, FalseClass],
        array_of_booleans: [TrueClass, FalseClass],
        bool: [TrueClass, FalseClass],
        array_of_bools: [TrueClass, FalseClass],
        object: [Hash],
        array_of_objects: [Hash],
        array: [Array],
        array_of_arrays: [Array],
        date: [DateTime],
        null: [NilClass]
      }

      mapper = base_mapper.clone
      base_mapper.each do |key, value|
        mapper[(key.to_s + '_or_null').to_sym] = value + [NilClass]
      end
      mapper
    end

    def resolve_status(candidate, authority)
      candidate = Rack::Utils::SYMBOL_TO_STATUS_CODE[candidate] if candidate.is_a?(Symbol)
      case authority
      when String then candidate.to_s
      when Fixnum then candidate.to_i
      else candidate
      end
    end

    def match_none?
      !match_actual? && !match_expected?
    end

    def match_actual?
      Airborne.configuration.match_actual?
    end

    def match_expected?
      Airborne.configuration.match_expected?
    end
  end
end
