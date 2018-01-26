'use strict';

angular.module('hopsWorksApp')
        .controller('ModalCtrl', ['$uibModalInstance', 'title', 'msg', 'projectId',
          function ($uibModalInstance, title, msg, projectId) {

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
              $uibModalInstance.close(self.password);
            };

          }]);
