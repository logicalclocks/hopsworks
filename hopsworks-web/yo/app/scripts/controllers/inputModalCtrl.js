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
        .controller('InputModalCtrl', ['$uibModalInstance', 'title', 'msg','val', '$uibModal',
          function ($uibModalInstance, title, msg, val, $uibModal) {

            var self = this;
            self.title = title;
            self.msg = msg;
            self.val = {name:''};

            self.ok = function () {
              $uibModalInstance.close({val: self.val});
            };

            self.cancel = function () {
              $uibModalInstance.dismiss('cancel');
            };
            
            self.reject = function () {
              $uibModalInstance.dismiss('reject');
            };
            
            self.selectFile = function (extension) {

                var modalInstance = $uibModal.open({
                  templateUrl: 'views/selectFile.html',
                  controller: 'SelectFileCtrl as selectFileCtrl',
                  size: 'md',
                  resolve: {
                    regex: function () {
                      return '/.' + extension + '\b/';
                    },
                    errorMsg: function () {
                      return "Please select a " + extension + " file.";
                    }
                  }
                });
              
              modalInstance.result.then(
                      function (success) {
                        if (extension === ".zip") {
                          self.val.archives = "hdfs://" + success;
                        } else if (extension === ".jar") {
                          self.val.jars = "hdfs://" + success;
                        } else if (extension === "*") {
                          self.val.files = "hdfs://" + success;
                        } else if (extension === ".py") {
                          self.val.pyFiles = "hdfs://" + success;
                        } else {
                          self.val.archives = "hdfs://" + success;
                        }
                      }, function (error) {
                //The user changed their mind.
              });            

            };
            
            

          }]);


