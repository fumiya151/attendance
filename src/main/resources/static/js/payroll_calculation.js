document.addEventListener('DOMContentLoaded', function () {
    const calculateBtn = document.getElementById('calculate-btn');
    const tableBody = document.getElementById('payroll-table-body');

    // ★ 修正箇所：期間入力フィールドの要素を取得
    const startDateInput = document.getElementById('start-date-input');
    const endDateInput = document.getElementById('end-date-input');

    // 初期値として当月の日付を設定
    const today = new Date();
    // 当月の1日
    const firstDay = new Date(today.getFullYear(), today.getMonth(), 1).toISOString().split('T')[0];
    // 当月の最終日
    const lastDay = new Date(today.getFullYear(), today.getMonth() + 1, 0).toISOString().split('T')[0];
    startDateInput.value = firstDay;
    endDateInput.value = lastDay;


    calculateBtn.addEventListener('click', fetchAndDisplayPayroll);

    // ページロード時に自動計算を実行 (初期値として設定した当月分を計算)
    fetchAndDisplayPayroll();


    function fetchAndDisplayPayroll() {
        // ★ 修正箇所：期間の値を取得
        const startDate = startDateInput.value;
        const endDate = endDateInput.value;

        // 入力値の基本的な検証
        if (!startDate || !endDate) {
            tableBody.innerHTML = '<tr><td colspan="4" style="color: orange;">⚠️ 計算期間を選択してください。</td></tr>';
            return;
        }

        // ★ 修正箇所：APIのURLに期間をクエリパラメータとして追加
        // Javaコントローラーが期待する形式: /api/payroll/calculate?startDate=...&endDate=...
        const url = `/api/payroll/calculate?startDate=${startDate}&endDate=${endDate}`;

        // ローディング表示
        tableBody.innerHTML = '<tr><td colspan="4">計算中...</td></tr>';

        // ★ 修正箇所：構築したURLを使用してfetchを実行
        fetch(url)
            .then(response => {
                if (!response.ok) {
                    // サーバーからのエラー応答を解析し、エラーメッセージを取得
                    return response.text().then(text => {
                        let errorMsg = 'サーバー側で予期せぬエラーが発生しました。';
                        try {
                            const jsonError = JSON.parse(text);
                            if (jsonError.message) errorMsg = jsonError.message;
                        } catch (e) {
                            // JSONではない場合は、レスポンステキストをそのまま使用
                            errorMsg = text.length > 500 ? 'サーバーエラーが発生しました。' : text;
                        }
                        throw new Error(errorMsg);
                    });
                }
                return response.json();
            })
            .then(data => {
                tableBody.innerHTML = ''; // テーブルをクリア

                if (data.length === 0) {
                    tableBody.innerHTML = '<tr><td colspan="4">計算対象の従業員が見つかりません。期間と時給設定を確認してください。</td></tr>';
                    return;
                }

                data.forEach(payroll => {
                    const row = document.createElement('tr');
                    row.innerHTML = `
                        <td>${payroll.employeeId}</td>
                        <td>${payroll.employeeName}</td>
                        <td>${payroll.totalHours.toFixed(2)}</td>
                        <td>${Math.round(payroll.calculatedSalary).toLocaleString()}</td>
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
});