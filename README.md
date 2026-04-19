# LOGO LSP Server

## Build

From the project root:

```powershell
.\gradlew.bat installDist
```

This creates the runnable server in:

```text
build/install/LSP-project/
```

## Run

Start the server with:

```powershell
.\build\install\LSP-project\bin\LSP-project.bat
```

The server uses `stdio`, so it should be launched by an LSP client.

## LSP Client Setup

Configure your LSP client to start:

```text
build/install/LSP-project/bin/LSP-project.bat
```

and associate it with `.logo` files.
