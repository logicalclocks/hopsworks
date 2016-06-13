module CI::Reporter
  module RSpec3
    class Failure
      def initialize(example)
        @example = example
      end

      def name
        exception.class.name
      end

      def message
        exception.message
      end

      def failure?
        exception.is_a?(::RSpec::Expectations::ExpectationNotMetError)
      end

      def error?
        !failure?
      end

      def location
        output = []
        output.push "#{name}:" unless name =~ /RSpec/
        output.push message
        output.push @example.formatted_backtrace
        output.join "\n"
      end

      def exception
        @example.exception
      end
    end
  end
end
