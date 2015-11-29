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
     #"\"planets\"\s*:\s*\]" "\"scores\": []"))

(defn convert [turn-string]
  (let [cleaned (cleanup-json turn-string)
        data (json/read-str cleaned)
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
       :active-hulls (vec (.split (get player "activehulls") ","))
       :active-advantages (vec (.split (get player "activeadvantages") ","))
       :score-planets score-planets
       :score-capital score-capital
       :score-freighter score-freighter
       :score-bases score-bases
       :score-military (get my-scores "militaryscore")
       )))

