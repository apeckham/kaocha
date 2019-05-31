(ns kaocha.type.clojure.spec.test.check
  (:require [clojure.spec.alpha :as s]
            [kaocha.hierarchy :as hierarchy]
            [kaocha.load :as load]
            [kaocha.specs]
            [kaocha.testable :as testable]
            [kaocha.type :as type]
            [kaocha.type.clojure.spec.test.fdef :as type.fdef]
            [kaocha.type.clojure.spec.test.ns :as type.spec.ns]
            [kaocha.test-suite :as test-suite]))

(alias 'stc 'clojure.spec.test.check)

(defn check-tests [{::keys [syms] :as check}]
  (let [check (update check :kaocha/ns-patterns #(or % [".*"]))]
    (condp = syms
      :all-fdefs   (load/namespace-testables :kaocha/source-paths check
                                             type.spec.ns/->testable)
      :other-fdefs nil ;; TODO: this requires orchestration from the plugin
      :else        (type.fdef/load-testables syms))))

(defn checks [{::keys [checks] :as testable}]
  (let [checks (or checks [{}])]
    (map #(merge testable %) checks)))

(defmethod testable/-load :kaocha.type/clojure.spec.test.check [testable]
  (->> (checks testable)
       (map check-tests)
       (apply concat)
       (assoc testable :kaocha/tests)
       (testable/add-desc "clojure.spec.test.check")))

(defmethod testable/-run :kaocha.type/clojure.spec.test.check [testable test-plan]
  (test-suite/run testable test-plan))

(s/def ::syms (s/or :given-symbols (s/coll-of symbol?)
                    :catch-all #{:all-fdefs :other-fdefs}))
(s/def ::check (s/keys :opt [::syms ::stc/opts :kaocha/ns-patterns]))
(s/def ::checks (s/coll-of ::check))

(s/def :kaocha.type/clojure.spec.test.check
  (s/merge (s/keys :req [:kaocha.testable/type
                         :kaocha.testable/id
                         :kaocha/source-paths]
                   :opt [:kaocha.filter/skip-meta
                         :kaocha/ns-patterns
                         ::checks])
           ::check))

(hierarchy/derive! :kaocha.type/clojure.spec.test.check
                   :kaocha.testable.type/suite)
