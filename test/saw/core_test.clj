(ns saw.core-test
  (:require
   [clojure.test :refer :all]
   [saw.core :as saw])
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

(deftest ^:integration profile-provider-test
  (is (nil?
         (saw/login provider))))

(deftest ^:integration mfa-test
  (is (= :validation-error
         (-> (saw/login provider "12345")
             :error-id))))

(deftest ^:integration mfa-test-with-assume-role
  (is (= :static
         (-> (saw/login provider)
             (saw/assume :staging)
             (saw/static-creds)
             :provider))))
