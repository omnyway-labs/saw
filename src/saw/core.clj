(ns saw.core
  (:require
   [clojure.java.io :as io]
   [saw.provider :as p]
   [saw.session :as session]))

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
       (p/set-region! region)
       (-> (p/as-provider provider)
           (p/resolve)
           (p/set!)))))

  ([provider mfa-code]
   (when-let [role (System/getenv "AWS_MFA_ARN")]
     (login provider mfa-code role)))

  ([{:keys [region] :as provider} mfa-code mfa-role]
   (let [region (p/lookup-region provider)]
     (p/set-region! region)
     (->> (p/as-provider provider)
          (p/resolve)
          (session/create region mfa-code mfa-role)
          (session/cache!)
          (p/resolve)
          (p/set!)))))

(defn assume
  ([profile]
   (let [{:keys [role_arn region]} (p/lookup-profile profile)
         creds  (-> (session/find)
                    (p/resolve))]
     (assume creds role_arn region)))

  ([creds profile]
   (let [{:keys [role_arn region]} (p/lookup-profile profile)]
     (assume creds role_arn region)))

  ([creds role-arn region]
   (let [session-name (gen-session-name)
         sp (session/assume-role region role-arn session-name creds)]
     (login sp))))

(defn creds
  ([] (p/lookup))
  ([provider] (p/resolve provider)))

(defn static-creds [creds]
  (session/as-static-creds (region) creds))
