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
    .controller('FeatureListCtrl', ['$scope', 'ModalService',
        function($scope, ModalService) {
            var self = this;
            self.filter = "";
            self.fgFilter = "";
            self.versionFilter = "";

            self.pageSize = 20;

            self.sortKey = "name";
            self.reverse = false;

            self.filterForm = false;
            self.filteredResult = [];

            self.fromDate = new Date();
            self.toDate = new Date();

            self.init = function() { 
                $scope.$watch('features', function(newValue, oldValue, scope) {
                    var dates = newValue.map(function(f) { return f.date });
                    dates.sort(function(a, b) {
                        return new Date(b) - new Date(a)
                    });
                    self.fromDate = new Date(dates[dates.length - 1]);
                    self.fromDate.setDate(self.fromDate.getDate() - 1);
                    self.toDate = new Date(dates[0]);
                    self.toDate.setDate(self.toDate.getDate() + 1);
                });
            };

            /**
             * Whether to show the filter search advanced filter form in the UI
             */
            self.setFilterForm = function() {
                self.filterForm = !self.filterForm;
            };

            self.showAdd = function() { 
                return (typeof $scope.addFeatureToBasket !== 'undefined');
            };

            self.sortByKey = function(key) {
                self.sortKey = key;
                self.reverse = !self.reverse;
            };

            self.sortFn = function(feature) {
                if (typeof $scope.selectedFeature !== 'undefined') {
                    return undefined;
                } else if (self.sortKey === "fg") {
                    return feature.featuregroup.name;
                } else {
                    return feature[self.sortKey];
                }
            };

            self.init();
        }])
    .filter('filterByFg', ['$filter', function() {
            return function(items, searchText) {
                var filtered = [];
                angular.forEach(items, function(item) {
                    if(item.featuregroup != null) {
                        if (item.featuregroup.name.indexOf(searchText) >= 0 ) {
                            filtered.push(item);
                        }
                    } 
                });
                return filtered;
            };
        }])
   .filter('dateRange', ['$filter', function() {
            return function(items, fromDate, toDate) {
                var filtered = [];
                //var from_date = Date.parse(fromDate);
                // var to_date = Date.parse(toDate);
                angular.forEach(items, function(item) {
                    var createdDate = new Date(item.date)
                    if(createdDate > fromDate && createdDate < toDate) {
                        filtered.push(item);
                    }
                });
                return filtered;
            };
        }]);