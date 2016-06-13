module CI::Reporter
  module RSpec2
    class Failure
      attr_reader :exception

      def initialize(example, formatter)
        @formatter = formatter
        @example = example
        if @example.respond_to?(:execution_result)
          @exception = @example.execution_result[:exception] || @example.execution_result[:exception_encountered]
        else
          @exception = @example.metadata[:execution_result][:exception]
        end
      end

      def name
        @exception.class.name
      end

      def message
        @exception.message
      end

      def failure?
        exception.is_a?(::RSpec::Expectations::ExpectationNotMetError)
      end

      def error?
        !failure?
      end

      def location
        output = []
        output.push "#{exception.class.name << ":"}" unless exception.class.name =~ /RSpec/
        output.push @exception.message

        [@formatter.format_backtrace(@exception.backtrace, @example)].flatten.each do |backtrace_info|
          backtrace_info.lines.each do |line|
            output.push "     #{line}"
          end
        end
        output.join "\n"
      end
    end
  end
end
