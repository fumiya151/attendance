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
                // IDがない場合は致命的なエラーとして処理
                throw new Error('ログイン成功しましたが、従業員IDが返されませんでした。');
            }

            sessionStorage.setItem('loggedInEmployeeId', employeeId);

            if (role === 'user') {
                window.location.href = 'main.html'; 
            } else if (role === 'admin') {
                window.location.href = 'admin_main.html'; 
            }
        } else {
            // ステータスコードが200以外の場合
            // JSONでエラーレスポンスをパース試みる
            let errorText = await response.text();
            let errorData = null;
            try {
                errorData = JSON.parse(errorText);
            } catch (e) {
                // JSONパース失敗時は、そのままレスポンステキストを使用
                console.error("Error response was not JSON:", errorText);
            }

            const message = errorData ? (errorData.message || `ログイン失敗 (Status: ${response.status})`) : `サーバーエラーが発生しました (Status: ${response.status} / Content: ${errorText.substring(0, 50)}...)`;
            errorMessage.textContent = message;
        }
    } catch (error) {
        // ネットワークエラーやJSONパース失敗など
        console.error('Fatal Login Error:', error);
        errorMessage.textContent = '致命的な通信エラーが発生しました。サーバーまたはネットワーク接続を確認してください。';
    }
});