# propprop
> pluck a property out of an object.


#### Before
```js
var friendsAsJavaScriptLibraries = [
  {
    name: 'passy.js',
    hobby: 'clay'
  },
  {
    name: 'sindre.js',
    vehicle: 'unicorn taxi'
  },
  {
    name: 'addy.js',
    invented: 'google *'
  }
];

var names = friendsAsJavaScriptLibraries.map(function (item) {
  return item.name;
});
```

#### After
```js
var friendsAsJavaScriptLibraries = [ /* ... */ ];

var names = friendsAsJavaScriptLibraries.map(prop('name'));
```

The benefit is more noticeable when you're using multiple [Array.prototype](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/prototype) methods to shape your objects:

```js
var friendsAsJavaScriptLibraries = [ /* ... */ ];
var urls;

if (friendsAsJavaScriptLibraries.every(prop('name'))) {
  urls = friendsAsJavaScriptLibraries
    .map(function (item) {
      item.url = 'https://github.com/magical-library-land/' + item.name;
      return item;
    })
    .map(prop('url'));
}
```


## Why?
Perhaps you're already using [Lo-Dash](http://lodash.com), [Underscore.js](http://underscorejs.org), or another utility library. This should not supplement those libraries, and definitely cannot replace. This is better suited towards applications that don't want to carry the weight and/or functionality of such libraries, and just want something simple and modular.


## Install
`prop` can be used in the browser or within your Node.js apps.

#### Browser
```bash
$ bower install --save propprop
```
```html
<script src="bower_components/propprop/browser.js"></script>
```

#### Node.js
```bash
$ npm install --save propprop
```
```js
var prop = require('propprop');
```


## License

[MIT](http://opensource.org/licenses/MIT) © [Stephen Sawchuk](http://sawchuk.me)
