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

// zooming
function performZoomIn(e) {
	console.log("performing zoom in");
	performZoom(e, performZoomIn);
}

function performZoomOut(e) {
	console.log("performing zoom out");
	performZoom(e, performZoomOut);
}

function performZoom(e, action) {
	if (!e.data.canPerform(e, action)) {
		console.log("could not perform zoom");

		return;
	}

	var toolIndexes = e.data.data.selectedTool;
	var tool = e.data.data.options.tools[toolIndexes[0]][toolIndexes[1]];

	zoomGraph(e.cy, e.originalEvent.offsetX, e.originalEvent.offsetY, tool.options.cy);
}

function zoomGraph(core, x, y, factors) {
	console.log("zooming:");
	console.log({ x : x, y : y, factors : factors });

	var factor = 1 + factors.zoom;

	var zoom = core.zoom();

	var lvl = zoom * factor;

	if (lvl < factors.minZoom) {
		lvl = factors.minZoom;
	}

	if (lvl > factors.maxZoom) {
		lvl = factors.maxZoom;
	}

	if ((lvl == factors.maxZoom && zoom == factors.maxZoom) ||
		(lvl == factors.minZoom && zoom == factors.minZoom)
	) {
		return;
	}

	zoomTo(core, x, y, lvl);
}

var zx, zy;
function zoomTo(core, x, y, level) {
	core.zoom({
		level: level,
		renderedPosition: { x: x, y: y }
	});
}
// end zooming

// panning
function performPanRight(e) {
	console.log("performing pan right");
	performPan(e, performPanRight, 0);
}

function performPanDown(e) {
	console.log("performing pan down");
	performPan(e, performPanDown, 1);
}

function performPanLeft(e) {
	console.log("performing pan left");
	performPan(e, performPanLeft, 2);
}

function performPanUp(e) {
	console.log("performing pan up");
	performPan(e, performPanUp, 3);
}

function performPan(e, action, direction) {
	if (!e.data.canPerform(e, action)) {
		console.log("could not perform pan");
		return;
	}

	console.log("performing pan");

	var toolIndexes = e.data.data.selectedTool;
	var tool = e.data.data.options.tools[toolIndexes[0]][toolIndexes[1]];

	pan(e.cy, direction, tool.options.cy);
}

function pan(core, direction, factors) {
	switch (direction) {
		case 0:
		case 2:
			core.panBy({ x: factors.distance, y: 0 });
			break;
		case 1:
		case 3:
			core.panBy({ x: 0, y: factors.distance });
			break;
	}
}
// end panning

(function ($) {
	var defaults = {
		cyContainer: 'cy', // id being used for cytoscape core instance
		tools: [], // an array of tools to list in the toolbar
		appendTools: false, // set whether or not to append your custom tools list to the default tools list
		position: 'left', // set position of toolbar (right, left, up, down)
		toolbarClass: 'ui-cytoscape-toolbar', // set a class name for the toolbar to help with styling
		multipleToolsClass: 'tool-item-list', // set a class name for the tools that should be shown in the same position
		toolItemClass: 'tool-item', // set a class name for a toolbar item to help with styling
		autodisableForMobile: true, // disable the toolbar completely for mobile (since we don't really need it with gestures like pinch to zoom)
		zIndex: 9999, // the z-index of the ui div
		longClickTime: 325 // time until a multi-tool list will present other tools
	};

	console.log("creating cytoscape-toolbar with defaults:");
	console.log(defaults);

	// registers the extension on a cytoscape lib ref
	var register = function( cytoscape, $ ) {
		if( !cytoscape ) {
			console.log("cytoscape is not defined");

			return;
		} // can't register if cytoscape unspecified

		cytoscape('core', 'toolbar', function(params) {
			var options = $.extend(true, {}, defaults, params);

			console.log("final cytoscape-toolbar options:");
			console.log(options);

			if (params) {
				if (params.tools === undefined) { params.tools = defaults.tools; }

				options.tools = params.tools;
			}

			if (options.appendTools) {
				if (!options.tools) {
					options.tools = defaults.tools;
				} else {
					var finalToolsList = [];

					for (var d = 0; d < defaults.tools.length; d++) {
						finalToolsList.push(defaults.tools[d]);
					}

					for (var i = 0; i < options.tools.length; i++) {
						finalToolsList.push(options.tools[i]);
					}

					options.tools = finalToolsList;
				}
			}

			var fn = params;
			var $container = $( this.container() );
			var cy;
			var hoveredTool;

			var functions = {
				destroy: function () {
					var data = $(this).data('cytoscapeToolbar');
					var options = data.options;
					var handlers = data.handlers;
					var cy = data.cy;

					// remove bound cy handlers
					for (var i = 0; i < handlers.length; i++) {
						var handler = handlers[i];
						cy.off(handler.events, handler.selector, handler.fn);
					}

					// remove container from dom
					data.$container.remove();
				},

				canPerform: function (e, fn) {
					if (!this.data.selectedTool) {
						return false;
					}

					var toolIndexes = this.data.selectedTool;
					var tool = this.data.options.tools[toolIndexes[0]][toolIndexes[1]];
					var handlerIndex = this.handlerIndex;

					if (!(toolIndexes === undefined) && $.inArray(fn, tool.action) > -1) {
						var selector = this.data.handlers[handlerIndex].selector;

						switch (selector) {
							case 'node':
								return e.cyTarget.isNode();
							case 'edge':
								return e.cyTarget.isEdge();
							case 'node,edge':
							case 'edge,node':
								return e.cyTarget.isNode() || e.cyTarget.isEdge();
							case 'cy':
								return e.cyTarget == cy || tool.bubbleToCore;
						}
					}

					return false;
				},

				getToolOptions: function(selectedTool) {
					var tool = this.data.options.tools[selectedTool[0]][selectedTool[1]];

					return tool.options;
				},

				init: function () {
					// check for a mobile device
					var browserIsMobile = 'ontouchstart' in window;

					// **** REMOVE THIS CHECK IF YOU DON'T CARE ABOUT SHOWING IT IN MOBILE
					// don't do anything because this plugin hasn't been tested for mobile
					if (browserIsMobile && options.autodisableForMobile) {
						return $(this);
					}

					// setup an object to hold data needed for the future
					var data = {
						selectedTool: undefined,
						options: options,
						handlers: []
					};

					// setup default css values
					var cssOptions = {
						position: 'absolute',
						top: 0,
						left: 0,
						width: 0,
						height: 0,
						minWidth: 0,
						minHeight: 0,
						maxWidth: 0,
						maxHeight: 0,
						zIndex: options.zIndex
					};

					// check for toolbar position to calculate CSS position values

					if (options.position === 'top') {
						cssOptions.top = $container.offset().top - 45;
						cssOptions.left = $container.offset().left;
						cssOptions.width = $container.outerWidth(true);
						cssOptions.minWidth = $container.outerWidth(true);
						cssOptions.maxWidth = $container.outerWidth(true);
					} else if (options.position === 'bottom') {
						cssOptions.top = $container.offset().top + $container.outerHeight(true);
						cssOptions.left = $container.offset().left;
						cssOptions.width = $container.outerWidth(true);
						cssOptions.minWidth = $container.outerWidth(true);
						cssOptions.maxWidth = $container.outerWidth(true);
					} else if (options.position === 'right') {
						cssOptions.top = $container.offset().top;
						cssOptions.left = $container.offset().left + $container.outerWidth(true)-46;
						cssOptions.height = $container.outerHeight(true);
						cssOptions.minHeight = $container.outerHeight(true);
						cssOptions.maxHeight = $container.outerHeight(true);
					} else { // default - it is either 'left' or it is something we don't know so we use the default of 'left'
						cssOptions.top = $container.offset().top;
						cssOptions.left = $container.offset().left - 45;
						cssOptions.height = $container.outerHeight(true);
						cssOptions.minHeight = $container.outerHeight(true);
						cssOptions.maxHeight = $container.outerHeight(true);
					}

					// create toolbar element with applied css
					var $toolbar = $('<div class="' + options.toolbarClass + '"></div>')
						.css(cssOptions)
					data.$container = $toolbar;
					$toolbar.appendTo('#workflow');

					$.each(options.tools, function (toolListIndex, toolList) {
						var $toolListWrapper = $('<div class="' + options.multipleToolsClass + '-wrapper"></div>')
							.css({
								width: 45,
								height: 45,
								position: 'relative',
								overflow: 'hidden',
								float: 'left'
							});

						$toolbar.append($toolListWrapper);

						if (toolList.length > 1) {
							var $moreArrow = $('<span class="fa fa-caret-right"></span>')
								.css({
									'background-color': 'transparent',
									position: 'absolute',
									top: 28,
									left: 35,
									zIndex: 9999
								});
							$toolListWrapper.append($moreArrow);
						}

						var $toolList = $('<div class="' + options.multipleToolsClass + '"></div>')
							.css({
								position: 'absolute',
								width: toolList.length * 55,
								height: 45,
								'background-color': '#f9f9f9'
							});

						$toolListWrapper.append($toolList);

						$.each(toolList, function (toolIndex, element) {
							var padding = "";

							if (toolIndex != options.tools.length - 1) {
								if (options.position === 'top' || options.position === 'bottom') {
									padding = "padding: 10px 0 10px 10px;";
								} else if (options.position === 'right' || options.position === 'left') {
									padding = "padding: 10px 10px 0 10px;";
								}
							} else {
								padding = "padding: 10px;";
							}

							var clazz = options.toolItemClass + ' icon ' + element.icon;
							var style = 'cursor: pointer; color: #aaa; width: 35px; height: 35px; font-size: 24px; ' + padding;

							var jElement = $('<span ' +
								'id="tool-' + toolListIndex + '-' + toolIndex + '" ' +
								'class="' + clazz + '" ' +
								'style="' + style + '" ' +
								'title="' + element.tooltip + '" ' +
								'data-tool="' + toolListIndex + ',' + toolIndex + '"' +
								'></span>');

							data.options.tools[toolListIndex][toolIndex].element = jElement;

							$toolList.append(jElement);

							var pressTimer;
							var startTime, endTime;
							var toolItemLongHold = false;

							jElement
								.mousedown(function () {
									startTime = new Date().getTime();
									endTime = startTime;

									pressTimer = window.setTimeout(function () {
										if (startTime == endTime) {
											toolItemLongHold = true;
											$toolListWrapper.css('overflow', 'visible');
										}
									}, options.longClickTime);
								})
								.mouseup(function () {
									endTime = new Date().getTime();

									if (data.selectedTool != [toolListIndex, toolIndex] && !toolItemLongHold) {
										if (data.selectedTool != undefined) {
											data.options.tools[data.selectedTool[0]][data.selectedTool[1]].element.css('color', '#aaa');
										}
										data.selectedTool = [toolListIndex, toolIndex];
										$('.' + options.toolbarClass).find('.selected-tool').css('color','#aaa').removeClass('selected-tool');
										$(this).addClass('selected-tool').css('color', '#000');
									}
								});
							;

							$(window)
								.mouseup(function (e) {
									if (toolItemLongHold) {
										var moveLeft = 0;
										$.each(hoveredTool.parent().children(), function (index, element) {
											if (hoveredTool.index() == index) {
												return false;
											}

											moveLeft += $(element).outerWidth(true);
										});
										var indexes = hoveredTool.attr('data-tool').split(',');
										data.selectedTool = indexes;
										var offsetLeft = 0 - moveLeft;
										$toolList.css('left', offsetLeft);
										$toolListWrapper.css('overflow', 'hidden');
										$('.' + options.toolbarClass).find('.selected-tool').removeClass('selected-tool');
										hoveredTool.addClass('selected-tool');
										clearTimeout(pressTimer);
										toolItemLongHold = false;
										startTime = -1;
										endTime = -1;
										return false;
									}
								})
							;

							jElement
								.hover(function () {
									hoveredTool = $(this);

									hoveredTool.css('color', '#000');
								}, function () {
									if (hoveredTool.hasClass('selected-tool')) {
										hoveredTool.css('color', '000');
									} else {
										hoveredTool.css('color', '#aaa');
									}
								})
							;
						});
					});

					var bindings = {
						on: function (event, selector, action) {
							var index = data.handlers.push({
								events: event,
								selector: selector,
								action: action
							});

							var eventData = {
								data: data,
								handlerIndex: index - 1,
								canPerform: functions.canPerform,
								getToolOptions: functions.getToolOptions
							};

							if (selector === 'cy') {
								cy.bind(event, eventData, action);
							} else {
								cy.on(event, selector, eventData, action);
							}

							return this;
						}
					};

					function addEventListeners() {
						$.each(options.tools, function (index, toolList) {
							$.each(toolList, function (index, toolElement) {
								var unequalsLengths = false;

								if (toolElement.event.length != toolElement.action.length) {
									var tooltip = (toolElement.tooltip) ? toolElement.tooltip : "<no tooltip>";
									console.log("Unequal lengths for event and action variables on " + index + "-" + tooltip);
									unequalsLengths = true;
								}

								if (!unequalsLengths) {
									for (var i = 0; i < toolElement.event.length; i++) {
										bindings.on(toolElement.event[i], toolElement.selector, toolElement.action[i]);
									}
								}
							});
						});
					}

					$container.cytoscape(function (e) {
						cy = this;
						data.cy = cy;

						addEventListeners();

						$container.data('cytoscapeToolbar', data);
					});
				}
			};

			if (functions[fn]) {
				return functions[fn].apply(this, Array.prototype.slice.call(arguments, 1));
			} else if (typeof fn == 'object' || !fn) {
				return functions.init.apply(this, arguments);
			} else {
				$.error("No such function `" + fn + "` for jquery.cytoscapeToolbar");
			}

			return $(this);
		}); // cytoscape()
	}; // register

	if( typeof module !== 'undefined' && module.exports ){ // expose as a commonjs module
		module.exports = register;
	}

	if( typeof define !== 'undefined' && define.amd ){ // expose as an amd/requirejs module
		define('cytoscape-toolbar', function(){
			return register;
		});
	}

	if( typeof cytoscape !== 'undefined' ){ // expose to global cytoscape (i.e. window.cytoscape)
		register( cytoscape, $ );
	}

})(jQuery);
