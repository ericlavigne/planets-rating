(ns vgap.workflow
  (:require [aws.sdk.s3 :as s3]
            [vgap.nu-api :as nu]
            [vgap.storage :as storage]
            [vgap.util :refer :all]))

(defn transfer-game-full-nu-to-s3 ; 2.5 minutes
  ([gameid]
     (transfer-game-full-nu-to-s3 gameid {:access-key (setting :aws-access-key) :secret-key (setting :aws-secret-key)}))
  ([gameid creds]
     (let [game-from-nu (nu/fetch-game-full gameid)]
       (s3/put-multipart-object creds "vgap" (str "game/loadall/" gameid ".zip") game-from-nu)
       (.delete game-from-nu))
       nil))

(defn transfer-completed-rated-games-to-s3 ; May take days for first run.
  ([] (transfer-completed-rated-games-to-s3 {:access-key (setting :aws-access-key) :secret-key (setting :aws-secret-key)}))
  ([creds] (let [available (map :game-id (filter :ended (nu/fetch-rated-games)))
                 already-have (storage/fetch-game-ids-from-s3 creds)
                 remaining (clojure.set/difference (set available) (set already-have))]
             (doseq [game-id (shuffle remaining)]
               (println (str "Transfering game " game-id))
               (transfer-game-full-nu-to-s3 game-id creds)))))

