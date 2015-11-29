(ns vgap.game-file
  (:require [vgap.rating :as rating]
            [vgap.turn-file :as turn]))

(defn convert-turns-for-game-file [game-file]
  (rating/zip-file-map game-file turn/convert))

(defn write-example-turns-for-game-id-to-file [game-id file-name]
  (let [game-file (rating/fetch-game-full-from-s3 game-id)
        turns (convert-turns-for-game-file game-file)
        sorted-turns (sort-by (fn [t] [(:turn-num t) (:slot-num t) (:account-id t)]) turns)]
    (with-open [w (clojure.java.io/writer file-name)]
      (binding [clojure.pprint/*print-right-margin* 120]
        (clojure.pprint/pprint sorted-turns w)
        file-name))))

(defn convert-turns-to-game [turns]
  (let [turns (filter #(> (:turn-num %) 0) turns) ; Remove turn 0, which doesn't seem meaningful and has a lot of missing data.
        turn (first turns)
        turn-num-to-turns (group-by :turn-num turns)
        turn-nums (sort (keys turn-num-to-turns))
        planets (into (sorted-map)
                  (map (fn [turn-num]
                         [turn-num
                          (into (sorted-map)
                            (map (fn [turn] [(:slot-num turn) (:score-planets turn)])
                                 (get turn-num-to-turns turn-num)))])
                       turn-nums))
        bases (into (sorted-map)
                (map (fn [turn-num]
                       [turn-num
                        (into (sorted-map)
                          (map (fn [turn] [(:slot-num turn) (:score-bases turn)])
                               (get turn-num-to-turns turn-num)))])
                     turn-nums))
        warships (into (sorted-map)
                   (map (fn [turn-num]
                          [turn-num
                           (into (sorted-map)
                             (map (fn [turn] [(:slot-num turn) (:score-capital turn)])
                                  (get turn-num-to-turns turn-num)))])
                        turn-nums))
        freighters (into (sorted-map)
                     (map (fn [turn-num]
                            [turn-num
                             (into (sorted-map)
                               (map (fn [turn] [(:slot-num turn) (:score-freighter turn)])
                                    (get turn-num-to-turns turn-num)))])
                          turn-nums))
        military-score (into (sorted-map)
                         (map (fn [turn-num]
                                [turn-num
                                 (into (sorted-map)
                                   (map (fn [turn] [(:slot-num turn) (:score-military turn)])
                                        (get turn-num-to-turns turn-num)))])
                              turn-nums))
        max-turn (apply max turn-nums)
        ; Highest planet count at end, ignoring alliances and winning conditions
        winning-slot (first (last (sort-by #(get % 1) (get planets max-turn))))
        slot-num-to-turns (group-by :slot-num turns)
        slot-nums (sort (keys slot-num-to-turns))
        slots (into (sorted-map)
                (map (fn [slot-num]
                       [slot-num
                        (let [turns (sort-by :turn-num (get slot-num-to-turns slot-num))]
                          (sorted-map :race (:race (last turns))
                            :players
                            (vec (reduce (fn [players turn]
                                           (let [previous (last players)]
                                             (cond
                                               ; Turn can't contribute
                                               (#{"open" "dead"} (:account-name turn)) players
                                               ; Turn matches last - modify last record
                                               (and previous
                                                    (or (= (:account-name previous) (:account-name turn))
                                                        (and (:account-id previous) (= (:account-id previous) (:account-id turn)))))
                                               (concat (butlast players)
                                                       [(sorted-map
                                                          :account-name (:account-name turn)
                                                          :account-id (if (zero? (:account-id turn))
                                                                          (:account-id previous)
                                                                          (or (:account-id previous) (:account-id turn)))
                                                          :start-turn (:start-turn previous)
                                                          :start-date (or (:start-date previous) (:turn-date turn))
                                                          :end-turn (:turn-num turn)
                                                          :end-date (or (:turn-date turn) (:end-date previous)))])
                                               ; Different player - turn creates new record
                                               :else
                                               (concat players [(sorted-map
                                                                  :account-name (:account-name turn)
                                                                  :account-id (if (zero? (:account-id turn))
                                                                                  nil
                                                                                  (:account-id turn))
                                                                  :start-turn (:turn-num turn)
                                                                  :end-turn (:turn-num turn)
                                                                  :start-date (:turn-date turn)
                                                                  :end-date (:turn-date turn))])
                                            )))
                                         []
                                         turns))))])
                     slot-nums))
        turn-data (into (sorted-map)
                    (map (fn [turn-num]
                           [turn-num {:date (:turn-date (first (get turn-num-to-turns turn-num)))}])
                         turn-nums))
       ]
    (sorted-map
      :id (:game-id turn)
      :name (:game-name turn)
      :short-description (:game-short-description turn)
      :win-condition (:win-condition turn)
      :winners [winning-slot]
      :planets planets
      :bases bases
      :warships warships
      :freighters freighters
      :military-score military-score
      :slots slots
      :turns turn-data
     )
  ))

