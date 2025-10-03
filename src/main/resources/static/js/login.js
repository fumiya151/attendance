const tabs = document.querySelectorAll('.tab-item');
const roleInput = document.getElementById('login-role');
const loginForm = document.getElementById('login-form');
const usernameInput = document.getElementById('username');
const passwordInput = document.getElementById('password');
const errorMessage = document.getElementById('error-message');

tabs.forEach(tab => {
    tab.addEventListener('click', () => {
        tabs.forEach(t => t.classList.remove('active'));
        tab.classList.add('active');
        roleInput.value = tab.dataset.role;
    });
});

loginForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    
    const role = roleInput.value;
    const username = usernameInput.value;
    const password = passwordInput.value;

    errorMessage.textContent = '';

    try {
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ username, password, role })
        });

        if (response.ok) {
            if (role === 'user') {
                window.location.href = 'main.html';
            } else if (role === 'admin') {
                window.location.href = 'admin_main.html';
            }
        } else {
            const errorData = await response.json();
            errorMessage.textContent = errorData.message || 'ログインに失敗しました。';
        }
    } catch (error) {
        console.error('Login error:', error);
        errorMessage.textContent = 'ログイン処理中にエラーが発生しました。';
    }
});
