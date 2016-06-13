require 'fileutils'

module CI #:nodoc:
  module Reporter #:nodoc:
    class ReportManager
      def initialize(prefix)
        @basedir = ENV['CI_REPORTS'] || File.expand_path("#{Dir.getwd}/#{prefix.downcase}/reports")
        @basename = "#{@basedir}/#{prefix.upcase}"
        FileUtils.mkdir_p(@basedir)
      end

      def write_report(suite)
        File.open(filename_for(suite), "w") do |f|
          f << suite.to_xml
        end
      end

    private


      # creates a uniqe filename per suite
      # to prevent results from being overwritten
      # if a result file is already written, it appends an index
      # e.g.
      #   SPEC-MailsController.xml
      #   SPEC-MailsController.0.xml
      #   SPEC-MailsController.1.xml
      #   SPEC-MailsController...xml
      #   SPEC-MailsController.N.xml
      #
      # with N < 100000, to prevent endless sidestep loops
      MAX_SIDESTEPS     = 100000
      MAX_FILENAME_SIZE = 240
      #
      def filename_for(suite)
        basename = "#{@basename}-#{suite.name.gsub(/[^a-zA-Z0-9]+/, '-')}"
        suffix = "xml"

        # shorten basename if it exceeds 240 characters
        # most filesystems have a 255 character limit
        # so leave some room for the sidesteps
        max_filename_size = (ENV['MAX_FILENAME_SIZE'] || MAX_FILENAME_SIZE).to_i
        basename = basename[0..max_filename_size] if basename.length > max_filename_size

        # the initial filename, e.g. SPEC-MailsController.xml
        filename = [basename, suffix].join(".")

        # if the initial filename is already in use
        # do sidesteps, beginning with SPEC-MailsController.0.xml
        i = 0
        while File.exists?(filename) && i < MAX_SIDESTEPS
          filename = [basename, i, suffix].join(".")
          i += 1
        end

        filename
      end
    end
  end
end
