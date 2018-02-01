
/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

//<![CDATA[
$(document).ready(function () {

  var search = function () {
    var data = {
      query: {
        filtered: {
          query: {
            has_parent: {
              type: "parent",
              query: {
                match: {
                  name: $('#hiddenProjectName').val()
                }
              }
            }
          },
          filter: {
            query: {
              //combine the results of a prefix and a fuzzy query
              bool: {
                must: {
                  //matches names with the given prefix
                  match_phrase_prefix: {
                    name: {
                      query: $('#searchbox').val(),
                      slop: 0
                    }
                  }
                },
                should: {
                  fuzzy_like_this_field: {
                    name: {
                      like_text: $('#searchbox').val()
                    }
                  }
                }
              }
            }
          }
        }
      },
      size: 10,
      from: 0
    };

    $.ajax({
      type: "POST",
      url: "http://localhost:9200/project/_search",
      datatype: "json",
      crossDomain: true,
      contentType: 'application/x-www-form-urlencoded',
      data: JSON.stringify(data),
      beforeSend: function (xhr, settings) {
        //console.log(JSON.stringify(data));
      },
      success: function (response) {
        var data = response.hits.hits;
        //console.log("DATA RETURNED " + JSON.stringify(data));

        var result = "";
        if (data.length > 0) {
          $.each(data, function (i, val) {

            var source = val._source;
            result += "<h3>" + source.name + "</h3>modified at <b> " + source.modified + "</b><hr/>";
          });
          $("#searchresults").html(result);
        } else {
          $("#searchresults").html("Search <b>" + $('#searchbox').val() + "</b> did not \n\
                                                    find any document. Try different keywords");
        }
      },
      async: true,
      error: function (xhr, textStatus, errorThrown) {
        JSON.stringify(xhr.responseText);
      }
    });
  };

  $("#searchbox").keyup(function (event) {

    if (event.which === 13) {
      event.preventDefault();
      search();
    }

    var searchText = $(this).val();
    if (searchText.length > 3) {
      search();
    } else {
      $("#searchresults").html("");
    }
  });
});
//]]>