// edit_employee.js

// ログイン中の従業員IDをセッションストレージから取得する関数 (共通JSファイルに存在することを前提)
function getLoggedInEmployeeId() {
    const employeeId = sessionStorage.getItem('loggedInEmployeeId'); 
    if (!employeeId) {
        // IDが取れない場合は明確にエラーを投げる
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
    
    // JWTのチェック
    if (!token) {
        alert("認証セッションが無効です。再度ログインしてください。");
        window.location.href = '/html/admin_login.html'; 
        return {};
    }
    
    return {
        'Authorization': `Bearer ${token}`,
        'X-Operator-Id': employeeId 
    };
}


document.addEventListener('DOMContentLoaded', async () => {
    const form = document.getElementById('employee-edit-form');
    const urlParams = new URLSearchParams(window.location.search);
    
    const employeeId = urlParams.get('employeeId');

    if (!employeeId) {
        alert('従業員IDが指定されていません。');
        window.location.href = 'employee_management.html';
        return;
    }

    const headers = getAuthHeaders();
    if (!headers['Authorization']) return; // トークンがない場合は処理を中断

    // 従業員データを取得してフォームに設定
    try {
        const res = await fetch(`/api/employees/${employeeId}`, { headers });
        
        if (!res.ok) {
            const errorText = await res.text();
            if (res.status === 403) {
                 throw new Error('アクセス権限がありません。');
            }
            throw new Error(`従業員情報の取得に失敗しました (Status: ${res.status} / Error: ${errorText.substring(0, 50)}...)`);
        }

        const employee = await res.json();
        
        // ★修正ポイント 1: APIレスポンス (EmployeeDto) から直接値を取得し、デフォルト値 (0) を設定 ★
        const dependentCount = employee.dependentCount !== undefined && employee.dependentCount !== null ? employee.dependentCount : 0;
        const residentTax = employee.monthlyResidentTax !== undefined && employee.monthlyResidentTax !== null ? employee.monthlyResidentTax : 0.0;

        // 取得した元データをセッションストレージに保存
        sessionStorage.setItem('originalEmployeeData', JSON.stringify(employee));

        // フォームに値を設定
        form.employeeId.value = employee.employeeId;
        form.name.value = employee.name;
        
        const roleSelect = form.roleSelect;
        // roleSelect.options は事前にAPIなどで取得・HTMLに設定されている前提
        const options = Array.from(roleSelect.options);
        
        // DTOの 'department' フィールドに、ロール名や部署名が入っていると仮定して選択
        const optionToSelect = options.find(option => option.dataset.department === employee.department);
        if (optionToSelect) {
            optionToSelect.selected = true;
        }

        form.email.value = employee.email || '';
        form.wage.value = employee.wage || '';
        
        // ★追加: 税務情報をフォームに設定（要素が存在することを前提）★
        const dependentCountInput = document.getElementById('dependentCount');
        const residentTaxInput = document.getElementById('residentTax');

        if (dependentCountInput) dependentCountInput.value = dependentCount;
        if (residentTaxInput) residentTaxInput.value = residentTax; // residentTaxはDoubleなのでそのまま

    } catch (error) {
        alert(error.message);
        console.error("Employee fetch error:", error);
        window.location.href = 'employee_management.html';
    }

    // フォーム送信時の処理
    form.addEventListener('submit', (e) => {
        e.preventDefault();

        const selectedOption = form.roleSelect.options[form.roleSelect.selectedIndex];
        
        // ★修正ポイント 2: 編集後のデータに税務情報を追加し、値を正しく処理★
        const dependentCountInput = document.getElementById('dependentCount');
        const residentTaxInput = document.getElementById('residentTax');

        // 数値変換のためのヘルパー関数
        const getIntValue = (element) => element && element.value ? parseInt(element.value) || 0 : 0;
        const getFloatValue = (element) => element && element.value ? parseFloat(element.value) || 0.0 : 0.0;

        const editedEmployeeData = {
            employeeId: employeeId,
            name: form.name.value,
            department: selectedOption.dataset.department, // 役職/部署名
            email: form.email.value,
            wage: form.wage.value, // 時給は文字列として扱う (バックエンドでBigDecimalに変換)
            active: true, 
            roleId: form.roleSelect.value, // 役割ID
            
            // ★追加: 税務情報 - DTOの型に合わせて数値に変換★
            dependentCount: getIntValue(dependentCountInput),
            monthlyResidentTax: getFloatValue(residentTaxInput),
        };

        // セッションストレージに保存
        sessionStorage.setItem('editedEmployeeData', JSON.stringify(editedEmployeeData));

        // 確認画面へ遷移
        window.location.href = 'edit_employee_confirm.html';
    });
});