(ns instar.stack)

(defn pop! [stack]
  (.pop stack))

(defn push! [stack value]
  (.push stack value))

(defn push-all! [stack value-list]
  (doseq [x value-list]
    (.push stack x)))

(defn index-of [stack sym]
  (.indexOf stack sym))

(defn stack [initial]
  (clj->js initial))

(defn empty? [stack]
  (= (.-length stack) 0))

(defn not-empty? [stack]
  (not (empty? stack)))

(defn as-set [stack]
  (into #{} stack))

(defn as-vector [stack]
  (into [] stack))
