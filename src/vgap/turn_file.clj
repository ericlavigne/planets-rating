(ns vgap.turn-file
  (:require [clojure.data.json :as json]))

(defn string-replace
  "Like clojure.string/replace but supports multiple regex/replacement pairs"
  [string regex replacement & others]
    (let [string (clojure.string/replace string regex replacement)]
      (if (empty? others)
          string
          (apply string-replace string (first others) (second others) (drop 2 others)))))

; For JSON validatin issues use https://jsonformatter.curiousconcept.com/

(defn convert [turn-string]
  (let [cleaned (string-replace turn-string
                  ; NQ-PLS-70 (2014) game 100282 turn 0 - scores and planets are ] instead of []
                  #"\"scores\"\s*:\s*\]" "\"scores\": []"
                  #"\"planets\"\s*:\s*\]" "\"scores\": []")
        data (json/read-str cleaned)]
    {:game-name (get-in data ["settings" "name"])}))

