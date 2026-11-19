(ns yang.codec
  (:import [java.util Base64 UUID]
           [java.math BigInteger]
           [java.nio ByteBuffer]
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

(def ^:private base62-alphabet
  "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz")

(def ^:private base62-radix (BigInteger/valueOf 62))

(def ^:private uuid-limit (.shiftLeft BigInteger/ONE 128))

(defn uuid->base62
  "encodes a UUID as a 22 character base62 string
   (left padded with '0's, so encoded strings sort in the same order as UUIDs)"
  [^UUID uuid]
  (when uuid
    (let [bytes (-> (ByteBuffer/allocate 16)
                    (.putLong (.getMostSignificantBits uuid))
                    (.putLong (.getLeastSignificantBits uuid))
                    .array)
          sb    (StringBuilder.)]
      (loop [n (BigInteger. 1 bytes)]
        (when (pos? (.signum n))
          (let [[q r] (.divideAndRemainder n base62-radix)]
            (.append sb (.charAt ^String base62-alphabet (.intValue ^BigInteger r)))
            (recur q))))
      (while (< (.length sb) 22)
        (.append sb \0))
      (str (.reverse sb)))))

(defn base62->uuid
  "decodes a base62 string (1 to 22 characters, leading zeros may be omitted) to a UUID"
  [^String s]
  (when s
    (when-not (<= 1 (count s) 22)
      (throw (ex-info "base62 UUID must be 1 to 22 characters long" {:length (count s)})))
    (let [n (reduce (fn [^BigInteger acc c]
                      (let [d (.indexOf ^String base62-alphabet (int c))]
                        (when (neg? d)
                          (throw (ex-info "invalid base62 character" {:value s :char c})))
                        (.add (.multiply acc base62-radix)
                              (BigInteger/valueOf d))))
                    BigInteger/ZERO
                    s)]
      (when (>= (.compareTo ^BigInteger n uuid-limit) 0)
        (throw (ex-info "base62 value does not fit in 128 bits" {:value s})))
      (UUID. (.longValue (.shiftRight ^BigInteger n 64))
             (.longValue ^BigInteger n)))))
