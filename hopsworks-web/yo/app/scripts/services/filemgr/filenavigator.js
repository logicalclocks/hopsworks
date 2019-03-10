/* 
 * The MIT License (MIT)
 *
 * Copyright (c) 2013 joni2back
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
(function(angular) {
    'use strict';
    angular.module('hopsWorksApp').service('fileNavigator', [
        '$route', 'apiMiddleware', 'fileManagerConfig', 'item', 'AirflowService', 
        function ($route, ApiMiddleware, fileManagerConfig, Item, AirflowService) {
	    
        var FileNavigator = function(projectId) {
            this.apiMiddleware = new ApiMiddleware();
            this.requesting = false;
            this.fileList = [];
            this.history = [];
            this.error = '';
            this.projectId = projectId;
            this.onRefresh = function() {};
            this.currentPath = [];
            var self = this;
            AirflowService.getSecretPath(this.projectId).then(
		        function (success) {
                    self.currentPath = constructCurrentPath(success.data);
                    console.log("Set current path FileNavigator: " + self.currentPath);
                }, function (error) {
                    console.log("Error getting base path");
            });
            	
        };

        function constructCurrentPath(secretSubPath) {
            var path = (fileManagerConfig.basePath + '/' + secretSubPath  || '').replace(/^\//, '');
            return path.trim() ? path.split('/') : [];
        }

        FileNavigator.prototype.deferredHandler = function(data, deferred, code, defaultMsg) {
            if (!data || typeof data !== 'object') {
                this.error = 'Error %s - Bridge response error, please check the API docs or this ajax response.'.replace('%s', code);
            }
            if (code == 404) {
                this.error = 'Error 404 - Backend bridge is not working, please check the ajax response.';
            }
            if (code == 200) {
                this.error = null;
            }
            if (!this.error && data.result && data.result.error) {
                this.error = data.result.error;
            }
            if (!this.error && data.error) {
                this.error = data.error.message;
            }
            if (!this.error && defaultMsg) {
                this.error = defaultMsg;
            }
            if (this.error) {
                return deferred.reject(data);
            }
            return deferred.resolve(data);
        };

        FileNavigator.prototype.list = function() {
            return this.apiMiddleware.list(this.currentPath, this.deferredHandler.bind(this));
        };

        FileNavigator.prototype.actualRefresh = function() {
            var self = this;
            var path = this.currentPath.join('/');
            console.log("Found current path refresh: " + path);	    
            self.requesting = true;
            self.fileList = [];
            return self.list().then(function(data) {
                self.fileList = (data.result || []).map(function(file) {
                    return new Item(file, self.currentPath);
                });
                self.buildTree(path);
                self.onRefresh();
            }).finally(function() {
                self.requesting = false;
            });
		
        };

        FileNavigator.prototype.refresh = function() {
            var self = this;
            if (self.currentPath == undefined || self.currentPath == null || !self.currentPath.length ) {

              AirflowService.getSecretPath(this.projectId).then(
		            function (success) {
                        self.currentPath = constructCurrentPath(success.data);
                        console.log("Set current path refresh: " + self.currentPath);
      		            self.actualRefresh();		    
                    }, function (error) {
                        console.log("Error getting base path");
                });
            } else {
		        self.actualRefresh();
	        }
        };
        
        FileNavigator.prototype.buildTree = function(path) {
            console.log("Build tree: " + path);
            var flatNodes = [], selectedNode = {};

            function recursive(parent, item, path) {
                var absName = path ? (path + '/' + item.model.name) : item.model.name;
                if (parent.name && parent.name.trim() && path.trim().indexOf(parent.name) !== 0) {
                    parent.nodes = [];
                }
                if (parent.name !== path) {
                    parent.nodes.forEach(function(nd) {
                        recursive(nd, item, path);
                    });
                } else {
                    for (var e in parent.nodes) {
                        if (parent.nodes[e].name === absName) {
                            return;
                        }
                    }
                    parent.nodes.push({item: item, name: absName, nodes: []});
                }
                
                parent.nodes = parent.nodes.sort(function(a, b) {
                    return a.name.toLowerCase() < b.name.toLowerCase() ? -1 : a.name.toLowerCase() === b.name.toLowerCase() ? 0 : 1;
                });
            }

            function flatten(node, array) {
                array.push(node);
                for (var n in node.nodes) {
                    flatten(node.nodes[n], array);
                }
            }

            function findNode(data, path) {
                return data.filter(function (n) {
                    return n.name === path;
                })[0];
            }

            //!this.history.length && this.history.push({name: '', nodes: []});
            !this.history.length && this.history.push({ name: this.currentPath[0] || '', nodes: [] });
            flatten(this.history[0], flatNodes);
            selectedNode = findNode(flatNodes, path);
            selectedNode && (selectedNode.nodes = []);

            for (var o in this.fileList) {
                var item = this.fileList[o];
                item instanceof Item && item.isFolder() && recursive(this.history[0], item, path);
            }
        };

        FileNavigator.prototype.folderClick = function(item) {
            this.currentPath = [];
            if (item && item.isFolder()) {
                this.currentPath = item.model.fullPath().split('/').splice(1);
            }
            this.refresh(this.projectId);
        };

        FileNavigator.prototype.upDir = function() {
            if (this.currentPath[0]) {
                this.currentPath = this.currentPath.slice(0, -1);
                this.refresh(this.projectId);
            }
        };

        FileNavigator.prototype.goTo = function(index) {
            console.log("goTo: " + index);
            console.log("current foldername: " + this.getCurrentFolderName());
            this.currentPath = this.currentPath.slice(0, index + 1);
            this.refresh(this.projectId);
        };

        FileNavigator.prototype.fileNameExists = function(fileName) {
            return this.fileList.find(function(item) {
                return fileName && item.model.name.trim() === fileName.trim();
            });
        };

        FileNavigator.prototype.listHasFolders = function() {
            return this.fileList.find(function(item) {
                return item.model.type === 'dir';
            });
        };

        FileNavigator.prototype.getCurrentFolderName = function() {
            var path = this.currentPath.slice(-1)[0] || '/';
            console.log("get current path: " + path);
            return path;
        };

        return FileNavigator;
    }]);
})(angular);
