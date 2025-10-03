// 新規従業員登録フォーム送信
const form = document.getElementById('employee-register-form');
form.addEventListener('submit', async (e) => {
    e.preventDefault();
    // パスワード一致チェック
    if (form.password.value !== form.passwordConfirm.value) {
        alert('パスワードが一致しません');
        form.password.value = '';
        form.passwordConfirm.value = '';
        form.password.focus();
        return;
    }
    const data = {
        employeeCode: form.employeeCode.value,
        name: form.employeeName.value,
        department: form.department.value,
        email: form.email.value,
        password: form.password.value
    };
    try {
        const res = await fetch('/api/employees', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        if (res.ok) {
            alert('登録しました');
            window.location.href = 'employee_management.html';
        } else {
            const err = await res.json();
            alert('登録失敗: ' + (err.message || 'エラー'));
        }
    } catch (err) {
        alert('通信エラー');
    }
});
