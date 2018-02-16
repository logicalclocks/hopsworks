/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

(function e(t,n,r){function s(o,u){if(!n[o]){if(!t[o]){var a=typeof require=="function"&&require;if(!u&&a)return a(o,!0);if(i)return i(o,!0);var f=new Error("Cannot find module '"+o+"'");throw f.code="MODULE_NOT_FOUND",f}var l=n[o]={exports:{}};t[o][0].call(l.exports,function(e){var n=t[o][1][e];return s(n?n:e)},l,l.exports,e,t,n,r)}return n[o].exports}var i=typeof require=="function"&&require;for(var o=0;o<r.length;o++)s(r[o]);return s})({1:[function(require,module,exports){
function addBlankNode(ele){
    var cy = ele.cy();
    var source = ele.source();
    var target = ele.target();
    var node = cy.add(cy.defaults.blankNodeOpts());
  var edges = [
    { data: { id: cy.uuid(), source: source.id(), target: node.id() } },
    { data: { id: cy.uuid(), source: node.id(), target: target.id() } }
  ];
    cy.add(edges);
    cy.remove(ele);
}
function addDecisionSon(ele){
    var cy = ele.cy();
    var source = ele;
    var target = cy.$('#' + ele.data("targetNodeId"));
    var node = cy.add(cy.defaults.blankNodeOpts());

  var edges = [
    { data: { id: cy.uuid(), source: source.id(), target: node.id(), type: "decision-edge" }},
    { data: { id: cy.uuid(), source: node.id(), target: target.id() } }
  ];
    cy.add(edges);
}
function addForkSon(ele){
    var cy = ele.cy();
    var source = ele;

    var target = cy.$('#' + ele.data("joinNodeId"));
    var node = cy.add(cy.defaults.blankNodeOpts());

  var edges = [
    { data: { id: cy.uuid(), source: source.id(), target: node.id() } },
    { data: { id: cy.uuid(), source: node.id(), target: target.id() } }
  ];
    cy.add(edges);
}

function addFork(ele){
  var cy = ele.cy()
  var inEdge = ele.incomers()[0];
  var outEdge = ele.outgoers()[0];

  var source = inEdge.source()
  var target = outEdge.target()
  var join = cy.add(cy.defaults.joinNodeOpts())
  var forkOptions = cy.defaults.forkNodeOpts()
  forkOptions.data["joinNodeId"] = join.id()
  var fork = cy.add(forkOptions)

  var node = cy.add(cy.defaults.blankNodeOpts())

  var edges = [
    { data: { id: cy.uuid(), source: source.id(), target: fork.id() } },
    { data: { id: cy.uuid(), source: fork.id(), target: ele.id() } },
    { data: { id: cy.uuid(), source: fork.id(), target: node.id() } },
    { data: { id: cy.uuid(), source: node.id(), target: join.id() } },
    { data: { id: cy.uuid(), source: ele.id(), target: join.id() } },
    { data: { id: cy.uuid(), source: join.id(), target: target.id() } }
  ]
  cy.remove(inEdge)
  cy.remove(outEdge)
  cy.add(edges)
}
function addDecision(ele){
  var cy = ele.cy()
  var inEdge = ele.incomers()[0];
  var outEdge = ele.outgoers()[0];

  var source = inEdge.source()
  var target = outEdge.target()

  cy.remove(inEdge)
  cy.remove(outEdge)

  var node = cy.add(cy.defaults.blankNodeOpts())
  var decisionOptions = cy.nodeOptions["decisionNode"]()
  decisionOptions.data["targetNodeId"] = target.id()
  var decision = cy.add(decisionOptions)

  var edges = [
    { data: { id: cy.uuid(), source: source.id(), target: decision.id() } },
    { data: { id: cy.uuid(), source: decision.id(), target: ele.id(), type: "decision-edge" }},
    { data: { id: cy.uuid(), source: decision.id(), target: node.id(), type: "decision-edge" }},
    { data: { id: cy.uuid(), source: decision.id(), target: target.id(), type: "default-decision-edge" }, classes: "default-decision-edge"},
    { data: { id: cy.uuid(), source: node.id(), target: target.id() } },
    { data: { id: cy.uuid(), source: ele.id(), target: target.id() } }
  ]
  cy.add(edges)
}

function deleteNode(ele){
  var cy = ele.cy()
  var inEdge = ele.incomers().edges()[0];
  var outEdge = ele.outgoers().edges()[0];

  var source = inEdge.source()
  var target = outEdge.target()
  sourceType = source.data("type")
  cy.remove(ele)
  cy.remove(outEdge)
  switch (sourceType) {
    case "fork-node":
      childCount = source.outgoers().nodes().length
      if(childCount == 0){
        var joinNode = cy.$('#' + source.data("joinNodeId"))
        if(target.id() == joinNode.id()){
          var newInEdge = source.incomers()[0];
          var newOutEdge = joinNode.outgoers()[0];

          var newTarget = newOutEdge.target()

          cy.add({data:{source: newInEdge.source().id(), target: newTarget.id()}})
          cy.remove(newInEdge)

          cy.remove(source)
          cy.remove(target)
          cy.remove(newOutEdge)
          cy.remove(inEdge)
        }else{
          cy.add({data:{source: inEdge.source().id(), target: target.id()}})
          cy.remove(inEdge)
        }

      }

      break;

      case "decision-node":

        childCount = source.outgoers("[class != 'default-decision-edge']").edges().length
        if(childCount == 1){
          var targetNode = cy.$('#' + source.data("targetNodeId"))
          if(target.id() == targetNode.id()){
            var e = source.incomers().edges()[0]
            // e.move({target: targetNode.id()})
            cy.add({data:{id: cy.uuid(), source: e.source().id(), target: targetNode.id()}})
            cy.remove(e)

            cy.remove(source.outgoers().edges())
            cy.remove(inEdge)
            cy.remove(source)

          }else{
            // inEdge.move({target: target.id()})
            cy.add({data:{id: cy.uuid(), source: inEdge.source().id(), target: target.id()}})
            cy.remove(inEdge)
          }
        }

        break;
    default:
      cy.add({data:{id: cy.uuid(), source: inEdge.source().id(), target: target.id()}})
      cy.remove(inEdge)
  }

}

var init = function(cy){
  cy.cxtmenu({
    selector: 'edge[^type]',
    openMenuEvents: 'cxttapstart',
    commands: [
      {
        content: '<span class="fa fa-circle-o fa-2x"></span>',
        select: function(ele){
          var sourceType = ele.source().data("type")
          switch(sourceType) {
              default:
                  addBlankNode(ele)
          }
        }
      }
    ]
  });

  cy.cxtmenu({
    selector: 'node.options-node',
    openMenuEvents: 'cxttapstart',
    commands: [
      {
        content: '<span class="fa fa-question-circle fa-2x"></span>',
        select: function(ele){
          addDecision(ele)
        }
      },
      {
        content: '<span class="fa fa-cogs fa-2x"></span>',
        select: function(ele){
          addFork(ele)
        }
      },
      {
        content: '<span class="fa fa-trash-o fa-2x"></span>',
        select: function(ele){
          deleteNode(ele)
        }
      }
    ]
  });

  cy.cxtmenu({
    selector: 'node.action-node',
    openMenuEvents: 'cxttapstart',
    commands: [
      {
        content: '<span class="fa fa-circle-o fa-2x"></span>',
        select: function(ele){
          var sourceType = ele.data("type")
          switch(sourceType) {
              case "fork-node":
                addForkSon(ele)
                break;
              case "decision-node":
                addDecisionSon(ele)
                break;
              // default:
              //   addForkSon(ele)
          }
        }
      }
    ]
  });
}
module.exports = {
  init : init
}

},{}],2:[function(require,module,exports){
module.exports = function(target, dir, cb) {
  // var root = $('<div></div>');
  var root = $('#hdfsViewer')

  var addFolder = $('<button type="button" class="btn btn-default">Add Folder</button>')
  root.find('.modal-header').html(addFolder)
  addFolder.on('click', function(){
    var inst = $.jstree.reference('#')
    var selected = inst.get_selected(true)[0]

    inst.create_node(selected, {data:{dir: true}, icon: 'fa fa-folder'}, "last", function (new_node) {
      var inst = $.jstree.reference('#')
      var selected = inst.get_selected(true)[0]
      if(!selected){return false}
      if(!selected.data.dir){return false}
      setTimeout(function () { inst.edit(new_node, "New Node",function(n){
        var parent = $.jstree.reference('#').get_node(n.parent)
        var path = parent.data.relPath+ "/" + n.text + "/"
        n.data['fullPath'] = parent.data.fullPath+ "/" + n.text + "/"
        n.data['relPath'] = path

        $.post(getApiPath() + "/project/:project_id/dataset", JSON.stringify({name: path, description: "", searchable: true})).retry({times:3, timeout:500})
      }); },0);
    });
  })
  var content = root.find('.modal-body')
  content.html('')
  content.attr('class', 'modal-body')
  content.jstree({
    'core' : {
      check_callback : true,
      multiple: false,
      data : function (node, callback) {
        var path = (node.data) ? node.data.relPath : ""
        $.get(getApiPath() + '/project/:project_id/dataset' + path, function(data){
          var tree = data.map(function(i){
            var path = i.path.split('/')
            path = '/' + path.splice(3, path.length).join('/')
            var parent = (path.match(/\//g).length === 1) ? "#" : i.parentId
            var n = {id:i.id, text: i.name, data:{relPath: path, fullPath: i.path, dir: i.dir}, children: i.dir, parent: parent}
            if(i.template != -1){
              n['data']['templateId'] = i.template
            }
            n['icon'] = (i.dir ? 'fa fa-folder' : 'fa fa-file')
            return n
          })
          callback(tree)
        })
      }
    }
  });

  content.on("select_node.jstree", function (e, data) {
    if(dir || data.node.data.dir == dir){
      if(cb){
        cb($(target), data.node)
      }else{
        $(target).val(data.node.data.fullPath).change()
      }
    }
  });

  root.modal()
}

},{}],3:[function(require,module,exports){
(function(_window) {
  'use strict';

  var recase = Recase.create({})

  $.ajaxPrefilter(function( options, originalOptions, jqXHR ) {
    if(options.url.indexOf(":workflow_id") > -1){
      options.dataTypes.push('json')
      options.contentType = "application/json"
      if( _window.location.hash.match(/workflows\/(\d+)/)){
        options.url = options.url.replace(":workflow_id", _window.location.hash.match(/workflows\/(\d+)/)[1])
      }else{
        options.url = options.url.replace(":workflow_id", _window.location.pathname.match(/workflows\/(\d+)/)[1])
      }
    }

    if(options.url.indexOf(":project_id") > -1){
      if( _window.location.hash.match(/project\/(\d+)/)){
        options.url = options.url.replace(":project_id", _window.location.hash.match(/project\/(\d+)/)[1])
      }else{
        options.url = options.url.replace(":project_id", _window.location.pathname.match(/project\/(\d+)/)[1])
      }
    }

    if(options.url.indexOf(":execution_id") > -1){
      if( _window.location.hash.match(/wfexecutions\/(\d+)/)){
        options.url = options.url.replace(":execution_id", _window.location.hash.match(/wfexecutions\/(\d+)/)[1])
      }else{
        options.url = options.url.replace(":execution_id", _window.location.pathname.match(/wfexecutions\/(\d+)/)[1])
      }
    }
    if(options.url.indexOf(":job_id") > -1){
      if( _window.location.hash.match(/jobs\/((\w|-)+)/)){
        options.url = options.url.replace(":job_id", _window.location.hash.match(/jobs\/((\w|-)+)/)[1])
      }else{
        options.url = options.url.replace(":job_id", _window.location.hash.match(/jobs\/((\w|-)+)/)[1])
      }
    }
});

  jQuery.each( [ "put", "delete" ], function( i, method ) {
    jQuery[ method ] = function( url, data, callback, type ) {
      if ( jQuery.isFunction( data ) ) {
        type = type || callback;
        callback = data;
        data = undefined;
      }

      return jQuery.ajax({
        url: url,
        type: method,
        dataType: type,
        data: data,
        success: callback
      });
    };
  });

  String.prototype.camelCase = function () {
    return this.replace(/-([a-z])/g, function (g) { return g[1].toUpperCase() })
  }
  String.prototype.titleCase = function() {
    return this.charAt(0).toUpperCase() + this.slice(1)
  }

  var ajaxEdgeDataSetup = function(d, update){
    var data = jQuery.extend(true, {}, d);
    data["sourceId"] = data.source
    data["targetId"] = data.target
    delete data.source
    delete data.target
    if(update){
      delete data.id
    }
    return data
  }

  var ajaxNodeDataSetup = function(d){
    var data = jQuery.extend(true, {}, d);
    delete data.id
    return data
  }

  function ready(event){
    var cy = _window.cy

    cy.on('add', '*', function(e){
      var ele = e.cyTarget
      if(ele.isNode()){
        var data = ele.data()
        data['data'] = jQuery.extend(true, {}, data)
        $.post(getApiPath() + "/project/:project_id/workflows/:workflow_id/nodes", JSON.stringify(data)).retry({times:3, timeout:500})
      }

      if(ele.isEdge()){
        var data = ajaxEdgeDataSetup(ele.data(), false)
        $.post(getApiPath() + "/project/:project_id/workflows/:workflow_id/edges", JSON.stringify(data)).retry({times:3, timeout:500})
      }

    });
    cy.on('data', '*', function(e){
      var ele = e.cyTarget

      if(ele.isNode()){
        var data = ajaxNodeDataSetup(ele.data(), true)
        if(!jQuery.isEmptyObject(data)){
          data['data'] = jQuery.extend(true, {}, ajaxNodeDataSetup(ele.data(), true))
          $.put(getApiPath() + "/project/:project_id/workflows/:workflow_id/nodes/"+ele.id(), JSON.stringify(data)).retry({times:3, timeout:500})
        }
      }

      if(ele.isEdge()){
        var data = ajaxEdgeDataSetup(ele.data())
        $.put(getApiPath() + "/project/:project_id/workflows/:workflow_id/edges/"+ele.id(), JSON.stringify(data)).retry({times:3, timeout:500})
      }
    });

    cy.on('remove', '*', function(e){
      var ele = e.cyTarget
      if(ele.isNode()){
        $.delete(getApiPath() + "/project/:project_id/workflows/:workflow_id/nodes/"+ele.id()).retry({times:3, timeout:500})
      }

      if(ele.isEdge()){
        $.delete(getApiPath() + "/project/:project_id/workflows/:workflow_id/edges/"+ele.id()).retry({times:3, timeout:500})
      }

    });
    cy.tools.init()
  }

  var layoutOpts = function (){
    var cy = _window.cy
    return {
      animate: false,
      name: 'dagre',
      // avoidOverlap: true,
      // flow: { axis: 'y', minSeparation: 10 },
      fit: true,
      rankDir: "TB",
      stop: function(){cy.nodes().lock()}
    }
  }

  var loadDefaults = function(cy){
    cy.uuid = uuid.v4
    cy.nodeOptions = {}
    var defaults = require("./node_defaults.js")
    cy.defaults = defaults
    cy.nodeOptions["blankNode"] = defaults.blankNodeOpts
    cy.nodeOptions["joinNode"] = defaults.joinNodeOpts

    cy.nodeOptions["forkNode"] = defaults.forkNodeOpts
    cy.nodeOptions["rootNode"] = defaults.rootNodeOpts
    cy.nodeOptions["endNode"] = defaults.endNodeOpts
    cy.decisionNode = defaults.decisionNode
    cy.nodeOptions["decisionNode"] = cy.decisionNode.opts
    cy.emailNode = require("./nodes/email.js")
    cy.nodeOptions["emailNode"] = cy.emailNode.opts
    cy.sparkCustomNode = require("./nodes/spark-custom.js")
    cy.nodeOptions["sparkCustomNode"] = cy.sparkCustomNode.opts
  }
  var init = function(){

    var cy = _window.cy = cytoscape({
        container: $('#workflow #cy'),
        style: require('./style.js'),
    });

      var cyToolbar = cy.tools = require("./toolbar.js");
      loadDefaults(_window.cy);
      cy.viewDatasets = require("./hdfsViewer.js");
      $.jstree.defaults.core.check_callback = true;
      cy.emailNode.init(cy.tools);
    cy.sparkCustomNode.init(cy.tools);


    require("./cxtmenu.js").init(cy);
    $('#cy').cyNavigator({container: '#navigator'});

    cy.on('add remove', 'edge', function(e){
      cy.nodes().unlock()
      cy.layout( layoutOpts() )
    });

    cy.on('tap', function(e){
      cy.$('node.selected').removeClass('selected')
      $('#options').empty()
    });

    $('.tool-item').on('click', function(e){
      $('#options').empty()
    });
    cy.on('tap', 'node.form-node', function(e){
      var node = e.cyTarget
      cy.$('node.selected').removeClass('selected')
      node.addClass("selected")
      $('#options').empty()
      var form = cy[node.data("type").camelCase()].form(node)
      $('#options').html(form)
    });

    $.get(getApiPath() + "/project/:project_id/workflows/:workflow_id", function(data){
      if(data.nodes.length == 0){
          var ele0 = defaults.blankNodeOpts();
        var nodes = [
          defaults.rootNodeOpts(),
          ele0,
          defaults.endNodeOpts()
        ];
        var edges =[
          { data: { id: cy.uuid(), source: "root", target: ele0.data.id } },
          { data: { id: cy.uuid(), source: ele0.data.id, target: "end" } }
        ];
          cy.ready(ready);
          cy.add(nodes);
          cy.add(edges);
      }else{
          var nodes = [];
          var edges = [];
        data.nodes.forEach(function(node){
            var opts = cy.nodeOptions[node.type.camelCase()]();
            jQuery.extend(opts.data, node.data);
            opts.data["id"] = node.id;
            nodes.push(opts);
        });

        data.edges.forEach(function(edge){
            var e = {data:{id: edge.id, source: edge.sourceId, target: edge.targetId}};
            if(edge.type){e.data['type'] = edge.type};
            edges.push(e);
        });
          cy.add(nodes);
          cy.add(edges);
          cy.ready(ready);
      }
    });

  }

  var drawAction = function(actions, i){
    if(actions.length === i) return
    var action = actions[i]
    setTimeout(function () {
      if(action.node){
        var node = cy.$('#' + action.node.id)
        switch (action.status.toLowerCase()) {
          case "failed":
          case "error":
            node.addClass("error")
            if(action && (action.errorMessage || action.errorCode)){
              node.qtip({
                  content: action.errorCode + ': ' + action.errorMessage
              });
            }else{
              var error = $.grep(actions, function(o){ return o.nodeId === action.transition})[0]
              if(error && (error.errorMessage || error.errorCode)){
                node.qtip({
                    content: error.errorCode + ': ' + error.errorMessage
                });
              }
            }
            break;

          case "killed":
            node.addClass("killed")
            break;
          case "ok":
          case "done":
            node.addClass("completed")
            setTimeout(function () {
              if(action.transition === "*"){
                node.outgoers('edge').addClass("completed")
              }else{
                if(!action.transition) return;
                var transition = (action.transition.indexOf("_") > 0) ? action.transition.split("_")[1] : action.transition
                node.edgesTo("#" + transition).addClass("completed")
              }
            }, 50);
            break;
          case "prep":
          case "running":
            node.addClass("current")
            break;

          default:
          break;
        }
      }
      drawAction(actions, ++i)
    }, 200);
  }
  var pollData = function(){
    setTimeout(function () {
      $.get(getApiPath() + "/project/:project_id/workflows/:workflow_id/executions/:execution_id", function(data){
        drawAction(data.actions, 0)
        if(data.status.toLowerCase === "running") pollData()
      })
    }, 10000);
  }
  var image = function(target){
    if(!(target instanceof jQuery)){
      target = $(target)
    }
    var cy = _window.cy = cytoscape({
        container: target,
        style: require('./style.js'),
    });
    if(target.find('#navigator').length == 0){
      var nav = $('<div id="navigator" class="cytoscape-navigator mouseover-view"></div>')
      target.append(nav)
    }
    $('#image').cyNavigator({container: '#navigator'})
    loadDefaults(cy)

    $.get(getApiPath() + "/project/:project_id/workflows/:workflow_id/executions/:execution_id", function(success){
      var data = success.snapshot
      debugger
      if(data.nodes.length == 0) return
      var cy = _window.cy
      var nodes = []
      var edges = []
      data.nodes.forEach(function(node){
        var opts = cy.nodeOptions[node.type.camelCase()]()
        jQuery.extend(opts.data, node.data)
        opts.data["id"] = node.id
        opts['selectable'] = false
        opts['grabbable'] = false
        nodes.push(opts)
      })

      data.edges.forEach(function(edge){
        var e = {data:{id: edge.id, source: edge.sourceId, target: edge.targetId}}
        e['selectable'] = false
        e['grabbable'] = false
        if(edge.type){e.data['type'] = edge.type}
        edges.push(e)
      })
      cy.add(nodes)
      cy.add(edges)
      cy.ready(function(){
        cy.layout( layoutOpts() )
        $.get(getApiPath() + "/project/:project_id/workflows/:workflow_id/executions/:execution_id/jobs/:job_id", function(data){
          drawAction(data.actions, 0)
          if(data.status.toLowerCase === "running") pollData()
        })
      })

    })

  }
  _window.workflows = {init : init,
                       image: image}
})('undefined' !== typeof window ? window : null);

},{"./cxtmenu.js":1,"./hdfsViewer.js":2,"./node_defaults.js":4,"./nodes/email.js":5,"./nodes/spark-custom.js":6,"./style.js":7,"./toolbar.js":8}],4:[function(require,module,exports){

var forkNodeOpts = function() {
  return {
    group: 'nodes',
    data: {
        id: cy.uuid(),
        name: "Fork",
        type: "fork-node"
    },
   classes: "fork-node tool-node deletable action-node"
 }
}

var decisionNodeOpts = function() {
  return {
    group: 'nodes',
    data: {
        id: cy.uuid(),
        name: "Decision",
        type: "decision-node"
    },
   classes: "decision-node tool-node deletable action-node form-node"
 }
}
var joinNodeOpts = function() {
  return {
    group: 'nodes',
    data: {
        id: cy.uuid(),
        name: "Join",
        type: "join-node"
    },
   classes: "join-node tool-node deletable"
 }
}

var blankNodeOpts = function() {
  return {
    group: 'nodes',
    data: {
        id: cy.uuid(),
        name: "Blank",
        type: "blank-node",
        mutable: true
    },
   classes: "blank-node tool-node deletable options-node"
 }
}

var rootNodeOpts = function(){
  return {
    data: {
      id: 'root',
      undeletable: true,
      type: "root-node"
   },
   classes: "root-node tool-node"
  }
}

var endNodeOpts = function(){
  return {
    data: {
      id: 'end',
      undeletable: true,
      type: "end-node"
   },
   classes: "end-node tool-node"
  }
}

var textInput = function(node, type, text, key, cb){
  var root = $('<div></div>').addClass("form-group")
  root.append($('<label>' + text + ':</label>').addClass("col-sm-3 control-label"))
  var input = $('<input />').attr({type: type, placeholder: text, id: key}).addClass("form-control").val(node.data(key))
  input.on('input', function() {
    if(cb){
      cb($(this).val())
    }else{
        node.data(key, $(this).val())
    }
  })
  root.append($('<div></div>').addClass("col-sm-9").append(input))
  return root

}

var hdfsInput = function(node, dir, text, key, cb){
  var root = $('<div></div>').addClass("form-group")
  root.append($('<label>' + text + ':</label>').addClass("col-sm-3 control-label"))
  var input = $('<input />').attr({type: 'text', placeholder: text, disabled: ''}).addClass("form-control").val(node.data(key))
  var button = $('<div class="input-group-addon fa fa-folder"></div>')
  button.on('click', function(){
    node.cy().viewDatasets(input, dir, cb)
  })
  input.on('change', function() {
    node.data(key, $(this).val())
  })
  root.append($('<div></div>').addClass(" col-sm-9").append($('<div></div>').addClass("input-group").append(input).append(button)))
  return root
}

var decisionNodeForm = function(node){
  var form = $('<form class="form-horizontal"></form>')
  $.each(node.outgoers("edge") ,function(i, edge){
    var ele = edge.target()
    var shortId = (ele.id().length > 8) ? ele.id().substr(ele.id().length - 5) : ele.id()

    if(edge.data("type") !== "default-decision-edge"){
      var input = textInput(ele, "text", shortId, "decision")
      form.append(input)
    //   var parentDiv = input.find('input').parent()
    //   var checked = node.data("default") || false
    //   var checkDefault = $("<input type='checkbox'>")
    //   checkDefault.attr('value', true)
    //   if(checked) checkDefault.attr('checked', checked)
    //   var textNode = $('<label>Default Edge:</label>')
    //   parentDiv.append(textNode)
    //   parentDiv.append(checkDefault)
    //   checkDefault.on('click',{id: node.id()}, function(e){
    //     var node = cy.$('#' + e.data.id)
    //     var checked = $(e.target).is(":checked")
    //     node.data("default", checked)
    //   })
    }

  })

  return form
}
module.exports = {
    blankNodeOpts: blankNodeOpts,
    joinNodeOpts: joinNodeOpts,
    decisionNode: {opts: decisionNodeOpts, form: decisionNodeForm},
    forkNodeOpts: forkNodeOpts,
    rootNodeOpts: rootNodeOpts,
    endNodeOpts: endNodeOpts,
    textInput: textInput,
    hdfsInput: hdfsInput
}

},{}],5:[function(require,module,exports){
var defaults = {}
var init = function(toolbar){
  toolbar.tools.push(tool)
  defaults.addObject = toolbar.addObject
}

var opts = function() {
  return {
    group: 'nodes',
    data: {
      text: 'Email',
      type: 'email-node',
      mutable: true,
      to: "",
      from: "",
      body: ""
    },
   classes: "email-node tool-node deletable options-node form-node"
 }
}

var tool =[
  {
      icon: 'fa fa-envelope-o fa-2x',
      event: ['tap'],
      selector: 'node',
      options: {
          clazz: opts().classes,
          data: opts().data
      },
      bubbleToCore: false,
      tooltip: 'Email Node',
      action: [addEmailToGraph]
  }
]

function addEmailToGraph(e) {
    defaults.addObject(e, addEmailToGraph);
}

var formValues = {
    to:{
      type: "email",
      value:""
    },
    from:{
      type: "email",
      value:""
    },
    cc:{
      type: "email",
      value:""
    },
    subject:{
      type: "text",
      value:""
    },
    body:{
      type: "textarea",
      value:""
    }
}
function form(node){
  var form =$('<form class="form-horizontal"></form>')
  $.each(formValues ,function(key, value){
    form.append(node.cy().defaults.textInput(node, value.type, key.titleCase(), key))
  })
  return form
}
module.exports = {
  init: init,
  opts: opts,
  form: form
}

},{}],6:[function(require,module,exports){
var defaults = {}

var init = function(toolbar){
  toolbar.tools.push(tool)
  defaults.addObject = toolbar.addObject
}

var opts = function() {
  var mainClassId = "jar-main-class" + new Date().getTime().toString()
  return {
    group: 'nodes',
    data: {
      text: 'Spark',
      type: 'spark-custom-node',
      mutable: true,
      jar: "",
      mainClass: "",
      name: "",
      sparkOptions:"",
      arguments: [],
      jobXmls: [],
      configurations: [],
      mkDirs: [],
      rmDirs: [],
      jarINodeId: null,
      metaKey: null
    },
   classes: "spark-custom-node tool-node deletable options-node form-node"
 }
}

var tool = [{
    icon: 'fa fa-star-o fa-2x',
    event: ['tap'],
    selector: 'node',
    options: {
        clazz: opts().classes,
        data: opts().data
    },
    bubbleToCore: false,
    tooltip: 'Spark Node',
    action: [addSparkCustomToGraph]
}]

function addSparkCustomToGraph(e) {
    defaults.addObject(e, addSparkCustomToGraph);
}

function form(node){


  var createArg = function (r, value) {
    r.append(multiInput(node, "Argument", value, false, true, function(){
      var new_args = $('#arguments input').map(function(i, elem){ return $(elem).val()}).get().filter(function(e){ return e != "" })
      node.data('arguments', new_args)
    }))
  }

  var createXml = function (r, value){
    r.append(multiInput(node, "Xml File", value, true, false, function(){
      var xml_files = $('#xml_files input').map(function(i, elem){ return $(elem).val()}).get().filter(function(e){ return e != "" })
      node.data('jobXmls', xml_files)
    }))
  }

  var createMkDir = function (r, value){
    r.append(multiInput(node, "Add Directory", value, true, true, function(){
      var add_dirs = $('#mk_dirs input').map(function(i, elem){ return $(elem).val()}).get().filter(function(e){ return e != "" })
      node.data('mkDirs', add_dirs)
    }))
  }

  var createRmDir = function (r, value){
    r.append(multiInput(node, "Remove Directory", value, true, true, function(){
      var remove_dirs = $('#rm_dirs input').map(function(i, elem){ return $(elem).val()}).get().filter(function(e){ return e != "" })
      node.data('rmDirs', remove_dirs)
    }))
  }

  var createConfiguration = function (r, value){
    var nval = (value) ? value[0] : ""
    var vval = (value) ? value[1] : ""
    var root = $('<div></div>').addClass("form-group")
    root.append($('<label>Configuration:</label>').addClass("col-sm-3 control-label"))
    var name = $('<input />').attr({type: 'text', placeholder: "Name"}).addClass("form-control").val(nval)
    root.append($('<div></div>').addClass("col-sm-4").append(name))

    var value = $('<input />').attr({type: 'text', placeholder: "Value"}).addClass("form-control").val(vval)
    var button = $('<div class="input-group-addon fa fa-folder"></div>')
    var minus = $('<div class="input-group-addon fa fa-minus"></div>')

    var updateConfs = function () {
      var confs = $('#configurations .form-group').map(function(i, elem){
        var inputs = $(elem).find('input')
        if(inputs.eq(0).val() != "" && inputs.eq(1).val() != ""){
            return [[inputs.eq(0).val(), inputs.eq(1).val()]]
        }
      }).get()
      node.data('configurations', confs)
    }

    button.on('click', function(){
      node.cy().viewDatasets(input, dir, function(n){
        value.val(n.data.fullPath);
        value.change()
      })
    })
    minus.on('click', function(o){
      var ele = $(o.target).parents(".form-group")
      ele.remove()
      updateConfs()
    })

    name.on('input', updateConfs)
    value.on('input change', updateConfs)

    root.append($('<div></div>').addClass(" col-sm-5").append($('<div></div>').addClass("input-group").append(value).append(button).append(minus)))
    r.append(root)
  }

  var form =$('<form class="form-horizontal" id="spark-form"></form>')
  form.append(node.cy().defaults.hdfsInput(node, false, "Jar*", "jar", function(target, n){
    target.val(n.data.fullPath).change()
    node.data({'jarINodeId': n.id, 'metaKey': null})
    populateTemplates(n.id)
  }))
  form.append(node.cy().defaults.textInput(node, "text", "Main Class*", "mainClass"))
  form.append(node.cy().defaults.textInput(node, "text", "Name", "name"))
  form.append(node.cy().defaults.textInput(node, "text", "Spark Options", "sparkOptions"))
  var argsDiv = $('<div></div>').attr('id', 'arguments')
  var xmlDiv = $('<div></div>').attr('id', 'xml_files')
  var mkDirDiv = $('<div></div>').attr('id', 'mk_dirs')
  var rmDirDiv = $('<div></div>').attr('id', 'rm_dirs')
  var ConfsDiv = $('<div></div>').attr('id', 'configurations')
  form.append(argsDiv)
  form.append(xmlDiv)
  form.append(mkDirDiv)
  form.append(rmDirDiv)
  form.append(ConfsDiv)

  node.data("arguments").forEach(function(e){ createArg(argsDiv, e)})
  node.data("jobXmls").forEach(function(e){ createXml(xmlDiv, e)})
  node.data("mkDirs").forEach(function(e){ createMkDir(mkDirDiv, e)})
  node.data("rmDirs").forEach(function(e){ createRmDir(rmDirDiv, e)})
  node.data("configurations").forEach(function(e){ createConfiguration(ConfsDiv, e)})



  var addArgs = addButton("Add Argument", function(){createArg($('#arguments'))})
  var addXml = addButton("Add Xml File", function(){createXml($('#xml_files'))})
  var addDirs = addButton("Add Directory", function(){createMkDir($('#mk_dirs'))})
  var addRemoveDirs = addButton("Remove Directory", function(){createRmDir($('#rm_dirs'))})
  var addConfigurations = addButton("Add Configuration", function(){createConfiguration($('#configurations'))})

  var buttonMenu = $('<ul class="uib-dropdown-menu"></ul>')

  buttonMenu.append($('<li></li>').append(addArgs))
  buttonMenu.append($('<li></li>').append(addXml))
  buttonMenu.append($('<li></li>').append(addDirs))
  buttonMenu.append($('<li></li>').append(addRemoveDirs))
  buttonMenu.append($('<li></li>').append(addConfigurations))

  var options = $('<div class="form-group"></div>')
  var buttons = $('<div class="btn-group col-sm-4"></div>')
  buttons.append($('<button type="button" class="btn btn-default uib-dropdown-toggle" data-toggle="uib-dropdown" aria-haspopup="true" aria-expanded="false">Advance <span class="caret"></span></button>')).append(buttonMenu)
  options.append(buttons)
  var tables = $('<select id = "tables"></select>')
  options.append($('<div class="col-sm-3"></div>').append(tables))
  tables.on('change', function(o){
    $.get(getApiPath() + "/metadata/" + node.data('jarINodeId'), function(data){
      var selected = $('#tables').find('option:selected').val().split('-')
      var table = data[selected[0]][selected[1]]
      var data = {}
      $.each(opts().data, function(k,v){
        if(table[k]){
          data[k] = table[k]
        }
      })
      if(!$.isEmptyObject(data)){
        data['metaKey'] = $('#tables').find('option:selected').val()
        node.data(data)
        node.trigger('tap')
      }

    })
  })
  form.prepend(options)
  if(node.data('jarINodeId')){
    populateTemplates(node.data('jarINodeId'))
  }
  return form
}

var multiInput = function(node, text, value, disabled, dir, cb){

  var root = $('<div></div>').addClass("form-group")
  root.append($('<label>' + text + ':</label>').addClass("col-sm-3 control-label"))
  var input = $('<input />').attr({type: 'text', placeholder: text}).addClass("form-control").val(value)
  if(disabled){
    input.attr("disabled", "disabled")
  }
  var button = $('<div class="input-group-addon fa fa-folder"></div>')
  var minus = $('<div class="input-group-addon fa fa-minus"></div>')

  input.on('input change', function() {
    cb()
  })

  button.on('click', function(){
    node.cy().viewDatasets(input, dir, function(n){
      input.val(n.data.fullPath);
      input.change()
    })
  })
  minus.on('click', function(o){
    var ele = $(o.target).parents(".form-group")
    ele.remove()
    cb()
  })
  root.append($('<div></div>').addClass(" col-sm-9").append($('<div></div>').addClass("input-group").append(input).append(button).append(minus)))
  return root

}

var populateTemplates = function(inodeId){
  $.get(getApiPath() + "/metadata/" + inodeId, function(data){
    $('#tables').html($("<option></option>"))
    var node = cy.$('node.selected').first()
    $.each(data, function(temp, v) {
      $.each(v, function(table, v) {
        var opt = $("<option></option>")
        var val = temp + '-' + table
        opt.attr("value",val).text(table)
        if(val === node.data('metaKey')){
          opt.attr('selected', 'selected')
        }
        $('#tables').append(opt);
      });
    });
  })
}

var addButton = function(text, cb){
  var button = $('<a>' + text + '</a>').attr('href', '#')
  button.click( function(){
    cb()
  })
  return button
}
module.exports = {
  init: init,
  opts: opts,
  form: form
}

},{}],7:[function(require,module,exports){
module.exports = [
  {
      selector: 'node',
      css: {
          'text-halign': 'center',
          'text-valign': 'center',
          'background-opacity': '0'
      }
  },
  {
    selector: 'edge',
    style: {
      'width': 3,
      'line-color': '#ccc',
      'target-arrow-color': '#ccc',
      'target-arrow-shape': 'triangle'
    }
  },
  {
    selector: 'edge[type = "default-decision-edge"]',
    style: {
      // 'curve-style': "segments",
      'line-color': '#aaa',
      'line-style': 'dashed'
    }
  },
  {
      selector: 'node.selected',
      css: {
        color: '#337ab7'
      }
  },
  {
      selector: 'node.current',
      css: {
        color: '#337ab7'
      }
  },
  {
      selector: 'node.error',
      css: {
        color: '#d9534f'
      }
  },
  {
      selector: 'node.killed',
      css: {
        color: '#f0ad4e'
      }
  },
  {
      selector: 'node.completed',
      css: {
        color: '#5cb85c'
      }
  },
  {
      selector: 'edge.completed',
      css: {
        'line-color':'#ACE8AC',
        'target-arrow-color': '#ACE8AC'
      }
  },
  {
    selector: '.tool-node',
    css: {
        'font-family': 'FontAwesome',
        'font-size': '2em',
    }
  },
  {
    selector: '.spark-custom-node',
    css: {
        'padding-left': '75px',
        'label': function(ele){
          return content('\uf006 ' ,ele)
        }
    }
  },
  {
    selector: '.end-node',
    css: {
        'content': '\uf28d'
    }
  },
  {
    selector: '.root-node',
    css: {
        'content': '\uf144'
    }
  },
  {
    selector: '.success-node',
    css: {
        'content': '\uf058'
    }
  },
  {
    selector: '.fail-node',
    css: {
        'content': '\uf06a'
    }
  },
  {
    selector: '.email-node',
    css: {
        'padding-left': '75px',
        'label': function(ele){
          return content('\uf003' ,ele)
        }
    }
  },
  {
    selector: '.blank-node',
    css: {
        'padding-left': '75px',
        'label': function(ele){
          return content('\uf10c ' ,ele)
        }
    }
  },
  {
    selector: '.decision-node',
    css: {
        'content': '\uf059'
    }
  },
  {
    selector: '.decision-eval-node',
    css: {
        'content': '\uf0ae'
    }
  },
  {
    selector: '.fork-node',
    css: {
      'label': function(ele){
        return content('\uf085 ' ,ele)
      }
    }
  },
  {
    selector: '.join-node',
    css: {
      'label': function(ele){
        return content('\uf013 ' ,ele)
      }
    }
  }
]

var content = function(icon, ele){
  return icon + ele.id().substr(ele.id().length - 5)
}

},{}],8:[function(require,module,exports){
var tools = []
var init = function(){
  window.cy.toolbar({
      autodisableForMobile: false,
      tools: tools,
      position: 'right',
      appendTools: true,
      zIndex: 1
  });
  tools.splice(0, tools.length)
}
var addObject = function(e, action) {
    if (!e.data.canPerform(e, action)) {
        return;
    }
    var node = e.cyTarget;
    if (node.is("[!mutable]")) {
        return;
    }
    var toolIndexes = e.data.data.selectedTool;
    var tool = e.data.data.options.tools[toolIndexes[0]][toolIndexes[1]];
    cy.batch(function(){
      node.removeData()
      node.data(tool.options.data).classes('').addClass(tool.options.clazz);
    })
    removeSelectedTool(e);
}
function removeSelectedTool(e){
  e.data.data.selectedTool = undefined;
  $('.' + e.data.data.options.toolbarClass).find('.selected-tool').css('color','#aaa').removeClass('selected-tool');
}
module.exports = {
  addObject: addObject,
  init : init,
  tools: tools
}

},{}]},{},[3]);
