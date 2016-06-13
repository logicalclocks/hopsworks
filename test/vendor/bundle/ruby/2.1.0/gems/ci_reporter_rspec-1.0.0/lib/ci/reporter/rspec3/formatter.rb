require 'ci/reporter/core'
require 'ci/reporter/rspec3/failure'

module CI::Reporter
  module RSpec3
    class Formatter
      attr_accessor :report_manager

      def initialize(*args)
        @report_manager = ReportManager.new("spec")
      end

      def example_group_started(notification)
        new_suite(notification.group.metadata[:full_description])
      end

      def example_started(notification)
        spec = TestCase.new
        @suite.testcases << spec
        spec.start
      end

      def example_failed(notification)
        # In case we fail in before(:all)
        example_started(nil) if @suite.testcases.empty?

        failure = Failure.new(notification)

        current_spec.finish
        current_spec.name = notification.example.full_description
        current_spec.failures << failure
      end

      def example_passed(notification)
        current_spec.finish
        current_spec.name = notification.example.full_description
      end

      def example_pending(notification)
        current_spec.finish
        current_spec.name = "#{notification.example.full_description} (PENDING)"
        current_spec.skipped = true
      end

      def stop(notification)
        write_report
      end

      private

      def current_spec
        @suite.testcases.last
      end

      def write_report
        return unless @suite
        @suite.finish
        @report_manager.write_report(@suite)
      end

      def new_suite(name)
        write_report
        @suite = TestSuite.new(name)
        @suite.start
      end
    end

    ::RSpec::Core::Formatters.register Formatter,
                                       :example_group_started,
                                       :example_started,
                                       :example_passed,
                                       :example_failed,
                                       :example_pending,
                                       :stop
  end
end
