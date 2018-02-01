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

'use strict';

angular.module('hopsWorksApp')
        .controller('PublicDSPanelCtrl', ['$scope', 'DelaService',
          function ($scope, DelaService) {
            var self = this;
            $scope.$watch("content", function (newValue, oldValue) {
              init(newValue);
            });

            var init = function (content) {
              DelaService.getDetails(content.publicId).then(function (success) {
//                console.log("init", success);
                content["leechers"] = success.data.dataset.datasetHealth.leechers;
                content["seeders"] = success.data.dataset.datasetHealth.seeders;
                content["bootstrap"] = success.data.bootstrap;
              }, function (error) {
                return [];
                console.log("init", error);
              });
            };

            self.sizeOnDisk = function (fileSizeInBytes) {
              if (fileSizeInBytes === undefined) {
                return '--';
              }
              return convertSize(fileSizeInBytes);
            };
          }]);

