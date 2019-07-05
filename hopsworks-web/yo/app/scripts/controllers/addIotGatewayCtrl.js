/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 */

angular.module('hopsWorksApp')
        .controller('AddIotGatewayCtrl', ['$uibModalInstance', 'KafkaService',
        'growl', 'projectId',
          function ($uibModalInstance, KafkaService, growl, projectId) {

            var self = this;
            self.projectId = projectId;
            self.name;
            self.domain;
            self.port;
            self.name_wrong_value = 1;
            self.domain_wrong_value = 1;
            self.port_wrong_value = 1;
            self.wrong_values = 1;
            self.working = false;
            

            self.init = function() {
            };
            
            self.init();

            self.addIotGateway = function () {
              self.working = true;
              self.wrong_values = 1;
              self.name_wrong_value = 1;
              self.domain_wrong_value = 1;
              self.port_wrong_value = 1;
              
              if(!self.name){
                self.name_wrong_value =-1;
                self.wrong_values=-1;
              }else{
                self.name_wrong_value =1;
              }
              if(!self.domain){
                  self.domain_wrong_value =-1;
                  self.wrong_values=-1;
              }else{
                  self.domain_wrong_value =1;
              }
              if(!self.port){
                  self.port_wrong_value =-1;
                  self.wrong_values=-1;
              }else{
                  self.port_wrong_value =1;
              }
              
              if(self.wrong_values === -1){
                  self.working = false;
                  return;
              }

                var iotGatewayDetail ={};
                iotGatewayDetail.name = self.name;
                iotGatewayDetail.domain = self.domain;
                iotGatewayDetail.port = self.port;
              

              KafkaService.registerIotGateway(self.projectId, iotGatewayDetail).then(
                      function (success) {
                        self.working = false;
                          $uibModalInstance.close(success);
                      }, function (error) {
                      if (typeof error.data.usrMsg !== 'undefined') {
                          growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 5000, referenceId: 21});
                      } else {
                          growl.error("", {title: error.data.errorMsg, ttl: 5000, referenceId: 21});
                      }
                        self.working = false;
              });      
            };

            self.close = function () {
              $uibModalInstance.dismiss('cancel');
            };
          }]);
