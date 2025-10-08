function getLoggedInEmployeeId() {
    const employeeId = sessionStorage.getItem('id'); 
    if (!employeeId) {
        throw new Error("操作を行う従業員IDが見つかりません。");
    }
    return employeeId;
}

// 従業員一覧取得＆テーブル描画
async function fetchAndRenderEmployees() {
    try {
        // /api/employees/limited は、Controllerの @GetMapping("/limited") に対応
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
    // 'employee-row' IDを持つ要素を削除
    while ((next = insertPoint.previousSibling) && next && next.id === 'employee-row') {
        tbody.removeChild(next);
    }
    
    list.forEach(emp => {
        const tr = document.createElement('tr');
        tr.id = 'employee-row';
        
        // tr.dataset.id に emp.employeeId を設定
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
        tbody.insertBefore(tr, insertPoint);
    });
}

document.addEventListener('DOMContentLoaded', () => {
    // 初期表示
    fetchAndRenderEmployees();

    // HTMLからselect要素を取得
    const periodSelect = document.getElementById('aggregation-period-select');

    // --- 【期間選択リストの動的生成】 ---
    if (periodSelect) {
        periodSelect.innerHTML = ''; 
        
        const today = new Date();
        const currentYear = today.getFullYear();
        const currentMonth = today.getMonth(); // 0 (1月) から 11 (12月)

        // 過去3ヶ月分のデータを生成（今月, 先月, 先々月）
        for (let i = 0; i < 3; i++) {
            // 月を遡る: Date()コンストラクタは月を修正してくれる
            const targetDate = new Date(currentYear, currentMonth - i, 1);
            
            const year = targetDate.getFullYear();
            const month = targetDate.getMonth() + 1; 

            const monthString = `${year}年${month}月度`;
            
            const option = document.createElement('option');
            // APIに渡しやすく処理しやすい YYYY-MM 形式を値として設定
            const valueString = `${year}-${String(month).padStart(2, '0')}`; 
            
            option.value = valueString;
            option.textContent = monthString;

            // i=0 (今月) をデフォルトで選択状態にする
            if (i === 0) {
                option.selected = true;
            }

            periodSelect.appendChild(option);
        }
    }
    // ------------------------------------------


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
            const editBtn = target.closest('.edit-btn');
            const deleteBtn = target.closest('.delete-btn');

            if (editBtn) {
                const tr = editBtn.closest('tr');
                const employeeId = tr.dataset.id; 
                
                // 編集ページへ遷移。パラメータ名も 'employeeId' に統一
                window.location.href = `edit_employee.html?employeeId=${employeeId}`;
            }

            if (deleteBtn) {
                const tr = deleteBtn.closest('tr');
                const employeeId = tr.dataset.id; 
                
                if (confirm(`従業員コード: ${employeeId} の従業員情報を本当に削除しますか？`)) {
                    try {
                        const operatorId = getLoggedInEmployeeId(); // ★ 修正: ログインIDを取得
                        
                        const res = await fetch(`/api/employees/${employeeId}`, {
                            method: 'DELETE',
                            headers: {
                                // ★ 修正: 削除操作のヘッダーを追加
                                'X-Operator-Id': operatorId
                            }
                        });
                        if (res.ok) {
                            alert('従業員情報を削除しました。');
                            fetchAndRenderEmployees(); // テーブルから行を再描画
                        } else {
                            throw new Error('削除に失敗しました。');
                        }
                    } catch (err) {
                        // getLoggedInEmployeeId()のエラーもここでキャッチされます
                        alert(`処理に失敗しました: ${err.message}`);
                    }
                }
            }
        });
    }

    // ------------------------------------------
    // ★ NEW: 集計実行ボタンのイベントリスナー
    // ------------------------------------------
    const executeAggregationBtn = document.getElementById('execute-aggregation-btn');

    if (executeAggregationBtn && periodSelect) {
        executeAggregationBtn.addEventListener('click', () => {
            // 選択された期間 (value: YYYY-MM) を取得
            const selectedPeriod = periodSelect.value;
            
            // payroll_calculation.htmlへ遷移し、期間をURLパラメータとして渡す
            window.location.href = `payroll_calculation.html?period=${encodeURIComponent(selectedPeriod)}`;
        });
    }
});