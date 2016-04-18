(ns vgap.whisperer
  (:require [vgap.nu-api :as nu]
            [vgap.storage :as storage]
            [vgap.util :as util]))

(defn convert-game [game nu-game]
  (let [final-turn (apply max (keys (:planets game)))]
    (sorted-map
      :id (:id game)
      :name (:name game)
      :short-description (:short-description game)
      :win-condition (:win-condition game)
      :winners (:winners game)
      :final-planets (get-in game [:planets final-turn])
      :final-turn final-turn
      :end-date (get-in game [:turns final-turn :date])
      :final-military-scores (get-in game [:military-score final-turn])
      :slots (:slots game)
      :final-teams (get-in game [:teams final-turn])
      :team-game (:team-game game)
      :early-relations (into (sorted-map)
                         (map (fn [turn-num]
                                [turn-num (get-in game [:relations turn-num])])
                              (filter #(<= % 30)
                                      (keys (:relations game)))))
      :host (:host game)
      :min-nu-rank (:min-nu-rank nu-game)
      :max-nu-rank (:max-nu-rank nu-game)
    )))

(defn export []
  (let [game-ids (storage/fetch-game-summary-ids-from-s3)
        nu-games-by-id (group-by :game-id (nu/fetch-rated-games))]
    (doseq [game-id game-ids]
      (let [game (read-string (storage/fetch-game-summary-from-s3 game-id))
            nu-game (first (nu-games-by-id game-id))
            converted (convert-game game nu-game)
            final-turn (:final-turn converted)
            final-turn-date (:end-date converted)
            final-turn-year (aget (.split (or final-turn-date "unknown") "-") 0)
            file-name (str "whisperer/" final-turn-year "/game-" game-id ".edn")]
        (when (:min-nu-rank converted) (println (str "Found game with min rank: " file-name)))
        (clojure.java.io/make-parents file-name)
        (spit file-name (util/pprint-edn converted))))))

