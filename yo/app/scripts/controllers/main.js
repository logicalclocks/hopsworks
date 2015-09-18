/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

angular.module('hopsWorksApp')
        .controller('MainCtrl', ['$interval','$cookies', '$location','$scope', 'AuthService', 'UtilsService', 'ElasticService', 'md5', 'ModalService','ProjectService','growl','MessageService',
          function ($interval, $cookies, $location, $scope, AuthService, UtilsService, ElasticService, md5, ModalService, ProjectService, growl, MessageService) {

            var self = this;
            self.email = $cookies['email'];
            self.emailHash = md5.createHash(self.email || '');

            self.logout = function () {
                AuthService.logout(self.user).then(
                    function (success) {
                        $location.url('/login');
                        delete $cookies.email;
                        localStorage.removeItem("SESSIONID");
                        sessionStorage.removeItem("SESSIONID");
                    }, function (error) {
                        self.errorMessage = error.data.msg;
                    });
            };

            self.profileModal = function () {
                ModalService.profile('md');
            };

            self.sshKeysModal = function () {
                ModalService.sshKeys('lg');
            };

            self.getHostname = function()  {
                return $location.host();
            };

            self.getUser = function()  {
                return self.email.substring(0, self.email.indexOf("@"));
            };


            self.view = function (selected, projectOrDataset) {
                if (projectOrDataset === 'parent') {
                    ProjectService.getProjectInfo({projectName: selected.name}).$promise.then(
                        function (success) {
                            ModalService.viewSearchResult('md', success, projectOrDataset)
                                .then(function (success) {
                                    growl.success(success.data.successMessage, {title: 'Success', ttl: 5000});
                                }, function (error) {

                                });
                        }, function (error) {
                            growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
                        }
                    );
                } else if (projectOrDataset === 'child') {
                    ProjectService.getDatasetInfo({inodeId: selected.inode_id}).$promise.then(
                        function (success) {
                            ModalService.viewSearchResult('md', success, projectOrDataset)
                                .then(function (success) {
                                    growl.success(success.data.successMessage, {title: 'Success', ttl: 5000});
                                }, function (error) {

                                });
                        }, function (error) {
                            growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
                        }
                    );
                }
            };
            
            var getUnreadCount = function () {
                MessageService.getUnreadCount().then(
                    function (success) {
                      self.unreadMessages = success.data.data.value;
                    }, function (error) {

                    });
            };
            var getMessages = function () {//
                MessageService.getMessages().then(
                    function (success) {
                        self.messages = success.data;
                        console.log(success);
                    }, function (error) {

                    });
            };
            getUnreadCount();
            getMessages();
            //this might be a bit to frequent for refresh rate 
            var getUnreadCountInterval = $interval(function () { getUnreadCount(); },1000);
            self.getMessages = function () {
                getMessages();
            };
            self.openMessageModal = function (selected) {
                if (selected !== undefined) {
                    MessageService.markAsRead(selected.id);
                };
                ModalService.messages('lg', selected)
                    .then(function (success) {
                        growl.success(success.data.successMessage, {title: 'Success', ttl: 5000});
                    }, function (error) { });
            };

            self.searchTerm = "";
            self.searchReturned = "";
            self.searchResult = [];
            self.resultPages = 0;
            self.resultItems = 0;
            self.currentPage = 1;
            self.pageSize = 5;

            self.keyTyped = function (evt) {

                if (self.searchTerm.length > 3) {
                    self.search();
                } else {
                    self.searchResult = [];
                    self.searchReturned = "";
                }
            };

            self.search = function () {
                //ask for the index name and project name when it is time to search
                self.index = UtilsService.getIndex();
                self.projectName = UtilsService.getProjectName();
                self.currentPage = 1;
                self.pageSize = 5;
                var searchQuery = ElasticService.query(self.index, self.projectName, self.searchTerm);

                ElasticService.search(searchQuery, self.index)
                    .then(function (response) {
                        var data = response.data.hits.hits;
                        self.searchResult = [];
                        self.searchReturned = "";
                        if (data.length > 0) {
                            self.searchReturned = "Result for <b>" + self.searchTerm + "</b>";
                            self.searchResult = data;
                        } else {
                            self.searchResult = [];
                            self.searchReturned = "No result found for <b>" + self.searchTerm + "</b>";
                        }
                        self.resultPages = Math.ceil(self.searchResult.length / self.pageSize);
                        self.resultItems = self.searchResult.length;
                    }, function (error) {
                    });

                datePicker();// this will load the function so that the date picker can call it.
            };
            var datePicker = function () {
                $(function () {
                    $('#datetimepicker1').datetimepicker();
                });
            };
            $scope.$on("$destroy", function (){$interval.cancel(getUnreadCountInterval); });
        }]);
