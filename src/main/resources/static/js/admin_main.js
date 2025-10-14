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
    // insertPointの前にあるデータ行をすべて削除します。
    while (insertPoint.previousElementSibling && insertPoint.previousElementSibling.tagName === 'TR') {
        tbody.removeChild(insertPoint.previousElementSibling);
    }
    
    list.forEach(emp => {
        const tr = document.createElement('tr');
        tr.className = 'employee-row-data'; // IDの代わりにクラスを使用
        tr.dataset.id = emp.employeeId; 
        
        tr.innerHTML = `
            <td>${emp.employeeId || '---'}</td>
            <td>${emp.name || '---'}</td>
            <td>${emp.department || '---'}</td>
            <td>${emp.email || '---'}</td>
            <td>
                <button class="small-btn edit-btn"><i class="fas fa-pen"></i> 編集</button>
                <button class="small-btn delete-btn"><i class="fas fa-trash-alt"></i> 削除</button>
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
            
            if (confirm(`従業員コード: ${employeeId} の従業員を本当に削除しますか？`)) {
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

async function fetchMonthlySummary() {
    const totalWorkHoursEl = document.getElementById('total-work-hours');
    const averageOvertimeEl = document.getElementById('average-overtime');

    try {
        const today = new Date();
        const year = today.getFullYear();
        const month = String(today.getMonth() + 1).padStart(2, '0');
        const yearMonth = `${year}-${month}`;

        const res = await fetch(`/api/summaries/monthly?yearMonth=${yearMonth}`);
        if (!res.ok) throw new Error('月次サマリーの取得に失敗しました');
        
        const summary = await res.json();

        totalWorkHoursEl.textContent = `${summary.totalWorkHours.toFixed(1)} 時間`;
        averageOvertimeEl.textContent = `${summary.averageOvertimeHours.toFixed(1)} 時間`;

    } catch (err) {
        console.error(err);
        totalWorkHoursEl.textContent = '- 時間';
        averageOvertimeEl.textContent = '- 時間';
    }
}

document.addEventListener('DOMContentLoaded', () => {
    // 初期表示
    fetchAndRenderEmployees();
    fetchMonthlySummary();
    
    // ★ 修正点: ページロード時に集計データを取得＆表示する処理を追加 ★
    fetchAndRenderAttendanceSummary(); 

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
    // 集計実行ボタンのイベントリスナー (デバッグ/手動実行用)
    // ------------------------------------------
    const executeAggregationBtn = document.getElementById('execute-aggregation-btn');
    const periodSelectCheck = document.getElementById('aggregation-period-select');

    if (executeAggregationBtn && periodSelectCheck) {
        console.log("INFO: (手動)集計実行ボタンのイベントリスナーを登録しました。"); 
        
        executeAggregationBtn.addEventListener('click', () => {
            console.log("ACTION: (手動)集計実行ボタンがクリックされました。");
            fetchAndRenderAttendanceSummary();
        });
    } else {
        console.error("ERROR: 集計実行ボタン (execute-aggregation-btn) または期間選択 (aggregation-period-select) 要素が見つかりません。HTML IDを確認してください。");
    }
});

// --- ★ 修正・確認箇所: fetchAndRenderAttendanceSummary 関数 (エラーハンドリング強化) ★ ---
async function fetchAndRenderAttendanceSummary() {
    const periodSelect = document.getElementById('aggregation-period-select');
    // 初期表示時は、動的に生成されたオプションの中から選択されている最初の値を取得
    const selectedPeriod = periodSelect.value; // "YYYY-MM"
    
    // YYYY-MM形式でない値が選択された場合に備えてチェック
    if (!selectedPeriod || selectedPeriod.split('-').length !== 2) {
        console.error('ERROR: 有効な集計期間が選択されていません。');
        return;
    }

    const [year, month] = selectedPeriod.split('-').map(Number);
    const startDate = new Date(year, month - 1, 1);
    const endDate = new Date(year, month, 0); // 月の最終日

    const formattedStartDate = startDate.toISOString().split('T')[0];
    const formattedEndDate = endDate.toISOString().split('T')[0];

    try {
        console.log(`API呼び出し: /api/attendance/summary?startDate=${formattedStartDate}&endDate=${formattedEndDate}`);
        const res = await fetch(`/api/attendance/summary?startDate=${formattedStartDate}&endDate=${formattedEndDate}`);
        
        if (!res.ok) {
            // API呼び出しが失敗した場合、詳細なエラーメッセージを表示
            const errorText = await res.text();
            console.error('APIエラーレスポンス:', errorText);
            throw new Error(`勤怠概要データの取得に失敗しました (ステータス: ${res.status})`);
        }
        
        const summaries = await res.json();
        renderAttendanceSummaryTable(summaries);
    } catch (err) {
        console.error('集計データの取得または描画中にエラーが発生しました:', err);
        // エラー発生時はテーブルをクリアしてメッセージを表示
        const tbody = document.getElementById('payroll-results-body');
        tbody.innerHTML = '<tr><td colspan="5" style="text-align: center; color: red;">データの取得に失敗しました。コンソールを確認してください。</td></tr>';
        
        // alertは初期ロード時には邪魔になる可能性があるため、ここではコンソールに留めます
        // alert(`処理エラー: ${err.message}`);
    }
}

// --- ★ 修正・確認箇所: renderAttendanceSummaryTable 関数 (互換性・デバッグログ強化) ★ ---
function renderAttendanceSummaryTable(summaries) {
    const tbody = document.getElementById('payroll-results-body');
    tbody.innerHTML = ''; // 既存の行をクリア

    console.log("集計結果データ:", summaries); // 取得したデータの中身を確認

    if (!Array.isArray(summaries) || summaries.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" style="text-align: center;">対象期間の集計データがありません。</td></tr>';
        return;
    }

    summaries.forEach(p => {
        const tr = document.createElement('tr');
        // Nullish Coalescing (??) の代わりに論理OR (||) を使用し、値がない場合は 0 をデフォルトにする
        const totalHours = (p.totalHours || 0).toFixed(2);
        const overtimeHours = (p.overtimeHours || 0).toFixed(2);
        const lateNightHours = (p.lateNightHours || 0).toFixed(2);

        tr.innerHTML = `
            <td>${p.employeeName || '---'}</td>
            <td>${totalHours}</td>
            <td>${overtimeHours}</td>
            <td>${lateNightHours}</td>
            <td><button class="small-btn export-btn" data-employee-id="${p.employeeId}">明細出力</button></td>
        `;
        tbody.appendChild(tr);
    });

    console.log(`集計結果テーブルに ${summaries.length} 件のデータを描画しました。`);
}