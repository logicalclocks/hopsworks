
// MooTools: the javascript framework.
// Load this file's selection again by visiting: http://mootools.net/more/98fc877e84c1594d47fd0ae27648bc30 
// Or build this file again with packager using: packager build More/Class.Binds More/Element.Measure More/Locale
/*
---

script: More.js

name: More

description: MooTools More

license: MIT-style license

authors:
  - Guillermo Rauch
  - Thomas Aylott
  - Scott Kyle
  - Arian Stolwijk
  - Tim Wienk
  - Christoph Pojer
  - Aaron Newton
  - Jacob Thornton

requires:
  - Core/MooTools

provides: [MooTools.More]

...
*/

MooTools.More = {
  'version': '1.4.0.1',
  'build': 'a4244edf2aa97ac8a196fc96082dd35af1abab87'
};


/*
---

script: Class.Binds.js

name: Class.Binds

description: Automagically binds specified methods in a class to the instance of the class.

license: MIT-style license

authors:
  - Aaron Newton

requires:
  - Core/Class
  - /MooTools.More

provides: [Class.Binds]

...
*/

Class.Mutators.Binds = function (binds) {
  if (!this.prototype.initialize)
    this.implement('initialize', function () {
    });
  return Array.from(binds).concat(this.prototype.Binds || []);
};

Class.Mutators.initialize = function (initialize) {
  return function () {
    Array.from(this.Binds).each(function (name) {
      var original = this[name];
      if (original)
        this[name] = original.bind(this);
    }, this);
    return initialize.apply(this, arguments);
  };
};


/*
---

script: Element.Measure.js

name: Element.Measure

description: Extends the Element native object to include methods useful in measuring dimensions.

credits: "Element.measure / .expose methods by Daniel Steigerwald License: MIT-style license. Copyright: Copyright (c) 2008 Daniel Steigerwald, daniel.steigerwald.cz"

license: MIT-style license

authors:
  - Aaron Newton

requires:
  - Core/Element.Style
  - Core/Element.Dimensions
  - /MooTools.More

provides: [Element.Measure]

...
*/

(function () {

  var getStylesList = function (styles, planes) {
    var list = [];
    Object.each(planes, function (directions) {
      Object.each(directions, function (edge) {
        styles.each(function (style) {
          list.push(style + '-' + edge + (style == 'border' ? '-width' : ''));
        });
      });
    });
    return list;
  };

  var calculateEdgeSize = function (edge, styles) {
    var total = 0;
    Object.each(styles, function (value, style) {
      if (style.test(edge))
        total = total + value.toInt();
    });
    return total;
  };

  var isVisible = function (el) {
    return !!(!el || el.offsetHeight || el.offsetWidth);
  };


  Element.implement({
    measure: function (fn) {
      if (isVisible(this))
        return fn.call(this);
      var parent = this.getParent(),
              toMeasure = [];
      while (!isVisible(parent) && parent != document.body) {
        toMeasure.push(parent.expose());
        parent = parent.getParent();
      }
      var restore = this.expose(),
              result = fn.call(this);
      restore();
      toMeasure.each(function (restore) {
        restore();
      });
      return result;
    },
    expose: function () {
      if (this.getStyle('display') != 'none')
        return function () {
        };
      var before = this.style.cssText;
      this.setStyles({
        display: 'block',
        position: 'absolute',
        visibility: 'hidden'
      });
      return function () {
        this.style.cssText = before;
      }.bind(this);
    },
    getDimensions: function (options) {
      options = Object.merge({computeSize: false}, options);
      var dim = {x: 0, y: 0};

      var getSize = function (el, options) {
        return (options.computeSize) ? el.getComputedSize(options) : el.getSize();
      };

      var parent = this.getParent('body');

      if (parent && this.getStyle('display') == 'none') {
        dim = this.measure(function () {
          return getSize(this, options);
        });
      } else if (parent) {
        try { //safari sometimes crashes here, so catch it
          dim = getSize(this, options);
        } catch (e) {
        }
      }

      return Object.append(dim, (dim.x || dim.x === 0) ? {
        width: dim.x,
        height: dim.y
      } : {
        x: dim.width,
        y: dim.height
      }
      );
    },
    getComputedSize: function (options) {


      options = Object.merge({
        styles: ['padding', 'border'],
        planes: {
          height: ['top', 'bottom'],
          width: ['left', 'right']
        },
        mode: 'both'
      }, options);

      var styles = {},
              size = {width: 0, height: 0},
      dimensions;

      if (options.mode == 'vertical') {
        delete size.width;
        delete options.planes.width;
      } else if (options.mode == 'horizontal') {
        delete size.height;
        delete options.planes.height;
      }

      getStylesList(options.styles, options.planes).each(function (style) {
        styles[style] = this.getStyle(style).toInt();
      }, this);

      Object.each(options.planes, function (edges, plane) {

        var capitalized = plane.capitalize(),
                style = this.getStyle(plane);

        if (style == 'auto' && !dimensions)
          dimensions = this.getDimensions();

        style = styles[plane] = (style == 'auto') ? dimensions[plane] : style.toInt();
        size['total' + capitalized] = style;

        edges.each(function (edge) {
          var edgesize = calculateEdgeSize(edge, styles);
          size['computed' + edge.capitalize()] = edgesize;
          size['total' + capitalized] += edgesize;
        });

      }, this);

      return Object.append(size, styles);
    }

  });

})();


/*
---

script: Object.Extras.js

name: Object.Extras

description: Extra Object generics, like getFromPath which allows a path notation to child elements.

license: MIT-style license

authors:
  - Aaron Newton

requires:
  - Core/Object
  - /MooTools.More

provides: [Object.Extras]

...
*/

(function () {

  var defined = function (value) {
    return value != null;
  };

  var hasOwnProperty = Object.prototype.hasOwnProperty;

  Object.extend({
    getFromPath: function (source, parts) {
      if (typeof parts == 'string')
        parts = parts.split('.');
      for (var i = 0, l = parts.length; i < l; i++) {
        if (hasOwnProperty.call(source, parts[i]))
          source = source[parts[i]];
        else
          return null;
      }
      return source;
    },
    cleanValues: function (object, method) {
      method = method || defined;
      for (var key in object)
        if (!method(object[key])) {
          delete object[key];
        }
      return object;
    },
    erase: function (object, key) {
      if (hasOwnProperty.call(object, key))
        delete object[key];
      return object;
    },
    run: function (object) {
      var args = Array.slice(arguments, 1);
      for (var key in object)
        if (object[key].apply) {
          object[key].apply(object, args);
        }
      return object;
    }

  });

})();


/*
---

script: Locale.js

name: Locale

description: Provides methods for localization.

license: MIT-style license

authors:
  - Aaron Newton
  - Arian Stolwijk

requires:
  - Core/Events
  - /Object.Extras
  - /MooTools.More

provides: [Locale, Lang]

...
*/

(function () {

  var current = null,
          locales = {},
          inherits = {};

  var getSet = function (set) {
    if (instanceOf(set, Locale.Set))
      return set;
    else
      return locales[set];
  };

  var Locale = this.Locale = {
    define: function (locale, set, key, value) {
      var name;
      if (instanceOf(locale, Locale.Set)) {
        name = locale.name;
        if (name)
          locales[name] = locale;
      } else {
        name = locale;
        if (!locales[name])
          locales[name] = new Locale.Set(name);
        locale = locales[name];
      }

      if (set)
        locale.define(set, key, value);



      if (!current)
        current = locale;

      return locale;
    },
    use: function (locale) {
      locale = getSet(locale);

      if (locale) {
        current = locale;

        this.fireEvent('change', locale);


      }

      return this;
    },
    getCurrent: function () {
      return current;
    },
    get: function (key, args) {
      return (current) ? current.get(key, args) : '';
    },
    inherit: function (locale, inherits, set) {
      locale = getSet(locale);

      if (locale)
        locale.inherit(inherits, set);
      return this;
    },
    list: function () {
      return Object.keys(locales);
    }

  };

  Object.append(Locale, new Events);

  Locale.Set = new Class({
    sets: {},
    inherits: {
      locales: [],
      sets: {}
    },
    initialize: function (name) {
      this.name = name || '';
    },
    define: function (set, key, value) {
      var defineData = this.sets[set];
      if (!defineData)
        defineData = {};

      if (key) {
        if (typeOf(key) == 'object')
          defineData = Object.merge(defineData, key);
        else
          defineData[key] = value;
      }
      this.sets[set] = defineData;

      return this;
    },
    get: function (key, args, _base) {
      var value = Object.getFromPath(this.sets, key);
      if (value != null) {
        var type = typeOf(value);
        if (type == 'function')
          value = value.apply(null, Array.from(args));
        else if (type == 'object')
          value = Object.clone(value);
        return value;
      }

      // get value of inherited locales
      var index = key.indexOf('.'),
              set = index < 0 ? key : key.substr(0, index),
              names = (this.inherits.sets[set] || []).combine(this.inherits.locales).include('en-US');
      if (!_base)
        _base = [];

      for (var i = 0, l = names.length; i < l; i++) {
        if (_base.contains(names[i]))
          continue;
        _base.include(names[i]);

        var locale = locales[names[i]];
        if (!locale)
          continue;

        value = locale.get(key, args, _base);
        if (value != null)
          return value;
      }

      return '';
    },
    inherit: function (names, set) {
      names = Array.from(names);

      if (set && !this.inherits.sets[set])
        this.inherits.sets[set] = [];

      var l = names.length;
      while (l--)
        (set ? this.inherits.sets[set] : this.inherits.locales).unshift(names[l]);

      return this;
    }

  });



})();

