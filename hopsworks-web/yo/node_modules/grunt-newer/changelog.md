# Change Log

## 1.2.0

 * Add `tolerance` option to account for filesystem time precision (thanks @jorrit, see [#94][#94])
 * Updated dependencies (thanks @jorrit, see [#93][#93])

## 1.1.2

 * Update peer dependency for Grunt (thanks @steveoh, see [#91][91])

## 1.1.1

 * Update license identifier (MIT)

## 1.1.0

 * Write current time to timestamp file (thanks @malys, see [#69][69])

## 1.0.0

 * Document that grunt-newer works with grunt-spritesmith >= 3.1.0 (thanks @danez, see [#66][66])
 * Support for an empty list of source files (thanks @ruslansagitov, see [#62][62])

## 0.8.0

 * Support for a single source file that matches the dest file (thanks @btholt, see [#42][42] and [#62][62])
 * Avoid unhandled error when task is aliased (see [#61][61])

## 0.7.0

 * Support for `override` option.  In cases where a `src` file should be included even if it has not been modified (e.g. a LESS file whose imports have been modified), the `override` option can be used (see [#35][35])

## 0.6.1

 * When `src` and `dest` files are the same, the previous run time is considered (see [#24][24])

## 0.6.0

 * Deprecated `any-newer` task (`newer` task now handles this automatically, see [#17][17])
 * Deprecated `timestamps` option (use `cache` instead)
 * Consolidated `newer-reconfigure` and `newer-timestamp` into single `newer-postrun` task
 * Refactor task for easier unit testing (see [#16][16])

## 0.5.4

 * Correctly handle cases where `dest` file is not present (thanks @royriojas, see [#11][11])

## 0.5.3

 * Add `newer-reconfigure` to properly reset task configuration (see [#8][8])

## 0.5.2

 * Fix use of `any-newer` on task with multiple targets (thanks @royriojas, see [#7][7])

## 0.5.1

 * Filter out file objects with no remaining `src` files (see [#6][6])

## 0.5.0

 * Compare `src` file modification times to `dest` files if present (see [#2][2])

 [2]: https://github.com/tschaub/grunt-newer/pull/2
 [6]: https://github.com/tschaub/grunt-newer/pull/6
 [7]: https://github.com/tschaub/grunt-newer/pull/7
 [8]: https://github.com/tschaub/grunt-newer/pull/8
 [11]: https://github.com/tschaub/grunt-newer/pull/11
 [16]: https://github.com/tschaub/grunt-newer/pull/16
 [17]: https://github.com/tschaub/grunt-newer/pull/17
 [24]: https://github.com/tschaub/grunt-newer/pull/24
 [35]: https://github.com/tschaub/grunt-newer/pull/35
 [42]: https://github.com/tschaub/grunt-newer/pull/42
 [61]: https://github.com/tschaub/grunt-newer/pull/61
 [62]: https://github.com/tschaub/grunt-newer/pull/62
 [66]: https://github.com/tschaub/grunt-newer/pull/66
 [69]: https://github.com/tschaub/grunt-newer/pull/69
 [91]: https://github.com/tschaub/grunt-newer/pull/91
 [93]: https://github.com/tschaub/grunt-newer/pull/93
 [94]: https://github.com/tschaub/grunt-newer/pull/94
