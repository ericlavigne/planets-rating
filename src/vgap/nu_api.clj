(ns vgap.nu-api
  (:require [clj-http.client :as client]
            [clj-time.format :as timef]
            [clojure.data.json :as json]
            [clojure.java.io :as io]))

(def datetime-format (timef/formatter "MM/dd/YYYY h:mm:ss a"))

(defn parse-datetime-as-date [datetime-string]
  "Parse Nu-style datetime like 9/23/2014 6:35:52 PM and return date (without time)"
  (.toLocalDate (timef/parse datetime-format datetime-string)))

(defn fetch-rated-games [] ; 5 seconds, basic info about all games, should filter by :ended property
  (let [api-games (json/read-str
                          (:body (client/get "http://api.planets.nu/games/list?status=2,3"
                                             {:query-params {"status" "2,3"}})))
        games (map (fn [g]
                     (let [end-date (if (g "dateended") (parse-datetime-as-date (g "dateended")))
                           create-date (parse-datetime-as-date (g "datecreated"))]
                       (merge {:name (g "name") :description (g "description")
                               :created create-date
                               :game-id (g "id")}
                              (if (and end-date (not (.isBefore end-date create-date)))
                                  {:ended end-date}
                                  {}))))
                   api-games)
        rated-games (remove (fn [g] (re-find #"mentor"
                                             (.toLowerCase (:description g))))
                            games)
        ]
    rated-games))

(defn fetch-game-full [gameid] ; 70 sec
  (let [req (client/get "http://api.planets.nu/game/loadall"
                        {:query-params {"gameid" gameid}
                         :as :byte-array})
        _ (assert (= 200 (:status req)) (str "Status: " (:status req)))
        _ (assert (< 1000 (count (:body req))) (str "Response too small to be game file: " (apply str (map char (:body req)))))
        tmp-file (java.io.File/createTempFile (str "game-" gameid "-") ".zip")]
    (with-open [w (io/output-stream tmp-file)]
      (.write w (:body req)))
    tmp-file))

