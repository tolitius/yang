(ns yang.lang-test
  (:require [yang.lang :as yl]
            [clojure.test :refer [deftest testing is]]))

(deftest ^:support-converting-seq-of-values-into-hashmap
  validate-seq->hash-map
  (let [case1 "convert list of values into hash-map"]
    (println case1)
    (testing case1
      (let [input  [1 2 3 4]
            output (yl/seq->hash-map {:key-fn dec} input)]
        (is (= 1 (get output 0))
        (is (= 2 (get output 1))
        (is (= {0 1
                1 2
                2 3
                3 4}
               output)))))))
  (let [case2 "convert list of map into hash-map"]
    (println case2)
    (testing case2
      (let [input  [{:name "VickySuraj" :id "vicky-suraj"}
                    {:name "Anatoly"    :id "tolitius"}]
            output (yl/seq->hash-map {:key-fn (comp keyword :id)
                                      :val-fn (comp (partial str "Hi I am ") :name)}
                                     input)]
        (is (= "Hi I am VickySuraj" (:vicky-suraj output))
        (is (= "Hi I am Anatoly" (:tolitius output))))))))
