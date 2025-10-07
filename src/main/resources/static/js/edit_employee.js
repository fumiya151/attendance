document.addEventListener('DOMContentLoaded', async () => {
    const form = document.getElementById('employee-edit-form');
    const urlParams = new URLSearchParams(window.location.search);
    const employeeId = urlParams.get('employeeId');

    if (!employeeId) {
        alert('従業員IDが指定されていません。');
        window.location.href = 'employee_management.html';
        return;
    }

    // 従業員データを取得してフォームに設定
    try {
        const res = await fetch(`/api/employees/${employeeId}`);
        if (!res.ok) throw new Error('従業員情報の取得に失敗しました。');

        const employee = await res.json();

        // 取得した元データをセッションストレージに保存
        sessionStorage.setItem('originalEmployeeData', JSON.stringify(employee));

        // フォームに値を設定
        form.employeeId.value = employee.employeeId;
        form.name.value = employee.name;
        form.department.value = employee.department;
        form.email.value = employee.email || '';
        form.querySelector(`input[name="isActive"][value="${String(employee.active)}"]`).checked = true;

    } catch (error) {
        alert(error.message);
        window.location.href = 'employee_management.html';
    }

    // フォーム送信時の処理
    form.addEventListener('submit', (e) => {
        e.preventDefault();

        // 編集後のデータをオブジェクトとしてまとめる
        const editedEmployeeData = {
            id: employeeId,
            employeeId: form.employeeId.value,
            name: form.name.value,
            department: form.department.value,
            email: form.email.value,
            active: form.querySelector('input[name="isActive"]:checked').value === 'true'
        };

        // セッションストレージに保存
        sessionStorage.setItem('editedEmployeeData', JSON.stringify(editedEmployeeData));

        // 確認画面へ遷移
        window.location.href = 'edit_employee_confirm.html';
    });
});
