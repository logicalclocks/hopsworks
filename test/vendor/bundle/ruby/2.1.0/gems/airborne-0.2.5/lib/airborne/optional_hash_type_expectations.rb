module Airborne
  class OptionalHashTypeExpectations
    include Enumerable
    attr_accessor :hash
    def initialize(hash)
      @hash = hash
    end

    def each
      @hash.each do|k, v|
        yield(k, v)
      end
    end

    def [](val)
      @hash[val]
    end

    def keys
      @hash.keys
    end
  end
end
