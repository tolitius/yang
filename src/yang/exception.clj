(ns yang.exception
  (:require [clojure.pprint :as pp])
  (:import [java.io StringWriter]))

(defn set-default-exception-handler [{:keys [log]
                                      :or {log println}}]
  (Thread/setDefaultUncaughtExceptionHandler
    (reify Thread$UncaughtExceptionHandler
      (uncaughtException [_ thread ex]
        (log ex "uncaught exception on" (.getName thread))))))

(defn throwable->str [t]
  (let [sw (StringWriter.)]
    (pp/write t :stream sw)
    (str sw)))
