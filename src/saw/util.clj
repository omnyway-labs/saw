(ns saw.util
  (:require
   [clojure.string :as str]))

(defn read-string-safely
  "Call `read-string` if `s` is non-blank, with `*read-eval*`
  disabled."
  [s]
  (binding [*read-eval* false]
    (when (and (string? s) (not (str/blank? s)))
      (read-string s))))

(defn as-kebabs [s]
  (->> (str/replace s #"(?<!^)([A-Z][a-z]|(?<=[a-z])[A-Z])" "-$1")
       (str/lower-case)
       (keyword)))

(defn as-error [er]
  ;; FIXME: use regex
  (try
    (let [msg (str/split er #";")]
      {:error-id (-> (nth msg 2)
                     (str/split #":")
                     (nth 1)
                     (str/triml)
                     (as-kebabs))
       :msg      (-> (nth msg 0)
                     (str/split #"\(")
                     (nth 0)
                     (str/trimr))})
    (catch Exception e
      {:error-id :parse-error
       :msg       er})))

(defmacro error-as-value [& body]
  `(try
    ~@body
    (catch Exception e#
      (as-error (.getMessage e#)))))

(defn omethods [obj]
  (map #(.getName %) (-> obj class .getMethods)))
