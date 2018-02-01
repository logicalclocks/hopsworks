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

angular.module('hopsWorksApp')
        .controller('DelaClientCtrl', ['DelaService', '$routeParams', '$scope', '$interval', 'growl', 'ModalService',
          function (DelaService, $routeParams, $scope, $interval, growl, ModalService) {
            var self = this;
            self.clientType = "NO_CLIENT";
            
            self.hopsUploadEnabled = function() {
              if(self.clientType === "FULL_CLIENT") {// DelaClientType.java
                return true;
              } else {
                return false;
              }
            };
            
            var getClientType = function() {
              DelaService.getClientType().then(
                  function (success) {                   
                    self.clientType = success.data.clientType;
                  }, function (error) {
                    growl.error(error.data.errorMsg, {title: 'Error', ttl: 1500});
                });
            };
            
            getClientType();
          }]);
