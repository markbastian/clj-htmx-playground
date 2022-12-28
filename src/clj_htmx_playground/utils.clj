(ns clj-htmx-playground.utils
  (:require
    [clojure.pprint :as pp]
    [jsonista.core :as j]))

(defn to-json-str [m]
  (j/write-value-as-string m j/keyword-keys-object-mapper))

(defn read-json [str]
  (j/read-value str j/keyword-keys-object-mapper))

(defn print-params [request]
  (pp/pprint
    (select-keys
      request
      [:params :parameters :form-params :path-params :query-params])))
