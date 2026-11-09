# MIUI / HyperOS (Redmi Note 11S) — bateria, autostart e voz

Para o assistente por voz/serviço em primeiro plano funcionar bem:

1. **Economia de bateria do app**: Configurações → Apps → Lambda → Economia de bateria → **Sem restrições**.
2. **Início automático (Autostart)**: Segurança/Apps → Permissões → Início automático → ativar para Lambda.
3. **Bloquear nos recentes** (opcional, ajuda a manter o serviço): abrir recentes → pressionar o card do app → ícone de cadeado.
4. **Notificações**: permitir notificações (necessário para foreground service).
5. **Microfone**: permitir "Durante o uso do app" (e seguir prompts para uso em segundo plano quando aplicável).

Se o sistema encerrar o serviço, reabra o app e reative o assistente. O app funciona 100% offline; voz/STT depende das permissões concedidas.