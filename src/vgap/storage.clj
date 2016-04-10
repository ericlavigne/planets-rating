(ns vgap.storage
  (:require [aws.sdk.s3 :as s3]
            [clojure.java.io :as io]
            [vgap.util :refer :all])
  (:import java.io.FileInputStream
           java.util.zip.ZipInputStream))

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

