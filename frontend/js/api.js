// js/api.js
const API_BASE_URL = "http://177.44.248.112:8080"; 

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