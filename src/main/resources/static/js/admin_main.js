// ログイン中の従業員IDをセッションストレージから取得する関数 (共通JSファイルに存在することを前提)
function getLoggedInEmployeeId() {
    return sessionStorage.getItem('loggedInEmployeeId'); 
}

/**
 * ログインユーザーのロールを取得する。
 * @returns {string} ロールコード (例: 'ADMIN', 'MGR', 'EMP')
 */
function getLoggedInUserRole() {
    // ログイン時に保存したロールコードを取得
    const role = sessionStorage.getItem('loggedInUserRole'); 
    return role ? role.toUpperCase() : 'UNKNOWN'; 
}

// 取得した全サマリーを保持するためのグローバル変数
let allSummaries = []; 
// 現在テーブルに表示されている（フィルタリング後の）サマリーを保持する変数
let currentDisplayedSummaries = []; 

// --- JWTトークンとオペレーターIDを取得するヘルパー関数 (修正箇所) ---
function getAuthHeaders() {
    const token = sessionStorage.getItem('token');
    const employeeId = getLoggedInEmployeeId(); 
    
    if (!token || !employeeId) {
        // トークンまたはIDがない場合、再ログインを促す
        alert("認証セッションが無効です。再度ログインしてください。");
        window.location.href = 'admin_login.html';
        return {};
    }
    
    // Authorization ヘッダーと X-Operator-Id ヘッダーの両方を含める
    return {
        'Authorization': `Bearer ${token}`,
        'X-Operator-Id': employeeId 
    };
}


// -------------------------------------------------------------
// 【従業員マスタ関連関数】
// -------------------------------------------------------------

// 従業員一覧取得＆テーブル描画
async function fetchAndRenderEmployees() {
    const headers = getAuthHeaders();
    if (!headers['Authorization']) return;

    try {
        const res = await fetch('/api/employees/limited', { headers }); 
        
        if (!res.ok) {
            let errorMessage = `従業員一覧の取得に失敗しました (ステータス: ${res.status})`;
            if (res.status === 403) {
                // 権限エラーの場合
                errorMessage = 'アクセス権限がありません (ADMIN/MGRロールが必要です)。';
            } else {
                // その他のエラーの場合、詳細なエラーメッセージを取得
                const errorText = await res.text();
                errorMessage = `取得失敗: ${errorText}`;
            }
            
            alert(errorMessage);
            throw new Error(errorMessage);
        }
        
        const employees = await res.json();
        renderEmployeeRows(employees);
    } catch (err) {
        // alertはif (!res.ok)ブロック内ですでに行われている
        console.error(err);
    }
}

// 検索・表示用関数 (従業員マスタ用)
function renderEmployeeRows(list) {
    const tbody = document.querySelector('#employees .data-table tbody');
    const insertPoint = document.getElementById('employee-insert-point');
    
    tbody.querySelectorAll('.employee-row-data').forEach(row => {
        tbody.removeChild(row);
    });
    
    list.forEach(emp => {
        const tr = document.createElement('tr');
        tr.className = 'employee-row-data'; 
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
        
        tr.querySelector('.edit-btn').addEventListener('click', () => {
            window.location.href = `edit_employee.html?employeeId=${emp.employeeId}`;
        });
        
        tr.querySelector('.delete-btn').addEventListener('click', async (event) => {
            const deleteBtn = event.target.closest('.delete-btn');
            if (!deleteBtn) return; 

            const tr = deleteBtn.closest('tr');
            const employeeId = tr.dataset.id;
            
            if (confirm(`従業員コード: ${employeeId} の従業員を本当に削除しますか？`)) {
                try {
                    // getAuthHeaders()内で認証チェックが行われるため、operatorIdの取得は不要
                    
                    const res = await fetch(`/api/employees/${employeeId}`, {
                        method: 'DELETE', 
                        headers: getAuthHeaders()
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

        if (insertPoint) {
            tbody.insertBefore(tr, insertPoint);
        } else {
            tbody.appendChild(tr);
        }
    });
}

// 月次サマリー概要（トップカード用）
async function fetchMonthlySummary() {
    const headers = getAuthHeaders();
    if (!headers['Authorization']) return;

    const totalWorkHoursEl = document.getElementById('total-work-hours');
    const averageOvertimeEl = document.getElementById('average-overtime');

    try {
        const today = new Date();
        const year = today.getFullYear();
        const month = String(today.getMonth() + 1).padStart(2, '0');
        const yearMonth = `${year}-${month}`;

        const res = await fetch(`/api/summaries/monthly?yearMonth=${yearMonth}`, { headers });
        
        if (!res.ok) {
            let errorMessage;
            if (res.status === 403) {
                errorMessage = 'アクセス権限がありません。';
            } else {
                errorMessage = '月次サマリーの取得に失敗しました';
            }
            // 権限エラーの場合のみアラートを表示
            if (res.status === 403) alert(errorMessage); 
            throw new Error(errorMessage);
        }
        
        const summary = await res.json();

        totalWorkHoursEl.textContent = `${summary.totalWorkHours.toFixed(1)} 時間`;
        averageOvertimeEl.textContent = `${summary.averageOvertimeHours.toFixed(1)} 時間`;

    } catch (err) {
        console.error(err);
        totalWorkHoursEl.textContent = '- 時間';
        averageOvertimeEl.textContent = '- 時間';
    }
}

// -------------------------------------------------------------
// 【給与計算セクション】 集計結果テーブル描画
// -------------------------------------------------------------

async function fetchAndRenderAttendanceSummary() {
    const headers = getAuthHeaders();
    if (!headers['Authorization']) return;

    const periodSelect = document.getElementById('aggregation-period-select');
    
    // プルダウンのオプションが生成されているか確認し、デフォルト期間を使用
    let selectedPeriod;
    if (periodSelect && periodSelect.options.length > 0) {
        selectedPeriod = periodSelect.value;
    } else {
        // オプションがない場合は今月をデフォルトとする
        const today = new Date();
        const year = today.getFullYear();
        const month = today.getMonth() + 1;
        selectedPeriod = `${year}-${String(month).padStart(2, '0')}`;
    }

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
        console.log(`API呼び出し(給与): /api/attendance/summary?startDate=${formattedStartDate}&endDate=${formattedEndDate}`);
        const res = await fetch(`/api/attendance/summary?startDate=${formattedStartDate}&endDate=${formattedEndDate}`, { headers });
        
        if (!res.ok) {
            let errorMessage;
            if (res.status === 403) {
                errorMessage = 'アクセス権限がありません。';
            } else {
                const errorText = await res.text();
                console.error('APIエラーレスポンス:', errorText);
                errorMessage = `勤怠概要データの取得に失敗しました (ステータス: ${res.status})`;
            }
            if (res.status === 403) alert(errorMessage);
            throw new Error(errorMessage);
        }
        
        const summaries = await res.json();
        renderAttendanceSummaryTable(summaries);
    } catch (err) {
        console.error('集計データの取得または描画中にエラーが発生しました:', err);
        const tbody = document.getElementById('payroll-results-body');
        tbody.innerHTML = '<tr><td colspan="5" style="text-align: center; color: red;">データの取得に失敗しました。コンソールを確認してください。</td></tr>';
    }
}

function renderAttendanceSummaryTable(summaries) {
    const tbody = document.getElementById('payroll-results-body');
    tbody.innerHTML = ''; 

    console.log("給与計算集計結果データ:", summaries); 

    if (!Array.isArray(summaries) || summaries.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" style="text-align: center;">対象期間の集計データがありません。</td></tr>';
        return;
    }

    summaries.forEach(p => {
        const tr = document.createElement('tr');
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


// -------------------------------------------------------------
// 【勤怠セクション】 日次勤怠ログの表示とフィルタリング
// -------------------------------------------------------------

async function fetchAndDisplaySummaries() {
    const headers = getAuthHeaders();
    if (!headers['Authorization']) return;

    try {
        const response = await fetch('/api/summaries/limitsummaries', { headers }); 
        
        if (!response.ok) {
            let errorMessage;
            if (response.status === 403) {
                errorMessage = 'アクセス権限がありません。';
                alert(errorMessage);
            } else {
                const errorText = await response.text();
                console.error('APIエラーレスポンス(勤怠ログ):', errorText);
                errorMessage = `勤怠サマリーの取得に失敗しました (ステータス: ${response.status})。`;
            }
            throw new Error(errorMessage);
        }
        
        const summaries = await response.json(); 

        allSummaries = summaries; // 全データをグローバル変数に保存
        
        // プルダウンメニューを初期化/描画
        populateSearchDropdowns(summaries);

        // フィルタリングなしで初期表示
        applyFiltersAndRenderTable(allSummaries); 

    } catch (error) {
        console.error('エラー(勤怠ログ):', error);
        // 描画先の tbody を明示的に指定し、colspanを9に修正
        const tableBody = document.querySelector('#attendance-logs .data-table tbody');
        const COL_SPAN = 9; 
        if (tableBody) {
            tableBody.innerHTML = `<tr><td colspan="${COL_SPAN}" style="color: red; text-align: center;">エラー: ${error.message}</td></tr>`;
        }
    }
}

/**
 * 取得したサマリーデータに基づき、検索プルダウンを生成する
 */
function populateSearchDropdowns(summaries) {
    const employeeDropdown = document.getElementById('search-employee');
    const monthDropdown = document.getElementById('search-month');

    if (!employeeDropdown || !monthDropdown) return; // 要素が存在しない場合はスキップ

    // 1. 従業員名のリストを生成 (Setで重複を除去)
    const employeeNames = [...new Set(summaries.map(s => s.employeeName))].sort();
    
    // 2. 対象月のリストを生成 (YYYY-MM形式, Setで重複を除去し、降順にソート)
    const months = [...new Set(summaries.map(s => s.workDate ? s.workDate.substring(0, 7) : null).filter(m => m !== null))].sort((a, b) => b.localeCompare(a));

    // ドロップダウンを初期化 (初期オプションを残すため、既存のものを除去)
    employeeDropdown.innerHTML = '<option value="">--- 全従業員 ---</option>';
    monthDropdown.innerHTML = '<option value="">--- 全期間 ---</option>';

    // 従業員名オプションの追加
    employeeNames.forEach(name => {
        const option = document.createElement('option');
        option.value = name; 
        option.textContent = name;
        employeeDropdown.appendChild(option);
    });

    // 対象月オプションの追加
    months.forEach(month => {
        const option = document.createElement('option');
        const displayMonth = month.replace('-', '年') + '月'; 
        option.value = month; 
        option.textContent = displayMonth;
        monthDropdown.appendChild(option);
    });
}


/**
 * 検索処理を実行する関数 (プルダウン対応)
 */
function handleSearch() {
    // 検索プルダウンから値を取得
    const selectedMonth = document.getElementById('search-month').value;
    const selectedEmployee = document.getElementById('search-employee').value;
    
    // 全データからフィルタリング
    const filteredSummaries = allSummaries.filter(summary => {
        let isMatch = true;
        
        // 1. 月によるフィルタリング
        if (selectedMonth) {
            const summaryMonth = summary.workDate ? summary.workDate.substring(0, 7) : '---';
            if (summaryMonth !== selectedMonth) {
                isMatch = false;
            }
        }
        
        // 2. 従業員名によるフィルタリング
        if (isMatch && selectedEmployee) {
            if (summary.employeeName !== selectedEmployee) {
                isMatch = false;
            }
        }
        
        return isMatch;
    });

    applyFiltersAndRenderTable(filteredSummaries);
}


/**
 * フィルタリングされたサマリーデータをテーブルに描画する関数 (勤怠ログ用)
 */
function applyFiltersAndRenderTable(summaries) {
    // 描画に使用するサマリーをグローバル変数に保存
    currentDisplayedSummaries = summaries; 
    
    // 描画先の tbody を正確に取得（#attendance-logsセクション内の.data-tableのtbody）
    const tableBody = document.querySelector('#attendance-logs .data-table tbody');
    if (!tableBody) {
        console.error("ERROR: 勤怠ログテーブルの tbody 要素が見つかりません。");
        return;
    }
    tableBody.innerHTML = ''; // テーブルをクリア

    // HTMLの列数（9列）に合わせて colspan を設定
    const COL_SPAN = 9; 
    
    if (summaries.length === 0) {
        tableBody.innerHTML = `<tr><td colspan="${COL_SPAN}" style="text-align: center;">該当する勤怠サマリーがありません。</td></tr>`;
        return;
    }
    
    // 時刻整形ヘルパー関数を定義
    const formatTime = (timeString) => {
        if (!timeString) return '---';
        const parts = timeString.split('.');
        return parts[0]; 
    };

    summaries.forEach(sum => {
        const row = document.createElement('tr');

        // 承認ステータスの表示ロジック
        const rawStatus = sum.approvalStatus || 'PENDING'; 
        const cleanStatus = String(rawStatus).trim().replace(/\s/g, ''); 
        const approvalStatus = cleanStatus.toUpperCase(); 

        let approvalText;
        let approvalClass;

        if (approvalStatus === 'APPROVED' || approvalStatus === 'FINALIZED') {
            approvalText = '承認済';
            approvalClass = 'status-approved';
        } else if (approvalStatus === 'PENDING') {
            approvalText = '承認待ち';
            approvalClass = 'status-pending';
        } else {
            // 予期しないステータスの場合（データが破損している可能性）
            approvalText = rawStatus; 
            approvalClass = 'status-adjusted'; 
        }

        // アクションボタン
        const actions = approvalStatus === 'PENDING' ? 
            `<button class="small-btn edit-btn" data-id="${sum.id}"><i class="fas fa-pen"></i> 修正</button>
             <button class="small-btn primary-btn approve-single-btn" data-id="${sum.id}">承認</button>` :
            `<button class="small-btn edit-btn" data-id="${sum.id}"><i class="fas fa-pen"></i> 修正</button>`;


        // HTMLの9列構成に合わせて描画ロジックを修正
        row.innerHTML = `
            <td>${sum.workDate || '---'}</td>
            <td>${sum.employeeName || '---'}</td>
            <td>${formatTime(sum.actualInTime)}</td>
            <td>${sum.totalBreakMinutes ? (sum.totalBreakMinutes + '分') : '---'}</td> 
            <td>${formatTime(sum.actualOutTime)}</td>
            <td>${sum.totalWorkMinutes ? (sum.totalWorkMinutes + '分') : '---'}</td> 
            <td><span class="${approvalClass}">${approvalText}</span></td> 
            <td>${actions}</td>
        `;
        tableBody.appendChild(row);
    });

    // -----------------------------------------------------------------
    // ★★★ 修正機能の実装: edit-btn クリック時のイベントリスナー設定 ★★★
    // -----------------------------------------------------------------
    document.querySelectorAll('#attendance-logs .edit-btn').forEach(button => {
        button.addEventListener('click', (event) => {
            const summaryId = event.currentTarget.dataset.id;
            if (summaryId) {
                // 修正専用の画面（edit_attendance.html）にIDを渡して遷移
                window.location.href = `edit_attendance.html?summaryId=${summaryId}`;
            }
        });
    });

    // 単体承認ボタンにイベントリスナーを設定
    document.querySelectorAll('#attendance-logs .approve-single-btn').forEach(button => {
        button.addEventListener('click', async (event) => {
            const summaryId = event.currentTarget.dataset.id;
            
            const targetSummary = currentDisplayedSummaries.find(s => String(s.id) === summaryId);
            const workDate = targetSummary ? targetSummary.workDate : null;

            if (!summaryId) {
                alert('承認対象のデータIDが見つかりません。');
                return;
            }
            if (!workDate) {
                alert('承認対象の勤務日が見つかりません。');
                return;
            }

            if (!confirm(`ID: ${summaryId} (${workDate}) の勤怠データを承認済みにしますか？`)) {
                return;
            }

            try {
                // 認証情報はgetAuthHeadersに含まれる
                const res = await fetch(`/api/summaries/approve/list`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        ...getAuthHeaders()
                    },
                    body: JSON.stringify({ 
                        summaryIds: [parseInt(summaryId, 10)], 
                        // workDateを期間として使用
                        startDate: workDate, 
                        endDate: workDate 
                    }) 
                });

                const data = await res.json(); 

                if (res.ok) {
                    alert(`✅ ID: ${summaryId} の勤怠サマリーを承認しました。`);
                    fetchAndDisplaySummaries(); // 承認後、データを再読み込み
                } else {
                    throw new Error(data.message || '単体承認処理中に不明なエラーが発生しました。');
                }
            } catch (error) {
                // getAuthHeadersがリダイレクトするため、エラーはAPI通信後のエラーに絞られる
                alert(`❌ 単体承認に失敗しました: ${error.message}`);
                console.error("Single approval error:", error);
            }
        });
    });
}


document.addEventListener('DOMContentLoaded', function () {
    
    // --- ロールに基づく画面表示制御の実行 ---
    const userRole = getLoggedInUserRole();
    try {
        // getLoggedInEmployeeId()がエラーをthrowしなくなったため、try...catchは不要。
        // getAuthHeaders()内でチェックとリダイレクトが行われる。
        const employeeId = getLoggedInEmployeeId(); 

        // 1. 権限がないユーザーのチェック（ログインセッションの確認）
        if (!employeeId || userRole === 'UNKNOWN') {
            alert('セッション情報が無効です。再度ログインしてください。');
            window.location.href = 'admin_login.html';
            return; 
        }
    } catch (e) {
        // 念のため、エラーが発生した場合の保険
        alert('セッション情報が無効です。再度ログインしてください。');
        window.location.href = 'admin_login.html';
        return;
    }
    
    // 2. EMP ロール時の処理（リダイレクトを推奨）
    if (userRole === 'EMP') {
        // EMPは管理者画面にいるべきではないため、専用画面へ即座にリダイレクト
        window.location.href = 'emp_main.html'; 
        return;
    }


    // 3. ADMIN/MGR/OWNER向け初期処理（EMPではない場合のみ実行）
    fetchAndRenderEmployees();
    fetchMonthlySummary();
    fetchAndRenderAttendanceSummary();
    fetchAndDisplaySummaries();


    // ------------------------------------------
    // 共通のイベントリスナー設定（ロールによらず動作させる）
    // ------------------------------------------
    const searchButton = document.querySelector('#search-button');
    if (searchButton) {
        searchButton.addEventListener('click', handleSearch);
    }
    
    const executeAggregationBtn = document.getElementById('execute-aggregation-btn');
    const periodSelectCheck = document.getElementById('aggregation-period-select');

    if (executeAggregationBtn && periodSelectCheck) {
        executeAggregationBtn.addEventListener('click', () => {
            fetchAndRenderAttendanceSummary();
        });
    }

    // 従業員マスタ検索イベント
    const searchEmpBtn = document.getElementById('employee-search-btn');
    const searchEmpInput = document.getElementById('employee-search-input');
    if (searchEmpBtn && searchEmpInput) {
        searchEmpBtn.addEventListener('click', async () => {
            const keyword = searchEmpInput.value.trim();
            try {
                // headersにX-Operator-Idが含まれるようになりました
                const res = await fetch(`/api/employees?keyword=${encodeURIComponent(keyword)}&limit=3`, { headers: getAuthHeaders() }); 
                
                if (!res.ok) throw new Error('検索失敗');
                const employees = await res.json();
                
                const activeEmployees = employees.filter(emp => emp.active === true);
                renderEmployeeRows(activeEmployees);
            } catch (err) {
                alert('検索に失敗しました');
            }
        });
        searchEmpInput.addEventListener('keydown', e => {
            if (e.key === 'Enter') searchEmpBtn.click();
        });
    }
    
    // 給与計算セクションの期間プルダウンの初期化（ADMIN/OWNERのみ実行）
    const periodSelect = document.getElementById('aggregation-period-select');
    if (periodSelect) {
        periodSelect.innerHTML = ''; 
        const today = new Date();
        const currentYear = today.getFullYear();
        const currentMonth = today.getMonth();

        for (let i = 0; i < 3; i++) {
            const targetDate = new Date(currentYear, currentMonth - i, 1);
            const year = targetDate.getFullYear();
            const month = targetDate.getMonth() + 1; 
            const monthString = `${year}年${month}月度`;
            const valueString = `${year}-${String(month).padStart(2, '0')}`; 
            
            const option = document.createElement('option');
            option.value = valueString;
            option.textContent = monthString;

            if (i === 0) {
                option.selected = true;
            }
            periodSelect.appendChild(option);
        }
    }
});