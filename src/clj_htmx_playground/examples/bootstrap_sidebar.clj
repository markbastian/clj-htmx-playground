(ns clj-htmx-playground.examples.bootstrap-sidebar
  (:require
    [clojure.java.io :as io]
    [hiccup.page :refer [html5 include-css include-js]]))

(def sidebar
  (html5
    (include-css "https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css")
    (include-js
      "https://unpkg.com/htmx.org@1.8.4"
      "https://unpkg.com/htmx.org/dist/ext/ws.js"
      "https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.bundle.min.js")
    [:style (slurp (io/resource "clj_htmx_playground/sidebars.css"))]
    [:div.flex-shrink-0.p-3.bg-white {:style "width: 280px;"}
     [:a.d-flex.align-items-center.pb-3.mb-3.link-dark.text-decoration-none.border-bottom {:href "/"}
      [:svg.bi.me-2 {:width "30" :height "24"} [:use {:xlink:href "#bootstrap"}]]
      [:span.fs-5.fw-semibold "Collapsible"]]
     [:ul.list-unstyled.ps-0
      [:li.mb-1
       [:button.btn.btn-toggle.align-items-center.rounded.collapsed {:data-bs-toggle "collapse" :data-bs-target "#home-collapse" :aria-expanded "true"} "Home"]
       [:div#home-collapse.collapse.show
        [:ul.btn-toggle-nav.list-unstyled.fw-normal.pb-1.small
         [:li [:a.link-dark.rounded {:href "#"} "Overview"]]
         [:li [:a.link-dark.rounded {:href "#"} "Updates"]]
         [:li [:a.link-dark.rounded {:href "#"} "Reports"]]]]]
      [:li.mb-1
       [:button.btn.btn-toggle.align-items-center.rounded.collapsed {:data-bs-toggle "collapse" :data-bs-target "#dashboard-collapse" :aria-expanded "false"} "Dashboard"]
       [:div#dashboard-collapse.collapse
        [:ul.btn-toggle-nav.list-unstyled.fw-normal.pb-1.small
         [:li [:a.link-dark.rounded {:href "#"} "Overview"]]
         [:li [:a.link-dark.rounded {:href "#"} "Weekly"]]
         [:li [:a.link-dark.rounded {:href "#"} "Monthly"]]
         [:li [:a.link-dark.rounded {:href "#"} "Annually"]]]]]
      [:li.mb-1
       [:button.btn.btn-toggle.align-items-center.rounded.collapsed {:data-bs-toggle "collapse" :data-bs-target "#orders-collapse" :aria-expanded "false"} "Orders"]
       [:div#orders-collapse.collapse
        [:ul.btn-toggle-nav.list-unstyled.fw-normal.pb-1.small
         [:li [:a.link-dark.rounded {:href "#"} "New"]]
         [:li [:a.link-dark.rounded {:href "#"} "Processed"]]
         [:li [:a.link-dark.rounded {:href "#"} "Shipped"]]
         [:li [:a.link-dark.rounded {:href "#"} "Returned"]]]]]
      [:li.border-top.my-3]
      [:li.mb-1
       [:button.btn.btn-toggle.align-items-center.rounded.collapsed {:data-bs-toggle "collapse" :data-bs-target "#account-collapse" :aria-expanded "false"} "Account"]
       [:div#account-collapse.collapse
        [:ul.btn-toggle-nav.list-unstyled.fw-normal.pb-1.small
         [:li [:a.link-dark.rounded {:href "#"} "New..."]]
         [:li [:a.link-dark.rounded {:href "#"} "Profile"]]
         [:li [:a.link-dark.rounded {:href "#"} "Settings"]]
         [:li [:a.link-dark.rounded {:href "#"} "Sign out"]]]]]]]))