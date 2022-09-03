(ns cli
  (:require [clj-http.client :as client]
            [hickory.core :as hickory]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.java.io :as io]
            [taoensso.timbre :as log]
            [clojurewerkz.machine-head.client :as mqtt])
  (:import  [java.net URL URI MalformedURLException URISyntaxException]))

(defn third [x]
  (nth x 2))

(defn extract-code-from-dt [node]
  (when (and
         (vector? node)
         (<= 3 (count node)))
    (let [span (third node)]
      (when (and (vector? span) (= (first span) :span))
        (let [content (third span)]
          (when (and (string? content)
                     (re-matches #"....(-....)+" content))
            content))))))

(defn extract-codes-from-html [html-dump]
  (->> html-dump
       hickory/parse
       hickory/as-hiccup
       (tree-seq seqable? identity)
       (filter (fn [node]
                 (and (vector? node)
                      (= (first node) :td))))
       (keep extract-code-from-dt)))

(defn file-exists? [file-name]
  (.exists (io/file file-name)))

(defn url? [s]
  (try
    (URL. s) true
    (catch MalformedURLException _ false)))

(defn is-uri? [s]
  (try
    (URI. s) true
    (catch URISyntaxException _ false)))


(def cli-options
  ;; An option with a required argument
  [[nil "--mqtt-uri HOST" "mqtt-uri" :validate [is-uri? "mqtt-uri is malformed"]]
   [nil "--mqtt-topic TOPIC" "mqtt-topic" :default "idle-champions"]
   [nil "--state-file FILE" "state file path" :validate [file-exists? "file does not exist"]]
   [nil "--chest-combinations-url URL" "chest combinations url"
    :default "https://idlechampions.fandom.com/wiki/Combinations"
    :validate [url? "malformed url"]]
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
          html-dump (-> (client/get chest-combinations-url)
                        :body)
          available-codes (set (extract-codes-from-html html-dump))
          new-codes (clojure.set/difference available-codes seen-codes)]

      (log/debug "Available codes: " (vec available-codes))
      (log/debug "New codes: " (vec new-codes))

      (when (not-empty new-codes)
        (doseq [code new-codes]
          (some-> mqtt-conn (mqtt/publish mqtt-topic code 2)))

        (when state-file
          (->> (clojure.string/join "\n" available-codes)
              (spit state-file))))

      (some-> mqtt-conn mqtt/disconnect-and-close))))
