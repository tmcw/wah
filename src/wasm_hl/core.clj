(ns wasm-hl.core
  (:require [clojure.tools.cli :refer [cli]])
  (:require [clojure.tools.reader.edn :as edn])
  (:require [clojure.walk :as w])
  (:require [clojure.string :as str])
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

(defn local-shortcut
  "%$foo -> (get_local $foo)"
  [expr]
  (let [s (str expr)]
    (if (str/starts-with? s "%")
      (list 'get_local (symbol (subs s 1)))
      expr)))

(defn arrange-triple-list
  [expr]
  (let [maybe-op (second expr)]
    (if (contains? (into wasm-ops (keys op-shortcuts)) maybe-op)
      (list (maybe-op op-shortcuts maybe-op) (first expr) (last expr))
      expr)))

(defn maybe-expand-number
  [x]
  (if (number? x)
    (list 'f64.const x) x))

(defn expand-triple-list
  [expr]
  (if (= 'set_local (first expr))
    (list (first expr) (second expr) (maybe-expand-number (last expr)))
    (map maybe-expand-number expr)))

(defn expand-double-list
  [expr]
  (if (contains? wasm-const (first expr))
    expr
    (list (first expr) (maybe-expand-number (second expr)))))

(defn arrange-list
  [expr]
  ((condp = (count expr)
     2 expand-double-list
     3 (comp expand-triple-list arrange-triple-list)
     identity) expr))

(defn prefixexpression
  [expr]
  ((if (list? expr)
     arrange-list
     local-shortcut) expr))

(defn fixinfix
  "Move infix operators to prefix"
  [str]
  (let [expr (edn/read-string str)]
    (w/prewalk
     prefixexpression
     expr)))

(defn -main
  "compile wast-hl to wast"
  [& args]
  (let [[opts args banner] (cli args
                                ["-h" "--help" "Print this help"
                                 :default false :flag true])]
    (clojure.pprint/pprint
     (fixinfix (slurp (first args))))))
