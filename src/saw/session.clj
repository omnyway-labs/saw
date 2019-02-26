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

(defn session-file-name [session-name]
  (if session-name
    (str (System/getenv "HOME") "/.aws/session-" session-name)
    (str (System/getenv "HOME") "/.aws/session")))

(defn find [name]
  (let [f (session-file-name name)]
    (when (.exists (io/as-file f))
      (-> (slurp f)
          (read-string)))))

(defn- cache! [session-name session]
  (spit (session-file-name session-name) session)
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

(defn- create! [session-name region mfa-code creds assume-role]
  (let [client   (make-client region creds)]
    (->> (doto (AssumeRoleRequest.)
         (.withTokenCode mfa-code)
         (.withDurationSeconds (get-timeout))
         (.withSerialNumber (get-mfa-arn))
         (.withRoleArn (or assume-role
                           (get-role-arn)))
         (.withRoleSessionName session-name))
         (.assumeRole client)
         (.getCredentials)
         (as-static-creds)
         (merge {:region region})
         (cache! session-name))))

(defn validate! [region creds]
  (error-as-value
    (let [client (make-client region creds)]
      (->> (GetCallerIdentityRequest.)
           (.getCallerIdentity client)
           (.getArn))
      creds)))

(defn clear! [session-name]
  (let [f (io/as-file (session-file-name session-name))]
    (when (.exists f)
      (io/delete-file f))))

(defn find-or-create! [{:keys [session-name region assume-role]}
                       mfa-code creds]
  (let [sess (name (or session-name :saw))]
    (or (find session-name)
        (error-as-value
         (create! sess region mfa-code creds assume-role)))))
