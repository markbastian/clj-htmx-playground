(ns clj-htmx-playground.modal
  (:require [hiccup.page :refer [html5 include-css include-js]]))

(def page
  (html5
    [:button.btn.btn-primary {:type "button" :data-bs-toggle "modal" :data-bs-target "#exampleModal"} "Launch demo modal"]
    [:div#exampleModal.modal.fade {:tabindex "-1" :aria-labelledby "exampleModalLabel" :aria-hidden "true"}
     [:div.modal-dialog
      [:div.modal-content
       [:div.modal-header
        [:h1#exampleModalLabel.modal-title.fs-5 "Modal title"]
        [:button.btn-close {:type "button" :data-bs-dismiss "modal" :aria-label "Close"}]]
       [:div.modal-body "..."]
       [:div.modal-footer
        [:button.btn.btn-secondary {:type "button" :data-bs-dismiss "modal"} "Close"]
        [:button.btn.btn-primary {:type "button"} "Save changes"]]]]]))