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

(defn- index-of [coll val]
  (first (keep-indexed #(when (= val %2) %1) coll)))

(defn- append-vec [base key value]
  (update-in base [key] #(conj (vec %) value)))

(defn- expand-path-with [state base keep?]
  (let [value (get-in state (:path base))]
    (if-let [ks (filter #(keep? %) (expand-keys value))]
      (for [k ks] (append-vec base :path k)))))

(defn expand-path
  "Expand all valid paths corresponding to path expression, with corresponding captures"
  [state path & [init]]
  (letfn [(expand-path-once [base crumb]
            (cond
             (star? crumb)  (expand-path-with state base (constantly true))
             (fn? crumb)    (expand-path-with state base crumb)
             (regex? crumb) (expand-path-with state base #(re-find crumb (name* %)))
             :default       [(append-vec base :path crumb)]))
          (expand-capture-path [{:keys [captures] :as base} [type & path]]
            ;; wildcards do not make sense for non-resolving capture
            (if (= CAPTURE_WITHOUT_RESOLUTION type)
              (assert (not-any? #(or (star? %) (fn? %) (regex? %)) path)))
            (let [next-paths (reduce expand-paths-once [base] path)
                  values     (map #(if % (get-in state (:path %))) next-paths)]
              (into #{}
                    (if (= CAPTURE_WITH_RESOLUTION type)
                      (remove nil?
                              (map (fn [next-path value]
                                     (if next-path
                                       (append-vec next-path :captures value)))
                                   next-paths values))
                      ;; leverage values has length at most 1 without wildcards
                      [(append-vec base :captures (first values))]))))
          (expand-paths-once [base-paths crumb]
            (let [expand* (if (capture? crumb) expand-capture-path expand-path-once)]
              (distinct (mapcat #(expand* % crumb) base-paths))))]
    (reduce expand-paths-once [init] path)))

(defn resolve-paths-for-transform [m args]
  (let [pairs  (partition 2 args)
        expand (fn [[p f]] (expand-path m p {:f f}))]
    (mapcat expand pairs)))

(defn transform-resolved-paths [m path-transforms]
  (reduce
   (fn [state {:keys [path f captures]}]
     (if (fn? f)
       (if (= f dissoc) ; This is a special case for usability purposes
         (if (= (count path) 1) ; update-in doesn't support empty path: http://dev.clojure.org/jira/browse/CLJ-373
           (dissoc state (last path))
           (update-in state (drop-last 1 path) #(dissoc % (last path))))
         (if (seq captures)
           (assoc-in state path (apply f captures))
           (update-in state path f)))
       (assoc-in state path f)))
   m
   path-transforms))


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
  (for [path (map :path (mapcat (partial expand-path m) args))]
    [path (get-in m path)]))

(defn get-values-in-paths [& args]
  (map second (apply get-in-paths args)))
