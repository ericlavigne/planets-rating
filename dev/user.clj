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
            [vgap.game-file :as game-file]
            [vgap.turn-file :as turn-file]
            [vgap.rating :as rating]))

(defn refresh-test-files []
  (time (game-file/write-example-turns-for-game-id-to-file 100282 "test/vgap/turn_list_examples/nq-pls-70-2014.txt")))

(defn convert-nq-pls []
  (time (game-file/convert-turns-to-game (read-string (slurp "test/vgap/turn_list_examples/nq-pls-70-2014.txt")))))

(defn show-nq-pls []
  (binding [clojure.pprint/*print-right-margin* 120]
    (pprint (convert-nq-pls))))

(defn convert-nq-pls-turn []
  (turn-file/convert (slurp "test/vgap/turn_examples/player6-turn102.trn")))

(defn show-nq-pls-turn []
  (binding [clojure.pprint/*print-right-margin* 120]
    (pprint (convert-nq-pls-turn))))

