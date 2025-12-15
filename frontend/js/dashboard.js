// frontend/js/dashboard.js

// Função auxiliar para pegar o ID do usuário do Token (Reutilizando lógica)
function getUserIdFromToken() {
    const token = localStorage.getItem('token');
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
    const userId = getUserIdFromToken();
    if (!userId) return;

    // 1. Busca as Inscrições (Registration Service)
    const resRegs = await fetchAuth('/inscricoes');
    if (!resRegs) return;
    const regs = await resRegs.json();

    // 2. Busca os Eventos para pegar os nomes
    const resEvents = await fetchAuth('/eventos');
    let eventosMap = {};
    if (resEvents && resEvents.ok) {
        const eventos = await resEvents.json();
        eventos.forEach(evt => {
            eventosMap[evt.id] = evt.titulo || evt.title;
        });
    }

    // 3. BUSCA CRÍTICA: Busca as Presenças reais (Attendance Service)
    // Isso resolve o problema do status sempre "Pendente"
    let presencasIds = [];
    try {
        const resPresencas = await fetchAuth(`/presencas/usuario/${userId}`);
        if (resPresencas.ok) {
            const listaPresencas = await resPresencas.json();
            // Mapeia para uma lista de IDs de eventos que o usuário compareceu
            presencasIds = listaPresencas.map(p => p.eventId);
        }
    } catch (e) {
        console.error("Erro ao buscar presenças", e);
    }

    const tbody = document.getElementById('myRegistrationsTable');
    if (tbody) {
        tbody.innerHTML = '';
        
        regs.forEach(reg => {
            // VERIFICAÇÃO DE OURO: O usuário está na lista de presenças do Attendance Service?
            // Ignoramos o campo 'presente' do registration-service pois ele pode estar desatualizado
            const isPresente = presencasIds.includes(reg.eventoId);

            const status = isPresente 
                ? '<span class="badge bg-success">Presente</span>' 
                : '<span class="badge bg-warning">Pendente</span>';
            
            let actionBtn = '';
            
            if (!isPresente) {
                // Botão corrigido para usar a nova lógica de check-in
                actionBtn = `
                    <button onclick="checkIn(${reg.eventoId})" class="btn btn-sm btn-outline-success me-1">Check-in</button>
                    <button onclick="cancelarInscricao(${reg.id})" class="btn btn-sm btn-outline-danger">Cancelar</button>
                `;
            } else {
                // Se já está presente, libera certificado
                // Passamos os dados na URL ou salvamos no storage para a pag de certificados usar
                actionBtn = `<a href="certificados.html" class="btn btn-sm btn-outline-primary">Ver Certificado</a>`;
            }

            const nomeEvento = eventosMap[reg.eventoId] || `Evento #${reg.eventoId}`;

            const row = `
                <tr>
                    <td>${reg.eventoId}</td>
                    <td><strong>${nomeEvento}</strong></td>
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
    const userId = getUserIdFromToken();
    const res = await fetchAuth('/inscricoes', {
        method: 'POST',
        body: JSON.stringify({ userId: userId, eventoId: eventoId })
    });

    if (res.ok) {
        alert('Inscrição realizada com sucesso!');
        loadRegistrations();
    } else {
        const msg = await res.text();
        alert('Erro: ' + msg);
    }
}

// CORREÇÃO CRÍTICA: Aponta para o Attendance Service
async function checkIn(eventoId) {
    const userId = getUserIdFromToken();
    
    // Agora bate em /presencas e manda userId + eventId
    const res = await fetchAuth('/presencas', {
        method: 'POST',
        body: JSON.stringify({ 
            userId: userId, 
            eventId: eventoId 
        })
    });

    if (res.ok) {
        alert('Check-in realizado! Você agora pode emitir seu certificado.');
        loadRegistrations();
    } else {
        const erro = await res.text();
        alert('Erro ao realizar check-in: ' + erro);
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

// Inicialização segura
document.addEventListener('DOMContentLoaded', () => {
    loadEvents();
    loadRegistrations();
    verificarAdmin();
});