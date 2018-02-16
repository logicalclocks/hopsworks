
/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
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