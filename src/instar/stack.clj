(ns instar.stack)

(defn pop! [stack]
  (.remove ^java.util.ArrayList stack (int (- (.size stack) 1))))

(defn push! [stack value]
  (.add stack value))

(defn push-all! [stack value-list]
  (.addAll stack value-list))

(defn index-of [stack sym]
  (.indexOf stack sym))

(defn stack [initial]
  (java.util.ArrayList. initial))

(defn empty? [stack]
  (.isEmpty stack))

(defn not-empty? [stack]
  (not (empty? stack)))

(defn as-set [stack]
  (into #{} stack))

(defn as-vector [stack]
  (into [] stack))
