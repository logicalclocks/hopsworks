module FactoryHelper
  def random_id
    SecureRandom.hex(20)
  end

  def short_random_id
    SecureRandom.hex(4)
  end

  def pr(res)
    res.on_output do |val1, val2|
      # a is the process itself, b is the output
      p val2
    end
    # puts "Exit Status:#{res.exit_status}"
    # puts "Command Executed:#{res.command}"
  end
end
