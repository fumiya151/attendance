const roleInput = document.getElementById('login-role');
const loginForm = document.getElementById('login-form');
const usernameInput = document.getElementById('username');
const passwordInput = document.getElementById('password');
const errorMessage = document.getElementById('error-message');

loginForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    
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
            const loginData = await response.json(); 
            const employeeId = loginData.employeeId; 

            if (!employeeId) {
                throw new Error('ログイン成功しましたが、従業員IDが返されませんでした。');
            }

            // IDは正しいキーで保存されている (問題なし)
            sessionStorage.setItem('loggedInEmployeeId', employeeId);

            // ★ 修正点: 遷移先のファイル名を明確化 ★
            if (role === 'user') {
                // 打刻画面へ遷移
                window.location.href = 'main.html'; 
            } else if (role === 'admin') {
                // 管理者画面へ遷移
                window.location.href = 'admin_main.html'; 
            }
        } else {
            // ステータスコードが200以外の場合のデバッグ強化ロジックは問題なし
            let errorText = await response.text();
            let errorData = null;
            try {
                errorData = JSON.parse(errorText);
            } catch (e) {
                console.error("Error response was not JSON:", errorText);
            }

            const message = errorData ? (errorData.message || `ログイン失敗 (Status: ${response.status})`) : `サーバーエラーが発生しました (Status: ${response.status} / Content: ${errorText.substring(0, 50)}...)`;
            errorMessage.textContent = message;
        }
    } catch (error) {
        console.error('Fatal Login Error:', error);
        errorMessage.textContent = '致命的な通信エラーが発生しました。サーバーまたはネットワーク接続を確認してください。';
    }
});