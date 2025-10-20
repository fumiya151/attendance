// ログイン中の従業員IDをセッションストレージから取得する関数 (共通JSファイルに存在することを前提)
function getLoggedInEmployeeId() {
    const employeeId = sessionStorage.getItem('loggedInEmployeeId'); 
    if (!employeeId) {
        throw new Error("操作を行う従業員IDが見つかりません。ログインが必要です。");
    }
    return employeeId;
}

/**
 * 現在のデータリストから従業員名に対応する従業員IDを検索します。
 * @param {string} employeeName 検索対象の従業員名
 * @returns {string | null} 従業員ID、見つからない場合はnull
 */
function getEmployeeIdFromName(employeeName) {
    // allSummaries から検索
    const summary = allSummaries.find(s => s.employeeName === employeeName);
    return summary ? summary.employeeId : null;
}

function getAuthHeaders() {
    const token = sessionStorage.getItem('token');
    const employeeId = sessionStorage.getItem('loggedInEmployeeId');
    
    if (!token || !employeeId) {
        alert("認証セッションが無効です。再度ログインしてください。");
        // /html/admin_login.html は SecurityConfiguration で PermitAll されています
        window.location.href = '/html/admin_login.html'; 
        return {};
    }
    
    return {
        'Authorization': `Bearer ${token}`,
        'X-Operator-Id': employeeId 
    };
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
    if (searchButton) {
        searchButton.addEventListener('click', handleSearch);
    }

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
                // API呼び出し: POST /api/summaries/approve/list
                const res = await fetch(`/api/summaries/approve/list`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        ...getAuthHeaders()
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
                if (error.message.includes("操作を行う従業員IDが見つかりません") || error.message.includes("認証セッションが無効です")) {
                    // getAuthHeaders内でリダイレクトされる可能性もあるため、冗長なチェック
                    console.error("Batch approval authentication error:", error);
                } else {
                    alert(`❌ 承認に失敗しました: ${error.message}`);
                    console.error("Batch approval error:", error);
                }
            }
        });
    }

    // PDFエクスポートボタンのイベントリスナー (修正)
    const pdfExportBtn = document.getElementById('csv-export-btn'); // IDは元のまま使用
    if (pdfExportBtn) {
        pdfExportBtn.addEventListener('click', () => {
            const selectedMonth = document.getElementById('search-month').value;
            const selectedEmployeeName = document.getElementById('search-employee').value;

            if (!selectedEmployeeName || !selectedMonth) {
                alert("PDFエクスポートを行うには、検索プルダウンで対象の【従業員】と【月】を一つずつ選択してください。");
                return;
            }
            
            // 選択された従業員名から従業員IDを取得
            const employeeId = getEmployeeIdFromName(selectedEmployeeName);

            if (!employeeId) {
                alert(`従業員名 "${selectedEmployeeName}" のIDが見つかりません。`);
                return;
            }

            // 新しい PDF出力関数を呼び出す
            exportToPdf(employeeId, selectedMonth, selectedEmployeeName); 
        });
    }
});


async function fetchAndDisplaySummaries() {
    const headers = getAuthHeaders();
    if (!headers['Authorization']) return; // トークンがない場合は処理を中断

    try {
        const response = await fetch('/api/summaries', { headers }); 
        
        if (!response.ok) {
             const errorText = await response.text();
             if (response.status === 403) throw new Error('アクセス権限がありません。');
             throw new Error(`勤怠サマリーの取得に失敗しました (ステータス: ${response.status} / エラー: ${errorText.substring(0, 50)}...)`);
        }
        
        const summaries = await response.json(); 

        allSummaries = summaries; // 全データをグローバル変数に保存
        
        // プルダウンメニューを初期化/描画
        populateSearchDropdowns(summaries);

        // フィルタリングなしで初期表示
        applyFiltersAndRenderTable(allSummaries); 

    } catch (error) {
        console.error('エラー:', error);
        // colspan を 9 に修正（前の会話で 9 列のデータがあると確認されたため）
        document.querySelector('.data-table tbody').innerHTML = `<tr><td colspan="9" style="color: red; text-align: center;">エラー: ${error.message}</td></tr>`;
        if (error.message.includes('権限')) alert(error.message);
    }
}

/**
 * 取得したサマリーデータに基づき、検索プルダウンを生成する
 */
function populateSearchDropdowns(summaries) {
    const employeeDropdown = document.getElementById('search-employee');
    const monthDropdown = document.getElementById('search-month');

    if (!employeeDropdown || !monthDropdown) return; // 別のセクションのドロップダウンかもしれないため

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

    // HTMLの列数に合わせて colspan を設定
    const COL_SPAN = 9; 

    if (summaries.length === 0) {
        tableBody.innerHTML = `<tr><td colspan="${COL_SPAN}" style="text-align: center;">該当する勤怠サマリーがありません。</td></tr>`;
        return;
    }
    
    // 時刻整形ヘルパー関数を定義
    const formatTime = (timeString) => {
        if (!timeString) return '---';
        // JSONで受け取る時刻文字列（例: "09:00:00.000000"）をHH:mmに整形
        const parts = timeString.split(':');
        return parts.length >= 2 ? `${parts[0]}:${parts[1]}` : '---'; 
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
            `<button class="small-btn edit-btn" data-id="${sum.id}"><i class="fas fa-pen"></i> 修正</button>`;

        // 8列の描画ロジックと整形適用 (実働時間を追加)
        row.innerHTML = `
            <td>${sum.workDate}</td>
            <td>${sum.employeeName}</td>
            <td>${formatTime(sum.actualInTime)}</td> 
            <td>${sum.totalBreakMinutes ? (sum.totalBreakMinutes + '分') : '---'}</td> 
            <td>${formatTime(sum.actualOutTime)}</td>
            <td>${sum.totalWorkMinutes ? (sum.totalWorkMinutes + '分') : '---'}</td> 
            <td><span class="${approvalClass}">${approvalText}</span></td> 
            <td>${actions}</td>
        `;
        tableBody.appendChild(row);
    });

    document.querySelectorAll('.edit-btn').forEach(button => {
        button.addEventListener('click', (event) => {
            const summaryId = event.currentTarget.dataset.id;
            if (summaryId) {
                // 修正専用の画面（edit_attendance.html）にIDを渡して遷移
                window.location.href = `edit_attendance.html?summaryId=${summaryId}`;
            }
        });
    });
    // -----------------------------------------------------------------


    // 単体承認ボタンにイベントリスナーを設定
    document.querySelectorAll('.approve-single-btn').forEach(button => {
        button.addEventListener('click', async (event) => {
            const summaryId = event.currentTarget.dataset.id;
            
            // 期間入力フィールドから現在の日付を取得
            let startDate = document.getElementById('approval-start-date')?.value;
            let endDate = document.getElementById('approval-end-date')?.value;
            
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
                // API呼び出し: POST /api/summaries/approve/list (1件のIDリストと期間を送信)
                const res = await fetch(`/api/summaries/approve/list`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        ...getAuthHeaders()
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
                if (error.message.includes("操作を行う従業員IDが見つかりません") || error.message.includes("認証セッションが無効です")) {
                    console.error("Single approval authentication error:", error);
                } else {
                    alert(`❌ 単体承認に失敗しました: ${error.message}`);
                    console.error("Single approval error:", error);
                }
            }
        });
    });
}


// --- PDF生成・ダウンロード関数 (API経由に修正) ---
/**
 * 指定された従業員と月の勤務表PDFをバックエンドAPI経由で取得し、ダウンロードします。
 * @param {string} employeeId 対象従業員ID
 * @param {string} yearMonthStr 対象年月 (YYYY-MM)
 * @param {string} employeeName ファイル名表示用の従業員名
 */
async function exportToPdf(employeeId, yearMonthStr, employeeName) {
    
    const headers = getAuthHeaders();
    if (!headers['Authorization']) return; // トークンがない場合は処理を中断

    // ユーザーに処理中であることを知らせる
    const originalButton = document.getElementById('csv-export-btn');
    const originalButtonText = originalButton.innerHTML;
    originalButton.innerHTML = '<i class="fas fa-spinner fa-spin"></i> PDF生成中...';
    originalButton.disabled = true;

    try {
        // バックエンドAPIの呼び出し
        const url = `/api/exports/pdf/summaries?employeeId=${employeeId}&yearMonth=${yearMonthStr}`;
        const response = await fetch(url, {
            method: 'GET',
            headers: headers
        });
        
        // エラー処理
        if (!response.ok) {
            // エラー応答がテキスト（日本語メッセージ）の場合を考慮
            const errorText = await response.text();
            throw new Error(`PDF生成APIエラー (${response.status}): ${errorText.substring(0, 100)}...`); 
        }

        // レスポンスがバイナリデータ（PDF）であると想定
        const blob = await response.blob(); 
        
        // ファイル名をレスポンスヘッダーから取得（Content-Disposition）
        const disposition = response.headers.get('Content-Disposition');
        let filename = `${employeeName}_${yearMonthStr}_勤務表.pdf`; // デフォルトのファイル名
        
        if (disposition && disposition.indexOf('attachment') !== -1) {
            // ヘッダーからファイル名を取得するロジック（URLエンコードされている場合に対応）
            const filenameMatch = disposition.match(/filename\*=UTF-8''(.+)/i);
            if (filenameMatch && filenameMatch[1]) {
                // エンコードされたファイル名をデコード
                filename = decodeURIComponent(filenameMatch[1]);
            }
        }

        // ダウンロード処理
        const downloadUrl = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = downloadUrl;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(downloadUrl);

        alert(`✅ PDFファイル (${filename}) のダウンロードを開始しました。`);

    } catch (error) {
        console.error("PDF生成エラー:", error);
        alert(`❌ PDF生成中にエラーが発生しました。\n${error.message}`);
    } finally {
        // ボタンを元に戻す
        originalButton.innerHTML = originalButtonText;
        originalButton.disabled = false;
    }
}