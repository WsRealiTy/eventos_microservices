function visualizarCertificado(eventId, dataEmissao, codigoValidacao) {
    const nomeUsuario = localStorage.getItem('userEmail') || "Participante";
    
    const dataFormatada = new Date(dataEmissao).toLocaleDateString('pt-BR', {
        day: '2-digit', month: 'long', year: 'numeric'
    });

    const printWindow = window.open('', '_blank');
    
    const htmlContent = `
        <html>
        <head>
            <title>Certificado - Evento ${eventId}</title>
            <style>
                @import url('https://fonts.googleapis.com/css2?family=Pinyon+Script&family=Roboto:wght@300;700&display=swap');
                
                body { 
                    font-family: 'Roboto', sans-serif; 
                    text-align: center; 
                    padding: 40px; 
                    background-color: #f0f0f0; 
                }
                .certificado-borda {
                    border: 10px double #198754; /* Verde sucesso bootstrap */
                    padding: 50px;
                    background-color: #fff;
                    width: 900px;
                    margin: 0 auto;
                    position: relative;
                    box-shadow: 0 0 20px rgba(0,0,0,0.1);
                }
                h1 { 
                    font-family: 'Pinyon Script', cursive; 
                    font-size: 80px; 
                    color: #198754; 
                    margin: 0;
                }
                .subtitulo { font-size: 20px; color: #555; text-transform: uppercase; letter-spacing: 2px; margin-top: -10px;}
                p { font-size: 22px; color: #333; margin: 30px 0; line-height: 1.5; }
                
                .destaque { 
                    font-weight: bold; 
                    font-size: 28px;
                    color: #000; 
                    border-bottom: 2px solid #ccc; 
                    display: inline-block; 
                    min-width: 400px; 
                    padding: 5px 20px;
                }
                
                .codigo-area {
                    margin-top: 60px;
                    padding-top: 20px;
                    border-top: 1px dashed #ccc;
                    font-size: 14px;
                    color: #666;
                }
                .uuid { 
                    font-family: 'Courier New', monospace; 
                    font-weight: bold; 
                    font-size: 16px; 
                    letter-spacing: 1px; 
                    background: #eee;
                    padding: 5px 10px;
                    border-radius: 4px;
                }
                .validacao-link {
                    color: #198754;
                    font-weight: bold;
                    text-decoration: none;
                }
                
                .btn-print {
                    margin-top: 30px;
                    padding: 15px 30px;
                    font-size: 18px;
                    background-color: #198754;
                    color: white;
                    border: none;
                    cursor: pointer;
                    border-radius: 5px;
                }
                .btn-print:hover { background-color: #146c43; }

                @media print {
                    body { background: none; padding: 0; }
                    .certificado-borda { width: 100%; border: 5px solid #198754; box-shadow: none; }
                    .btn-print { display: none; }
                }
            </style>
        </head>
        <body>
            <div class="certificado-borda">
                <h1>Certificado</h1>
                <div class="subtitulo">de Participação</div>
                
                <p>Certificamos que</p>
                <div class="destaque">${nomeUsuario}</div>
                
                <p>participou com êxito do evento</p>
                <div class="destaque">Evento ID #${eventId}</div>
                
                <p>Data de emissão: ${dataFormatada}</p>

                <div class="codigo-area">
                    <p>Este documento possui autenticidade verificável.</p>
                    <p>Acesse: <span class="validacao-link">http://localhost:8080/frontend/validar.html</span></p>
                    <p>Código de validação:</p>
                    <span class="uuid">${codigoValidacao}</span>
                </div>

                <button class="btn-print" onclick="window.print()">Imprimir Certificado</button>
            </div>
        </body>
        </html>
    `;

    printWindow.document.write(htmlContent);
    printWindow.document.close();
}

async function loadCertificates() {
    const container = document.getElementById('certContainer');
    
    try {
        const res = await fetchAuth('/certificados');
        if (!res) return; // Erro de auth já tratado no fetchAuth

        if (!res.ok) {
            throw new Error("Erro ao buscar certificados");
        }

        const certs = await res.json();
        container.innerHTML = '';

        if (certs.length === 0) {
            container.innerHTML = '<div class="alert alert-info w-100">Nenhum certificado emitido ainda.</div>';
            return;
        }

        certs.forEach(cert => {
            // Formata data para exibição no card
            const dataCard = new Date(cert.issuedAt).toLocaleDateString('pt-BR');
            // Nota: O backend retorna 'code', não 'signature'
            const codigo = cert.code; 

            const card = `
                <div class="col-md-6 mb-4">
                    <div class="card h-100 shadow-sm border-success">
                        <div class="card-header bg-success text-white d-flex justify-content-between align-items-center">
                            <span>Certificado Emitido</span>
                            <small>Evento #${cert.eventId}</small>
                        </div>
                        <div class="card-body">
                            <h5 class="card-title text-success">Participação Confirmada</h5>
                            <p class="card-text">
                                <strong>Data de Emissão:</strong> ${dataCard}<br>
                                <small class="text-muted">Cód: ${codigo.substring(0, 15)}...</small>
                            </p>
                            
                            <div class="d-grid gap-2">
                                <button onclick="visualizarCertificado('${cert.eventId}', '${cert.issuedAt}', '${codigo}')" 
                                        class="btn btn-outline-success">
                                    Visualizar / Imprimir
                                </button>
                            </div>
                        </div>
                        <div class="card-footer text-muted font-monospace" style="font-size: 0.8rem">
                           ID: ${codigo}
                        </div>
                    </div>
                </div>
            `;
            container.innerHTML += card;
        });

    } catch (error) {
        console.error(error);
        container.innerHTML = '<div class="alert alert-danger">Erro ao carregar certificados.</div>';
    }
}

async function emitirCertificado() {
    const eventIdInput = document.getElementById('certEventId');
    const eventId = eventIdInput.value;

    if (!eventId) return alert("Digite o ID do evento");

    // Feedback visual de carregamento
    const btn = document.querySelector('button[onclick="emitirCertificado()"]');
    const btnOriginalText = btn.innerText;
    btn.innerText = "Emitindo...";
    btn.disabled = true;

    try {
        const res = await fetchAuth('/certificados', {
            method: 'POST',
            body: JSON.stringify({ eventId: parseInt(eventId) })
        });

        if (res.ok) {
            alert("Certificado emitido com sucesso!");
            eventIdInput.value = ""; // Limpa campo
            loadCertificates(); // Recarrega a lista
        } else {
            const err = await res.text();
            alert("Não foi possível emitir: " + err);
        }
    } catch (error) {
        alert("Erro de conexão ao tentar emitir.");
        console.error(error);
    } finally {
        // Restaura o botão
        btn.innerText = btnOriginalText;
        btn.disabled = false;
    }
}

// Carrega ao iniciar
document.addEventListener("DOMContentLoaded", () => {
    loadCertificates();
    // Adicionado chamada para verificar se é admin e liberar opções da navbar
    verificarAdmin();
});