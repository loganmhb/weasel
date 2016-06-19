(ns weasel.http
  (:require [clj-time.core :as time]
            [clj-time.format :as tfmt]
            [clojure.java.io :as io]
            [instaparse.core :as insta]))

(def transforms
  {:header (fn [& args] (vec args))
   :headers (fn [& args] [:headers (into {} args)])
   :request (fn [& args] (into {} args))})

(def parse-http (insta/parser (io/resource "minimal.ebnf")))

(defn parse-request [req]
  (insta/transform transforms
                   (parse-http req)))

(defn handler [request]
  (let [ok-body "<h1>Hello, world!</h1>"
        not-found-body "<h1>Not found :(</h1>"
        base-headers {"Date" (tfmt/unparse (:rfc822 tfmt/formatters)
                                           (time/now))
                      "Server" "Weasel 0.1.0"
                      "Content-Type" "text/html"
                      "Connection" "close"}]
    (if (= "/" (:path request))
      {:status 200
       :protocol (:protocol request)
       :headers (merge base-headers {"Content-Length" (count ok-body)})
       :body ok-body}
      {:status 404
       :protocol (:protocol request)
       :headers (merge base-headers {"Content-Length" (count not-found-body)})
       :body not-found-body})))

(defn format-response-line [resp]
  (let [status-msgs {200 "OK"
                     404 "Not Found"}]
    (str (:protocol resp) " "
         (:status resp) " "
         (status-msgs (:status resp)) "\r\n")))

(defn format-headers [headers]
  (apply str
         (for [[k v] headers]
           (str k ": " v "\r\n"))))

(defn format-response [resp]
  (str (format-response-line resp)
       (format-headers (:headers resp))
       "\r\n"
       (:body resp)))
