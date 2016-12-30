> -
# grunt-wiredep
*Inject Bower packages into your source code with Grunt.*
>
> -

## What is this?
[Grunt](http://gruntjs.com) is great.

[Bower](http://bower.io) is great.

**And now they work great together.**

`grunt-wiredep` is a Grunt plug-in, which finds your components and injects them directly into the HTML file you specify.

Whether you're already using Bower and Grunt, or new to both, `grunt-wiredep` will be easy to plug in, as you will see in the steps below.

_**do note**: Bower is still a young little birdy, so things are changing rapidly. Authors of Bower components must follow certain conventions and best practices in order for this plug-in to be as accurate as possible. It's not a perfect world out there, so needless to say, some Bower components may not work as well as others._

## Getting Started

*If you are new to Grunt, you will find a lot of answers to your questions in their [getting started guide](http://gruntjs.com/getting-started).

To install the module:
```
npm install --save-dev grunt-wiredep
```

Include the task in your Gruntfile:
```js
grunt.loadNpmTasks('grunt-wiredep');
```

Create a config block within your Gruntfile:
```js
wiredep: {

  task: {

    // Point to the files that should be updated when
    // you run `grunt wiredep`
    src: [
      'app/views/**/*.html',   // .html support...
      'app/views/**/*.jade',   // .jade support...
      'app/styles/main.scss',  // .scss & .sass support...
      'app/config.yml'         // and .yml & .yaml support out of the box!
    ],
    
    options: {
      // See wiredep's configuration documentation for the options
      // you may pass:

      // https://github.com/taptapship/wiredep#configuration
    }
  }
}
```

See [wiredep's readme](http://github.com/taptapship/wiredep#configuration) for more options of customization, such as other file types, regex patterns, exclusions, and more.

Options can be specified on both task and target level. See [grunt documentation](http://gruntjs.com/configuring-tasks#options) for more details on how this works.

For JavaScript dependencies, pop this in your HTML file:
```html
<!-- bower:js -->
<!-- endbower -->
```

Install a Bower component:
```
bower install jquery --save
```

Call the Grunt task:
```
grunt wiredep
```

You're in business!
```html
<!-- bower:js -->
<script src="bower_components/jquery/jquery.js"></script>
<!-- endbower -->
```

## Behind the Scenes
This plug-in uses [wiredep](https://github.com/stephenplusplus/wiredep), which takes a look at all of the components you have, then determines the best order to inject your scripts in to your HTML file.

Putting script tags that aren't managed by `grunt-wiredep` is not advised, as anything between `<!-- bower:js -->` and `<!-- endbower -->` will be overwritten with each command.

## Examples
A simple sample apple:
[website](http://stephenplusplus.github.io/grunt-wiredep) | [github](https://github.com/stephenplusplus/grunt-wiredep/tree/gh-pages)

## Tutorial

A simple [tutorial](http://grunt-tasks.com/grunt-wiredep/) on how to use grunt-wiredep

## License
Copyright (c) 2014 Stephen Sawchuk
Licensed under the MIT license.
