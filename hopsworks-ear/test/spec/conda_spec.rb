=begin
Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved

Permission is hereby granted, free of charge, to any person obtaining a copy of this
software and associated documentation files (the "Software"), to deal in the Software
without restriction, including without limitation the rights to use, copy, modify, merge,
publish, distribute, sublicense, and/or sell copies of the Software, and to permit
persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or
substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
=end

describe 'conda' do
  after (:all){clean_projects}
  describe "#create" do
    context 'without authentication' do
      before :all do
        with_valid_project
        reset_session
      end
      it "not authenticated" do
        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps/enable/2.7/true"
        expect_json(errorMsg: "Client not authorized for this invocation")
        expect_status(401)
      end
    end

    context 'with authentication' do
      before :all do
        with_valid_project
      end
      it 'enable anaconda' do
        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps/enable/2.7/true"
        expect_status(200)

        wait_for do
          response = get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps/"
          hops_library = json_body.detect { |library| library[:lib] == "hops" }
          !hops_library.nil?
        end
      end

      it 'install libraries' do
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps/install", {lib: "imageio", version: "2.2.0", channelUrl: "defaults", installType: "CONDA", machineType: "CPU"}
        expect_status(200)

        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps/install", {lib: "tflearn", version: "0.3.2", channelUrl: "PyPi", installType: "PIP", machineType: "ALL"}
        expect_status(200)

      end


      it 'list libraries' do
        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps/"

        tflearn_library = json_body.detect { |library| library[:lib] == "tflearn" }
        hops_library = json_body.detect { |library| library[:lib] == "hops" }
        imageio_library = json_body.detect { |library| library[:lib] == "imageio" }

        expect(tflearn_library[:machineType]).to eq ("ALL")
        expect(tflearn_library[:installType]).to eq ("PIP")
        expect(tflearn_library[:version]).to eq ("0.3.2")

        expect(hops_library[:machineType]).to eq ("ALL")
        expect(hops_library[:installType]).to eq ("PIP")

        expect(imageio_library[:machineType]).to eq("CPU")
        expect(imageio_library[:installType]).to eq("CONDA")
        expect(imageio_library[:version]).to eq ("2.2.0")

      end

      it 'uninstall libraries' do

        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps/remove", {lib: "tflearn", version: "0.3.2", channelUrl: "PyPi", installType: "PIP", machineType: "ALL"}
        expect_status(200)

        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps/remove", {lib: "imageio", version: "2.2.0", channelUrl: "defaults", installType: "CONDA", machineType: "CPU"}
        expect_status(200)

        wait_for do
            get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps"
            imageio_library = json_body.detect { |library| library[:lib] == "imageio" }
            imageio_library.nil?
        end

        wait_for do
            get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps"
            tflearn_library = json_body.detect { |library| library[:lib] == "tflearn" }
            tflearn_library.nil?
        end

      end

      it 'remove env' do

        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/pythonDeps/destroyAnaconda"
        expect_status(200)

      end
    end
  end
end
