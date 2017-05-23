# Why wah?

Well, WebAssembly text isn't _designed_ to be written from scratch, but it's
still writable, and really isn't that bad. wah makes WebAssembly text just
a little more humane, so that people with a need for extreme simplicity and
low-level access can get it.

WebAssembly will definitely, primarily, be a compilation target for things
like emscripten. But why not have some low-level mathematical code written in,
and purely written in, WebAssembly?

# Why is the compiler implemented in Clojure?

Clojure _looks_ a lot like WebAssembly text format because they both use S-Expressions,
but, obviously, WebAssembly text format is not a lisp and has very little else
in common. The compiler is implemented in Clojure because of Clojure's excellent
support for the [edn format](https://github.com/edn-format/edn), which fortunately
is a superset of WebAssembly text syntax. Clojure is also a very nice, functional
language with efficient and direct ways of expressing the kind of data-transformation
logic required for wah.

# Could it be implemented in ClojureScript?

Sure! It might even already work in ClojureScript, I just haven't tried yet. PRs welcome!
