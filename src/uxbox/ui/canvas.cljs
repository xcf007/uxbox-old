(ns uxbox.ui.canvas
  (:require
   rum
   [uxbox.streams :as s]
   [uxbox.ui.mixins :as mx]
   [uxbox.shapes.actions :as actions]
   [uxbox.ui.canvas.streams :as cs]
   [uxbox.shapes.protocols :as shapes]))

(rum/defc grid < rum/static
  [width height start-width start-height zoom]
  (let [padding (* 20 zoom)
        ticks-mod (/ 100 zoom)
        step-size (/ 10 zoom)

        vertical-ticks (range (- padding start-height) (- height start-height padding) step-size)
        horizontal-ticks (range (- padding start-width) (- width start-width padding) step-size)

        vertical-lines (fn
          [position value padding]
          (if (< (mod value ticks-mod) step-size)
             [:line {:key position
                     :y1 padding
                     :y2 width
                     :x1 position
                     :x2 position
                     :stroke "blue"
                     :stroke-width (/ 1 zoom)
                     :opacity 0.75}]
             [:line {:key position
                     :y1 padding
                     :y2 width
                     :x1 position
                     :x2 position
                     :stroke "blue"
                     :stroke-width (/ 1 zoom)
                     :opacity 0.25}]))

        horizontal-lines (fn
          [position value padding]
          (if (< (mod value ticks-mod) step-size)
             [:line {:key position
                     :y1 position
                     :y2 position
                     :x1 padding
                     :x2 height
                     :stroke "blue"
                     :stroke-width (/ 1 zoom)
                     :opacity 0.75}]
             [:line {:key position
                     :y1 position
                     :y2 position
                     :x1 padding
                     :x2 height
                     :stroke "blue"
                     :stroke-width (/ 1 zoom)
                     :opacity 0.25}]))]
    [:g.grid
     (map #(vertical-lines (+ %1 start-width) %1 padding) vertical-ticks)
     (map #(horizontal-lines (+ %1 start-height) %1 padding) horizontal-ticks)]))

(defonce canvas-coordinates (s/pipe-to-atom cs/canvas-coordinates))
(rum/defc debug-coordinates < rum/reactive
  []
  (let [[x y] (rum/react canvas-coordinates)]
    [:div
     {:style #js {:position "absolute"
                  :left "80px"
                  :top "20px"}}
     [:table
      [:tr
       [:td "X:"]
       [:td x]]
      [:tr
       [:td "Y:"]
       [:td y]]]]))

(rum/defc background < rum/static
  []
  [:rect
   {:x 0
    :y 0
    :width "100%"
    :height "100%"
    :fill "white"}])

(def draw! (s/pipe-to-atom cs/draw-stream))
(def move! (s/pipe-to-atom cs/move-stream))
(def drawing (s/pipe-to-atom cs/draw-in-progress))
(def selected-shapes (s/pipe-to-atom (s/map vals cs/selected)))
(def selected-ids (s/pipe-to-atom (s/map (comp set keys) cs/selected)))

(def shapes-push-mixin
  {:transfer-state (fn [old new]
                     (let [[_ _ shapes] (:rum/args new)]
                       (cs/set-current-shapes! shapes)
                       new))})

(rum/defc canvas < rum/reactive
                   shapes-push-mixin
                   (mx/cmds-mixin
                    [::draw draw! (fn [[conn page] shape]
                                    (actions/draw-shape conn page shape))]

                    [::move move! (fn [[conn] selections]
                                    (actions/update-shapes conn selections))])
  [conn
   page
   shapes
   {:keys [viewport-height
           viewport-width
           document-start-x
           document-start-y]}]
  (let [page-width (:page/width page)
        page-height (:page/height page)
        selection-uuids (rum/react selected-ids)
        selected-shapes (rum/react selected-shapes)
        raw-shapes (into []
                         (comp
                          (filter :shape/visible?)
                          (filter #(not (contains? selection-uuids (:shape/uuid %))))
                          (map :shape/data))
                         shapes)]
    [:svg#page-canvas
     {:x document-start-x
      :y document-start-y
      :width page-width
      :height page-height
      :on-mouse-down cs/on-mouse-down
      :on-mouse-up cs/on-mouse-up}
     (background)
     (apply vector :svg#page-layout (map shapes/shape->svg raw-shapes))
     (when-let [shape (rum/react drawing)]
       (shapes/shape->drawing-svg shape))
     (when-not (empty? selected-shapes)
       (let [rs selected-shapes]
         (vec (cons :g
                    (concat
                     (map shapes/shape->selected-svg rs)
                     (map shapes/shape->svg rs))))))]))
