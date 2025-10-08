document.addEventListener('DOMContentLoaded', () => {
    const originalData = JSON.parse(sessionStorage.getItem('originalEmployeeData'));
    const editedData = JSON.parse(sessionStorage.getItem('editedEmployeeData'));

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
        { key: 'active', label: '在職状況' }
    ];

    // 変更点を比較してテーブルを生成
    fields.forEach(field => {
        const originalValue = field.key === 'active' ? (originalData[field.key] ? '在職中' : '退職') : originalData[field.key];
        const editedValue = field.key === 'active' ? (editedData[field.key] ? '在職中' : '退職') : editedData[field.key];

        const tr = document.createElement('tr');
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
            const res = await fetch(`/api/employees/${editedData.id}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(editedData)
            });

            if (res.ok) {
                alert('従業員情報を更新しました。');
                // ストレージをクリアして一覧画面へ
                sessionStorage.removeItem('originalEmployeeData');
                sessionStorage.removeItem('editedEmployeeData');
                window.location.href = 'employee_management.html';
            } else {
                throw new Error('更新に失敗しました。');
            }
        } catch (error) {
            alert(error.message);
        }
    });
});
