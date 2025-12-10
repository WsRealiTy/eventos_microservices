import requests
import json
import os
import time

# URL do seu microsserviço Java
API_URL = "http://localhost:8080/presencas"
ARQUIVO_OFFLINE = "dados_offline.json"

def salvar_localmente(dados):
    lista_atual = []
    if os.path.exists(ARQUIVO_OFFLINE):
        with open(ARQUIVO_OFFLINE, 'r') as f:
            try:
                lista_atual = json.load(f)
            except:
                lista_atual = []
    
    lista_atual.append(dados)
    
    with open(ARQUIVO_OFFLINE, 'w') as f:
        json.dump(lista_atual, f, indent=4)
    print("⚠️  Sem internet! Dados salvos localmente no arquivo.")

def registrar_presenca(user_id, event_id):
    payload = {
        "userId": user_id,
        "eventId": event_id,
        "checkedIn": True,
        "timestamp": time.strftime("%Y-%m-%d %H:%M:%S")
    }

    try:
        # Tenta enviar para o Java
        response = requests.post(API_URL, json=payload, timeout=2)
        if response.status_code == 200:
            print("✅ Sucesso! Presença registrada no servidor (Online).")
        else:
            print(f"Erro no servidor: {response.status_code}")
            salvar_localmente(payload)
    except requests.exceptions.ConnectionError:
        # Se o Java estiver desligado ou sem rede
        salvar_localmente(payload)

def sincronizar_dados():
    if not os.path.exists(ARQUIVO_OFFLINE):
        print("Nenhum dado offline para sincronizar.")
        return

    with open(ARQUIVO_OFFLINE, 'r') as f:
        dados_offline = json.load(f)

    if not dados_offline:
        return

    print(f"🔄 Tentando sincronizar {len(dados_offline)} registros...")
    
    try:
        # Manda tudo de uma vez para o endpoint /sync
        response = requests.post(f"{API_URL}/sync", json=dados_offline)
        if response.status_code == 200:
            print("✅ Sincronização concluída com sucesso!")
            # Limpa o arquivo local
            os.remove(ARQUIVO_OFFLINE) 
        else:
            print("Erro ao sincronizar via API.")
    except Exception as e:
        print(f"Ainda sem conexão: {e}")

# Menu simples para simular
while True:
    print("\n--- TERMINAL PORTARIA ---")
    print("1. Registrar Presença")
    print("2. Tentar Sincronizar (Voltou a Internet)")
    print("3. Sair")
    opcao = input("Opção: ")

    if opcao == "1":
        u_id = input("ID do Usuário: ")
        e_id = input("ID do Evento: ")
        registrar_presenca(u_id, e_id)
    elif opcao == "2":
        sincronizar_dados()
    elif opcao == "3":
        break