(def project 'provisdom/boot-tools-deps)
(def version "0.1.0")

(set-env! :resource-paths #{"src"})

(task-options!
 pom {:project     project
      :version     version
      :description "A Boot task that reads deps.edn file using tools.deps."
      :url         "https://github.com/Provisdom/boot-tools-deps"
      :scm         {:url "https://github.com/Provisdom/boot-tools-deps"}
      :license     {"Eclipse Public License"
                    "http://www.eclipse.org/legal/epl-v10.html"}})


(require
  '[boot-tools-deps.tasks :refer [deps]])