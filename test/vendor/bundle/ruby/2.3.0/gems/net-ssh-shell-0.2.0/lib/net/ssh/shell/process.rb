module Net; module SSH; class Shell
  class Process
    attr_reader :state
    attr_reader :command
    attr_reader :manager
    attr_reader :callback
    attr_reader :exit_status
    attr_reader :properties

    def initialize(manager, command, properties, callback)
      @command = command
      @manager = manager
      @callback = callback
      @properties = properties
      @on_output = nil
      @on_error_output = nil
      @on_finish = nil
      @state = :new
    end

    def [](key)
      @properties[key]
    end

    def []=(key, value)
      @properties[key] = value
    end

    def send_data(data)
      manager.channel.send_data(data)
    end

    def run
      if state == :new
        state = :starting
        manager.open do
          state = :running
          manager.channel.on_data(&method(:on_stdout))
          manager.channel.on_extended_data(&method(:on_stderr))
          manager.channel.on_close(&method(:on_close))

          callback.call(self) if callback

          cmd = command.dup
          cmd << ";" if cmd !~ /[;&]$/
          cmd << " DONTEVERUSETHIS=$?; echo #{manager.separator} $DONTEVERUSETHIS; echo \"exit $DONTEVERUSETHIS\"|sh"

          send_data(cmd + "\n")
        end
      end

      self
    end

    def starting?
      state == :starting
    end

    def running?
      state == :running
    end

    def finished?
      state == :finished
    end

    def busy?
      starting? || running?
    end

    def wait!
      manager.session.loop { busy? }
      self
    end

    def on_output(&callback)
      @on_output = callback
    end

    def on_error_output(&callback)
      @on_error_output = callback
    end

    def on_finish(&callback)
      @on_finish = callback
    end

    protected

    def output!(data)
      @on_output.call(self, data) if @on_output
    end

    def on_stdout(ch, data)
      if data.strip =~ /#{manager.separator} (\d+)$/
        before = $`
        output!(before) unless before.empty?
        finished!($1)
      else
        output!(data)
      end
    end

    def on_stderr(ch, type, data)
      @on_error_output.call(self, data) if @on_error_output
    end

    def on_close(ch)
      manager.on_channel_close(ch)
      finished!(-1)
    end

    def finished!(status)
      @state = :finished
      @exit_status = status.to_i
      @on_finish.call(self) if @on_finish
      manager.child_finished(self)
    end
  end
end; end; end
