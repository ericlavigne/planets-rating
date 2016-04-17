(ns vgap.turn-file
  (:require [clojure.data.json :as json]
            [clj-time.core :as time]
            [clj-time.format :as timef]
            [clj-time.coerce :as timec]))

(def nu-datetime-format (timef/formatter "MM/dd/YYYY h:mm:ss a"))

(defn parse-nu-datetime-as-date [datetime-string]
  "Parse Nu-style datetime like 9/23/2014 6:35:52 PM and return date (without time)"
  (.toLocalDate (timef/parse nu-datetime-format datetime-string)))

(defn date-to-iso-format [date]
  (timef/unparse (timef/formatter "YYYY-MM-dd") (.toDateTimeAtStartOfDay date)))

(defn nu-datetime-to-iso-date [nu-date]
  (date-to-iso-format (parse-nu-datetime-as-date nu-date)))

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
;     end condition (win, live, die)
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
;       end condition (resign, drop, complete)

(defn cleanup-json [turn-string]
  (string-replace turn-string
     ; NQ-PLS-70 (2014) game 100282 turn 0 - scores and planets are ] instead of []
     #"\"scores\"\s*:\s*\]" "\"scores\": []"
     #"\"planets\"\s*:\s*\]" "\"planets\": []"
     ; Bubble World (2011) game 21094 player 1 turn 0 - maps are also ] instead of []
     #"\"maps\"\s*:\s*\]" "\"maps\": []"
     ; Fast Start 19 (2010) game 1679 player 3 turn 49 - ships are ] instead of []
     #"\"ships\"\s*:\s*\]" "\"ships\": []"
     ; Leshy (2012) game 33663 player 1 turn 88
     ; Ships have history that doesn't match json-format: "history":1183,1814:1183,1814:
     ; Battle Star (2012) game 36293 player 1 turn 73 confuses this issue further by
     ; omitting the last colon.
     ; Current games represent history as list of maps (valid JSON) which should not
     ; be affected by this patch.
     #"\"history\":\d[\d,:]*" ""
     ))

(defn parse-json-with-autotermination
  "If JSON is truncated, try adding a few end characters to allow partial reading"
  ([json-str] (try
                (json/read-str json-str)
                (catch Exception e
                  (parse-json-with-autotermination json-str e 1))))
  ([json-str prev-exception attempts]
     (when (> attempts 9) (throw (ex-info "Giving up after 10 attempts to autoterminate" {} prev-exception)))
     (let [prev-msg (.getMessage prev-exception)
           new-json-str (cond (re-find #"inside string" prev-msg) (str json-str "\"")
                              (re-find #"inside object" prev-msg) (str json-str "}")
                              (re-find #"inside array" prev-msg) (str json-str "]")
                              (re-find #"key missing value in object" prev-msg) (string-replace json-str #",[^,]*\z" "")
                              (re-find #"[\-\d]+\z" json-str) (string-replace json-str #"[\-\d]+\z" " null")
                              :else (throw (ex-info (str "Unrecognized variety of JSON termination with final characters: "
                                                         (subs json-str (- (.length json-str) 20)))
                                                    {} prev-exception)))]
       (try (json/read-str new-json-str)
            (catch Exception e
              (parse-json-with-autotermination new-json-str e (inc attempts)))))))

(defn convert [turn-string]
  (let [cleaned (cleanup-json turn-string)
        data (if (.endsWith (clojure.string/trim cleaned) "}")
                 (json/read-str cleaned)
                 (do (println "Warning: Turn file does not end in '}'. Attempting auto-termination.")
                     (parse-json-with-autotermination cleaned)))
        settings (get data "settings")
        turn-num (get settings "turn")
        game (get data "game")
        player (get data "player")
        players (get data "players") ; Probably don't need this. Might fill in information for other missing turns.
        scores (get data "scores")
        planets (get data "planets")
        ships (get data "ships") ; Complicated. Will be useful later for estimating player's standing early in game. (firepower, influence, intel)
        bases (get data "starbases")
        relations (get data "relations") ; alliance, safe passage
        ; Skipping: notes, vcrs, races, hulls, ionstorms, nebulas, stock (starbase storage), minefields,
        ;           messages, mymessages, racehulls, beams, torpedoes, engines, advantages
        slot-num (get player "id")
        my-scores (first (filter #(= slot-num (get % "ownerid")) scores))
        ; Believe score unless missing or 0 in first few turns.
        score-planets (get my-scores "planets")
        score-planets (if (and score-planets
                               (or (> turn-num 5) (> score-planets 0)))
                          score-planets
                          (count (filter #(= slot-num (get % "ownerid")) planets)))
        score-bases (get my-scores "starbases")
        score-bases (if (and score-bases
                               (or (> turn-num 5) (> score-bases 0)))
                        score-bases
                        (count (clojure.set/intersection (set (map #(get % "planetid") bases))
                                                         (set (map #(get % "id")
                                                                   (filter #(= slot-num (get % "ownerid"))
                                                                           planets))))))
        score-capital (get my-scores "capitalships")
        score-capital (if (and score-capital
                               (or (> turn-num 5) (> score-capital 0)))
                          score-capital
                          (count (filter #(and (= slot-num (get % "ownerid"))
                                               (> (get % "beams") 0))
                                         ships)))
        score-freighter (get my-scores "freighters")
        score-freighter (if (and score-freighter
                                 (or (> turn-num 5) (> score-freighter 0)))
                            score-freighter
                            (count (filter #(and (= slot-num (get % "ownerid"))
                                                 (= (get % "beams") 0))
                                           ships)))
        relations (get data "relations")
        ]
    (sorted-map
       :game-name (get settings "name")
       :turn-num turn-num
       :turn-date (let [date-string (get game "lasthostdate")
                        scheduled-string (get settings "hoststart")]
                    (or (when-not (empty? date-string)
                          (nu-datetime-to-iso-date date-string))
                        (when-not (empty? scheduled-string)
                          (nu-datetime-to-iso-date scheduled-string))))
       :max-allies (get settings "maxallies")
       :map-width (get settings "mapwidth")
       :map-height (get settings "mapheight")
       :map-planets (get settings "numplanets")
       :fascist-double-beams (get settings "fascistdoublebeams")
       :end-turn (get settings "endturn") ; Only important if fixed-turn win condition - how to know that?
       :game-description (get game "description")
       :game-short-description (get game "shortdescription") ; Such as "Custom Game". Might be the "game type" wanted for ladder.
       :game-created (get game "datecreated")
       :game-type (get game "gametype") ; Don't know what this is. NQ-PLS-70 (2014) was 2.
       :win-condition (get game "wincondition") ; NQ-PLS-70 (2014) was 4 on last turn. Maybe different on earlier turns. Need translation.
       :host-username (get game "createdby")
       :total-slots (get game "slots")
       :game-id (get game "id")
       :account-id (get player "accountid")
       :account-name (get player "username")
       :turn-joined (get player "turnjoined")
       :slot-num slot-num
       :race (get player "raceid")
       :team (get player "teamid")
       :priority-points (get player "prioritypoints")
       :active-hulls (if (contains? player "activehulls")
                         (vec (.split (get player "activehulls") ","))
                         [])
       :active-advantages (if (contains? player "activeadvantages")
                              (vec (.split (get player "activeadvantages") ","))
                              [])
       :score-planets score-planets
       :score-capital score-capital
       :score-freighter score-freighter
       :score-bases score-bases
       :score-military (get my-scores "militaryscore")
       :allies (set (map #(get % "playertoid")
                         (filter #(= 4 (% "relationfrom") (% "relationto"))
                                 relations)))
       )))

