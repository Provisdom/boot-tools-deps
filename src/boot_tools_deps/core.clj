(ns boot-tools-deps.core
  "Set up dependencies from deps.edn files using tools.deps."
  (:require
    [clojure.java.io :as io]
    [clojure.tools.deps.alpha.extensions.maven]
    [clojure.tools.deps.alpha.extensions.git]
    [clojure.tools.deps.alpha.extensions.local]
    [clojure.tools.deps.alpha.extensions.deps]
    [clojure.tools.deps.alpha :as tools.deps]
    [clojure.tools.deps.alpha.reader :as reader]))

(defn read-dep-files
  "Returns a single deps map."
  [config-paths repeatable?]
  (let [home-dir (System/getProperty "user.home")
        _ (assert home-dir "Unable to determine your home directory!")
        deps-files (if (seq config-paths)
                     config-paths
                     (cond->> ["deps.edn"]
                              (not repeatable?)
                              (into [(str home-dir "/.clojure/deps.edn")])))]
    (reader/read-deps (into []
                            (comp (map io/file)
                                  (filter #(.exists %)))
                            deps-files))))

(defn resolve-deps
  [deps aliases]
  (tools.deps/resolve-deps deps (tools.deps/combine-aliases deps aliases)))

(defn java-classpath
  "Returns a valid Java classpath string."
  [{:keys [config-paths resolve-aliases repeatable? verbose]}]
  (let [deps (read-dep-files config-paths repeatable?)
        lib-map (resolve-deps deps resolve-aliases)]
    (tools.deps/make-classpath lib-map (:paths deps) {})))

(defn boot-environment
  "Returns a map that can be merged into the Boot environment."
  [{:keys [config-paths resolve-aliases repeatable? verbose]}]
  (let [deps (read-dep-files config-paths repeatable?)
        lib-map (resolve-deps deps resolve-aliases)]
    {:dependencies
     (reduce-kv (fn [boot-deps artifact {:keys [mvn/version dependents]}]
                  ;; only include dependencies directly written in deps.edn
                  (if (and version (nil? dependents))
                    (conj boot-deps [artifact version])
                    boot-deps))
                [] lib-map)}))