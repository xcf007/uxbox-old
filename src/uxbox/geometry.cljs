(ns uxbox.geometry)

(defn coords->rect
  "Given the (x1,y1) and (x2,y2) coordinates return the rectangle that
  define as (top-left corner, width, height)"
  [x1 y1 x2 y2]
  (let [rect-x (if (> x1 x2) x2 x1)
        rect-y (if (> y1 y2) y2 y1)
        rect-width (if (> x1 x2) (- x1 x2) (- x2 x1))
        rect-height (if (> y1 y2) (- y1 y2) (- y2 y1))]
    [rect-x rect-y rect-width rect-height]))

(defn clientcoord->viewportcord
  [client-x client-y]
  (if-let [canvas-element (.getElementById js/document "page-canvas")]
    (let [bounding-rect (.getBoundingClientRect canvas-element)
          offset-x (.-left bounding-rect)
          offset-y (.-top bounding-rect)
          new-x (- client-x offset-x)
          new-y (- client-y offset-y)]
      [new-x new-y])
    [client-x client-y]))

(defn viewportcord->clientcoord
  [viewport-x viewport-y]
  (if-let [canvas-element (.getElementById js/document "page-canvas")]
      (let [bounding-rect (.getBoundingClientRect canvas-element)
            offset-x (.-left bounding-rect)
            offset-y (.-top bounding-rect)
            new-x (+ viewport-x offset-x)
            new-y (+ viewport-y offset-y)]
        [new-x new-y])
      [viewport-x viewport-y]))

(defn slope [x1 y1 x2 y2]
  (/ (- y1 y2) (- x1 x2)))

(defn distance [x1 y1 x2 y2]
  (let [deltax (- x1 x2)
        deltay (- y1 y2)
        deltaxsq (* deltax deltax)
        deltaysq (* deltay deltay)]
    .sqrt js/Math (+ deltaxsq deltaysq)))
