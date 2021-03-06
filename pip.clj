(ns pip
  "Implementation of Perceptually Important Points algorithm"
  (:gen-class)
  (:require [clojure.contrib.math :as math])
  (:use [incanter core charts processing])
  (:use clojure.test))


(defn max-key-index
  "Returns [key, index] where (f (k item)) is the greatest. Most
applicable to vectors."
  [f k & coll]
  (apply max-key f (map #(vec [(k %1) %2]) coll (range (count coll)))))

(defn max-key-value
  "Returns [key, value] where (f (k item)) is the greatest. Most applicable to vectors."
  [f k & coll]
  (apply max-key f (map #(vec [(k %1) %1]) coll)))



(defn euclidian-distance
  [[x1 y1] [x2 y2]]
  (math/sqrt (+ (math/expt (- x1 x2) 2)
		(math/expt (- y1 y2) 2))))


(defn distance
  "Receives three points in [x y] format"
  [distance-fn first last point]
  (+ (distance-fn first point) (distance-fn last point)))


(defn max-distance
  "Returns [distance, index of point] where distance is greatest. The extremes are excluded from candidates so the index returned is offseted by 1. pre: to > from + 1."
  [series from to]
  (let [calc-dist (partial distance euclidian-distance (nth series from) (nth series to))]
    (apply max-key-index first calc-dist (subvec series (+ 1 from) to))))


(def max-distance (memoize max-distance))


(defn max-distance-over-intervals 
  "Find new candidate pip for each interval, and returns [[distance index-point-in-interval] interval] for the interval where distance of the pip is the greatest."
  [series pips]
  (let [candidate-intervals (filter (fn [[p1 p2]] (> p2 (+ 1 p1))) pips)]
    (when-not (empty? candidate-intervals)  
      (apply max-key-value ffirst #(apply max-distance series %) candidate-intervals))))


(defn pip*
  "Expect series as vector of points. pips is a set of [start end] intervals which are defined by the important points extracted so far. Given the current intervals, this function chooses a new important point with max distance across all segments. The new point is used to split one of the current segments."
  [series pips]
  (if-let [[[_ relative-index-pip] [p1 p2 :as interval-to-split]] (max-distance-over-intervals series pips)]
    (let [index-new-pip (+ 1 p1 relative-index-pip)]
      (conj (disj pips interval-to-split) 
	    [p1 index-new-pip] [index-new-pip p2]))))


(defn pip-seq
  "Lazy strategy using a recursive definition."
  [series pips]
  (if-let [new-pips (pip* series pips)]
    (cons new-pips
	  (lazy-seq (pip-seq series new-pips)))))




(defn find-pips 
  "Returns lazy sequence of segments defined by the indices of PIPs. The extreme points are given as the first set of PIPs."
  ([series]  
     (let [extremes [0 (- (count series) 1)]]
       (conj (pip-seq series #{extremes}) #{extremes})))

  ([k series] 
     (sort (distinct (flatten (vec (last (take k (find-pips series)))))))))



(defn animate-chart [k series]
  (let [p (line-chart [] [])
	v (view p)]
    (doseq [pips (to-points (sort-pips (take k (find-pips series))) series)] 
      (add-categories p 
		      (flatten (map (fn [[ [x1 _] [x2 _]]] [x1 x2]) pips))
		      (flatten (map (fn [[ [_ y1] [_ y2]]] [y1 y2]) pips)) )
      (. Thread (sleep 500) ))))



(defn sort-pips [pips] 
  (map #(sort-by first %) pips))


(defn to-points 
  [pips-indices series]
  (map #(map (fn [[p1 p2]] [(nth series p1) (nth series p2)]) %) pips-indices))





(defn animate-chart [k series]
  (let [p (line-chart [] [])
	v (view p)]
    (doseq [pips (to-points (sort-pips (take k (find-pips series))) series)] 
      (add-categories p 
		      (flatten (map (fn [[ [x1 _] [x2 _]]] [x1 x2]) pips))
		      (flatten (map (fn [[ [_ y1] [_ y2]]] [y1 y2]) pips)) )
      (. Thread (sleep 500) ))))








;tests TODO: completar!
(deftest test-euclidian-distnace 
  (is (= (euclidian-distance [10 20] [5 15]) (math/sqrt 50) )))

(deftest test-distance
  (testing "euclidian distance"
    (is (= (distance euclidian-distance [1 10] [100 26] [3 40]) 128.07169466475875))))

(deftest test-max-distance 
  (is (= (max-distance [[0 1] [1 5] [2 8]] 0 2) [7.28538328578604 0])))





;examples
(comment 
  (run-tests)

  (def series-example-y [1,  2,  4,  10,  20,  1,  3 ,  1,  10,  1,  10,
			 5, 30,  5,  1,  9,  13,  1,  0,  20,  1,  2,  4,  10,  5,  2,  50,
			 0,  7,  8, 5,  4,  3,  2,  20,  1,  3 ,  1,  10,  1,  10,  5, 30,  5,
			 1,  9,  13,  1,  0,  20,  1,  2,  4,  10,  20,  1,  3 ,  1,  10,  1,
			 10,  5, 30,  5,  1,  9,  13,  1,  0,  20,  1,  2,  4,  10,  5,  2,
			 50,  0,  7,  8, 5,  4,  3,  2,  20,  1,  3 ,  1,  10,  1,  10,  5, 30,
			 5,  1,  9,  13,  1,  0, 20])
  (def series-example (vec (map (fn [x y] [x y]) (range (count
							 series-example-y)) series-example-y)))


  (def pips (find-pips series-example))

  (map (partial sort-by first) (take 15 pips))


  (find-pips 15 series-example)

  (animate-chart 20 series-example)

  )



(defn -main [& args]
  (print args))
