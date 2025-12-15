// frontend/js/portaria.js

// --- GESTÃO DE ESTADO ONLINE/OFFLINE ---

function updateStatus() {
    const isOnline = navigator.onLine;
    document.getElementById('offlineBanner').style.display = isOnline ? 'none' : 'block';
    document.getElementById('onlineBanner').style.display = isOnline ? 'block' : 'none';
    
    const pendentes = getFilaOffline();
    const btnSync = document.getElementById('btnSync');
    if (isOnline && pendentes.length > 0) {
        btnSync.disabled = false;
        btnSync.innerText = `Sincronizar (${pendentes.length} itens)`;
    } else {
        btnSync.disabled = true;
    }
    
    document.getElementById('pendentesCount').innerText = pendentes.length;
}

window.addEventListener('online', updateStatus);
window.addEventListener('offline', updateStatus);

// Carregar eventos ao iniciar a página
window.addEventListener('load', () => {
    updateStatus();
    carregarOpcoesEventos(); 
});

// --- LÓGICA DO DROPDOWN DE EVENTOS ---

async function carregarOpcoesEventos() {
    const select = document.getElementById('eventoId');
    
    try {
        const token = getAdminToken();
        const headers = token ? { 'Authorization': `Bearer ${token}` } : {};

        const res = await fetch(`${API_BASE_URL}/eventos`, { headers });
        
        if (res.ok) {
            const eventos = await res.json();
            localStorage.setItem('eventos_cache', JSON.stringify(eventos));
            renderizarOpcoes(eventos);
        } else {
            console.error("Erro ao buscar eventos:", res.status);
            throw new Error('Falha ao buscar eventos');
        }
    } catch (e) {
        console.warn('Usando cache de eventos:', e);
        const cache = localStorage.getItem('eventos_cache');
        if (cache) {
            renderizarOpcoes(JSON.parse(cache));
        } else {
            select.innerHTML = '<option value="">Não foi possível carregar eventos (Verifique se é Admin)</option>';
        }
    }
}

function renderizarOpcoes(eventos) {
    const select = document.getElementById('eventoId');
    select.innerHTML = '<option value="" disabled selected>Selecione um evento</option>';
    
    eventos.forEach(evt => {
        const option = document.createElement('option');
        option.value = evt.id;
        option.text = `${evt.id} - ${evt.titulo || evt.title}`; 
        select.appendChild(option);
    });
}

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

async function realizarCheckIn() {
    const email = document.getElementById('emailCheckin').value;
    const eventoId = document.getElementById('eventoId').value;

    if(!email || !eventoId) return alert('Preencha o e-mail e selecione o evento.');

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

async function cadastrarECheckIn() {
    const nome = document.getElementById('novoNome').value;
    const email = document.getElementById('novoEmail').value;
    const eventoId = document.getElementById('eventoId').value;

    if(!nome || !email || !eventoId) return alert('Preencha todos os campos (Nome, Email e Selecione o Evento).');

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

// --- LÓGICA DE SINCRONIZAÇÃO ---

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

async function processarItemUnitario(item) {
    let userId = null;
    
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
    } else {
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

    const resPresenca = await fetch(`${API_BASE_URL}/presencas`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${getAdminToken()}`
        },
        body: JSON.stringify({ 
            userId: userId, 
            eventId: parseInt(item.eventoId) 
        })
    });

    if (!resPresenca.ok) {
        const errTxt = await resPresenca.text();
        throw new Error("Erro no Check-in final: " + errTxt);
    }
}

function getAdminToken() {
    return localStorage.getItem('token') || ''; 
}

// --- FUNÇÕES DE PERFIL (Adicionadas) ---

function getUserIdFromToken() {
    const token = getAdminToken();
    if (!token) return null;
    try {
        const base64Url = token.split('.')[1];
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
        const jsonPayload = decodeURIComponent(window.atob(base64).split('').map(function(c) {
            return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
        }).join(''));
        return JSON.parse(jsonPayload).id;
    } catch (e) {
        return null;
    }
}

async function abrirModalPerfil() {
    const userId = getUserIdFromToken();
    if(!userId) return alert("Erro de autenticação.");

    const modalEl = document.getElementById('editProfileModal');
    const modal = new bootstrap.Modal(modalEl);
    
    const res = await fetchAuth(`/users/${userId}`);
    if(res.ok) {
        const user = await res.json();
        
        document.getElementById('editName').value = user.name || '';
        document.getElementById('editCpf').value = user.cpf || '';
        document.getElementById('editEmail').value = user.email || '';
        
        document.getElementById('editRua').value = user.enderecoRua || '';
        document.getElementById('editNum').value = user.enderecoNumero || '';
        document.getElementById('editBairro').value = user.enderecoBairro || '';
        document.getElementById('editCidade').value = user.enderecoCidade || '';
        document.getElementById('editUF').value = user.enderecoEstado || '';
        
        document.getElementById('editPassword').value = ''; 
        
        modal.show();
    } else {
        alert("Erro ao carregar perfil.");
    }
}

async function salvarPerfil() {
    const userId = getUserIdFromToken();
    const payload = {
        name: document.getElementById('editName').value,
        cpf: document.getElementById('editCpf').value,
        enderecoRua: document.getElementById('editRua').value,
        enderecoNumero: document.getElementById('editNum').value,
        enderecoBairro: document.getElementById('editBairro').value,
        enderecoCidade: document.getElementById('editCidade').value,
        enderecoEstado: document.getElementById('editUF').value,
        password: document.getElementById('editPassword').value
    };

    const res = await fetchAuth(`/users/${userId}`, {
        method: 'PUT',
        body: JSON.stringify(payload)
    });

    if(res.ok) {
        alert('Perfil atualizado com sucesso!');
        const modalEl = document.getElementById('editProfileModal');
        const modal = bootstrap.Modal.getInstance(modalEl);
        modal.hide();
    } else {
        alert('Erro ao atualizar perfil.');
    }
}