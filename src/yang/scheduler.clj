(ns yang.scheduler
  (:import [java.util.concurrent.atomic AtomicInteger]
           [java.util.concurrent ThreadFactory Executors TimeUnit ScheduledExecutorService ExecutorService]
           [java.time Instant ZoneId ZonedDateTime]
           [java.time.format DateTimeFormatter]))

;; TODO: plugin da logger
(defn elog [& xs]
  (println (apply format xs)))

(deftype YangThreadFactory [name ^AtomicInteger thread-counter]
  ThreadFactory

  (newThread [_ r]
    (doto
      (Thread. r)
      (.setName (format "%s-%d" name (.getAndIncrement thread-counter)))
      (.setDaemon true)
      (.setUncaughtExceptionHandler
        (reify Thread$UncaughtExceptionHandler
          (uncaughtException [_ thread ex]
            (elog "error in thread id: %s name: %s" (.getId thread) (.getName thread)) ex))))))

(defn thread-name []
  (.getName (Thread/currentThread)))

(defn new-executor
  ([] (new-executor 1))
  ([num-threads]
   (Executors/newFixedThreadPool num-threads
                                 (YangThreadFactory. "yang-runner"
                                                     (AtomicInteger. 0)))))
(defn- parse-long [l]
  (try (Long/valueOf l)
       (catch Exception e)))

(defn run-fun [fun threads]
  (let [threads (or (parse-long (str threads)) 1)
        pool (new-executor threads)
        running? (atom true)
        ^Runnable spinner #(while @running?
                             (try (fun)
                                  (catch Exception e
                                    (elog e))))]
    (dotimes [_ threads]
      (.submit pool spinner))

    {:pool pool :running? running?}))

(defn stop [f]
  (when f
    (.cancel f true)))

(defn every
  "schedules a function to run every \"interval\"
   returns a map with the scheduled task and visibility info
   opts: {:time-unit TimeUnit, :task-name String}
         defaults to MILLISECONDS and 'funtask'."
  ([interval fun]
   (every interval fun {}))
  ([interval fun {:keys [time-unit task-name]
                  :or {time-unit TimeUnit/MILLISECONDS
                       task-name "funtask"}}]
   (let [executor (Executors/newScheduledThreadPool 1)
         started-at (Instant/now)
         f #(try
              (fun)
              (catch Exception e
                (println (str "problem running a scheduled task (\"" task-name "\") due to:") e)))
         scheduled (.scheduleAtFixedRate executor
                                         f
                                         0
                                         interval
                                         time-unit)]
     {:scheduled scheduled
      :fun fun
      :cancel (fn [] (.cancel scheduled false))
      :intel {:task-name task-name
              :interval interval
              :time-unit time-unit
              :started-at started-at
              :cancelled? (fn [] (.isCancelled scheduled))
              :done? (fn [] (.isDone scheduled))
              :running? (fn [] (and (not (.isCancelled scheduled)) (not (.isDone scheduled))))
              :next-run (fn [] (when-not (.isCancelled scheduled)
                                 (let [delay (.getDelay scheduled TimeUnit/MILLISECONDS)]
                                   (.plusMillis (Instant/now) delay))))}})))


(defn ftimes [n f]
  (future
    (dotimes [_ n]
      (try (f)
        (catch Exception e (elog (.printStackTrace e System/out))))
      (Thread/sleep 1000))))
