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

/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

/**
 * A helper service to enable communication between datasetsCtrl and 
 * metadataCtrl.
 */
angular.module('hopsWorksApp')
        .service('MetadataHelperService', ['$cookies', '$q', 'MetadataActionService',
          function ($cookies, $q, MetadataActionService) {

            var currentFile = {};
            var availableTemplates = [];
            var dirContents = "false";

            return {
              setCurrentFile: function (currentfile) {
                currentFile = currentfile;
              },
              getCurrentFile: function () {
                return currentFile;
              },
              fetchAvailableTemplates: function () {
                var defer = $q.defer();

                MetadataActionService.fetchTemplates($cookies.get("email"))
                        .then(function (data) {
                          if (data !== null && data.status !== "ERROR" && 
                              data.board !== null && data.board !== {}) {
                              angular.copy(JSON.parse(data.board).templates, availableTemplates);
                              defer.resolve(data);
                          }
                        });

                return defer.promise;
              },
              getAvailableTemplates: function () {
                return availableTemplates;
              },

              getDirContents: function(){
                return dirContents;
              },
              setDirContents: function(value){
                dirContents = "true";
              }
            };
          }]);


