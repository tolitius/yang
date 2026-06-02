 (ns yang.test.lang
   (:require [clojure.test :refer [deftest testing is are]]
             [yang.lang :as y]))

  (deftest should-parse-valid-numbers
    (testing "should parse integers (EDN rules, leading zeros = octal)"
      (are [input expected] (= expected (y/parse-number input))
                            "0" 0
                            "1" 1
                            "42" 42
                            "123456789" 123456789
                            "-1" -1
                            "-42" -42
                            "-999" -999
                            "045" 37           ; octal 045 = decimal 37
                            "007" 7            ; octal 007 = decimal 7
                            "-012" -10))       ; octal -012 = decimal -10

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

    (testing "should parse numbers from various input types"
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
                  " 42"
                  "42 "
                  " 42.5 "

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
                  ".5"
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

                  ;; positive sign (not supported)
                  "+42"
                  "+3.14"

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

  (deftest should-parse-decimal-numbers
    (testing "should parse as decimal (base-10) ignoring octal/hex semantics"
      (are [input expected] (= expected (y/parse-number-literal input))
                            ;; decimal integers with leading zeros
                            "045" 45           ; decimal 45, NOT octal 37
                            "007" 7
                            "-012" -12
                            "0011" 11
                            "-0012" -12
                            "0000" 0
                            "-0000" 0
                            
                            ;; regular decimals
                            "42" 42
                            "-10" -10
                            "1,234" 1234
                            "1,234,567" 1234567
                            
                            ;; decimal floats with leading zeros
                            "00.5" 0.5
                            "-001.23" -1.23
                            "-0012.5" -12.5
                            "0.0" 0.0
                            "42.42" 42.42
                            "1,234.56" 1234.56))
    
    (testing "should reject hex prefixes and invalid formats"
      (are [input] (nil? (y/parse-number-literal input))
                   "0x2A"         ; hex rejected
                   "0xFF"
                   "-0x10"
                   ""
                   "abc"
                   "1,23"
                   ".5"
                   "0-12"
                   "-0-12")))

 (deftest should-handle-boundary-cases
   (testing "should handle Long/MIN_VALUE and Long/MAX_VALUE"
     (is (= Long/MAX_VALUE (y/parse-number (str Long/MAX_VALUE))))
     (is (= Long/MIN_VALUE (y/parse-number (str Long/MIN_VALUE)))))

   (testing "should handle very small decimals"
     (is (= 0.000001 (y/parse-number "0.000001")))
     (is (= 0.123456789 (y/parse-number "0.123456789")))))
