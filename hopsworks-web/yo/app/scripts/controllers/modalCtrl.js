'use strict';

angular.module('hopsWorksApp')
        .controller('ModalCtrl', ['$uibModalInstance', 'AuthService', 'DataSetService', 'growl', 'title', 'msg', 'projectId',
          function ($uibModalInstance, AuthService, DataSetService, growl, title, msg, projectId) {

            var self = this;
            self.title = title;
            self.msg = msg;
            self.projectId = projectId;
            self.password;
            self.content;

            self.ok = function () {
              $uibModalInstance.close(self.content);
            };

            self.cancel = function () {
              $uibModalInstance.dismiss('cancel');
            };

            self.reject = function () {
              $uibModalInstance.dismiss('reject');
            };

            self.certs = function () {
              var user = {password: self.password};

              AuthService.validatePassword(user).then(
                      function (success) {
                        self.password = "";
                        var dataSetService = DataSetService(self.projectId);
                        dataSetService.getCerts().then(
                                function (success) {
                                }, function (error) {
                          growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
                        });
                      }, function (error) {
                self.password = "";
                growl.error("Wrong password", {title: 'Error', ttl: 5000});
              });
              $uibModalInstance.close(self.content);
            };

          }]);
