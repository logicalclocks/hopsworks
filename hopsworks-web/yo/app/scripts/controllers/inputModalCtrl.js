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


