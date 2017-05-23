(module
  (func
    (export "f")
    (param i32 i32)
    (result i32)
    (i32.add_s
      (get_local 0)
      (get_local 1))))
