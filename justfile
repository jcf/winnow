_default:
    @just lint
    @just test
    @just hygiene
    @just test-bb
    @just test-cljs

# Format project files
[group('dev')]
fmt:
    treefmt

# Run lints
[group('dev')]
lint:
    clj-kondo --lint src test

# Run tests
[group('test')]
test *args:
    clojure -M:dev:test:runner {{ args }}

# Run conformance test suite
[group('test')]
conform:
    cd conformance && pnpm test

# Run benchmarks
[group('test')]
bench:
    clojure -M:dev:bench -m winnow.bench

# Compare tailwind-merge-clj conformance
[group('test')]
compare:
    clojure -M:compare -m winnow.compare

# Run Babashka tests
[group('test')]
test-bb:
    bb -m cognitect.test-runner/-main -d test -n winnow.api-test -n winnow.parse-test

# Run ClojureScript tests
[group('test')]
test-cljs:
    clojure -M:cljs -m cljs.main -d target/cljs -o target/cljs/test.js -co '{:target :nodejs}' -c winnow.test-runner
    node target/cljs/test.js

# Check for reflection and boxing warnings
[group('test')]
hygiene:
    clojure -M:dev -m winnow.hygiene

# Generate documentation
[group('docs')]
docs:
    clojure -M:dev -m winnow.doc > doc/supported-classes.org
