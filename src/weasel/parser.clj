(ns weasel.parser
  (:require [instaparse.core :as insta]
            [clojure.java.io :as io]))


(def transforms
  {:HEADER (fn [[_ name] [_ val]] [name val])
   :HEADERS (fn [& args] [:HEADERS (into {} args)])})

(def parse-http (insta/parser (io/resource "http.abnf")))

