# wah

Shortcuts for wasm

| wasm-hl       | wasm                                      |
|---------------|-------------------------------------------|
| `(0 = 1)`     | `(set_local 0 (f64.const 0))`             |
| `(%$a + %$b)` | `(f64.add (get_local $a) (get_local $b))` |
| `(0 + 1)` | `(i32.add (i32.const 0) (i32.const 1))` |
| `(0.0 + 1.0)` | `(f64.add (f64.const 0) (f64.const 1))` |

* Adds infix operators: `*, +, -, /, ==, >, <, >=, <=`
* Supports (0 = 1) shortcut for `set_local`
* Supports bare numbers that become f64.const if they have a decimal point, and
  i32.const if they don't.
* Supports type inference by walking up the tree, paying attention to param
  and local types.

## Installation


## Usage

FIXME: explanation

    $ java -jar wasm-hl-0.1.0-standalone.jar [args]

## Options

## Examples

...

### Bugs

...

## License

Copyright © 2017 Tom MacWright

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
