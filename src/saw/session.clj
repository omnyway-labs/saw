(ns saw.session
  (:refer-clojure :exclude [find])
  (:require
   [clojure.java.io :as io]
   [saw.util :as u])
  (:import
   [com.amazonaws.auth
    AWSStaticCredentialsProvider]
   [com.amazonaws.services.securitytoken
    AWSSecurityTokenService
    AWSSecurityTokenServiceClientBuilder]
   [com.amazonaws.services.securitytoken.model
    Credentials
    GetSessionTokenRequest
    AssumeRoleRequest
    GetCallerIdentityRequest
    AWSSecurityTokenServiceException]))

(defn session-file-name []
  (str (System/getenv "HOME") "/.aws/session"))

(defn find []
  (let [f (session-file-name)]
    (when (.exists (io/as-file f))
      (-> (slurp f)
          (read-string)))))

(defn cache! [session]
  (spit (session-file-name) session)
  session)

(defn- make-client [region creds]
  (-> (AWSSecurityTokenServiceClientBuilder/standard)
      (.withCredentials creds)
      (.withRegion region)
      (.build)))

(defn as-static-creds [region creds]
  (cond
    (instance? Credentials creds)
    {:provider      :static
     :region        region
     :access-key    (.getAccessKeyId creds)
     :secret-key    (.getSecretAccessKey creds)
     :session-token (.getSessionToken creds)
     :expiration    (.getExpiration creds)}

    (instance? AWSStaticCredentialsProvider creds)
    (let [c (.getCredentials creds)]
      {:provider      :static
       :region        region
       :access-key    (.getAWSAccessKeyId c)
       :secret-key    (.getAWSSecretKey c)
       :session-token (.getSessionToken c)})
    :else nil))

;; expires after 8 hours
(defn- get-timeout []
  (-> (or (System/getenv "AWS_SESSION_TIMEOUT")
          "28800")
      (u/read-string-safely)
      (int)))

(defn create [region mfa-code mfa-role creds]
  (let [client (make-client region creds)]
    (->> (doto (GetSessionTokenRequest.)
           (.withTokenCode mfa-code)
           (.withSerialNumber mfa-role)
           (.withDurationSeconds (get-timeout)))
         (.getSessionToken client)
         (.getCredentials)
         (as-static-creds region))))

(defn assume-role [region role session-name creds]
  (let [client  (make-client region creds)
        timeout (int 3600)]
    (->> (doto (AssumeRoleRequest.)
           (.withDurationSeconds timeout)
           (.withRoleArn role)
           (.withRoleSessionName session-name))
         (.assumeRole client)
         (.getCredentials)
         (as-static-creds region))))

(defn validate! [region creds]
  (let [client (make-client region creds)]
    (->> (GetCallerIdentityRequest.)
         (.getCallerIdentity client)
         (.getArn)))
  creds)

(defn clear! []
  (let [f (io/as-file (session-file-name))]
    (when (.exists f)
      (io/delete-file f))))
