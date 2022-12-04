(ns clj-htmx-playground.examples.tailwind.simple
  (:require [hiccup.page :refer [html5 include-css include-js]]))

(def page
  (html5
    [:head
     [:meta {:charset "UTF-8"}]
     [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
     [:script {:src "https://cdn.tailwindcss.com"}]]
    [:body
     [:h1.text-3xl.font-bold.underline "Hello world!"]]))