// js/portaria.js

const API_BASE_URL = "http://localhost:8080"; 

// --- GESTÃO DE ESTADO ONLINE/OFFLINE ---

function updateStatus() {
    const isOnline = navigator.onLine;
    document.getElementById('offlineBanner').style.display = isOnline ? 'none' : 'block';
    document.getElementById('onlineBanner').style.display = isOnline ? 'block' : 'none';
    
    const pendentes = getFilaOffline();
    const btnSync = document.getElementById('btnSync');
    if (isOnline && pendentes.length > 0) {
        btnSync.disabled = false;
        btnSync.innerText = `🔄 Sincronizar (${pendentes.length} itens)`;
    } else {
        btnSync.disabled = true;
    }
    
    document.getElementById('pendentesCount').innerText = pendentes.length;
}

window.addEventListener('online', updateStatus);
window.addEventListener('offline', updateStatus);
window.addEventListener('load', updateStatus);

// --- LÓGICA DE FILA OFFLINE ---

function getFilaOffline() {
    const fila = localStorage.getItem('fila_eventos');
    return fila ? JSON.parse(fila) : [];
}

function salvarNaFila(acao) {
    const fila = getFilaOffline();
    fila.push(acao);
    localStorage.setItem('fila_eventos', JSON.stringify(fila));
    updateStatus();
    alert('Sem internet! Ação salva localmente para sincronizar depois.');
}

function limparFila() {
    localStorage.removeItem('fila_eventos');
    updateStatus();
}

// --- AÇÕES DO USUÁRIO ---

// 1. Check-in Simples (Usuário já existe)
async function realizarCheckIn() {
    const email = document.getElementById('emailCheckin').value;
    const eventoId = document.getElementById('eventoId').value;

    if(!email || !eventoId) return alert('Preencha todos os campos');

    const payload = { email, eventoId, tipo: 'CHECKIN_EXISTENTE' };

    if (!navigator.onLine) {
        salvarNaFila(payload);
        document.getElementById('emailCheckin').value = '';
        return;
    }

    try {
        await processarItemUnitario(payload);
        alert('Check-in realizado com sucesso (Online)!');
        document.getElementById('emailCheckin').value = '';
    } catch (e) {
        alert('Erro ao processar online: ' + e.message);
    }
}

// 2. Cadastro Rápido + Check-in
async function cadastrarECheckIn() {
    const nome = document.getElementById('novoNome').value;
    const email = document.getElementById('novoEmail').value;
    const eventoId = document.getElementById('eventoId').value;

    if(!nome || !email || !eventoId) return alert('Preencha tudo');

    const payload = { 
        nome, 
        email, 
        eventoId, 
        tipo: 'CADASTRO_NOVO' 
    };

    if (!navigator.onLine) {
        salvarNaFila(payload);
        document.getElementById('novoNome').value = '';
        document.getElementById('novoEmail').value = '';
        return;
    }

    try {
        await processarItemUnitario(payload);
        alert('Cadastro e Check-in realizados (Online)!');
        document.getElementById('novoNome').value = '';
        document.getElementById('novoEmail').value = '';
    } catch (e) {
        alert('Erro: ' + e.message);
    }
}

// --- LÓGICA DE SINCRONIZAÇÃO (O "CÉREBRO" DO SISTEMA) ---

async function sincronizarAgora() {
    const fila = getFilaOffline();
    if (fila.length === 0) return;

    const logDiv = document.getElementById('logSync');
    logDiv.innerHTML = 'Iniciando sincronização...<br>';
    
    let erros = 0;
    let novosPendentes = [];

    for (const item of fila) {
        try {
            logDiv.innerHTML += `Processando: ${item.email}... `;
            await processarItemUnitario(item);
            logDiv.innerHTML += `<span class="text-success">OK</span><br>`;
        } catch (error) {
            console.error(error);
            logDiv.innerHTML += `<span class="text-danger">ERRO (${error.message})</span><br>`;
            erros++;
            novosPendentes.push(item); 
        }
    }

    localStorage.setItem('fila_eventos', JSON.stringify(novosPendentes));
    updateStatus();

    if (erros === 0) {
        alert("Sincronização concluída com sucesso!");
        logDiv.innerHTML += "<strong>Tudo sincronizado!</strong>";
    } else {
        alert(`Sincronização finalizada com ${erros} erros. Tente novamente.`);
    }
}

// Função Auxiliar que faz as chamadas reais à API
async function processarItemUnitario(item) {
    let userId = null;
    
    // Logica para CADASTRO_NOVO
    if (item.tipo === 'CADASTRO_NOVO') {
        const resCreate = await fetch(`${API_BASE_URL}/users`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({
                name: item.nome,
                email: item.email,
                password: '123', 
                role: 'PARTICIPANTE'
            })
        });
        
        if (resCreate.status === 409) {
            throw new Error("Usuário já existe. Use Check-in Existente.");
        }
        
        if (!resCreate.ok) throw new Error("Falha ao criar usuário");
        const userCreated = await resCreate.json();
        userId = userCreated.id;
    } 
    // Logica para CHECKIN_EXISTENTE
    else {
        // --- CORREÇÃO AQUI: Adicionado header Authorization ---
        const resList = await fetch(`${API_BASE_URL}/users`, {
            headers: {
                'Authorization': `Bearer ${getAdminToken()}`
            }
        });
        
        if (!resList.ok) throw new Error("Erro ao buscar usuários (Permissão ou Falha)");

        const users = await resList.json();
        const user = users.find(u => u.email === item.email);
        
        if (!user) throw new Error("Usuário não encontrado no banco.");
        userId = user.id;
    }

    // Passo 2: Garantir Inscrição
    const resInscricao = await fetch(`${API_BASE_URL}/inscricoes`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${getAdminToken()}`
        },
        body: JSON.stringify({ userId: userId, eventoId: item.eventoId })
    });
    
    if (!resInscricao.ok && resInscricao.status !== 400) {
        const txt = await resInscricao.text();
        if (!txt.includes("já inscrito")) throw new Error("Erro na inscrição: " + txt);
    }

    // Passo 3: Registrar Presença (Check-in)
    const resPresenca = await fetch(`${API_BASE_URL}/inscricoes/presenca`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${getAdminToken()}`
        },
        body: JSON.stringify({ userId: userId, eventoId: item.eventoId })
    });

    if (!resPresenca.ok) throw new Error("Erro no Check-in final");
}

function getAdminToken() {
    return localStorage.getItem('token') || ''; 
}