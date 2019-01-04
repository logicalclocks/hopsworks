/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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

/**
 * Controller for the Feature-Info view
 */
angular.module('hopsWorksApp')
    .controller('featureViewInfoCtrl', ['$uibModalInstance', '$scope', 'FeaturestoreService', 'growl', 'projectId', 'feature', 'featurestore',
        function ($uibModalInstance, $scope, FeaturestoreService, growl, projectId, feature, featurestore) {

            /**
             * Initialize controller state
             */
            var self = this;
            self.projectId = projectId;
            self.feature = feature
            self.featurestore = featurestore;
            self.code = ""
            self.table = []

            self.pageSize = 1000; //don't show pagination controls, let the user scroll instead
            self.featureDetailsSortKey = 'property';
            self.featureDetailsreverse = false;

            /**
             * Get the API code to retrieve the feature
             */
            self.getCode = function (feature, featurestore) {
                var codeStr = "from hops import featurestore\n"
                codeStr = codeStr + "featurestore.get_feature(\n"
                codeStr = codeStr + "'" + feature.name + "'"
                codeStr = codeStr + ",\nfeaturestore="
                codeStr = codeStr + "'" + featurestore.featurestoreName + "'"
                codeStr = codeStr + ",\nfeaturegroup="
                codeStr = codeStr + "'" + feature.featuregroup + "'"
                codeStr = codeStr + ",\nfeaturegroup_version="
                codeStr = codeStr + feature.version
                codeStr = codeStr + ")"
                return codeStr
            };

            /**
             * Initialization function
             */
            self.init= function () {
                self.code = self.getCode(self.feature, self.featurestore)
                self.table.push({"property": "Name", "value": self.feature.name})
                self.table.push({"property": "Type", "value": self.feature.type})
                self.table.push({"property": "Version", "value": self.feature.version})
                self.table.push({"property": "Primary", "value": self.feature.primary})
                self.table.push({"property": "Description", "value": self.feature.description})
                self.table.push({"property": "Featurestore", "value": self.featurestore.featurestoreName})
                self.table.push({"property": "Featuregroup", "value": self.feature.featuregroup})
                self.table.push({"property": "API Retrieval Code", "value": self.code})
            };

            /**
             * Closes the modal
             */
            self.close = function () {
                $uibModalInstance.dismiss('cancel');
            };

            self.init()
        }]);

