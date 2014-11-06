(ns instar.core
  (:require [instar.stack :as s]))

(def ^:private STAR *)

(def ^:private star? #{STAR})

(defn- noop [x] x)

(defn- split-at-exclusive [index path]
  (let [[a b] (split-at index path)]
    [(into [] a)
     (into [] (drop 1 b))]))

(defn- regex? [path-segment]
  (instance?
   #+clj  java.util.regex.Pattern
   #+cljs js/RegExp
   path-segment))

(defn- or*
  "Takes any number of functions and return short-circuiting function
   that returns the `or` of their values for a given input"
  [& preds]
  (fn [x] (reduce #(or %1 (%2 x)) false preds)))

(defn- expand-keys [value]
  (cond (sequential? value) (range (count value))
        (associative? value) (keys value)))

(defn index-of [coll val]
  (first (keep-indexed #(when (= val %2) %1) coll)))

(defn expand-path-with [state path match? keep?]
  (let [crumb (first (filter match? path))
        index (index-of path crumb)
        [a z] (split-at-exclusive index path)
        value (get-in state a)]
    (if-let [ks (filter #(keep? crumb %) (expand-keys value))]
      (for [k ks] (into (into a [k]) z))
      [])))

(defn expand-star-path [state path]
  (expand-path-with state path star? (constantly true)))

(defn expand-fn-path [state path]
  (expand-path-with state path fn? #(%1 %2)))

(defn expand-regex-path [state path]
  (expand-path-with state path regex? #(re-find %1 (name %2))))

(defn expand-path-once [state path]
  (cond
   (some star? path)  (expand-star-path state path)
   (some fn? path)    (expand-fn-path state path)
   (some regex? path) (expand-regex-path state path)))

(defn expand-path [state path]
  (let [paths (s/stack [path])
        result (s/stack [])]
    (while (s/not-empty? paths)
      (let [path (s/pop! paths)]
        (if-not (some (or* star? fn? regex?) path)
          (s/push! result path)
          (s/push-all! paths (expand-path-once state path)))))
    (s/as-set result)))

(defn resolve-paths-for-transform [m args]
  (let [pairs (partition 2 args)
        result (s/stack [])]
    (doseq [[p f] pairs]
      (doseq [p (expand-path m p)]
        (s/push-all! result [p f])))
    (s/as-vector result)))

(defn transform-resolved-paths [m args]
  (let [pairs (partition 2 args)]
    (reduce
     (fn [state [path f]]
       (if (fn? f)
         (if (= f dissoc) ; This is a special case for usability purposes
           (if (= (count path) 1) ; update-in doesn't support empty path: http://dev.clojure.org/jira/browse/CLJ-373
             (dissoc state (last path))
             (update-in state (drop-last 1 path) #(dissoc % (last path))))
           (update-in state path f))
         (assoc-in state path f)))
     m
     pairs)))

(defn transform [m & args]
  (let [x (resolve-paths-for-transform m args)]
    (transform-resolved-paths m x)))

(defn get-in-paths [m & args]
  (let [args-with-bogus-op (apply concat (for [arg args] [arg noop])) ; ..because resolve-paths-for-transform expects pairs
        paths (resolve-paths-for-transform m args-with-bogus-op)]
    (for [[p _] (partition 2 paths)]
      [p (get-in m p)])))

(defn get-values-in-paths [& args]
  (for [[path value] (apply get-in-paths args)]
    value))
