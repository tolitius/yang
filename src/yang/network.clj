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
