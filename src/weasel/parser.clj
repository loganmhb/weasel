(ns weasel.parser
  (:require [instaparse.core :as insta]
            [clojure.java.io :as io]))


(def parse-http (insta/parser (io/resource "http.abnf")))


