module CI
  module Reporter
    def self.maybe_quote_filename(fn)
      if fn =~ /\s/
        fn = %{"#{fn}"}
      end
      fn
    end
  end
end
