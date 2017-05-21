(ns wah.core
  (:require [clojure.tools.cli :refer [cli]])
  (:require [clojure.tools.reader.edn :as edn])
  (:require [clojure.walk :as w])
  (:require [clojure.string :as str])
  (:require [clojure.test :refer :all]) (:gen-class))

(def i32-sources (set ['i32.const
                       'i32.load8_s
                       'i32.load8_u
                       'i32.load16_s
                       'i32.load16_u
                       'i32.load
                       'i32.abs
                       'i32.wrap/i64
                       'i32.trunc_s/f32
                       'i32.trunc_s/f64
                       'i32.trunc_u/f32
                       'i32.trunc_u/f64
                       'i32.reinterpret/f32]))
(def i64-sources (set ['i64.const
                       'i64.load8_s
                       'i64.load8_u
                       'i64.load16_s
                       'i64.load16_u
                       'i64.load32_s
                       'i64.load32_u
                       'i64.load
                       'i64.abs
                       'i64.extend_s/i32
                       'i64.extend_u/i32
                       'i64.trunc_s/f32
                       'i64.trunc_s/f64
                       'i64.trunc_u/f32
                       'i64.trunc_u/f64
                       'i64.reinterpret/f64]))
(def f32-sources (set ['f32.const
                       'f32.load
                       'f32.abs
                       'f32.demote/f64
                       'f32.convert_s/i32
                       'f32.convert_s/i64
                       'f32.convert_u/i32
                       'f32.convert_u/i64
                       'f32.reinterpret/i32]))
(def f64-sources (set ['f64.const
                       'f64.load
                       'f64.abs
                       'f64.promote/f32
                       'f64.convert_s/i32
                       'f64.convert_s/i64
                       'f64.convert_u/i32
                       'f64.convert_u/i64
                       'f64.reinterpret/i64]))

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

(def all-ops (into wasm-ops (keys infix-ops)))

(with-test
  (defn merge-meta
    [obj m]
    (with-meta obj (merge (meta obj) m)))
  (is (= {:a 1 :b 2} (meta (merge-meta (with-meta () {:a 1}) {:b 2})))))

(with-test
  (defn arrange-triple-list
    "Rearrange (1 + 1) into (+ 1 1) and (0 = 1) into (set_local 0 1)"
    [expr]
    (let [maybe-op (second expr)]
      (cond
        (contains? all-ops maybe-op) (list maybe-op (first expr) (last expr))
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
  (defn inherit-meta
    "When we create new objects, give them the metadata of the source they're derived from"
    [from to]
    (if-let [from-meta (meta from)]
      (merge-meta to from-meta)
      to))
  (is (= {:a 1} (meta (inherit-meta (with-meta [] {:a 1}) [])))))

(with-test
  (defn expand-triple-list
    "Expand triple lists, and support infix if they aren't set_local"
    [expr]
    (condp = (first expr)
      'set_local (list (first expr) (second expr) (maybe-expand-number (last expr)))
      (map maybe-expand-number expr)))
  (is (= (list) (expand-triple-list (list))))
  (is (= '(set_local $foo (f64.const 0.0)) (expand-triple-list '(set_local $foo 0.0)))))

(with-test
  (defn expand-double-list
    "if a list isn't (f32.const 1) or (get_local 1), expand bare numbers in it into constants"
    [expr]
    (cond
      (contains? wasm-const (first expr)) expr
      (= 'get_local (first expr)) expr
      :else (list (first expr) (maybe-expand-number (second expr)))))
  (is (= (expand-double-list '(get_local 1)) '(get_local 1)))
  (is (= (expand-double-list '(f64.const 1)) '(f64.const 1)))
  (is (= (expand-double-list '(f64.abs 1.0)) '(f64.abs (f64.const 1.0)))))

(with-test
  (defn get-return-type
    "Get return type"
    [expr]
    (when-let [return-value (first (filter (fn [x] (and (seq? x) (= 'result (first x)))) (seq expr)))]
      (second return-value)))
  (is (= (get-return-type '(func (export "errorFunction") (param f64) (result f64))) 'f64))
  (is (= (get-return-type '(func (export "errorFunction") (param f64) (result f32))) 'f32)))

(with-test
  (defn type-to-map
    "Is this statement a local declaration?"
    [x]
    (condp = (and (seq? x) (first x))
      'param (apply merge (map-indexed
                           (fn [idx, kind]
                             (assoc {} idx kind))
                           (drop 1 x)))
      'local (assoc {} (second x) (last x))
      'func (if (symbol? (second x))
              (assoc {} (second x) (get-return-type x))
              {})
      nil))

  (is (= '{$foo f32} (type-to-map '(func $foo (result f32)))))
  (is (= '{$ent f64} (type-to-map '(local $ent f64))))
  (is (= '{0 f64 1 f32 2 i32} (type-to-map '(param f64 f32 i32)))))

(with-test
  (defn determine-type-map
    "From a top-level wasty tree, find all local statements"
    [expr]
    (let [param-counter (atom -1)]
      (or
       (->>
        (tree-seq seq? identity expr)
        (map type-to-map)
        (filter some?)
        (apply merge)) {})))
  (is (= {} (determine-type-map '())))
  (is (= '{$foo f32} (determine-type-map '(local $foo f32))))
  (is (= '{0 f32 $foo f32} (determine-type-map '((param f32) (local $foo f32)))))
  (is (= '{0 f32 1 f64 $foo f32} (determine-type-map '((param f32 f64) (local $foo f32))))))

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
    (let [s (str expr)
          bare-identifier (subs s 1)]
      (cond
        (str/starts-with? s "%$") (list 'get_local (symbol bare-identifier))
        (str/starts-with? s "%") (list 'get_local (Integer/parseInt bare-identifier))
        :else expr)))
  (is (= (local-shortcut '%$foo) '(get_local $foo)))
  (is (= (local-shortcut '$foo) '$foo)))

(defn transform-tree-node
  "The first level of abstraction for the tree-walker. Arranges lists, expands shortcuts"
  [expr]
  (inherit-meta expr ((if (seq? expr)
                        arrange-list
                        local-shortcut) expr)))

(with-test
  (defn get-type
    [expr]
    (:type (meta expr)))
  (is (= 'f64 (get-type (merge-meta '(f64.const 0) {:type 'f64}))))
  (is (= nil (get-type '(f64.const 0)))))

(with-test
  (defn resolve-infix-expression
    "Resolve (+ (i32.const 0) (i32.const 1)) -> (f32.const (i32.const 0) (i32.const 1))"
    [expr a b]
    (cond
      (nil? a) (throw (Exception. "nil type detected"))
      (= a b) (merge-meta (concat
                           (list (symbol (str a "." (get infix-ops (first expr)))))
                           (drop 1 expr)) {:type a})
      ;; TODO: track type output of other functions
      ;; :else (throw (Exception. (str "Types did not match: " a ", " b " in the expression " (clojure.pprint/write expr :stream nil))))))
      :else expr))
  (is (= '(i32.add (i32.const 0) (i32.const)) (resolve-infix-expression '(+ (i32.const 0) (i32.const)) 'i32 'i32))))

(with-test
  (defn assign-final-types
    "The first level of abstraction for the tree-walker. Arranges lists, expands shortcuts"
    [type-map expr]
    (condp = (seq? expr)
      false expr
      true (let [op (first expr)]
             (cond
               (= 'get_local op) (merge-meta expr {:type (get type-map (second expr))})
               (= 'call op) (merge-meta expr {:type (get type-map (second expr))})
               (contains? f64-sources op) (merge-meta expr {:type 'f64})
               (contains? i64-sources op) (merge-meta expr {:type 'i64})
               (contains? f32-sources op) (merge-meta expr {:type 'f32})
               (contains? i32-sources op) (merge-meta expr {:type 'i32})
               (contains? infix-ops op) (resolve-infix-expression expr
                                                                  (get-type (second expr))
                                                                  (get-type (last expr)))
               :else expr))))
  (is (= '(get_local 0) (assign-final-types {0 'f32} '(get_local 0))))
  (is (= 'f32 (get-type (assign-final-types '{$foo f32} '(call $foo)))))
  (is (= 'f32 (get-type (assign-final-types {0 'f32} '(get_local 0)))))
  (is (= 'i32 (get-type (assign-final-types {} '(i32.const 0)))))
  (is (= 'f32 (get-type (assign-final-types {} '(f32.const 0)))))
  (is (= 'f64 (get-type (assign-final-types {} '(f64.const 0)))))
  (is (= 'i32 (get-type (assign-final-types {'$f 'i32} '(get_local $f))))))

(with-test
  (defn wah-to-wasm
    "Transform a wasm hl edn to a wasm edn"
    [expr]
    (let [arranged-tree (w/prewalk
                         transform-tree-node
                         expr)
          type-map (determine-type-map arranged-tree)]
      (w/postwalk
       (partial assign-final-types type-map)
       arranged-tree)))
  (is (= '(i32.add (i32.const 0) (i32.const 0)) (wah-to-wasm '(0 + 0))))
  (is (= '(i32.add (i32.const 0) (i32.add (i32.const 0) (i32.const 1))) (wah-to-wasm '(0 + (0 + 1)))))
  (is (= '(f64.add (f64.const 0.0) (f64.const 1.0)) (wah-to-wasm '(0.0 + 1.0))))
  (is (= '(func (local $foo f64) (f64.add (f64.const 0.0) (get_local $foo))) (wah-to-wasm '(func (local $foo f64) (0.0 + %$foo)))))
  (is (= 'i32 (get (meta (second (wah-to-wasm '(0 + 0)))) :type)))
  (is (= 'i32 (get (meta (second (wah-to-wasm '(0 + 0)))) :type))))

(with-test
  (defn convert-file
    [st]
    (wah-to-wasm (edn/read-string (slurp st))))
  (is (= (edn/read-string (slurp "fixture/1.wast"))  (convert-file "fixture/1.wah")))
  (is (= (edn/read-string (slurp "fixture/2.wast"))  (convert-file "fixture/2.wah")))
  (is (= (edn/read-string (slurp "fixture/3.wast"))  (convert-file "fixture/3.wah")))
  (is (= (edn/read-string (slurp "fixture/4.wast"))  (convert-file "fixture/4.wah")))
  (is (thrown? Exception (convert-file "fixture/e1.wah"))))

(defn -main
  "compile wast-hl to wast"
  [& args]
  (let [[opts args banner] (cli args
                                ["-h" "--help" "Print this help"
                                 :default false :flag true])]
    (clojure.pprint/pprint
     (convert-file (first args)))))
