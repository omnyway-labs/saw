(ns saw.provider
  (:refer-clojure :exclude [resolve])
  (:require
   [saw.session :as session])
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

(def ^:private current (atom nil))

(defn lookup []
  @current)

(defn set! [creds]
  (reset! current creds ))

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

(defn resolve [{:keys [provider profile] :as config}]
  (condp = provider
    :instance (InstanceProfileCredentialsProvider. false)
    :profile  (ProfileCredentialsProvider. (name profile))
    :env      (EnvironmentVariableCredentialsProvider.)
    :static   (AWSStaticCredentialsProvider. (static-credentials config))
    :default  (DefaultAWSCredentialsProviderChain.)))
