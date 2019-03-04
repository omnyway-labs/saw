(ns saw.core-test
  (:require
   [clojure.test :refer :all]
   [saw.core :as saw])
  (:import
   [com.amazonaws.auth
    AWSStaticCredentialsProvider]
   [com.amazonaws.auth.profile
    ProfileCredentialsProvider]))

(defn auth []
  {:provider :profile
   :profile  (System/getenv "AWS_PROFILE")
   :region   "us-east-1"})

(deftest basic-test
  (is (instance? ProfileCredentialsProvider
                 (saw/creds (auth)))))

(deftest ^:integration profile-provider-test
  (is (nil?
         (-> (saw/login (auth))
             :error-id))))

(deftest ^:integration mfa-test
  (is (= :validation-error
         (-> (saw/login (auth) "12345" "a" "b")
             :error-id))))

(deftest ^:integration mfa-test-with-assume-role
  (is (= :400
         (-> (saw/login (auth) "12345" "assume-role" "my-session")
             :error-id))))


(deftest ^:integration use-session-test
  (is (instance? AWSStaticCredentialsProvider
                 (saw/login {:provider :profile
                             :profile  :humans
                             :session? true})))

  (is (instance? ProfileCredentialsProvider
                 (saw/login {:provider :profile
                             :profile  :humans
                             :session? false}))))
