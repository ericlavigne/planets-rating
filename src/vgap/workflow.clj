(ns vgap.workflow
  (:require [aws.sdk.s3 :as s3]
            [vgap.game-file :as game-file]
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

(defn transform-game-full-to-summary-in-s3 ; 3.5 minutes
  ([gameid]
    (transform-game-full-to-summary-in-s3 gameid {:access-key (setting :aws-access-key) :secret-key (setting :aws-secret-key)}))
  ([gameid creds]
    (let [full-game-file (storage/fetch-game-full-from-s3 gameid creds)
          turns (game-file/convert-turns-for-game-file full-game-file)
          _ (.delete full-game-file)
          game-summary (game-file/convert-turns-to-game turns)
          formatted-summary (pprint-edn game-summary)
          _ (s3/put-object creds "vgap" (str "game/summary/" gameid ".edn") formatted-summary)]
      nil)))

(defn transform-all-game-full-to-summary-in-s3
  ([] (transform-all-game-full-to-summary-in-s3 {:access-key (setting :aws-access-key) :secret-key (setting :aws-secret-key)}))
  ([creds]
    (let [available (storage/fetch-game-ids-from-s3 creds)
          already-have (storage/fetch-game-summary-ids-from-s3 creds)
          remaining (clojure.set/difference (set available) (set already-have))]
      (doseq [game-id (shuffle remaining)]
        (println (str "Transforming game " game-id))
        (try
          (transform-game-full-to-summary-in-s3 game-id creds)
          (catch Exception exception
            (clojure.stacktrace/e)))))))

