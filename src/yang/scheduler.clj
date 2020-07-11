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
  ([interval fun]
   (every interval fun TimeUnit/MILLISECONDS))
  ([interval fun time-unit]
   (let [f #(try (fun) (catch Exception e (elog e)))]
    (.scheduleAtFixedRate (Executors/newScheduledThreadPool 1)
      f 0 interval time-unit))))

(defn do-times [n f]
  (future
    (dotimes [_ n]
      (try (f)
        (catch Exception e (elog (.printStackTrace e System/out))))
      (Thread/sleep 1000))))
