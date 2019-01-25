(ns saw.core
  (:require
   [saw.provider :as provider]
   [saw.session :as session]))

(defn- mfa-enabled? []
  (session/get-mfa-arn))

(defn creds
  ([] (provider/lookup))
  ([auth] (provider/resolve auth)))

(defn session []
  (session/find))

(defn validate-session
  ([region]
   (->> (session)
        (validate-session region)))
  ([region creds]
   (session/validate! region creds)))

(defn- find-or-create-session [region mfa-code]
  (let [session (->> (provider/lookup)
                     (session/find-or-create! region mfa-code))]
    (if-not (:error-id session)
      (let [creds (->> (provider/resolve session)
                       (session/validate! region))]
        (if-not (:error-id creds)
          (provider/set! creds)
          creds))
      session)))

(defn clear-session []
  (session/clear!))

(defn login
  ([auth]
   (-> (provider/resolve auth)
       (provider/set!)))
  ([{:keys [region] :as auth} mfa-code]
   (-> (provider/resolve auth)
       (provider/set!))
   (if (session/mfable?)
     (find-or-create-session region mfa-code)
     {:error-id :env-not-configured
      :msg      "AWS_ASSUME_ROLE_ARN or AWS_MFA_ARN not set"})))
