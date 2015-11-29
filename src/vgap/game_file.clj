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
        turn-nums (sort (keys turn-num-to-turns))]
    {:id (:game-id turn)
     :name (:game-name turn)
     :short-description (:game-short-description turn)
     :win-condition (:win-condition turn)
     ; winners - not sure how to get this. calculate based on win condition and relationships?
     :planets (into (sorted-map)
                (map (fn [turn-num]
                       [turn-num
                        (into (sorted-map)
                          (map (fn [turn] [(:slot-num turn) (:score-planets turn)])
                               (get turn-num-to-turns turn-num)))])
                     turn-nums))
     :bases (into (sorted-map)
              (map (fn [turn-num]
                     [turn-num
                      (into (sorted-map)
                        (map (fn [turn] [(:slot-num turn) (:score-bases turn)])
                             (get turn-num-to-turns turn-num)))])
                   turn-nums))
     :warships (into (sorted-map)
                 (map (fn [turn-num]
                        [turn-num
                         (into (sorted-map)
                           (map (fn [turn] [(:slot-num turn) (:score-capital turn)])
                                (get turn-num-to-turns turn-num)))])
                      turn-nums))
     :freighters (into (sorted-map)
                   (map (fn [turn-num]
                          [turn-num
                           (into (sorted-map)
                             (map (fn [turn] [(:slot-num turn) (:score-freighter turn)])
                                  (get turn-num-to-turns turn-num)))])
                        turn-nums))
     :military-score (into (sorted-map)
                       (map (fn [turn-num]
                              [turn-num
                               (into (sorted-map)
                                 (map (fn [turn] [(:slot-num turn) (:score-military turn)])
                                      (get turn-num-to-turns turn-num)))])
                            turn-nums))
     }
  ))

