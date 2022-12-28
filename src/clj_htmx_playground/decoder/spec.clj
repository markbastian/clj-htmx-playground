(ns clj-htmx-playground.decoder.spec
  (:require [malli.generator :as mg]))

(def schema
  [:map
   [:players
    [:vector
     [:map
      [:name [:string {:min 1 :max 10}]]
      [:team [:enum :black :white]]]]]
   [:black
    [:map
     [:words [:vector {:min 4 :max 4} :string]]]]])

(mg/generate schema)