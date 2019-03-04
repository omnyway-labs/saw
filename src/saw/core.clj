(ns saw.core
  (:require
   [saw.provider :as provider]
   [saw.session :as session]))

(defn creds
  ([] (provider/lookup))
  ([auth] (provider/resolve auth)))

(defn session
  ([] (session/find nil))
  ([session-name]
   (session/find session-name)))

(defn validate-session
  ([region]
   (->> (session)
        (validate-session region)))
  ([region creds]
   (session/validate! region creds)))


(defn clear-session []
  (session/clear!))

(defn mfable? [role]
  (and role (session/get-mfa-arn)))

(defn maybe-use-session [{:keys [session? assume-role]
                          :as auth}]
  (if (and session? (mfable? assume-role))
    (if-let [session (session/find)]
      (provider/resolve session)
      (provider/resolve auth))
    (provider/resolve auth)))

(defn assume-role! [region role session-name creds]
  (let [st (session/assume-role region role session-name creds)]
    (->> (provider/resolve st)
         (provider/set!))
    st))

(defn login
  ([auth]
   (-> (maybe-use-session auth)
       (provider/set!)))
  ([auth mfa-code role]
   (login auth mfa-code role "saw"))
  ([{:keys [region] :as auth} mfa-code role session-name]
   (-> (provider/resolve auth)
       (provider/set!))
   (if (mfable? role)
     (let [session (->> (provider/resolve auth)
                        (session/find-or-create! auth mfa-code))
           creds   (->> (provider/resolve session)
                        (session/validate! region))]
       (if-not (and (:error-id session) role)
         (assume-role! region role session-name creds)
         session))
     {:error-id :env-not-configured
      :msg      "AWS_MFA_ARN not set"})))
