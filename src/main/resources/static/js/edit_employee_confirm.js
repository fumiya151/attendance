// ログイン中の従業員IDをセッションストレージから取得する関数 (共通JSファイルに存在することを前提)
function getLoggedInEmployeeId() {
    const employeeId = sessionStorage.getItem('loggedInEmployeeId'); 
    if (!employeeId) {
        // IDが取れない場合は明確にエラーを投げる
        throw new Error("操作を行う従業員IDが見つかりません。ログインが必要です。");
    }
    return employeeId;
}


document.addEventListener('DOMContentLoaded', () => {
    const originalData = JSON.parse(sessionStorage.getItem('originalEmployeeData'));
    const editedData = JSON.parse(sessionStorage.getItem('editedEmployeeData'));
    
    // editedData.employeeId が前の画面で正しく設定されていることを前提とする
    // originalData の active 属性は、編集画面で変更されていないため、ここでは表示ロジックから削除する

    if (!originalData || !editedData) {
        alert('確認データが見つかりません。');
        window.location.href = 'employee_management.html';
        return;
    }

    const tbody = document.getElementById('confirmation-tbody');
    
    const fields = [
        { key: 'name', label: '氏名' },
        { key: 'department', label: '役職（権限）' },
        { key: 'email', label: 'メールアドレス' },
    ];

    // 変更点を比較してテーブルを生成
    fields.forEach(field => {
        
        const originalValue = originalData[field.key];
        const editedValue = editedData[field.key];

        const tr = document.createElement('tr');
        
        // ★ 修正済み: <td>タグの重複を解消（初期値） ★
        let beforeCell = `<td>${originalValue}</td>`;
        let afterCell = `<td>${editedValue}</td>`; 

        if (String(originalData[field.key]) !== String(editedData[field.key])) {
            beforeCell = `<td class="diff-removed">${originalValue}</td>`;
            afterCell = `<td class="diff-added">${editedValue}</td>`;
        }
        
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

    // 「確定」ボタン
    document.getElementById('confirm-btn').addEventListener('click', async () => {
        try {
            // ログインIDを取得（監査ヘッダー用）
            const operatorId = getLoggedInEmployeeId(); 

            // 更新対象ID (employeeId)
            const employeeIdToUpdate = editedData.employeeId; 

            const res = await fetch(`/api/employees/${employeeIdToUpdate}`, {
                method: 'PUT',
                headers: { 
                    'Content-Type': 'application/json',
                    // 監査ヘッダー
                    'X-Operator-Id': operatorId 
                },
                body: JSON.stringify(editedData)
            });

            if (res.ok) {
                alert('従業員情報を更新しました。');
                // ストレージをクリアして一覧画面へ
                sessionStorage.removeItem('originalEmployeeData');
                sessionStorage.removeItem('editedEmployeeData');
                window.location.href = 'employee_management.html';
            } else {
                 // サーバーからのエラーレスポンスを詳細に表示
                 const errorData = await res.json();
                 throw new Error(errorData.message || '更新に失敗しました。');
            }
        } catch (error) {
            // ログインID取得エラーもここでキャッチし、ユーザーに伝達
            alert(`更新処理エラー: ${error.message}`);
        }
    });
});