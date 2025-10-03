// 従業員一覧取得＆テーブル描画
async function fetchAndRenderEmployees() {
    try {
        const res = await fetch('/api/employees');
        if (!res.ok) throw new Error('取得失敗');
        const employees = await res.json();
        const tbody = document.querySelector('#employees .data-table tbody');
        const insertPoint = document.getElementById('employee-insert-point');
        let next;
        while ((next = insertPoint.previousSibling) && next && next.id === 'employee-row') {
            tbody.removeChild(next);
        }
        employees.forEach(emp => {
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
    } catch (err) {
        alert('従業員一覧の取得に失敗しました');
    }
}

// 初期表示
window.addEventListener('DOMContentLoaded', fetchAndRenderEmployees);
