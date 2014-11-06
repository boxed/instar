(ns instar.core
  (:require [instar.stack :as s]))

(def ^:private STAR *)

(defn ^:private noop [x] x)


(defn ^:private split-at-exclusive [index path]
  (let [[a b] (split-at index path)]
    [(into [] a)
     (into [] (drop 1 b))]))

(defn- expand-keys [value]
  (cond
   (sequential? value) (range (count value))
   (associative? value) (keys value)))

(defn index-of [coll val]
  (first (keep-indexed #(when (= val %2) %1) coll)))

(defn expand-path-once [state path]
  (if-let [index-of-star (index-of path STAR)]
    (let [[path-base path-rest] (split-at-exclusive index-of-star path)
          value (get-in state path-base)]
      (if-let [ks (expand-keys value)]
        (for [k ks]
          (into (into path-base [k]) path-rest))
        []))
    [path]))

(defn expand-path [state path]
  (let [paths (s/stack [path])
        result (s/stack [])]
    (while (s/not-empty? paths)
      (let [path (s/pop! paths)]
        (if (nil? (some #{STAR} path))
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
