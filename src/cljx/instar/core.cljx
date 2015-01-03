(ns instar.core)

(def ^:private STAR *)

(def ^:private star? #{STAR})

(defn- noop [x] x)

(defn- name* [x]
  (if (number? x) (str x) (name x)))

(defn- split-at-exclusive [index path]
  (let [[a b] (split-at index path)]
    [(into [] a)
     (into [] (drop 1 b))]))

(defn- regex? [path-segment]
  (instance?
   #+clj  java.util.regex.Pattern
   #+cljs js/RegExp
   path-segment))

(defn- expand-keys [value]
  (cond (sequential? value) (range (count value))
        (associative? value) (keys value)))

(defn index-of [coll val]
  (first (keep-indexed #(when (= val %2) %1) coll)))

(defn expand-path-with [state base keep?]
  (let [value (get-in state base)]
    (if-let [ks (filter #(keep? %) (expand-keys value))]
      (for [k ks] (into base [k]))
      [])))

(defn expand-path [state path]
  (letfn [(expand-path-once [base crumb]
            (cond
             (star? crumb)  (expand-path-with state base (constantly true))
             (fn? crumb)    (expand-path-with state base crumb)
             (regex? crumb) (expand-path-with state base #(re-find crumb (name* %)))
             :default       [(conj base crumb)]))
          (expand-paths-once [base-paths crumb]
            (into #{} (mapcat #(expand-path-once % crumb) base-paths)))]
    (reduce expand-paths-once #{[]} path)))

(defn resolve-paths-for-transform [m args]
  (let [pairs  (partition 2 args)
        expand (fn [[p f]] (mapcat #(vector % f) (expand-path m p)))]
    (into [] (mapcat expand pairs))))

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
