RSpec.configure do |config|
  config.expect_with :rspec do |c|
    c.syntax = [:should, :expect]
  end
end

describe "a passing example" do
  it "passes" do
    true.should be true
  end
end

describe "a failing example" do
  it "fails" do
    true.should be false
  end
end

describe "an errored example" do
  it "errors" do
    raise "What happened?"
  end
end

describe "a pending example" do
  it "is not run"
end

describe "a failure in a before block" do
  before do
    true.should be false
  end

  it "doesn't matter" do
    true.should be true
  end
end

describe "outer context" do
  it "passes" do
    true.should be true
  end

  describe "inner context" do
    it "passes" do
      true.should be true
    end
  end
end
