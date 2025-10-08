const roleInput = document.getElementById('login-role'); // admin_login.htmlにのみ存在する
const loginForm = document.getElementById('login-form');
const usernameInput = document.getElementById('username');
const passwordInput = document.getElementById('password');
const errorMessage = document.getElementById('error-message');

// ★ 修正点1: タブに関する処理をすべて削除

loginForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    
    // roleInputが存在しない場合は 'user' (打刻) と見なす
    // admin_login.html の場合は hidden input の値 ('admin') が使われる
    const role = roleInput ? roleInput.value : 'user'; 
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
            // ★ 修正点2: レスポンスボディからemployeeIdを取得
            const loginData = await response.json(); 
            const employeeId = loginData.employeeId; 

            if (!employeeId) {
                 // IDがない場合は致命的なエラーとして処理
                throw new Error('ログイン成功しましたが、従業員IDが返されませんでした。');
            }

            // ★ 修正点3: 従業員IDをセッションストレージに保存
            sessionStorage.setItem('loggedInEmployeeId', employeeId);

            // 画面遷移ロジック
            if (role === 'user') {
                // 打刻画面への遷移 (例: punch.html)
                window.location.href = 'punch.html'; 
            } else if (role === 'admin') {
                // 管理者画面への遷移
                window.location.href = 'employee_management.html'; 
            }
        } else {
            const errorData = await response.json();
            errorMessage.textContent = errorData.message || 'ログインに失敗しました。ユーザー名またはパスワードを確認してください。';
        }
    } catch (error) {
        console.error('Login error:', error);
        errorMessage.textContent = 'ログイン処理中にエラーが発生しました。詳細はコンソールを確認してください。';
    }
});