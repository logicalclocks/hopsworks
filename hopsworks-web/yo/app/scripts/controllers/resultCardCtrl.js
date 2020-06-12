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
    .controller('ResultCardCtrl', ['$location', '$sce', 'ProjectService', 'ModalService', 'RequestService', 'growl',
    function($location, $sce, ProjectService, ModalService, RequestService, growl) {
        var self = this;

        self.showMatch = function (highlight) {
            return typeof highlight !== 'undefined';
        };

        self.showTitle = function(highlight) {
            if (typeof highlight !== 'undefined') {
                if (Array.isArray(highlight) && highlight.length > 0) {
                    var more = highlight.length > 1? ' ...' : '';
                    var value = highlight[0] + more;// if a feature
                    //if it is a tag
                    if (typeof highlight[0].key !== "undefined" || typeof highlight[0].value !== "undefined") {
                        value = (typeof highlight[0].key !== "undefined"? highlight[0].key + ' ' : '') +
                            (typeof highlight[0].value !== "undefined"? highlight[0].value : '') + more;
                    }
                    return $sce.trustAsHtml('<span class="hw-ellipsis">' + value + '</span>');
                } else if(highlight.size > 0) {
                    var more = highlight.size > 1? ' ...' : '';
                    var xattr = JSON.stringify(highlight.entries().next().value) + more;
                    return $sce.trustAsHtml('<span class="hw-ellipsis">' + xattr + '</span>');
                }
            } else {
                return undefined;
            }
        };

        var requestAccessToDataset = function (datasetIId) {
            var projects;
            ProjectService.query().$promise.then(
                function (success) {
                    projects = success;
                    ModalService.requestAccess('md', {inodeId: datasetIId}, projects);
                }, function (error) {
                    growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000, referenceId: 143});
                });
        };

        var gotoFs = function (itemType, projectId, featurestore, name, version) {
            $location.search('');
            $location.path('/project/' + projectId + '/featurestore');
            $location.search('featurestore', featurestore);
            $location.search(itemType, name);
            $location.search('version', version);
        };

        var gotoFeaturestore = function(itemType, accessProjects, featurestore, name, version, datasetIId) {
            if (accessProjects.length < 1) {
                //request access
                requestAccessToDataset(datasetIId);
            } else if (accessProjects.length === 1) {
                gotoFs(itemType, accessProjects[0].key, featurestore, name, version);
            } else {
                ModalService.selectFromList('sm', 'Select project to go to.', accessProjects).then(
                    function (success) {
                        gotoFs(itemType, success, featurestore, name, version);
                    });
            }
        };

        var requestToJoin = function (projectId) {
            ModalService.confirm('sm', 'Request to join', 'Send join request?').then( function () {
                RequestService.joinRequest({projectId: projectId}).then(
                    function (success) {
                        growl.info("Request sent.", {title: 'Success', ttl: 5000, referenceId: 143});
                    }, function (error) {
                        growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000, referenceId: 143});
                    });
            });
        }

        var gotoProject = function (isMember, projectId) {
            if (isMember) {
                $location.search('');
                $location.path('/project/' + projectId);
            } else {
                requestToJoin(projectId);
            }
        };

        var gotoDataset = function (accessProjects, dataset, datasetIId, parentProjectId, parentProjectName) {
            if (accessProjects.length < 1) {
                //request access
                requestAccessToDataset(datasetIId);
            } else if (accessProjects.length === 1) {
                if (parentProjectId != accessProjects[0].key) {//!== will always return true
                    dataset = parentProjectName + '::' + dataset;
                }
                $location.path('/project/' + accessProjects[0].key + '/datasets/' + dataset);
                $location.search('');
            } else {
                ModalService.selectFromList('sm', 'Select project to go to.', accessProjects).then(
                    function (success) {
                        if (parentProjectId != success) { //!== will always return true
                            dataset = parentProjectName + '::' + dataset;
                        }
                        $location.search('');
                        $location.path('/project/' + success + '/datasets/' + dataset);
                    });
            }
        };

        self.sendAccessRequest = function (item) {
            requestAccessToDataset(item.datasetIId);
        };

        self.sendJoinRequest = function (item) {
            requestToJoin(item.projectId);
        };

        var gotoInode = function (projectId, path) {
            $location.search('');
            $location.path('/project/' + projectId + '/datasets/' + path);
        };

        self.goto = function (itemType, item) {
            switch (itemType) {
                case "featureGroup":
                case "trainingDataset":
                case "features":
                    gotoFeaturestore(itemType, item.accessProjects.entry, item.featurestoreId, item.name, item.version, item.datasetIId);
                    break;
                case "project":
                    gotoProject(item.member, item.projectId);
                    break;
                case "dataset":
                    gotoDataset(item.accessProjects.entry, item.name, item.datasetIId, item.parentProjectId, item.parentProjectName);
                    break;
                case "others":
                    var path = item.path.split(item.parentDatasetName);
                    gotoInode(item.parentProjectId, item.parentDatasetName + path[1]);//need to split path by dataset
                    break;
            }
        };

    }]);