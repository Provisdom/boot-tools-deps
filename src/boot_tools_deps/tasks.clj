(ns boot-tools-deps.tasks
  {:boot/export-tasks true}
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [boot.core :as boot]
    [boot.pod :as pod]
    [boot.task.built-in :as tasks]))

(defn deps-edn->pod-env
  "Returns the map passed to pod/make-pod. Contains :dependencies info and
  :directories."
  []
  (let [deps-edn (-> "deps.edn"
                     (slurp)
                     (edn/read-string))]
    {:directories
     (:paths deps-edn)
     :dependencies
     (reduce-kv (fn [boot-deps artifact {:keys [mvn/version]}]
                  (if version
                    (conj boot-deps [artifact version])
                    boot-deps))
                []
                (:deps deps-edn))}))

(defmacro eval-in-pod
  [& body]
  `(let [pod# (pod/make-pod (deps-edn->pod-env))
         cp# (pod/with-eval-in
               pod#
               ~@body)]
     (pod/destroy-pod pod#)
     cp#))

(defn java-classpath
  "Returns the Java classpath string."
  [opts]
  (eval-in-pod
    (require '[boot-tools-deps.core])
    (boot-tools-deps.core/java-classpath ~opts)))

(defn boot-environment
  [opts]
  (eval-in-pod
    (require '[boot-tools-deps.core])
    (boot-tools-deps.core/boot-environment ~opts)))

(boot/deftask deps
  "Use tools.deps to read and resolve the specified deps.edn files.

  The dependencies read in are added to your Boot :dependencies vector.

  With the exception of -A, -r, and -v, the arguments are intended to match
  the clj script usage (as passed to clojure.tools.deps.alpha.makecp/-main).
  Note, in particular, that -c / --config-paths is assumed to be the COMPLETE
  list of EDN files to read (and therefore overrides the default set of
  system deps, user deps, and local deps).

  The -r option is equivalent to the -Srepro option in tools.deps, which will
  exclude both the system deps and the user deps."
  [c config-paths PATH [str] "the list of deps.edn files to read"
   A aliases KW [kw] "the list of aliases (for both -C and -R)"
   R resolve-aliases KW [kw] "the list of resolve aliases to use"
   r repeatable? bool "Use only the specified deps.edn file for a repeatable build"
   m maven-only? bool "Use this flag to set to Boot env only using Maven deps."
   v verbose bool "Be verbose (and ask tools.deps to be verbose too)"]
  (let [opts {:config-paths      config-paths
              :resolve-aliases   (into (vec aliases) resolve-aliases)
              :repeatable?       repeatable?
              :verbose           verbose}]
    (if maven-only?
      (do
        (apply boot/set-env! (mapcat identity (boot-environment opts)))
        identity)
      (let [cp (java-classpath opts)
            out-file (io/file (boot/tmp-dir!) ".tools.deps-cp.txt")]
        (spit out-file cp)
        (tasks/with-cp :file (.getAbsolutePath out-file))))))