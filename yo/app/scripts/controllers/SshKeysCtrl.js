'use strict'

angular.module('hopsWorksApp')
        .controller('SshKeysCtrl', ['UserService', '$location', '$scope', 'growl', '$modalInstance',
          function (UserService, $location, $scope, growl, $modalInstance) {

            var self = this;
            self.working = false;

            self.keys = [];

            self.key = {
              name: '',
              publicKey: '',
              status: false
            };

            self.addSshKey = function () {
              growl.success("Add Ssh Key.", {title: 'Success', ttl: 5000, referenceId: 1});
              self.working = true;
              UserService.addSshKey(self.key.name, self.key.publicKey).then(
                      function (success) {
                        self.keys.push(success.data);
                        self.keys[self.keys.length-1].status = false;
                        growl.success("Your ssh key has been successfully added.", {title: 'Success', ttl: 5000, referenceId: 1});
                        $scope.sshKeysForm.$setPristine();
                        self.working = false;
                      },
                      function (error) {
                        self.errorMsg = error.data.errorMsg;
                        growl.error("Failed to add your ssh key.", {title: 'Error', ttl: 5000, referenceId: 1});
                        $scope.sshKeysForm.$setPristine();
                        self.working = false;
                      });
            };


            $scope.putSshKey = function () {
              growl.success("Put Ssh Key.", {title: 'Success', ttl: 2000, referenceId: 1});
              self.working = true;
              UserService.addSshKey(self.key.name, self.key.publicKey).then(
                  function (success) {
                    self.keys.push(success.data);
                    self.keys[self.keys.length-1].status = false;
                    growl.success("Your ssh key has been successfully added.", {title: 'Success', ttl: 5000, referenceId: 1});
                    $scope.sshKeysForm.$setPristine();
                    self.working = false;
                  },
                  function (error) {
                    self.errorMsg = error.data.errorMsg;
                    growl.error("Failed to add your ssh key.", {title: 'Error', ttl: 5000, referenceId: 1});
                    $scope.sshKeysForm.$setPristine();
                    self.working = false;
                  });
            };

            self.removeSshKey = function (keyName) {
              self.working = true;
              UserService.removeSshKey(keyName).then(
                      function (success) {
                        growl.success("Your ssh key has been successfully removed.", {title: 'Success', ttl: 5000, referenceId: 1});
                        self.working = false;
                      }, function (error) {
                        self.working = false;
                        self.errorMsg = error.data.errorMsg;
                        growl.error("Failed to remove your ssh key.", {title: 'Error', ttl: 5000, referenceId: 1});
              });
            };

            self.getSshKeys = function () {
              UserService.getSshKeys().then(
                  function (success) {
                    self.keys = success.data;
                  }, function (error) {
                    self.errorMsg = error.data.errorMsg;
                    growl.info("No ssh keys registered.", {title: 'Info', ttl: 5000, referenceId: 1});
                  });
            };



            self.reset = function () {
              $scope.sshKeysForm.$setPristine();
            };

            self.close = function () {
              $modalInstance.dismiss('cancel');
            };


            self.getSshKeys();

          }]);
