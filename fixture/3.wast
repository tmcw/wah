(module
  (func
    (export "f")
    (param f64)
    (result f64)
    (f64.div
      (f64.const 1.0)
      (f64.add
	(f64.const 1.0)
	(f64.mul
	  (f64.const 0.5)
	  (f64.abs
	    (get_local 0)))))))
