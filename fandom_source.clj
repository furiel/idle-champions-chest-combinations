(ns fandom-source
  (:require [clj-http.client :as client]
            [hickory.core :as hickory]
            [taoensso.timbre :as log]))

;; https://idlechampions.fandom.com/wiki/Combinations

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

(defn collect []
  (let [html-dump (-> (client/get "https://idlechampions.fandom.com/wiki/Combinations")
                      :body)
        codes (set (extract-codes-from-html html-dump))]
    (log/debug "fandom codes: " codes)
    codes))
