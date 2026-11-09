# Assinatura do APK (release)

O Lambda usa uma keystore própria para as builds de release. Ela **não** é versionada.

## Onde ficam os arquivos
- Keystore: `keystores/lambda-release.jks`
- Senhas/alias: `keystore.properties` (na raiz do projeto)

Ambos estão no `.gitignore` e nunca vão para o GitHub. Faça backup dos dois em local seguro.

> Se a keystore for perdida, não será possível atualizar um app já instalado com a mesma assinatura.
> Guarde uma cópia offline (pendrive/gerenciador de senhas) junto com o `keystore.properties`.

## Como a build usa isso
`app/build.gradle.kts` lê `keystore.properties` automaticamente. Se o arquivo não existir,
a build de release sai sem assinatura (não instalável) — útil apenas para conferir compilação.

## Gerar o APK assinado
```powershell
cd Lambda
.\gradlew.bat :app:assembleRelease
```
Saída: `app/build/outputs/apk/release/app-release.apk`

## Conferir a assinatura
```powershell
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\35.0.0\apksigner.bat" verify --print-certs app\build\outputs\apk\release\app-release.apk
```

## Identificadores
- `applicationId` release: `com.lambda.assistente`
- `applicationId` debug: `com.lambda.assistente.debug` (pode conviver com a versão release)
- Alias da chave: `lambda`
