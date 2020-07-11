(ns yang.network)

(defonce hostname
  (str (java.net.InetAddress/getLocalHost)))
