RegExp.quote = require("./regexp-quote.js")

var foo = "-\\^$*+?.()|[]{}"

console.assert((foo + foo).replace(new RegExp(
  "(" + RegExp.quote(foo) + "){2}"
), "ok") === "ok")
console.log("OK.")
