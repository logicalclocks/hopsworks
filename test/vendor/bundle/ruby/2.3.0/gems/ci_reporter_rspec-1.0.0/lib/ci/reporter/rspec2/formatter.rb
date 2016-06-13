require 'rspec/core/formatters/base_formatter'
require 'ci/reporter/rspec2/failure'

module CI::Reporter
  module RSpec2
    class Formatter < ::RSpec::Core::Formatters::BaseFormatter
      attr_accessor :report_manager
      def initialize(*args)
        @report_manager = ReportManager.new("spec")
        @suite = nil
      end

      def example_group_started(example_group)
        new_suite(description_for(example_group))
      end

      def example_started(name_or_example)
        spec = TestCase.new
        @suite.testcases << spec
        spec.start
      end

      def example_failed(name_or_example, *rest)
        # In case we fail in before(:all)
        example_started(name_or_example) if @suite.testcases.empty?

        failure = Failure.new(name_or_example, self)

        spec = @suite.testcases.last
        spec.finish
        spec.name = description_for(name_or_example)
        spec.failures << failure
      end

      def example_passed(name_or_example)
        spec = @suite.testcases.last
        spec.finish
        spec.name = description_for(name_or_example)
      end

      def example_pending(*args)
        name = description_for(args[0])
        spec = @suite.testcases.last
        spec.finish
        spec.name = "#{name} (PENDING)"
        spec.skipped = true
      end

      def dump_summary(*args)
        write_report
      end

      private
      def description_for(name_or_example)
        if name_or_example.respond_to?(:full_description)
          name_or_example.full_description
        elsif name_or_example.respond_to?(:metadata)
          name_or_example.metadata[:example_group][:full_description]
        elsif name_or_example.respond_to?(:description)
          name_or_example.description
        else
          "UNKNOWN"
        end
      end

      def write_report
        if @suite
          @suite.finish
          @report_manager.write_report(@suite)
        end
      end

      def new_suite(name)
        write_report if @suite
        @suite = TestSuite.new name
        @suite.start
      end
    end
  end
end
