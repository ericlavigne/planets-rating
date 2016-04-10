(ns vgap.rating
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]
            [clj-time.core :as time]
            [clj-time.format :as timef]
            [clj-time.coerce :as timec]
            [medley.core :as m]
            [clojure.java.io :as io]
            [aws.sdk.s3 :as s3]
            [clojure.edn :as edn]
  )
  (:import java.io.FileInputStream
           java.util.zip.ZipInputStream))

(defn setting [k]
  (let [s (edn/read-string (slurp "settings.clj"))]
    (s k)))

(def nu-datetime-format (timef/formatter "MM/dd/YYYY h:mm:ss a"))

(defn parse-nu-datetime-as-date [datetime-string]
  "Parse Nu-style datetime like 9/23/2014 6:35:52 PM and return date (without time)"
  (.toLocalDate (timef/parse nu-datetime-format datetime-string)))

(defn string-replace
  "Like clojure.string/replace but supports multiple regex/replacement pairs"
  [string regex replacement & others]
    (let [string (clojure.string/replace string regex replacement)]
      (if (empty? others)
          string
          (apply string-replace string (first others) (second others) (drop 2 others)))))

(defn fetch-rated-games-from-nu [] ; 5 seconds, basic info about all games, should filter by :ended property
  (let [api-games (json/read-str
                          (:body (client/get "http://api.planets.nu/games/list?status=2,3"
                                             {:query-params {"status" "2,3"}})))
        games (map (fn [g]
                     (let [end-date (if (g "dateended") (parse-nu-datetime-as-date (g "dateended")))
                           create-date (parse-nu-datetime-as-date (g "datecreated"))]
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

(defn fetch-game-full-from-nu [gameid] ; 70 sec
  (let [req (client/get "http://api.planets.nu/game/loadall"
                        {:query-params {"gameid" gameid}
                         :as :byte-array})
        _ (assert (= 200 (:status req)) (str "Status: " (:status req)))
        _ (assert (< 1000 (count (:body req))) (str "Response too small to be game file: " (apply str (map char (:body req)))))
        tmp-file (java.io.File/createTempFile (str "game-" gameid "-") ".zip")]
    (with-open [w (io/output-stream tmp-file)]
      (.write w (:body req)))
    tmp-file))

(defn transfer-game-full-nu-to-s3 ; 2.5 minutes
  ([gameid]
     (transfer-game-full-nu-to-s3 gameid {:access-key (setting :aws-access-key) :secret-key (setting :aws-secret-key)}))
  ([gameid creds]
     (let [game-from-nu (fetch-game-full-from-nu gameid)]
       (s3/put-multipart-object creds "vgap" (str "game/loadall/" gameid ".zip") game-from-nu)
       (.delete game-from-nu))
       nil))

(defn fetch-game-ids-from-s3
  ([] (fetch-game-ids-from-s3 {:access-key (setting :aws-access-key) :secret-key (setting :aws-secret-key)}))
  ([creds]
     (letfn [(convert-to-game-id [s3-obj]
               (let [game-id-str (string-replace (:key s3-obj) #"game/loadall/" "" #"\.zip" "")
                     game-id (Integer/parseInt game-id-str)]
                 (assert (= game-id-str (str game-id)))
                 game-id))]
       (loop [game-ids-so-far [] from-marker nil]
         (let [response (s3/list-objects creds "vgap"
                          (merge {:prefix "game/loadall/"}
                                 (if from-marker {:marker from-marker} {})))
               new-game-ids (map convert-to-game-id (:objects response))
               game-ids-so-far (concat game-ids-so-far new-game-ids)]
           (if (:truncated? response)
             (recur game-ids-so-far (:next-marker response))
             (set game-ids-so-far)))))))

(defn fetch-game-full-from-s3 ; Fetching from s3 takes about 10 seconds
  ([gameid]
     (fetch-game-full-from-s3 gameid {:access-key (setting :aws-access-key) :secret-key (setting :aws-secret-key)}))
  ([gameid creds]
     (let [s3-res (s3/get-object creds "vgap" (str "game/loadall/" gameid ".zip"))
           s3-bytes (org.apache.commons.io.IOUtils/toByteArray (:content s3-res))
           tmp-file (java.io.File/createTempFile (str "game-" gameid "-") ".zip")]
       (with-open [w (io/output-stream tmp-file)]
         (.write w s3-bytes))
       tmp-file)))

(defn delete-game-full-from-s3
  ([gameid] (delete-game-full-from-s3 gameid {:access-key (setting :aws-access-key) :secret-key (setting :aws-secret-key)}))
  ([gameid creds] (s3/delete-object creds "vgap" (str "game/loadall/" gameid ".zip"))))

(defn transfer-completed-rated-games-to-s3 ; May take days for first run.
  ([] (transfer-completed-rated-games-to-s3 {:access-key (setting :aws-access-key) :secret-key (setting :aws-secret-key)}))
  ([creds] (let [available (map :game-id (filter :ended (fetch-rated-games-from-nu)))
                 already-have (fetch-game-ids-from-s3 creds)
                 remaining (clojure.set/difference (set available) (set already-have))]
             (doseq [game-id (shuffle remaining)]
               (println (str "Transfering game " game-id))
               (transfer-game-full-nu-to-s3 game-id creds)))))

; http://www.thecoderscorner.com/team-blog/java-and-jvm/12-reading-a-zip-file-from-java-using-zipinputstream
;
; (first (zip-file-map (fetch-game-full-from-s3 100282) (fn [s] (apply str (take 50 s)))))
;
;   => "{\"settings\": {\"name\":\"NQ-PLS-70\",\"turn\":0,\"buildqu"
;
; Applying to large game, such as 100282 example above, takes about two minutes.
;
(defn zip-file-map [file fun]
  (let [buffer (byte-array 2048)
        fis (FileInputStream. file)
        zis (ZipInputStream. fis)
        res (loop [zip-entry (.getNextEntry zis)
                   zip-entry-pieces []
                   results []]
              (if (nil? zip-entry)
                  results
                  (let [len (.read zis buffer)]
                    (if (> len 0)
                        (recur zip-entry
                               (conj zip-entry-pieces (String. buffer 0 len))
                               results)
                        (recur (.getNextEntry zis)
                               []
                               (conj results (fun (apply str zip-entry-pieces))))))))]
    (.close zis)
    res))

