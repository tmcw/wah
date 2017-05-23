(module
  (func $a
  (param f64)
  (result f64)
  (return (f64.mul (f64.const 0.0) (get_local 0))))
  (func $b
  (param i32)
  (result i32)
  (return (i32.mul_s (i32.const 0) (get_local 0))))
  )
