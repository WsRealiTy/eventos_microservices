import requests
import json
import os
import time
import uuid

# --- CONFIGURAÇÕES ---
API_GATEWAY = "http://localhost:8080"
ARQUIVO_DB_LOCAL = "db_cache.json"    # Guarda usuários e eventos baixados
ARQUIVO_FILA = "fila_offline.json"    # Guarda ações para sincronizar

# Token de admin para o terminal realizar operações (login fixo ou hardcoded para teste)
TOKEN = None

# --- UTILITÁRIOS ---

def limpar_tela():
    os.system('cls' if os.name == 'nt' else 'clear')

def salvar_json(arquivo, dados):
    with open(arquivo, 'w', encoding='utf-8') as f:
        json.dump(dados, f, indent=4, ensure_ascii=False)

def ler_json(arquivo):
    if not os.path.exists(arquivo):
        return [] if arquivo == ARQUIVO_FILA else {}
    try:
        with open(arquivo, 'r', encoding='utf-8') as f:
            return json.load(f)
    except:
        return [] if arquivo == ARQUIVO_FILA else {}

def get_headers():
    return {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {TOKEN}"
    }

# --- FUNÇÕES DE REDE (ONLINE) ---

def login():
    global TOKEN
    print("\n🔐 == LOGIN DO TERMINAL ==")
    email = input("Email admin: ")
    senha = input("Senha: ")
    try:
        resp = requests.post(f"{API_GATEWAY}/auth", json={"email": email, "password": senha})
        if resp.status_code == 200:
            TOKEN = resp.json()['token']
            print("✅ Login realizado com sucesso!")
            time.sleep(1)
            return True
        else:
            print("❌ Falha no login.")
            return False
    except:
        print("❌ Erro de conexão. O servidor está online?")
        return False

def baixar_dados_servidor():
    """Passo 12: Sincronizar dados com ambiente local"""
    print("\n⬇️  Baixando dados do portal...")
    try:
        users = requests.get(f"{API_GATEWAY}/users", headers=get_headers()).json()
        events = requests.get(f"{API_GATEWAY}/eventos", headers=get_headers()).json()
        
        db = {
            "users": users,
            "events": events,
            "timestamp": time.strftime("%Y-%m-%d %H:%M:%S")
        }
        salvar_json(ARQUIVO_DB_LOCAL, db)
        print(f"✅ Dados atualizados! {len(users)} usuários e {len(events)} eventos em cache.")
    except Exception as e:
        print(f"⚠️  Erro ao baixar dados (Modo Offline?): {e}")

def subir_dados_pendentes():
    """Passo 19: Sincronizar dados com o Portal (Upload)"""
    fila = ler_json(ARQUIVO_FILA)
    if not fila:
        print("✅ Nada para sincronizar.")
        return

    print(f"\n🔄 Sincronizando {len(fila)} ações pendentes...")
    
    # Mapa para converter ID temporário -> ID Real do banco
    mapa_ids = {} 

    nova_fila = []
    
    for item in fila:
        acao = item['acao']
        dados = item['dados']
        sucesso = False
        
        try:
            # 1. Resolver IDs temporários se necessário
            if 'userId' in dados and str(dados['userId']).startswith('temp_'):
                temp_id = dados['userId']
                if temp_id in mapa_ids:
                    dados['userId'] = mapa_ids[temp_id] # Troca pelo ID real
                else:
                    print(f"⚠️  Dependência falhou: Usuário {temp_id} ainda não sincronizado.")
                    nova_fila.append(item)
                    continue

            # 2. Executar Ação
            if acao == 'cadastro_usuario':
                # Remove ID temporário antes de enviar
                temp_id = dados.pop('temp_id', None)
                print(f"📤 Cadastrando usuário: {dados['name']}...")
                resp = requests.post(f"{API_GATEWAY}/users", json=dados, headers=get_headers()) # Sem autenticação no endpoint users? Se precisar, ajustar.
                
                if resp.status_code in [200, 201]:
                    user_criado = resp.json()
                    real_id = user_criado['id']
                    if temp_id:
                        mapa_ids[temp_id] = real_id # Guarda mapeamento
                    print(f"   ✅ Criado com ID real: {real_id}")
                    sucesso = True
                else:
                    print(f"   ❌ Erro {resp.status_code}: {resp.text}")

            elif acao == 'inscricao':
                print(f"📤 Inscrevendo User {dados['userId']} no Evento {dados['eventoId']}...")
                resp = requests.post(f"{API_GATEWAY}/inscricoes", json=dados, headers=get_headers())
                if resp.status_code in [200, 201]:
                    print("   ✅ Inscrição OK")
                    sucesso = True
                else:
                    print(f"   ❌ Erro {resp.status_code}: {resp.text}")

            elif acao == 'checkin':
                print(f"📤 Check-in User {dados['userId']} no Evento {dados['eventoId']}...")
                # O endpoint de presença pode variar, ajuste conforme sua API
                # Se for POST /presencas
                payload = {
                    "userId": dados['userId'],
                    "eventId": dados['eventoId'],
                    "checkedIn": True
                }
                resp = requests.post(f"{API_GATEWAY}/presencas", json=payload, headers=get_headers())
                if resp.status_code in [200, 201]:
                    print("   ✅ Check-in OK")
                    sucesso = True
                else:
                    print(f"   ❌ Erro {resp.status_code}: {resp.text}")

        except Exception as e:
            print(f"   ❌ Erro de conexão: {e}")

        if not sucesso:
            # Se falhou, mantém na fila (com os dados originais/atualizados)
            # Se falhou cadastro, precisamos garantir que o temp_id volte para tentar de novo? 
            # Simplificação: Se falhou cadastro, o resto vai falhar. Mantem tudo na fila.
            if acao == 'cadastro_usuario' and temp_id:
                dados['temp_id'] = temp_id # Devolve o temp_id para tentar de novo
            item['dados'] = dados
            nova_fila.append(item)

    salvar_json(ARQUIVO_FILA, nova_fila)
    if len(nova_fila) == 0:
        print("🎉 Sincronização completa!")
    else:
        print(f"⚠️  {len(nova_fila)} itens restaram na fila.")

# --- FUNÇÕES DE MODO OFFLINE ---

def novo_participante_offline():
    """Passo 14: Cadastrar participante offline"""
    print("\n📝 Cadastro Offline")
    nome = input("Nome: ")
    email = input("Email: ")
    senha = "123" # Senha padrão para cadastro rápido
    
    temp_id = f"temp_{uuid.uuid4().hex[:6]}"
    
    dados = {
        "name": nome,
        "email": email,
        "password": senha,
        "role": "PARTICIPANTE",
        "temp_id": temp_id
    }
    
    # Salva na fila
    fila = ler_json(ARQUIVO_FILA)
    fila.append({"acao": "cadastro_usuario", "dados": dados})
    salvar_json(ARQUIVO_FILA, fila)
    
    # Atualiza cache local para aparecer nas listas
    db = ler_json(ARQUIVO_DB_LOCAL)
    if 'users' not in db: db['users'] = []
    
    # Adiciona visualmente ao cache (com ID temporário)
    user_cache = dados.copy()
    user_cache['id'] = temp_id
    db['users'].append(user_cache)
    salvar_json(ARQUIVO_DB_LOCAL, db)
    
    print(f"⚠️  Usuário salvo localmente (ID Provisório: {temp_id})")
    return temp_id

def listar_e_selecionar(lista, chave_exibicao):
    if not lista:
        print("(Nenhum dado disponível no cache)")
        return None
    for i, item in enumerate(lista):
        display = item.get(chave_exibicao) or item.get('title') or "Sem Nome"
        print(f"{i+1}. [{item['id']}] {display}")
    
    try:
        opt = int(input("Selecione o número: ")) - 1
        if 0 <= opt < len(lista):
            return lista[opt]['id']
    except:
        pass
    return None

def operacao_evento_offline(tipo_acao):
    """Passo 15 e 16: Inscrição e Presença Offline"""
    db = ler_json(ARQUIVO_DB_LOCAL)
    print(f"\n-- {tipo_acao.upper()} (MODO OFFLINE/LOCAL) --")
    
    print("Selecione o Evento:")
    evento_id = listar_e_selecionar(db.get('events', []), 'titulo')
    if not evento_id: return

    print("Selecione o Usuário:")
    user_id = listar_e_selecionar(db.get('users', []), 'name')
    if not user_id: return
    
    # Adiciona à fila
    fila = ler_json(ARQUIVO_FILA)
    fila.append({
        "acao": tipo_acao,
        "dados": {
            "eventoId": evento_id,
            "userId": user_id,
            "timestamp": time.strftime("%Y-%m-%d %H:%M:%S")
        }
    })
    salvar_json(ARQUIVO_FILA, fila)
    print("⚠️  Ação salva na fila para sincronização.")

# --- MENU PRINCIPAL ---

def menu():
    while True:
        # limpar_tela()
        print("\n=== 📡 TERMINAL DE PORTARIA (Python) ===")
        print(f"Status: {'ONLINE 🟢' if TOKEN else 'OFFLINE 🔴 (Token não definido)'}")
        print("1. Login (Necessário para Online)")
        print("2. Baixar Dados (Cache)")
        print("---------------------------")
        print("3. Cadastrar Participante (Simulação Offline)")
        print("4. Inscrever em Evento (Simulação Offline)")
        print("5. Registrar Presença/Check-in (Simulação Offline)")
        print("---------------------------")
        print("6. SINCRONIZAR COM O PORTAL (Upload)")
        print("0. Sair")
        
        opcao = input("Opção: ")
        
        if opcao == '1': login()
        elif opcao == '2': baixar_dados_servidor()
        elif opcao == '3': novo_participante_offline()
        elif opcao == '4': operacao_evento_offline('inscricao')
        elif opcao == '5': operacao_evento_offline('checkin')
        elif opcao == '6': subir_dados_pendentes()
        elif opcao == '0': break

if __name__ == "__main__":
    menu()