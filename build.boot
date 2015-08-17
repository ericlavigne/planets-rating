(set-env!
  :source-paths #{"src" "test"}
  :dependencies '[
                  [adzerk/boot-test "1.0.4" :scope "test"]
                  [clj-time "0.6.0"]
                  [clj-http "0.9.2"]
                  [org.clojure/data.json "0.2.5"]
                  [medley "0.6.0"]
                  [clj-aws-s3 "0.3.10"] ; :exclusions [joda-time]
                  [commons-io "2.4"]
                  ])

(require '[adzerk.boot-test :refer :all])

(task-options!
  repl {:init-ns 'vgap.rating}
)

