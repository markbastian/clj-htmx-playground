(ns clj-htmx-playground.examples.echarts.pie
  (:require [clj-htmx-playground.utils :as u]
            [clojure.string :as str]
            [hiccup.page :refer [html5 include-js]]
            [ring.util.http-response :refer [ok]]))

(def bar-data {:title   {:text "ECharts Getting Started Example"}
               :tooltip {}
               :legend  {:data ["sales"]}
               :xAxis   {:data ["Shirts" "Cardigans" "Chiffons" "Pants" "Heels" "Socks"]}
               :yAxis   {}
               :series  [{:name "sales"
                          :type "bar"
                          :data [5 20 36 10 10 20]}]})

(def pie-data {:title   {:text    "Referer of a Website",
                         :subtext "Fake Data",
                         :left    "center"},
               :tooltip {:trigger "item"},
               :legend  {:orient "vertical",
                         :left   "left"},
               :series  [{:name     "Access From",
                          :type     "pie",
                          :radius   "50 %",
                          :data     [{:value 1048, :name "Search Engine"},
                                     {:value 735, :name "Direct"},
                                     {:value 580, :name "Email"},
                                     {:value 484, :name "Union Ads"},
                                     {:value 300, :name "Video Ads"}],
                          :emphasis {:itemStyle {:shadowBlur    10,
                                                 :shadowOffsetX 0,
                                                 :shadowColor   "rgba (0, 0, 0, 0.5) "}}}]})

(def data-all
  [
   [[10.0 8.04]
    [8.0 6.95]
    [13.0 7.58]
    [9.0 8.81]
    [11.0 8.33]
    [14.0 9.96]
    [6.0 7.24]
    [4.0 4.26]
    [12.0 10.84]
    [7.0 4.82]
    [5.0 5.68]]
   [[10.0 9.14]
    [8.0 8.14]
    [13.0 8.74]
    [9.0 8.77]
    [11.0 9.26]
    [14.0 8.1]
    [6.0 6.13]
    [4.0 3.1]
    [12.0 9.13]
    [7.0 7.26]
    [5.0 4.74]]
   [[10.0 7.46]
    [8.0 6.77]
    [13.0 12.74]
    [9.0 7.11]
    [11.0 7.81]
    [14.0 8.84]
    [6.0 6.08]
    [4.0 5.39]
    [12.0 8.15]
    [7.0 6.42]
    [5.0 5.73]]
   [[8.0 6.58]
    [8.0 5.76]
    [8.0 7.71]
    [8.0 8.84]
    [8.0 8.47]
    [8.0 7.04]
    [8.0 5.25]
    [19.0 12.5]
    [8.0 5.56]
    [8.0 7.91]
    [8.0 6.89]]])

(def mark-line-opt
  {:animation false,
   :label     {:formatter "y = 0.5 * x + 3",
               :align     "right"},
   :lineStyle {:type "solid"},
   :tooltip   {:formatter "y = 0.5 * x + 3"},
   :data      [[{:coord  [0, 3],
                 :symbol "none"}
                {:coord  [20, 13],
                 :symbol "none"}]]})

(def ascombe-quartet-data
  {:title   {:text "Anscombe quartet",
             :left "center",
             :top  0},
   :grid    [{:left "7%", :top "7%", :width "38%", :height "38%"},
             {:right "7%", :top "7%", :width "38%", :height "38%"},
             {:left "7%", :bottom "7%", :width "38%", :height "38%"},
             {:right "7%", :bottom "7%", :width "38%", :height "38%"}],
   :tooltip {:formatter "Group {a}: ({c})"},
   :xAxis   [{:gridIndex 0, :min 0, :max 20},
             {:gridIndex 1, :min 0, :max 20},
             {:gridIndex 2, :min 0, :max 20},
             {:gridIndex 3, :min 0, :max 20}],
   :yAxis   [{:gridIndex 0, :min 0, :max 15},
             {:gridIndex 1, :min 0, :max 15},
             {:gridIndex 2, :min 0, :max 15},
             {:gridIndex 3, :min 0, :max 15}],
   :series  [{:name       "I",
              :type       "scatter",
              :xAxisIndex 0,
              :yAxisIndex 0,
              :data       (data-all 0)
              :markLine   mark-line-opt},
             {:name       "II",
              :type       "scatter",
              :xAxisIndex 1,
              :yAxisIndex 1,
              :data       (data-all 1)
              :markLine   mark-line-opt},
             {:name       "III",
              :type       "scatter",
              :xAxisIndex 2,
              :yAxisIndex 2,
              :data       (data-all 2),
              :markLine   mark-line-opt},
             {:name       "IV",
              :type       "scatter",
              :xAxisIndex 3,
              :yAxisIndex 3,
              :data       (data-all 3)
              :markLine   mark-line-opt}]})

;(def s
;  "{\n  title: {\n    text: \"Anscombe's quartet\",\n    left: 'center',\n    top: 0\n  },\n  grid: [\n    { left: '7%', top: '7%', width: '38%', height: '38%' },\n    { right: '7%', top: '7%', width: '38%', height: '38%' },\n    { left: '7%', bottom: '7%', width: '38%', height: '38%' },\n    { right: '7%', bottom: '7%', width: '38%', height: '38%' }\n  ],\n  tooltip: {\n    formatter: 'Group {a}: ({c})'\n  },\n  xAxis: [\n    { gridIndex: 0, min: 0, max: 20 },\n    { gridIndex: 1, min: 0, max: 20 },\n    { gridIndex: 2, min: 0, max: 20 },\n    { gridIndex: 3, min: 0, max: 20 }\n  ],\n  yAxis: [\n    { gridIndex: 0, min: 0, max: 15 },\n    { gridIndex: 1, min: 0, max: 15 },\n    { gridIndex: 2, min: 0, max: 15 },\n    { gridIndex: 3, min: 0, max: 15 }\n  ],\n  series: [\n    {\n      name: 'I',\n      type: 'scatter',\n      xAxisIndex: 0,\n      yAxisIndex: 0,\n      data: dataAll[0],\n      markLine: markLineOpt\n    },\n    {\n      name: 'II',\n      type: 'scatter',\n      xAxisIndex: 1,\n      yAxisIndex: 1,\n      data: dataAll[1],\n      markLine: markLineOpt\n    },\n    {\n      name: 'III',\n      type: 'scatter',\n      xAxisIndex: 2,\n      yAxisIndex: 2,\n      data: dataAll[2],\n      markLine: markLineOpt\n    },\n    {\n      name: 'IV',\n      type: 'scatter',\n      xAxisIndex: 3,\n      yAxisIndex: 3,\n      data: dataAll[3],\n      markLine: markLineOpt\n    }\n  ]\n};")
;
;(println (-> s
;             (str/replace #"([a-zA-Z]+):" (fn [[_ s]] (format ":%s" s)))
;             (str/replace #"'" "\"")))

(def page
  (html5
    (include-js
      "https://unpkg.com/htmx.org@1.8.4"
      "https://cdnjs.cloudflare.com/ajax/libs/echarts/5.4.3/echarts.min.js"
      "echarts.js")
    [:div#bar
     {:style "width: 600px;height:400px;"}
     [:script {:type "text/javascript"}
      (format "makeChart('%s', %s)" "bar" (u/to-json-str bar-data))]]
    [:div#pie
     {:style "width: 600px;height:400px;"}
     [:script {:type "text/javascript"}
      (format "makeChart('%s', %s)" "pie" (u/to-json-str pie-data))]]
    [:div#ansq
     {:style "width: 600px;height:400px;"}
     [:script {:type "text/javascript"}
      (format "makeChart('%s', %s)" "ansq" (u/to-json-str ascombe-quartet-data))]]))

(def routes
  [["/pie" {:handler (fn [_] (ok page))}]])