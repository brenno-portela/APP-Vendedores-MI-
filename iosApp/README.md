# iOS App

Este e o shell iOS inicial do projeto Vendedores Minum.

## Como rodar em um Mac

1. Abra `iosApp/iosApp.xcodeproj` no Xcode.
2. Selecione o scheme `iosApp`.
3. Selecione um simulador ou um iPhone fisico.
4. Ajuste `Signing & Capabilities` com o seu Apple Team.
5. Rode o app.

O Xcode executa uma Build Phase que chama:

```bash
../gradlew :shared:assembleSharedDebugXCFramework
```

O framework gerado fica em:

```text
shared/build/XCFrameworks/debug/Shared.xcframework
```

Se preferir validar manualmente antes de abrir o Xcode:

```bash
./gradlew :shared:assembleSharedDebugXCFramework
```
