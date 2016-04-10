(defproject vgap-rating "0.1.0-SNAPSHOT"
  :description "Replacement rating system for PlanetsNu"
  :url "http://planets.nu"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :source-paths ["src"]
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [clj-time "0.6.0"]
                 [clj-http "0.9.2"]
                 [org.clojure/data.json "0.2.5"]
                 [medley "0.6.0"]
                 [clj-aws-s3 "0.3.10" :exclusions [joda-time]]
                 [commons-io "2.4"]]
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[org.clojure/tools.namespace "0.2.3"]
                                  [org.clojure/java.classpath "0.2.0"]]}}
  :jvm-opts ["-Xms1g" "-Xmx1g"])

