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


