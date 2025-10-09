// ログイン中の従業員IDをセッションストレージから取得する関数 (共通JSファイルに存在することを前提)
function getLoggedInEmployeeId() {
    const employeeId = sessionStorage.getItem('loggedInEmployeeId'); 
    if (!employeeId) {
        throw new Error("操作を行う従業員IDが見つかりません。ログインが必要です。");
    }
    return employeeId;
}


document.addEventListener('DOMContentLoaded', function () {
    // ★ 修正点1: ログではなくサマリーデータを取得するように変更
    fetchAndDisplaySummaries();
    
    // 【既存】検索ボタンにイベントリスナーを設定 (検索ロジックも修正が必要)
    const searchButton = document.querySelector('#search-button');
    searchButton.addEventListener('click', handleSearch);
    
    // ----------------------------------------------------
    // ★ 修正点: 一括承認ボタンのイベントリスナー (変更なし)
    // ----------------------------------------------------
    const approveBtn = document.getElementById('batch-approve-btn');
    const startDateInput = document.getElementById('approval-start-date');
    const endDateInput = document.getElementById('approval-end-date');

    if (approveBtn && startDateInput && endDateInput) {
        approveBtn.addEventListener('click', async () => {
            const startDate = startDateInput.value;
            const endDate = endDateInput.value;
            
            if (!startDate || !endDate) {
                alert("承認期間を正しく入力してください（例: YYYY-MM-DD）。");
                return;
            }

            if (!confirm(`${startDate}から${endDate}までの全従業員の勤怠データ（未承認分）を承認済みにします。よろしいですか？\n(承認後は給与計算の対象となり、修正が困難になります)`)) {
                return;
            }

            try {
                const approverId = getLoggedInEmployeeId(); 
                
                const res = await fetch(`/api/summaries/approve/batch?startDate=${startDate}&endDate=${endDate}`, {
                    method: 'POST',
                    headers: {
                        'X-Operator-Id': approverId 
                    }
                });

                const data = await res.json(); 

                if (res.ok) {
                    alert(`✅ 勤怠サマリーを一括承認しました。\n承認件数: ${data.approvedCount}件`);
                    // 承認後に画面を更新してステータス変更を反映
                    fetchAndDisplaySummaries(); 
                } else {
                    throw new Error(data.message || '承認処理中に不明なエラーが発生しました。');
                }
            } catch (error) {
                if (error.message.includes("操作を行う従業員IDが見つかりません")) {
                    alert('❌ 承認失敗: ログインセッションが無効です。ログアウトして再度ログインしてください。');
                } else {
                    alert(`❌ 承認に失敗しました: ${error.message}`);
                    console.error("Batch approval error:", error);
                }
            }
        });
    }
});

// 取得した全サマリーを保持するためのグローバル変数
let allSummaries = []; 

/**
 * ★ 新しいデータ取得関数: サマリーデータと従業員名を取得する
 */
async function fetchAndDisplaySummaries() {
    try {
        // ★ 修正点2: 新しい API を叩く
        const response = await fetch('/api/summaries'); 
        if (!response.ok) {
            throw new Error('勤怠サマリーの取得に失敗しました。');
        }
        
        // 取得したデータは DailyAttendanceSummaryDto の配列
        const summaries = await response.json(); 

        allSummaries = summaries; // グローバル変数に保存
        
        // フィルタリングなしで初期表示
        applyFiltersAndRenderTable(allSummaries); 

    } catch (error) {
        console.error('エラー:', error);
        // ヘッダーが8列 (日付〜アクション) に増えたため colspan を修正
        document.querySelector('.data-table tbody').innerHTML = `<tr><td colspan="8" style="color: red;">エラー: ${error.message}</td></tr>`;
    }
}

/**
 * 検索処理を実行する関数 (サマリーデータに合わせて修正)
 */
function handleSearch() {
    const searchTerm = document.querySelector('#search-input').value.toLowerCase();
    
    if (!searchTerm) {
        applyFiltersAndRenderTable(allSummaries);
        return;
    }
    
    // 氏名 (employeeName) または 日付 (workDate) を含むログをフィルタリング
    const filteredSummaries = allSummaries.filter(summary => {
        // 氏名 (employeeName)
        const nameMatch = summary.employeeName.toLowerCase().includes(searchTerm);
        // 日付 (workDate)
        const dateMatch = summary.workDate.includes(searchTerm); 
        
        return nameMatch || dateMatch;
    });

    applyFiltersAndRenderTable(filteredSummaries);
}


/**
 * フィルタリングされたサマリーデータをテーブルに描画する関数 (8列表示に対応)
 * @param {Array<Object>} summaries - 描画する DailyAttendanceSummaryDto の配列
 */
function applyFiltersAndRenderTable(summaries) {
    const tableBody = document.querySelector('.data-table tbody');
    tableBody.innerHTML = ''; // テーブルをクリア

    // ヘッダーが8列 (日付, 氏名, 出勤, 休憩, 退勤, データ状態, 承認状態, アクション)
    if (summaries.length === 0) {
        tableBody.innerHTML = `<tr><td colspan="8">該当する勤怠サマリーがありません。</td></tr>`;
        return;
    }

    summaries.forEach(sum => {
        const row = document.createElement('tr');

        // 承認ステータスの表示ロジック
        const approvalStatus = sum.approvalStatus; // PENDING, APPROVED, FINALIZED など
        let approvalText;
        let approvalClass;

        if (approvalStatus === 'APPROVED' || approvalStatus === 'FINALIZED') {
            approvalText = '承認済';
            approvalClass = 'status-approved';
        } else if (approvalStatus === 'PENDING') {
            approvalText = '承認待ち';
            approvalClass = 'status-pending';
        } else {
            approvalText = approvalStatus;
            approvalClass = 'status-adjusted'; // その他
        }

        // アクションボタン: APPROVED/FINALIZED でなければ承認ボタンを表示
        const actions = approvalStatus === 'PENDING' ? 
            `<button class="small-btn edit-btn" data-id="${sum.id}"><i class="fas fa-pen"></i> 修正</button>
             <button class="small-btn primary-btn approve-single-btn" data-id="${sum.id}">承認</button>` :
            `<button class="small-btn secondary-btn" data-id="${sum.id}">詳細</button>`;


        // ★ 8列の描画ロジック ★
        row.innerHTML = `
            <td>${sum.workDate}</td>
            <td>${sum.employeeName}</td>
            <td>${sum.actualInTime || '---'}</td>
            <td>${sum.totalBreakMinutes ? (sum.totalBreakMinutes + '分') : '---'}</td> 
            <td>${sum.actualOutTime || '---'}</td> 
            <td>${sum.logStatus || '---'}</td> <td><span class="${approvalClass}">${approvalText}</span></td> <td>${actions}</td>
        `;
        tableBody.appendChild(row);
    });
}