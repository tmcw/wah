(ns wasm-hl.core
  (:require [clojure.tools.cli :refer [cli]])
  (:require [clojure.tools.reader.edn :as edn])
  (:require [clojure.walk :as w])
  (:require [clojure.string :as str])
  (:require [clojure.test :refer :all])
  (:gen-class))

(load "constants")

(with-test
  (defn arrange-triple-list
    "Rearrange (1 + 1) into (+ 1 1) and (0 = 1) into (set_local 0 1)"
    [expr]
    (let [maybe-op (second expr)]
      (cond
        (contains? (into wasm-ops (keys infix-ops)) maybe-op) (list maybe-op (first expr) (last expr))
        (= '= maybe-op) (list 'set_local (first expr) (last expr))
        :else expr)))
  (is (= (arrange-triple-list '(1 = 1)) '(set_local 1 1)))
  (is (= (arrange-triple-list '(1 1 1)) '(1 1 1))))

(with-test
  (defn maybe-expand-number
    "Literal number -> (float|int) constant"
    [x]
    (cond
      (float? x) (list 'f64.const x)
      (integer? x) (list 'i32.const x)
      :else x))
  (is (= (maybe-expand-number 1.0) '(f64.const 1.0)))
  (is (= (maybe-expand-number 1) '(i32.const 1)))
  (is (= (maybe-expand-number ()) '())))

(with-test
  (defn expand-triple-list
    "Expand triple lists, and support infix if they aren't set_local"
    [expr]
    (if (= 'set_local (first expr))
      (list (first expr) (second expr) (maybe-expand-number (last expr)))
      (map maybe-expand-number expr)))
  (is (= (list) (expand-triple-list (list))))
  (is (= '(set_local $foo (f64.const 0.0)) (expand-triple-list '(set_local $foo 0.0)))))

(with-test
  (defn expand-double-list
    "if a list isn't (f32.const 1), expand bare numbers in it into constants"
    [expr]
    (if (contains? wasm-const (first expr))
      expr
      (list (first expr) (maybe-expand-number (second expr)))))
  (is (= (expand-double-list '(f64.const 1)) '(f64.const 1)))
  (is (= (expand-double-list '(f64.abs 1.0)) '(f64.abs (f64.const 1.0)))))

(with-test
  (defn type-to-map
    "Is this statement a local declaration?"
    [param-counter x]
    (condp = (and (seq? x) (first x))
      'param (assoc {} (swap! param-counter inc) (second x))
      'local (assoc {} (second x) (last x))
      nil))

  (is (= '{$ent f64} (type-to-map (atom 0) '(local $ent f64)))))

(with-test
  (defn determine-type-map
    "From a top-level wasty tree, find all local statements"
    [expr]
    (let [param-counter (atom -1)]
      (or
       (apply merge
              (filter some?
                      (map (partial type-to-map param-counter)
                           (tree-seq seq? identity expr)))) {})))
  (is (= {} (determine-type-map '())))
  (is (= '{$foo f32} (determine-type-map '(local $foo f32))))
  (is (= '{0 f32 $foo f32} (determine-type-map '((param f32) (local $foo f32)))))
  (is (= '{0 f32 1 f64 $foo f32} (determine-type-map '((param f32) (param f64) (local $foo f32))))))

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

(defn get-type
  [expr]
  (get (meta expr) :type))

(with-test
  (defn resolve-infix-expression
    "Resolve (+ (i32.const 0) (i32.const 1)) -> (f32.const (i32.const 0) (i32.const 1))"
    [expr a b]
    (cond
      (nil? a) (throw (Exception. "nil type detected"))
      (= a b) (with-meta (concat
                          (list (symbol (str a "." (get infix-ops (first expr)))))
                          (drop 1 expr)) {:type a})
      :else (throw (Exception. (str "Types did not match: " a ", " b)))))
  (is (= '(i32.add (i32.const 0) (i32.const)) (resolve-infix-expression '(+ (i32.const 0) (i32.const)) 'i32 'i32))))

(with-test
  (defn assign-final-types
    "The first level of abstraction for the tree-walker. Arranges lists, expands shortcuts"
    [type-map expr]
    (condp = (seq? expr)
      false expr
      true (let [op (first expr)]
             (cond
               (= 'get_local op) (with-meta expr {:type (get type-map (second expr))})
               (contains? f64-sources op) (with-meta expr {:type 'f64})
               (contains? i64-sources op) (with-meta expr {:type 'i64})
               (contains? f32-sources op) (with-meta expr {:type 'f32})
               (contains? i32-sources op) (with-meta expr {:type 'i32})
               (contains? infix-ops op) (resolve-infix-expression expr
                                                                  (get-type (second expr))
                                                                  (get-type (last expr)))
               :else expr))))
  (is (= '(get_local 0) (assign-final-types {0 'f32} '(get_local 0))))
  (is (= {:type 'f32} (meta (assign-final-types {0 'f32} '(get_local 0)))))
  (is (= {:type 'i32} (meta (assign-final-types {} '(i32.const 0)))))
  (is (= {:type 'f32} (meta (assign-final-types {} '(f32.const 0)))))
  (is (= {:type 'f64} (meta (assign-final-types {} '(f64.const 0)))))
  (is (= {:type 'i32} (meta (assign-final-types {'$f 'i32} '(get_local $f))))))

(with-test
  (defn wasm-hl-to-wasm
    "Transform a wasm hl edn to a wasm edn"
    [expr]
    (let [arranged-tree (w/prewalk
                         transform-tree-node
                         expr)
          type-map (determine-type-map arranged-tree)]
      (w/postwalk
       (partial assign-final-types type-map)
       arranged-tree)))
  (is (= '(i32.add (i32.const 0) (i32.const 0)) (wasm-hl-to-wasm '(0 + 0))))
  (is (= '(i32.add (i32.const 0) (i32.add (i32.const 0) (i32.const 1))) (wasm-hl-to-wasm '(0 + (0 + 1)))))
  (is (= '(f64.add (f64.const 0.0) (f64.const 1.0)) (wasm-hl-to-wasm '(0.0 + 1.0))))
  (is (= '(func (local $foo f64) (f64.add (f64.const 0.0) (get_local $foo))) (wasm-hl-to-wasm '(func (local $foo f64) (0.0 + %$foo)))))
  (is (= 'i32 (get (meta (second (wasm-hl-to-wasm '(0 + 0)))) :type))))

(defn -main
  "compile wast-hl to wast"
  [& args]
  (let [[opts args banner] (cli args
                                ["-h" "--help" "Print this help"
                                 :default false :flag true])]
    (clojure.pprint/pprint
     (wasm-hl-to-wasm (edn/read-string (slurp (first args)))))))
