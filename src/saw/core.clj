(ns saw.core
  (:require
   [saw.provider :as provider]
   [saw.session :as session]))

(defn- mfa-enabled? []
  (session/get-mfa-arn))

(defn creds
  ([] (provider/lookup))
  ([auth] (provider/resolve auth)))

(defn session [session-name]
  (session/find session-name))

(defn validate-session
  ([region]
   (->> (session)
        (validate-session region)))
  ([region creds]
   (session/validate! region creds)))

(defn- find-or-create-session [{:keys [region] :as auth} mfa-code]
  (let [session (->> (provider/lookup)
                     (session/find-or-create! auth mfa-code))]
    (if-not (:error-id session)
      (let [creds (->> (provider/resolve session)
                       (session/validate! region))]
        (if-not (:error-id creds)
          (provider/set! creds)
          creds))
      session)))

(defn clear-session [session-name]
  (session/clear! session-name))

(defn maybe-use-session [{:keys [session? session-name] :as auth}]
  (if (and session? (session/mfable?))
    (if-let [session (session/find session-name)]
      (provider/resolve session)
      (provider/resolve auth))
    (provider/resolve auth)))

(defn mfable? [role]
  (and (or role (session/get-role-arn))
       (session/get-mfa-arn)))

(defn login
  ([auth]
   (-> (maybe-use-session auth)
       (provider/set!)))
  ([{:keys [session-name region assume-role] :as auth} mfa-code]
   (-> (provider/resolve auth)
       (provider/set!))
   (if (mfable? assume-role)
     (find-or-create-session auth mfa-code)
     {:error-id :env-not-configured
      :msg      "AWS_ASSUME_ROLE_ARN or AWS_MFA_ARN not set"})))
