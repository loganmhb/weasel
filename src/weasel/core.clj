(ns weasel.core
  (:import [java.net ServerSocket Socket]
           [java.io InputStream])
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [weasel.http :as http]))


(defn transition-state
  "State machine for finding the end of an HTTP request."
  [current-state next-char]
  (let [expect-char (fn [c succ-state]
                      (if (= next-char c) succ-state :start))]
    (case current-state
      :start (expect-char \return :first-return)
      :first-return (expect-char \newline :first-newline)
      :first-newline (expect-char \return :second-return)
      :second-return (expect-char \newline :success)
      :start)))


(defn read-http-request
  "Since Instaparse can only work with strings, not streams, we need to pull
  off the request line and headers before parsing them. This is pretty easy -
  we just have to look for a sequence of CR LF CR LF using a little state machine.

  Does NOT close the stream -- we need it in case there's a body."
  ([^InputStream stream] (read-http-request stream :start ""))
  ([^InputStream stream state request]
   (let [next-char (char (.read stream))
         new-state (transition-state state next-char)
         req-with-char (str request next-char)]
     (if (= new-state :success)
       req-with-char
       (recur stream new-state req-with-char)))))


(defn handle-http-client
  "Handles HTTP requests on the given socket."
  [^Socket client-sock request-handler]
  (try
    (let [request (read-http-request (.getInputStream client-sock))
          response (-> request
                       http/parse-request
                       http/handler
                       http/format-response)]
      (with-open [output (io/writer client-sock)]
        (.write output response)
        (.flush output)))
    (finally (.close client-sock))))


(defn start-server
  "Starts a TCP server on the given port.
  Uses `client-handler' to process incoming connections. Returns a
  function that will stop the server."
  [port client-handler]
  (let [run (atom true)]
    (future (try (with-open [server-socket (ServerSocket. port)]
                   (while @run
                     (client-handler (.accept server-socket))))
                 (catch Exception e
                   (println "Problem!" e))))
    (fn [] (reset! run false))))


(def stop-server (start-server 9123 #(handle-http-client % http/handler)))
#_(stop-server)
