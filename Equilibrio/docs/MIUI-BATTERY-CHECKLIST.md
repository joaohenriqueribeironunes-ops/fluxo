# Checklist MIUI/HyperOS — Redmi Note 11S

O Android permite que o app colete em segundo plano, mas o MIUI/HyperOS costuma encerrar serviços para economizar bateria. Faça os ajustes abaixo **depois de instalar** e sempre que o app parar de registrar janelas.

> A coleta usa um serviço em primeiro plano com notificação permanente. Isso é obrigatório para acessar o microfone e os sensores em segundo plano. Não é possível coletar com o app totalmente fechado sem essa notificação.

## 1. Permissões do app

Abra **Configurações → Apps → Gerenciar apps → Equilíbrio**.

- **Notificações**: permitir. Sem isso, o serviço não consegue mostrar a notificação obrigatória.
- **Microfone**: “Permitir somente durante o uso do app”.
- **Sensores / atividade física**: permitir, se aparecer.
- **Calendário**: opcional; só marque se quiser correlacionar eventos.
- **Pausar atividade do app se não for usado**: **desativar**.
- **Economia de bateria / atividade em segundo plano**: **sem restrições**.

## 2. Autostart

**Configurações → Apps → Gerenciar apps → Equilíbrio → Autostart** (ou **Iniciar automaticamente**): **ativar**.

Em algumas versões: **Segurança → Permissões → Iniciar automaticamente → Equilíbrio**.

## 3. Economia de bateria

**Configurações → Bateria e desempenho → Economia de bateria de apps → Equilíbrio → Sem restrições**.

Se existir **“Otimização de bateria”**:

1. Abra **Configurações → Bateria e desempenho → Otimização de bateria**.
2. Toque em **Apps** (ou no menu ⋮ → **Otimização de bateria**).
3. Encontre **Equilíbrio** e escolha **Não otimizar**.

O app também oferece um atalho para solicitar a isenção de otimização no onboarding/configurações. Aceite.

## 4. Bloquear o app na tela de recentes

1. Abra o Equilíbrio.
2. Abra o seletor de apps recentes.
3. Puxe o cartão do Equilíbrio para baixo (ou toque no cadeado) para **fixar/bloquear**.

Isso reduz a chance de o MIUI matar o processo ao limpar a memória.

## 5. Desativar economia extrema

- **Configurações → Bateria e desempenho → Economia de bateria**: desative a economia extrema enquanto estiver coletando.
- **Configurações → Bateria e desempenho → Modo de economia de bateria adaptável**: desative se o app parar após algumas horas.
- Não use **Ultra economia de bateria** durante a coleta; ela encerra serviços em segundo plano.

## 6. Tela de bloqueio e “segundo plano”

- Mantenha a notificação **“Coleta em segundo plano”** visível. Se ela desaparecer, o serviço foi encerrado.
- A tela pode apagar; o serviço deve continuar. Se parar ao apagar a tela, verifique novamente as etapas 2–4.
- Se você reiniciar o celular, o app **não** reinicia a coleta automaticamente (o Android exige o app em primeiro plano para microfone). Abra o app e reative a coleta.

## 7. Teste rápido (10–15 min)

1. Abra o app e ative a coleta.
2. Confirme a notificação permanente.
3. Apague a tela e deixe o celular parado por 10–15 minutos.
4. Abra o app → **Linha do tempo**. Devem aparecer janelas novas.
5. Verifique a bateria: com a coleta ativa, o consumo deve ser modesto (o ciclo dorme 15 s a cada 30 s).
6. Se não houver janelas:
   - confira a permissão de microfone;
   - confira “Sem restrições” na bateria;
   - confira Autostart;
   - confira se a notificação ainda está ativa;
   - desative a economia extrema e tente novamente.

## 8. Dicas de uso

- A coleta pausa sozinha com bateria abaixo de 15% (configurável). Se quiser coletar por mais tempo, reduza a frequência ou mantenha o celular carregando.
- O microfone é processado em memória e descartado; nenhum áudio é salvo. O indicador de microfone no app mostra quando ele está ativo.
- Se você usa outro app que grava áudio, o Android pode negar o microfone ao Equilíbrio. Nesse caso, a janela é salva sem features de som (o app não inventa dados).
- Após atualizar o APK, as permissões e o autostart costumam ser mantidos; confira a notificação na primeira abertura.

## 9. Se o app for encerrado pelo sistema

Sintomas: notificação some, Timeline para de receber janelas, nenhum alerta novo.

1. Abra o app e reative a coleta.
2. Repita as etapas 2–5.
3. Em último caso, remova o app da lista de “apps em segundo plano restritos” do MIUI.
4. Mantenha o app atualizado; versões novas podem ajustar o serviço.

> O comportamento varia entre versões do MIUI/HyperOS. Os nomes podem aparecer em português ou inglês, mas a lógica é sempre: **autostart ativo, sem restrição de bateria, atividade em segundo plano permitida e app fixado nos recentes**.
