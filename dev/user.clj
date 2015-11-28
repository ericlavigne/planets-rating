(ns user
  (:require [clojure.tools.namespace.repl :refer (refresh)]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.pprint :refer (pprint)]
            [clojure.repl :refer :all]
            [clojure.test :as test]
            [vgap.game-file :as game-file]
            [vgap.turn-file :as turn-file]
            [vgap.rating :as rating]))

(defn refresh-test-files []
  (time (game-file/write-example-turns-for-game-id-to-file 100282 "test/vgap/turn_list_examples/nq-pls-70-2014.txt")))

(defn convert-nq-pls []
  (time (game-file/convert-turns-to-game (read-string (slurp "test/vgap/turn_list_examples/nq-pls-70-2014.txt")))))

