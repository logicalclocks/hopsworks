/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

/**
 * A helper service to enable communication between datasetsCtrl and 
 * metadataSliderCtrl.
 */
angular.module('hopsWorksApp')
        .service('MetadataHelperService', ['$cookies', '$rootScope', '$q', 'MetadataActionService',
          function ($cookies, $rootScope, $q, MetadataActionService) {

            var self = this;
            var currentFile = {};
            var availableTemplates = [];

            return {
              setCurrentFile: function (currentfile) {
                currentFile = currentfile;
              },
              getCurrentFile: function () {
                return currentFile;
              },
              fetchAvailableTemplates: function () {
                var defer = $q.defer();

                MetadataActionService.fetchTemplates($cookies['email'])
                        .then(function (data) {
                          angular.copy(JSON.parse(data.board).templates, availableTemplates);
                          console.log("FETCHED AVAILABLE TEMPLATES " + JSON.stringify(availableTemplates));
                          defer.resolve(data);
                        });

                return defer.promise;
              },
              getAvailableTemplates: function () {
                return availableTemplates;
              }
            };
          }]);


