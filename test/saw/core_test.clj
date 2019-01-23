(ns saw.core-test
  (:require
   [clojure.test :refer :all]
   [saw.core :as saw])
  (:import
   [com.amazonaws.auth.profile
    ProfileCredentialsProvider]))

(defn auth []
  {:provider :profile
   :profile  (System/getenv "AWS_PROFILE")})

(deftest basic-test
  (is (instance? ProfileCredentialsProvider
                 (saw/creds (auth)))))

(deftest ^:integration profile-provider-test
  (is (nil?
         (-> (saw/login (auth))
             :error-id))))

(deftest ^:integration mfa-test
  (is (= :validation-error
         (-> (saw/login (auth) "12345")
             :error-id))))
