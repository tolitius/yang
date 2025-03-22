# 0.1.50

### 2025-03-22

update "`yang.scheduler`"

`every` function now:

* returns a map with the scheduled task, functions and visibility info
* takes an optional "`task-name`"

```clojure
(def s (every 1000
              #(println "scanning asteroids")))  ;; default: "funtask", milliseconds
(:intel s)                                       ;; => {:task-name "funtask"
                                                 ;;     :interval 1000
                                                 ;;     :time-unit :MILLISECONDS
                                                 ;;     :started-at [java.time.Instant "2025-03-22T12:34:56Z"]
                                                 ;;     :cancelled? #function[...]
                                                 ;;     :done? #function[...]
                                                 ;;     :running? #function[...]
                                                 ;;     :next-run #function[...]}

(def s2 (every 2
               #(println "Beep from Mars")
               {:task-name "rover-ping"
                :time-unit TimeUnit/SECONDS}))
(:interval (:intel s2))                          ;; => 2
((:next-run (:intel s2)))                        ;; => [java.time.Instant "2025-03-22T18:34:49.365088Z"]

(def s3 (every 1000
               #(throw (RuntimeException. "no oxygen!"))
               {:task-name "life-support"}))
;; prints: "problem running a scheduled task (\"life-support\") due to: java.lang.RuntimeException: no oxygen!"
;; but does not stop running
```

# 0.1.48

[add](https://github.com/tolitius/yang/pull/13) "`swallow`"

# 0.1.46

add "`remove-missing-paths`":

```clojure
=> (def m {:a 42 :m {:b 28 :c {:z 32} :d nil :w 34}})
=> (def s {:a 42 :m {:f 28 :c {:z 32 :g 12} :d 12 :z 21 :v 14} :k 18})

=> (remove-missing-paths m s)
   {:kept {:a 42, :m {:c {:z 32}, :d 12}},
    :removed ([:k] [:m :f] [:m :v] [:m :z] [:m :c :g])}"
```

# 0.1.44

moved reflection to the `yang.java` namespace
making most of yang babashka compatible

# 0.1.43

+ add [assoc-if](https://github.com/tolitius/yang/blob/b9ffbfec3261e08e867501870f4345b7faf2fb5a/src/yang/lang.clj#L70-L100)

```clojure
=> (require '[yang.lang :as y])
=> (def m {:moo 42})

=> (y/assoc-if m :zoo 12)
{:moo 42, :zoo 12}

;; nil is not a value: hence skipped
=> (y/assoc-if m :zoo 12 :moo nil)
{:moo 42, :zoo 12}

;; false is a value: hence not skipped
=> (y/assoc-if m :zoo 12 :moo nil :boo false)
{:moo 42, :zoo 12, :boo false}
```

# 0.1.42

+ [remove-keys-by-prefix](https://github.com/tolitius/yang/blob/af35279e4b13c36927572c0c5bd6d1144ee22f6c/src/yang/lang.clj#L176-L193)

# 0.1.39

+ [rebrace](https://github.com/tolitius/yang/blob/cd8c4e94160b5e0e375a49b0dd0cb881015ddb41/src/yang/lang.clj#L208)
+ [index-by](https://github.com/tolitius/yang/blob/cd8c4e94160b5e0e375a49b0dd0cb881015ddb41/src/yang/lang.clj#L374)

# 0.1.34

* lang: add `filter-kv` & `map->keys-as-path` _([0af41e8](https://github.com/tolitius/yang/commit/0af41e847df9f2c1b8b9948242dbcd38b6971d71) thanks to [@sirmspencer](https://github.com/sirmspencer))_

# 0.1.33

* HTTP nil status to 500 in case of an error

# 0.1.29

* add a `strip-margin` function _([4f92fd4](https://github.com/tolitius/yang/commit/4f92fd416425930c822da1d4a67748cac2c5f19a) thanks to [@danielmiladinov](https://github.com/danielmiladinov))_
