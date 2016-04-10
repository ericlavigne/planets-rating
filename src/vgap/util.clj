(ns vgap.util
  (:require [clojure.edn :as edn])
  (import java.io.StringWriter))

(defn sum [list-of-numbers]
  (apply + list-of-numbers))

(defn setting [k]
  (let [s (edn/read-string (slurp "settings.clj"))]
    (s k)))

(defn string-replace
  "Like clojure.string/replace but supports multiple regex/replacement pairs"
  [string regex replacement & others]
    (let [string (clojure.string/replace string regex replacement)]
      (if (empty? others)
          string
          (apply string-replace string (first others) (second others) (drop 2 others)))))

(defn pprint-edn [data]
  (let [w (StringWriter.)]
    (binding [clojure.pprint/*print-right-margin* 120]
      (clojure.pprint/pprint data w)
      (.toString w))))

