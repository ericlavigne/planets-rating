(ns vgap.turn-file-test
  (:require [clojure.test :refer :all]
            [vgap.turn-file :as turn]))

(defn load-turn [name]
  (turn/convert (slurp (str "test/vgap/turn_examples/" name ".trn"))))

(deftest nq-pls-70-2014
  (testing "NQ-PLS-70 (2014) last turn"
    (let [t (load-turn "nq-pls-2014-p6-t102")]
      (is (= "NQ-PLS-70" (:game-name t)))
      (is (= 107 (:score-planets t)))
      (is (= 12 (:score-bases t)))
      (is (= 36 (:score-capital t)))
      (is (= 5 (:score-freighter t)))
      (is (= 491566 (:score-military t)))
      ))
  (testing "NQ-PLS-70 (2014) turn 0 - happy if it parses without exceptions"
    (let [t (load-turn "nq-pls-2014-p6-t0")]
      (is (= "NQ-PLS-70" (:game-name t)))))
  (testing "NQ-PLS-70 (2014) turn 1 - scoreboard shows all zeros but we want correct numbers"
    (let [t (load-turn "nq-pls-2014-p6-t1")]
      (is (= "NQ-PLS-70" (:game-name t)))
      (is (= 1 (:score-planets t)))
      (is (= 1 (:score-bases t)))
      (is (= 0 (:score-capital t)))
      (is (= 1 (:score-freighter t)))
    ))
  )

(deftest madonna-2014
  (testing "Madonna - allies"
    (let [p5 (load-turn "madonna-p5-t167")
          p6 (load-turn "madonna-p6-t167")
          p11 (load-turn "madonna-p11-t167")]
      (is (= (:allies p5) #{6}))
      (is (= (:allies p6) #{5}))
      (is (= (:allies p11) #{}))))
  )

(deftest bubble-2011
  (testing "Bubble - can parse player 1 turn 0 (previously unmatched ])"
    (load-turn "bubble-p1-t0"))
  (testing "Bubble - can parse player 5 turn 71 (JSON truncated mid-string)"
    (let [p5t71 (load-turn "bubble-p5-t71")]
      (is (= 137 (:score-planets p5t71)))))
  (testing "Bubble - winner has correct planets near end"
    (let [p3t91 (load-turn "bubble-p3-t91")]
      (is (= 152 (:score-planets p3t91)))))
  )

