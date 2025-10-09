function getLoggedInEmployeeId() {
    const employeeId = sessionStorage.getItem('loggedInEmployeeId'); 
    if (!employeeId) {
        throw new Error("操作を行う従業員IDが見つかりません。ログインが必要です。");
    }
    return employeeId;
}

// 従業員一覧取得＆テーブル描画
async function fetchAndRenderEmployees() {
    try {
        // Controllerの/limitedエンドポイントを使用 (Service側で既にアクティブフィルタリング済み)
        const res = await fetch('/api/employees/limited');
        if (!res.ok) throw new Error('取得失敗');
        const employees = await res.json();
        
        // Serviceからアクティブな従業員のみが返される前提で、そのまま描画する
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
        tr.dataset.id = emp.employeeId; 
        
        tr.innerHTML = `
            <td>${emp.employeeId || '---'}</td>
            <td>${emp.name || '---'}</td>
            <td>${emp.department || '---'}</td>
            <td>${emp.email || '---'}</td>
            <td>
                <button class="small-btn edit-btn"><i class="fas fa-pen"></i> 編集</button>
                <button class="small-btn delete-btn"><i class="fas fa-trash-alt"></i> 退職処理</button>
            </td>
        `;
        
        // 編集ボタン
        tr.querySelector('.edit-btn').addEventListener('click', () => {
            window.location.href = `edit_employee.html?employeeId=${emp.employeeId}`;
        });
        
        // 削除ボタン (論理削除/退職処理)
        tr.querySelector('.delete-btn').addEventListener('click', async (event) => {
            const deleteBtn = event.target.closest('.delete-btn');
            if (!deleteBtn) return; 

            const tr = deleteBtn.closest('tr');
            const employeeId = tr.dataset.id;
            
            if (confirm(`従業員コード: ${employeeId} の従業員を本当に退職処理（論理削除）しますか？`)) {
                try {
                    const operatorId = getLoggedInEmployeeId();
                    
                    const res = await fetch(`/api/employees/${employeeId}`, {
                        method: 'DELETE', 
                        headers: {
                            'X-Operator-Id': operatorId
                        }
                    });
                    
                    if (res.ok) {
                        alert('従業員の退職処理（論理削除）が完了しました。');
                        fetchAndRenderEmployees(); 
                    } else {
                        const errorData = await res.json();
                        throw new Error(errorData.message || '退職処理に失敗しました。');
                    }
                } catch (err) {
                    alert(`処理エラー: ${err.message}`);
                }
            }
        });
        tbody.insertBefore(tr, insertPoint);
    });
}

document.addEventListener('DOMContentLoaded', () => {
    // 初期表示
    fetchAndRenderEmployees();

    const periodSelect = document.getElementById('aggregation-period-select');

    // --- 期間選択リストの動的生成 ---
    if (periodSelect) {
        periodSelect.innerHTML = ''; 
        
        const today = new Date();
        const currentYear = today.getFullYear();
        const currentMonth = today.getMonth();

        // 過去3ヶ月分のデータを生成
        for (let i = 0; i < 3; i++) {
            const targetDate = new Date(currentYear, currentMonth - i, 1);
            
            const year = targetDate.getFullYear();
            const month = targetDate.getMonth() + 1; 

            const monthString = `${year}年${month}月度`;
            
            const option = document.createElement('option');
            const valueString = `${year}-${String(month).padStart(2, '0')}`; 
            
            option.value = valueString;
            option.textContent = monthString;

            if (i === 0) {
                option.selected = true;
            }

            periodSelect.appendChild(option);
        }
    }


    // 検索イベント
    const searchBtn = document.getElementById('employee-search-btn');
    const searchInput = document.getElementById('employee-search-input');
    if (searchBtn && searchInput) {
        searchBtn.addEventListener('click', async () => {
            const keyword = searchInput.value.trim();
            try {
                // 検索API呼び出し。キーワードと3件制限を渡す
                const res = await fetch(`/api/employees?keyword=${encodeURIComponent(keyword)}&limit=3`);
                if (!res.ok) throw new Error('検索失敗');
                const employees = await res.json();
                
                // Service側でアクティブフィルタリングされているが、念のため論理削除後の表示に備える
                const activeEmployees = employees.filter(emp => emp.active === true);
                renderEmployeeRows(activeEmployees);
            } catch (err) {
                alert('検索に失敗しました');
            }
        });
        searchInput.addEventListener('keydown', e => {
            if (e.key === 'Enter') searchBtn.click();
        });
    }


    // ------------------------------------------
    // 集計実行ボタンのイベントリスナー
    // ------------------------------------------
    const executeAggregationBtn = document.getElementById('execute-aggregation-btn');

    if (executeAggregationBtn && periodSelect) {
        executeAggregationBtn.addEventListener('click', () => {
            const selectedPeriod = periodSelect.value;
            window.location.href = `payroll_calculation.html?period=${encodeURIComponent(selectedPeriod)}`;
        });
    }
});