(ns saw.core
  (:require
   [clojure.java.io :as io]
   [saw.provider :as p]
   [saw.session :as session]
   [saw.error :refer [assert!]]))

(defn region [] (p/get-region))

(defn session [] (session/find))

(defn validate-session
  ([]
   (when-let [s (session)]
     (->> (p/resolve s)
          (validate-session (:region s)))))
  ([region creds]
   (session/validate! region creds)))

(defn clear-session [] (session/clear!))

(defn- gen-session-name []
  (str (System/currentTimeMillis)))

(defn login
  ([provider]
   (if (p/creds? provider)
     provider
     (let [region (p/lookup-region provider)]
       (assert! region :region-not-found)
       (p/set-region! region)
       (-> (p/as-provider provider)
           (p/resolve)
           (assert! :provider-not-resolved {:provider provider})
           (p/set!)))))

  ([provider mfa-code]
   (let [role (System/getenv "AWS_MFA_ARN")]
     (assert! role :role-arn-not-found)
     (login provider mfa-code role)))

  ([{:keys [region] :as config} mfa-code mfa-role]
   (let [region   (p/lookup-region config)
         provider (p/as-provider config)]
     (assert! provider :provider-not-supported {:provider config})
     (assert! (string? mfa-code) :mfa-code-not-string)
     (assert! region :region-not-found)
     (p/set-region! region)
     (->> (p/resolve provider)
          (session/create region mfa-code mfa-role)
          (session/cache!)
          (p/resolve)
          (p/set!)))))

(defn assume
  ([profile]
   (let [{:keys [role_arn region]} (p/lookup-profile profile)]
     (assert! role_arn :region-not-found-in-profile)
     (assert! region :role-arn-not-found-in-profile)
     (let [session (session/find)]
       (assert! session :session-not-found)
       (-> (p/resolve session)
           (assert! :provider-not-resolved {:provider session})
           (assume role_arn region)))))

  ([creds profile]
   (let [{:keys [role_arn region]} (p/lookup-profile profile)]
     (assert! role_arn :region-not-found-in-profile)
     (assert! region :role-arn-not-found-in-profile)
     (assume creds role_arn region)))

  ([creds role-arn region]
   (assert! (p/creds? creds) :invalid-creds-object)
   (let [session-name (gen-session-name)
         sp (session/assume-role region role-arn session-name creds)]
     (login sp))))

(defn creds
  ([] (p/lookup))
  ([provider] (p/resolve provider)))

(defn static-creds [creds]
  (assert! (region) :region-not-found)
  (session/as-static-creds (region) creds))
