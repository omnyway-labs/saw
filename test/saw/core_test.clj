(ns saw.core-test
  (:require
   [clojure.test :refer :all]
   [saw.core :as saw]))

(deftest profile-provider-test
  (is (nil?
         (-> (saw/login {:provider :profile
                         :profile  (System/getenv "AWS_PROFILE")})
             :error-id))))

(deftest mfa-test
  (is (= :validation-error
         (-> (saw/login {:provider :profile
                         :profile  (System/getenv "AWS_PROFILE")}
                        "12345")
             :error-id))))
