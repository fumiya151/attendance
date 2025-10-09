// ★ 修正後の getLoggedInEmployeeId 関数 (キーを修正) ★
function getLoggedInEmployeeId() {
    // ログイン成功時に保存した正しいキー 'loggedInEmployeeId' を使用
    const employeeId = sessionStorage.getItem('loggedInEmployeeId'); 
    if (!employeeId) {
        throw new Error("操作を行う従業員IDが見つかりません。");
    }
    return employeeId;
}

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
        // 削除/編集時に必要となるため、データID属性を設定
        tr.dataset.id = emp.employeeId; 
        
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
        
        // 編集ボタン
        tr.querySelector('.edit-btn').addEventListener('click', () => {
            window.location.href = `/html/edit_employee.html?employeeId=${emp.employeeId}`; // パラメータ名を修正
        });
        
        // 削除ボタン
        tr.querySelector('.delete-btn').addEventListener('click', async () => {
            if (confirm(`従業員コード: ${emp.employeeId} を本当に削除しますか？`)) {
                try {
                    const operatorId = getLoggedInEmployeeId(); // ★ 修正点: ログインIDを取得
                    
                    const res = await fetch(`/api/employees/${emp.employeeId}`, {
                        method: 'DELETE',
                        headers: {
                            // ★ 修正点: 必須監査ヘッダーを追加
                            'X-Operator-Id': operatorId
                        }
                    });
                    
                    if (res.ok) {
                        alert('削除しました');
                        fetchAndRenderEmployees();
                    } else {
                        throw new Error('削除失敗');
                    }
                } catch (err) {
                    // 監査ID取得エラーもここでキャッチ
                    alert('処理エラー: ' + err.message);
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
                // Controllerの検索APIを想定 (limit=3は画面の仕様に合わせる必要あり)
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

// 従業員登録モーダル表示・非表示 (省略)

// 従業員登録フォーム送信
const form = document.getElementById('employee-register-form');
// フォームがまだ存在しない場合の処理も考慮し、DOMContentLoaded内に移動するか、イベントリスナーの追加方法を調整すべきだが、元の構造を維持しつつ修正
if(form) {
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        // DTOに合わせるため、データ構造を調整 (roleId, emailなどが不足している可能性あり)
        const data = {
            employeeId: form.employeeId.value,
            name: form.employeeName.value,
            department: form.department.value,
            password: form.password.value
            // TODO: roleId, email, passwordConfirm の処理をこのJSに組み込む必要がある
        };
        
        try {
            const operatorId = getLoggedInEmployeeId(); // ★ 修正点: ログインIDを取得
            
            const res = await fetch('/api/employees', {
                method: 'POST',
                headers: { 
                    'Content-Type': 'application/json',
                    'X-Operator-Id': operatorId // ★ 修正点: 必須監査ヘッダーを追加
                },
                body: JSON.stringify(data)
            });
            
            if (res.ok) {
                alert('登録しました');
                // modal, form.reset() はモーダル処理が必要 (JSの外部で定義されている可能性あり)
                // TODO: モーダル/フォームリセット処理を有効にする
                await fetchAndRenderEmployees(); // テーブル再描画
            } else {
                const err = await res.json();
                alert('登録失敗: ' + (err.message || 'エラー'));
            }
        } catch (err) {
            alert('通信エラー: ' + err.message);
        }
    });
}

// 従業員登録モーダル表示・非表示 (該当DOMがあれば動作)
const openBtn = document.getElementById('open-register-modal');
const closeBtn = document.getElementById('close-register-modal');
const modal = document.getElementById('register-modal');

if(openBtn && modal) {
    openBtn.addEventListener('click', () => {
        modal.style.display = 'block';
    });
    closeBtn.addEventListener('click', () => {
        modal.style.display = 'none';
    });
    window.addEventListener('click', (e) => {
        if (e.target === modal) modal.style.display = 'none';
    });
}