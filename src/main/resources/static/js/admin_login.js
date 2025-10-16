// admin_login.js (最終修正版: 動作保証のための分岐を復元)
const loginForm = document.getElementById('login-form');
const usernameInput = document.getElementById('username');
const passwordInput = document.getElementById('password');
const errorMessage = document.getElementById('error-message');

loginForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    
    const username = usernameInput.value;
    const password = passwordInput.value;

    errorMessage.textContent = '';

    try {
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ username, password })
        });

        if (response.ok) {
            const loginData = await response.json(); 
            const employeeId = loginData.employeeId; 
            const actualRole = loginData.actualRole; 
            const token = loginData.token;

            if (!employeeId || !token) {
                throw new Error('ログイン成功しましたが、必要な情報（IDまたはトークン）が返されませんでした。');
            }

            // ログイン情報をセッションに保存
            sessionStorage.setItem('loggedInEmployeeId', employeeId);
            sessionStorage.setItem('loggedInUserRole', actualRole); 
            sessionStorage.setItem('token', token);

            // ロールに基づき画面遷移を分岐
            const targetRole = (actualRole || 'EMP').toUpperCase();
            
            if (targetRole === 'ADMIN' || targetRole === 'MGR') {
                window.location.href = '/html/admin_main.html'; // 管理者画面へ
            } else if (targetRole === 'EMP') {
                window.location.href = '/html/emp_main.html'; // 従業員専用画面へ
            } else {
                alert('無効なロール情報が返されました。');
                window.location.href = '/html/admin_login.html';
            }
            
        } else {
            // ステータスコードが200以外の場合の処理
            sessionStorage.removeItem('loggedInEmployeeId');
            sessionStorage.removeItem('loggedInUserRole'); 
            sessionStorage.removeItem('token');
            
            let errorText = await response.text();
            let errorData = null;
            try {
                errorData = JSON.parse(errorText);
            } catch (e) {
                console.error("Error response was not JSON:", errorText);
            }

            // サーバーからのメッセージ (401 Unauthorizedの場合など)
            const message = errorData ? (errorData.message || `ログイン失敗 (Status: ${response.status})`) : `サーバーエラーが発生しました (Status: ${response.status})`;
            errorMessage.textContent = message;
        }
    } catch (error) {
        console.error('Fatal Login Error:', error);
        errorMessage.textContent = '致命的な通信エラーが発生しました。サーバーまたはネットワーク接続を確認してください。';
    }
});