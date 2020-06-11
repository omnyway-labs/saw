(ns saw.main
  (:require
   [saw.core :as saw]
   [saw.error :refer [error-as-value]]))

(defn format-creds [{:keys [region access-key
                        secret-key session-token]
                 :as c}]
  (if session-token
    (format "%s\n%s\n%s\n%s"
            (str "aws_access_key_id=" access-key)
            (str "aws_secret_access_key=" secret-key)
            (str "region=" (or region "us-east-1"))
            (str "aws_session_token=" session-token))
    c))


(defn validate! [region]
  (->> (saw/session)
       (saw/creds)
       (saw/validate-session region)))

(defn -main [& args]
  (let [[profile region role mfa] args
        role (or (not (empty? role))
                 (System/getenv "AWS_MFA_ARN"))
        provider {:provider    :profile
                  :profile (keyword profile)
                  :region  region}]
    (if mfa
      (let [creds (error-as-value
                   (saw/login provider mfa role))]
        (if (:error creds)
          (println creds)
          (-> (saw/static-creds creds)
              (format-creds)
              (println)))))))
