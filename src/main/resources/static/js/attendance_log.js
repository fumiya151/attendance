// ログイン中の従業員IDをセッションストレージから取得する関数 (共通JSファイルに存在することを前提)
function getLoggedInEmployeeId() {
    const employeeId = sessionStorage.getItem('loggedInEmployeeId'); 
    if (!employeeId) {
        throw new Error("操作を行う従業員IDが見つかりません。ログインが必要です。");
    }
    return employeeId;
}


// 取得した全サマリーを保持するためのグローバル変数
let allSummaries = []; 
// 現在テーブルに表示されている（フィルタリング後の）サマリーを保持する変数
let currentDisplayedSummaries = []; 


document.addEventListener('DOMContentLoaded', function () {
    // ログではなくサマリーデータを取得
    fetchAndDisplaySummaries();
    
    // 検索ボタンにイベントリスナーを設定
    const searchButton = document.querySelector('#search-button');
    searchButton.addEventListener('click', handleSearch);

    const approveBtn = document.getElementById('batch-approve-btn');
    // 期間入力フィールドの参照
    const startDateInput = document.getElementById('approval-start-date'); 
    const endDateInput = document.getElementById('approval-end-date');   

    // 一括承認ボタンのイベントリスナー（検索結果承認 + 期間チェック）
    if (approveBtn && startDateInput && endDateInput) {
        approveBtn.addEventListener('click', async () => {
            
            // 期間値を取得
            const startDate = startDateInput.value;
            const endDate = endDateInput.value;

            if (!startDate || !endDate) {
                alert("承認期間を正しく入力してください（例: YYYY-MM-DD）。");
                return;
            }

            // 現在表示されている（検索結果）データから未承認のIDを抽出
            const pendingIds = currentDisplayedSummaries
                .filter(summary => summary.approvalStatus === 'PENDING')
                .map(summary => summary.id);

            if (pendingIds.length === 0) {
                alert("承認待ちの勤怠サマリーがありません。\n（検索結果には、承認済みのもの、または修正中のものしか含まれていません。）");
                return;
            }
            
            // 確認ダイアログ
            if (!confirm(`期間: ${startDate}〜${endDate} 内で、現在表示されている承認待ちの勤怠データ ${pendingIds.length}件を承認済みにします。よろしいですか？\n(承認後は給与計算の対象となり、修正が困難になります)`)) {
                return;
            }

            try {
                const approverId = getLoggedInEmployeeId(); 
                
                // API呼び出し: POST /api/summaries/approve/list (IDリストと期間の両方を送信)
                const res = await fetch(`/api/summaries/approve/list`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'X-Operator-Id': approverId 
                    },
                    body: JSON.stringify({ 
                        summaryIds: pendingIds, 
                        startDate: startDate, 
                        endDate: endDate      
                    }) 
                });

                const data = await res.json(); 

                if (res.ok) {
                    alert(`✅ 勤怠サマリーを一括承認しました。\n承認件数: ${data.approvedCount}件`);
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


async function fetchAndDisplaySummaries() {
    try {
        const response = await fetch('/api/summaries'); 
        if (!response.ok) {
            throw new Error('勤怠サマリーの取得に失敗しました。');
        }
        
        const summaries = await response.json(); 

        allSummaries = summaries; // 全データをグローバル変数に保存
        
        // プルダウンメニューを初期化/描画
        populateSearchDropdowns(summaries);

        // フィルタリングなしで初期表示
        applyFiltersAndRenderTable(allSummaries); 

    } catch (error) {
        console.error('エラー:', error);
        document.querySelector('.data-table tbody').innerHTML = `<tr><td colspan="8" style="color: red;">エラー: ${error.message}</td></tr>`;
    }
}

/**
 * 取得したサマリーデータに基づき、検索プルダウンを生成する
 */
function populateSearchDropdowns(summaries) {
    const employeeDropdown = document.getElementById('search-employee');
    const monthDropdown = document.getElementById('search-month');

    // 1. 従業員名のリストを生成 (Setで重複を除去)
    const employeeNames = [...new Set(summaries.map(s => s.employeeName))].sort();
    
    // 2. 対象月のリストを生成 (YYYY-MM形式, Setで重複を除去し、降順にソート)
    const months = [...new Set(summaries.map(s => s.workDate.substring(0, 7)))].sort((a, b) => b.localeCompare(a));

    // ドロップダウンを初期化 (初期オプションを残すため、既存のものを除去)
    employeeDropdown.innerHTML = '<option value="">--- 全従業員 ---</option>';
    monthDropdown.innerHTML = '<option value="">--- 全期間 ---</option>';

    // 従業員名オプションの追加
    employeeNames.forEach(name => {
        const option = document.createElement('option');
        option.value = name; // 値も表示名も氏名
        option.textContent = name;
        employeeDropdown.appendChild(option);
    });

    // 対象月オプションの追加
    months.forEach(month => {
        const option = document.createElement('option');
        // YYYY-MMを YYYY年MM月 の形式に変換して表示
        const displayMonth = month.replace('-', '年') + '月'; 
        option.value = month; // 値は YYYY-MM
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
            // summary.workDate (YYYY-MM-DD) が selectedMonth (YYYY-MM) で始まるかチェック
            const summaryMonth = summary.workDate.substring(0, 7);
            if (summaryMonth !== selectedMonth) {
                isMatch = false;
            }
        }
        
        // 2. 従業員名によるフィルタリング
        if (isMatch && selectedEmployee) {
            // summary.employeeName と selectedEmployee が一致するかチェック
            if (summary.employeeName !== selectedEmployee) {
                isMatch = false;
            }
        }
        
        return isMatch;
    });

    applyFiltersAndRenderTable(filteredSummaries);
}


/**
 * フィルタリングされたサマリーデータをテーブルに描画する関数
 */
function applyFiltersAndRenderTable(summaries) {
    // 描画に使用するサマリーをグローバル変数に保存
    currentDisplayedSummaries = summaries; 
    
    const tableBody = document.querySelector('.data-table tbody');
    tableBody.innerHTML = ''; // テーブルをクリア

    if (summaries.length === 0) {
        tableBody.innerHTML = `<tr><td colspan="8">該当する勤怠サマリーがありません。</td></tr>`;
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
        const approvalStatus = sum.approvalStatus; 
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
            approvalClass = 'status-adjusted'; 
        }

        // アクションボタン
        const actions = approvalStatus === 'PENDING' ? 
            `<button class="small-btn edit-btn" data-id="${sum.id}"><i class="fas fa-pen"></i> 修正</button>
             <button class="small-btn primary-btn approve-single-btn" data-id="${sum.id}">承認</button>` :
            `<button class="small-btn secondary-btn" data-id="${sum.id}">詳細</button>`;


        // 8列の描画ロジックと整形適用
        row.innerHTML = `
            <td>${sum.workDate}</td>
            <td>${sum.employeeName}</td>
            <td>${formatTime(sum.actualInTime)}</td> <td>${sum.totalBreakMinutes ? (sum.totalBreakMinutes + '分') : '---'}</td> 
            <td>${formatTime(sum.actualOutTime)}</td> <td>${sum.logStatus || '---'}</td> 
            <td><span class="${approvalClass}">${approvalText}</span></td> 
            <td>${actions}</td>
        `;
        tableBody.appendChild(row);
    });

    // 単体承認ボタンにイベントリスナーを設定
    document.querySelectorAll('.approve-single-btn').forEach(button => {
        button.addEventListener('click', async (event) => {
            const summaryId = event.currentTarget.dataset.id;
            
            // 期間入力フィールドから現在の日付を取得
            let startDate = document.getElementById('approval-start-date').value;
            let endDate = document.getElementById('approval-end-date').value;
            
            // ★ 修正: 期間入力が空の場合、本日をデフォルトとして使用 ★
            if (!startDate || !endDate) {
                const today = new Date().toISOString().substring(0, 10); // YYYY-MM-DD 形式
                startDate = today;
                endDate = today;
                // Controllerの必須チェックを満たすための措置
            }

            if (!summaryId) {
                alert('承認対象のデータIDが見つかりません。');
                return;
            }

            if (!confirm(`ID: ${summaryId} の勤怠データを承認済みにしますか？\n(Controllerの必須チェックを満たすため、期間: ${startDate}〜${endDate} を使用します)`)) {
                return;
            }

            try {
                const approverId = getLoggedInEmployeeId();
                
                // API呼び出し: POST /api/summaries/approve/list (1件のIDリストと期間を送信)
                const res = await fetch(`/api/summaries/approve/list`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'X-Operator-Id': approverId 
                    },
                    body: JSON.stringify({ 
                        summaryIds: [parseInt(summaryId, 10)], // 1件のIDリスト
                        startDate: startDate, 
                        endDate: endDate 
                    }) 
                });

                const data = await res.json(); 

                if (res.ok) {
                    alert(`✅ ID: ${summaryId} の勤怠サマリーを承認しました。`);
                    fetchAndDisplaySummaries(); 
                } else {
                    throw new Error(data.message || '単体承認処理中に不明なエラーが発生しました。');
                }
            } catch (error) {
                if (error.message.includes("操作を行う従業員IDが見つかりません")) {
                    alert('❌ 承認失敗: ログインセッションが無効です。ログアウトして再度ログインしてください。');
                } else {
                    alert(`❌ 単体承認に失敗しました: ${error.message}`);
                    console.error("Single approval error:", error);
                }
            }
        });
    });
}