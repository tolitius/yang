(ns yang.io
  (:require [clojure.java.io :as jio]))

;; from https://clojuredocs.org/clojure.java.io/input-stream#example-5542a631e4b01bb732af0a8f
(defn file->bytes [file]
  (with-open [xin (jio/input-stream file)
              xout (java.io.ByteArrayOutputStream.)]
    (jio/copy xin xout)
    (.toByteArray xout)))
