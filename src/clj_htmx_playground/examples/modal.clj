(ns clj-htmx-playground.examples.modal
  (:require [hiccup.page :refer [html5 include-css include-js]]
            [ring.util.http-response :refer [ok]]))

(def page
  (html5
    (include-css "https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css")
    (include-js
      "https://unpkg.com/htmx.org@1.8.4"
      "https://unpkg.com/htmx.org/dist/ext/ws.js"
      "https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.bundle.min.js")
    [:button#mainButton.btn.btn-primary
     {:type           "button"
      :data-bs-toggle "modal"
      :data-bs-target "#exampleModal"}
     "Show modal"]
    [:div#exampleModal.modal.fade
     {:tabindex "-1"}
     [:div.modal-dialog
      [:div.modal-content
       [:div.modal-header
        [:h1#exampleModalLabel.modal-title.fs-5 "Enter new value"]
        [:button.btn-close
         {:type            "button"
          :data-bs-dismiss "modal"}]]
       [:form
        [:div.modal-body
         [:div.form-group
          [:label.col-form-label {:for "recipient-name"} "New value:"]
          [:input#recipient-name.form-control {:type "text"
                                               :name "new-button-text"}]]]
        [:div.modal-footer
         [:button.btn.btn-secondary
          {:type            "button"
           :data-bs-dismiss "modal"}
          "Close"]
         [:button.btn.btn-primary
          {:type            "button"
           :data-bs-dismiss "modal"
           :hx-target       "#mainButton"
           :hx-post         "/modal/save"}
          "Save changes"]]]]]]))

(defn replace-button [label]
  (html5
    [:button#mainButton.btn.btn-primary
     {:type           "button"
      :data-bs-toggle "modal"
      :data-bs-target "#exampleModal"}
     label]))

(def routes
  [["/modal" {:handler (fn [_] (ok page))}]
   ["/modal/save" {:post (fn [request]
                           (let [label (-> request :params :new-button-text)]
                             (ok (replace-button label))))}]])