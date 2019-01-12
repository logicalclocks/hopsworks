(function(angular) {
    'use strict';
    angular.module('hopsWorksApp').service('fileNavigator', [
        '$routeParams', '$route', 'apiMiddleware', 'fileManagerConfig', 'item', 'AirflowService', 
        function ($routeParams, $route, ApiMiddleware, fileManagerConfig, Item, AirflowService) {


        self.projectId = $routeParams.projectID;
//        self.basePath = [];
	    
        var FileNavigator = function() {
            this.apiMiddleware = new ApiMiddleware();
            this.requesting = false;
            this.fileList = [];
//            this.currentPath = this.basePath;
            this.history = [];
            this.error = '';
            this.onRefresh = function() {};
	    this.currentPath = [];
            AirflowService.getSecretPath(self.projectId).then(
		function (success) {
                    var subPath = success.data;
                    var path = (fileManagerConfig.basePath + '/' + subPath  || '').replace(/^\//, '');
                    FileNavigator.currentPath = path.trim() ? path.split('/') : [];

                }, function (error) {
                    console.log("Error getting base path");
            });
	
        };

        // self.init = function() {
	//     AirflowService.getSecretPath(projectId).then(
	// 	function (success) {
        //             var subPath = success.data;
        //             var path = (fileManagerConfig.basePath + '/' + subPath  || '').replace(/^\//, '');
        //             self.basePath = path.trim() ? path.split('/') : [];
	// 	    FileNavigator.prototype.currentPath = self.basePath;
        //         }, function (error) {
        //             console.log("Error getting base path");
        //     });

        // };
        // self.init();

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
            self.requesting = true;
            self.fileList = [];
            return self.list().then(function(data) {
                self.fileList = (data.result || []).map(function(file) {
                    return new Item(file, this.currentPath);
                });
                self.buildTree(path);
                self.onRefresh();
            }).finally(function() {
                self.requesting = false;
            });
		
        };
	    
        FileNavigator.prototype.refresh = function() {
            var self = this;
            if (self.currentPath == undefined || ! self.currentPath.length) {

              AirflowService.getSecretPath(projectId).then(
		function (success) {
                    var subPath = success.data;
                    var path = (fileManagerConfig.basePath + '/' + subPath  || '').replace(/^\//, '');
                    self.currentPath = path.trim() ? path.split('/') : [];
      		    self.actualRefresh();		    
                }, function (error) {
                    console.log("Error getting base path");
              });
            } else {
		self.actualRefresh();
	    }
	    
        };
        
        FileNavigator.prototype.buildTree = function(path) {
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
            !this.history.length && this.history.push({ name: self.basePath[0] || '', nodes: [] });
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
            this.refresh();
        };

        FileNavigator.prototype.upDir = function() {
            if (this.currentPath[0]) {
                this.currentPath = this.currentPath.slice(0, -1);
                this.refresh();
            }
        };

        FileNavigator.prototype.goTo = function(index) {
            this.currentPath = this.currentPath.slice(0, index + 1);
            this.refresh();
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
            return this.currentPath.slice(-1)[0] || '/';
        };

        return FileNavigator;
    }]);
})(angular);
