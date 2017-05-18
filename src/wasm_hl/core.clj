(ns wasm-hl.core
  (:require [clojure.tools.cli :refer [cli]])
  (:require [clojure.tools.reader.edn :as edn])
  (:require [clojure.walk :as w])
  (:require [clojure.string :as str])
  (:require [clojure.test :refer :all])
  (:gen-class))

(def wasm-const (set ['f32.const
                      'f64.const
                      'i32.const]))

(def op-shortcuts {'+ 'f64.add
                   '- 'f64.sub
                   '* 'f64.mul
                   '/ 'f64.div
                   '== 'f64.eq
                   '> 'f64.gt
                   '>= 'f64.ge
                   '< 'f64.lt
                   '<= 'f64.le
                   '= 'set_local})
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

(with-test
  (defn arrange-triple-list
    [expr]
    (let [maybe-op (second expr)]
      (if (contains? (into wasm-ops (keys op-shortcuts)) maybe-op)
        (list (maybe-op op-shortcuts maybe-op) (first expr) (last expr))
        expr)))
  (is (= (arrange-triple-list '(1 = 1)) '(set_local 1 1)))
  (is (= (arrange-triple-list '(1 == 1)) '(f64.eq 1 1)))
  (is (= (arrange-triple-list '(1 1 1)) '(1 1 1))))

(with-test
  (defn maybe-expand-number
    [x]
    (if (number? x)
      (list 'f64.const x) x))
  (is (= (maybe-expand-number 1) '(f64.const 1)))
  (is (= (maybe-expand-number ()) '())))

(with-test
  (defn expand-triple-list
    "Expand triple lists, and support infix if they aren't set_local"
    [expr]
    (if (= 'set_local (first expr))
      (list (first expr) (second expr) (maybe-expand-number (last expr)))
      (map maybe-expand-number expr)))
  (is (= (list) (expand-triple-list (list))))
  (is (= '(set_local $foo (f64.const 0)) (expand-triple-list '(set_local $foo 0)))))

(with-test
  (defn expand-double-list
    "if a list isn't (f32.const 1), expand bare numbers in it into constants"
    [expr]
    (if (contains? wasm-const (first expr))
      expr
      (list (first expr) (maybe-expand-number (second expr)))))
  (is (= (expand-double-list '(f64.const 1)) '(f64.const 1)))
  (is (= (expand-double-list '(f64.abs 1)) '(f64.abs (f64.const 1)))))

(with-test
  (defn type-to-map
    "Is this statement a local declaration?"
    [param-counter x]
    (condp = (and (seq? x) (first x))
      'param (assoc nil (swap! param-counter inc) (second x))
      'local (assoc nil (second x) (last x))
      nil))

  (is (= '{$ent f64} (type-to-map '(local $ent f64)))))

(with-test
  (defn select-type-statements
    "From a top-level wasty tree, find all local statements"
    [expr]
    (let [param-counter (atom -1)]
      (apply merge
             (filter some?
                     (map (partial type-to-map param-counter)
                          (tree-seq seq? identity expr))))))
  (is (= (list) (select-local-statements '()))))

(with-test
  (defn collect-explicit-types
    "Given a program, find all local declarations and generate a map of their types"
    [expr]
    (reduce
     (fn [types x]
       (assoc types (second x) (last x)))
     {}
     (select-type-statements expr)))
  (is (= (collect-explicit-types '((local $a f32))) '{$a f32})))

(defn arrange-list
  "Branch based on 2 (expansion) and 3 (expansion & support infix) sized lists"
  [expr]
  ((condp = (count expr)
     2 expand-double-list
     3 (comp expand-triple-list arrange-triple-list)
     identity) expr))

(with-test
  (defn local-shortcut
    "%$foo -> (get_local $foo)"
    [expr]
    (let [s (str expr)]
      (if (str/starts-with? s "%")
        (list 'get_local (symbol (subs s 1)))
        expr)))
  (is (= (local-shortcut '%$foo) '(get_local $foo)))
  (is (= (local-shortcut '$foo) '$foo)))

(defn transform-tree-node
  "The first level of abstraction for the tree-walker. Arranges lists, expands shortcuts"
  [expr]
  ((if (list? expr)
     arrange-list
     local-shortcut) expr))

(defn wasm-hl-to-wasm
  "Transform a wasm hl edn to a wasm edn"
  [expr]
  (w/prewalk
   transform-tree-node
   expr))

(defn -main
  "compile wast-hl to wast"
  [& args]
  (let [[opts args banner] (cli args
                                ["-h" "--help" "Print this help"
                                 :default false :flag true])]
    (clojure.pprint/pprint
     (wasm-hl-to-wasm (edn/read-string (slurp (first args)))))))
