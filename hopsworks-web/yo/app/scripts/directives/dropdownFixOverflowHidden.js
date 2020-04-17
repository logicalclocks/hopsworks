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

angular.module('hopsWorksApp').directive("dropdownFixOverflowHidden", function() {
    return {
        restrict: 'A',
        link: function (scope, element) {
            element.on("show.bs.dropdown", function () {
                var $btnDropDown = element.find(".dropdown-toggle");
                var $menu = element.find(".dropdown-menu");
                element.css("position", "static");
                $menu.css({
                    "top": $btnDropDown.position().top + $btnDropDown.innerHeight(),
                    "left": $btnDropDown.position().left + $btnDropDown.innerWidth() - $menu.innerWidth(),
                    "z-index": 1
                });
                $menu.data("open", true);
            });
        }
    };
});