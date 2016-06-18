(ns weasel.parser
  (:require [instaparse.core :as insta]
            [clojure.java.io :as io]))


(def transforms
  {:header (fn [[_ name] [_ val]] [name val])
   :headers (fn [& args] [:headers (into {} args)])
   :request (fn [& args] (into {} args))})

(def parse-http (insta/parser (io/resource "http.abnf")))

(defn parse-request [req]
  (insta/transform transforms
                   (parse-http req)))

