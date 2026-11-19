(ns yang.test.codec
  (:require [clojure.test :refer [deftest testing is are]]
            [yang.codec :as c]
            [yang.lang :as y])
  (:import [java.util UUID]))

(def max-uuid (UUID/fromString "ffffffff-ffff-ffff-ffff-ffffffffffff"))

(deftest should-encode-uuid-to-base62
  (testing "should encode edge UUIDs"
    (is (= "0000000000000000000000" (c/uuid->base62 y/zero-uuid)))
    (is (= "7n42DGM5Tflk9n8mt7Fhc7" (c/uuid->base62 max-uuid))))

  (testing "should always produce 22 characters"
    (is (every? #(= 22 (count (c/uuid->base62 %)))
                (repeatedly 1000 y/uuid))))

  (testing "should return nil for nil"
    (is (nil? (c/uuid->base62 nil)))))

(deftest should-decode-base62-to-uuid
  (testing "should round trip"
    (is (= y/zero-uuid (c/base62->uuid (c/uuid->base62 y/zero-uuid))))
    (is (= max-uuid (c/base62->uuid (c/uuid->base62 max-uuid))))
    (is (every? #(= % (c/base62->uuid (c/uuid->base62 %)))
                (repeatedly 1000 y/uuid))))

  (testing "should restore omitted leading zeros"
    (are [input expected] (= (UUID/fromString expected) (c/base62->uuid input))
                          "0"    "00000000-0000-0000-0000-000000000000"
                          "1"    "00000000-0000-0000-0000-000000000001"
                          "10"   "00000000-0000-0000-0000-00000000003e"
                          "0001" "00000000-0000-0000-0000-000000000001"))

  (testing "should return nil for nil"
    (is (nil? (c/base62->uuid nil))))

  (testing "should reject invalid input"
    (are [input] (thrown? clojure.lang.ExceptionInfo (c/base62->uuid input))
                 ""
                 "00000000000000000000000"   ;; 23 chars
                 "abc-def"
                 "zzzzzzzzzzzzzzzzzzzzzz"))) ;; > 128 bits

(deftest should-preserve-uuid-order
  (testing "encoded strings sort in unsigned UUID order"
    (let [unsigned (fn [^UUID u]
                     (BigInteger. (str (Long/toUnsignedString (.getMostSignificantBits u) 16)
                                       (format "%016x" (.getLeastSignificantBits u)))
                                  16))
          uuids    (repeatedly 1000 y/uuid)]
      (is (= (map c/uuid->base62 (sort-by unsigned uuids))
             (sort (map c/uuid->base62 uuids)))))))
