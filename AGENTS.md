# Working on SCViewer

## Project layout

SCViewer is an Eclipse RCP application and a set of Eclipse plug-ins for viewing
waveforms and transaction recordings. The root Maven reactor uses Tycho; this is
not a conventional Maven `src/main/java` project. Java sources generally live in
each plug-in's `src/` directory.

- `plugins/com.minres.scviewer.database`: shared waveform, event, and transaction
  interfaces, data structures, and database services.
- `plugins/com.minres.scviewer.database.{vcd,fst,ftr,text,sqlite}`: format loaders.
- `plugins/com.minres.scviewer.database.ui.swt`: SWT waveform rendering and controls.
- `plugins/com.minres.scviewer.e4.application`: standalone e4 application, handlers,
  views, application model, and settings.
- `plugins/com.minres.scviewer.ui`: Eclipse workbench integration.
- `tests/`: database and application test bundles.
- `features/`: Eclipse feature definitions and their bundle inclusions.
- `releng/com.minres.scviewer.target`: shared PDE/Tycho target platform.
- `releng/com.minres.scviewer.updateSite`: p2 repository configuration and categories.
- `products/com.minres.scviewer.e4.product`: product definition and platform archives.
- `doc/com.minres.scviewer.doc`: AsciiDoc sources and documentation generation.
- `plugins/com.minres.scviewer.e4.application.help`: embedded help bundle, including
  generated documentation.

## Building and checking changes

Run commands from the repository root:

```sh
mvn -version
mvn verify
```

Maven itself must run on JDK 21. Check the Java version in `mvn -version`; changing
the target platform's JRE or a bundle manifest does not change Maven's JVM. For
Eclipse Maven launches, select JDK 21 in the launch configuration's JRE tab.
Maven 3.9.16 with JDK 21 is a validated combination for this repository.

The Tycho version is centralized in the root POM's `tycho-version` property.
`.mvn/jvm.config` opens `sun.nio.ch` for JRuby used by documentation generation;
preserve this option when changing Maven JVM configuration.

`mvn verify` generates documentation, compiles the bundles, runs Tycho Surefire
tests, assembles the p2 repository, and packages Linux, Windows, and macOS x86_64
products. Network access is needed for uncached Maven and p2 dependencies.
Product archives are under
`products/com.minres.scviewer.e4.product/target/products/`; the update site is under
`releng/com.minres.scviewer.updateSite/target/repository/`.

For code or build changes, run the relevant checks and use the full reactor to
validate integration and packaging. Avoid treating a standalone module build as
equivalent: Tycho also needs the reactor's target-definition artifact and bundle
dependencies. For documentation-only instruction changes, a build is unnecessary.
Report the command, result, and any remaining warnings or untested behavior.

## Tests

- Tests use JUnit 4 (`org.junit.Test`) inside `eclipse-test-plugin` modules, with
  sources under `tests/<bundle>/src/`.
- Database tests cover loaders, indexing, concurrency, and resource lifecycles;
  recording fixtures live under `tests/com.minres.scviewer.database.test/inputs/`.
- Application tests cover handlers, filtering, settings, selection, grouping,
  background jobs, and viewer lifecycles. The application test bundle is a fragment
  of the application bundle and uses a headless Tycho harness; tests that create
  SWT controls may still need a display.
- Add focused regression tests for behavior changes. Preserve deterministic event
  ordering, lazy-loading semantics, and compatibility with existing recordings and
  settings files.
- Inspect each test module's `target/surefire-reports/` on failure. Eclipse runtime
  diagnostics may also appear in `target/work/data/.metadata/.log`.

## Implementation conventions

- Follow the surrounding Java formatting and existing package structure; avoid
  unrelated reformatting. Preserve license headers.
- Respect OSGi bundle boundaries. When adding dependencies or resources, update
  `META-INF/MANIFEST.MF`, `build.properties`, `plugin.xml`, or `OSGI-INF` declarations
  as appropriate; a Java import or Maven dependency alone may not be sufficient.
- Keep database code independent of SWT/e4. Put rendering and UI behavior in the
  appropriate UI bundle. Keep implementation packages internal unless a public API
  change is intentional.
- Perform SWT widget operations on the UI thread. Keep expensive loading and
  querying off it; dispose resources, detach listeners, and cancel background work
  when their owning view or database closes.
- Preserve compatibility with the selected Eclipse platform. It currently uses
  `javax.inject`; introducing `jakarta.inject` requires a coordinated platform
  migration rather than an isolated import replacement.
- FST native sources and CMake scripts live under
  `plugins/com.minres.scviewer.database.fst/csrc/fst/`. Native builds are separate
  from the Java reactor; rebuild platform libraries only when the change requires
  it and explain which platforms were validated.

## Build and documentation maintenance

- Keep the target definition, bundle execution environments, product JRE, and
  Maven build JDK compatible. Check dependency resolution after target changes.
- The Orbit repository selection avoids an obsolete Felix SCR provider that
  conflicts with Eclipse's Declarative Services API. Preserve that compatibility
  when changing repository versions.
- Publish source bundles using `plugin-source` and the update site's
  `includeAllSources`; do not reintroduce the deprecated `feature-source` goal or
  generated source-feature references in `category.xml`.
- Keep the parent UTF-8 source encoding. Asciidoctor syntax-highlighter settings
  belong under `<attributes>` as `<source-highlighter>`.
- Edit help content in `doc/com.minres.scviewer.doc/src/asciidoc/`. The documentation
  build copies DocBook intermediates into `src/docbkx/` and generates files inside
  the help bundle; review resulting diffs rather than hand-editing generated HTML.
- Read `README.md` for launch guidance, including the embedded-help configuration
  directory restriction on spaces and percent escapes.
- Preserve existing user changes and local recording fixtures. Do not include
  unrelated untracked inputs, generated `target/` contents, or rebuilt native
  binaries in a change.
