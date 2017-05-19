(def i32-sources (set ['i32.const
                       'i32.load8_s
                       'i32.load8_u
                       'i32.load16_s
                       'i32.load16_u
                       'i32.load]))
(def i64-sources (set ['i64.const
                       'i64.load8_s
                       'i64.load8_u
                       'i64.load16_s
                       'i64.load16_u
                       'i64.load32_s
                       'i64.load32_u
                       'i64.load]))
(def f32-sources (set ['f32.const
                       'f32.load]))
(def f64-sources (set ['f64.const
                       'f64.load]))

(def wasm-const (set ['f32.const
                      'f64.const
                      'i64.const
                      'i32.const]))

(def infix-ops {'+ 'add
                '- 'sub
                '* 'mul
                '/ 'div
                '== 'eq
                '> 'gt
                '>= 'ge
                '< 'lt
                '<= 'le})

(def wasm-ops (set ['f32.add
                    'f32.sub
                    'f32.mul
                    'f32.div
                    'f32.copysign
                    'f32.ceil
                    'f32.eq
                    'f32.ne
                    'f32.lt
                    'f32.le
                    'f32.gt
                    'f32.ge
                    'f32.min
                    'f32.max
                    'f64.add
                    'f64.sub
                    'f64.mul
                    'f64.div
                    'f64.copysign
                    'f64.ceil
                    'f64.eq
                    'f64.ne
                    'f64.lt
                    'f64.le
                    'f64.gt
                    'f64.ge
                    'f64.min
                    'f64.max
                    'i32.add
                    'i32.sub
                    'i32.mul
                    'i32.div_s
                    'i32.div_u
                    'i32.rem_s
                    'i32.rem_u
                    'i32.and
                    'i32.or
                    'i32.xor
                    'i32.shl
                    'i32.shr_u
                    'i32.shr_s
                    'i32.rotl
                    'i32.rotr
                    'i32.eq
                    'i32.ne
                    'i32.lt_s
                    'i32.le_s
                    'i32.lt_u
                    'i32.le_u
                    'i32.gt_s
                    'i32.ge_s
                    'i32.gt_u
                    'i32.ge_u
                    'i32.clz
                    'i32.ctz
                    'i32.popcnt
                    'i32.eqz]))
