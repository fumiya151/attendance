// punch_login.js (最終修正版: ADMIN/MGR のみ許可するロジック)

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
            const actualRole = (loginData.actualRole || 'UNKNOWN').toUpperCase(); // サーバーが返した実際のロール
            const token = loginData.token; 

            if (!employeeId || !token || actualRole === 'UNKNOWN') {
                throw new Error('ログイン成功しましたが、必要な情報（ID、ロール、またはトークン）が返されませんでした。');
            }

            if (actualRole === 'ADMIN' || actualRole === 'MGR') {
                
                // ログイン情報をセッションに保存
                sessionStorage.setItem('loggedInEmployeeId', employeeId);
                sessionStorage.setItem('loggedInUserRole', actualRole); 
                sessionStorage.setItem('token', token); 

                // 許可されたロールは、管理者メイン画面に遷移
                window.location.href = '/html/main.html';
                
            } else {
                // EMP、USER、その他のロールは許可しない
                errorMessage.textContent = 'このログイン画面は管理者専用です。ログインをやり直してください。';
                errorMessage.className = 'message-box is-error';
                sessionStorage.clear(); // セッションをクリア
                
                // パスワード入力欄をクリア
                passwordInput.value = '';
            }
            
        } else {
            // ステータスコードが200以外の場合の処理
            let errorText = await response.text();
            let errorData = null;
            try {
                errorData = JSON.parse(errorText);
            } catch (e) {
                console.error("Error response was not JSON:", errorText);
            }

            const message = errorData ? (errorData.message || `ログイン失敗 (Status: ${response.status})`) : `サーバーエラーが発生しました (Status: ${response.status} / Content: ${errorText.substring(0, 50)}...)`;
            errorMessage.textContent = message;
            errorMessage.className = 'message-box is-error';
        }
    } catch (error) {
        console.error('Fatal Login Error:', error);
        errorMessage.textContent = '致命的な通信エラーが発生しました。サーバーまたはネットワーク接続を確認してください。';
        errorMessage.className = 'message-box is-error';
    }
});