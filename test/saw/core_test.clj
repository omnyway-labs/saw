(ns saw.core-test
  (:require
   [clojure.test :refer :all]
   [saw.core :as saw]
   [saw.error :refer [err error-as-value]])
  (:import
   [com.amazonaws.auth
    AWSStaticCredentialsProvider]
   [com.amazonaws.auth.profile
    ProfileCredentialsProvider]))


(def provider {:provider :profile
               :profile  (System/getenv "AWS_PROFILE")
               :region   "us-east-1"})

(deftest basic-test
  (is (instance? ProfileCredentialsProvider
                 (saw/creds provider))))

(deftest profile-not-found-test
  (is (= :profile-not-found
         (err
          (saw/login (assoc provider :profile :bad) "12345")))))

(deftest sesssion-create-test
  (is (= :session-create-failed
         (err
          (saw/login (assoc provider :profile :default) "12345")))))

(deftest provider-not-supported-test
  (is (= :provider-not-supported
         (err
          (saw/login {} "12345")))))
