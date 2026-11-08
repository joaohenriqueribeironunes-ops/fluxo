# Privacidade do Equilíbrio

O Equilíbrio foi feito para guardar dados sensíveis no próprio aparelho. Esta página descreve, em linguagem simples, o que é coletado, onde fica e o que pode sair do celular.

> **Resumo:** os dados ficam no seu celular, em um banco criptografado. Áudio nunca é salvo. Nada é enviado para a internet por padrão. A análise com Gemini é opcional, exige consentimento e recebe apenas resumos agregados — sem áudio, sem amostras brutas de sensores e sem texto livre de check-in.

## 1. Quais dados o app coleta

Com a coleta ativa:

- **Sensores**: acelerômetro, giroscópio, luz e proximidade. O app calcula agitação, orientação, luminosidade e proximidade. As amostras brutas **não** são gravadas; apenas agregados por janela.
- **Microfone**: captura processada em memória a 16 kHz mono. O app calcula nível em dB, bandas de frequência, impulsividade e índice de sobrecarga sonora. O áudio bruto **nunca** é gravado, transmitido ou reproduzido.
- **Check-ins**: emoções, intensidade, sono, fome, dor, tensão, sensibilidade sensorial, cafeína, refeições, medicação, atividades, transições, pensamentos e nota. Esses campos ficam no banco local.
- **Crises**: tipo, intensidade, duração, gatilhos, o que ajudou, tempo de recuperação e se foi retroativa.
- **Previsões e métricas**: risco calculado, previsões, acertos, falsos alarmes e feedback.
- **Calendário** (opcional): títulos e horários de eventos, se você autorizar. Usado para correlacionar rotina e crises.
- **Configurações**: preferências de coleta, acessibilidade, alertas, Gemini etc.

## 2. Onde os dados ficam

- No armazenamento privado do app, em um banco **Room + SQLCipher** criptografado.
- A chave do banco é aleatória, guardada no **Android Keystore** e não sai do aparelho.
- O backup do Android está **desativado** (`allowBackup=false`), e as regras de extração excluem banco, arquivos e preferências.
- Relatórios PDF são gerados em `filesDir/reports` e só saem do app quando **você** compartilha.
- O app não tem conta, login, anúncios, analytics ou rastreamento.

## 3. Microfone

O microfone só é usado quando:

1. você ativou a coleta;
2. o serviço em primeiro plano está rodando com a notificação visível;
3. a permissão de microfone foi concedida.

O processamento é local e em memória. Ao final da janela, o buffer de áudio é descartado. O banco guarda apenas números agregados. O app mostra um indicador de microfone quando ele está ativo.

## 4. Gemini (opcional)

A análise com Gemini é **desligada por consentimento**. Ela só acontece se:

- `Gemini` estiver ativado;
- você tiver dado consentimento explícito;
- houver rede e um proxy configurado.

### O que é enviado

Um JSON com:

- período analisado, quantidade de dias e de crises;
- agregados por janela (agitação, sobrecarga sonora, dB médio/máximo, bandas, picos impulsivos, luz, proximidade e risco local);
- check-ins **sem pensamentos e sem nota livre** (emoções, intensidade, sono, fome, dor, tensão, sensibilidade, cafeína, refeições, medicação, atividades e transições);
- crises (tipo, intensidade, duração, gatilhos e o que ajudou — labels escolhidos por você);
- resumos diários e previsões locais;
- métricas de precisão;
- títulos/horários de eventos de calendário, **somente se você autorizou o calendário**;
- padrões recorrentes calculados localmente.

### O que NÃO é enviado

- áudio bruto ou qualquer gravação;
- amostras individuais de acelerômetro/giroscópio;
- formas de onda;
- seus pensamentos ou notas livres de check-in;
- localização, contatos, identificadores de publicidade ou dados de conta.

### Para onde vai

O app envia para o **proxy** configurado (por padrão, um Cloudflare Worker seu). O Worker guarda a chave da API Gemini no servidor e repassa o payload ao Google. Sem proxy configurado, a análise fica indisponível. O app não contém chave de API.

O Google pode reter dados conforme os termos da API Gemini. Se isso for um problema para você, mantenha o Gemini desativado: todo o resto funciona offline.

## 5. Permissões

| Permissão | Para quê | Obrigatória? |
|---|---|---|
| `RECORD_AUDIO` | Features de som agregadas | Não (pode desativar o microfone) |
| `POST_NOTIFICATIONS` | Notificação do serviço e lembretes | Sim para coleta em segundo plano |
| `FOREGROUND_SERVICE` + `MICROPHONE` + `HEALTH` | Serviço de coleta | Sim para coleta |
| `HIGH_SAMPLING_RATE_SENSORS` | Sensores a ~25 Hz | Sim para coleta |
| `RECEIVE_BOOT_COMPLETED` | Avisar para reabrir o app após reiniciar | Opcional |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Atalho para o MIUI não matar o serviço | Opcional |
| `READ_CALENDAR` | Correlacionar eventos | Não |

## 6. Compartilhamento e exportação

- **PDF**: gerado localmente; você escolhe para onde compartilhar pelo seletor do Android.
- **Backup**: desativado pelo app. Se você usar um backup de terceiros, ele pode copiar arquivos fora do controle do Equilíbrio.
- **Gemini**: só com consentimento, conforme a seção 4.

## 7. Retenção e exclusão

- Os dados ficam no aparelho até você apagar.
- Em **Configurações → Dados e privacidade**, use **Apagar todos os dados** para remover banco, relatórios, exports e preferências.
- Desinstalar o app também remove os dados privados (o backup está desativado).
- Se você usou o Gemini, o histórico do lado do Google segue a política do Google; o app não tem como apagar isso.

## 8. Segurança

- Banco criptografado com SQLCipher.
- Chave no Keystore, protegida pelo sistema.
- `FLAG_SECURE` opcional para esconder o conteúdo na miniatura do multitarefa.
- Sem servidor próprio de contas; o proxy do Gemini só recebe o payload agregado e não mantém histórico no app.
- O APK de release é assinado; verifique a origem antes de instalar.

## 9. Crianças e vulnerabilidade

O app é pessoal e não se destina a crianças. Ele não faz diagnóstico, não substitui acompanhamento profissional e não deve ser a única fonte de apoio em crise. Em risco à segurança, procure ajuda imediata: **CVV 188** ou **SAMU 192**.

## 10. Mudanças

Se o app passar a enviar novos dados, esta página e o consentimento serão atualizados antes. A versão do app e a data desta política aparecem no repositório.

**Última atualização:** novembro de 2026.
