async function loadCertificates() {
    const res = await fetchAuth('/certificados');
    if (!res) return;

    const certs = await res.json();
    const container = document.getElementById('certContainer');
    container.innerHTML = '';

    if (certs.length === 0) {
        container.innerHTML = '<p class="text-muted">Nenhum certificado encontrado.</p>';
        return;
    }

    certs.forEach(cert => {
        const card = `
            <div class="col-md-6 mb-3">
                <div class="card border-success">
                    <div class="card-header bg-success text-white">Certificado de Participação</div>
                    <div class="card-body">
                        <h5 class="card-title">Código: ${cert.signature}</h5>
                        <p class="card-text">
                            <strong>Evento ID:</strong> ${cert.eventId}<br>
                            <strong>Emissão:</strong> ${cert.issuedAt}
                        </p>
                        <div class="alert alert-secondary py-1">
                            Este certificado comprova sua participação.
                        </div>
                    </div>
                </div>
            </div>
        `;
        container.innerHTML += card;
    });
}

async function emitirCertificado() {
    const eventId = document.getElementById('certEventId').value;
    if (!eventId) return alert("Digite o ID do evento");

    const res = await fetchAuth('/certificados', {
        method: 'POST',
        body: JSON.stringify({ eventId: parseInt(eventId) })
    });

    if (res.ok) {
        alert("Certificado emitido com sucesso!");
        loadCertificates();
    } else {
        const err = await res.text();
        alert("Erro ao emitir (Você confirmou presença?): " + err);
    }
}

loadCertificates();