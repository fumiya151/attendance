// 汎用メッセージ表示関数 (alertの代替)
function showMessage(message, isError = false) {
    const messageBox = document.getElementById('message-box');
    messageBox.textContent = message;
    messageBox.style.display = 'block';
    messageBox.className = isError ? 'is-error' : 'is-success';
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
        const res = await fetch('/api/employees', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
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
        console.error(err);
        showMessage('❌ 通信エラー: サーバーに接続できませんでした。', true);
    }
});
