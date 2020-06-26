/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */

'use strict';

angular.module('hopsWorksApp')
    .controller('SearchCtrl', ['$q', '$location', '$rootScope', '$scope', '$routeParams', 'PaginationService', 'growl', 'ElasticService', 'FeaturestoreService', 'HopssiteService',
        function($q, $location, $rootScope, $scope, $routeParams, PaginationService, growl, ElasticService, FeaturestoreService, HopssiteService) {
            const MAX_IN_MEMORY_ITEMS = 1000;
            var self = this;
            self.closeInfo = true;
            self.selected = undefined;
            self.selectedType = undefined;
            self.searchScope = $location.search()['scope'];
            self.searchTerm = $location.search()['q'];
            self.projectId = $routeParams.projectID;
            self.searching = false;
            self.gettingReadme = false;
            self.readme = undefined;
            self.hideReadme = true;
            self.categories = [];

            $scope.reverse = undefined;
            $scope.sortBy = undefined;
            $scope.selectedIndex = 0;

            const searchScopes = {
                "datasetCentric": ['This dataset'],
                "projectCentric": ['This project', 'Datasets', 'Feature store'],
                "global": ['Everything', 'Projects', 'Datasets', 'Feature store']
            };

            const fsResults = {featuregroups: [], featuregroupsTotal: 0,
                trainingdatasets: [], trainingdatasetsTotal: 0,
                features: [], featuresTotal: 0};

            const otherResults = {projects: [], datasets: [], inodes: []};

            var getTags = function () {
                FeaturestoreService.getTags('?sort_by=name:asc').then(function (success) {
                    self.categories =  success.data.items;
                    self.clearCategorySelected();
                }, function (error) {

                })
            };

            self.clearCategorySelected = function () {
                if (angular.isUndefined(self.categories)) {
                    return;
                }
                for(var i = 0; i < self.categories.length; i++) {
                    self.categories[i].selected = false;
                }
            };

            self.select = function (item, type) {
                self.closeInfo = false;
                self.selected = item;
                self.selectedType = type;
            };

            self.deselect = function () {
                self.selected = undefined;
            };

            var docTypeToResponseObject = function (docType) {
                return docType.toLowerCase() + 's';
            };

            var getFromServer = function (docType, paginationService) {
                var search;
                if (self.searchType === "global") {
                    if (typeof docType !== 'undefined') {
                        search = ElasticService.globalfeaturestore(self.searchFor, docType,
                            paginationService.getLimit(), paginationService.getServerOffset());
                    } else {
                        search = ElasticService.globalSearch(self.searchFor);
                    }
                } else if (self.searchType === "projectCentric") {
                    if (typeof docType !== 'undefined') {
                        search = ElasticService.localFeaturestoreSearch($routeParams.projectID, self.searchFor,
                            docType, paginationService.getLimit(), paginationService.getServerOffset());
                    } else {
                        search = ElasticService.projectSearch($routeParams.projectID, self.searchFor);
                    }
                } else if (self.searchType === "datasetCentric") {
                    search = ElasticService.datasetSearch($routeParams.projectID, $routeParams.datasetName, self.searchFor);
                }

                search.then( function(response) {
                    var data = response.data;
                    if (typeof docType !== 'undefined') {
                        data = data[docTypeToResponseObject(docType)];
                    }
                    paginationService.setContent(data);
                }, function (error) {
                    var errorMsg = (typeof error.data.usrMsg !== 'undefined')? error.data.usrMsg : error.data.errorMsg;
                    growl.error(errorMsg, {title: 'Error', ttl: 5000});
                });
            };

            var searchResultGetFromServer = function (paginationService) {
                //currently not supported for project and dataset search
                //getFromServer('', paginationService);
                throw "Unsupported Operation";
            };

            var featuregroupsSearchResultGetFromServer = function (paginationService) {
                getFromServer('FEATUREGROUP', paginationService);
            };

            var trainingdatasetsSearchResultGetFromServer = function (paginationService) {
                getFromServer('TRAININGDATASET', paginationService);
            };

            var featureSearchResultGetFromServer = function (start, paginationService) {
                getFromServer('FEATURE', paginationService);
            };

            self.projectResult = new PaginationService([], searchResultGetFromServer);
            self.datasetResult = new PaginationService([], searchResultGetFromServer);
            self.otherResults = new PaginationService([], searchResultGetFromServer);
            self.featuregroupsSearchResult =
                new PaginationService([], featuregroupsSearchResultGetFromServer, 0, 20, 1, MAX_IN_MEMORY_ITEMS);
            self.trainingdatasetsSearchResult =
                new PaginationService([], trainingdatasetsSearchResultGetFromServer, 0, 20, 1, MAX_IN_MEMORY_ITEMS);
            self.featureSearchResult =
                new PaginationService([], featureSearchResultGetFromServer, 0, 20, 1, undefined);//get all until
            // features total is fixed

            var sendSearchRequest = function(searches, deferred) {
                var searchHits = {fsResults: fsResults, otherResults: otherResults};
                $q.all(searches).then( function(response) {
                    searchHits.otherResults = response.length > 0? response[0].data : otherResults;
                    searchHits.fsResults = response.length > 1? response[1].data : fsResults;
                    if (response.length > 2) {
                        searchHits.datasetResult.push(response[2].data);//dela results might take longer so maybe
                        // should be moved out
                    }
                    deferred.resolve(searchHits);
                }, function (error) {
                    var errorMsg = (typeof error.data.usrMsg !== 'undefined')? error.data.usrMsg : error.data.errorMsg;
                    deferred.reject(errorMsg);
                });
            };

            var globalSearch = function(deferred) {
                var searches = [];
                if (self.searchScope.toLowerCase() === self.searchScopes[0].toLowerCase()) {
                    searches[0] = ElasticService.globalSearch(self.searchTerm);
                    searches[1] = ElasticService.globalfeaturestore(self.searchTerm, 'ALL', MAX_IN_MEMORY_ITEMS, 0);
                } else if (self.searchScope.toLowerCase() === self.searchScopes[1].toLowerCase()) {
                    searches[0] = ElasticService.globalSearch(self.searchTerm);//project
                    searches[1] = {data: fsResults};
                } else if (self.searchScope.toLowerCase() === self.searchScopes[2].toLowerCase()) {
                    searches[0] = ElasticService.globalSearch(self.searchTerm);//dataset
                    searches[1] = {data: fsResults};
                } else if (self.searchScope.toLowerCase() === self.searchScopes[3].toLowerCase()) {
                    searches[0] = {data: otherResults};
                    searches[1] = ElasticService.globalfeaturestore(self.searchTerm, 'ALL', MAX_IN_MEMORY_ITEMS, 0);
                }
                if ($rootScope.isDelaEnabled) {
                    searches[2] = DelaService.search(self.searchTerm);
                }
                sendSearchRequest(searches, deferred);
            };

            var projectCentricSearch = function(deferred) {
                var searches = [];
                if (self.searchScope.toLowerCase() === self.searchScopes[0].toLowerCase()) {
                    searches[0] = ElasticService.projectSearch($routeParams.projectID, self.searchTerm);
                    searches[1] = ElasticService.localFeaturestoreSearch($routeParams.projectID, self.searchTerm, 'ALL', MAX_IN_MEMORY_ITEMS, 0);
                } else if (self.searchScope.toLowerCase() === self.searchScopes[1].toLowerCase()) {
                    searches[0] = ElasticService.projectSearch($routeParams.projectID, self.searchTerm);
                    searches[1] = {data: fsResults};
                } else if (self.searchScope.toLowerCase() === self.searchScopes[2].toLowerCase()) {
                    searches[0] = {data: otherResults};
                    searches[1] = ElasticService.localFeaturestoreSearch($routeParams.projectID, self.searchTerm, 'ALL', MAX_IN_MEMORY_ITEMS, 0);
                }
                sendSearchRequest(searches, deferred);
            };

            var datasetCentric = function(deferred) {
                var searches = [];
                searches[0] = ElasticService.datasetSearch($routeParams.projectID, $routeParams.datasetName, self.searchTerm);
                sendSearchRequest(searches, deferred);
            };

            var search = function () {
                const deferred = $q.defer();
                self.searchResult = new PaginationService();
                if (self.searchType === "global") {
                    globalSearch(deferred);
                } else if (self.searchType === "projectCentric") {
                    projectCentricSearch(deferred);
                } else if (self.searchType === "datasetCentric") {
                    datasetCentric(deferred);
                }
                return deferred.promise;
            };

            var searchInScope = function () {
                if (self.searchTerm === undefined || self.searchTerm === "" || self.searchTerm === null) {
                    return;
                }
                self.searching = true;
                search().then(function (results) {
                    self.searching = false;
                    self.projectResult.setContent(results.otherResults.projects.items);
                    self.projectResult.setTotal(results.otherResults.projects.count);

                    self.datasetResult.setContent(results.otherResults.datasets.items);
                    self.datasetResult.setTotal(results.otherResults.datasets.count);

                    self.otherResults.setContent(results.otherResults.inodes.items);
                    self.otherResults.setTotal(results.otherResults.inodes.count);

                    self.featuregroupsSearchResult.setContent(results.fsResults.featuregroups);
                    self.featuregroupsSearchResult.setTotal(results.fsResults.featuregroupsTotal);

                    self.trainingdatasetsSearchResult.setContent(results.fsResults.trainingdatasets);
                    self.trainingdatasetsSearchResult.setTotal(results.fsResults.trainingdatasetsTotal);

                    self.featureSearchResult.setContent(results.fsResults.features);
                    self.featureSearchResult.setTotal(results.fsResults.featuresTotal);

                    if (!angular.isUndefined(self.featuregroupsSearchResult.result.content)
                        && self.featuregroupsSearchResult.result.content.length > 0) {
                        $scope.selectedIndex = 0;
                    } else if (!angular.isUndefined(self.trainingdatasetsSearchResult.result.content)
                        && self.trainingdatasetsSearchResult.result.content.length > 0) {
                        $scope.selectedIndex = 1;
                    } else if (!angular.isUndefined(self.featureSearchResult.result.content)
                        && self.featureSearchResult.result.content.length > 0) {
                        $scope.selectedIndex = 2;
                    } else if (!angular.isUndefined(self.projectResult.result.content)
                        && self.projectResult.result.content.length > 0) {
                        $scope.selectedIndex = 3;
                    } else if (!angular.isUndefined(self.datasetResult.result.content)
                        && self.datasetResult.result.content.length > 0) {
                        if (self.searchScope === 'This project') {
                            $scope.selectedIndex = 3;
                        } else {
                            $scope.selectedIndex = 4;
                        }
                    } else if (!angular.isUndefined(self.otherResults.result.content)
                        && self.otherResults.result.content.length > 0) {
                        $scope.selectedIndex = 5;
                    }
                }, function(error) {
                    self.searching = false;
                    growl.error(error, {title: 'Error', ttl: 5000});
                });
            };

            self.searchView = function (page) {
                switch (page) {
                    case "featureGroupTab":
                    case "trainingDatasetTab":
                    case "featureTab":
                        return self.searchScope === 'Everything' || self.searchScope === 'This project' || self.searchScope === 'Feature store';
                    case "projectTab":
                        return self.searchScope === 'Everything' || self.searchScope === 'Projects';
                    case "datasetTab":
                        return self.searchScope === 'Everything' || self.searchScope === 'This project' || self.searchScope === 'Datasets';
                    case "othersTab":
                        return self.searchScope === 'This project' || self.searchScope === 'This dataset';
                }
            };

            self.getReadme = function (dataset) {
                self.gettingReadme = false;
                self.hideReadme = false;
                if (dataset.datasetIId === undefined) {
                    self.readme = undefined;
                    return;
                }
                if (!dataset.publicDataset) {
                    self.gettingReadme = true;
                    HopssiteService.getReadmeByInode(dataset.datasetIId).then(
                        function (success) {
                            var content = success.data.content;
                            self.gettingReadme = false;
                            var conv = new showdown.Converter({parseImgDimensions: true});
                            self.readme = conv.makeHtml(content);
                        }, function (error) {
                            //To hide README from UI
                            self.gettingReadme = false;
                            growl.error(error.data.errorMsg, {
                                title: 'Error retrieving README file',
                                ttl: 5000,
                                referenceId: 3
                            });
                            self.readme = undefined;
                        });
                } else {
                    // dela
                }
            };

            var init = function () {
                if (!angular.isUndefined($routeParams.datasetName)) {
                    self.searchType = "datasetCentric";
                } else if (!angular.isUndefined($routeParams.projectID)) {
                    self.searchType = "projectCentric";
                } else {
                    self.searchType = "global";
                }
                self.searchScopes = searchScopes[self.searchType];
                if (angular.isUndefined(self.searchScope) || self.searchScope === null || self.searchScope === ''
                    || !self.searchScopes.includes(self.searchScope)) {
                    self.searchScope = self.searchScopes[0];
                }
                getTags();
                searchInScope();
            };

            init();
        }]);