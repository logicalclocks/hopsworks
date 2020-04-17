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
    .controller('FeatureStoreTableCtrl', ['$scope', '$location', '$sce',
        function($scope, $location, $sce) {
            var self = this;
            self.tgState = false;
            self.fromDate = undefined;
            self.toDate = new Date();
            self.created = 'created';
            self.selected = undefined;

            self.goto = function(projectId, featurestore, name, version) {
                $location.path('/project/' + projectId + '/featurestore');
                $location.search('featurestore', featurestore);
                $location.search($scope.name, name);
                $location.search('version', version);
            };

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

            self.pageChange = function (newPageNumber) {
                $scope.paginationService.pageChanged(newPageNumber);
            };

            self.toggleTgstate = function () {
                self.tgState = !self.tgState;
            };
    }]);