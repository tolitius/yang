(ns yang.network
  (:import [java.net InetAddress URI]))

(defonce hostname
  (str (InetAddress/getLocalHost)))

(defn uri->map [uri]
  ;; add raw props if/when needed
  (let [u (URI/create uri)]
    {:host (.getHost u)
     :port (.getPort u)
     :path (.getPath u)
     :fragment (.getFragment u)
     :query (.getQuery u)
     :authority (.getAuthority u)
     :scheme (.getScheme u)
     :scheme-specific-part (.getSchemeSpecificPart u)
     :user-info (.getUserInfo u)}))

(defn jdbc-uri->map [uri]
  (when (seq uri)
    (let [wo-jdbc (.substring uri 5)              ;; jdbc:postgres:1.2.3.4 => postgres:1.2.3.4
          {:keys [path] :as m} (uri->map wo-jdbc)
          dbname (->> (rest path)                 ;; "/foo" => "foo"
                      (apply str))]
      (assoc m :dbname dbname))))
