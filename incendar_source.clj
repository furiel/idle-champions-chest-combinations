(ns incendar-source
  (:require [clj-http.client :as client]
            [hickory.core :as hickory]
            [taoensso.timbre :as log]))

;; https://incendar.com/idlechampions_codes.php

(defn third [x]
  (nth x 2))

(defn extract-code-from-area [node]
  (when (and
         (vector? node)
         (<= 3 (count node)))
    (-> node
        third
        clojure.string/split-lines)))

(defn extract-codes-from-html [html-dump]
  (->> html-dump
       hickory/parse
       hickory/as-hiccup
       (tree-seq seqable? identity)
       (filter (fn [node]
                 (and (vector? node)
                      (= (first node) :textarea))))
       (keep extract-code-from-area)
       (apply concat)
       (filter #(re-matches #"....(-....)+" %))
       set))

(defn collect []
  (let [html-dump (-> (client/get "https://incendar.com/idlechampions_codes.php")
                      :body)
        codes (extract-codes-from-html html-dump)]
    (log/debug "incendar codes: " codes)
    codes))
