// 従業員一覧取得＆テーブル描画
async function fetchAndRenderEmployees() {
    try {
        const res = await fetch('/api/employees/limited');
        if (!res.ok) throw new Error('取得失敗');
        const employees = await res.json();
        renderEmployeeRows(employees);
    } catch (err) {
        alert('従業員一覧の取得に失敗しました');
    }
}

// 検索・表示用関数
function renderEmployeeRows(list) {
    const tbody = document.querySelector('#employees .data-table tbody');
    const insertPoint = document.getElementById('employee-insert-point');
    // 既存行削除
    let next;
    while ((next = insertPoint.previousSibling) && next && next.id === 'employee-row') {
        tbody.removeChild(next);
    }
    list.forEach(emp => {
        const tr = document.createElement('tr');
        tr.id = 'employee-row';
        tr.dataset.id = emp.id; // 従業員IDをdata属性として保持
        tr.innerHTML = `
            <td>${emp.employeeCode}</td>
            <td>${emp.name}</td>
            <td>${emp.department}</td>
            <td><span class="${emp.active ? 'status-active' : 'status-inactive'}">${emp.active ? '在職中' : '退職'}</span></td>
            <td>
                <button class="small-btn edit-btn"><i class="fas fa-pen"></i> 編集</button>
                <button class="small-btn delete-btn"><i class="fas fa-trash-alt"></i> 削除</button>
            </td>
        `;
        tbody.insertBefore(tr, insertPoint);
    });
}

document.addEventListener('DOMContentLoaded', () => {
    // 初期表示
    fetchAndRenderEmployees();

    // 検索イベント
    const searchBtn = document.getElementById('employee-search-btn');
    const searchInput = document.getElementById('employee-search-input');
    if (searchBtn && searchInput) {
        searchBtn.addEventListener('click', async () => {
            const keyword = searchInput.value.trim();
            try {
                const res = await fetch(`/api/employees?keyword=${encodeURIComponent(keyword)}&limit=3`);
                if (!res.ok) throw new Error('検索失敗');
                const employees = await res.json();
                renderEmployeeRows(employees);
            } catch (err) {
                alert('検索に失敗しました');
            }
        });
        searchInput.addEventListener('keydown', e => {
            if (e.key === 'Enter') searchBtn.click();
        });
    }

    // 編集・削除ボタンのイベントリスナー（イベント委譲）
    const tbody = document.querySelector('#employees .data-table tbody');
    if (tbody) {
        tbody.addEventListener('click', async (event) => {
            const target = event.target;
            // Element.closest() を使って、クリックされた要素がボタンまたはその子要素であるかを確認
            const editBtn = target.closest('.edit-btn');
            const deleteBtn = target.closest('.delete-btn');

            if (editBtn) {
                const tr = editBtn.closest('tr');
                const employeeId = tr.dataset.id;
                // 編集ページへ遷移
                window.location.href = `edit_employee.html?id=${employeeId}`;
            }

            if (deleteBtn) {
                const tr = deleteBtn.closest('tr');
                const employeeId = tr.dataset.id;
                const employeeCode = tr.cells[0].textContent;
                if (confirm(`従業員コード: ${employeeCode} の従業員情報を本当に削除しますか？`)) {
                    try {
                        const res = await fetch(`/api/employees/${employeeId}`, {
                            method: 'DELETE',
                        });
                        if (res.ok) {
                            alert('従業員情報を削除しました。');
                            fetchAndRenderEmployees(); // テーブルから行を削除
                        } else {
                            throw new Error('削除に失敗しました。');
                        }
                    } catch (err) {
                        alert(err.message);
                    }
                }
            }
        });
    }
});