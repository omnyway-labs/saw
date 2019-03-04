(ns saw.core
  (:require
   [saw.provider :as provider]
   [saw.session :as session]
   [saw.util :refer [error-as-value] :as u]))

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

(defn find-or-create! [{:keys [region] :as auth} mfa-code]
  (u/error-as-value
   (if mfa-code
    (->> (provider/resolve auth)
         (session/create! region mfa-code))
    (session/find))))

(defn resolve-session [region session]
  (when-not (u/error? session)
    (->> (provider/resolve session)
         (session/validate! region))))

(defn login
  ([auth]
   (-> (maybe-use-session auth)
       (provider/set!)))
  ([auth role session-name]
   (login auth role session-name nil))
  ([{:keys [region] :as auth} role session-name mfa-code]
   (if (mfable? role)
     (let [session (find-or-create! auth mfa-code)
           creds   (resolve-session region session)]
       (if-not (or (u/error? session) (u/error? creds))
         (assume-role! region role session-name creds)
         (u/some-error session creds)))
     {:error-id :env-not-configured
      :msg      "AWS_MFA_ARN not set or no Role provided"})))
