(ns vgap.nu-api-test
  (:require [clojure.test :refer :all]
            [vgap.nu-api :as nu]))

(deftest fetch-rated-games-from-nu-test
  (testing "Fetched games from Nu include 2014 NQ-PLS-70"
    (let [pls-expectations {:game-id 100282 :name "NQ-PLS-70"}
          all-games (nu/fetch-rated-games)
          nq-pls-70-games (filter #(= (:game-id %)
                                      (:game-id pls-expectations))
                                  all-games)
          nq-pls-70-game (first nq-pls-70-games)]
      (is (= (:name pls-expectations)
             (:name nq-pls-70-game)))
      (is (= 1 (count nq-pls-70-games)))
  ))
)
