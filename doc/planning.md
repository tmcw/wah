Herein lies ideas and thoughts for where this could go:

* The next big step is _arrays_ and dealing with data. Will we want to introduce
  new syntax sugar for that too?
* Right now it only supports floats, indicated by a decimal point, and ints,
  indicated by not. How could we make it simple to use all number types, so both
  f64 and f32?
* Since source and output is 1:1, can / should we support compilation from
  WAST to WAH too?
* Can we go a step beyond infix notation and also support associativity, so you can write
  `(1 * 2 + 6 + 3 / 4)` instead of `((1 * 2) + 6 + (3 / 4))`?
