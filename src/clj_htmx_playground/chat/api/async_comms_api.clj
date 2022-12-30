(ns clj-htmx-playground.chat.api.async-comms-api)

(defprotocol IAsyncComms
  (send! [context username message])
  (broadcast! [context users message]))