(ns yang.java
  ; (:require [yang.lang :as l])      ;; AOT'ed as well hence blows up the jar size. so hold off for now
  (:import [clojure.lang Keyword IFn]
           [java.util Map]
           [java.util.function Function BiFunction Consumer BiConsumer]))

(gen-class
  :name tolitius.Yang
  :methods [^{:static true} [mapToEdn [java.util.Map] java.util.Map]
            ; ^{:static true} [fmk [java.util.Map clojure.lang.IFn] java.util.Map]
            ; ^{:static true} [fmv [java.util.Map clojure.lang.IFn] java.util.Map]
            ^{:static true} [toFun [Object] clojure.lang.IFn]])

(defn map->edn
  ;; from https://github.com/juxt/crux/blob/ca9e15d2119aef5e4605363a0979793183bea440/crux-kafka-connect/src/crux/kafka/connect.clj#L24-L30
  "often needed for to pass EDN maps to Clojure libs from Java
    => (def m (java.util.HashMap. {\"crux.id/foo\" {\"crux.db/bar\" \"baz\"} \"crux.asnwer/life\" 42}))
    => (map->edn m)
    {:crux.id/foo #:crux.db{:bar \"baz\"}, :crux.asnwer/life 42}
  "
  [m]
  (->> (for [[k v] m]
         [(keyword k)
          (if (instance? Map v)
            (map->edn v)
            v)])
       (into {})))

(defn jfun->fun
  "convert a Java function to a Clojure fn
   so it can be later composed together with other Clojure functions"
  [fun]
  (letfn [(instance-of [types v]
            (some #(instance? % v) types))]
    (condp instance-of fun
      #{Function} (fn [x] (.apply fun x))
      #{BiFunction} (fn [x y] (.apply fun x y))
      #{Consumer} (fn [x] (.accept fun x))
      #{BiConsumer} (fn [x y] (.accept fun x y))
      :not-a-function)))


;; a Java friendly faces

(defn -mapToEdn [m]
  (map->edn m))

(defn -toFun [fun]
  (jfun->fun fun))

; (defn -fmk [m f]
;   (l/fmk m f))

; (defn -fmv [m f]
;   (l/fmv m f))
