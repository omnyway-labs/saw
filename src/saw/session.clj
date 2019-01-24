(ns saw.session
  (:refer-clojure :exclude [find])
  (:require
   [clojure.java.io :as io]
   [saw.util :refer [as-error error-as-value]
    :as u])
  (:import
   [com.amazonaws.services.securitytoken
    AWSSecurityTokenService
    AWSSecurityTokenServiceClientBuilder]
   [com.amazonaws.services.securitytoken.model
    GetSessionTokenRequest
    AssumeRoleRequest
    GetCallerIdentityRequest
    AWSSecurityTokenServiceException]))

(defn get-role-arn []
  (System/getenv "AWS_ASSUME_ROLE_ARN"))

(defn get-mfa-arn []
  (System/getenv "AWS_MFA_ARN"))

(def session-file (str (System/getenv "HOME") "/.aws/session"))

(defn find []
  (when (.exists (io/as-file session-file))
    (-> (slurp session-file)
        (read-string))))

(defn- cache! [session]
  (spit session-file session)
  session)

(defn- make-client [region creds]
  (-> (AWSSecurityTokenServiceClientBuilder/standard)
      (.withCredentials creds)
      (.withRegion (or region "us-east-1"))
      (.build)))

(defn- as-static-creds [creds]
  {:provider      :static
   :access-key    (.getAccessKeyId creds)
   :secret-key    (.getSecretAccessKey creds)
   :session-token (.getSessionToken creds)
   :expiration    (.getExpiration creds)})

;; expires after 8 hours
(defn get-timeout []
  (-> (or (System/getenv "AWS_SESSION_TIMEOUT")
          "28800")
      (u/read-string-safely)
      (int)))

(defn- create* [region mfa-code creds]
  (let [client   (make-client region creds)]
    (->> (doto (AssumeRoleRequest.)
         (.withTokenCode mfa-code)
         (.withDurationSeconds (get-timeout))
         (.withSerialNumber (get-mfa-arn))
         (.withRoleArn (get-role-arn))
         (.withRoleSessionName "saw"))
         (.assumeRole client)
         (.getCredentials)
         (as-static-creds)
         (merge {:region region})
         (cache!))))

(defn create! [region mfa-code creds]
  (error-as-value
    (create* region mfa-code creds)))

(defn validate! [region creds]
  (error-as-value
    (let [client (make-client region creds)]
      (->> (GetCallerIdentityRequest.)
           (.getCallerIdentity client)
           (.getArn))
      creds)))

(defn clear! []
  (when (.exists (io/as-file session-file))
    (io/delete-file (io/as-file session-file))))

(defn find-or-create! [region mfa-code creds]
  (or (find)
      (create! region mfa-code creds)))
