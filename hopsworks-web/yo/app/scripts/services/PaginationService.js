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

angular.module('hopsWorksApp').service('PaginationService', [
    function() {
        var Pagination = function (totalItems, itemsPerPage, currentPage, maxPagesInMem) {
            this.totalItems = typeof totalItems !== 'undefined'? totalItems : 0;
            this.itemsPerPage = typeof itemsPerPage !== 'undefined'? itemsPerPage : 20;
            this.currentPage = typeof currentPage !== 'undefined'? currentPage : 1;
            this.serverCurrentPage = 1;
            this.maxPagesInMem = maxPagesInMem;
            if (typeof this.maxPagesInMem !== 'undefined') {
                console.assert(this.itemsPerPage % 10 === 0 && this.itemsPerPage <= this.maxPagesInMem, "items per page" +
                    " should be <= " + this.maxPagesInMem + " and a multiple of 10.");
            }
        };

        var Result = function (content) {
            this.content = typeof content !== 'undefined'? content : [];
            this.paginatedContent = [];
        };

        var PaginationService = function(result, getFromServerFn, totalItems, itemsPerPage, currentPage, maxPagesInMem) {
            this.result = new Result(result);
            this.pagination = new Pagination(totalItems, itemsPerPage, currentPage, maxPagesInMem);
            this.setPaginated();
            this.getFromServer = getFromServerFn;
        };

        PaginationService.prototype.getOffset = function () {
            return (this.pagination.currentPage - 1) * this.pagination.itemsPerPage;
        };

        PaginationService.prototype.getServerOffset = function () {
            return (this.pagination.serverCurrentPage - 1) * this.pagination.maxPagesInMem;
        };

        PaginationService.prototype.getLimit = function () {
            return this.pagination.maxPagesInMem;
        };

        PaginationService.prototype.getItemsPerPage = function () {
            return this.pagination.itemsPerPage;
        };

        PaginationService.prototype.setContent = function (content) {
            this.result.content = content;
            this.setPaginated();
        };

        PaginationService.prototype.setTotal = function (total) {
            this.pagination.totalItems = total;
        };

        PaginationService.prototype.updateServerCurrentPage = function () {
            var start = this.getOffset();
            var serverLimit = this.getLimit();
            var end = start + this.getItemsPerPage();
            if (end > serverLimit) {
                this.pagination.serverCurrentPage++;
            } else if (start < this.getServerOffset()) {
                this.pagination.serverCurrentPage--;
            }
        };

        PaginationService.prototype.shouldGetFromServer = function () {
            var start = this.getOffset();
            var serverLimit = this.getLimit();
            var end = start + this.getItemsPerPage();
            return typeof serverLimit !== 'undefined' && (end > serverLimit || start < this.getServerOffset());
        };

        PaginationService.prototype.setPaginated = function () {
            var start = this.getOffset();
            var serverLimit = this.getLimit();
            if (serverLimit !== 'undefined' && start >= serverLimit) {
                start %= serverLimit;
            }
            var end = start + this.getItemsPerPage();
            this.result.paginatedContent = typeof this.result.content !== "undefined" && this.result.content.length > this.pagination.itemsPerPage ? this.result.content.slice(start, end) : this.result.content;
        };

        PaginationService.prototype.pageChanged = function (newPageNumber) {
            this.pagination.currentPage = newPageNumber;
            if (this.shouldGetFromServer() && typeof this.getFromServer === 'function') {
                this.updateServerCurrentPage();
                this.getFromServer(this);
            } else {
                this.setPaginated();
            }
        };

        PaginationService.prototype.numPages = function () {
            return Math.ceil(this.pagination.totalItems / this.pagination.itemsPerPage);
        };

        return PaginationService;
    }
]);