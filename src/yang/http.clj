(ns yang.http)

(defn successful-http-status? [status]
  (when (number? status)
    (= 2
       (int (/ status 100)))))

(defn error-check [{:keys [error status] :as resp}]
  (if (or error
          (not (successful-http-status? status)))
    {:error (-> resp
                (update-in [:opts :headers] dissoc "Authorization") ;; strip JWT header
                (update-in [:headers] dissoc :authorization)) ;; ^^
     :status status}))
