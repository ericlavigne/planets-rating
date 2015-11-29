(ns vgap.game-file-test
  (:require [clojure.test :refer :all]
            [vgap.game-file :as game]))

(deftest nq-pls-70-2014
  (testing "Confirm that NQ-PLS-70 (2014) is parsed with correct content"
    (let [g (game/convert-turns-to-game (read-string (slurp "test/vgap/turn_list_examples/nq-pls-2014.txt")))]
      (is (= "NQ-PLS-70" (:name g)))
      (is (= 100282 (:id g)))
      (is (= 1 (get-in g [:planets 1 6])) "I (borg) had 1 planet on the first turn.")
      (is (= 1 (get-in g [:bases 1 6])))
      (is (= 0 (get-in g [:warships 1 6])))
      (is (= 1 (get-in g [:freighters 1 6])))
      (is (= 107 (get-in g [:planets 102 6])) "I (borg) had 107 planets on the last turn.")
      (is (= 12 (get-in g [:bases 102 6])))
      (is (= 36 (get-in g [:warships 102 6])))
      (is (= 5 (get-in g [:freighters 102 6])))
      (is (= 491566 (get-in g [:military-score 102 6])))
      (is (= [10] (:winners g)) "Yahoud won the game (Rebels, slot 10)")
      (is (= (get-in g [:slots 5])
             {:race 5
              :players [{:account-name "incideous" :account-id 1887 :start-turn 1 :start-date "2014-06-19" :end-turn 85 :end-date "2015-01-12"}
                        {:account-name "scotty2beam" :account-id 18497 :start-turn 86 :start-date "2015-01-14" :end-turn 102 :end-date "2015-03-21"}]}))
    )))

(deftest ally-victory
  (testing "Madonna - two allied players can win together"
    (let [g (game/convert-turns-to-game (read-string (slurp "test/vgap/turn_list_examples/madonna.txt")))]
      (is (= "Madonna Sector" (:name g)))
      (is (= [6 5] (:winners g))))))

