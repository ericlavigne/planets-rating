(ns user
  (:require [clojure.tools.namespace.repl :refer (refresh)]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.pprint :refer (pprint)]
            [clojure.repl :refer :all]
            [clojure.test :as test]
            [clj-time.core :as time]
            [clj-time.format :as timef]
            [clj-time.coerce :as timec]
            [clojure.data.json :as json]
            [vgap.game-file :as game-file]
            [vgap.turn-file :as turn-file]
            [vgap.rating :as rating]))

(def example-games
  [{:game-id 100282 :game-name "nq-pls-2014" :turns #{[6 0] [6 1] [6 102]}}
   {:game-id 94061 :game-name "madonna" :turns #{[5 167] [6 167] [11 167]}}])

(defn refresh-example-files
  ([] (doseq [g example-games] (refresh-example-files g)))
  ([g]
    (let [rated-ids (set (map :game-id (rating/fetch-rated-games-from-nu)))
          s3-ids (set (rating/fetch-game-ids-from-s3))
          game-id (:game-id g)
          game-desc (str (:game-name g) " (" game-id ")")]
      (if (not (rated-ids game-id))
        (println "Skipping" game-desc "which is not on the list of rated games")
        (do (when-not (s3-ids game-id)
              (println "Transferring" game-desc "from Nu to S3")
              (time (rating/transfer-game-full-nu-to-s3 game-id)))
            (let [_ (println "Fetching" game-desc "from S3")
                  raw-game-file (time (rating/fetch-game-full-from-s3 game-id))
                  _ (println "Parsing turns for" game-desc)
                  turns (time (rating/zip-file-map raw-game-file
                                (fn [turn-string]
                                  (let [turn (turn-file/convert turn-string)
                                        slot-turn [(:slot-num turn) (:turn-num turn)]]
                                    (when ((:turns g) slot-turn)
                                      (spit (str "test/vgap/turn_examples/" (:game-name g) "-p" (slot-turn 0) "-t" (slot-turn 1) ".trn")
                                            turn-string))
                                    turn))))
                  sorted-turns (sort-by (fn [t] [(:turn-num t) (:slot-num t) (:account-id t)]) turns)
                  game-file-name (str "test/vgap/turn_list_examples/" (:game-name g) ".txt")]
              (println "Writing" game-desc "turn list to" game-file-name)
              (with-open [w (clojure.java.io/writer game-file-name)]
                (binding [clojure.pprint/*print-right-margin* 120]
                  (clojure.pprint/pprint sorted-turns w)))))))))

(defn refresh-turn-list-examples []
  (time (game-file/write-example-turns-for-game-id-to-file 100282 "test/vgap/turn_list_examples/nq-pls-2014.txt"))
)

(defn convert-game [game-name]
  (time (game-file/convert-turns-to-game (read-string (slurp (str "test/vgap/turn_list_examples/" game-name ".txt"))))))

(defn convert-nq-pls []
  (convert-game "nq-pls-2014"))

(defn show-game [game-name]
  (binding [clojure.pprint/*print-right-margin* 120]
    (pprint (convert-game game-name))))

(defn show-nq-pls []
  (show-game "nq-pls-2014"))

(defn convert-nq-pls-turn []
  (turn-file/convert (slurp "test/vgap/turn_examples/nq-pls-2014-p6-t102.trn")))

(defn show-nq-pls-turn []
  (binding [clojure.pprint/*print-right-margin* 120]
    (pprint (convert-nq-pls-turn))))

(defn parse-turn-json [file-name]
  (let [turn-string (slurp (str "test/vgap/turn_examples/" file-name ".trn"))
        cleaned (turn-file/cleanup-json turn-string)]
    (json/read-str cleaned)))

(def m11 (parse-turn-json "madonna-p11-t167"))

(pprint (m11 "relations"))

