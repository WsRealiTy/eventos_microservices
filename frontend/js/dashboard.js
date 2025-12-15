// frontend/js/dashboard.js
// Observação: API_BASE_URL, fetchAuth e logout são herdados de api.js

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

// Inicialização segura
document.addEventListener('DOMContentLoaded', () => {
    loadEvents();
    loadRegistrations();
    verificarAdmin();
});