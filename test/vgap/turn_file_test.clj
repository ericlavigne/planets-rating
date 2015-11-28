(ns vgap.turn-file-test
  (:require [clojure.test :refer :all]
            [vgap.turn-file :as turn]))

(deftest nq-pls-70-2014
  (testing "NQ-PLS-70 (2014) last turn"
    (let [t (turn/convert (slurp "test/vgap/turn_examples/player6-turn102.trn"))]
      (is (= "NQ-PLS-70" (:game-name t)))))
  (testing "NQ-PLS-70 (2014) turn 0 - happy if it parses without exceptions"
    (let [t (turn/convert (slurp "test/vgap/turn_examples/player6-turn0.trn"))]
      (is (= "NQ-PLS-70" (:game-name t)))))
   )

