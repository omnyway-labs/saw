(ns saw.provider
  (:refer-clojure :exclude [resolve])
  (:require
   [saw.config :as config])
  (:import
   [com.amazonaws.auth
    AWSCredentials
    BasicAWSCredentials
    BasicSessionCredentials
    AWSSessionCredentials
    AWSStaticCredentialsProvider
    InstanceProfileCredentialsProvider
    EnvironmentVariableCredentialsProvider
    DefaultAWSCredentialsProviderChain]
   [com.amazonaws.auth.profile ProfileCredentialsProvider]))

(defonce ^:private current (atom nil))

(defonce ^:private current-region (atom nil))

(defn lookup []
  @current)

(defn- ^AWSCredentials
  static-credentials
  [{:keys [access-key secret-key session-token] :as creds}]
  (cond
    (and access-key secret-key session-token)
    (BasicSessionCredentials. access-key secret-key session-token)

    (and access-key secret-key)
    (BasicAWSCredentials. access-key secret-key)))

(def ^:private providers
  {:instance InstanceProfileCredentialsProvider
   :profile  ProfileCredentialsProvider
   :env      EnvironmentVariableCredentialsProvider
   :default  DefaultAWSCredentialsProviderChain})

(defn resolve [{:keys [provider auth-type profile] :as config}]
  (condp = (or auth-type provider)
    :instance (InstanceProfileCredentialsProvider. false)
    :profile  (ProfileCredentialsProvider. (name profile))
    :env      (EnvironmentVariableCredentialsProvider.)
    :static   (AWSStaticCredentialsProvider. (static-credentials config))
    :default  (DefaultAWSCredentialsProviderChain.)))

(defn creds? [thing]
  (or (instance? InstanceProfileCredentialsProvider thing)
      (instance? ProfileCredentialsProvider thing)
      (instance? EnvironmentVariableCredentialsProvider thing)
      (instance? AWSStaticCredentialsProvider thing)
      (instance? DefaultAWSCredentialsProviderChain thing)))

(defn lookup-profile [profile]
  (-> (config/read-credentials-file)
      (get profile)))

(defn- lookup-region [{:keys [provider profile auth-type region]}]
  (if (or (= provider :profile)
          (= auth-type :auth-type))
    (:region (lookup-profile profile))
    region))

(defn set! [creds]
  (reset! current creds))

(defn set-region! [region]
  (reset! current-region region))

(defn get-region []
  @current-region)
