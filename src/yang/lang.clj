(ns yang.lang
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as s]
            [clojure.set :as sets]
            [clojure.pprint :as pp]
            [clojure.walk :as walk])
  (:import [java.util UUID]
           [java.io ByteArrayOutputStream ByteArrayInputStream Reader]
           [java.util.zip GZIPOutputStream GZIPInputStream]
           [java.util.concurrent Future CompletableFuture]
           [java.util.function Supplier Function]))

(defonce zero-uuid (UUID. 0 0))

(defn suuid? [s]
  (try (UUID/fromString s)
       true
       (catch Exception e)))

(defn uuid []
  (UUID/randomUUID))

(defn squuid
  "tasty sequential UUIDs
   from: https://github.com/clojure-cookbook/clojure-cookbook/blob/1b3754a7f4aab51cc9b254ea102870e7ce478aa0/01_primitive-data/1-24_uuids.asciidoc"
  []
  (let [uuid      (UUID/randomUUID)
        time      (System/currentTimeMillis)
        secs      (quot time 1000)
        lsb       (.getLeastSignificantBits uuid)
        msb       (.getMostSignificantBits uuid)
        timed-msb (bit-or (bit-shift-left secs 32)
                          (bit-and 0x00000000ffffffff msb))]
    (java.util.UUID. timed-msb lsb)))

(defn str->uuid [s]
  (when (seq s)
    (UUID/fromString s)))

(defn fmv
  "apply f to each value v of map m"
  [m f]
  (into {}
        (for [[k v] m]
          [k (f v)])))

(defn fmk
  "apply f to each key k of map m"
  [m f]
  (into {}
        (for [[k v] m]
          [(f k) v])))

(defn rfmk
  "recursively apply f to each key k of map m"
  [m f]
  (let [fun (fn [[k v]]
              (if (or (string? k)
                      (simple-keyword? k))
                [(f k) v]
                [k v]))]
    (walk/postwalk (fn [x]
                     (if (map? x)
                       (into {}
                             (map fun x)) x))
                   m)))

(defn rfmv
  "recursively apply f to each value v of map m"
  [m f]
  (into {}
        (map (fn [[k v]]
               [k (if (map? v)
                    (rfmv v f)
                    (f v))]))
        m))

(defn assoc-if
    "associates key/value pairs into the map `m` if the values are not nil
     if a value is nil, the corresponding key is not added to the map

     args:
       m:   the initial map
       kvs: a sequence of alternating keys and values (e.g., :k1 v1 :k2 v2)

     returns:
       a new map with the key/value pairs added, excluding pairs where the value is nil

     example:
       => (def m {:moo 42})

       => (assoc-if m :zoo 12)
       {:moo 42, :zoo 12}

       ;; nil is not a value: hence skipped
       => (assoc-if m :zoo 12 :moo nil)
       {:moo 42, :zoo 12}

       ;; false is a value: hence not skipped
       => (assoc-if m :zoo 12 :moo nil :boo false)
       {:moo 42, :zoo 12, :boo false}"
  [m & kvs]
  (reduce (fn [acc [k v]]
            (if (some? v)
              (assoc acc k v)
              acc))
          m
          (partition 2 kvs)))

(defn filter-kv
  "Takes a map, filters on (f k v) for each map entry, returns a map."
  [m f]
  {:pre [(map? m)]}
  (persistent!
   (reduce (fn [acc [k v]]
             (if (f k v)
               (conj! acc [k v])
               acc))
           (transient {})
           m)))

(defn map->keys-as-path
  "Takes a nested map and returns a flat map where each key is the path of the original map.
   => (map->path-keys {:a {:b 'abc'}})
   {[:a :b] 'abc'}"
  ([m]
   (map->keys-as-path m []))
  ([m path]
   {:pre [(map? m)]}
   (->> m
        (reduce-kv (fn [acc k v]
                     (if (and (map? v)
                              (seq v))
                       (merge acc (map->keys-as-path v (conj path k)))
                       (assoc acc (conj path k) v)))
                   {}))))

(defn jcoll? [x]
  (instance? java.util.Collection x))

(defn str=
  "case insensitive"
  [s v]
  (= (s/lower-case (or s ""))
     (s/lower-case (or v ""))))

(defn str-val [v]
  (if v
    (str v)
    ""))

(defn sval? [v]
  (and (string? v)
       (not (s/blank? v))))

(defn lower-case [s]
  (when (string? s)
    (s/lower-case s)))

(defn upper-case [s]
  (when (string? s)
    (s/upper-case s)))

(defn trim [s]
  (if (string? s)
    (s/trim s)))

(defn to-long [n]
  (if (number? n)
    (long n)))

(defn to-double [n]
  (if (number? n)
    (double n)))

(defn parse-long [l]
  (try (Long/valueOf l)
       (catch Exception e)))

(defn parse-double [d]
  (try (Double/valueOf d)
       (catch Exception e)))

(defn parse-number [n]
  (let [s (str n)]
    (if (re-find #"^-?\d+\.?\d*$" s)
      (edn/read-string s))))

(defn ranged-rand [start end]
  (+ start (long (rand (- end start)))))

(defn plus [& xs]
  (apply + (remove nil? xs)))

(defn value? [v]
  (or (number? v)
      (uuid? v)
      (seq v)))

(defn value [v]
  (when (value? v)
    v))

(defn none? [v]
  (cond
    (= v "") true
    (coll? v) (empty? v)
    (nil? v) true))

(defn one-if-none [n]
  (if (or (not n)
          (zero? n))
    1
    n))

(defn remove-keys-by-prefix
  "removes map entries by a key prefix

   => (def m {:a 42 :foo.bar/zoo 39 :moo 82 :oop.oop/noop :noop})

   => (y/remove-keys-by-prefix m :a)
      {:foo.bar/zoo 39, :moo 82, :oop.oop/noop :noop}

   => (y/remove-keys-by-prefix m :foo)
      {:a 42, :moo 82, :oop.oop/noop :noop}"
  [m prefix]
  (if-not (and prefix m)
    m
    (->> m
         (remove (fn [[k v]]
                   (s/starts-with? (str k)
                                   (str prefix))))
         (into {}))))

(defn remove-key-ns
  ([m]
   (fmk m name))
  ([m kns]
   (->> (filter (fn [[k v]] (not= (namespace k)
                                  (name kns))) m)
        (into {}))))

(defn extract-key-ns
  "=> (extract-key-ns {:a/one :a-one :a/two :a-two :b/one :b-one :b/two :b-two}
                      :a)

      {:a {:one :a-one
           :two :a-two}}"
  [m kns]
  {kns (reduce-kv (fn [a k v]
                    (if (= (namespace k)
                           (name kns))
                      (assoc a (-> k name keyword)          ;; :foo/bar -> :bar
                               v)
                      a))
                  {}
                  m)})

(defn- unbrace
  "=> (unbrace \"I choose the {{choice}} pill\" :choice \"red\")

   \"I choose the red pill\""
  [this k with]
  (let [wrap (fn [x] (str "\\{\\{" x "?\\}\\}"))
        in-braces (-> k name wrap re-pattern)]
    (s/replace this in-braces with)))

(defn rebrace
  "=> (rebrace \"{{child}}, I am your {{parent}}\"
               {:child \"Luke\" :parent \"father\"})

   \"Luke, I am your father\""
  [this with]
  (reduce-kv (fn [braced k v]
               (unbrace braced k v))
    this
    with))

(defn replace-in-kw
  "=> (replace-in-kw :foo-bar-baz \"bar\" \"zoo\")
      :foo-zoo-baz"
  [kw from to]
  (-> kw
      name
      (s/replace from to)
      keyword))

(defn replace-in-keys [m from to]
  (into {}
        (for [[k v] m]
          [(replace-in-kw k from to) v])))

(defn replace-line [path from to]
  (->> (slurp path)
       s/split-lines
       (map #(s/replace-first % from to))
       (s/join "\n")
       (spit path)))

(defn dash-keys
  ([m]
   (dash-keys "_" m))
  ([from m]
   (replace-in-keys m from "-")))

(defn underscore-keys
  ([m]
   (underscore-keys "-" m))
  ([from m]
   (replace-in-keys m from "_")))

(defn dash-to-camel [xs]
  (s/replace xs #"-(\w)"
             #(s/upper-case (second %))))

(defn camel-to-dash [k]
  (->> (map s/lower-case
            (s/split (name k) #"(?=[A-Z])"))
       (s/join "-")
       keyword))

(defn and->
  ([] true)
  ([x] x)
  ([x & [f & fs]]
   (let [r (f x)]
     (if (and r (seq fs))
       (apply and-> x fs)
       r))))

(defn pretty [& args]
  (with-out-str
    (apply pp/pprint args)))

(defn positive? [xs]
  (every? #(and-> % number? pos?)
          xs))

(defn pos-number? [n]
  (and n
       (number? n)
       (pos? n)))

(defn capitalize [xs]
  (when (seq xs)
    (->> (s/split xs #" ")
         (map (comp s/capitalize s/lower-case))
         (s/join #" "))))

(defn to-alpha-num [xs]
  (->> (re-seq #"[a-zA-Z0-9]" xs)
       (apply str)))

(defn append-if-absent
  ([v xs]
   (append-if-absent v xs ", "))
  ([v xs delim]
   (if-not (re-find (re-pattern (str "(?i)"                 ;; case insensitive
                                     (to-alpha-num v))) xs)
     (str xs delim v)
     xs)))

(defn seq->in-params
  ;; convert seqs to IN params: i.e. [1 "2" 3] => "('1','2','3')"
  [xs]
  (as-> xs $
        (map #(str "'" % "'") $)
        (s/join "," $)
        (str "(" $ ")")))

(defn remove-empty
  "=> (remove-empty {:a nil :b \"\" :c [] :d 42})
      {:d 42}"
  [m]
  (into {}
        (for [[k v] m]
          (when-not (none? v)
            [k v]))))

(defn remove-empty-keys
  "=> (remove-empty-keys {:a 42 nil 37 \"\" 28 [] 12 {} 7 :b 34})
      {:a 42 :b 34}"
  [m]
  (into {}
        (for [[k v] m]
          (when-not (none? k)
            [k v]))))

(defn props->map
  "java.util.Properties to map"
  [props]
  (into {}
        (for [[k v] props]
          [(-> k s/trim keyword)
           (s/trim v)])))

(defn ^CompletableFuture future->completable-future [^Future fut pool]
  (CompletableFuture/supplyAsync
    (reify Supplier
      (get [_]
        (try
          (.get fut)
          (catch Exception e
            (throw (ex-info "error in future->completable-future" {} e))))))
    pool))

(defn then-compose [^CompletableFuture cf f]
  (.thenCompose cf (reify Function (apply [_ x] (f x)))))

(defn then-apply [^CompletableFuture cf f]
  (.thenApply cf (reify Function (apply [_ x] (f x)))))

(defn group-by-ns
  "=> (group-by-ns {:a/one :a-one :b/one :b-one :a/two :a-two :b/two :b-two})

      {:a {:one :a-one, :two :a-two}
       :b {:one :b-one, :two :b-two}}"
  [m]
  (-> (group-by (comp namespace key) m)
      (fmk keyword)
      (fmv #(fmk % (comp keyword name)))))

(defn group-by-first
  "takes a seq of 2 element tuples
   and groups them by the first (key) element.
    (group-by-first [[1 2][1 4][2 3][2 7]])
      {1 (4 2), 2 (7 3)}"
  [m]
  (reduce (fn [m [k v]]
            (update m k conj v))
          {}
          m))

(defn index-by
  "given a sequence of maps
   index them by the key

   (!) duplicate/nil keys: latest wins (!)

   => (index [{:a 123, :b \"first\"}
              {:a 234, :b \"second\"}
              {:a 345, :b \"third\"}]  ;; seq of maps
              :a)                      ;; key to index by

      {123 {:a 123, :b \"first\"},
       234 {:a 234, :b \"second\"},
       345 {:a 345, :b \"third\"}}
  "
  [by xs]
  (reduce (fn [idx m]
            (assoc idx (by m) m))
          {}
          xs))

(defn deep-merge-with
  "like merge-with, but merges maps recursively, appling the given fn
   only when there's a non-map at a particular level.
   (deepmerge + {:a {:b {:c 1 :d {:x 1 :y 2}} :e 3} :f 4}
                {:a {:b {:c 2 :d {:z 9} :z 3} :e 100}})
   -> {:a {:b {:z 3, :c 3, :d {:z 9, :x 1, :y 2}}, :e 103}, :f 4}"
  [f & maps]
  (apply
    (fn m [& maps]
      (if (every? map? maps)
        (apply merge-with m maps)
        (apply f maps)))
    maps))

(defn remove-deep [key-set data]
  "from https://stackoverflow.com/a/52041784/114359

  Remove every key in the key-set from the data, deeply throughout its structure."
  (clojure.walk/prewalk
    (fn [node]
      (if (map? node)
        (apply dissoc node key-set)
        node))
    data))

(defn merge-maps [& m]
  (apply deep-merge-with (fn [_ v] v) m))

(defn dissoc-in
  "from https://github.com/clojure/core.incubator

   dissociates an entry from a nested associative structure returning a new
   nested structure. keys is a sequence of keys. Any empty maps that result
   will not be present in the new structure."
  [m [k & ks :as keys]]
  (if ks
    (if-let [nextmap (get m k)]
      (let [newmap (dissoc-in nextmap ks)]
        (if (seq newmap)
          (assoc m k newmap)
          (dissoc m k)))
      m)
    (dissoc m k)))

(defn freq-map [m by]
  "frequences but for seq of maps (ignores nil values)
   => (def m [{:a 42, :b 34} {:a 31, :b 10} {:a 42, :c 0} {:a 31, :b 3} {:a 42, :b 5} {:a nil, :b 10} {:a 30, :r 5}])
   => (freq-map m :a)
      {42 3, 31 2, 30 1}"
  (when (seq m)
    (reduce (fn [a e]
              (let [k (e by)]
                (if k
                  (assoc a k (inc (get a k 0)))
                  a)))
            {} m)))

(defn cconj
  "circular conj
   adds an item into a collection, droping the last element
   in case a collection count is greater than a :size.
   (def l '(10 11 12 13 14 15 16 17))
   (take 5 (iterate #(cconj % (rand-int 10) :size 10) l))
   =>
   ((10 11 12 13 14 15 16 17)
    (3 10 11 12 13 14 15 16 17)
    (9 3 10 11 12 13 14 15 16 17)
    (5 9 3 10 11 12 13 14 15 16)
    (2 5 9 3 10 11 12 13 14 15))"
  [xs item & {:keys [size]}]
  (let [c (count xs)
        s (or size c)
        l (-> (take s xs)
              (conj item))]
    (if (>= c s)
      (drop-last l)
      l)))

(defn rm-sort
  "reverse map sort (by key)"
  [m]
  (sort-by first #(compare %2 %1) m))

(defn join [xs1 xs2 on]
  "joins two seqs of maps \"on\" a key:
   (join [{:a 20, :b 34} {:a 31, :b 27} {:a 28, :b 42}]
         [{:a 31, :b 27} {:a 12, :b 4} {:a 28, :b 42}]
         :a)
   => [{:a 31, :b 27} {:a 28, :b 42}]"
  (let [f     #(set (map on %))
        inter (sets/intersection (f xs1)
                                 (f xs2))]
    (reduce (fn [a m]
              (if (inter (m on))
                (conj a m)
                a))
            [] xs1)))

(defn remove-missing-paths
  "given two maps, removes paths from the second map that are not in the first 'source' map.
   returns {:kept <the second map with paths that exist in the first map>
            :removed <vector of paths that were removed from the second map>}

   => (def m {:a 42 :m {:b 28 :c {:z 32} :d nil :w 34}})
   => (def s {:a 42 :m {:f 28 :c {:z 32 :g 12} :d 12 :z 21 :v 14} :k 18})

   => (remove-missing-paths m s)
      {:kept {:a 42, :m {:c {:z 32}, :d 12}},
       :removed ([:k] [:m :f] [:m :v] [:m :z] [:m :c :g])}"

  ([source to-match]
   (let [[kept removed] (remove-missing-paths source to-match [] [])]
     {:kept kept
      :removed (sort-by (fn [path]
                          [(count path)
                           (str path)])
                        removed)}))

  ([source to-match path removed]
   (reduce-kv
     (fn [[kept-map removed-keys] k v]
       (if (contains? source k)
         (let [source-v (get source k)]
           (cond

             ;; both are maps? => go in
             (and (map? source-v)
                  (map? v))          (let [[nested-kept nested-removed]
                                           (remove-missing-paths source-v
                                                                 v
                                                                 (conj path k)
                                                                 removed-keys)]
                                       [(assoc kept-map k nested-kept)
                                        nested-removed])

             ;; source is not a map but "match" is? => remove it
             (map? v)                [kept-map (into removed-keys
                                                     (map #(into path [k %])
                                                          (keys v)))]

             ;; neither is a map => it's a keeper
             :else                   [(assoc kept-map k v)
                                      removed-keys]))

         ;; key (path) doesn't exist in a source map => remove it
         [kept-map (conj removed-keys (conj path k))]))
     [{} removed]
     to-match)))

(defn validate
  "
  takes in a sequence of validator functions and a fact to validate
  returns :valid if all validators pass, otherwise returns a sequence of errors

  if check-all? is true, all validators will be run
  otherwise, validators will be run until the first error is found

  example:

  (defn purrs? [cat]
    (or (= (:purrs cat) true)
        {:error \"cat doesn't purr\"}))

  (defn says-meow? [cat]
    (or (= (:says cat) \"meow\")
        {:error \"cat doesn't say meow\"}))

  (defn one-tail? [cat]
    (or (= (:tail cat) 1)
        {:error \"cat doesn't have 1 tail\"}))

  (defn four-legs? [cat]
    (or (= (:legs cat) 4)
        {:error \"cat doesn't have 4 legs\"}))

  (validate [purrs?
             says-meow?
             one-tail?
             four-legs?]
            {:legs 3 :tail 3 :says \"bow\" :purrs true})

  ; => [{:error \"cat doesn't say meow\"}
        {:error \"cat doesn't have 1 tail\"}
        {:error \"cat doesn't have 4 legs\"}]

  (validate [purrs?
             says-meow?
             one-tail?
             four-legs?]
            {:legs 3 :tail 3 :says \"bow\" :purrs true}
            {:check-all? false})

  ; => [{:error \"cat doesn't say meow\"}]
  "
  ([validators fact]
   (validate validators
             fact
             {:check-all? true}))
  ([validators fact {:keys [check-all?]}]
   (let [results (reduce (fn [results validator]
                           (let [{:keys [error] :as result} (validator fact)
                                 acc (conj results result)]
                             (if (and error
                                      (not check-all?))
                               (reduced acc)
                               acc)))
                         [] validators)]
     (if (every? true? results)
       :valid
       (->> results (remove true?) vec)))))

(defn csv->vec [csv]
  (when (value? csv)
    (->> (s/split csv #",| ")
         (remove empty?)
         (mapv keyword))))

(defn str->edn [xs]
  (try (edn/read-string xs)
       (catch Exception ex
         (printf "could not parse \"%s\" to EDN due to: %s" xs ex))))

(defn- reader->str [^Reader r]
  (let [^StringBuilder sb (StringBuilder.)]
    (loop [buf (char-array 128) n-read (.read r buf)]
      (when (not= n-read -1)
        (.append sb buf 0 n-read)
        (recur buf (.read r buf))))
    (.toString sb)))

(defn gzip-edn [edn]
  (with-open [out  (ByteArrayOutputStream.)
              gzip (GZIPOutputStream. out)]
    (do
      (.write gzip (.getBytes (str edn)))
      (.finish gzip)
      (.toByteArray out))))

(defn gunzip-edn [^bytes bs]
  (with-open [bais (ByteArrayInputStream. bs)
              gis  (GZIPInputStream. bais)]
    (-> gis
        clojure.java.io/reader
        reader->str
        edn/read-string)))

(defn bbuffer->str [bb]
  (when (instance? java.nio.ByteBuffer bb)
    (String. (.array ^java.nio.ByteBuffer bb))))

(defn str->bytes
  ([^String xs]
   (str->bytes xs "UTF-8"))
  ([xs enc]
   (.getBytes xs enc)))

(defn bytes->str
  ([^bytes bs]
   (bytes->str bs "UTF-8"))
  ([bs enc]
   (String. bs enc)))

(defn slurp-resource [path]
  (-> path
      io/resource
      slurp))

(defn edn-resource [path]
  (-> (slurp-resource path)
      edn/read-string))

(defn strip-margin
  ([string]
   (strip-margin string "\\|"))
  ([string delimiter] (->> string
                           s/split-lines
                           (map s/triml)
                           (map #(.replaceFirst % delimiter ""))
                           (s/join "\n"))))

;; threads

(defn show-threads
  "from: https://twitter.com/chrishouser/status/1306000820580876288"
  []
  (->>
    (Thread/getAllStackTraces)
    (map (fn [[thread frames]]
           [(.getName thread)
            (mapv #(read-string (pr-str %))
                  frames)]))
    (into {})
    pp/pprint))
