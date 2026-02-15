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

# Run benchmarks (EDN output for comparison)
[group('bench')]
bench:
    clojure -M:dev:bench -m winnow.bench

# Run benchmarks (human-readable output)
[group('bench')]
bench-pretty:
    clojure -M:dev:bench -m winnow.bench --pretty

# Compare two benchmark files
[group('bench')]
bench-compare before after:
    clojure -M:dev:bench -m winnow.bench-compare {{ before }} {{ after }}

# Save baseline benchmark
[group('bench')]
bench-baseline:
    clojure -M:dev:bench -m winnow.bench > bench/baseline.edn
    @echo "Saved to bench/baseline.edn"

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

# Tag and deploy a release to Clojars
[group('release')]
release version:
    #!/usr/bin/env zsh
    set -e
    if [[ -n $(git status --porcelain) ]]; then
        echo "Error: uncommitted changes" >&2
        exit 1
    fi
    just
    git tag -s "v{{ version }}" -m "Release {{ version }}"
    git push --tags
    op run -- clojure -T:build deploy :version '"{{ version }}"'

# Build JAR locally
[group('release')]
build version:
    clojure -T:build jar :version '"{{ version }}"'

# Install to local Maven repo
[group('release')]
install version:
    clojure -T:build install :version '"{{ version }}"'
