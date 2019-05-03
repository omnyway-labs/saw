(ns saw.core
  (:require
   [clojure.java.io :as io]
   [saw.provider :as provider]
   [saw.session :as session]
   [saw.util :refer [error-as-value] :as u]))

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

(defn clear-session []
  (session/clear!))

(defn- mfable? [role]
  (and role (session/get-mfa-arn)))

(defn- maybe-use-session [{:keys [session? assume-role]
                          :as auth}]
  (if (and session? (mfable? assume-role))
    (if-let [session (session/find)]
      (provider/resolve session)
      (provider/resolve auth))
    (provider/resolve auth)))

(defn- assume-role! [region role session-name creds]
  (let [st (session/assume-role region role session-name creds)]
    (->> (provider/resolve st)
         (provider/set!))
    st))

(defn- find-or-create! [{:keys [region] :as auth} mfa-code]
  (if mfa-code
    (->> (provider/resolve auth)
         (session/create! region mfa-code))
    (session/find)))

(defn- resolve-session [region session]
  (when-not (u/error? session)
    (->> (provider/resolve session)
         (session/validate! region))))

(defn lookup-role [env]
  (let [f (str (System/getenv "HOME") "/.saw")]
    (when (.exists (io/as-file f))
      (-> (slurp f)
          read-string
          (get env)))))

(defn- gen-session-name []
  (str (System/currentTimeMillis)))

(defn login
  ([auth]
   (if (keyword? auth)
     (when-let [role (lookup-role auth)]
       (login (session) role nil))
     (-> (maybe-use-session auth)
         (provider/set!))))
  ([auth role]
   (login auth role nil))
  ([{:keys [region] :as auth} role mfa-code]
   (error-as-value
    (let [session (find-or-create! auth mfa-code)
          creds   (resolve-session region session)
          session-name (gen-session-name)]
      (if-let [error (u/some-error session creds)]
        error
        (assume-role! region role session-name creds))))))
