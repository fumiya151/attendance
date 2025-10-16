// payroll_calculation.js (最終修正版: JWT認証対応)

// ログイン中の従業員IDをセッションストレージから取得する関数 (共通JSファイルに存在することを前提)
function getLoggedInEmployeeId() {
    const employeeId = sessionStorage.getItem('loggedInEmployeeId'); 
    if (!employeeId) {
        throw new Error("操作を行う従業員IDが見つかりません。ログインが必要です。");
    }
    return employeeId;
}

/**
 * JWTトークンとX-Operator-Idを取得するヘルパー関数 (★追加★)
 */
function getAuthHeaders() {
    const token = sessionStorage.getItem('token');
    const employeeId = sessionStorage.getItem('loggedInEmployeeId');
    
    if (!token || !employeeId) {
        alert("認証セッションが無効です。再度ログインしてください。");
        // 給与計算は管理者画面からの遷移のため、こちらにリダイレクト
        window.location.href = '/html/admin_login.html'; 
        return {};
    }
    
    return {
        'Authorization': `Bearer ${token}`,
        'X-Operator-Id': employeeId 
    };
}


document.addEventListener('DOMContentLoaded', function () {
    const calculateBtn = document.getElementById('calculate-btn');
    const tableBody = document.getElementById('payroll-table-body');
    
    // 期間入力フィールドの要素を取得
    const startDateInput = document.getElementById('start-date-input');
    const endDateInput = document.getElementById('end-date-input');

    // URLから期間パラメータを取得
    const urlParams = new URLSearchParams(window.location.search);
    const period = urlParams.get('period'); // 'YYYY-MM' 形式を期待

    // ------------------------------------------
    // 期間 YYYY-MM を YYYY-MM-DD に変換するヘルパー関数
    // ------------------------------------------
    function convertPeriodToDates(periodString) {
        if (!periodString || periodString.length !== 7 || periodString.indexOf('-') === -1) {
            return null; // 無効な形式
        }
        const [year, month] = periodString.split('-').map(Number);
        
        // Date.UTC() を使用し、タイムゾーンのズレを回避 (ISO形式 YYYY-MM-DD)
        const startDate = new Date(Date.UTC(year, month - 1, 1)).toISOString().split('T')[0];
        const endDate = new Date(Date.UTC(year, month, 0)).toISOString().split('T')[0];
        
        return { startDate, endDate };
    }
    
    // ------------------------------------------
    // 計算処理の実行を担うメイン関数
    // ------------------------------------------
    function fetchAndDisplayPayroll(start, end) {
        const url = `/api/payroll/calculate?startDate=${start}&endDate=${end}`;
        
        const headers = getAuthHeaders();
        if (!headers['Authorization']) {
            tableBody.innerHTML = '<tr><td colspan="4" style="color: red;">❌ エラー: 認証情報がありません。ログインし直してください。</td></tr>';
            return;
        }

        tableBody.innerHTML = '<tr><td colspan="4">計算中...</td></tr>';
        
        // ★修正点: 認証ヘッダーを付与
        fetch(url, { headers })
            .then(response => {
                if (!response.ok) {
                    return response.text().then(text => { 
                        // エラーレスポンスを詳細に解析
                        let errorMsg = 'サーバー側で予期せぬエラーが発生しました。';
                        try {
                            const jsonError = JSON.parse(text);
                            if (jsonError.message) errorMsg = jsonError.message;
                        } catch (e) {}
                        // 4xx, 5xx ステータスの場合
                        throw new Error(errorMsg || `計算に失敗しました (Status: ${response.status})`); 
                    });
                }
                return response.json();
            })
            .then(data => {
                tableBody.innerHTML = '';
                if (data.length === 0) {
                    // ★ 修正: エラーログ WARN で除外されたケースを考慮したメッセージに更新
                    tableBody.innerHTML = '<tr><td colspan="4">給与計算対象の従業員が見つかりません。期間内の**確定済み勤怠**または**有効な時給設定**を確認してください。</td></tr>';
                    return;
                }
                
                data.forEach(payroll => {
                    const employeeName = payroll.employeeName || payroll.employeeId; // ★ 修正: 名前がない場合のフォールバック
                    const row = document.createElement('tr');
                    row.innerHTML = `
                        <td>${payroll.employeeId}</td>
                        <td>${employeeName}</td>
                        <td>${payroll.totalHours.toFixed(2)}</td>
                        <td>¥${Math.round(payroll.calculatedSalary).toLocaleString()}</td>
                    `;
                    tableBody.appendChild(row);
                });
            })
            .catch(error => {
                console.error('Error fetching payroll data:', error);
                const userMessage = error.message.includes("Required parameter 'startDate'") ?
                    "期間が正しく選択されていません。" :
                    error.message;
                tableBody.innerHTML = `<tr><td colspan="4" style="color: red;">❌ エラー: ${userMessage}</td></tr>`;
            });
    }
    
    // ------------------------------------------
    // ページロード時の判定ロジック (自動実行の制御)
    // ------------------------------------------
    if (period) {
        // ★ 1. URLパラメータで期間が渡された場合、自動で集計を開始
        const dates = convertPeriodToDates(period);
        
        if (dates) {
            // 期間入力フィールドを反映 (ユーザー確認用)
            if (startDateInput) startDateInput.value = dates.startDate;
            if (endDateInput) endDateInput.value = dates.endDate;
            
            // 自動で集計を開始
            fetchAndDisplayPayroll(dates.startDate, dates.endDate);
        } else {
            tableBody.innerHTML = '<tr><td colspan="4" style="color: orange;">⚠️ URLから無効な期間情報が渡されました。</td></tr>';
        }
    } else {
       // URLパラメータがない場合、計算ボタンクリックを待つ
       tableBody.innerHTML = '<tr><td colspan="4">集計期間を選択し、「集計実行」ボタンを押してください。</td></tr>';
        
        // ★ 期間が未設定の場合、入力フィールドに今月の日付を初期設定する
        const today = new Date();
        const firstDay = new Date(Date.UTC(today.getFullYear(), today.getMonth(), 1)).toISOString().split('T')[0];
        const lastDay = new Date(Date.UTC(today.getFullYear(), today.getMonth() + 1, 0)).toISOString().split('T')[0];
        if (startDateInput) startDateInput.value = firstDay;
        if (endDateInput) endDateInput.value = lastDay;
    }


    // ------------------------------------------
    // 画面内でのボタンクリックイベント (再実行)
    // ------------------------------------------
    if (calculateBtn) {
        // ★ 2. 画面上のボタンが押された場合、入力フィールドの値で計算を再実行
        calculateBtn.addEventListener('click', () => {
            // バリデーションチェック
            if (!startDateInput.value || !endDateInput.value) {
                 alert("開始日と終了日を入力してください。");
                 return;
            }
            fetchAndDisplayPayroll(startDateInput.value, endDateInput.value);
        });
    }
});