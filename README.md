## yang <img src="doc/img/yang-logo.png" width="42px">

.. of Clojure's yin

[![<! release](https://img.shields.io/badge/dynamic/json.svg?label=release&url=https%3A%2F%2Fclojars.org%2Ftolitius%2Fyang%2Flatest-version.json&query=version&colorB=blue)](https://github.com/tolitius/yang/releases)
[![<! clojars](https://img.shields.io/clojars/v/tolitius/yang.svg)](https://clojars.org/tolitius/yang)

- [why](#why)
- [show me](#show-me)
  - [lang](#lang)
  - [time](#time)
  - [codec](#codec)
  - [network](#network)
  - [schedule](#schedule)
  - [exceptions](#exceptions)
  - [io](#io)
  - [java](#java)
    - [from java](#from-java)
    - [from clojure](#from-clojure)
- [license](#license)

## why

.. not to carry often useful functions from project to project

some of these have a faint "missing from Clojure" feeling<br/>
some just very useful but have no such feeling

one thing they all have in common: **no external dependencies**

## show me

ok. here are a few examples, there are many more inside:

### lang

```clojure
=> (require '[yang.lang :as l])
```

fn on every key in a map:

```clojure
=> (l/fmk {"a" 42 "b" 34} keyword)
{:a 42, :b 34}
```

fn on every value in a map:

```clojure
=> (l/fmv {"a" 42 "b" 34} inc)
{"a" 43, "b" 35}
```

dissoc-in.. oh, yea

```clojure
=> (l/dissoc-in {:foo {:bar {:a 42 :b "don't need this"}}}
                [:foo :bar :b])
{:foo {:bar {:a 42}}}
```

tasty sequential UUIDs:

```clojure
=> (repeatedly 4 #(l/squuid))
(#uuid "5f62baed-553c-49a1-ad85-2b387634e773"
 #uuid "5f62baed-93d2-4e9a-bcd1-beedcaa7e1ee"
 #uuid "5f62baed-b730-44f6-946f-d55b56893121"
 #uuid "5f62baed-d45d-49be-9034-c661fe8069f1")
```

templating:

```clojure
=> (l/rebrace "{{child}}, I am your {{parent}}"
              {:child "Luke" :parent "father"})
"Luke, I am your father"
```

make it Clojure:

```clojure
=> (l/dash-keys {:a_foo 42 :b_bar 34})
{:a-foo 42, :b-bar 34}
```

thread `and` on functions:

```clojure
=> (l/and-> 5 number? pos?)
true
=> (l/and-> nil number? pos?)
false
```

tame those namespaced keys:

```clojure
=> (l/group-by-ns {:a/one :a-one :b/one :b-one :a/two :a-two :b/two :b-two})
{:a {:one :a-one, :two :a-two},
 :b {:one :b-one, :two :b-two}}
```

be a database, do da joins:

```clojure
=> (l/join [{:a 20, :b 34} {:a 31, :b 27} {:a 28, :b 42}]
           [{:a 31, :b 27} {:a 12, :b 4} {:a 28, :b 42}]
           :a)
[{:a 31, :b 27} {:a 28, :b 42}]
```

merge maps, but merge it deep:

```clojure
=> (l/merge-maps {:a {:b {:c 12}} :d 21 :z 34}
                 {:a {:b {:c 42}} :d 25})
{:a {:b {:c 42}}, :d 25, :z 34}
```

gzip / gunzip edn:

```clojure
=> (l/gzip-edn {:a 42 :b 28 :c [{:z #{:a :b 42}}]})
#object["[B" 0x2aafa84f "[B@2aafa84f"]

=> (l/gunzip-edn *1)
{:a 42, :b 28, :c [{:z #{:b 42 :a}}]}
```

validation:

```clojure
=> (defn purrs? [cat]
     (or (= (:purrs cat) true)
         {:error "cat doesn't purr"}))

=> (defn says-meow? [cat]
     (or (= (:says cat) "meow")
         {:error "cat doesn't say meow"}))

=> (defn one-tail? [cat]
     (or (= (:tail cat) 1)
         {:error "cat doesn't have 1 tail"}))

=> (defn four-legs? [cat]
     (or (= (:legs cat) 4)
         {:error "cat doesn't have 4 legs"}))
```

```clojure
=> (y/validate [purrs?
                says-meow?
                one-tail?
                four-legs?]
               {:legs 3 :tail 3 :says "bow" :purrs true})

;; => [{:error "cat doesn't say meow"}
;;     {:error "cat doesn't have 1 tail"}
;;     {:error "cat doesn't have 4 legs"}]

=> (y/validate [purrs?
                says-meow?
                one-tail?
                four-legs?]
               {:legs 3 :tail 3 :says "bow" :purrs true}
               {:check-all? false})

;; => [{:error "cat doesn't say meow"}]

=> (y/validate [purrs?
                says-meow?
                one-tail?
                four-legs?]
               {:legs 4 :tail 1 :says "meow" :purrs true})
;; => :valid
```

### time

```clojure
=> (require '[yang.time :as t])
```

sorting (java.time) instants:

```clojure
=> (def dates [{:date (t/now-utc)} {:date (t/now-utc)} {:date (t/now-utc)}])
#'user/dates

;; DESC in time
=> (sort-by :date t/time> dates)
({:date #object[java.time.Instant 0x2f75a9b1 "2020-07-13T19:56:11.794186Z"]}
 {:date #object[java.time.Instant 0x9cc0505 "2020-07-13T19:56:11.794174Z"]}
 {:date #object[java.time.Instant 0x26cdd4af "2020-07-13T19:56:11.794141Z"]})
```

measure things:

```clojure
=> (t/measure "42 sum" println (reduce + (range 42)))
"42 sum" took: 79,319 nanos
861
```

### codec

```clojure
=> (require '[yang.codec :as c])
```

base64 is just too common:

```clojure
=> (c/base64-encode (-> "distance from you to mars is 69,561,042" .getBytes))
"ZGlzdGFuY2UgZnJvbSB5b3UgdG8gbWFycyBpcyA2OSw1NjEsMDQy"

=> (-> "ZGlzdGFuY2UgZnJvbSB5b3UgdG8gbWFycyBpcyA2OSw1NjEsMDQy"
       c/base64-decode
       String.)
"distance from you to mars is 69,561,042"
```

### network

```clojure
=> (require '[yang.network :as n])
```

name of da host:

```clojure
=> n/hostname
"tweedledee/10.143.34.42"
```

destructure URIs:

```clojure
=> (n/uri->map "postgresql://192.168.10.42:4242/planets")

{:path "/planets",
 :user-info nil,
 :fragment nil,
 :authority "192.168.10.42:4242",
 :port 4242,
 :host "192.168.10.42",
 :scheme-specific-part "//192.168.10.42:4242/planets",
 :query nil,
 :scheme "postgresql"}
```

even if URIs are JDBC:

```clojure
=> (n/jdbc-uri->map "jdbc:postgresql://192.168.10.42:4242/planets")

{:path "/planets",
 :user-info nil,
 :fragment nil,
 :authority "192.168.10.42:4242",
 :port 4242,
 :dbname "planets",
 :host "192.168.10.42",
 :scheme-specific-part "//192.168.10.42:4242/planets",
 :query nil,
 :scheme "postgresql"}
```

### schedule

```clojure
=> (require '[yang.scheduler :as s])
```

schedule functions to run on intervals:

```clojure
=> (def hh (s/every 1000 #(println "hey humans!")))
#'user/hh

hey humans!
hey humans!
hey humans!
hey humans!
hey humans!
hey humans!
hey humans!
hey humans!
hey humans!

user=> (s/stop hh)
true
```

start/stop a farm of threads running a function:

```clojure

=> (defn f []
     (println (s/thread-name))
     (Thread/sleep 5000))
#'user/f

;; schedule a function "f" to run with 42 threads:

=> (def farm (s/run-fun f 42))
yang-runner-0
yang-runner-1
yang-runner-2
...
yang-runner-39
yang-runner-40
yang-runner-41

;; stop the farm of threads from calling "f":

=> (-> farm :running? (reset! false))
false

=> farm
{:pool ThreadPoolExecutor [Running, pool size = 42, active threads = 0, queued tasks = 0, completed tasks = 42]"],
 :running? #atom[false 0x340b4f07]}
```

schedule to run a function `n` times (on a different thread):

```clojure
=> (sc/ftimes 5 #(println "lotery numbers are:" (repeatedly 5 (fn [] (rand-int 42)))))
lotery numbers are: (31 2 27 29 28)
lotery numbers are: (3 28 40 15 1)
lotery numbers are: (13 26 18 19 21)
lotery numbers are: (37 18 18 23 17)
lotery numbers are: (7 16 20 35 8)
```

### exceptions

```clojure
=> (require '[yang.exception :as ex])
```

make sure no exception is left behind:

```clojure
=> (ex/set-default-exception-handler)
```

### io

```clojure
=> (require '[yang.io :as io])
```

read files for what they are.. bytes:

```clojure
=> (io/file->bytes "src/yang/io.clj")
#object["[B" 0x6339e604 "[B@6339e604"]
```

### java

#### from Java

```java
jshell> import tolitius.Yang;

jshell> var m = Map.of("foo", 42, "nested", Map.of("bar", 34), "zoo", 28)
m ==> {foo=42, nested={bar=34}, zoo=28}

jshell> var edn = Yang.mapToEdn(m)
edn ==> {:foo 42, :nested {:bar 34}, :zoo 28}

// so now it can be injected in any Clojure lib that is called from Java and expects EDN
```

##### composing Java and Clojure functions

```java
jshell> import tolitius.Yang;
        import com.google.common.base.CaseFormat;
        import clojure.java.api.Clojure;

        var require = Clojure.var("clojure.core", "require");
        var kw = Clojure.var("clojure.core", "keyword");
        var comp = Clojure.var("clojure.core", "comp");

        require.invoke(Clojure.read("yang.lang"));
        var fmk = Clojure.var("yang.lang", "fmk");

require ==> #'clojure.core/require
kw ==> #'clojure.core/keyword
comp ==> #'clojure.core/comp
fmk ==> #'yang.lang/fmk

jshell> var m = Map.of("answerToLife", 42, "meaningOfLifeQuestion", "what is the...")
m ==> {answerToLife=42, meaningOfLifeQuestion=what is the...}

jshell> Function<String, String> jdash = x -> CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, x);
jdash ==> $Lambda$34/0x0000000800e60040@213c3543
```

convert `java.util.function.Function` / `Consumer` / `BiFunction` / `BiConsumer` to a Clojure function
so it later be composed together with other Clojure functions:

```java
jshell> var dashKeys = Yang.toFun(jdash);
dashKeys ==> yang.java$jfun__GT_fun$fn__158@9d7ccfe
```

compose it:

```java
jshell> fmk.invoke(m, comp.invoke(kw, dashKeys))
$14 ==> {:answer-to-life 42, :meaning-of-life-question "what is the..."}
```

#### from Clojure

```clojure
=> (require '[yang.java :as j])

=> (def m (java.util.HashMap. {"crux.id/foo" {"crux.db/bar" "baz"} "crux.answer/life" 42}))

=> (j/map->edn m)
{:crux.id/foo #:crux.db{:bar "baz"}, :crux.answer/life 42}
```

## license

Copyright © 2020 tolitius

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
