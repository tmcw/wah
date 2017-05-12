# wasm-hl

Shortcuts for wasm

| wasm-hl       | wasm                                      |
|---------------|-------------------------------------------|
| `(0 = 1)`     | `(set_local 0 (f64.const 0))`             |
| `(%$a + %$b)` | `(f64.add (get_local $a) (get_local $b))` |

* Adds infix operators: `*`, +, -, / (all compile to f64 right now)
* Supports (0 = 1) shortcut for `set_local`
* Supports bare numbers that become f64.const

## Installation

Download from http://example.com/FIXME.

## Usage

FIXME: explanation

    $ java -jar wasm-hl-0.1.0-standalone.jar [args]

## Options

FIXME: listing of options this app accepts.

## Examples

...

### Bugs

...

### Any Other Sections
### That You Think
### Might be Useful

## License

Copyright © 2017 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

;; From https://github.com/WebAssembly/design/blob/master/Semantics.md
;; `f32.add`: addition
;; `f32.sub`: subtraction
;; `f32.mul`: multiplication
;; `f32.div`: division
;; `f32.abs`: absolute value
;; `f32.neg`: negation
;; `f32.copysign`: copysign
;; `f32.ceil`: ceiling operator
;; `f32.floor`: floor operator
;; `f32.trunc`: round to nearest integer towards zero
;; `f32.nearest`: round to nearest integer, ties to even
;; `f32.eq`: compare ordered and equal
;; `f32.ne`: compare unordered or unequal
;; `f32.lt`: compare ordered and less than
;; `f32.le`: compare ordered and less than or equal
;; `f32.gt`: compare ordered and greater than
;; `f32.ge`: compare ordered and greater than or equal
;; `f32.sqrt`: square root
;; `f32.min`: minimum (binary operator); if either operand is NaN, returns NaN
;; `f32.max`: maximum (binary operator); if either operand is NaN, returns NaN
;; `f64.add`: addition
;; `f64.sub`: subtraction
;; `f64.mul`: multiplication
;; `f64.div`: division
;; `f64.abs`: absolute value
;; `f64.neg`: negation
;; `f64.copysign`: copysign
;; `f64.ceil`: ceiling operator
;; `f64.floor`: floor operator
;; `f64.trunc`: round to nearest integer towards zero
;; `f64.nearest`: round to nearest integer, ties to even
;; `f64.eq`: compare ordered and equal
;; `f64.ne`: compare unordered or unequal
;; `f64.lt`: compare ordered and less than
;; `f64.le`: compare ordered and less than or equal
;; `f64.gt`: compare ordered and greater than
;; `f64.ge`: compare ordered and greater than or equal
;; `f64.sqrt`: square root
;; `f64.min`: minimum (binary operator); if either operand is NaN, returns NaN
;; `f64.max`: maximum (binary operator); if either operand is NaN, returns NaN
;; `i32.add`: sign-agnostic addition
;; `i32.sub`: sign-agnostic subtraction
;; `i32.mul`: sign-agnostic multiplication (lower 32-bits)
;; `i32.div_s`: signed division (result is truncated toward zero)
;; `i32.div_u`: unsigned division (result is [floored](https://en.wikipedia.org/wiki/Floor_and_ceiling_functions))
;; `i32.rem_s`: signed remainder (result has the sign of the dividend)
;; `i32.rem_u`: unsigned remainder
;; `i32.and`: sign-agnostic bitwise and
;; `i32.or`: sign-agnostic bitwise inclusive or
;; `i32.xor`: sign-agnostic bitwise exclusive or
;; `i32.shl`: sign-agnostic shift left
;; `i32.shr_u`: zero-replicating (logical) shift right
;; `i32.shr_s`: sign-replicating (arithmetic) shift right
;; `i32.rotl`: sign-agnostic rotate left
;; `i32.rotr`: sign-agnostic rotate right
;; `i32.eq`: sign-agnostic compare equal
;; `i32.ne`: sign-agnostic compare unequal
;; `i32.lt_s`: signed less than
;; `i32.le_s`: signed less than or equal
;; `i32.lt_u`: unsigned less than
;; `i32.le_u`: unsigned less than or equal
;; `i32.gt_s`: signed greater than
;; `i32.ge_s`: signed greater than or equal
;; `i32.gt_u`: unsigned greater than
;; `i32.ge_u`: unsigned greater than or equal
;; `i32.clz`: sign-agnostic count leading zero bits (All zero bits are considered leading if the value is zero)
;; `i32.ctz`: sign-agnostic count trailing zero bits (All zero bits are considered trailing if the value is zero)
;; `i32.popcnt`: sign-agnostic count number of one bits
;; `i32.eqz`: compare equal to zero (return 1 if operand is zero, 0 otherwise)
