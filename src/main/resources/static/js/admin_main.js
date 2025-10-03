// 従業員一覧取得＆テーブル描画
async function fetchAndRenderEmployees() {
    try {
        const res = await fetch('/api/employees?limit=3');
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

// 検索イベント
document.addEventListener('DOMContentLoaded', () => {
    fetchAndRenderEmployees();
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
});

// 初期表示
window.addEventListener('DOMContentLoaded', fetchAndRenderEmployees);
// ...existing code...
