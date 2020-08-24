(ns yang.java
  [:import [java.util Map]])

(gen-class
  :name tolitius.Yang
  :methods [^{:static true} [mapToEdn [java.util.Map] java.util.Map]])

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

;; a Java friendly face
(defn -mapToEdn [m]
  (map->edn m))
