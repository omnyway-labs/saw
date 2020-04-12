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
               :profile  nil
               :region   "us-east-1"})

(deftest empty-profile-test
  (is (contains? #{:profile-not-found :role-arn-not-found}
         (err
          (saw/login provider "12345")))))

(deftest profile-not-found-test
  (is (contains? #{:profile-not-found :role-arn-not-found}
         (err
          (saw/login (assoc provider :profile :bad) "12345")))))

(deftest sesssion-create-test
  (is (contains? #{:session-create-failed
                   :profile-not-found
                   :role-arn-not-found}
         (err
          (saw/login (assoc provider :profile :default) "12345")))))

(deftest provider-not-supported-test
  (is (contains? #{:provider-not-supported
                   :role-arn-not-found}
         (err
          (saw/login {} "12345")))))
