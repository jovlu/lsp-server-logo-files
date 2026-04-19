# LOGO LSP Server

This setup is for IntelliJ IDEA using the `LSP4IJ` plugin.
But it may be used in other IDEs.

## IntelliJ Setup

1. Install the `LSP4IJ` plugin in IntelliJ IDEA.
2. Build the server from the project root:

```powershell
.\gradlew.bat installDist
```

This creates the runnable launcher in `build/install/LSP-project/bin/`.

3. In IntelliJ IDEA, go through Settings -> Languages & Frameworks -> Language Servers

```text
cmd /c "cd /d <project-root> && build\install\LSP-project\bin\LSP-project.bat"
```

4. Associate it with *.logo files.
