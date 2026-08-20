(ns yang.test.lang
  (:require [clojure.test :refer [deftest testing is are]]
            [yang.lang :as y]))

(deftest should-parse-valid-numbers
  (testing "should parse valid integers and does not parse octal"
    (are [input expected] (= expected (y/parse-number input))
                          "0" 0
                          "1" 1
                          "42" 42
                          "123456789" 123456789
                          "-1" -1
                          "-42" -42
                          "-999" -999
                          "045" nil      ; leading zeros rejected
                          "007" nil      ; leading zeros rejected
                          "-012" nil))   ; leading zeros rejected

  (testing "should parse decimals"
    (are [input expected] (= expected (y/parse-number input))
                          "0.0" 0.0
                          "1.5" 1.5
                          "42.42" 42.42
                          "3.14159" 3.14159
                          "0.1" 0.1
                          "123.456" 123.456
                          "-1.5" -1.5
                          "-42.999" -42.999
                          "-0.5" -0.5
                          "1,234" 1234
                          "1,234,567" 1234567
                          "1,234.56" 1234.56
                          "-1,234.56" -1234.56))

  (testing "parses numbers with number types"
    (are [input expected] (= expected (y/parse-number input))
                          42 42
                          42.5 42.5
                          -10 -10
                          -3.14 -3.14
                          0 0
                          0.0 0.0)))

(deftest should-reject-invalid-numbers
  (testing "should return nil for invalid formats"
    (are [input] (nil? (y/parse-number input))
                 ;; empty/whitespace
                 nil
                 ""
                 "   "
                 ;; malformed comma thousands separators
                 "1,23"
                 "1,234,56"
                 "12,34,567"
                 "1,0000"
                 "1,234,"
                 "1,234."
                 ;; multiple decimal points
                 "12.34.56"
                 "1..5"
                 ;; leading/trailing periods without digits
                 "5."
                 ;; letters and special characters
                 "abc"
                 "12a34"
                 "12.3b"
                 "a42"
                 "42b"
                 "$42"
                 "42$"
                 ;; multiple signs
                 "--42"
                 "+-42"
                 "-+42"
                 "++42"
                 ;; trailing sign
                 "42-"
                 "42+"
                 "0-12"
                 "-0-12"
                 ;; scientific notation (not supported)
                 "1e10"
                 "1E10"
                 "1.5e-3"
                 "2.5E+2"
                 ;; other invalid formats
                 "NaN"
                 "Infinity"
                 "-Infinity"
                 "nil"
                 "null"
                 ;; special characters
                 "42%"
                 "#42"
                 "42!"
                 "4_2"
                 "4-2"
                 ;; mixed content
                 "12 34"
                 "12/34"
                 "12,34"
                 "42:00")))

(deftest should-handle-edge-cases
  (testing "should handle edge cases properly"
    (is (= 0 (y/parse-number "0")))
    (is (= 0.0 (y/parse-number "0.0")))
    (is (= 0.0 (y/parse-number "-0.0")))
    (is (= 0 (y/parse-number 0)))
    (is (= 0.0 (y/parse-number 0.0)))
    ;; very large numbers
    (is (= 999999999999999 (y/parse-number "999999999999999")))
    (is (= 1.234567891234567 (y/parse-number "1.234567891234567")))
    ;; very small numbers
    (is (= -999999999999999 (y/parse-number "-999999999999999")))
    (is (= -1.234567891234567 (y/parse-number "-1.234567891234567")))))

(deftest should-handle-boundary-cases
  (testing "should handle Long/MIN_VALUE and Long/MAX_VALUE"
    (is (= Long/MAX_VALUE (y/parse-number (str Long/MAX_VALUE))))
    (is (= Long/MIN_VALUE (y/parse-number (str Long/MIN_VALUE)))))

  (testing "should handle very small decimals"
    (is (= 0.000001 (y/parse-number "0.000001")))
    (is (= 0.123456789 (y/parse-number "0.123456789")))))

(deftest should-merge-maps-strict-without-throwing
  (testing "nil map arguments are dropped instead of causing data loss"
    (is (= {:a 1}
           (y/merge-maps-strict {:a 1} nil)))
    (is (= {:a 1}
           (y/merge-maps-strict nil {:a 1})))
    (is (nil? (y/merge-maps-strict nil nil))))

  (testing "does not throw ArityException with 3+ maps when one is nil"
    (is (= {:a 3 :b 2}
           (y/merge-maps-strict {:a 1 :b 2} nil {:a 3})))
    (is (= {:a 3}
           (y/merge-maps-strict {:a 1} {:a nil} {:a 3})))
    (is (= {:a {:b 1 :c 2}}
           (y/merge-maps-strict {:a {:b 1}} nil {:a {:c 2}}))))

  (testing "explicit nil values are treated as absent rather than an override"
    (is (= {:a {:b 1 :c 2}}
           (y/merge-maps-strict {:a {:b 1 :c 2}} {:a nil}))))

  (testing "nil-valued keys are dropped at every nesting level"
    (is (= {:a {:b {:c 1}}}
           (y/merge-maps-strict {:a {:b {:c 1 :d nil}}}))))

  (testing "keeps false and zero, but drops empty collections/strings too, reusing remove-empty"
    (is (= {:a false :c 0}
           (y/merge-maps-strict {:a false :b [] :c 0 :d nil}))))

  (testing "single map argument is returned cleaned, unaffected by merging"
    (is (= {:a 1}
           (y/merge-maps-strict {:a 1 :b nil}))))

  (testing "a map that becomes entirely empty after cleaning merges away cleanly"
    (is (= {} (y/merge-maps-strict {:a nil} {:b nil}))))

  (testing "regression: the original merge-maps bug still reproduces, confirming it was left untouched"
    (is (nil? (y/merge-maps {:a 1} nil)))
    (is (thrown? clojure.lang.ArityException
                 (y/merge-maps {:a 1 :b 2} nil {:a 3})))))
