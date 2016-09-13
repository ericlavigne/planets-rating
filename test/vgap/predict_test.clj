(ns vgap.predict-test
  (:require [clojure.test :refer :all]
            [vgap.predict :refer :all]))

(deftest reasonable-predictions-test
  (testing "Predict game result for 2014 NQ-PLS-70"
    (let [game (read-string (slurp "test/vgap/game_examples/nq-pls-2014.txt"))]
      (is (= 102 (final-turn game)))
      (is (= (final-placements game)
             [10 6 11 5 4 7 1 2 3 8 9])))))
