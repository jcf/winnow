{pkgs, ...}: {
  claude.code.enable = true;

  env = {
    CLOJARS_USERNAME = "jcf";
    CLOJARS_PASSWORD = "op://Employee/Clojars/deploy-token-jcf.dev";
  };

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
