; (ns vfprocess.db.traversal
;   (:require [next.jdbc :as jdbc]
;             [next.jdbc.optional :as opt]
;             [com.walmartlabs.lacinia.schema :refer [tag-with-type]]
;             [vfprocess.db.queries :refer [db query ]]))

; (defn visited?
;   "Predicate which returns true if the node v has been visited already, false otherwise."
;   [v coll]
;   (some #(= % v) coll))


; (defn first-neighbors
;   [v]
;   (let [node (query "EconomicResource" v)]
;     (tag-with-type (-> node
;                        (merge {:type (str "outputOf_" (:id node))})) :EconomicEvent)
;     ))

; (defn find-id
;   [type]
;   (.substring type (+ (.indexOf type "_") 1)))


; (defn find-neighbors
;   "Returns the sequence of neighbors for the given node"
;   [v coll]
;   (get coll v))

; (defn find-tracks
;   "Returns the sequence of neighbors for the given node"
;   [v]
;   (let [id (find-id (:type v))]
;     (cond
;       ; If the node is a process, retrieve the input economic event
;       (.contains (:type v) "process")
;       (let [node (jdbc/execute-one! db
;                                 ["select * from EconomicEvent where inputOf = ? " id]
;                                 {:builder-fn opt/as-unqualified-maps})]
;         (if (= nil node)
;           nil
;           (tag-with-type (-> node
;                              (merge {:type (str "inputOf_" (:id node))})) :EconomicEvent)))

;       ; If the node is an economic event input of a process, retrieve the incoming resource
;       (.contains (:type v) "inputOf")
;       (let [node (query "EconomicResource" id)]
;         (if (= nil node)
;           nil
;           (tag-with-type (-> node
;                              (merge {:type (str "economicResource_" (:id node))})) :EconomicResource)))

;       ; If the node is an economic event output of a process, retrieve the incoming process
;       (.contains (:type v) "outputOf")
;       (let [node (query :Process (:outputOf v))]
;         (if (= nil node)
;           nil
;           (tag-with-type (-> node
;                              (merge {:type (str "process_" (:id node))})) :Process)))

;       ; If the node is a resource, retrieve the incoming event (outputOf)
;       (.contains (:type v) "economicResource")
;       (let [node (jdbc/execute-one! db
;                                 ["select * from EconomicEvent where resourceInventoriedAs = ?" id]
;                                 {:builder-fn opt/as-unqualified-maps})]
;         (if (= nil node)
;           nil
;           (tag-with-type (-> node
;                              (merge {:type (str "outputOf_" (:id node))})) :EconomicEvent)))
;       :else nil)))

; (defn incoming-vf-dfs
;   "Traverses a graph in Depth First Search (DFS)"
;   [v]
  
;   (loop [stack   (vector v) ;; Use a stack to store nodes we need to explore
;          visited []]        ;; A vector to store the sequence of visited nodes
;     (if (empty? stack)      ;; Base case - return visited nodes if the stack is empty
;       visited
;       (let [v           (peek stack)
;             neighbors   (find-tracks v)
;             new-stack   (if (= nil neighbors) [] (vector neighbors))]
;         (if (= nil neighbors)
;           (recur new-stack visited)
;           (recur new-stack (conj visited neighbors)))))))


; ; (defn graph-dfs
; ;   "Traverses a graph in Depth First Search (DFS)"
; ;   [v]
; ;   (loop [stack   (vector v) ;; Use a stack to store nodes we need to explore
; ;          visited []]        ;; A vector to store the sequence of visited nodes
; ;     (if (empty? stack)      ;; Base case - return visited nodes if the stack is empty
; ;       visited
; ;       (let [v           (peek stack)
; ;             neighbors   (find-tracks v)
; ;             not-visited (filter (complement #(visited? % visited)) neighbors)
; ;             new-stack   (into (pop stack) not-visited)]
; ;         (if (visited? v visited)
; ;           (recur new-stack visited)
; ;           (recur new-stack (conj visited v)))))))
