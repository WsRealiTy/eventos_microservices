#!/bin/bash

echo "🛑 --- FASE 1: LIMPEZA NUCLEAR --- 🛑"
# Tenta parar do jeito bonito
sudo docker-compose down --remove-orphans

# Força bruta para matar processos travados (Resolve o 'Permission Denied')
echo "🔪 Matando processos zumbis..."
sudo service apparmor stop 2>/dev/null
sudo killall -9 containerd-shim-runc-v2 2>/dev/null
sudo systemctl restart docker

# Limpa containers antigos que sobraram
sudo docker rm -f $(sudo docker ps -aq) 2>/dev/null

echo "✅ Limpeza concluída."
echo ""

echo "🚀 --- FASE 2: SUBINDO AMBIENTE (Porta Banco: 5438) --- 🚀"
# Sobe reconstruindo para garantir que pegou as configs novas (limite de conexões)
sudo docker-compose up --build -d

echo "⏳ Aguardando o Banco de Dados acordar (15s)..."
sleep 15

# Verifica se o user-service subiu
if sudo docker ps | grep -q "user-service"; then
    echo "✅ SISTEMA NO AR!"
    echo "📊 Status dos containers:"
    sudo docker-compose ps
else
    echo "⚠️ ALERTA: Algo não subiu corretamente. Verifique os logs."
fi
