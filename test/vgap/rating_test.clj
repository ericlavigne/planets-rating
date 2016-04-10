(ns vgap.rating-test
  (:require [clojure.test :refer :all]
            [vgap.rating :refer :all]
            [clojure.data.json :as json]
  ))

(def pls-expectations
  {:game-id 100282 :name "NQ-PLS-70"})

(deftest fetch-rated-games-from-nu-test
  (testing "Fetched games from Nu include 2014 NQ-PLS-70"
    (let [all-games (fetch-rated-games-from-nu)
          nq-pls-70-games (filter #(= (:game-id %)
                                      (:game-id pls-expectations))
                                  all-games)
          nq-pls-70-game (first nq-pls-70-games)]
      (is (= (:name pls-expectations)
             (:name nq-pls-70-game)))
      (is (= 1 (count nq-pls-70-games)))
  ))
)

(deftest fetch-game-from-s3-test
  (testing "Fetch and parse game file from S3 for 2014 NQ-PLS-70 (expect parse errors on turn 0)"
    (let [game-file (fetch-game-full-from-s3 100282)]
      (is (= {"JSON error (unexpected character): ]" 11, "NQ-PLS-70" 1122}
             (frequencies (zip-file-map game-file
                                        (fn [s] (try
                                                  (get-in (json/read-str s)
                                                          ["settings" "name"])
                                                (catch Exception e
                                                  (.getMessage e)))))))))))

