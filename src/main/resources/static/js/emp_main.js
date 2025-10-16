// emp_main.js (最終修正版: JWT認証対応)

// ログイン中の従業員IDをセッションストレージから取得する関数 (共通JSファイルに存在することを前提)
function getLoggedInEmployeeId() {
    const employeeId = sessionStorage.getItem('loggedInEmployeeId'); 
    if (!employeeId) {
        // セッション切れの場合はログイン画面へリダイレクト（セキュリティ担保）
        window.location.href = '/html/punch_login.html'; 
        // エラーをスローして後続の処理を停止
        throw new Error("操作を行う従業員IDが見つかりません。ログインが必要です。");
    }
    return employeeId;
}

/**
 * ログインユーザーのロールを取得する。
 * @returns {string} ロールコード (例: 'ADMIN', 'MGR', 'EMP')
 */
function getLoggedInUserRole() {
    const role = sessionStorage.getItem('loggedInUserRole'); 
    return role ? role.toUpperCase() : 'UNKNOWN'; 
}

// --- JWTトークン取得ヘルパー ---
function getAuthHeaders() {
    const token = sessionStorage.getItem('token');
    // ★修正点: getElementByIdはHTML要素の取得メソッド。sessionStorageからの取得は getItem を使う。
    const employeeId = sessionStorage.getItem('loggedInEmployeeId'); 

    if (!token || !employeeId) {
        // トークンがない場合、再ログインを促す
        alert("認証セッションが無効です。再度ログインしてください。");
        window.location.href = '/html/punch_login.html'; // パスを統一
        return {};
    }
    return {
        'Authorization': `Bearer ${token}`,
        'X-Operator-Id': employeeId // ★修正なし: 監査・認可のためにIDも常に付与
    };
}

// 取得した全サマリーを保持するためのグローバル変数
let allSummaries = []; 
let currentDisplayedSummaries = []; 


document.addEventListener('DOMContentLoaded', function () {
    const userRole = getLoggedInUserRole();
    try {
        const employeeId = getLoggedInEmployeeId(); 
        
        // 画面がロードされたら、ヘッダーにユーザー名を表示するための処理を想定
        const userInfoSpan = document.querySelector('.user-info span');
        if (userInfoSpan) {
            userInfoSpan.textContent = `従業員 (${employeeId})`;
        }
        
        // EMP専用画面のため、従業員名検索プルダウンを非表示/無効化
        const employeeDropdownWrapper = document.getElementById('search-employee')?.closest('.action-bar')?.querySelector('label[for="search-employee"]');
        const employeeDropdown = document.getElementById('search-employee');
        const searchButton = document.getElementById('search-button');

        if (employeeDropdownWrapper) {
            employeeDropdownWrapper.style.display = 'none';
            if (employeeDropdown) employeeDropdown.style.display = 'none';
        }
        
        // 初期データの取得 (ログイン中の従業員のみ)
        // APIパスを /api/summaries/emplimitsummaries に合わせる
        fetchAndDisplaySummaries(employeeId);
        
        // イベントリスナーの設定
        if (searchButton) {
            searchButton.addEventListener('click', handleSearch);
        }
        
        // PDFエクスポートボタンのイベントリスナー（EMPは自分の分だけ）
        const pdfExportBtn = document.getElementById('csv-export-btn'); 
        if (pdfExportBtn) {
            pdfExportBtn.addEventListener('click', () => {
                const selectedMonth = document.getElementById('search-month').value;
                
                if (!selectedMonth) {
                    alert("月次勤務表PDF出力を行うには、対象月を選択してください。");
                    return;
                }
                
                // employeeId を使って、ユーザー名を DOM から取得する代わりに取得することを推奨
                const employeeName = userInfoSpan ? userInfoSpan.textContent.split(' ')[1].replace(/[()]/g, '') : employeeId;

                // ★修正点: ダミーの alert を削除し、exportToPdf 関数を呼び出す★
                exportToPdf(employeeId, selectedMonth, employeeName); 
            });
        }

    } catch (e) {
        // getLoggedInEmployeeId内でリダイレクトされるため、ここでは何も処理しない
        // console.warn("セッション情報なし。ログイン画面へリダイレクトします。");
    }
});


// -------------------------------------------------------------
// 【データ取得/描画関数】
// -------------------------------------------------------------

async function fetchAndDisplaySummaries(employeeId) {
    const headers = getAuthHeaders();
    if (!headers['Authorization']) return;

    try {
        // ★修正点: APIパスを /api/summaries/emplimitsummaries に修正
        // EMPユーザーのIDをクエリパラメータとしてAPIに渡し、自身のデータのみを要求する
        const response = await fetch(`/api/summaries/emplimitsummaries?employeeId=${employeeId}`, { headers }); 
        
        if (!response.ok) {
             if (response.status === 403) throw new Error('データアクセスが拒否されました。');
             const errorText = await response.text();
             console.error('APIエラーレスポンス:', errorText);
             throw new Error(`勤怠サマリーの取得に失敗しました (ステータス: ${response.status})。`);
        }
        
        const summaries = await response.json(); 

        allSummaries = summaries; // 全データをグローバル変数に保存
        
        // プルダウンメニューを初期化/描画 (月度のみ)
        populateSearchDropdowns(summaries);

        // フィルタリングなしで初期表示
        applyFiltersAndRenderTable(allSummaries); 

    } catch (error) {
        console.error('エラー(勤怠ログ):', error);
        const tableBody = document.querySelector('#attendance-logs .data-table tbody');
        const COL_SPAN = 8; // EMP画面のHTMLに合わせた列数
        if (tableBody) {
             tableBody.innerHTML = `<tr><td colspan="${COL_SPAN}" style="color: red; text-align: center;">エラー: ${error.message}</td></tr>`;
        }
        if (error.message.includes('拒否')) alert(error.message);
    }
}

/**
 * 取得したサマリーデータに基づき、検索プルダウンを生成する (月度のみ)
 */
function populateSearchDropdowns(summaries) {
    const monthDropdown = document.getElementById('search-month');

    if (!monthDropdown) return; 

    // 対象月のリストを生成 (YYYY-MM形式, Setで重複を除去し、降順にソート)
    const months = [...new Set(summaries.map(s => s.workDate ? s.workDate.substring(0, 7) : null).filter(m => m !== null))].sort((a, b) => b.localeCompare(a));

    // ドロップダウンを初期化
    monthDropdown.innerHTML = '<option value="">--- 全期間 ---</option>';

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
 * 検索処理を実行する関数 (月度プルダウン対応)
 */
function handleSearch() {
    const selectedMonth = document.getElementById('search-month').value;
    
    // 全データからフィルタリング
    const filteredSummaries = allSummaries.filter(summary => {
        if (selectedMonth) {
            const summaryMonth = summary.workDate ? summary.workDate.substring(0, 7) : '---';
            return summaryMonth === selectedMonth;
        }
        return true; // 月の選択がなければ全て表示
    });

    applyFiltersAndRenderTable(filteredSummaries);
}


/**
 * フィルタリングされたサマリーデータをテーブルに描画する関数
 */
function applyFiltersAndRenderTable(summaries) {
    currentDisplayedSummaries = summaries; 
    
    const tableBody = document.querySelector('#attendance-logs .data-table tbody');
    if (!tableBody) return;
    tableBody.innerHTML = ''; 

    // HTMLの列数に合わせて colspan を設定 (日付,氏名,出,休,退,実働,承認,アクション = 8列)
    const COL_SPAN = 8; 
    
    if (summaries.length === 0) {
        tableBody.innerHTML = `<tr><td colspan="${COL_SPAN}" style="text-align: center;">該当する勤怠サマリーがありません。</td></tr>`;
        return;
    }
    
    // 時刻整形ヘルパー関数を定義
    const formatTime = (timeString) => {
        if (!timeString) return '---';
        const parts = timeString.split('.'); // 秒以下を削除
        return parts[0]; 
    };

    summaries.forEach(sum => {
        const row = document.createElement('tr');

        // 承認ステータスの表示ロジック
        const approvalStatus = (sum.approvalStatus || 'PENDING').toUpperCase(); 
        let approvalText = approvalStatus === 'APPROVED' || approvalStatus === 'FINALIZED' ? '承認済' 
                             : approvalStatus === 'PENDING' ? '承認待ち' : approvalStatus;
        let approvalClass = approvalStatus === 'APPROVED' || approvalStatus === 'FINALIZED' ? 'status-approved' : 'status-pending';
        
        // EMP画面では承認ボタンは不要だが、修正ボタンは必要
        const actions = `<button class="small-btn edit-btn" data-id="${sum.id}"><i class="fas fa-pen"></i> 修正</button>`;

        // HTMLの列構成に合わせて描画ロジックを修正
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
                // 修正画面（edit_attendance.html）にIDを渡して遷移
                window.location.href = `/html/edit_attendance.html?summaryId=${summaryId}`;
            }
        });
    });
}


// --- PDF生成・ダウンロード関数 (API経由) ---
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
        // バックエンドAPIの呼び出し（/api/summaries/export/pdf を想定）
        const url = `/api/exports/pdf/summaries?employeeId=${employeeId}&yearMonth=${yearMonthStr}`;
        // 認証ヘッダーを付与
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