(ns yang.http)

(defn error-check [{:keys [error status] :as resp}]
  (if (or error
          (not= 200 status))
    {:error (-> resp
                (update-in [:opts :headers] dissoc "Authorization") ;; strip JWT header
                (update-in [:headers] dissoc :authorization)) ;; ^^
     :status status}))
