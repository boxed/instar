(ns instar.core)

(def ^:private STAR *)

(def ^:private star? #{STAR})

(def ^:private CAPTURE_WITH_RESOLUTION (quote %>))

(def ^:private CAPTURE_WITHOUT_RESOLUTION (quote %%))

(def ^:private capture-symbol? #{CAPTURE_WITH_RESOLUTION CAPTURE_WITHOUT_RESOLUTION})

(defn- capture?
  "Determine if crumb is a capture form"
  [crumb]
  (and (vector? crumb)
       (capture-symbol? (first crumb))))

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

(defn expand-path-with-capture
  "WIP"
  [state path]
  (letfn [(expand-path-once [base crumb]
            (let [base (vec base)]
              (cond
               (star? crumb)  (expand-path-with state base (constantly true))
               (fn? crumb)    (expand-path-with state base crumb)
               (regex? crumb) (expand-path-with state base #(re-find crumb (name* %)))
               :default       [(conj base crumb)])))
          (expand-capture-path [[base captures] [type & path]]
            (let [captures (vec captures)]
              ;; wildcards do not make sense for non-resolving capture
              (if (= CAPTURE_WITHOUT_RESOLUTION type)
                (assert (not-any? #(or (star? %) (fn? %) (regex? %)) path)))
              (let [next-paths (reduce expand-paths-once #{[base captures]} path)
                    values     (map #(if % (get-in state (first %))) next-paths)]
                (into #{}
                      (if (= CAPTURE_WITH_RESOLUTION type)
                        (remove nil?
                                (map (fn [next-path value]
                                       (if next-path
                                         [(first next-path) (conj captures value)]))
                                     next-paths values))
                        ;; leverage values has length at most 1 without wildcards
                        [[base (conj captures (first values))]])))))
          (expand-paths-once [base-paths-with-captures crumb]
            (if (capture? crumb)
              (into #{} (mapcat #(expand-capture-path % crumb) base-paths-with-captures))
              (into #{} (mapcat #(map vector
                                      (expand-path-once (first %) crumb)
                                      (repeat (second %)))
                                base-paths-with-captures))))]
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


;;; Public API

(defn %>
  "Syntax sugar for marking crumb for capture, with resolution"
  [crumb]
  [CAPTURE_WITH_RESOLUTION crumb])

(defn %%
  "Syntax sugar for marking crumb(s) for capture, without resolution"
  [& crumbs]
  (into [CAPTURE_WITHOUT_RESOLUTION] crumbs))

(defn transform [m & args]
  (let [x (resolve-paths-for-transform m args)]
    (transform-resolved-paths m x)))

(defn get-in-paths [m & args]
  (for [path (mapcat (partial expand-path m) args)]
    [path (get-in m path)]))

(defn get-values-in-paths [& args]
  (map second (apply get-in-paths args)))
