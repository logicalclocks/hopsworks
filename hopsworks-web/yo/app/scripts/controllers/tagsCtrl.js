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
    .controller('TagsCtrl', ['FeaturestoreService', 'growl',
        function(FeaturestoreService, growl) {

            var self = this;

            self.selectedTag = null;
            self.selectedValue = "";
            self.allTags = [];

            FeaturestoreService.getTags('?sort_by=name:asc').then(
                function(success) {
                    if(success.data.items) {
                        self.allTags = [];
                        for(var i = 0; i < success.data.items.length; i++) {
                          self.allTags.push(success.data.items[i].name);
                          self.selectedTag = self.allTags[0];
                        }
                    }
                },
                function(error) {
                    if(error.status !== 404) {
                        if (typeof error.data.usrMsg !== 'undefined') {
                            growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                        } else {
                            growl.error("", {title: error.data.errorMsg, ttl: 8000});
                        }
                    }
                });

            self.sizeOf = function(tags) {
                return Object.keys(tags).length;
            };

        }])