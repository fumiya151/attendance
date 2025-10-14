document.addEventListener('DOMContentLoaded', async () => {
    const form = document.getElementById('employee-edit-form');
    const urlParams = new URLSearchParams(window.location.search);
    
    // URLパラメータのキーを 'id' から 'employeeId' に修正済み
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
        
        const roleSelect = form.roleSelect;
        const options = Array.from(roleSelect.options);
        const optionToSelect = options.find(option => option.dataset.department === employee.department);
        if (optionToSelect) {
            optionToSelect.selected = true;
        }

        form.email.value = employee.email || '';
        form.wage.value = employee.wage || '';
        
        // form.querySelector(`input[name="isActive"][value="${String(employee.active)}"]`).checked = true;

    } catch (error) {
        alert(error.message);
        window.location.href = 'employee_management.html';
    }

    // フォーム送信時の処理
    form.addEventListener('submit', (e) => {
        e.preventDefault();

        const selectedOption = form.roleSelect.options[form.roleSelect.selectedIndex];
        
        // 編集後のデータをオブジェクトとしてまとめる
        const editedEmployeeData = {
            // Note: editedData.id は不要だが、前のロジックを踏襲して employeeId を使用
            employeeId: employeeId, // employeeIdを正しく設定
            name: form.name.value,
            department: selectedOption.dataset.department,
            email: form.email.value,
            wage: form.wage.value,
            // ★ 修正: 在職状況（isActive）のロジックを削除し、DBに依存（true）させる
            active: true, 
            roleId: form.roleSelect.value
        };

        // セッションストレージに保存
        sessionStorage.setItem('editedEmployeeData', JSON.stringify(editedEmployeeData));

        // 確認画面へ遷移
        window.location.href = 'edit_employee_confirm.html';
    });
});