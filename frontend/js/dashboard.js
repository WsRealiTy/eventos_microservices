const API_BASE_URL = "http://localhost:8080"; 

async function fetchAuth(endpoint, options = {}) {
    const token = localStorage.getItem('token');
    
    if (!token) {
        window.location.href = 'index.html';
        return;
    }

    const headers = {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
        ...options.headers
    };

    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
        ...options,
        headers
    });

    if (response.status === 403 || response.status === 401) {
        alert("Sessão expirada. Faça login novamente.");
        logout();
    }

    return response;
}

function logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('userEmail');
    window.location.href = 'index.html';
}

async function loadEvents() {
    const res = await fetchAuth('/eventos');
    if (!res) return;
    
    const events = await res.json();
    const container = document.getElementById('eventsContainer');
    if (container) {
        container.innerHTML = '';
        events.forEach(evt => {
            const card = `
                <div class="col-md-4 mb-4">
                    <div class="card h-100">
                        <div class="card-body">
                            <h5 class="card-title">${evt.titulo || evt.title}</h5> 
                            <p class="card-text">
                                <strong>Data:</strong> ${evt.data || evt.date}<br>
                                <strong>Local:</strong> ${evt.local || evt.location}
                            </p>
                            <button onclick="inscrever(${evt.id})" class="btn btn-primary w-100">Inscrever-se</button>
                        </div>
                    </div>
                </div>
            `;
            container.innerHTML += card;
        });
    }
}

async function loadRegistrations() {
    const res = await fetchAuth('/inscricoes');
    if (!res) return;

    const regs = await res.json();
    const tbody = document.getElementById('myRegistrationsTable');
    if (tbody) {
        tbody.innerHTML = '';
        regs.forEach(reg => {
            const status = reg.presente 
                ? '<span class="badge bg-success">Presente</span>' 
                : '<span class="badge bg-warning">Pendente</span>';
            
            let actionBtn = '';
            
            if (!reg.presente) {
                actionBtn = `
                    <button onclick="checkIn(${reg.eventoId})" class="btn btn-sm btn-outline-success me-1">Check-in</button>
                    <button onclick="cancelarInscricao(${reg.id})" class="btn btn-sm btn-outline-danger">Cancelar</button>
                `;
            } else {
                actionBtn = `<a href="certificados.html" class="btn btn-sm btn-outline-primary">Ver Certificado</a>`;
            }

            const row = `
                <tr>
                    <td>${reg.id}</td>
                    <td>${reg.eventoId}</td>
                    <td>${status}</td>
                    <td>${actionBtn}</td>
                </tr>
            `;
            tbody.innerHTML += row;
        });
    }
}

async function cancelarInscricao(idInscricao) {
    if(!confirm("Tem certeza que deseja cancelar sua inscrição?")) return;

    const res = await fetchAuth(`/inscricoes/${idInscricao}`, {
        method: 'DELETE'
    });

    if (res.ok) {
        alert('Inscrição cancelada!');
        loadRegistrations(); 
    } else {
        alert('Erro ao cancelar inscrição.');
    }
}

async function inscrever(eventoId) {
    const res = await fetchAuth('/inscricoes', {
        method: 'POST',
        body: JSON.stringify({ eventoId: eventoId })
    });

    if (res.ok) {
        alert('Inscrição realizada com sucesso!');
        loadRegistrations();
    } else {
        const msg = await res.text();
        alert('Erro: ' + msg);
    }
}

async function checkIn(eventoId) {
    const res = await fetchAuth('/inscricoes/presenca', {
        method: 'POST',
        body: JSON.stringify({ eventoId: eventoId })
    });

    if (res.ok) {
        alert('Check-in realizado! Você agora pode emitir seu certificado.');
        loadRegistrations();
    } else {
        alert('Erro ao realizar check-in.');
    }
}

function toggleCreateEvent() {
    const form = document.getElementById('createEventForm');
    if(form) form.classList.toggle('d-none');
}

async function cadastrarNovoEvento() {
    const titulo = document.getElementById('newEvtTitle').value;
    const data = document.getElementById('newEvtDate').value;
    const local = document.getElementById('newEvtLoc').value;

    await fetchAuth('/eventos', {
        method: 'POST',
        body: JSON.stringify({ titulo, data, local })
    });

    toggleCreateEvent();
    loadEvents();
}

// --- FUNÇÃO CORRIGIDA: SALVAR NOVA SENHA ---
async function salvarNovaSenha() {
    const novaSenha = document.getElementById('newPasswordInput').value;
    
    if (!novaSenha) {
        alert("Por favor, digite uma nova senha.");
        return;
    }

    const token = localStorage.getItem('token');
    if (!token) return;

    let userId = null;
    try {
        const base64Url = token.split('.')[1];
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
        
        // CORREÇÃO AQUI: Dividi em duas linhas para garantir que os parênteses fechem corretamente
        const jsonString = decodeURIComponent(window.atob(base64).split('').map(function(c) {
            return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
        }).join(''));
        
        const payload = JSON.parse(jsonString);
        userId = payload.id;
    } catch (e) {
        console.error("Erro ao ler token", e);
        alert("Erro de autenticação. Faça login novamente.");
        return;
    }

    const res = await fetchAuth(`/users/${userId}/senha`, {
        method: 'PUT',
        body: JSON.stringify({ password: novaSenha })
    });

    if (res.ok) {
        alert("Senha alterada com sucesso! Faça login novamente com a nova senha.");
        
        // Tenta fechar o modal visualmente usando o Bootstrap
        const modalEl = document.getElementById('changePasswordModal');
        if (typeof bootstrap !== 'undefined' && modalEl) {
            const modal = bootstrap.Modal.getInstance(modalEl);
            if(modal) modal.hide();
        }
        
        logout(); 
    } else {
        alert("Erro ao alterar senha.");
    }
}

// --- LÓGICA DE ADMIN (PORTARIA) ---

function verificarAdmin() {
    const token = localStorage.getItem('token');
    if (!token) return;

    try {
        const base64Url = token.split('.')[1];
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
        const jsonString = decodeURIComponent(window.atob(base64).split('').map(function(c) {
            return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
        }).join(''));

        const payload = JSON.parse(jsonString);

        if (payload.role === 'ADMIN') {
            adicionarBotaoPortaria();
        }
    } catch (e) {
        console.error("Erro ao ler permissões", e);
    }
}

function adicionarBotaoPortaria() {
    const navMenu = document.getElementById('navMenu');
    
    if (!navMenu) {
        return;
    }
    
    // Verifica se já não foi adicionado para evitar duplicidade
    if(document.getElementById('btnPortaria')) return;

    const li = document.createElement('li');
    li.className = 'nav-item';
    li.id = 'btnPortaria';
    li.innerHTML = `
        <a class="nav-link text-warning fw-bold" href="portaria.html">
            Portaria (Admin)
        </a>
    `;

    navMenu.insertBefore(li, navMenu.firstChild); 
}

// Inicialização segura
document.addEventListener('DOMContentLoaded', () => {
    loadEvents();
    loadRegistrations();
    verificarAdmin();
});