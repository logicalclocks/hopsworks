/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

/**
 * A helper service to enable communication between datasetsCtrl and 
 * metadataSliderCtrl.
 */
angular.module('hopsWorksApp')
        .service('MetadataHelperService', ['$cookies', '$q', 'MetadataActionService',
          function ($cookies, $q, MetadataActionService) {

            var self = this;
            var currentFile = {};
            var availableTemplates = [];

            return function () {
              var services = {
                setCurrentFile: function (currentfile) {
                  currentFile = currentfile;
                },
                getCurrentFile: function () {
                  return currentFile;
                },
                getAvailableTemplates: function () {
                  var defer = $q.defer();

                  MetadataActionService.fetchTemplates($cookies['email'])
                          .then(function (data) {
                            availableTemplates = JSON.parse(data.board).templates;
                            defer.resolve(data);
                          });

                  return defer.promise;
                }
              };
              return services;
            };
          }]);


