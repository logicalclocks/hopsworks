/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

angular.module('hopsWorksApp')
        .controller('MainCtrl', ['$cookies', '$location', 'AuthService', 'UtilsService', 'ElasticService', 'md5', 'ModalService',
            function ($cookies, $location, AuthService, UtilsService, ElasticService, md5, ModalService) {

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

                self.searchTerm = "";
                self.searchResult = "";

                self.keyTyped = function (evt) {
                    
                    if (self.searchTerm.length > 3) {
                        self.search();
                    }else{
                        self.searchResult = "";
                    }

                    if (angular.equals(evt.keyCode, 13)) {
                        self.search();
                    }
                };

                self.search = function () {
                    //ask for the index name and project name when it is time to search
                    self.index = UtilsService.getIndex();
                    self.projectName = UtilsService.getProjectName();
                    
                    //console.log("SEARCHING FOR  " + self.searchTerm);
                    console.log("INDEX " + self.index + " PROJECTNAME: " + self.projectName);
                    
                    var searchQuery = ElasticService.query(self.index, self.projectName, self.searchTerm);
                    
                    ElasticService.search(searchQuery, self.index)
                            .then(function (response) {
                                console.log("DATA RETURNED " + JSON.stringify(response.data.hits));
                                var data = response.data.hits.hits;

                                self.searchResult = "";
                                if (data.length > 0) {
                                    $.each(data, function (i, val) {
                                        var source = val._source;
                                        self.searchResult += "<h3>" + source.name + "</h3>modified at <b> " + source.modified + "</b><hr/>";
                                    });
                                }
                                else {
                                    self.searchResult = "Search <b>" + self.searchTerm + "</b> did not \n\
                                                find any document. Try different keywords";
                                }
                            });
                };
            }]);