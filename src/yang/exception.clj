(ns yang.exception
  (:require [clojure.pprint :as pp])
  (:import [java.io StringWriter]))

(defn set-default-exception-handler
  "(set-default-exception-handler)
   or
   (set-default-exception-handler {:log (fn [ex tname] (log/error ex tname))})  ;; to wrap log/error macro
   or
   plugin another favorite logger"
  ([]
   (set-default-exception-handler {}))
  ([{:keys [log]
     :or {log println}}]
   (Thread/setDefaultUncaughtExceptionHandler
     (reify Thread$UncaughtExceptionHandler
       (uncaughtException [_ thread ex]
         (log ex (str "uncaught exception on"
                      (.getName thread))))))))

(defn throwable->str [t]
  (let [sw (StringWriter.)]
    (pp/write t :stream sw)
    (str sw)))
