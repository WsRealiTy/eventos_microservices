// js/api.js
//const API_BASE_URL = "http://177.44.248.112:8080";
const API_BASE_URL = "http://localhost:8080"; 

// Função auxiliar para fazer requisições autenticadas
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

// Em frontend/js/api.js

function verificarAdmin() {
    const token = localStorage.getItem('token');
    if (!token) return;

    try {
        const base64Url = token.split('.')[1];
        let base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');

        while (base64.length % 4) {
            base64 += '=';
        }

        const jsonString = decodeURIComponent(window.atob(base64).split('').map(function(c) {
            return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
        }).join(''));

        const payload = JSON.parse(jsonString);

        // Verifica se a role é exatamente 'ADMIN'
        if (payload.role === 'ADMIN') {
            adicionarBotaoPortaria();
            
            const btnCriar = document.getElementById('btnCriarEvento');
            if (btnCriar) btnCriar.classList.remove('d-none');
        }
    } catch (e) {
        console.error("Erro ao ler permissões ou token inválido", e);
    }
}

function adicionarBotaoPortaria() {
    const navMenu = document.getElementById('navMenu'); // Certifique-se que a UL na navbar tem id="navMenu"
    if (!navMenu || document.getElementById('btnPortaria')) return;

    const li = document.createElement('li');
    li.className = 'nav-item';
    li.id = 'btnPortaria';
    li.innerHTML = `<a class="nav-link text-warning fw-bold" href="portaria.html">Portaria (Admin)</a>`;
    
    // Insere no início ou onde preferir
    navMenu.insertBefore(li, navMenu.firstChild); 
}

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