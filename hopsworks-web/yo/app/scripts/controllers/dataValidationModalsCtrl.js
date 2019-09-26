/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
    .controller('DataValidationModalsCtrl', ['$uibModalInstance', 'features', 'rule', 'groups', 'growl',
        function ($uibModalInstance, features, rule, groups, growl) {
            self = this;
            self.features = features;
            self.rule = rule;
            self.groups = groups;

            self.addNewRule = function() {
                var check = self.rule.checkInput();
                if (check > 0) {
                    growl.error('Missing required arguments',
                            {title: 'Failed to add predicate', ttl: 2000, referenceId: 'dv_growl'});
                    $uibModalInstance.dismiss('cancel');
                } else {
                    var predicate = self.rule.constructPredicate();
                    growl.info('Added new predicate ' + predicate.predicate,
                            {title: 'Added predicate', ttl: 2000, referenceId: 'dv_growl'})
                    $uibModalInstance.close(predicate);
                }
            }
            self.close = function () {
                $uibModalInstance.dismiss('cancel');
            }
        }
    ]);