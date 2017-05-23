# Introduction to wah

wah is a slightly higher level language that is a superset of WebAssembly.
It aims to make WebAssembly's text format slightly more friendly to humans, without
introducing new syntax or datatypes. On top of WebAssembly's text format,
wah adds:

* [Infix](https://en.wikipedia.org/wiki/Infix_notation) mathematical operators: `+, -, *, /, ==, >, >=, <, <=`
* Basic type inference that works with those infix operators
* Shortcuts for getting & setting local variables and parameters, using the `%` character
* Literal number syntax for integers (without `.`) and f64 numbers

These language features are normal for programmers coming from C-family languages:
to understand the intention behind `wah`, consider the verbose literal syntax
of the WebAssembly text format: to add the number 1 to the number 2, you'd write:

```
(i32.add (i32.const 1) (i32.const 2))
```

And the same for most math: all numeric operators are typed, so you have to
remember the types of the arguments, and there's no numeric literal syntax. Same
with local variables and parameters: to add the first argument to the second,
you'd write:

```
(i32.add (get_local 0) (get_local 1))
```

In comparison, wah makes these two examples succinct:

```
(1 + 2)
```

and

```
(%0 + %1)
```

## The stack

The complete flow of using wah is:

* Write code in wah
* Expand wah to WebAssembly text (WAST) using this project
* Compile WAST to WASM using [wabt](https://github.com/webassembly/wabt)'s `wast2wasm`
* Load and run in a browser using the instantiation code like:

```js
fetch('./output.wasm').then(response =>
  response.arrayBuffer()
).then(bytes =>
  WebAssembly.instantiate(bytes)
).then(results => {
  console.log(results.instance.exports.exportedFunction(i));
});
```

The [MDN docs for the WebAssembly API](https://developer.mozilla.org/en-US/docs/WebAssembly) are a good
reference for the browser-side part of this process.

## Infix operators

Supported infix operators and their results are:

```
+ add
- sub
* mul
/ div
== eq
> gt
>= ge
< lt
<= le
```

Infix operators all support type inference: they look at each of their
arguments and specialize, adding `f32` or `f64` or their type, producing,
for instance, `f64.mul` from `*`.

**Note** that operators for the `i32` type are also specialized to signed and
unsigned numbers in WebAssembly: the signedness and unsigness of a number is
not part of the type in WebAssembly but is something you keep track of and
maintain using the right operators. Since this is 'invisible' in a way, wah
currently compiles all i32 operators to the signed versions. I'd be happy
to review & accept a PR with a more precise or creative solution to that issue!

## Getting & setting shortcuts

In WebAssembly, you need to use `get_local` to get any local parameter
or variable. wah adds a shortcut: the `%` character. You add this before
the parameter number or the local variable name, like so:

```
; WebAssembly
(get_local 0)
; wah
%0

; WebAssembly
(get_local $foo)
; wah
%$foo
```

It also supports shortcuts for setting variables, with an infix `=` "assignment operator":

```
; WebAssembly
(set_local $foo (f64.const 0))
; wah
($foo = 0.0)
```

## Type inference

WebAssembly is very strictly typed: all operators, return values, and parameters
are typed. wah adds a very basic type system that operates as follows:

1. It collects all explicit declared types of variables, parameters, and functions
2. It traverses the program from the leaf nodes up, assigning types based
   on that 'type map', and giving mathematical operators types according to their
   return values. If types clash, it throws an error.

This type system is complete enough for our current problem set, but it lacks
complete support for tables. I'll happily review PRs expanding type inference.

## Literal number types

WebAssembly's text format requires all number values to be declared using
`const`, like:

```
(f64.const 0)
(i32.const 0)
```

wah adds shortcuts, for both floating point and integer numbers:

```
0 ; integer i32
0.0 ; floating point f64
```

---

## Discussion

### Infix notation

Infix notation might be a controversial feature of wah. The initial purpose of
the language is to implement a port of
simple-statistics to WebAssembly, so it was a very mathematics heavy usecase.

I'd be happy to review & accept a PR that makes infix notation optional! It's
likely an opinionated feature, and I'd hate for this 'syntax sugar' level addition
to be a dealbreaker.

### Number types

Using the `.` character to differentiate between integer and floating point numbers
should be familiar to Python developers. I chose `i32` for the default integer type
because it is compatible with JavaScript's integer number range - `i64`, though
supported in WebAssembly, will get truncated if you try to move it into JavaScript.

I'd love to support `i64` numbers too, and am open to syntax that makes them possible:
right now I'm taking advantage of the [edn format](https://github.com/edn-format/edn)
for all the syntax representation, parsing, and output.
