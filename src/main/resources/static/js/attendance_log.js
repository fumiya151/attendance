// ログイン中の従業員IDをセッションストレージから取得する関数 (共通JSファイルに存在することを前提)
function getLoggedInEmployeeId() {
    const employeeId = sessionStorage.getItem('loggedInEmployeeId'); 
    if (!employeeId) {
        throw new Error("操作を行う従業員IDが見つかりません。ログインが必要です。");
    }
    return employeeId;
}


document.addEventListener('DOMContentLoaded', function () {
    fetchAndDisplayAttendanceLogs();
    
    // 【既存】検索ボタンにイベントリスナーを設定
    const searchButton = document.querySelector('#search-button');
    searchButton.addEventListener('click', handleSearch);
    
    // ----------------------------------------------------
    // ★ 修正点: 一括承認ボタンのイベントリスナーを追加
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
                // 監査ヘッダーの取得 (ログインIDが必要)
                const approverId = getLoggedInEmployeeId(); 
                
                // API呼び出し: POST /api/summaries/approve/batch
                const res = await fetch(`/api/summaries/approve/batch?startDate=${startDate}&endDate=${endDate}`, {
                    method: 'POST',
                    headers: {
                        // 必須: 監査ヘッダーを追加
                        'X-Operator-Id': approverId 
                    }
                });

                const data = await res.json(); // レスポンスJSONを解析

                if (res.ok) {
                    alert(`✅ 勤怠サマリーを一括承認しました。\n承認件数: ${data.approvedCount}件`);
                    // 承認処理はログ自体を変更しないが、ステータスが変わったことを通知
                    // 必要であれば、テーブルを再読み込み
                    // fetchAndDisplayAttendanceLogs(); 
                } else {
                    // サーバーからのエラーメッセージ（401 Unauthorized, 403 Forbiddenなど）を処理
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
    // ----------------------------------------------------
});

// 取得した全ログを保持するためのグローバル変数
let allPairedLogs = []; 

async function fetchAndDisplayAttendanceLogs() {
    try {
        const response = await fetch('/api/attendance/logs');
        if (!response.ok) {
            throw new Error('勤怠ログの取得に失敗しました。');
        }
        const logs = await response.json();

        // ログを日ごと・従業員IDごとにペアリングし、全ログとして保存
        allPairedLogs = pairDailyAttendance(logs);
        
        // フィルタリングなしで初期表示
        applyFiltersAndRenderTable(allPairedLogs); 

    } catch (error) {
        console.error('エラー:', error);
        document.querySelector('.data-table tbody').innerHTML = `<tr><td colspan="7" style="color: red;">エラー: ${error.message}</td></tr>`;
    }
}

// 【追加】検索処理を実行する関数
function handleSearch() {
    const searchTerm = document.querySelector('#search-input').value.toLowerCase();
    
    if (!searchTerm) {
        // 検索文字列がない場合は全ログを表示
        applyFiltersAndRenderTable(allPairedLogs);
        return;
    }
    
    // 氏名 (employeeName) または 日付 (date) を含むログをフィルタリング
    const filteredLogs = allPairedLogs.filter(log => {
        // 氏名 (小文字化して比較)
        const nameMatch = log.employeeName.toLowerCase().includes(searchTerm);
        // 日付 (YYYY-MM-DD形式で比較)
        const dateMatch = log.date.includes(searchTerm);
        
        return nameMatch || dateMatch;
    });

    applyFiltersAndRenderTable(filteredLogs);
}


/**
 * フィルタリングされたログをテーブルに描画する関数 (抽出)
 * @param {Array<Object>} logs - 描画する勤怠ログの配列
 */
function applyFiltersAndRenderTable(logs) {
    const tableBody = document.querySelector('.data-table tbody');
    tableBody.innerHTML = ''; // テーブルをクリア

    // ヘッダーが7列なので、colspanを7に変更
    if (logs.length === 0) {
        tableBody.innerHTML = `<tr><td colspan="7">該当する勤怠ログがありません。</td></tr>`;
        return;
    }

    logs.forEach(dayLog => {
        const row = document.createElement('tr');

        // 複数の休憩時間を表示用に整形 (例: 12:00 - 13:00, 15:00 - 15:30)
        const breakText = dayLog.breaks.map(b =>
            `${b.start}${b.end ? ' - ' + b.end : ' (未終了)'}`
        ).join('<br>');

        // ステータス判定: IN/OUTが揃っていて、休憩がすべて閉じているか
        const isCompleted = dayLog.inTime && dayLog.outTime && !dayLog.isBreakOpen;
        const statusClass = isCompleted ? 'status-approved' : 'status-pending';
        const statusText = isCompleted ? '完了' : '未完了';

        const actions = `
            <button class="small-btn edit-btn" data-employee-id="${dayLog.employeeId}"><i class="fas fa-pen"></i> 修正依頼</button>
        `;

        // 休憩の列を1つに統合 (breakText)
        row.innerHTML = `
            <td>${dayLog.date}</td>
            <td>${dayLog.employeeName}</td>
            <td>${dayLog.inTime || '---'}</td>
            <td>${breakText || '---'}</td> 
            <td>${dayLog.outTime || '---'}</td> 
            <td><span class="${statusClass}">${statusText}</span></td>
            <td>${actions}</td>
        `;
        tableBody.appendChild(row);
    });
}


/**
 * 勤怠ログを日付と従業員ごとにペアリングし、日報形式に整形する関数 (複数休憩対応版)
 * @param {Array<Object>} logs - DBから取得した生ログの配列
 * @returns {Array<Object>} 日報形式に整形されたログの配列
 */
function pairDailyAttendance(logs) {
    // ログを時系列順にソート
    logs.sort((a, b) => new Date(a.stampTime) - new Date(b.stampTime));

    const dailyMap = {};
    const timeOptions = { hour: '2-digit', minute: '2-digit' }; // 時刻表示形式

    logs.forEach(log => {
        // タイムゾーンを考慮せず、日付部分だけをキーとして使用 (例: 2025-10-07)
        const dateKey = new Date(log.stampTime).toLocaleDateString('sv-SE');
        const key = `${dateKey}_${log.employeeId}`;

        if (!dailyMap[key]) {
            // 新しい日報レコードを作成
            dailyMap[key] = {
                date: dateKey,
                employeeId: log.employeeId,
                employeeName: log.employeeName || log.employeeId,
                inTime: null,
                outTime: null,
                breaks: [], // 休憩ペアの配列: [{ start: 'HH:MM', end: 'HH:MM' }]
                isBreakOpen: false, // 現在休憩中か
            };
        }

        const currentDayLog = dailyMap[key];
        const timeString = new Date(log.stampTime).toLocaleTimeString('ja-JP', timeOptions);

        // 打刻種別に基づいて時刻を割り当てる (簡易的なペアリング)
        if (log.stampType === '出勤' && !currentDayLog.inTime) {
            currentDayLog.inTime = timeString;
        } else if (log.stampType === '退勤') {
            // 最後の退勤時刻を常に記録
            currentDayLog.outTime = timeString;
        } else if (log.stampType === '休憩開始') {
            currentDayLog.breaks.push({
                start: timeString,
                end: null, // 終了時刻はまだない
            });
            currentDayLog.isBreakOpen = true; // 休憩が開始された
        } else if (log.stampType === '休憩終了') {
            // breaks配列の最新の（endがnullの）要素に終了時刻を記録
            const lastBreak = currentDayLog.breaks[currentDayLog.breaks.length - 1];
            if (lastBreak && !lastBreak.end) {
                lastBreak.end = timeString;
                currentDayLog.isBreakOpen = false; // 休憩が閉じられた
            }
        }
    });

    // マップの値を配列に戻し、日付降順でソート
    return Object.values(dailyMap).sort((a, b) => b.date.localeCompare(a.date));
}