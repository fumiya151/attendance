// edit_employee_confirm.js (確定ボタン、修正ボタン、差分表示の問題を解決した安全版)

// ログイン中の従業員IDをセッションストレージから取得する関数 (共通JSファイルに存在することを前提)
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

    if (!token || !employeeId) {
        // 例外をスローし、呼び出し元でキャッチさせる
        throw new Error("認証セッションが無効です。再度ログインしてください。");
    }

    return {
        'Authorization': `Bearer ${token}`,
        'X-Operator-Id': employeeId
    };
}


document.addEventListener('DOMContentLoaded', () => {
    const originalData = JSON.parse(sessionStorage.getItem('originalEmployeeData')) || {};
    const editedData = JSON.parse(sessionStorage.getItem('editedEmployeeData'));

    if (!editedData || !editedData.employeeId) {
        alert('確認データまたは従業員IDが見つかりません。');
        window.location.href = 'employee_management.html';
        return;
    }
    
    // 確定ボタンの問題を解決: API呼び出し変数の定義
    const employeeId = editedData.employeeId;
    const url = `/api/employees/${employeeId}`;
    const method = 'PUT'; // 更新
    const dataToSend = editedData; // EmployeeDtoとして送信
    const successMessage = '従業員情報を更新しました。';

    const tbody = document.getElementById('confirmation-tbody');

    const fields = [
        { key: 'name', label: '氏名', isNumeric: false },
        { key: 'department', label: '役職（権限）', isNumeric: false },
        { key: 'email', label: 'メールアドレス', isNumeric: false },
        { key: 'wage', label: '時給', isNumeric: true },
        { key: 'dependentCount', label: '扶養人数', isNumeric: true },
        { key: 'monthlyResidentTax', label: '住民税（月額）', isNumeric: true }
    ];

    // 変更点を比較してテーブルを生成
    fields.forEach(field => {
        
        // 1. 値の取得と標準化 (null/undefined -> 0 or '')
        // rawValue: セッションストレージから取得した生の値 (null, "1000", 0 など)
        const rawOriginalValue = originalData[field.key];
        const rawEditedValue = editedData[field.key];

        // 2. 比較用の値 (null/undefined/'' を標準化して比較)
        // 数値項目の比較は、null/undefined/'' を 0 として扱う
        let compOriginal;
        let compEdited;

        if (field.isNumeric) {
            // 数値項目: null/undefined/'' は 0 として扱う
            compOriginal = parseFloat(rawOriginalValue || 0);
            compEdited = parseFloat(rawEditedValue || 0);
        } else {
            // 文字列項目: null/undefined を空文字として扱う
            compOriginal = String(rawOriginalValue || '');
            compEdited = String(rawEditedValue || '');
        }

        // 3. 差分チェック
        // 値と型が完全に一致するか、または文字列化して比較して変更がないか
        const isDifferent = String(compOriginal) !== String(compEdited); 

        // 4. 表示用の値の整形
        let formattedOriginal;
        let formattedEdited;

        if (field.isNumeric) {
            // 数値項目
            const numOriginal = parseFloat(rawOriginalValue);
            const numEdited = parseFloat(rawEditedValue);
            
            // 0または数値でない場合はハイフン表示 ('-' 表示を適用)
            if (field.key === 'wage' || field.key === 'monthlyResidentTax') {
                 formattedOriginal = (isNaN(numOriginal) || numOriginal === 0) ? '-' : numOriginal.toLocaleString() + '円';
                 formattedEdited = (isNaN(numEdited) || numEdited === 0) ? '-' : numEdited.toLocaleString() + '円';
            } else {
                 // 扶養人数 (0の場合は 0 を表示)
                 formattedOriginal = (isNaN(numOriginal) || numOriginal === null) ? 0 : numOriginal;
                 formattedEdited = (isNaN(numEdited) || numEdited === null) ? 0 : numEdited;
            }
        } else {
            // 文字列項目
            formattedOriginal = String(rawOriginalValue || '') === '' ? '-' : String(rawOriginalValue);
            formattedEdited = String(rawEditedValue || '') === '' ? '-' : String(rawEditedValue);
        }
        
        // 5. テーブルセルとクラスの適用
        let beforeCell = `<td>${formattedOriginal}</td>`;
        let afterCell = `<td>${formattedEdited}</td>`;

        if (isDifferent) {
            beforeCell = `<td class="diff-removed">${formattedOriginal}</td>`;
            afterCell = `<td class="diff-added">${formattedEdited}</td>`;
        }
        
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <th>${field.label}</th>
            ${beforeCell}
            ${afterCell}
        `;
        tbody.appendChild(tr);
    });

    // 「修正に戻る」ボタン
    document.getElementById('back-btn').addEventListener('click', () => {
        window.history.back();
    });

    // 「確定」ボタンのロジック
    document.getElementById('confirm-btn').addEventListener('click', async () => {
        const confirmBtn = document.getElementById('confirm-btn');
        confirmBtn.disabled = true;

        try {
            const authHeaders = getAuthHeaders();
            // APIの呼び出し - 定義された url, method, dataToSend, successMessage を使用
            const res = await fetch(url, {
                method: method,
                headers: {
                    'Content-Type': 'application/json',
                    ...authHeaders
                },
                body: JSON.stringify(dataToSend)
            });

            if (res.ok) {
                alert(successMessage);
                sessionStorage.removeItem('originalEmployeeData');
                sessionStorage.removeItem('editedEmployeeData');
                window.location.href = 'employee_management.html';
            } else {
                 const errorData = await res.json();
                 throw new Error(errorData.message || `処理に失敗しました (Status: ${res.status})`);
            }
        } catch (error) {
            alert(`処理エラー: ${error.message}`);
            console.error("Employee process error:", error);

            if (error.message.includes("認証セッションが無効です")) {
                window.location.href = '/html/admin_login.html';
            }
        } finally {
             confirmBtn.disabled = false;
        }
    });
});