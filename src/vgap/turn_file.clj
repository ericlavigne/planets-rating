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

(defn convert [turn-string]
  (let [cleaned (string-replace turn-string
                  ; NQ-PLS-70 (2014) game 100282 turn 0 - scores and planets are ] instead of []
                  #"\"scores\"\s*:\s*\]" "\"scores\": []"
                  #"\"planets\"\s*:\s*\]" "\"scores\": []")
        data (json/read-str cleaned)
        settings (get data "settings")
        turn-num (get settings "turn")
        game (get data "game")
        player (get data "player")
        players (get data "players") ; Probably don't need this. Might fill in information for other missing turns.
        scores (get data "scores")
        ships (get data "ships") ; Complicated. Will be useful later for estimating player's standing early in game. (firepower, influence, intel)
        bases (get data "starbases")
        relations (get data "relations") ; alliance, safe passage
        ; Skipping: notes, vcrs, races, hulls, ionstorms, nebulas, stock (starbase storage), minefields,
        ;           messages, mymessages, racehulls, beams, torpedoes, engines, advantages
        slot-num (get player "id")
        my-scores (first (filter #(= slot-num (get % "ownerid")) scores))
        score-planets (get my-scores "planets")
        planets (get data "planets")
        score-planets (if (and score-planets (or (> turn-num 5) (> score-planets 0)))
                          score-planets ; Believe score unless missing or 0 in first few turns.
                          (count (filter #(= slot-num (get % "ownerid")) planets)))
        ]
    {:game-name (get settings "name")
     :turn-num turn-num
     :turn-start (get settings "hostcompleted") ; Not sure about this mapping
     :turn-end (get settings "hoststart") ; Not sure about this mapping
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
     :score-capital (get my-scores "capitalships")
     :score-freighter (get my-scores "freighters")
     :score-bases (get my-scores "starbases")
     :score-military (get my-scores "militaryscore")
     }))

