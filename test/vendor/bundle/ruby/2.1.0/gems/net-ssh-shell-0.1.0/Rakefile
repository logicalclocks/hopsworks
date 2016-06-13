require './lib/net/ssh/shell/version'

begin
  require 'echoe'
rescue LoadError
  abort "You'll need to have `echoe' installed to use Net::SSH::Shell's Rakefile"
end

version = Net::SSH::Shell::Version::STRING.dup
if ENV['SNAPSHOT'].to_i == 1
  version << "." << Time.now.utc.strftime("%Y%m%d%H%M%S")
end

Echoe.new('net-ssh-shell', version) do |p|
  p.changelog        = "CHANGELOG.rdoc"

  p.author           = "Jamis Buck"
  p.email            = "jamis@jamisbuck.org"
  p.summary          = "A simple library to aid with stateful shell interactions"
  p.url              = "http://net-ssh.rubyforge.org/shell"

  p.dependencies     = ["net-ssh >=2.0.9"]

  p.need_zip         = true
  p.include_rakefile = true

  p.rdoc_pattern     = /^(lib|README.rdoc|CHANGELOG.rdoc)/
end
