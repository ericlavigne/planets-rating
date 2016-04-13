(ns vgap.whisperer
  (:require [vgap.storage :as storage]
            [vgap.util :as util]))

(defn export []
  (let [game-ids (storage/fetch-game-summary-ids-from-s3)]
    (doseq [game-id game-ids]
      (let [game (read-string (storage/fetch-game-summary-from-s3 game-id))
            abridged (dissoc game :bases :freighters :turns :warships)
            max-turn (apply max (keys (:turns game)))
            max-turn-date (get-in game [:turns max-turn :date])
            max-turn-year (aget (.split (or max-turn-date "unknown") "-") 0)
            file-name (str "whisperer/" max-turn-year "/game-" game-id ".edn")]
        (clojure.java.io/make-parents file-name)
        (spit file-name (util/pprint-edn abridged))))))

