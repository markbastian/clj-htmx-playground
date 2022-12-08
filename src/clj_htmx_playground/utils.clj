(ns clj-htmx-playground.utils
  (:require [jsonista.core :as j]))

(defn to-json-str [m]
  (j/write-value-as-string m j/keyword-keys-object-mapper))