'use strict'

angular.module('hopsWorksApp')
    .controller('SshKeysCtrl', ['UserService', '$location', '$scope', 'growl', '$modalInstance',
        function (UserService, $location, $scope, growl, $modalInstance) {

            var self = this;
            self.working = false;

            $scope.keys = [];

            self.key = {
                name: '',
                publicKey: ''
            };


            var idx = function (obj) {
                return obj.name;
            };

            $scope.status = {};

            self.addSshKey = function () {
                self.working = true;
                UserService.addSshKey(self.key.name, self.key.publicKey).then(
                    function (success) {
                        growl.success("Your ssh key has been successfully added.", {
                            title: 'Success',
                            ttl: 5000,
                            referenceId: 1
                        });
                        $scope.keys.push(self.key);
                        $scope.sshKeysForm.$setPristine();
                        $scope.keys[$scope.keys.length - 1].status = false;
                        //var name = self.key.name;
                        //$scope.status[idx(self.key)] = false;
                        self.working = false;
                    },
                    function (error) {
                        self.errorMsg = error.data.errorMsg;
                        growl.error("Failed to add your ssh key: " + self.errorMsg, {
                            title: 'Error',
                            ttl: 5000,
                            referenceId: 1
                        });
                        $scope.sshKeysForm.$setPristine();
                        self.working = false;
                    });
            };


            self.removeSshKey = function (keyName) {
                self.working = true;
                UserService.removeSshKey(keyName).then(
                    function (success) {
                        self.working = false;
                        self.getSshKeys();
                        growl.success("Your ssh key has been successfully removed.", {
                            title: 'Success',
                            ttl: 5000,
                            referenceId: 1
                        });
                    }, function (error) {
                        self.working = false;
                        self.errorMsg = error.data.errorMsg;
                        growl.error("Failed to remove your ssh key.", {title: 'Error', ttl: 5000, referenceId: 1});
                    });
            };

            self.getSshKeys = function () {
                UserService.getSshKeys().then(
                    function (success) {
                        $scope.keys = success.data;
                    });
            }, function (error) {
                self.errorMsg = error.data.errorMsg;
                growl.info("No ssh keys registered: " + error.data.errorMsg, {
                    title: 'Info',
                    ttl: 5000,
                    referenceId: 1
                });
            };


            self.reset = function () {
                $scope.sshKeysForm.$setPristine();
            };

            self.close = function () {
                $modalInstance.dismiss('cancel');
            };


            self.getSshKeys();

        }
    ]
);
