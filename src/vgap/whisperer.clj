(ns vgap.whisperer
  (:require [vgap.storage :as storage]
            [vgap.util :as util]))

(defn export []
  (let [game-ids (storage/fetch-game-summary-ids-from-s3)]
    (with-open [w (clojure.java.io/writer "whisperer-games.edn")]
      (.write w "[\n")
      (doseq [game-id game-ids]
        (let [game (read-string (storage/fetch-game-summary-from-s3 game-id))
              abridged (dissoc game :bases :freighters :turns :warships)]
          (.write w (util/pprint-edn abridged))
          (.write w "\n")))
      (.write w "]\n"))))

