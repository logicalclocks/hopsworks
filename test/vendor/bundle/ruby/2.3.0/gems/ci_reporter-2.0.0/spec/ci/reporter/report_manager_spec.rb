require File.dirname(__FILE__) + "/../../spec_helper.rb"

module CI::Reporter
  describe ReportManager do
    before(:each) do
      @reports_dir = REPORTS_DIR
      ENV.delete 'MAX_FILENAME_SIZE'
    end

    after(:each) do
      FileUtils.rm_rf @reports_dir
      ENV["CI_REPORTS"] = nil
    end

    it "creates the report directory according to the given prefix" do
      CI::Reporter::ReportManager.new("spec")
      expect(File.directory?(@reports_dir)).to be true
    end

    it "creates the report directory based on CI_REPORTS environment variable if set" do
      @reports_dir = "#{Dir.getwd}/dummy"
      ENV["CI_REPORTS"] = @reports_dir
      CI::Reporter::ReportManager.new("spec")
      expect(File.directory?(@reports_dir)).to be true
    end

    it "writes reports based on name and xml content of a test suite" do
      reporter = CI::Reporter::ReportManager.new("spec")
      suite = double("test suite")
      expect(suite).to receive(:name).and_return("some test suite name")
      expect(suite).to receive(:to_xml).and_return("<xml></xml>")
      reporter.write_report(suite)
      filename = "#{REPORTS_DIR}/SPEC-some-test-suite-name.xml"
      expect(File.exist?(filename)).to be true
      expect(File.read(filename)).to eql "<xml></xml>"
    end

    it "shortens extremely long report filenames" do
      reporter = CI::Reporter::ReportManager.new("spec")
      suite = double("test suite")
      very_long_name = "some test suite name that goes on and on and on and on and on and on and does not look like it will end any time soon and just when you think it is almost over it just continues to go on and on and on and on and on until it is almost over but wait there is more and then el fin"
      expect(suite).to receive(:name).and_return(very_long_name)
      expect(suite).to receive(:to_xml).and_return("<xml></xml>")
      reporter.write_report(suite)
      filename = "#{REPORTS_DIR}/SPEC-#{very_long_name}"[0..CI::Reporter::ReportManager::MAX_FILENAME_SIZE].gsub(/\s/, '-') + ".xml"
      expect(filename.length).to be <= 255
      expect(File.exist?(filename)).to be true
      expect(File.read(filename)).to eql "<xml></xml>"
    end

    it "shortens extremely long report filenames to custom length" do
      reporter = CI::Reporter::ReportManager.new("spec")
      suite = double("test suite")
      very_long_name = "some test suite name that goes on and on and on and on and on and on and does not look like it will end any time soon and just when you think it is almost over it just continues to go on and on and on and on and on until it is almost over but wait there is more and then el fin"
      expect(suite).to receive(:name).and_return(very_long_name)
      expect(suite).to receive(:to_xml).and_return("<xml></xml>")
      ENV['MAX_FILENAME_SIZE'] = '170'
      reporter.write_report(suite)
      filename = "#{REPORTS_DIR}/SPEC-#{very_long_name}"[0..170].gsub(/\s/, '-') + ".xml"
      expect(filename.length).to be <= 188
      expect(File.exist?(filename)).to be true
      expect(File.read(filename)).to eql "<xml></xml>"
    end

    it "sidesteps existing files by adding an incrementing number" do
      filename = "#{REPORTS_DIR}/SPEC-colliding-test-suite-name.xml"
      FileUtils.mkdir_p(File.dirname(filename))
      FileUtils.touch filename
      reporter = CI::Reporter::ReportManager.new("spec")
      suite = double("test suite")
      expect(suite).to receive(:name).and_return("colliding test suite name")
      expect(suite).to receive(:to_xml).and_return("<xml></xml>")
      reporter.write_report(suite)
      expect(File.exist?(filename.sub('.xml', '.0.xml'))).to be true
    end
  end
end
