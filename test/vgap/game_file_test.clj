(ns vgap.game-file-test
  (:require [clojure.test :refer :all]
            [vgap.game-file :as game]))

(deftest nq-pls-70-2014
  (testing "Confirm that NQ-PLS-70 (2014) is parsed with correct content"
    (let [g (game/convert-turns-to-game (read-string (slurp "test/vgap/turn_list_examples/nq-pls-70-2014.txt")))]
      (is (= "NQ-PLS-70" (:name g)))
      (is (= 100282 (:id g)))
      (is (= 1 (get-in g [:planets 1 6])) "I (borg) had 1 planet on the first turn.")
      (is (= 107 (get-in g [:planets 102 6])) "I (borg) had 107 planets on the last turn.")
    )))

