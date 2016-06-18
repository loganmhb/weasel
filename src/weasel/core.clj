(ns weasel.core
  (:import [java.net ServerSocket]
           [java.io InputStreamReader BufferedReader PrintWriter])
  (:require [clojure.string :as str]
            [clojure.java.io :as io]))


(defn read-request [client-sock]
  (with-open [input (io/reader client-sock)]))


(defn handle-client [client-sock req-handler]
  (try
    (let [request (read-request client-sock)]
      (with-open [output (io/writer client-sock)]
        (.write output (req-handler request))
        (.flush output)))
    (finally (.close client-sock))))


(defprotocol Lifecycle
  (start [this])
  (stop [this]))


(defn serve [socket]
  (let [running (atom true)]
    (future
      (while @running
        (let [client-sock (.accept sock)]
          (future (handle-client client-sock))))
      (.close socket))
    running))


(defrecord Server [port socket stop?]
  Lifecycle
  (start [this]
    (if (:socket this)
      this
      (let [new-socket (ServerSocket. port)]
        (-> this
            (assoc :socket new-socket)
            (assoc :stop? (serve new-socket))))))
  (stop [this]
    (swap! (:stop? this) false)))
