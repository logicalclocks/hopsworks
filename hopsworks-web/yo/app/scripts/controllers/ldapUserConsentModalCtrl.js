'use strict';

angular.module('hopsWorksApp')
  .controller('LdapUserConsentModalCtrl', ['$uibModalInstance', 'data', 'val',
    function ($uibModalInstance, data, val) {

      var self = this;
      self.data = data;
      self.user = {givenName: '', surname: '', chosenEmail: data.email[0], consent: ''};

      self.ok = function () {
        $uibModalInstance.close({val: self.user});
      };
      
      self.cancel = function () {
        $uibModalInstance.dismiss('cancel');
      };

      self.reject = function () {
        $uibModalInstance.dismiss('reject');
      };
    }]);


