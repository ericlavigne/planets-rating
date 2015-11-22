(ns vgap.turn-file
  (:require [clojure.data.json :as json]))

(defn string-replace
  "Like clojure.string/replace but supports multiple regex/replacement pairs"
  [string regex replacement & others]
    (let [string (clojure.string/replace string regex replacement)]
      (if (empty? others)
          string
          (apply string-replace string (first others) (second others) (drop 2 others)))))

; For JSON validation issues use https://jsonformatter.curiousconcept.com/

; Need to extract information that will be needed for game representation.
; ( Based roughly on http://help.planets.nu/Ladder )
;
;   game ID
;   game name
;   game type (campaign, classic, standard, melee, championship, MvM, custom public, custom private, senior officer, team, training)
;   win conditions
;   winner(s)
;   player slots:
;     slot number
;     race
;     players:
;       user ID
;       user name
;       start turn
;       start date
;       end turn
;       end date
;       start planets
;       start military
;       end planets
;       end military
;       end condition (resign, drop, win, FoF, survive)

(defn convert [turn-string]
  (let [cleaned (string-replace turn-string
                  ; NQ-PLS-70 (2014) game 100282 turn 0 - scores and planets are ] instead of []
                  #"\"scores\"\s*:\s*\]" "\"scores\": []"
                  #"\"planets\"\s*:\s*\]" "\"scores\": []")
        data (json/read-str cleaned)]
    {:game-name (get-in data ["settings" "name"])}))

