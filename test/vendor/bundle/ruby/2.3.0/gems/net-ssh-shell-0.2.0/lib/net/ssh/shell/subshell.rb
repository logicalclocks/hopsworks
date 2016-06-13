require 'net/ssh/shell/process'

module Net; module SSH; class Shell
  class Subshell < Process
    protected

    def on_stdout(ch, data)
      if !output!(data)
        ch.on_data(&method(:look_for_finalize_initializer))
        ch.send_data("export PS1=; echo #{manager.separator} $?\n")
      end
    end

    def look_for_finalize_initializer(ch, data)
      if data =~ /#{manager.separator} (\d+)/
        ch.on_close(&@master_onclose)
        finished!($1)
      end
    end
  end
end; end; end
