// payroll_calculation.js (最終修正版: 個別/全体 PDF出力対応)

// ログイン中の従業員IDをセッションストレージから取得する関数 (共通JSファイルに存在することを前提)
function getLoggedInEmployeeId() {
    const employeeId = sessionStorage.getItem('loggedInEmployeeId');
    if (!employeeId) {
        throw new Error("操作を行う従業員IDが見つかりません。ログインが必要です。");
    }
    return employeeId;
}

/**
 * JWTトークンとX-Operator-Idを取得するヘルパー関数
 */
function getAuthHeaders() {
    const token = sessionStorage.getItem('token');
    const employeeId = sessionStorage.getItem('loggedInEmployeeId');

    if (!token || !employeeId) {
        alert("認証セッションが無効です。再度ログインしてください。");
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
    // ★修正: IDを export-all-pdf-btn に変更★
    const exportAllPdfBtn = document.getElementById('export-all-pdf-btn'); 
    const tableBody = document.getElementById('payroll-table-body');

    // 期間入力フィールドの要素を取得
    const startDateInput = document.getElementById('start-date-input');
    const endDateInput = document.getElementById('end-date-input');

    // URLから期間パラメータを取得
    const urlParams = new URLSearchParams(window.location.search);
    const period = urlParams.get('period');

    // ------------------------------------------
    // 期間 YYYY-MM を YYYY-MM-DD に変換するヘルパー関数
    // ------------------------------------------
    function convertPeriodToDates(periodString) {
        if (!periodString || periodString.length !== 7 || periodString.indexOf('-') === -1) {
            return null;
        }
        const [year, month] = periodString.split('-').map(Number);

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
            // ★colspanを5に修正★
            tableBody.innerHTML = '<tr><td colspan="5" style="color: red;">❌ エラー: 認証情報がありません。ログインし直してください。</td></tr>';
            return;
        }

        tableBody.innerHTML = '<tr><td colspan="5">計算中...</td></tr>';

        fetch(url, { headers })
            .then(response => {
                if (!response.ok) {
                    return response.text().then(text => {
                        let errorMsg = 'サーバー側で予期せぬエラーが発生しました。';
                        try {
                            const jsonError = JSON.parse(text);
                            if (jsonError.message) errorMsg = jsonError.message;
                        } catch (e) {}
                        throw new Error(errorMsg || `計算に失敗しました (Status: ${response.status})`);
                    });
                }
                return response.json();
            })
            .then(data => {
                tableBody.innerHTML = '';
                if (data.length === 0) {
                    // ★colspanを5に修正★
                    tableBody.innerHTML = '<tr><td colspan="5">給与計算対象の従業員が見つかりません。期間内の**確定済み勤怠**または**有効な時給設定**を確認してください。</td></tr>';
                    return;
                }

                data.forEach(payroll => {
                    const employeeName = payroll.employeeName || payroll.employeeId;
                    const row = document.createElement('tr');
                    row.innerHTML = `
                        <td>${payroll.employeeId}</td>
                        <td>${employeeName}</td>
                        <td>${payroll.totalHours.toFixed(2)}</td>
                        <td>¥${Math.round(payroll.basePaySalary).toLocaleString()}</td>
                        <td>
                            <button class="secondary-btn export-single-pdf-btn" 
                                data-employee-id="${payroll.employeeId}">
                                <i class="fas fa-file-export"></i>
                                明細出力
                            </button>
                        </td>
                    `;
                    tableBody.appendChild(row);
                });
                
                // ★追加: 明細出力ボタンにイベントリスナーを設定★
                document.querySelectorAll('.export-single-pdf-btn').forEach(button => {
                    button.addEventListener('click', (event) => {
                        const employeeId = event.target.dataset.employeeId;
                        const start = startDateInput.value;
                        const end = endDateInput.value;

                        if (!start || !end) {
                            alert("期間が設定されていません。");
                            return;
                        }
                        // 個別従業員APIを呼び出す
                        downloadSinglePayrollPdf(employeeId, start, end);
                    });
                });

            })
            .catch(error => {
                console.error('Error fetching payroll data:', error);
                const userMessage = error.message.includes("Required parameter 'startDate'") ?
                    "期間が正しく選択されていません。" :
                    error.message;
                // ★colspanを5に修正★
                tableBody.innerHTML = `<tr><td colspan="5" style="color: red;">❌ エラー: ${userMessage}</td></tr>`;
            });
    }
    
    // ------------------------------------------
    // 個別給与明細PDFダウンロード関数 (★新規追加★)
    // ------------------------------------------
    /**
     * 指定した従業員の給与明細PDFをダウンロードする関数
     */
    function downloadSinglePayrollPdf(employeeId, startDateStr, endDateStr) { 
        // ★修正: APIにemployeeIdを渡すように修正 (バックエンドの /payroll/single パスを使用)★
        const url = `/api/exports/pdf/payroll/single?employeeId=${employeeId}&startDate=${startDateStr}&endDate=${endDateStr}`;
        const headers = getAuthHeaders();
        
        downloadPdfFromApi(url, headers, `${employeeId}_${startDateStr.substring(0, 7)}_明細`);
    }

    // ------------------------------------------
    // 全従業員給与明細PDFダウンロード関数 (★新規追加★)
    // ------------------------------------------
    /**
     * 全従業員分の給与明細PDFをダウンロードする関数
     */
    function downloadAllPayrollPdf(startDateStr, endDateStr) { 
        // 全体APIの呼び出し（既存の /payroll パスを使用）
        const url = `/api/exports/pdf/payroll?startDate=${startDateStr}&endDate=${endDateStr}`;
        const headers = getAuthHeaders();
        
        downloadPdfFromApi(url, headers, `${startDateStr}_${endDateStr}_給与明細`);
    }

    // ------------------------------------------
    // PDFダウンロード共通処理 (★新規追加★)
    // ------------------------------------------
    function downloadPdfFromApi(url, headers, alertBaseName) {
        fetch(url, { headers })
            .then(response => {
                const contentDisposition = response.headers.get('Content-Disposition');
                let filename = alertBaseName + ".pdf"; 
                if (contentDisposition) {
                    const match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/);
                    if (match) {
                        filename = decodeURIComponent(match[1].replace(/\+/g, ' '));
                    }
                }

                if (!response.ok) {
                    return response.text().then(text => {
                        throw new Error(`PDF生成に失敗しました: ${text || response.statusText}`);
                    });
                }

                return response.blob().then(blob => ({ blob, filename }));
            })
            .then(({ blob, filename }) => {
                const url = window.URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.style.display = 'none';
                a.href = url;
                a.download = filename;
                document.body.appendChild(a);
                a.click();
                window.URL.revokeObjectURL(url);
                alert(`✅ ${filename} のダウンロードを開始しました。`);
            })
            .catch(error => {
                console.error('PDFダウンロードエラー:', error);
                alert(`❌ PDF出力中にエラーが発生しました。\n${error.message}`);
            });
    }

    // ------------------------------------------
    // ページロード時の判定ロジック (URLパラメータ処理)
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
            tableBody.innerHTML = '<tr><td colspan="5" style="color: orange;">⚠️ URLから無効な期間情報が渡されました。</td></tr>';
        }
    } else {
       // URLパラメータがない場合、計算ボタンクリックを待つ
       tableBody.innerHTML = '<tr><td colspan="5">集計期間を選択し、「給与計算を実行」ボタンを押してください。</td></tr>'; 
       
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
        calculateBtn.addEventListener('click', () => {
            if (!startDateInput.value || !endDateInput.value) {
                 alert("開始日と終了日を入力してください。");
                 return;
            }
            fetchAndDisplayPayroll(startDateInput.value, endDateInput.value);
        });
    }

    // ------------------------------------------
    // 全員分のPDF出力ボタンのイベントリスナー (★修正★)
    // ------------------------------------------
    if (exportAllPdfBtn) {
        exportAllPdfBtn.addEventListener('click', () => {
            const startDateStr = startDateInput.value;
            const endDateStr = endDateInput.value;

            if (!startDateStr || !endDateStr) {
                alert("PDF出力のためには、開始日と終了日を設定してください。");
                return;
            }
            // 全従業員APIを呼び出す
            downloadAllPayrollPdf(startDateStr, endDateStr);
        });
    }
});