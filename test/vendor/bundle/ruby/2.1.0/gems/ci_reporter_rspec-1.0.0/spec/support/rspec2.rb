module RSpec2Helpers
  def rspec2_failing_example(exception_text)
    double('RSpec2.2 Example',
           :execution_result => {
             :exception => StandardError.new(exception_text)
           },
           :metadata => {
             :example_group => {
               :full_description => "description"
             }
           })
  end
end
