async function loadEvents() {
    const res = await fetchAuth('/eventos');
    if (!res) return;
    
    const events = await res.json();
    const container = document.getElementById('eventsContainer');
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

async function loadRegistrations() {
    const res = await fetchAuth('/inscricoes');
    if (!res) return;

    const regs = await res.json();
    const tbody = document.getElementById('myRegistrationsTable');
    tbody.innerHTML = '';

    regs.forEach(reg => {
        const status = reg.presente ? '<span class="badge bg-success">Presente</span>' : '<span class="badge bg-warning">Pendente</span>';
        const actionBtn = !reg.presente 
            ? `<button onclick="checkIn(${reg.eventoId})" class="btn btn-sm btn-outline-success">Fazer Check-in</button>`
            : `<a href="certificados.html" class="btn btn-sm btn-outline-primary">Ver Certificado</a>`;

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
    // Endpoint específico conforme seu RegistrationController
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

// Funções para criar evento (teste)
function toggleCreateEvent() {
    document.getElementById('createEventForm').classList.toggle('d-none');
}

async function cadastrarNovoEvento() {
    const title = document.getElementById('newEvtTitle').value;
    const date = document.getElementById('newEvtDate').value;
    const location = document.getElementById('newEvtLoc').value;

    await fetchAuth('/eventos', {
        method: 'POST',
        body: JSON.stringify({ title, date, location })
    });
    
    toggleCreateEvent();
    loadEvents();
}

// Inicialização
loadEvents();
loadRegistrations();