=begin
 This file is part of Hopsworks
 Copyright (C) 2020, Logical Clocks AB. All rights reserved

 Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 the GNU Affero General Public License as published by the Free Software Foundation,
 either version 3 of the License, or (at your option) any later version.

 Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 PURPOSE.  See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along with this program.
 If not, see <https://www.gnu.org/licenses/>.
=end
module SearchHelper
  def fix_search_xattr_json(json_string, symbolize_names)
    json1 = json_string.gsub(/([a-zA-Z_0-9]+)=/, '"\1"=')
    json2 = json1.gsub(/=([a-zA-Z_0-9]+)/, '="\1"')
    json3 = json2.gsub('=>', ':')
    json4 = json3.gsub('=', ':')
    #pp "#{[JSON.parse(json3)].class}"
    JSON.parse(json4, :symbolize_names => symbolize_names)
  end

  def project_search(project, term)
    get "#{ENV['HOPSWORKS_API']}/elastic/projectsearch/#{project[:id]}/#{term}"
    pp "#{ENV['HOPSWORKS_API']}/elastic/projectsearch/#{project[:id]}/#{term}" if (defined?(@debugOpt)) && @debugOpt
    expect_status_details(200)
    json_body
  end

  def dataset_search(project, dataset, term)
    get "#{ENV['HOPSWORKS_API']}/elastic/datasetsearch/#{project[:id]}/#{dataset[:inode_name]}/#{term}"
    pp "#{ENV['HOPSWORKS_API']}/elastic/datasetsearch/#{project[:id]}/#{dataset[:inode_name]}/#{term}" if (defined?(@debugOpt)) && @debugOpt
    expect_status_details(200)
    json_body
  end

  def global_featurestore_search(doc_type, term, from: nil, size: nil)
    if from != nil && size != nil
      pp "#{ENV['HOPSWORKS_API']}/elastic/featurestore/#{term}?docType=#{doc_type}&from=#{from}&size=#{size}" if (defined?(@debugOpt)) && @debugOpt
      result = get "#{ENV['HOPSWORKS_API']}/elastic/featurestore/#{term}?docType=#{doc_type}&from=#{from}&size=#{size}"
    else
      pp "#{ENV['HOPSWORKS_API']}/elastic/featurestore/#{term}?docType=#{doc_type}" if (defined?(@debugOpt)) && @debugOpt
      result = get "#{ENV['HOPSWORKS_API']}/elastic/featurestore/#{term}?docType=#{doc_type}"
    end
    expect_status_details(200)
    parsed_result = JSON.parse(result)
    pp parsed_result if (defined?(@debugOpt)) && @debugOpt
    parsed_result
  end

  def local_featurestore_search(project, doc_type, term, from: nil, size: nil)
    if from != nil && size != nil
      pp "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/elastic/featurestore/#{term}?docType=#{doc_type}&from=#{from}&size=#{size}" if (defined?(@debugOpt)) && @debugOpt
      result = get "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/elastic/featurestore/#{term}?docType=#{doc_type}&from=#{from}&size=#{size}"
    else
      pp "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/elastic/featurestore/#{term}?docType=#{doc_type}" if (defined?(@debugOpt)) && @debugOpt
      result = get "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/elastic/featurestore/#{term}?docType=#{doc_type}"
    end
    expect_status_details(200)
    parsed_result = JSON.parse(result)
    pp parsed_result if (defined?(@debugOpt)) && @debugOpt
    parsed_result
  end

  def result_contains_xattr_one_of(result, &xattr_predicate)
    array_contains_one_of(result) do |r|
      selected_aux = r[:map][:entry].select {|x| x[:key] == "xattr"}
      if selected_aux.length == 1
        selected_json = fix_search_xattr_json(selected_aux[0][:value], true)
        xattr_predicate.call(selected_json)
      else
        return false
      end
    end
  end

  def expect_searched(item, expected)
    expected = expected.transform_keys(&:to_s)
    #base check
    expected.each do |key, _|
      next if key == "highlight" || key == "count"
      expect(item[key]).to eq(expected[key])
    end
    if expected.key?("highlight")
      if expected["highlight"].is_a?(Hash)
        expected["highlight"].each do |key, value|
          if key == "features"
            matched = item["highlights"]["features"].select { |i| i.key?(value) }
            expect(matched.count).to eq(1)
          else
            expect(item["highlights"].key?(key)). to be true
          end
        end
      else
        expect(item["highlights"].key?(expected["highlight"])). to be true
      end
    end
    if expected.key?("count")
      expected_count = expected["count"].transform_keys(&:to_s)
      expected_count.each do |key, _|
        if item[key].key?("entry")
          #java maps get serialized like this
          expect(item[key]["entry"].length).to eq(expected_count[key])
        else
          expect(item[key].length).to eq(expected_count[key])
        end
      end
    end
  end

  def check_searched(item, expected)
    begin
      expect_searched(item, expected)
      true
    rescue RSpec::Expectations::ExpectationNotMetError => e
      pp "expected:#{expected}" if defined?(@debugOpt) && @debugOpt
      pp "found:#{item}" if defined?(@debugOpt) && @debugOpt
      pp e if defined?(@debugOpt) && @debugOpt
      false
    end
  end

  def project_search_test(project, term, type, items, result_type: nil)
    search_type = type.upcase
    result_type = "#{type}s" if result_type.nil?
    wait_result = wait_for_me_time(15) do
      search_hits = local_featurestore_search(project, search_type, term)["#{result_type}"]
      error_msg = "search expected:#{items.length}, found:#{search_hits.length}"
      if search_hits.length != items.length
        { 'success' => false, 'msg' => error_msg }
      else
        matched_items = 0
        items.each do |item|
          matched = search_hits.select { |r|
            check_searched(r, item)
          }
          matched_items = matched_items + 1 if matched.length == 1
        end
        if matched_items == items.length
          { 'success' => true }
        else
          { 'success' => false, 'msg' => "result items do not match expected" }
        end
      end
    end
    expect(wait_result["success"]).to be(true), wait_result["msg"]
  end

  def global_search_test(term, type, items, result_type: nil)
    search_type = type.upcase
    result_type = "#{type}s" if result_type.nil?
    wait_result = wait_for_me_time(15) do
      search_hits = global_featurestore_search(search_type, term)["#{result_type}"]
      pp search_hits if defined?(@debugOpt) && @debugOpt
      error_msg = "expected:#{items}, found:#{search_hits}"
      if search_hits.length < items.length
        { 'success' => false, 'msg' => error_msg }
      else
        matched_items = 0
        items.each do |item|
          matched = search_hits.select { |r|
            check_searched(r, item)
          }
          pp "mismatched: #{item}" if defined?(@debugOpt) && @debugOpt && matched.length != 1
          matched_items = matched_items + 1 if matched.length == 1
        end
        if matched_items == items.length
          { 'success' => true }
        else
          { 'success' => false, 'msg' => error_msg }
        end
      end
    end
    expect(wait_result["success"]).to be(true), wait_result["msg"]
  end
end