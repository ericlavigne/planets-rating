(ns vgap.storage-test
  (:require [clojure.data.json :as json]
            [clojure.test :refer :all]
            [vgap.storage :refer :all]))

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

