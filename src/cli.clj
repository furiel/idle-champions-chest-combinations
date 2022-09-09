(ns cli
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.java.io :as io]
            [taoensso.timbre :as log]
            [clojurewerkz.machine-head.client :as mqtt]
            [fandom-source]
            [incendar-source])
  (:import  [java.net URL URI MalformedURLException URISyntaxException]))

(defn file-exists? [file-name]
  (.exists (io/file file-name)))

(defn is-uri? [s]
  (try
    (URI. s) true
    (catch URISyntaxException _ false)))

(def cli-options
  ;; An option with a required argument
  [[nil "--mqtt-uri HOST" "mqtt-uri" :validate [is-uri? "mqtt-uri is malformed"]]
   [nil "--mqtt-topic TOPIC" "mqtt-topic" :default "idle-champions"]
   [nil "--state-file FILE" "state file path" :validate [file-exists? "file does not exist"]]
   ["-h" "--help"]])

(defn -main [& args]
  (let [{:keys [summary errors arguments]
         {:keys [mqtt-uri mqtt-topic state-file help chest-combinations-url]} :options} (parse-opts args cli-options)]
    (when (or errors (not-empty arguments))
      (log/error "Invalid arguments")
      (log/error errors)
      (System/exit 1))

    (when help
      (log/error summary))

    (let [mqtt-conn (some-> mqtt-uri (mqtt/connect {:opts {:auto-reconnect false}}))
          seen-codes (or (some-> state-file slurp clojure.string/split-lines set) #{})
          fandom-codes (fandom-source/collect)
          incendar-codes (incendar-source/collect)
          available-codes (clojure.set/union fandom-codes incendar-codes)
          new-codes (clojure.set/difference available-codes seen-codes)]
      (log/debug "new codes: " (vec new-codes))

      (when (not-empty new-codes)
        (doseq [code new-codes]
          (some-> mqtt-conn (mqtt/publish mqtt-topic code 2)))

        (when state-file
          (->> (clojure.string/join "\n" available-codes)
              (spit state-file))))

      (some-> mqtt-conn mqtt/disconnect-and-close))))
