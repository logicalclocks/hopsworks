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


