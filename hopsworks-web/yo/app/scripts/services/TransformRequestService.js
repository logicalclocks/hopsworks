'use strict';

angular.module('hopsWorksApp')
        .factory('TransformRequest', function () {
          return {
            jQueryStyle: function (data) {
              var requestStr;
              if (data) {
                for (var key in data) {
                  if (requestStr) {
                    requestStr += '&' + key + '=' + encodeURIComponent(data[key]);
                  } else {
                    requestStr = key + '=' + data[key];
                  }
                }
              }
              return requestStr;
            }
          };
        });
