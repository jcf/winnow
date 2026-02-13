{pkgs, ...}: {
  claude.code.enable = true;

  packages = with pkgs; [
    # Clojure
    babashka
    clj-kondo
    cljfmt
    clojure
    clojure-lsp

    # Development
    just

    # Formatters
    alejandra
    prettier
    shfmt
    treefmt
  ];

  languages.javascript.enable = true;
  languages.javascript.pnpm.enable = true;
  languages.javascript.pnpm.install.enable = true;
}
