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
        .controller('SchemaUpdateContentCtrl', ['$uibModalInstance', '$scope', 'KafkaService', 'growl', 'projectId', 'schemaName', 'schemaVersion',
            function ($uibModalInstance, $scope, KafkaService, growl, projectId, schemaName, schemaVersion) {

                var self = this;
                self.projectId = projectId;
                self.schemaName = schemaName;
                self.contents;
                self.schemaVersion = schemaVersion;
                self.content_empty = 1;
                self.message ="";
                self.validSchema = "invalid";

                self.init = function () {

                    KafkaService.getSchemaContent(self.projectId, schemaName, self.schemaVersion).then(
                            function (success) {
                                $scope.jsonObj = success.data.contents;
                            }, function (error) {
                        growl.error(error.data.errorMsg, {title: 'Could not get schema for topic', ttl: 5000, referenceId: 21});
                    });
                };

                self.init();
                
                self.validateSchema = function () {
                   
                    self.validSchema = "invalid";

                    if(!self.contents){
                        self.content_empty = -1;
                        self.wrong_values = -1;
                    }

                    if(self.wrong_values === -1){
                        return;
                    }

                    var schemaDetail ={};
                    schemaDetail.name=self.schemaName;
                    schemaDetail.contents =self.contents;
                    //schemaDetail.version =self.version;
                    schemaDetail.versions =[];

                    KafkaService.validateSchema(self.projectId, schemaDetail).then(
                            function (success) {
                                self.message = "schema is valid";
                                self.validSchema="";
                            }, function (error) {
                                self.message = error.data.errorMsg;
                    });
                 };
                
                self.createSchema = function () {                   
                    
                    var schemaDetail = {};
                    schemaDetail.name = schemaName;
                    schemaDetail.contents =self.contents;
                    schemaDetail.version = self.schemaVersion + 1;

                    KafkaService.createSchema(self.projectId, schemaDetail).then(
                            function (success) {
                                $uibModalInstance.close(success);
                            }, function (error) {
                                 self.message = error.data.errorMsg;
                                 self.validSchema="invalid";
                    });
                };
                
                $scope.doWith = function (newJson) {
                  self.content_empty = 1;
                  self.wrong_values = 1;
                  self.contents = JSON.stringify(newJson, null, 2);
                };

                self.close = function () {
                    $uibModalInstance.dismiss('cancel');
                };
            }]);