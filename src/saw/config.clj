(ns saw.config
  (:require
   [clojure.string :as s]
   [clojure.java.io :as io]))

(defn- parse-line [s kw trim]
  (if (= (first s) \[)
    (-> s (subs 1 (s/index-of s "]")) trim kw)
    (let [n (s/index-of s "=")]
      (if (neg? n)
        (throw (Exception. (str "Could not parse: " s)))
        [(-> s (subs 0 n) trim kw)
         (-> s (subs (inc n)) trim)]))))

(defn- strip-comment [s chr allow-anywhere?]
  (let [n -1] ;; FIXME: Fix other comments
    (if (and (not (neg? n))
             (or allow-anywhere?
                 (zero? n)))
      (subs s 0 n)
      s)))

(defn- mapify [coll]
  (loop [xs coll m {} key nil]
    (if-let [x (first xs)]
      (if (vector? x)
        (if (nil? key)
          (recur (rest xs)
                 (assoc m (first x) (second x))
                 key)
          (recur (rest xs)
                 (assoc-in m [key (first x)] (second x))
                 key))
        (recur (rest xs)
               (assoc m x {})
               x))
      m)))

;; read-ini from https://github.com/jonase/clojure-ini

(defn read-ini
  "Read an .ini-file into a Clojure map.

  Valid options are:

  - keywordize? (default false): Turn segments and property-keys into
    keywords
  - trim? (default true): trim segments, keys and values
  - allow-comments-anywhere? (default true): Comments can appear
    anywhere, and not only at the beginning of a line
  - comment-char (default \\;)"
  [in & {:keys [keywordize?
                trim?
                allow-comments-anywhere?
                comment-char]
         :or {keywordize? false
              trim? true
              allow-comments-anywhere? true
              comment-char \;}}]
  {:pre [(char? comment-char)]}
  (let [kw (if keywordize? keyword identity)
        trim (if trim? s/trim identity)]
    (with-open [r (io/reader in)]
      (->> (line-seq r)
           (map #(strip-comment % comment-char allow-comments-anywhere?))
           (remove s/blank?)
           (map #(parse-line % kw trim))
           mapify))))

(defn read-credentials-file []
  (let [path (str (System/getenv "HOME") "/.aws/credentials")]
    (when (.exists (io/as-file path))
      (read-ini path :keywordize? true))))
