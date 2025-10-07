// 従業員一覧取得＆テーブル描画
async function fetchAndRenderEmployees() {
    try {
        const res = await fetch('/api/employees');
        if (!res.ok) throw new Error('取得失敗');
        const employees = await res.json();
        renderEmployeeRows(employees);
    } catch (err) {
        alert('従業員一覧の取得に失敗しました');
    }
}

// 表示用関数
function renderEmployeeRows(list) {
    const tbody = document.querySelector('.data-table tbody');
    tbody.innerHTML = '';
    list.forEach(emp => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>${emp.employeeId}</td>
            <td>${emp.name}</td>
            <td>${emp.department}</td>
            <td><span class="${emp.active ? 'status-active' : 'status-inactive'}">${emp.active ? '在職中' : '退職'}</span></td>
            <td>
                <button class="small-btn edit-btn"><i class="fas fa-pen"></i> 編集</button>
                <button class="small-btn delete-btn"><i class="fas fa-trash-alt"></i> 削除</button>
            </td>
        `;
        tr.querySelector('.edit-btn').addEventListener('click', () => {
            window.location.href = `/html/edit_employee.html?id=${emp.employeeId}`;
        });
        // 削除ボタン
        tr.querySelector('.delete-btn').addEventListener('click', async () => {
            if (confirm('本当に削除しますか？')) {
                try {
                    const res = await fetch(`/api/employees/${emp.employeeId}`, {
                        method: 'DELETE'
                    });
                    if (res.ok) {
                        alert('削除しました');
                        fetchAndRenderEmployees();
                    } else {
                        alert('削除失敗');
                    }
                } catch {
                    alert('通信エラー');
                }
            }
        });
        tbody.appendChild(tr);
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
                const res = await fetch(`/api/employees?keyword=${encodeURIComponent(keyword)}`);
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

// 登録後にテーブル再描画
form.addEventListener('submit', async (e) => {
    // ...既存の登録処理...
    if (res.ok) {
        alert('登録しました');
        modal.style.display = 'none';
        form.reset();
        await fetchAndRenderEmployees(); // 追加: 登録後に一覧再取得
    }
    // ...既存のエラー処理...
});
// 従業員登録モーダル表示・非表示
const openBtn = document.getElementById('open-register-modal');
const closeBtn = document.getElementById('close-register-modal');
const modal = document.getElementById('register-modal');

openBtn.addEventListener('click', () => {
    modal.style.display = 'block';
});
closeBtn.addEventListener('click', () => {
    modal.style.display = 'none';
});
window.addEventListener('click', (e) => {
    if (e.target === modal) modal.style.display = 'none';
});

// 従業員登録フォーム送信
const form = document.getElementById('employee-register-form');
form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const data = {
        employeeId: form.employeeId.value,
        name: form.employeeName.value,
        department: form.department.value,
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
            modal.style.display = 'none';
            form.reset();
            // TODO: テーブル再描画処理
        } else {
            const err = await res.json();
            alert('登録失敗: ' + (err.message || 'エラー'));
        }
    } catch (err) {
        alert('通信エラー');
    }
});
// ...existing code...
