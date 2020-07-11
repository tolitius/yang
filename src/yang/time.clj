(ns yang.time
  (:import [java.time Instant ZoneId ZonedDateTime ZoneOffset]
           [java.time.format DateTimeFormatter]
           [java.time.temporal ChronoUnit]
           [java.sql Timestamp]
           [java.util Date]))

(def epoch Instant/EPOCH)

(def time>
  "(sort-by :foo time> [{:foo t3} {:foo t2} {:foo t1}])"
  (comparator (fn [x y] (.isAfter x y))))

(def time<
  "(sort-by :foo time< [{:foo t3} {:foo t2} {:foo t1}])"
  (comparator (fn [x y] (.isBefore x y))))

(defn str-now []
  (str (Instant/now)))

(defn hours-back [hours]
  (.minus (Instant/now)
          hours ChronoUnit/HOURS))

(defn str-big-bang []
  (str (Instant/EPOCH)))

(defn oracle-ts->instant [ts]
  (Instant/ofEpochMilli
    (.getTime (.dateValue ts))))

(defn now-utc []
  (-> (ZoneOffset/UTC)
      (ZonedDateTime/now)
      (.toInstant)))

(defn current-utc-millis []
  (-> (ZoneOffset/UTC)
      (ZonedDateTime/now)
      (.toInstant)
      (.toEpochMilli)))

(defn to-inst [date]
  (condp = (type date)
    Long (Instant/ofEpochMilli date)
    (.toInstant date)))

(defn str-now-utc []
  (if-let [timestamp (current-utc-millis)]
    (-> (ZonedDateTime/ofInstant
          (Instant/ofEpochMilli timestamp)
          (ZoneId/of "UTC"))
        (.format (DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss.SSS")))))

(defn str-ts-utc [ts]
  (if-let [timestamp (Long/valueOf ts)]
    (-> (ZonedDateTime/ofInstant
          (Instant/ofEpochMilli timestamp)
          (ZoneId/of "UTC"))
        (.format (DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss.SSS")))))

(defn ts->zonedt [ts]
  (if-let [timestamp (Long/valueOf ts)]
    (-> (ZonedDateTime/ofInstant
          (Instant/ofEpochMilli timestamp)
          (ZoneId/of "UTC")))))

(defn ts->inst [ts]
  (when-let [timestamp (Long/valueOf ts)]
    (Instant/ofEpochMilli timestamp)))

(defn ts->date [ts]
  (when-let [timestamp (Long/valueOf ts)]
    (Date. ts)))

(defn ts->sql [ts]
  (when-let [timestamp (Long/valueOf ts)]
    (Timestamp. ts)))

(defn inst->ts [inst]
  (when (instance? java.time.Instant inst)
    (.toEpochMilli inst)))

(defn str->date
  "parses string date in ISO 8601
   if fails returns nil OR a default value when provided:

   => (str->date 42)
   nil

   => (str->date 42 epoch)             ;; epoch is a value from this namespace
   #inst 1970-01-01T00:00:00.000-00:00

   => (str->date 2020-06-01T22:07:45.519Z epoch)
   #inst 2020-06-01T22:07:45.519-00:00"
  ([s]
   (str->date s nil))
  ([s default]
   (try
     (Instant/parse s)
     (catch Exception e
       default))))

(defn n-hours-ago [n]
  (-> (ZoneOffset/UTC)
      (ZonedDateTime/now)
      (.minusHours n)
      (.toInstant)
      (.toEpochMilli)))

(defmacro measure
  "measures a form:
   => (t/measure \"42 sum\"
                 println
                 (reduce + (range 42)))
      \"42 sum\" took: 49,054 nanos
      861
  "
  [fname report f]
  `(let [start# (System/nanoTime)
         res# (~@f)
         done# (System/nanoTime)]
     (~report (format "\"%s\" took: %,d nanos" ~fname (- done# start#)))
     res#))

(defmacro time-it
  "times a form and returns result and time it took in ms:

   => (t/time-it  (reduce + (range 42)))
   [861 {:unit \"ms\", :time 0.049786}]
  "
  [expr]
  `(let [start# (. System (nanoTime))
         ret# ~expr
         done# (. System (nanoTime))]
     [ret# {:time (/ (double (- done# start#)) 1000000.0)
            :unit "ms"}]))
