(ns saw.error)

(def errors
  {:role-arn-not-found-in-profile "No role_arn found for profile in credentials file"
   :region-not-found-in-profile   "No region found for profile in credentials file"
   :session-not-found             "Cannot assume role beacuse no Session not found"
   :role-arn-not-found            "AWS_MFA_ARN not set"
   :region-not-found              "Region not found in env or profile"
   :provider-not-resolved         "Cannot resolve Provider"
   :profile-not-found             "Profile not found"
   :invalid-creds-object          "Creds is not an instance of any CredentialsProvider. Try login first"
   :assume-role-failed            "Assume Role failed"
   :session-create-failed         "Session creation failed"
   :mfa-code-not-string           "Invalid MFA Code. Needs to be a string"
   :session-validaton-failed      "Session cannot be validated"
   :session-cache-failed          "Unable to persist session"
   :session-empty-cache-failed    "Session is empty and cannot be persisted"
   :session-lookup-failed         "~/.aws/session is empty or unreadable. Try login again"
   :provider-not-supported        "Invalid provider - try any of :default :env :static :profile"})

(defn assert!
  ([expr id]
   (assert! expr id {}))
  ([expr id info]
   (assert! expr id info nil))
  ([expr id info cause]
   (if (or expr (not *assert*))
     expr
     (throw
      (ex-info (get errors id)
               (assoc info
                      :error  id
                      :message (get errors id)
                      :type :saw-error
                      :expr expr)
               cause)))))

(defmacro error-as-value [& body]
  `(try
    ~@body
    (catch Exception e#
      (ex-data e#))))

(defmacro err [& body]
  `(try
    ~@body
    (catch Exception e#
      (:error (ex-data e#)))))

(defmacro with-error [id & body]
  `(try
    ~@body
    (catch Exception e#
      (throw
       (ex-info (get errors ~id)
                (merge (ex-data e#)
                       {:error  ~id
                        :message (.getMessage e#)
                        :cause (-> e#
                                   Throwable->map
                                   clojure.main/ex-triage
                                   clojure.main/ex-str)
                        :type :saw-error})
                nil)))))
