(ns instar.core)


(defn ^:private split-at-exclusive [index path]
  (let [[a b] (split-at index path)]
    [(into [] a)
     (into [] (drop 1 b))]))

(defn expand-path-once [state path]
  (let [index-of-star (.indexOf path :*)]
    (if (= index-of-star -1)
      [path]
      (let [[path-base path-rest] (split-at-exclusive index-of-star path)]
        (if (map? (get-in state path-base))
          (let [ks (keys (get-in state path-base))]
            (for [k ks]
              (into (into path-base [k]) path-rest)))
          [])))))

(defn expand-path [state path]
  (let [paths (java.util.ArrayList. [path])
        tmp (java.util.ArrayList. [])
        result (java.util.ArrayList. [])]
    (while (not (.isEmpty paths))
      (let [path (.remove ^java.util.ArrayList paths (int (- (.size paths) 1)))]
        (if (= (.indexOf path :*) -1)
          (.add result path)
          (.addAll paths (expand-path-once state path)))))
    (into #{} result)))

(defn resolve-paths-for-transform [m args]
  (let [pairs (partition 2 args)
        result (java.util.ArrayList. [])]
    (doseq [[p f] pairs]
      (doseq [p (expand-path m p)]
        (.addAll result [p f])))
    (into [] result)))

(defn transform-resolved-paths [m args]
  (let [pairs (partition 2 args)]
    (reduce
     (fn [state [path f]]
       (update-in state path f))
     m
     pairs
     )))

(defn transform [m & args]
  (let [x (resolve-paths-for-transform m args)]
    (transform-resolved-paths m x)))
