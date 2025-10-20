// 汎用メッセージ表示関数 (alertの代替)
function showMessage(message, isError = false) {
    const messageBox = document.getElementById('message-box');
    messageBox.textContent = message;
    messageBox.style.display = 'block';
    messageBox.className = isError ? 'is-error' : 'is-success';
}

/**
 * ログイン中の従業員IDをセッションストレージから取得する
 * (この関数は、他のファイルで定義されているか、このファイルに追加されている必要があります)
 * @returns {string} ログイン従業員ID。見つからない場合はエラーを投げる。
 */
function getLoggedInEmployeeId() {
    const employeeId = sessionStorage.getItem('loggedInEmployeeId'); 
    if (!employeeId) {
        throw new Error("操作を行う従業員IDが見つかりません。ログインが必要です。");
    }
    return employeeId;
}

/**
 * JWTトークンとX-Operator-Idを取得するヘルパー関数
 */
function getAuthHeaders() {
    const token = sessionStorage.getItem('token');
    const employeeId = sessionStorage.getItem('loggedInEmployeeId');
    
    // JWTトークンがない場合はリダイレクト処理を行い、ヘッダー返却はエラー処理に任せる
    if (!token || !employeeId) {
        // ここではエラーをthrowするgetLoggedInEmployeeId()に依存しているため、
        // トークンがない場合も認証エラーとして処理を進める
        return {};
    }
    
    return {
        'Authorization': `Bearer ${token}`,
        'X-Operator-Id': employeeId 
    };
}


// 新規従業員登録フォーム送信
const form = document.getElementById('employee-register-form');
form.addEventListener('submit', async (e) => {
    e.preventDefault();

    const roleSelect = document.getElementById('roleSelect');
    const selectedOption = roleSelect.options[roleSelect.selectedIndex];

    // パスワード一致チェック
    if (form.password.value !== form.passwordConfirm.value) {
        showMessage('⚠️ パスワードが一致しません。', true);
        form.password.value = '';
        form.passwordConfirm.value = '';
        form.password.focus();
        return;
    }
    
    // 役職が選択されているかチェック
    if (!roleSelect.value) {
        showMessage('⚠️ 役職を選択してください。', true);
        return;
    }

    // 役職（department）と役割ID（roleId）を取得
    const selectedRoleId = roleSelect.value;
    const selectedDepartment = selectedOption.getAttribute('data-department');


    // 送信するデータは EmployeeRegistrationRequest DTOに合わせる
    const data = {
        employeeId: form.employeeId.value,
        name: form.employeeName.value,
        department: selectedDepartment, // 役職名 (employeeテーブルのdepartmentカラム用)
        email: form.email.value,
        password: form.password.value,
        roleId: selectedRoleId // 役割ID (employee_roleテーブル用)
    };

    try {
        const headers = getAuthHeaders();
        // getLoggedInEmployeeId()はオペレーターIDを取得するために必要なため、ここでは重複を避けてJWTヘッダーのみ処理

        if (!headers['Authorization']) {
            // トークンがない場合、getLoggedInEmployeeIdがエラーを投げるはずだが、念のため
            showMessage('❌ 認証情報が不足しています。ログインし直してください。', true);
            return;
        }
        
        // POST /api/employees の呼び出し
        const res = await fetch('/api/employees', {
            method: 'POST',
            headers: { 
                'Content-Type': 'application/json',
                ...headers 
            },
            body: JSON.stringify(data)
        });

        if (res.ok) {
            showMessage('✅ 従業員を正常に登録しました。', false);
            // 成功したら一覧に戻る
            setTimeout(() => {
                window.location.href = 'employee_management.html';
            }, 1500);
        } else {
            const err = await res.json();
            showMessage('❌ 登録失敗: ' + (err.message || '不明なエラーが発生しました。'), true);
        }
    } catch (err) {
        if (err.message.includes("操作を行う従業員IDが見つかりません")) {
            // getLoggedInEmployeeId()から throw されたエラーの場合
            showMessage(`❌ ${err.message} 登録できませんでした。`, true);
        } else {
             console.error(err);
             showMessage('❌ 通信エラー: サーバーに接続できませんでした。', true);
        }
    }
});