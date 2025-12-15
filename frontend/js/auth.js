document.getElementById('loginForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const email = document.getElementById('loginEmail').value;
    const password = document.getElementById('loginPass').value;

    try {
        const res = await fetch(`${API_BASE_URL}/auth`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        });

        if (res.ok) {
            const data = await res.json();
            localStorage.setItem('token', data.token);
            localStorage.setItem('userEmail', email);
            window.location.href = 'painel.html';
        } else {
            alert('Login falhou: Credenciais inválidas');
        }
    } catch (err) {
        console.error(err);
        alert('Erro ao conectar com o servidor.');
    }
});

document.getElementById('registerForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    
    // Coleta dos dados completos
    const payload = {
        name: document.getElementById('regName').value,
        email: document.getElementById('regEmail').value,
        password: document.getElementById('regPass').value,
        cpf: document.getElementById('regCpf').value,
        enderecoRua: document.getElementById('regRua').value,
        enderecoNumero: document.getElementById('regNum').value,
        enderecoBairro: document.getElementById('regBairro').value,
        enderecoCidade: document.getElementById('regCidade').value,
        enderecoEstado: document.getElementById('regUF').value,
        role: "PARTICIPANTE"
    };

    try {
        const res = await fetch(`${API_BASE_URL}/users`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (res.ok) {
            alert('Cadastro realizado! Faça login.');
            document.getElementById('login-tab').click();
        } else {
            alert('Erro no cadastro. Verifique se o e-mail já existe.');
        }
    } catch (err) {
        alert('Erro ao conectar.');
    }
});