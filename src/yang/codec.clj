(ns yang.codec
  (:import [java.util Base64]
           [javax.crypto.spec SecretKeySpec]
           [javax.crypto Mac]
           [java.nio.charset StandardCharsets]))

(defn base64-decode [^String s]
  (.decode (Base64/getDecoder) s))

(defn base64-encode [#^bytes bytes]
  (String. (.encode (Base64/getEncoder) bytes)
           StandardCharsets/ISO_8859_1))

(defn hmac [^String key ^String message]
  (let [secret-key (SecretKeySpec. (.getBytes key StandardCharsets/US_ASCII)
                                   "HmacSHA1")
        mac        (doto (Mac/getInstance "HmacSHA1")
                         (.init secret-key))
        result     (.doFinal mac (.getBytes message StandardCharsets/US_ASCII))]
    (.encodeToString (Base64/getEncoder)
                     result)))
