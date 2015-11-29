(ns vgap.turn-file-test
  (:require [clojure.test :refer :all]
            [vgap.turn-file :as turn]))

(deftest nq-pls-70-2014
  (testing "NQ-PLS-70 (2014) last turn"
    (let [t (turn/convert (slurp "test/vgap/turn_examples/nq-pls-2014-p6-t102.trn"))]
      (is (= "NQ-PLS-70" (:game-name t)))
      (is (= 107 (:score-planets t)))
      (is (= 12 (:score-bases t)))
      (is (= 36 (:score-capital t)))
      (is (= 5 (:score-freighter t)))
      (is (= 491566 (:score-military t)))
      ))
  (testing "NQ-PLS-70 (2014) turn 0 - happy if it parses without exceptions"
    (let [t (turn/convert (slurp "test/vgap/turn_examples/nq-pls-2014-p6-t0.trn"))]
      (is (= "NQ-PLS-70" (:game-name t)))))
  (testing "NQ-PLS-70 (2014) turn 1 - scoreboard shows all zeros but we want correct numbers"
    (let [t (turn/convert (slurp "test/vgap/turn_examples/nq-pls-2014-p6-t1.trn"))]
      (is (= "NQ-PLS-70" (:game-name t)))
      (is (= 1 (:score-planets t)))
      (is (= 1 (:score-bases t)))
      (is (= 0 (:score-capital t)))
      (is (= 1 (:score-freighter t)))
    ))
   )

