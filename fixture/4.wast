(module
  (func
    (export "f")
    (local $a f32)
    (param f64)
    (result f64)
    (set_local $a (f64.const 0.0))
    (get_local $a)))
