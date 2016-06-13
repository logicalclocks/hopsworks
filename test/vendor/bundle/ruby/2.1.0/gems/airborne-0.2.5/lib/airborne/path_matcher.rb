module Airborne
  class PathError < StandardError; end

  module PathMatcher
    def get_by_path(path, json, &block)
      fail PathError, "Invalid Path, contains '..'" if /\.\./ =~ path
      type = false
      parts = path.split('.')
      parts.each_with_index do |part, index|
        if part == '*' || part == '?'
          ensure_array(path, json)
          type = part
          if index < parts.length.pred
            walk_with_path(type, index, path, parts, json, &block) && return
          end
          next
        end
        begin
          json = process_json(part, json)
        rescue
          raise PathError, "Expected #{json.class}\nto be an object with property #{part}"
        end
      end
      if type == '*'
        expect_all(json, &block)
      elsif type == '?'
        expect_one(path, json, &block)
      else
        yield json
      end
    end

    private

    def walk_with_path(type, index, path, parts, json, &block)
      last_error  = nil
      item_count = json.length
      error_count = 0
      json.each do |element|
        begin
          sub_path = parts[(index.next)...(parts.length)].join('.')
          get_by_path(sub_path, element, &block)
        rescue Exception => e
          last_error = e
          error_count += 1
        end
        ensure_match_all(last_error) if type == '*'
        ensure_match_one(path, item_count, error_count) if type == '?'
      end
    end

    def process_json(part, json)
      if index?(part) && json.is_a?(Array)
        part = part.to_i
        json = json[part]
      else
        json = json[part.to_sym]
      end
      json
    end

    def index?(part)
      part =~ /^\d+$/
    end

    def expect_one(path, json, &block)
      item_count = json.length
      error_count = 0
      json.each do |part|
        begin
          yield part
        rescue Exception
          error_count += 1
          ensure_match_one(path, item_count, error_count)
        end
      end
    end

    def expect_all(json, &block)
      last_error = nil
      begin
        json.each { |part| yield part }
      rescue Exception => e
        last_error = e
      end
      ensure_match_all(last_error)
    end

    def ensure_match_one(path, item_count, error_count)
      fail RSpec::Expectations::ExpectationNotMetError, "Expected one object in path #{path} to match provided JSON values" if item_count == error_count
    end

    def ensure_match_all(error)
      fail error unless error.nil?
    end

    def ensure_array(path, json)
      fail RSpec::Expectations::ExpectationNotMetError, "Expected #{path} to be array got #{json.class} from JSON response" unless json.class == Array
    end
  end
end
